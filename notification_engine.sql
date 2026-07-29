-- -------------------------------------------------------------
-- TablePlus 26.7.6(742)
--
-- https://tableplus.com/
--
-- Database: notification_engine
-- Generation Time: 2026-07-30 00:08:23.5390
-- -------------------------------------------------------------


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


DROP TABLE IF EXISTS `delivery_logs`;
CREATE TABLE `delivery_logs` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `notification_id` bigint NOT NULL,
  `channel` enum('email','sms','push','whatsapp') NOT NULL,
  `status` varchar(20) NOT NULL,
  `error_message` text,
  `attempted_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `notification_id` (`notification_id`),
  CONSTRAINT `delivery_logs_ibfk_1` FOREIGN KEY (`notification_id`) REFERENCES `notifications` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `channel` enum('email','sms','push','whatsapp') NOT NULL,
  `status` enum('pending','sent','failed') NOT NULL DEFAULT 'pending',
  `message` text,
  `notification_hash` char(128) NOT NULL,
  `request_content` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `priority` enum('1','2','3') NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`,`channel`,`notification_hash`),
  KEY `user_id_2` (`user_id`),
  KEY `status` (`status`),
  CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `preferences`;
CREATE TABLE `preferences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `channel` enum('email','sms','push','whatsapp') NOT NULL,
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `quiet_hours` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `allowed_messages_priority` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `preferences_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `templates`;
CREATE TABLE `templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `content` text NOT NULL,
  `placeholders` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `template_priority` enum('1','2','3') NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phone` varchar(15) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `fcm_token` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `preferences` (`id`, `user_id`, `channel`, `is_enabled`, `quiet_hours`, `created_at`, `updated_at`, `allowed_messages_priority`) VALUES
(16, 2, 'email', 1, '{\"end\": \"06:00\", \"start\": \"22:00\", \"quietHoursEnabled\": false}', '2026-07-29 12:35:07', '2026-07-29 12:35:07', '[1, 2, 3]'),
(17, 2, 'sms', 1, '{\"end\": \"09:00\", \"start\": \"18:00\", \"quietHoursEnabled\": false}', '2026-07-29 12:35:07', '2026-07-29 12:35:07', '[1, 2, 3]'),
(18, 2, 'push', 1, '{\"end\": \"00:00\", \"start\": \"00:00\", \"quietHoursEnabled\": false}', '2026-07-29 12:35:07', '2026-07-29 12:35:07', '[1, 2, 3]'),
(19, 2, 'whatsapp', 1, '{\"end\": \"00:00\", \"start\": \"00:00\", \"quietHoursEnabled\": false}', '2026-07-29 12:35:07', '2026-07-29 12:35:07', '[1, 2, 3]');

INSERT INTO `templates` (`id`, `name`, `content`, `placeholders`, `created_at`, `updated_at`, `template_priority`) VALUES
(12, 'order_placed_email', '<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head><body style=\"margin: 0; padding: 0; background-color: #f6f7f8; font-family: -apple-system, BlinkMacSystemFont, Arial, sans-serif;\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #f6f7f8; padding: 30px 0;\"><tr><td align=\"center\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 600px; background-color: #ffffff; border-radius: 4px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.05);\"><tr><td align=\"center\" style=\"background-color: #000000; padding: 25px 20px;\"><span style=\"color: #ffffff; font-size: 28px; font-weight: 800; font-style: italic; letter-spacing: -1px;\">SNEAKERX</span></td></tr><tr><td style=\"padding: 40px 30px;\"><h2 style=\"margin: 0 0 15px 0; font-size: 24px; color: #111111;\">Order Confirmed! 🎉</h2><p style=\"margin: 0 0 25px 0; font-size: 16px; color: #555555; line-height: 1.6;\">Hi {name},<br>Thanks for shopping with SneakerX! We\'ve received your order <strong>#{orderId}</strong> and we\'re getting it ready to ship.</p><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #f9f9f9; border-radius: 6px; margin-bottom: 25px;\"><tr><td style=\"padding: 15px;\"><p style=\"margin: 0; font-size: 14px; color: #555555;\"><strong>Product:</strong> {productName}</p><p style=\"margin: 8px 0 0 0; font-size: 14px; color: #555555;\"><strong>Expected Delivery:</strong> {expectedDelivery}</p></td></tr></table><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\"><tr><td align=\"center\" style=\"padding: 10px 0 30px 0;\"><a href=\"{trackingLink}\" style=\"background-color: #ff3f3f; color: #ffffff; text-decoration: none; padding: 14px 35px; border-radius: 4px; font-size: 16px; font-weight: bold; display: inline-block;\">Track Your Order</a></td></tr></table><p style=\"margin: 0; font-size: 14px; color: #777777; line-height: 1.5;\">Please find your official invoice attached to this email. If you have any questions, reply to this email or contact support.</p></td></tr><tr><td align=\"center\" style=\"background-color: #f1f1f1; padding: 20px 30px;\"><p style=\"margin: 0; font-size: 12px; color: #999999;\">© 2026 SneakerX Retail. All rights reserved.</p></td></tr></table></td></tr></table></body></html>', '[\"name\", \"orderId\", \"productName\", \"expectedDelivery\", \"trackingLink\"]', '2026-07-29 17:44:38', '2026-07-29 17:44:38', '1'),
(13, 'order_placed_sms', 'SneakerX: Hi {name}, your order #{orderId} for {productName} is confirmed! Expected delivery: {expectedDelivery}. Track it here: {trackingLink}', '[\"name\", \"orderId\", \"productName\", \"expectedDelivery\", \"trackingLink\"]', '2026-07-29 17:44:38', '2026-07-29 17:44:38', '1'),
(14, 'order_placed_push', 'Your order #{orderId} for {productName} is confirmed and will arrive by {expectedDelivery}. Tap to track!', '[\"name\", \"orderId\", \"productName\", \"expectedDelivery\"]', '2026-07-29 17:44:38', '2026-07-29 17:44:38', '1'),
(15, 'order_placed_whatsapp', 'Hi {name}! 👟🔥\n\nYour order *#{orderId}* is confirmed.\n\n*Item:* {productName}\n*Expected Delivery:* {expectedDelivery}\n\nTrack your shipment live: {trackingLink}\n\n_We\'ve attached your invoice and a pic of your new kicks below. Thanks for choosing SneakerX!_', '[\"name\", \"orderId\", \"productName\", \"expectedDelivery\", \"trackingLink\"]', '2026-07-29 17:44:38', '2026-07-29 17:44:38', '1');

INSERT INTO `users` (`id`, `name`, `email`, `phone`, `created_at`, `updated_at`, `fcm_token`) VALUES
(2, 'Akshit Tyagi', 'tyagiakshit171.learning@gmail.com', '+917906767568', '2026-07-29 12:35:07', '2026-07-29 18:35:41', 'xxxxxxxxxxx');



/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;