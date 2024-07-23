package com.woory.backend.repository;

import com.woory.backend.dto.ContentDto;
import com.woory.backend.entity.Comment;
import com.woory.backend.entity.Content;
import com.woory.backend.entity.ContentReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {
    Optional<Content> findByContentId(Long contentId);


    @Query(value = "SELECT * FROM content WHERE DATE_FORMAT(content_reg_date, '%Y-%m-%d') LIKE :date%", nativeQuery = true)
    List<Content> findContentsByRegDateLike(@Param("date") String date);
    @Query(value = "SELECT * FROM content WHERE DATE_FORMAT(content_reg_date, '%Y-%m') LIKE :date%", nativeQuery = true)
    List<Content> findContentsByRegDateMonthLike(@Param("date") String date);

    @Query(value = "SELECT c.content_id, c.topic_id, c.content_img_path, " +
            "DATE_FORMAT(c.content_reg_date, '%Y-%m-%d') AS content_reg_date, " +
            "c.content_text, c.userid " +
            "FROM content c " +
            "JOIN topic t ON c.topic_id = t.topic_id " +
            "JOIN group_table g ON t.group_id = g.group_id " +
            "WHERE g.group_id = :groupId " +
            "AND DATE_FORMAT(c.content_reg_date, '%Y-%m') LIKE CONCAT(:date, '%') " +
            "ORDER BY c.content_reg_date ASC", nativeQuery = true)
    List<Content> findByDateWithImgPath(@Param("groupId") Long groupId, @Param("date") String date);



    @Query(value = "SELECT c.* FROM content c " +
            "JOIN topic t ON c.topic_id = t.topic_id " +
            "JOIN group_table g ON t.group_id = g.group_id " +
            "WHERE g.group_id = :groupId " +
            "AND DATE_FORMAT(c.content_reg_date, '%Y-%m') = :regDate", nativeQuery = true)
    List<Content> findByGroupIdAndRegDate(@Param("groupId") Long groupId, @Param("regDate") String regDate);





}
