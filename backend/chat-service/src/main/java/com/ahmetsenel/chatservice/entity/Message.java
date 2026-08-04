package com.ahmetsenel.chatservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private String senderUsername;

    // type = DIRECT olduğunda dolu
    private Long receiverId;

    private String receiverUsername;

    // type = GROUP olduğunda dolu
    @ManyToOne(fetch = FetchType.LAZY)
    private Group group;

    @Column(nullable = false, length = 2000)
    private String content;

    private boolean read = false;

    private boolean delivered = false;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Long> deliveredToUsers = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Long> readByUsers = new HashSet<>();

    @Column(name = "is_deleted")
    private boolean deleted = false;

    private Long replyToId;

    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}