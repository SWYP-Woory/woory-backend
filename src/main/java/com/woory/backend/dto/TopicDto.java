package com.woory.backend.dto;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.woory.backend.entity.Topic;
import com.woory.backend.entity.TopicSet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TopicDto {
	private String topicContent;
	private Long topicId;
	private Date issueDate;
	private Long groupId;
	private int topicByte;
	private List<ContentDto> contents;

	public static TopicDto fromTopicSetWithGroupIdAndDate(TopicSet topicSet, Date date, long groupId) {
		return TopicDto.builder()
			.topicContent(topicSet.getValue())
			.topicByte(topicSet.getTopic_byte())
			.issueDate(date)
			.groupId(groupId)
			.build();
	}

	public static TopicDto fromTopicWithContent(Topic topic) {
		return TopicDto.builder()
			.topicContent(topic.getTopicContent())
			.topicId(topic.getTopicId())
			.issueDate(topic.getIssueDate())
			.contents(
				topic.getContent().stream()
					.map(ContentDto::toContentDto)
					.toList()
			)
			.build();
	}

	public static TopicDto fromTopic(Topic topic) {
		return TopicDto.builder()
			.topicContent(topic.getTopicContent())
			.topicId(topic.getTopicId())
			.issueDate(topic.getIssueDate())
			.build();
	}

}
