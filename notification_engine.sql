-- -------------------------------------------------------------
-- TablePlus 6.4.8(608)
--
-- https://tableplus.com/
--
-- Database: notification_engine
-- Generation Time: 2026-07-16 14:30:32.5000
-- -------------------------------------------------------------


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `channel` enum('email','sms','push') NOT NULL,
  `status` enum('pending','sent','failed') NOT NULL DEFAULT 'pending',
  `message` text,
  `notification_hash` char(128) DEFAULT NOT NULL,
  `request_content` JSON DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `priority` enum('1','2','3') NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`,`channel`,`notification_hash`),
  KEY `user_id_2` (`user_id`),
  KEY `status` (`status`),
  CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `preferences`;
CREATE TABLE `preferences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `channel` enum('email','sms','push') NOT NULL,
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `quiet_hours` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `allowed_messages_priority` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `preferences_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phone` varchar(15) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `delivery_logs`;
CREATE TABLE delivery_logs (
    `log_id` bigint AUTO_INCREMENT,
    `notification_id` bigint NOT NULL,
    `channel` enum('email', 'sms', 'push') NOT NULL,
    `status` varchar(20) NOT NULL,
    `error_message` text,
    `attempted_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`log_id`),
    FOREIGN KEY (`notification_id`) REFERENCES notifications(`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `preferences` (`id`, `user_id`, `channel`, `is_enabled`, `quiet_hours`, `created_at`, `updated_at`, `allowed_messages_priority`) VALUES
(13, 1, 'email', 1, '{\"end\": \"06:00\", \"start\": \"22:00\", \"quietHoursEnabled\": true}', '2026-07-16 08:58:00', '2026-07-16 08:58:00', '[1, 2]'),
(14, 1, 'sms', 0, '{\"end\": \"09:00\", \"start\": \"18:00\", \"quietHoursEnabled\": true}', '2026-07-16 08:58:00', '2026-07-16 08:58:00', '[1, 2]'),
(15, 1, 'push', 1, '{\"end\": \"00:00\", \"start\": \"00:00\", \"quietHoursEnabled\": false}', '2026-07-16 08:58:00', '2026-07-16 08:58:00', '[3]');

INSERT INTO `templates` (`id`, `name`, `content`, `placeholders`, `created_at`, `updated_at`, `template_priority`) VALUES
(1, 'OTP Verification', 'Your OTP is {otp}. Please use this to complete your verification. Do not share this code with anyone.', '[\"otp\"]', '2026-07-16 08:55:34', '2026-07-16 08:55:34', '1'),
(2, 'Welcome Greeting', 'Hello {name}, welcome to Trigear! We are excited to have you onboard.', '[\"name\"]', '2026-07-16 08:55:47', '2026-07-16 08:55:47', '2'),
(3, 'Password Reset', 'Hi {name}, you requested to reset your password. Use the link below to set a new password: {reset_link}', '[\"name\", \"reset_link\"]', '2026-07-16 08:55:54', '2026-07-16 08:55:54', '1'),
(4, 'Account Deactivation Warning', 'Dear {name}, your account is scheduled for deactivation on {deactivation_date}. Please contact support if this is a mistake.', '[\"name\", \"deactivation_date\"]', '2026-07-16 08:56:02', '2026-07-16 08:56:02', '1'),
(5, 'Birthday Wish', 'Happy Birthday, {name}! 🎉 Wishing you a fantastic day filled with joy and laughter. Here’s a special treat: {birthday_offer}.', '[\"name\", \"birthday_offer\"]', '2026-07-16 08:56:12', '2026-07-16 08:56:12', '2'),
(6, 'Trending Nearby', 'Hi {name}, check out what’s trending in {location}! Don’t miss out on amazing deals and events near you.', '[\"name\", \"location\"]', '2026-07-16 08:56:27', '2026-07-16 08:56:27', '3');

INSERT INTO `users` (`id`, `name`, `email`, `phone`, `created_at`, `updated_at`) VALUES
(1, 'Akshit', 'tyagiakshit171.learning@gmail.com', '7906767568', '2026-07-16 08:55:01', '2026-07-16 08:55:01');



/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;