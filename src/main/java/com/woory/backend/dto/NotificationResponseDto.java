package com.woory.backend.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.woory.backend.entity.NotificationType;

import lombok.Builder;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@Getter
@Builder
public class NotificationResponseDto {
	private Long notificationId;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
	private Date topicDate;
	private Long topicId;
	private String topicTitle;
	private String contentUser;
	private Long contentId;
	private String reactionUser;
	private NotificationType notificationType;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
	private Date issueDate;

	public static NotificationResponseDto fromTopicNotification(NotificationDto n) {
		return NotificationResponseDto.builder()
			.notificationId(n.getNotificationId())
			.notificationType(n.getNotificationType())
			.topicId(n.getTopicId())
			.topicDate(n.getTopicDate())
			.topicTitle(n.getTopicTitle())
			.issueDate(n.getIssueDate())
			.build();
	}

	public static NotificationResponseDto fromContentNotification(NotificationDto n) {
		return NotificationResponseDto.builder()
			.notificationId(n.getNotificationId())
			.notificationType(n.getNotificationType())
			.contentUser(n.getAuthor())
			.contentId(n.getContentId())
			.issueDate(n.getIssueDate())
			.build();
	}

	public static NotificationResponseDto fromReactionNotification(NotificationDto n) {
		return NotificationResponseDto.builder()
			.notificationId(n.getNotificationId())
			.notificationType(n.getNotificationType())
			.contentId(n.getContentId())
			.reactionUser(n.getAuthor())
			.issueDate(n.getIssueDate())
			.build();
	}
}
