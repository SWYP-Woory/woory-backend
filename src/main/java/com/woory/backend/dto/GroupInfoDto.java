package com.woory.backend.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.woory.backend.entity.Group;
import com.woory.backend.entity.GroupStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class GroupInfoDto {
	private Long groupId;
	private String groupName;
	private String groupImage;
	@JsonIgnore
	private GroupStatus status;

	public GroupInfoDto(Long groupId, String groupName, String groupImage, GroupStatus groupStatus) {
		this.groupId = groupId;
		this.groupName = groupName;
		this.groupImage = groupImage;
		this.status = groupStatus;
	}

	public static GroupInfoDto fromGroup(Group group) {
		return GroupInfoDto.builder()
			.groupId(group.getGroupId())
			.groupName(group.getGroupName())
			.groupImage(group.getPhotoPath())
			.build();
	}
}
