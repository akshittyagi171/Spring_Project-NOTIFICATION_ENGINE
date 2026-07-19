package com.notificationengine.NotificationProcessor.models.db;

import com.notificationengine.NotificationProcessor.models.enums.Channel;
import com.notificationengine.NotificationProcessor.models.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "attempted_at")
    private LocalDateTime attemptedAt = LocalDateTime.now();

    public DeliveryLog(Notification notification, Channel channel, Status status, String errorMessage){
        this.notification = notification;
        this.channel = channel;
        this.status = status;
        this.errorMessage = errorMessage;
    }
}

