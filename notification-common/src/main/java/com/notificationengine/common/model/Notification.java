package com.notificationengine.common.model;

import com.notificationengine.common.enums.Channel;
import com.notificationengine.common.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    private Status status = Status.pending;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "request_content", columnDefinition = "JSON")
    private String requestContent;

    @Column(name = "notification_hash", length = 128)
    private String notificationHash;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "provider_message_sid", length = 64)
    private String providerMessageSid;

    @Column(name = "priority")
    private String priority = "1";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Notification(User user, Channel channel, String message, String requestContent, String notificationHash){
        this.user = user;
        this.channel = channel;
        this.message = message;
        this.requestContent = requestContent;
        this.notificationHash = notificationHash;
    }
}