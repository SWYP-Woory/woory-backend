package com.woory.backend.service;

import com.woory.backend.dto.*;
import com.woory.backend.entity.*;
import com.woory.backend.error.CustomException;
import com.woory.backend.error.ErrorCode;
import com.woory.backend.repository.*;
import com.woory.backend.utils.SecurityUtil;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContentService {

	private ContentRepository contentRepository;
	private UserRepository userRepository;
	private GroupUserRepository groupUserRepository;
	private TopicRepository topicRepository;
	private final ContentReactionRepository contentReactionRepository;

	@Autowired
	public ContentService(UserRepository userRepository, GroupRepository groupRepository,
		ContentRepository contentRepository, GroupUserRepository groupUserRepository,
		TopicRepository topicRepository, ContentReactionRepository contentReactionRepository) {
		this.userRepository = userRepository;
		this.contentRepository = contentRepository;
		this.groupUserRepository = groupUserRepository;
		this.topicRepository = topicRepository;
		this.contentReactionRepository = contentReactionRepository;

	}

	@Transactional
	public Content createContent(Long groupId, Long topicId, String contentText, String contentImgPath) {
		Long userId = SecurityUtil.getCurrentUserId();
		User user = userRepository.findByUserIdWithGroups(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		groupUserRepository.findByUser_UserIdAndGroup_GroupId(userId, groupId)
			.orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
		Topic topic = topicRepository.findById(topicId)
			.orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));

		// Content 생성 및 저장 로직
		Content content = new Content();
		content.setContentText(contentText);
		content.setContentImgPath(contentImgPath);
		content.setUsers(user);
		content.setTopic(topic);
		content.setContentRegDate(new Date());

		return contentRepository.save(content);
	}

	@Transactional
	public void deleteContent(Long groupId, Long contentId) {
		Long userId = SecurityUtil.getCurrentUserId();
		GroupStatus status = groupUserRepository.findByUser_UserIdAndGroup_GroupId(userId, groupId)
			.orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND)).getStatus();
		Content content = contentRepository.findById(contentId)
			.orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

		//본인의 것만 삭제하기 위해서
		if (!content.getUsers().getUserId().equals(userId)) {
			throw new CustomException(ErrorCode.NO_PERMISSION_TO_DELETE);
		}
		if (status == GroupStatus.BANNED || status == GroupStatus.NON_MEMBER) {
			throw new CustomException(ErrorCode.NO_PERMISSION_TO_DELETE);
		}
		contentRepository.delete(content);
	}

	@Transactional
	public Content updateContent(Long groupId, Long contentId, String contentText, String contentImg) {
		Long userId = SecurityUtil.getCurrentUserId();

		groupUserRepository.findByUser_UserIdAndGroup_GroupId(userId, groupId)
			.orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
		Content content = contentRepository.findById(contentId)
			.orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));
		GroupStatus status = getGroupStatus(userId, groupId);

		if (!content.getUsers().getUserId().equals(userId)) {
			throw new CustomException(ErrorCode.NO_PERMISSION_TO_UPDATE);
		}

		if (status == GroupStatus.BANNED || status == GroupStatus.NON_MEMBER) {
			throw new CustomException(ErrorCode.NO_PERMISSION_TO_UPDATE);
		}
		content.setContentText(contentText);
		if (contentImg != null) {
			content.setContentImgPath(contentImg); // 사진 경로 수정
		}
		return contentRepository.save(content);

	}

	public List<ContentDto> getContentsByRegDateLike(String dateStr) {
		List<Content> contents = contentRepository.findContentsByRegDateLike(dateStr + "%");
		return contents.stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}
	public List<ContentDto> getContentsByRegDateMonthLike(String dateStr) {
		List<Content> contents = contentRepository.findContentsByRegDateMonthLike(dateStr + "%");
		return contents.stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	/**
	 * 리엑션을 추가하면 모아서 전달
	 * @param contentId
	 * @param userId
	 * @param newReaction
	 * @return
	 */
	public ContentReactionDto addOrUpdateReaction(Long contentId, Long userId, ReactionType newReaction) {
		Content content = contentRepository.findByContentId(contentId)
			.orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

		Optional<ContentReaction> byId = contentReactionRepository.findContentReactionByContent_ContentIdAndUser_UserId(
			contentId, userId);

		if (byId.isPresent()) {
			ContentReaction contentReaction = byId.get();
			if (contentReaction.getReaction() == newReaction) {
				removeReaction(contentReaction);
				return null;
			}
			// decreaseReactionCount(content, contentReaction.getReaction());
		}
		User user = userRepository.findById(userId)
			.orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));
		ContentReaction contentReaction = new ContentReaction(content, user, newReaction);
		contentReactionRepository.save(contentReaction);

		// increaseReactionCount(content, newReaction);
		contentRepository.save(content);

		return ContentReactionDto.toContentReactionDto(contentReaction);

	}

	/**
	 * 이 부분은 사용안 할 것 같음. -> 컨텐츠 조회 시 같이 조회되도록 수정
	 */
	//컨텐츠의 리액션 보기
	public List<ContentReactionDto> getReactionsByContentId(Long contentId) {
		Content content = contentRepository.findById(contentId)
			.orElseThrow(() -> new RuntimeException("Content not found"));

		List<ContentReaction> reactions = contentReactionRepository.findByContent_ContentId(contentId);

		// Convert List<ContentReaction> to List<ContentReactionDto>
		return reactions.stream()
			.map(ContentReactionDto::toContentReactionDto)
			.collect(Collectors.toList());
	}

	private void removeReaction(ContentReaction contentReaction) {
		Content content = contentReaction.getContent();
		// decreaseReactionCount(content, contentReaction.getReaction());

		contentReactionRepository.delete(contentReaction);

		// Save the content
		contentRepository.save(content);
	}

	private GroupStatus getGroupStatus(Long userId, Long groupId) {
		GroupStatus status = groupUserRepository.findByUser_UserIdAndGroup_GroupId(userId, groupId).get().getStatus();
		return status;
	}

	// private void increaseReactionCount(Content content, ReactionType reaction) {
	// 	switch (reaction) {
	// 		case LIKE -> content.setLikeCount(content.getLikeCount() + 1);
	// 		case LOVE -> content.setLoveCount(content.getLoveCount() + 1);
	// 		case WOW -> content.setWowCount(content.getWowCount() + 1);
	// 		case SAD -> content.setSadCount(content.getSadCount() + 1);
	// 		case ANGRY -> content.setAngryCount(content.getAngryCount() + 1);
	// 	}
	// }
	//
	// private void decreaseReactionCount(Content content, ReactionType reaction) {
	// 	switch (reaction) {
	// 		case LIKE -> content.setLikeCount(content.getLikeCount() - 1);
	// 		case LOVE -> content.setLoveCount(content.getLoveCount() - 1);
	// 		case WOW -> content.setWowCount(content.getWowCount() - 1);
	// 		case SAD -> content.setSadCount(content.getSadCount() - 1);
	// 		case ANGRY -> content.setAngryCount(content.getAngryCount() - 1);
	// 	}
	// }
	private ContentDto convertToDTO(Content content) {
		ContentDto dto = new ContentDto();
		dto.setContentId(content.getContentId());
		dto.setContentText(content.getContentText());
		dto.setContentImgPath(content.getContentImgPath());
		dto.setContentRegDate(content.getContentRegDate());

		TopicRequestDto topicDTO = new TopicRequestDto();
		topicDTO.setTopicId(content.getTopic().getTopicId());
		topicDTO.setTopicContent(content.getTopic().getTopicContent());
		topicDTO.setIssueDate(content.getTopic().getIssueDate());
		topicDTO.setTopicByte(content.getTopic().getTopicByte());

		GroupRequestDto groupDTO = new GroupRequestDto();
		groupDTO.setGroupId(content.getTopic().getGroup().getGroupId());
		groupDTO.setGroupName(content.getTopic().getGroup().getGroupName());
		groupDTO.setPhotoPath(content.getTopic().getGroup().getPhotoPath());

		topicDTO.setGroup(groupDTO);
		dto.setTopic(topicDTO);

		return dto;
	}

}
