package com.woory.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "topic")
public class Topic {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "topicId")
	private Long topicId;

	@Column(name = "topicContent")
	private String topicContent;

	@Temporal(TemporalType.DATE)
	@Column(name = "issueDate")
	private Date issueDate;

	private int topicByte;

	@ManyToOne
	@JoinColumn(name = "group_id")
	private Group group;

	@OneToMany(mappedBy = "topic")
	private List<Favorite> favorites = new ArrayList<>();

	@Builder.Default
	@OneToMany(mappedBy = "topic", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("contentRegDate asc")
	private List<Content> content = new ArrayList<>();

	public static Topic fromTopicSetWithDateAndGroup(Group group, TopicSet topicSet, Date date) {
		return Topic.builder().group(group)
			.issueDate(date)
			.topicByte(topicSet.getTopic_byte())
			.topicContent(topicSet.getValue())
			.build();
	}
}
