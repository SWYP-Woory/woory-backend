package com.woory.backend.repository;

import static org.junit.jupiter.api.TestInstance.*;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.woory.backend.dto.TopicDto;
import com.woory.backend.entity.Group;
import com.woory.backend.entity.Topic;
import com.woory.backend.entity.TopicSet;

import jakarta.transaction.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class BatchTopicRepositoryTest {

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private GroupRepository groupRepository;
	List<Group> groupIndex;

	@BeforeAll
	public void setUp() {
		List<Group> groups = new ArrayList<>();
		for (int i = 0; i < 10_000; i++) {
			groups.add(new Group());
		}
		groupIndex = groupRepository.saveAll(groups);
	}

	@Test
	@DisplayName("배치 사용X 테스트")
	void insertTopicAllGroups() {
		TopicSet topicSet = new TopicSet(1L, "가장 최근에 본 영화", 19);
		Date now = new Date();
		List<TopicDto> list = groupIndex.stream()
			.map(group ->
				TopicDto.fromTopicSetWithGroupIdAndDate(topicSet, now, group.getGroupId())
			).toList();
		long start = System.currentTimeMillis();
		// when
		for (Group group : groupIndex) {
			topicRepository.save(Topic.fromTopicSetWithDateAndGroup(group, topicSet, now));
		}
		long end = System.currentTimeMillis();
		System.out.println("10만건 배치 삽입(토픽) = " + (end - start));
		long count = topicRepository.count();
		// then
		Assertions.assertEquals(groupIndex.size(), count);
	}

	@Test
	@DisplayName("배치 저장 테스트")
	void insertTopicToAllGroups() {
		// given
		TopicSet topicSet = new TopicSet(1L, "가장 최근에 본 영화", 19);
		Date now = new Date();
		List<TopicDto> list = groupIndex.stream()
			.map(group ->
				TopicDto.fromTopicSetWithGroupIdAndDate(topicSet, now, group.getGroupId())
			).toList();
		long start = System.currentTimeMillis();
		// when
		int[] ints = topicRepository.saveAll(list);
		long end = System.currentTimeMillis();

		System.out.println("10만건 배치 삽입(토픽) = " + (end - start));
		long count = topicRepository.count();

		// then
		Assertions.assertEquals(groupIndex.size(), count);

	}
}
