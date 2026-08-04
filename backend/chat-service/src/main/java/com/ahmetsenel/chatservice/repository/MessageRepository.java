package com.ahmetsenel.chatservice.repository;

import com.ahmetsenel.chatservice.entity.Message;
import com.ahmetsenel.chatservice.repository.projection.GroupUnreadCountProjection;
import com.ahmetsenel.chatservice.repository.projection.UnreadCountProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Message m SET m.read = true WHERE m.type = 'DIRECT' AND " +
            "m.senderId = :senderId AND m.receiverId = :receiverId AND m.read = false")
    void markDirectMessagesAsRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.group.id = :groupId " +
            "AND m.senderId != :userId " +
            "AND m.senderId != 0L " +
            "AND :userId NOT MEMBER OF m.readByUsers " +
            "AND m.sentAt >= (SELECT rm.joinedAt FROM GroupMember rm WHERE rm.group.id = :groupId AND rm.userId = :userId)")
    long countUnreadGroupMessages(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.type = 'DIRECT' AND " +
            "((m.senderId = :user1 AND m.receiverId = :user2) OR " +
            "(m.senderId = :user2 AND m.receiverId = :user1)) " +
            "ORDER BY m.sentAt DESC")
    List<Message> findDirectMessagesPaged(@Param("user1") Long user1, @Param("user2") Long user2, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.group.id = :groupId AND m.sentAt >= " +
            "(SELECT rm.joinedAt FROM GroupMember rm WHERE rm.group.id = :groupId AND rm.userId = :userId) " +
            "ORDER BY m.sentAt DESC")
    List<Message> findGroupMessagesForUser(@Param("groupId") Long groupId, @Param("userId") Long userId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.id IN (" +
            "SELECT MAX(m2.id) FROM Message m2 " +
            "WHERE m2.type = 'DIRECT' AND (m2.senderId = :userId OR m2.receiverId = :userId) " +
            "GROUP BY CASE WHEN m2.senderId = :userId THEN m2.receiverId ELSE m2.senderId END) " +
            "ORDER BY m.sentAt DESC")
    List<Message> findLatestDirectMessagesForUser(@Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.type = 'GROUP' AND m.group.id = :groupId " +
            "AND m.read = false AND m.senderId != :userId " +
            "AND :userId NOT MEMBER OF m.readByUsers")
    List<Message> findUnreadGroupMessagesForUser(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT m.senderId AS senderId, COUNT(m) AS unreadCount " +
            "FROM Message m " +
            "WHERE m.type = 'DIRECT' AND m.receiverId = :userId AND m.read = false " +
            "GROUP BY m.senderId")
    List<UnreadCountProjection> getUnreadCountsGroupedBySender(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Message m SET m.delivered = true " +
            "WHERE m.type = 'DIRECT' AND m.senderId = :senderId " +
            "AND m.receiverId = :receiverId AND m.delivered = false")
    void markDirectMessagesAsDelivered(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Query("SELECT m FROM Message m WHERE m.type = 'GROUP' AND m.group.id = :groupId " +
            "AND m.delivered = false AND m.senderId != :userId " +
            "AND :userId NOT MEMBER OF m.deliveredToUsers")
    List<Message> findUndeliveredGroupMessagesForUser(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT m.group.id AS groupId, COUNT(m) AS unreadCount " +
            "FROM Message m JOIN GroupMember gm ON gm.group.id = m.group.id AND gm.userId = :userId " +
            "WHERE m.group.id IN :groupIds " +
            "AND m.senderId != :userId " +
            "AND m.senderId != 0L " +
            "AND :userId NOT MEMBER OF m.readByUsers " +
            "AND m.sentAt >= gm.joinedAt " +
            "GROUP BY m.group.id")
    List<GroupUnreadCountProjection> getUnreadCountsForGroups(@Param("groupIds") List<Long> groupIds, @Param("userId") Long userId);

    @Query("SELECT m FROM Message m JOIN GroupMember gm ON gm.group.id = m.group.id AND gm.userId = :userId " +
            "WHERE m.id IN (" +
            "  SELECT MAX(m2.id) FROM Message m2 JOIN GroupMember gm2 ON gm2.group.id = m2.group.id AND gm2.userId = :userId " +
            "  WHERE m2.type = 'GROUP' AND m2.group.id IN :groupIds AND m2.sentAt >= gm2.joinedAt " +
            "  GROUP BY m2.group.id" +
            ")")
    List<Message> findLastMessagesForGroups(@Param("groupIds") List<Long> groupIds, @Param("userId") Long userId);
}
