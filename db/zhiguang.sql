
DROP TABLE IF EXISTS `follower`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `follower` (
                            `id` bigint unsigned NOT NULL,
                            `to_user_id` bigint unsigned NOT NULL,
                            `from_user_id` bigint unsigned NOT NULL,
                            `rel_status` tinyint NOT NULL DEFAULT '1',
                            `created_at` datetime(3) NOT NULL,
                            `updated_at` datetime(3) NOT NULL,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_to_from` (`to_user_id`,`from_user_id`),
                            KEY `idx_to_created` (`to_user_id`,`created_at`,`from_user_id`,`rel_status`),
                            KEY `idx_from` (`from_user_id`,`to_user_id`,`rel_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `follower`
--

LOCK TABLES `follower` WRITE;
/*!40000 ALTER TABLE `follower` DISABLE KEYS */;
/*!40000 ALTER TABLE `follower` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `following`
--

DROP TABLE IF EXISTS `following`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `following` (
                             `id` bigint unsigned NOT NULL,
                             `from_user_id` bigint unsigned NOT NULL,
                             `to_user_id` bigint unsigned NOT NULL,
                             `rel_status` tinyint NOT NULL DEFAULT '1',
                             `created_at` datetime(3) NOT NULL,
                             `updated_at` datetime(3) NOT NULL,
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `uk_from_to` (`from_user_id`,`to_user_id`),
                             KEY `idx_from_created` (`from_user_id`,`created_at`,`to_user_id`,`rel_status`),
                             KEY `idx_to` (`to_user_id`,`from_user_id`,`rel_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `following`
--

LOCK TABLES `following` WRITE;
/*!40000 ALTER TABLE `following` DISABLE KEYS */;
/*!40000 ALTER TABLE `following` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `know_posts`
--

DROP TABLE IF EXISTS `know_posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `know_posts` (
                              `id` bigint unsigned NOT NULL,
                              `tag_id` bigint unsigned DEFAULT NULL COMMENT '主分类/内容分类ID',
                              `tags` json DEFAULT NULL COMMENT '标签名数组，例如 ["java","编程"]',
                              `title` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                              `description` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '摘要/描述，最多50字',
                              `content_url` text COLLATE utf8mb4_unicode_ci COMMENT '正文存储于OSS的访问URL或签名URL',
                              `content_object_key` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'OSS对象Key',
                              `content_etag` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'OSS ETag（用于校验）',
                              `content_size` bigint unsigned DEFAULT NULL COMMENT '正文字节大小',
                              `content_sha256` char(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '正文SHA-256哈希（hex）',
                              `creator_id` bigint unsigned NOT NULL,
                              `is_top` tinyint(1) NOT NULL DEFAULT '0',
                              `type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'image_text',
                              `visible` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'public',
                              `img_urls` json DEFAULT NULL COMMENT '图片URL数组或对象数组',
                              `video_url` text COLLATE utf8mb4_unicode_ci COMMENT '视频URL（一期不使用）',
                              `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft',
                              `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              `publish_time` timestamp NULL DEFAULT NULL,
                              PRIMARY KEY (`id`),
                              KEY `ix_know_posts_creator_ct` (`creator_id`,`create_time`),
                              KEY `ix_know_posts_status_ct` (`status`,`create_time`),
                              KEY `ix_know_posts_tag_ct` (`tag_id`,`create_time`),
                              KEY `ix_know_posts_top_ct` (`is_top`,`create_time`),
                              KEY `ix_know_posts_creator_status_pub` (`creator_id`,`status`,`publish_time`),
                              CONSTRAINT `fk_know_posts_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `know_posts`
--

LOCK TABLES `know_posts` WRITE;
/*!40000 ALTER TABLE `know_posts` DISABLE KEYS */;
/*!40000 ALTER TABLE `know_posts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `login_logs`
--

DROP TABLE IF EXISTS `login_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_logs` (
                              `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                              `user_id` bigint unsigned DEFAULT NULL,
                              `identifier` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
                              `channel` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
                              `ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                              `user_agent` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                              `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
                              `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (`id`),
                              KEY `ix_login_logs_user_created_at` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `login_logs`
--

LOCK TABLES `login_logs` WRITE;
/*!40000 ALTER TABLE `login_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `login_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `outbox`
--

DROP TABLE IF EXISTS `outbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `outbox` (
                          `id` bigint unsigned NOT NULL,
                          `aggregate_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
                          `aggregate_id` bigint unsigned DEFAULT NULL,
                          `type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
                          `payload` json NOT NULL,
                          `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                          PRIMARY KEY (`id`),
                          KEY `ix_outbox_agg` (`aggregate_type`,`aggregate_id`),
                          KEY `ix_outbox_ct` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `outbox`
--

LOCK TABLES `outbox` WRITE;
/*!40000 ALTER TABLE `outbox` DISABLE KEYS */;
/*!40000 ALTER TABLE `outbox` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
                         `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                         `phone` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                         `email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                         `password_hash` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                         `nickname` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
                         `avatar` text COLLATE utf8mb4_unicode_ci,
                         `bio` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                         `zg_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                         `gender` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                         `birthday` date DEFAULT NULL,
                         `school` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                         `tags_json` json DEFAULT NULL,
                         `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_users_phone` (`phone`),
                         UNIQUE KEY `uk_users_email` (`email`),
                         UNIQUE KEY `uk_users_zg_id` (`zg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-03 11:19:05
