package com.woory.backend.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FavoriteDto {
	private Long topicId;
	private Date issueDate;
	private String contentImg;
	private String topicText;

	public FavoriteDto(Long topicId, Date issueDate, String contentImg, String topicText) {
		this.topicId = topicId;
		this.issueDate = issueDate;
		this.contentImg = contentImg;
		this.topicText = topicText;
	}
}
