package com.woory.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.woory.backend.entity.Favorite;
import com.woory.backend.entity.GroupUser;
import com.woory.backend.entity.Topic;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
	boolean existsByTopicAndGroupUser(Topic topic, GroupUser groupUser);

	@Modifying
	@Query("delete from Favorite f where f.groupUser = :groupUser and f.topic = :topic")
	void deleteFavoriteByTopicAndGroupUser(@Param("topic") Topic topic, @Param("groupUser") GroupUser groupUser);

	@Query("select f from Favorite f join fetch f.topic where f.groupUser = :groupUser")
	List<Favorite> findAllWithTopicByGroupUser(@Param("groupUser") GroupUser groupUser);
}
