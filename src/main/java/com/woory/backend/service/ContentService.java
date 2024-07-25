package com.woory.backend.service;

import com.woory.backend.dto.*;
import com.woory.backend.entity.*;
import com.woory.backend.error.CustomException;
import com.woory.backend.error.ErrorCode;
import com.woory.backend.repository.*;
import com.woory.backend.utils.SecurityUtil;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

@Service
public class ContentService {

	private static final Logger log = LoggerFactory.getLogger(ContentService.class);
	private final TopicSetRepository topicSetRepository;
	private final GroupRepository groupRepository;
	private ContentRepository contentRepository;
	private UserRepository userRepository;
	private GroupUserRepository groupUserRepository;
	private TopicRepository topicRepository;
	private final ContentReactionRepository contentReactionRepository;

	@Autowired
	public ContentService(UserRepository userRepository,
		ContentRepository contentRepository, GroupUserRepository groupUserRepository,
		TopicRepository topicRepository, ContentReactionRepository contentReactionRepository,
		TopicSetRepository topicSetRepository, GroupRepository groupRepository) {
		this.userRepository = userRepository;
		this.contentRepository = contentRepository;
		this.groupUserRepository = groupUserRepository;
		this.topicRepository = topicRepository;
		this.contentReactionRepository = contentReactionRepository;
		this.topicSetRepository = topicSetRepository;
		this.groupRepository = groupRepository;
	}

