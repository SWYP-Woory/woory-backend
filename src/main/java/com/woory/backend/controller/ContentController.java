package com.woory.backend.controller;

import com.woory.backend.dto.*;

import com.woory.backend.entity.ReactionType;
import com.woory.backend.error.CustomException;
import com.woory.backend.error.ErrorCode;
import com.woory.backend.service.ContentService;

import com.woory.backend.utils.SecurityUtil;
import com.woory.backend.utils.StatusUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/contents")
@Tag(name = "글작성 관련", description = "글작성 관련 API")
public class ContentController {
	private static final SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd");
	private static final Logger log = LoggerFactory.getLogger(ContentController.class);

	private final ContentService contentService;

	@Autowired
	public ContentController(ContentService contentService) {
		this.contentService = contentService;
	}

	@Deprecated
	// 컨텐츠 조회 - 글만 조회
	@GetMapping("/detail/{contentId}")
	public ContentDto getContentById(@PathVariable("contentId") Long contentId) {
		return contentService.getContentById(contentId);
	}

	@Operation(summary = "content 생성")
	@PostMapping("/create")
	public ResponseEntity<Map<String, Object>> createContent(@RequestBody ContentRequestDto requestDto) {
		contentService.createContent(requestDto.getGroupId(), requestDto.getTopicId(),
			requestDto.getContentText(), requestDto.getImages());
		Map<String, Object> response = StatusUtil.getStatusMessage("컨텐츠가 생성되었습니다: ");
		//		response.put("data", content);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "content 수정")
	@PutMapping("/{groupId}/{contentId}")
	public ResponseEntity<Map<String, Object>> updateContent(
		@PathVariable("groupId") Long groupId,
		@PathVariable("contentId") Long contentId,
		@RequestBody ContentRequestDto requestDto) {
		contentService.updateContent(groupId, contentId, requestDto.getContentText(),
			requestDto.getImages());
		Map<String, Object> response = StatusUtil.getStatusMessage("컨텐츠가 수정되었습니다.");
		//		response.put("data", updatedContent);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "content 수정할 데이터 가져오기")
	@GetMapping("/modify/{contentId}")
	public ResponseEntity<Map<String, Object>> modifyContent(
		@PathVariable("contentId") Long contentId) {
		ContentUpdateDto modifyContentInf = contentService.getModifyContentInf(contentId);
		Map<String, Object> response = StatusUtil.getStatusMessage("컨텐츠의 정보입니다.");
		response.put("data", modifyContentInf);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "단독 content 조회")
	@GetMapping("/get/{contentId}")
	public ResponseEntity<Map<String, Object>> getContents(@PathVariable("contentId") Long contentId) {
		ContentWithUserAndTopicDto content = contentService.getContent(contentId);
		Map<String, Object> response = StatusUtil.getStatusMessage("컨텐츠가 조회되었습니다");
		response.put("data", content);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "그룹 내 일간 컨텐츠 모두 조회")
	@GetMapping("/{groupId}/get")
	public ResponseEntity<Map<String, Object>> getContentsByRegDate(
		@PathVariable("groupId") Long groupId,
		@RequestParam("day") String day) {
		LocalDate searchDate;
		if (day == null || !day.matches("\\d{4}-\\d{2}-\\d{2}")) {
			throw new CustomException(ErrorCode.INVALID_DATE_FORMAT);
		}
		try {
			searchDate = LocalDate.parse(day);
		} catch (DateTimeParseException e) {
			throw new CustomException(ErrorCode.INVALID_DATE_FORMAT);
		}
		log.info("조회하려는 날짜 = {}", searchDate);
		LocalDate asiaNow = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDate();
		log.info("현재 날짜 = {}", asiaNow.toString());
		if (searchDate.isAfter(asiaNow)) {
			throw new CustomException(ErrorCode.CAN_NOT_VIEW_AFTER_TODAY);
		}

		TopicDto topic = contentService.getTopicWithContents(searchDate, groupId);
		Map<String, Object> response = StatusUtil.getStatusMessage("컨텐츠가 조회되었습니다");
		response.put("data", topic);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "content 월 조회")
	@GetMapping("/get/month")
	public ResponseEntity<Map<String, Object>> getContentsByRegDateMonth(
		@RequestParam("groupId") Long groupId,
		@RequestParam("param") String param) {
		if (param == null || !param.matches("\\d{4}-\\d{2}")) {
			throw new CustomException(ErrorCode.INVALID_DATE_FORMAT);
		}
		List<ContentDto> contents = contentService.getContentsByRegDateMonthLike(groupId, param);
		Map<String, Object> response = StatusUtil.getStatusMessage("컨텐츠가 조회되었습니다");
		response.put("data", contents);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "컨텐츠 삭제")
	@DeleteMapping("/delete/{groupId}/{contentId}")
	public ResponseEntity<Map<String, Object>> deleteContent(
		@PathVariable("groupId") Long groupId,
		@PathVariable("contentId") Long contentId) {
		contentService.deleteContent(groupId, contentId);
		Map<String, Object> response = StatusUtil.getStatusMessage("컨텐츠가 삭제되었습니다");
		return ResponseEntity.ok(response);
	}

	@PostMapping("/reaction")
	public ResponseEntity<Map<String, Object>> addOrUpdateReaction(@RequestBody ReactionReqDto reactionDto) {
		ReactionType reactionType = ReactionType.valueOf(reactionDto.getReaction().toUpperCase());
		ContentReactionDto updatedReaction = contentService.addOrUpdateReaction(reactionDto.getContentId(),
			SecurityUtil.getCurrentUserId(), reactionType);
		if (updatedReaction == null) {
			return ResponseEntity.ok(StatusUtil.getStatusMessage("표현이 삭제되었습니다"));
		}
		return ResponseEntity.ok(StatusUtil.getStatusMessage("표현이 추가되었습니다."));
	}

	@GetMapping("/reaction")
	public ResponseEntity<Map<String, Object>> getReactions(@RequestParam("contentId") Long contentId) {
		List<ContentReactionDto.ForStatistics> reactionsByContentId = contentService.getReactionsByContentId(contentId);
		Map<String, Object> response = StatusUtil.getStatusMessage("반응 조회가 완료되었습니다.");
		response.put("data", reactionsByContentId);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/topic")
	public ResponseEntity<Map<String, Object>> getTodaysTopic(@RequestParam("groupId") Long groupId,
		@RequestParam("day") LocalDate day) {
		TopicDto topic = contentService.getTopicOnly(day, groupId);
		Map<String, Object> statusMessage = StatusUtil.getStatusMessage("토픽 조회에 성공했습니다.");
		statusMessage.put("data", topic);

		return ResponseEntity.ok(statusMessage);
	}

	@PostMapping("/{groupId}/favorites/{topicId}")
	public Map<String, Object> pushFavorite(@PathVariable("groupId") Long groupId,
		@PathVariable("topicId") Long topicId) {
		contentService.addOrDeleteHeart(groupId, topicId);
		return StatusUtil.getStatusMessage("마음 추가/삭제에 성공했습니다.");
	}

	@GetMapping("{groupId}/favorites")
	public Map<String, Object> getFavoritesInThisGroup(@PathVariable("groupId") Long groupId) {
		List<FavoriteDto> favorites = contentService.getFavorites(groupId);
		Map<String, Object> response = StatusUtil.getStatusMessage("마음함 조회에 성공했습니다.");
		response.put("data", favorites);
		return response;
	}
}
