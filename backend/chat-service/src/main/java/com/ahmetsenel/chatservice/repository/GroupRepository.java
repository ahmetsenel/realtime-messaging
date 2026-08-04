package com.ahmetsenel.chatservice.repository;

import com.ahmetsenel.chatservice.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g JOIN FETCH g.members WHERE g.id IN " +
            "(SELECT gm.group.id FROM GroupMember gm WHERE gm.userId = :userId)")
    List<Group> findAllByUserIdWithMembers(@Param("userId") Long userId);
}