	public ContentDto getContentById(Long contentId) {
		Content content = contentRepository.findByContentId(contentId)
			.orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));
		ContentDto contentDto = new ContentDto();
		contentDto.setContentId(content.getContentId());
		contentDto.setContentText(content.getContentText());
		contentDto.setContentImgPath(contentDto.getContentImgPath());
		contentDto.setContentRegDate(content.getContentRegDate());

		return contentDto;
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
		// 사용자가 이미 해당 주제에 콘텐츠를 작성했는지 확인
		boolean userHasContentForTopic = contentRepository.existsByTopic_TopicIdAndUsers_UserId(topicId, userId);
		if (userHasContentForTopic) {
			throw new CustomException(ErrorCode.CONTENT_ALREADY_EXISTS);
		}

		// Content 생성 및 저장 로직
		Content content = new Content();
		content.setContentText(contentText);
		if (contentImgPath != null) {
			content.setContentImgPath(contentImgPath);
		}
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

	// public List<ContentDto> getContentsByRegDateLike(Long groupId, String dateStr) {
	// 	List<Content> contents = contentRepository.findByDateWithImgPath(groupId, dateStr);
	// 	return contents.stream()
	// 		.map(this::convertToDTO1)
	// 		.collect(Collectors.toList());
	// }

	public List<ContentDto> getContentsByRegDateMonthLike(Long groupId, String dateStr) {
		List<Content> contents = contentRepository.findByDateWithImgPath(groupId, dateStr + "%");
		Long currentUserId = SecurityUtil.getCurrentUserId();
		checkUserGroup(groupId, currentUserId);

		// 날짜별로 그룹화합니다.
		Map<String, List<Content>> groupedByDate = contents.stream()
			.collect(Collectors.groupingBy(t -> t.getContentRegDate().toString()));

		// ContentDto 리스트로 변환합니다.
		List<Content> contentsToAdd = new ArrayList<>();
		for (Map.Entry<String, List<Content>> entry : groupedByDate.entrySet()) {
			String date = entry.getKey();
			List<Content> contentList = entry.getValue();

			Content firstContentWithImage = null;
			Content firstContent = null;

			for (Content content : contentList) {
				// 날짜별로 첫 번째 콘텐츠를 저장
				if (firstContent == null) {
					firstContent = content;
				}

				// 사진이 있는 경우
				if (content.getContentImgPath() != null && !content.getContentImgPath().isEmpty()) {
					if (firstContentWithImage == null) {
						firstContentWithImage = content;
					}
				}
			}
			// 사진이 있는 콘텐츠가 있는 경우 그것을 사용하고, 그렇지 않으면 첫 번째 콘텐츠를 사용합니다.
			Content finalContentToAdd = (firstContentWithImage != null) ? firstContentWithImage : firstContent;
			contentsToAdd.add(finalContentToAdd);

		}
		return contentsToAdd.stream()
			.map(this::convertToDTO1)
			.collect(Collectors.toList());
	}

	public ContentWithUserDto getContent(Long contentId) {
		Long currentUserId = SecurityUtil.getCurrentUserId();
		Content content = contentRepository.findByContentId(contentId)
			.orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));
		Long groupId = content.getTopic().getGroup().getGroupId();
		checkUserGroup(groupId, currentUserId);
		return ContentWithUserDto.toContentWithUserDto(currentUserId, content);

	}

	public ContentUpdateDto getModifyContentInf(Long contentId) {
		Long currentUserId = SecurityUtil.getCurrentUserId();
		ContentWithUserDto contentWithUserDto = new ContentWithUserDto();
		Content content = contentRepository.findByContentId(contentId)
			.orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));
		Long topicId = content.getTopic().getTopicId();
		Topic topic = topicRepository.findByTopicId(topicId)
			.orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));
		return ContentUpdateDto.ModifyForm(topic, content);
	}

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
		}
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		ContentReaction contentReaction = new ContentReaction(content, user, newReaction);
		contentReactionRepository.save(contentReaction);

		contentRepository.save(content);

		return ContentReactionDto.toContentReactionDto(contentReaction);

	}

	//컨텐츠의 리액션 보기
	public List<ContentReactionDto.ForStatistics> getReactionsByContentId(Long contentId) {
		List<ContentReaction> reactions = contentReactionRepository.findByContentIdWithUser(contentId);

		// Convert List<ContentReaction> to List<ContentReactionDto>
		return ContentReactionDto.toReactionForStatistics(SecurityUtil.getCurrentUserId(), reactions);
	}

	private void removeReaction(ContentReaction contentReaction) {
		Content content = contentReaction.getContent();

		contentReactionRepository.delete(contentReaction);

		// Save the content
		contentRepository.save(content);
	}

	private GroupStatus getGroupStatus(Long userId, Long groupId) {
		GroupStatus status = groupUserRepository.findByUser_UserIdAndGroup_GroupId(userId, groupId).get().getStatus();
		return status;
	}

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

		GroupResponseDto groupDTO = new GroupResponseDto();
		groupDTO.setGroupId(content.getTopic().getGroup().getGroupId());
		groupDTO.setGroupName(content.getTopic().getGroup().getGroupName());
		groupDTO.setPhotoPath(content.getTopic().getGroup().getPhotoPath());

		topicDTO.setGroup(groupDTO);
		dto.setTopic(topicDTO);

		return dto;
	}

	private ContentDto convertToDTO1(Content content) {
		return new ContentDto(
			content.getContentId(),
			content.getContentText(),
			content.getContentImgPath(),
			content.getContentRegDate()
			// Map other fields if necessary
		);
	}

	public TopicDto getTopicWithContents(LocalDate date, Long groupId) {
		log.info("date = {}", date.toString());
		Long currentUserId = SecurityUtil.getCurrentUserId();
		checkUserGroup(groupId, currentUserId);
		Group group = groupRepository.findByGroupId(groupId)
			.orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
		// 그룹 생성일 이전이라면
		if (group.getGroupRegDate().isAfter(date)) {
			throw new CustomException(ErrorCode.CAN_NOT_VIEW_BEFORE_GROUP_REG_DATE);
		}

		Topic topic = topicRepository.findTopicByGroupIdAndIssueDateWithContent(
			groupId, date).orElseGet(() -> {
			TopicSet topicSet = topicSetRepository.findRandomTopic();
			Topic topic2 = Topic.fromTopicSetWithDateAndGroup(group, topicSet, java.sql.Date.valueOf(date));
			return topicRepository.save(topic2);
		});

		boolean hasPrevDay = topicRepository.existsByGroupRegDate(groupId, date);
		boolean hasNextDay
			= topicRepository.existsByGroup_GroupIdAndAndIssueDate(groupId, date.plusDays(1L));

		return TopicDto.fromTopicWithContent(SecurityUtil.getCurrentUserId(), topic, hasPrevDay, hasNextDay);
	}

	private void checkUserGroup(Long groupId, Long currentUserId) {
		groupUserRepository.findByUser_UserIdAndGroup_GroupId(currentUserId, groupId)
				.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	public TopicDto getTopicOnly(LocalDate date, Long groupId) {
		Topic topic = topicRepository.findTopicByGroupIdAndIssueDate(groupId, date)
			.orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
		return TopicDto.fromTopic(topic);
	}
}
