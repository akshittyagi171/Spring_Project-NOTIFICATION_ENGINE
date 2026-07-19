package com.notificationengine.PushNConsumer.models.db;

import com.notificationengine.PushNConsumer.models.enums.Channel;
import com.notificationengine.PushNConsumer.models.enums.Status;
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

    @Column(length = 128, unique = true)
    private String notificationHash;

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

