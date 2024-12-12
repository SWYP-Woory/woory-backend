package com.woory.backend.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.woory.backend.dto.GroupInfoDto;
import com.woory.backend.entity.Group;
import com.woory.backend.entity.GroupStatus;
import com.woory.backend.entity.GroupUser;
import com.woory.backend.entity.User;

import jakarta.transaction.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class GroupRepositoryTest {

	@Autowired
	private GroupRepository groupRepository;
	@Autowired
	private GroupUserRepository groupUserRepository;
	@Autowired
	private UserRepository userRepository;

	@Test
	void 그룹_저장시_그룹유저_저장() {
		// given
		Group group = new Group();
		group.setGroupName("우리2");
		User user = new User();
		user.setNickname("이름2");
		User save1 = userRepository.save(user);
		Long userId = save1.getUserId();

		GroupUser groupUser = new GroupUser();
		groupUser.setGroup(group);
		groupUser.setUser(save1);

		group.getGroupUsers().add(groupUser);

		// when
		Group save = groupRepository.save(group);

		// then
		GroupUser groupUser1 = groupUserRepository.findByUser_UserIdAndGroup_GroupId(userId, save.getGroupId()).get();
		assertEquals(groupUser1.getGroup().getGroupName(), "우리2");
		assertEquals(groupUser1.getUser().getNickname(), "이름2");

	}

	@Test
	void 그룹_DTO로_조회() {
		Group group = new Group();
		group.setGroupName("우리1");
		User user = new User();
		user.setNickname("이름1");

		User save = userRepository.save(user);
		GroupUser groupUser = new GroupUser();
		groupUser.setUser(user);
		groupUser.setGroup(group);
		groupUser.setStatus(GroupStatus.GROUP_LEADER);
		group.setGroupUsers(List.of(groupUser));
		groupRepository.save(group);
		// given
		long userId = save.getUserId();

		// when
		List<GroupInfoDto> myGroup = groupUserRepository.findMyGroupInfoDto(userId);

		// then
		assertEquals(myGroup.size(), 1);
		assertEquals(myGroup.get(0).getGroupName(), "우리1");

	}
}
