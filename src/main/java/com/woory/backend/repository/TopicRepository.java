package com.woory.backend.repository;

import com.woory.backend.entity.Topic;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long>, BatchTopicRepository {
	Optional<Topic> findByTopicId(Long topicId);

	@Query("select t from Topic t where t.group.groupId = :groupId and t.issueDate = :date")
	Optional<Topic> findTopicByGroupIdAndIssueDate(@Param("groupId") Long groupId, @Param("date") LocalDate start);

	@Query("select t from Topic t where t.group.groupId = :groupId and t.issueDate = :date")
	@EntityGraph(attributePaths = {"content", "content.users"})
	Optional<Topic> findTopicByGroupIdAndIssueDateWithContent(@Param("groupId") Long groupId,
		@Param("date") LocalDate start);

	boolean existsByGroup_GroupIdAndAndIssueDate(long groupId, LocalDate date);

	// hasPrevDay = true 그룹 생성일 보다 조회하는 날짜 - 1일이 뒤임
	// hasPrevDay = false 그룹 생성일이 조회하는 날짜 -1일이 앞임
	@Query("select count(t.topicId) > 0 from Topic t where t.group.groupId = :groupId and t.group.groupRegDate < :date")
	boolean existsByGroupRegDate(@Param("groupId") Long groupId, @Param("date") LocalDate date);

	@Query("select t from Topic t left join fetch t.content where t.topicId = :topicId")
	Optional<Topic> findTopicWithContentsByTopicId(@Param("topicId") Long topicId);

	@Query("select t from Topic t left join fetch t.content where t in :topics order by t.issueDate desc")
	List<Topic> findAllWithContentsByTopics(@Param("topics") List<Topic> topics);
}