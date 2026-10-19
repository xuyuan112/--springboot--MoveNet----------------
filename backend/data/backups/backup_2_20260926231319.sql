-- Rehab Platform logical backup (JDBC engine, mysqldump-free)
-- generated_at=2026-09-26T23:13:19.5389428
-- backup_id=2
-- >>> STATEMENT <<<
SET FOREIGN_KEY_CHECKS=0
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `action`
-- >>> STATEMENT <<<
CREATE TABLE `action` (
  `action_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '动作ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作名称',
  `target_part_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标康复部位编码',
  `difficulty` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '难度：EASY/MEDIUM/HARD',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '动作说明',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态：DRAFT/PUBLISHED/DISABLED',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人用户ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (`action_id`),
  KEY `idx_action_part_status` (`target_part_code`,`status`),
  KEY `fk_action_creator` (`created_by`),
  CONSTRAINT `fk_action_creator` FOREIGN KEY (`created_by`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `fk_action_part` FOREIGN KEY (`target_part_code`) REFERENCES `rehab_part_dict` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标准动作'
-- >>> STATEMENT <<<
INSERT INTO `action` (`action_id`,`name`,`target_part_code`,`difficulty`,`description`,`status`,`created_by`,`created_at`,`updated_at`,`deleted`) VALUES (1,'膝关节屈伸','KNEE','EASY','测试动作更新','DRAFT',1,'2026-09-26T16:42:17.990','2026-09-26T16:42:17.990',0),(2,'?????','KNEE','EASY','API????','DISABLED',1,'2026-09-26T17:00:00.393','2026-09-26T17:00:00.393',0),(3,'M2????????','SHOULDER','EASY','API verify','DISABLED',1,'2026-09-26T17:46:55.641','2026-09-26T17:46:55.641',0),(4,'M2????????','SHOULDER','EASY','API verify','DISABLED',1,'2026-09-26T17:47:47.472','2026-09-26T17:47:47.472',0),(5,'M2补全自动测试动作','SHOULDER','EASY','x','DISABLED',1,'2026-09-26T17:50:02.275','2026-09-26T17:50:02.275',0),(6,'probe','SHOULDER','EASY',NULL,'DISABLED',1,'2026-09-26T17:50:36.430','2026-09-26T17:50:36.430',0)
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `action_rule`
-- >>> STATEMENT <<<
CREATE TABLE `action_rule` (
  `action_rule_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `action_version_id` bigint unsigned NOT NULL COMMENT '动作版本ID',
  `joint_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关节名称',
  `keypoint_indices` json NOT NULL COMMENT '参与计算的关键点索引',
  `angle_formula` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角度计算公式',
  `joint_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关节角色：PRIMARY主评估/AUXILIARY辅助',
  `dimension` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评估维度：ANGLE角度/STABILITY稳定/SYMMETRY对称/HOLD保持/SPEED速度',
  `phase` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作阶段：START准备/CONCENTRIC向心/ECCENTRIC离心/HOLD保持/FULL全程',
  `min_angle` decimal(6,2) DEFAULT NULL COMMENT '最小角度',
  `max_angle` decimal(6,2) DEFAULT NULL COMMENT '最大角度',
  `standard_angle` decimal(6,2) DEFAULT NULL COMMENT '标准角度',
  `tolerance_soft` decimal(6,2) DEFAULT NULL COMMENT '软容差（提示级）',
  `tolerance_hard` decimal(6,2) DEFAULT NULL COMMENT '硬容差（纠错级）',
  `hold_frames_required` int DEFAULT NULL COMMENT '保持所需帧数',
  `speed_min` decimal(6,2) DEFAULT NULL COMMENT '最小速度(度/秒)',
  `speed_max` decimal(6,2) DEFAULT NULL COMMENT '最大速度(度/秒)',
  `score_weight` decimal(5,4) NOT NULL COMMENT '评分权重(0-1)',
  `severity` tinyint NOT NULL COMMENT '严重等级（数值越大越严重）',
  `prompt_text` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '纠错提示文案',
  `symmetry_pair_rule_id` bigint unsigned DEFAULT NULL COMMENT '对称评估配对规则ID（同版本内）',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (`action_rule_id`),
  UNIQUE KEY `uk_rule_dimension` (`action_version_id`,`joint_name`,`dimension`),
  UNIQUE KEY `uk_rule_id_version` (`action_rule_id`,`action_version_id`),
  KEY `fk_rule_pair` (`symmetry_pair_rule_id`,`action_version_id`),
  CONSTRAINT `fk_rule_pair` FOREIGN KEY (`symmetry_pair_rule_id`, `action_version_id`) REFERENCES `action_rule` (`action_rule_id`, `action_version_id`),
  CONSTRAINT `fk_rule_version` FOREIGN KEY (`action_version_id`) REFERENCES `action_version` (`action_version_id`),
  CONSTRAINT `chk_rule_severity` CHECK ((`severity` >= 0)),
  CONSTRAINT `chk_rule_weight` CHECK (((`score_weight` >= 0) and (`score_weight` <= 1)))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动作评估规则'
-- >>> STATEMENT <<<
INSERT INTO `action_rule` (`action_rule_id`,`action_version_id`,`joint_name`,`keypoint_indices`,`angle_formula`,`joint_role`,`dimension`,`phase`,`min_angle`,`max_angle`,`standard_angle`,`tolerance_soft`,`tolerance_hard`,`hold_frames_required`,`speed_min`,`speed_max`,`score_weight`,`severity`,`prompt_text`,`symmetry_pair_rule_id`,`created_at`,`updated_at`,`deleted`) VALUES (1,1,'left_knee','[]','','PRIMARY','ANGLE','FULL',0.00,135.00,90.00,NULL,NULL,NULL,NULL,NULL,0.6000,1,'???????',NULL,'2026-09-26T17:00:00.562','2026-09-26T17:00:00.562',0),(2,1,'right_knee','[]','','AUXILIARY','STABILITY','HOLD',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0.4000,1,'????',NULL,'2026-09-26T17:00:00.598','2026-09-26T17:00:00.598',0),(3,2,'左肩','[6, 8, 10]','angle(6,8,10)','PRIMARY','ANGLE','FULL',NULL,NULL,90.00,8.00,15.00,NULL,NULL,NULL,1.0000,2,'请抬高左臂',NULL,'2026-09-26T17:50:02.846','2026-09-26T17:50:02.846',0)
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `action_template`
-- >>> STATEMENT <<<
CREATE TABLE `action_template` (
  `template_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `action_version_id` bigint unsigned NOT NULL COMMENT '动作版本ID',
  `source_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板来源：THERAPIST治疗师录制/ALGO算法生成',
  `frame_rate` decimal(6,2) NOT NULL COMMENT '采样帧率',
  `frame_count` int NOT NULL COMMENT '帧数',
  `feature_dim` int NOT NULL COMMENT '特征维度',
  `feature_sequence` json NOT NULL COMMENT '标准特征序列',
  `keyframe_index` json DEFAULT NULL COMMENT '关键帧索引',
  `sample_count` int NOT NULL COMMENT '采样样本数',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态：ACTIVE/DEPRECATED',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人用户ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_template_source` (`action_version_id`,`source_type`),
  KEY `fk_template_creator` (`created_by`),
  CONSTRAINT `fk_template_creator` FOREIGN KEY (`created_by`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `fk_template_version` FOREIGN KEY (`action_version_id`) REFERENCES `action_version` (`action_version_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标准动作模板'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `action_template_sample`
-- >>> STATEMENT <<<
CREATE TABLE `action_template_sample` (
  `template_sample_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '采样记录ID',
  `template_id` bigint unsigned NOT NULL COMMENT '模板ID',
  `file_id` bigint unsigned NOT NULL COMMENT '样本视频文件ID',
  `sample_no` int NOT NULL COMMENT '样本序号',
  `feature_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '特征摘要（去重用）',
  `captured_at` datetime(3) NOT NULL COMMENT '采集时间',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人用户ID',
  PRIMARY KEY (`template_sample_id`),
  UNIQUE KEY `uk_template_sample` (`template_id`,`sample_no`),
  KEY `fk_sample_file` (`file_id`),
  KEY `fk_sample_creator` (`created_by`),
  CONSTRAINT `fk_sample_creator` FOREIGN KEY (`created_by`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `fk_sample_file` FOREIGN KEY (`file_id`) REFERENCES `file_object` (`file_id`),
  CONSTRAINT `fk_sample_template` FOREIGN KEY (`template_id`) REFERENCES `action_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动作模板采样记录'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `action_version`
-- >>> STATEMENT <<<
CREATE TABLE `action_version` (
  `action_version_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '动作版本ID',
  `action_id` bigint unsigned NOT NULL COMMENT '动作ID',
  `version_no` int NOT NULL COMMENT '版本号',
  `action_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作类型：ROM关节活动度/STRENGTH力量/BALANCE平衡',
  `algo_config` json NOT NULL COMMENT '算法配置（采样率、平滑窗口等）',
  `motion_signature` json NOT NULL COMMENT '运动特征签名',
  `pose_engine_id` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '姿态引擎标识（如 movenet-lightning-v4）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态：DRAFT/PUBLISHED/DEPRECATED',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人用户ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (`action_version_id`),
  UNIQUE KEY `uk_action_version` (`action_id`,`version_no`),
  KEY `idx_action_version_status` (`action_id`,`status`),
  KEY `fk_av_creator` (`created_by`),
  CONSTRAINT `fk_av_action` FOREIGN KEY (`action_id`) REFERENCES `action` (`action_id`),
  CONSTRAINT `fk_av_creator` FOREIGN KEY (`created_by`) REFERENCES `user_account` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动作版本'
-- >>> STATEMENT <<<
INSERT INTO `action_version` (`action_version_id`,`action_id`,`version_no`,`action_type`,`algo_config`,`motion_signature`,`pose_engine_id`,`status`,`created_by`,`created_at`,`updated_at`,`deleted`) VALUES (1,2,1,'ROM','{\"fps\": 30}','{\"keypoints\": 17}','movenet-v1','PUBLISHED',1,'2026-09-26T17:00:00.505','2026-09-26T17:00:00.505',0),(2,5,1,'ROM','{\"confLow\": 0.2, \"confHigh\": 0.5, \"emaAlpha\": 0.35, \"weightHold\": 0.2, \"minCycleSec\": 1.2, \"maxGapFrames\": 5, \"medianWindow\": 3, \"poseEngineId\": \"movenet-lightning-web@v1\", \"weightRhythm\": 0.15, \"hysteresisBand\": 8, \"weightSymmetry\": 0.1, \"stateHoldFrames\": 3, \"timeWeightSigma\": 0.2, \"weightAmplitude\": 0.3, \"weightStability\": 0.15, \"boneLenTolerance\": 0.15, \"repairRatioLimit\": 0.3, \"weightSimilarity\": 0.1, \"smoothWindowAccel\": 11, \"smoothWindowSpeed\": 7, \"validFrameRatioMin\": 0.7}','{\"minRom\": 60, \"phaseOrder\": [\"READY\", \"RAISING\", \"HOLD\", \"LOWERING\"], \"targetJoints\": [\"左肩\"], \"peakAngleRange\": [70, 110], \"startAngleRange\": [0, 30], \"expectedDirection\": \"EXTEND\"}','movenet-lightning-web@v1','PUBLISHED',1,'2026-09-26T17:50:02.517','2026-09-26T17:50:02.517',0),(3,6,1,'ROM','{}','{}','e@v1','DRAFT',1,'2026-09-26T17:50:36.496','2026-09-26T17:50:36.496',0)
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `announcement`
-- >>> STATEMENT <<<
CREATE TABLE `announcement` (
  `announcement_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告正文',
  `scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '可见范围：ALL全部/PATIENT患者/THERAPIST治疗师',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态：DRAFT/PUBLISHED/OFFLINE',
  `start_time` datetime(3) NOT NULL COMMENT '展示开始时间',
  `end_time` datetime(3) NOT NULL COMMENT '展示结束时间',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人用户ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (`announcement_id`),
  KEY `idx_notice_scope_status` (`scope`,`status`,`start_time`,`end_time`),
  KEY `fk_notice_creator` (`created_by`),
  CONSTRAINT `fk_notice_creator` FOREIGN KEY (`created_by`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `chk_notice_period` CHECK ((`end_time` >= `start_time`))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台公告'
-- >>> STATEMENT <<<
INSERT INTO `announcement` (`announcement_id`,`title`,`content`,`scope`,`status`,`start_time`,`end_time`,`created_by`,`created_at`,`updated_at`,`deleted`) VALUES (1,'therapist-only-225638','th','THERAPIST','PUBLISHED','2026-01-01T00:00','2026-12-31T23:59:59',1,'2026-09-26T22:56:39.032','2026-09-26T22:56:39.032',0),(2,'M4-ann-file','all-scope-body','ALL','DRAFT','2026-09-26T23:00:33.762','2026-10-26T23:00:33.762',1,'2026-09-26T23:00:34.039','2026-09-26T23:00:34.039',0),(3,'M4-ann-cnpath','all-scope-body','ALL','DRAFT','2026-09-26T23:00:33.826','2026-10-26T23:00:33.826',1,'2026-09-26T23:00:34.103','2026-09-26T23:00:34.103',0),(4,'therapist-only-231316','th','THERAPIST','PUBLISHED','2026-01-01T00:00','2026-12-31T23:59:59',1,'2026-09-26T23:13:17.912','2026-09-26T23:13:17.912',0)
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `assessment_report`
-- >>> STATEMENT <<<
CREATE TABLE `assessment_report` (
  `report_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  `plan_id` bigint unsigned NOT NULL COMMENT '计划ID',
  `period_start` date NOT NULL COMMENT '统计周期开始',
  `period_end` date NOT NULL COMMENT '统计周期结束',
  `data_snapshot` json NOT NULL COMMENT '报告数据快照（指标聚合结果）',
  `ai_draft` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'AI生成的报告草稿',
  `therapist_conclusion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '治疗师审核结论',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态：DRAFT/AI_GENERATED/REVIEWED/PUBLISHED',
  `reviewed_by` bigint unsigned DEFAULT NULL COMMENT '审核治疗师用户ID',
  `reviewed_at` datetime(3) DEFAULT NULL COMMENT '审核时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`report_id`),
  UNIQUE KEY `uk_report_period` (`plan_id`,`period_start`,`period_end`),
  KEY `fk_report_reviewer` (`reviewed_by`),
  CONSTRAINT `fk_report_plan` FOREIGN KEY (`plan_id`) REFERENCES `rehab_plan` (`plan_id`),
  CONSTRAINT `fk_report_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `chk_report_period` CHECK ((`period_end` >= `period_start`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评估报告'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `consultation`
-- >>> STATEMENT <<<
CREATE TABLE `consultation` (
  `consultation_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `patient_id` bigint unsigned NOT NULL COMMENT '患者用户ID',
  `therapist_id` bigint unsigned NOT NULL COMMENT '治疗师用户ID',
  `binding_id` bigint unsigned NOT NULL COMMENT '患者治疗师绑定ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态：OPEN/CLOSED',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `closed_at` datetime(3) DEFAULT NULL COMMENT '关闭时间',
  PRIMARY KEY (`consultation_id`),
  KEY `idx_consult_patient` (`patient_id`,`status`),
  KEY `idx_consult_therapist` (`therapist_id`,`status`),
  KEY `idx_consult_binding` (`binding_id`),
  KEY `fk_consult_binding_pair` (`binding_id`,`patient_id`,`therapist_id`),
  CONSTRAINT `fk_consult_binding_pair` FOREIGN KEY (`binding_id`, `patient_id`, `therapist_id`) REFERENCES `patient_therapist` (`binding_id`, `patient_id`, `therapist_id`),
  CONSTRAINT `fk_consult_patient` FOREIGN KEY (`patient_id`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `fk_consult_therapist` FOREIGN KEY (`therapist_id`) REFERENCES `user_account` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='咨询会话'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `consultation_attachment`
-- >>> STATEMENT <<<
CREATE TABLE `consultation_attachment` (
  `consultation_attachment_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '消息附件ID',
  `message_id` bigint unsigned NOT NULL COMMENT '消息ID',
  `file_id` bigint unsigned NOT NULL COMMENT '文件ID',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`consultation_attachment_id`),
  UNIQUE KEY `uk_message_file` (`message_id`,`file_id`),
  KEY `fk_ma_file` (`file_id`),
  CONSTRAINT `fk_ma_file` FOREIGN KEY (`file_id`) REFERENCES `file_object` (`file_id`),
  CONSTRAINT `fk_ma_message` FOREIGN KEY (`message_id`) REFERENCES `consultation_message` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='咨询消息附件'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `consultation_message`
-- >>> STATEMENT <<<
CREATE TABLE `consultation_message` (
  `message_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `consultation_id` bigint unsigned NOT NULL COMMENT '会话ID',
  `sender_id` bigint unsigned NOT NULL COMMENT '发送人用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '文本内容（纯附件消息可为空）',
  `is_ai_draft` tinyint NOT NULL DEFAULT '0' COMMENT '是否AI草稿 1是0否',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读 1是0否',
  `read_at` datetime(3) DEFAULT NULL COMMENT '阅读时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发送时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_message_consult_time` (`consultation_id`,`created_at`),
  KEY `idx_message_unread` (`sender_id`,`is_read`),
  CONSTRAINT `fk_message_consult` FOREIGN KEY (`consultation_id`) REFERENCES `consultation` (`consultation_id`),
  CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user_account` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='咨询消息'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `content`
-- >>> STATEMENT <<<
CREATE TABLE `content` (
  `content_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '内容ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `content_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容类型：ARTICLE文章/VIDEO视频/GUIDE指南',
  `target_part_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关联康复部位编码',
  `target_group` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标人群标签',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '摘要',
  `content_body` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '正文',
  `author_id` bigint unsigned NOT NULL COMMENT '作者用户ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态：DRAFT/PENDING_REVIEW/PUBLISHED/OFFLINE',
  `reviewer_id` bigint unsigned DEFAULT NULL COMMENT '审核人用户ID',
  `review_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核意见',
  `valid_from` datetime(3) NOT NULL COMMENT '生效时间',
  `valid_to` datetime(3) DEFAULT NULL COMMENT '失效时间',
  `published_at` datetime(3) DEFAULT NULL COMMENT '发布时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (`content_id`),
  KEY `idx_content_filter` (`status`,`target_part_code`,`valid_from`,`valid_to`),
  KEY `fk_content_part` (`target_part_code`),
  KEY `fk_content_author` (`author_id`),
  KEY `fk_content_reviewer` (`reviewer_id`),
  CONSTRAINT `fk_content_author` FOREIGN KEY (`author_id`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `fk_content_part` FOREIGN KEY (`target_part_code`) REFERENCES `rehab_part_dict` (`code`),
  CONSTRAINT `fk_content_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `chk_content_valid_period` CHECK (((`valid_to` is null) or (`valid_to` >= `valid_from`)))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公共健康内容'
-- >>> STATEMENT <<<
INSERT INTO `content` (`content_id`,`title`,`content_type`,`target_part_code`,`target_group`,`summary`,`content_body`,`author_id`,`status`,`reviewer_id`,`review_remark`,`valid_from`,`valid_to`,`published_at`,`created_at`,`updated_at`,`deleted`) VALUES (1,'膝关节术后居家训练指南 - REVISED','ARTICLE','KNEE','术后0-6周患者','术后康复训练指导，含动作演示与注意事项。','请按医嘱完成每日训练，出现疼痛立即停止。',1,'OFFLINE',1,'内容已过期，统一下架','2026-09-26T20:43:37.489',NULL,'2026-09-26T20:43:38.181','2026-09-26T20:43:36.879','2026-09-26T21:32:04.927',1),(2,'draft-to-delete','GUIDE','LUMBAR',NULL,'s','b',1,'DRAFT',NULL,NULL,'2026-09-26T20:43:38.852',NULL,NULL,'2026-09-26T20:43:38.860','2026-09-26T20:43:38.926',1),(3,'膝关节术后居家训练指南 - REVISED','ARTICLE','KNEE','术后0-6周患者','术后康复训练指导，含动作演示与注意事项。','请按医嘱完成每日训练，出现疼痛立即停止。',1,'OFFLINE',1,'内容已过期，统一下架','2026-09-26T20:48:52.120',NULL,'2026-09-26T20:48:52.878','2026-09-26T20:48:51.952','2026-09-26T21:32:04.861',1),(4,'draft-to-delete','GUIDE','LUMBAR',NULL,'s','b',1,'DRAFT',NULL,NULL,'2026-09-26T20:48:53.589',NULL,NULL,'2026-09-26T20:48:54.054','2026-09-26T20:48:54.127',1),(5,'肩周居家训练-浏览器走查-修订','ARTICLE','SHOULDER','肩周炎患者','浏览器走查摘要','这是一个关于肩周炎患者居家康复训练的测试内容，包含多个康复动作和注意事项，用于验证系统的功能完整性。',1,'OFFLINE',1,NULL,'2026-09-26T21:13:17.767',NULL,'2026-09-26T21:13:58.513','2026-09-26T21:04:31.408','2026-09-26T21:32:04.790',1),(6,'回归测试内容','ARTICLE','SHOULDER',NULL,'regression','body',1,'OFFLINE',1,NULL,'2026-09-26T21:32:04.641',NULL,'2026-09-26T21:32:51.225','2026-09-26T21:32:05.143','2026-09-26T21:32:51.930',1)
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `content_attachment`
-- >>> STATEMENT <<<
CREATE TABLE `content_attachment` (
  `content_attachment_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '附件关联ID',
  `content_id` bigint unsigned NOT NULL COMMENT '内容ID',
  `file_id` bigint unsigned NOT NULL COMMENT '文件ID',
  `attachment_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '附件类型：COVER封面/VIDEO视频/IMAGE图片/DOC文档',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`content_attachment_id`),
  UNIQUE KEY `uk_content_file` (`content_id`,`file_id`),
  KEY `fk_ca_file` (`file_id`),
  CONSTRAINT `fk_ca_content` FOREIGN KEY (`content_id`) REFERENCES `content` (`content_id`),
  CONSTRAINT `fk_ca_file` FOREIGN KEY (`file_id`) REFERENCES `file_object` (`file_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容附件'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `content_collection`
-- >>> STATEMENT <<<
CREATE TABLE `content_collection` (
  `collection_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `content_id` bigint unsigned NOT NULL COMMENT '内容ID',
  `collected_at` datetime(3) NOT NULL COMMENT '收藏时间',
  PRIMARY KEY (`collection_id`),
  UNIQUE KEY `uk_collection_user_content` (`user_id`,`content_id`),
  KEY `fk_collection_content` (`content_id`),
  CONSTRAINT `fk_collection_content` FOREIGN KEY (`content_id`) REFERENCES `content` (`content_id`),
  CONSTRAINT `fk_collection_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容收藏'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `db_backup_task`
-- >>> STATEMENT <<<
CREATE TABLE `db_backup_task` (
  `backup_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '备份任务ID',
  `task_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务类型：BACKUP 手工备份；PRE_RESTORE_SNAPSHOT 恢复前自动快照',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务状态：RUNNING 执行中 / SUCCESS 成功 / FAILED 失败',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备份产物绝对路径（.sql）',
  `file_size` bigint unsigned NOT NULL DEFAULT '0' COMMENT '备份文件大小（字节）',
  `sha256` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备份文件 SHA-256 摘要',
  `error_msg` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因（成功为 NULL）',
  `triggered_by` bigint unsigned DEFAULT NULL COMMENT '触发操作人用户ID（系统任务为 NULL）',
  `related_backup_id` bigint unsigned DEFAULT NULL COMMENT '关联备份ID（恢复前快照指向被恢复的备份）',
  `restore_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '恢复原因（仅 PRE_RESTORE_SNAPSHOT 记录）',
  `started_at` datetime(3) NOT NULL COMMENT '任务开始时间',
  `finished_at` datetime(3) DEFAULT NULL COMMENT '任务结束时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`backup_id`),
  KEY `idx_backup_status` (`status`,`started_at`),
  KEY `fk_backup_operator` (`triggered_by`),
  KEY `fk_backup_related` (`related_backup_id`),
  CONSTRAINT `fk_backup_operator` FOREIGN KEY (`triggered_by`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `fk_backup_related` FOREIGN KEY (`related_backup_id`) REFERENCES `db_backup_task` (`backup_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据库备份恢复任务'
-- >>> STATEMENT <<<
INSERT INTO `db_backup_task` (`backup_id`,`task_type`,`status`,`file_path`,`file_size`,`sha256`,`error_msg`,`triggered_by`,`related_backup_id`,`restore_reason`,`started_at`,`finished_at`,`created_at`) VALUES (1,'BACKUP','RUNNING',NULL,0,NULL,NULL,1,NULL,NULL,'2026-09-26T22:56:40.814',NULL,'2026-09-26T22:56:41.108'),(2,'BACKUP','RUNNING',NULL,0,NULL,NULL,1,NULL,NULL,'2026-09-26T23:13:19.524',NULL,'2026-09-26T23:13:19.764')
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `file_object`
-- >>> STATEMENT <<<
CREATE TABLE `file_object` (
  `file_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `bucket_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MinIO存储桶',
  `object_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '对象键',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `content_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MIME类型',
  `file_size` bigint unsigned NOT NULL COMMENT '文件大小(字节)',
  `sha256` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SHA-256摘要',
  `purpose` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用途：TEMPLATE样本/ATTACHMENT附件/MESSAGE消息/AVATAR头像',
  `scan_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '安全扫描状态：PENDING/PASSED/FAILED',
  `created_by` bigint unsigned NOT NULL COMMENT '上传人用户ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `deleted_at` datetime(3) DEFAULT NULL COMMENT '删除时间（NULL未删）',
  PRIMARY KEY (`file_id`),
  UNIQUE KEY `uk_file_bucket_object` (`bucket_name`,`object_key`),
  KEY `idx_file_purpose` (`purpose`,`scan_status`),
  KEY `fk_file_creator` (`created_by`),
  CONSTRAINT `fk_file_creator` FOREIGN KEY (`created_by`) REFERENCES `user_account` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MinIO文件对象'
-- >>> STATEMENT <<<
INSERT INTO `file_object` (`file_id`,`bucket_name`,`object_key`,`file_name`,`content_type`,`file_size`,`sha256`,`purpose`,`scan_status`,`created_by`,`created_at`,`deleted_at`) VALUES (1,'rehab','content/61953a53ab5d4e77a9574f21bb6aa60a.pdf','M3-guide.pdf','application/pdf',39,'e377a0b561c527dad0e160c0b5751935bb975a8babd262ad69fee742e1847718','ATTACHMENT','PASSED',1,'2026-09-26T20:38:48.378','2026-09-26T20:38:48.584'),(2,'rehab','content/1690fd3c268847d799eccc46814df5a7.png','cover.png','image/png',39,'e377a0b561c527dad0e160c0b5751935bb975a8babd262ad69fee742e1847718','ATTACHMENT','PASSED',1,'2026-09-26T20:38:48.811',NULL),(3,'rehab','content/a3906048f4324f4baeee6b88bb3faa44.jpg','IMG_0341.JPG','image/jpeg',7597097,'e15c07664c28f2a9e56c53a8086f7107b050631433bf11c2baf1fc64f23b3992','ATTACHMENT','PASSED',1,'2026-09-26T21:03:43.893',NULL)
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `operation_log`
-- >>> STATEMENT <<<
CREATE TABLE `operation_log` (
  `log_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint unsigned DEFAULT NULL COMMENT '操作人用户ID（NULL为系统操作）',
  `module` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块名',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型',
  `target_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标对象类型',
  `target_id` bigint unsigned DEFAULT NULL COMMENT '目标对象ID',
  `ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '客户端IP（兼容IPv6）',
  `device_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备标识',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '结果：SUCCESS/FAIL',
  `detail` json DEFAULT NULL COMMENT '操作详情（参数、变更前后值等）',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_log_user_time` (`user_id`,`created_at`),
  KEY `idx_log_module_time` (`module`,`created_at`),
  CONSTRAINT `fk_log_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=243 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志'
-- >>> STATEMENT <<<
INSERT INTO `operation_log` (`log_id`,`user_id`,`module`,`action`,`target_type`,`target_id`,`ip`,`device_id`,`status`,`detail`,`created_at`) VALUES (1,NULL,'AUTH','LOGIN','USER_ACCOUNT',NULL,'127.0.0.1',NULL,'FAIL','{\"message\": \"用户名或密码错误，第 1 次\"}','2026-09-24T18:15:11.494'),(2,NULL,'AUTH','LOGIN','USER_ACCOUNT',NULL,'127.0.0.1',NULL,'FAIL','{\"message\": \"用户名或密码错误，第 1 次\"}','2026-09-25T12:05:33.483'),(3,NULL,'AUTH','LOGIN','USER_ACCOUNT',NULL,'127.0.0.1',NULL,'FAIL','{\"message\": \"用户名或密码错误，第 2 次\"}','2026-09-25T12:05:33.597'),(4,NULL,'AUTH','LOGIN','USER_ACCOUNT',NULL,'127.0.0.1',NULL,'FAIL','{\"message\": \"用户名或密码错误，第 3 次\"}','2026-09-25T12:08:14.480'),(5,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'127.0.0.1',NULL,'SUCCESS',NULL,'2026-09-25T12:10:24.699'),(6,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'127.0.0.1',NULL,'SUCCESS',NULL,'2026-09-25T14:59:04.787'),(7,1,'DICT','DELETE','REHAB_PART_DICT',NULL,'127.0.0.1',NULL,'SUCCESS',NULL,'2026-09-25T14:59:05.180'),(8,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T14:15:26.701'),(14,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T14:22:58.173'),(37,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T14:23:36.520'),(60,NULL,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"message\": \"用户名或密码错误，第 1 次\"}','2026-09-26T14:38:44.915'),(61,NULL,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"message\": \"用户名或密码错误，第 1 次\"}','2026-09-26T14:39:49.422'),(62,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T14:41:23.790'),(63,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T14:41:54.738'),(64,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T14:44:20.381'),(65,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T14:59:42.434'),(66,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T15:13:34.324'),(67,1,'USER','CREATE','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"phone\": \"13900001668\", \"gender\": \"MALE\", \"realName\": \"fepatient01\", \"roleCode\": \"PATIENT\", \"username\": \"fepatient01\", \"birthDate\": 631123200000}}','2026-09-26T15:13:34.698'),(68,1,'USER','CREATE','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"phone\": \"13900009668\", \"title\": \"主治医师\", \"realName\": \"fethera01\", \"roleCode\": \"THERAPIST\", \"username\": \"fethera01\", \"specialty\": \"运动康复\"}}','2026-09-26T15:13:34.797'),(69,1,'BINDING','CREATE','PATIENT_THERAPIST',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"endAt\": 1798732799000, \"startAt\": 1790407800000, \"patientId\": 11, \"therapistId\": 12, \"relationType\": \"PRIMARY\"}}','2026-09-26T15:13:34.848'),(70,1,'USER','CREATE','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"phone\": \"13900003700\", \"title\": \"医师\", \"realName\": \"fethera02\", \"roleCode\": \"THERAPIST\", \"username\": \"fethera02\", \"specialty\": \"颈肩\"}}','2026-09-26T15:13:34.982'),(71,1,'BINDING','UNBIND','PATIENT_THERAPIST',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Check constraint \'chk_binding_period\' is violated.\\r\\n### The error may exist in com/rehab/app/mapper/PatientTherapistMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.PatientTherapistMapper.updateById-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: UPDATE patient_therapist  SET patient_id=?, therapist_id=?, relation_type=?, status=?, start_at=?, end_at=?, created_by=?, created_at=?  WHERE binding_id=?\\r\\n### Cause: java.sql.SQLException: Check constraint \'chk_binding_period\' is violated.\\n; uncategorized SQLException; SQL state [HY000]; error code [3819]; Check constraint \'chk_binding_period\' is violated.\"}','2026-09-26T15:13:35.215'),(72,NULL,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"message\": \"用户名或密码错误，第 1 次\"}','2026-09-26T15:23:23.795'),(73,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T15:23:58.366'),(86,NULL,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"message\": \"用户名或密码错误，第 1 次\"}','2026-09-26T16:41:17.601'),(87,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T16:41:39.959'),(88,1,'ACTION','CREATE','ACTION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"name\": \"膝关节屈伸\", \"difficulty\": \"EASY\", \"description\": \"测试动作\", \"targetPartCode\": \"KNEE\"}}','2026-09-26T16:42:18.055'),(89,1,'ACTION','UPDATE','ACTION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"name\": \"膝关节屈伸\", \"difficulty\": \"EASY\", \"description\": \"测试动作更新\", \"targetPartCode\": \"KNEE\"}, \"actionId\": 1}','2026-09-26T16:42:36.744'),(90,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:43:33.799'),(91,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:43:42.947'),(92,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:43:54.111'),(93,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:44:06.657'),(94,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:44:18.812'),(95,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:44:28.155'),(96,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:44:45.325'),(97,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:47:11.432'),(98,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:47:31.838'),(99,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:47:49.345'),(100,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:49:05.831'),(101,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:49:13.781'),(102,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:49:25.952'),(103,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"\\r\\n### Error updating database.  Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\r\\n### The error may exist in com/rehab/app/mapper/ActionVersionMapper.java (best guess)\\r\\n### The error may involve com.rehab.app.mapper.ActionVersionMapper.insert-Inline\\r\\n### The error occurred while setting parameters\\r\\n### SQL: INSERT INTO action_version  ( action_id, version_no, action_type,   pose_engine_id, status, created_by )  VALUES (  ?, ?, ?,   ?, ?, ?  )\\r\\n### Cause: java.sql.SQLException: Field \'algo_config\' doesn\'t have a default value\\n; Field \'algo_config\' doesn\'t have a default value\"}','2026-09-26T16:49:46.499'),(104,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T16:56:57.849'),(105,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T16:59:05.194'),(106,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T16:59:23.311'),(107,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:00:00.334'),(108,1,'ACTION','CREATE','ACTION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"name\": \"?????\", \"difficulty\": \"EASY\", \"description\": \"API????\", \"targetPartCode\": \"KNEE\"}}','2026-09-26T17:00:00.477'),(109,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"actionType\": \"ROM\", \"algoConfig\": \"{\\\"fps\\\":30}\", \"poseEngineId\": \"movenet-v1\", \"motionSignature\": \"{\\\"keypoints\\\":17}\"}, \"actionId\": 2}','2026-09-26T17:00:00.515'),(110,1,'ACTION','CREATE','ACTION_RULE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"phase\": \"FULL\", \"maxAngle\": 135, \"minAngle\": 0, \"severity\": 1, \"dimension\": \"ANGLE\", \"jointName\": \"left_knee\", \"jointRole\": \"PRIMARY\", \"promptText\": \"???????\", \"scoreWeight\": 0.6, \"standardAngle\": 90}, \"versionId\": 1}','2026-09-26T17:00:00.581'),(111,1,'ACTION','CREATE','ACTION_RULE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"phase\": \"HOLD\", \"severity\": 1, \"dimension\": \"STABILITY\", \"jointName\": \"right_knee\", \"jointRole\": \"AUXILIARY\", \"promptText\": \"????\", \"scoreWeight\": 0.4}, \"versionId\": 1}','2026-09-26T17:00:00.605'),(112,1,'ACTION','CREATE','ACTION_TEMPLATE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"frameRate\": 30, \"featureDim\": 51, \"frameCount\": 300, \"sourceType\": \"THERAPIST\", \"sampleCount\": 1, \"keyframeIndex\": \"[0,30]\", \"featureSequence\": \"[[0.1,0.2]]\"}, \"versionId\": 1}','2026-09-26T17:00:00.642'),(113,1,'ACTION','PUBLISH','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:00:00.675'),(114,1,'ACTION','UPDATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"已发布版本不可修改，当前状态：PUBLISHED\"}','2026-09-26T17:00:00.696'),(115,1,'ACTION','PUBLISH','ACTION_TEMPLATE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:00:00.736'),(116,1,'ACTION','DELETE','ACTION_TEMPLATE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:00:00.759'),(117,1,'ACTION','OFFLINE','ACTION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:00:00.777'),(118,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:04:52.416'),(119,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:46:55.584'),(120,1,'ACTION','CREATE','ACTION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"name\": \"M2????????\", \"difficulty\": \"EASY\", \"description\": \"API verify\", \"targetPartCode\": \"SHOULDER\"}}','2026-09-26T17:46:55.734'),(121,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"算法配置不是合法 JSON\"}','2026-09-26T17:46:55.762'),(122,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:47:47.431'),(123,1,'ACTION','OFFLINE','ACTION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:47:47.457'),(124,1,'ACTION','CREATE','ACTION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"name\": \"M2????????\", \"difficulty\": \"EASY\", \"description\": \"API verify\", \"targetPartCode\": \"SHOULDER\"}}','2026-09-26T17:47:47.478'),(125,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"算法配置不是合法 JSON\"}','2026-09-26T17:47:47.497'),(126,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:50:02.077'),(127,1,'ACTION','OFFLINE','ACTION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:50:02.154'),(128,1,'ACTION','OFFLINE','ACTION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:50:02.214'),(129,1,'ACTION','CREATE','ACTION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"name\": \"M2补全自动测试动作\", \"difficulty\": \"EASY\", \"description\": \"x\", \"targetPartCode\": \"SHOULDER\"}}','2026-09-26T17:50:02.279'),(130,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"算法配置不是合法 JSON\"}','2026-09-26T17:50:02.342'),(131,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"运动签名不是合法 JSON\"}','2026-09-26T17:50:02.410'),(132,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"actionType\": \"ROM\", \"algoConfig\": \"{\\\"weightAmplitude\\\":0.3,\\\"boneLenTolerance\\\":0.15,\\\"weightRhythm\\\":0.15,\\\"repairRatioLimit\\\":0.3,\\\"weightSymmetry\\\":0.1,\\\"confHigh\\\":0.5,\\\"medianWindow\\\":3,\\\"confLow\\\":0.2,\\\"timeWeightSigma\\\":0.2,\\\"emaAlpha\\\":0.35,\\\"smoothWindowAccel\\\":11,\\\"validFrameRatioMin\\\":0.7,\\\"minCycleSec\\\":1.2,\\\"weightSimilarity\\\":0.1,\\\"weightStability\\\":0.15,\\\"smoothWindowSpeed\\\":7,\\\"poseEngineId\\\":\\\"movenet-lightning-web@v1\\\",\\\"weightHold\\\":0.2,\\\"maxGapFrames\\\":5,\\\"hysteresisBand\\\":8,\\\"stateHoldFrames\\\":3}\", \"poseEngineId\": \"movenet-lightning-web@v1\", \"motionSignature\": \"{\\\"expectedDirection\\\":\\\"EXTEND\\\",\\\"phaseOrder\\\":[\\\"READY\\\",\\\"RAISING\\\",\\\"HOLD\\\",\\\"LOWERING\\\"],\\\"startAngleRange\\\":[0,30],\\\"minRom\\\":60,\\\"peakAngleRange\\\":[70,110],\\\"targetJoints\\\":[\\\"左肩\\\"]}\"}, \"actionId\": 5}','2026-09-26T17:50:02.524'),(133,1,'ACTION','CREATE','ACTION_TEMPLATE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"frameRate\": 30, \"featureDim\": 2, \"frameCount\": 90, \"sourceType\": \"THERAPIST\", \"sampleCount\": 3, \"keyframeIndex\": \"[0,45,89]\", \"featureSequence\": \"[[0.01,0.12],[0.03,0.22],[0.05,0.41]]\"}, \"versionId\": 2}','2026-09-26T17:50:02.613'),(134,1,'ACTION','PUBLISH','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"发布失败：规则权重和必须等于 1，当前为 0.0000\"}','2026-09-26T17:50:02.764'),(135,1,'ACTION','CREATE','ACTION_RULE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"phase\": \"FULL\", \"severity\": 2, \"dimension\": \"ANGLE\", \"jointName\": \"左肩\", \"jointRole\": \"PRIMARY\", \"promptText\": \"请抬高左臂\", \"scoreWeight\": 1, \"angleFormula\": \"angle(6,8,10)\", \"standardAngle\": 90, \"toleranceHard\": 15, \"toleranceSoft\": 8, \"keypointIndices\": \"[6,8,10]\"}, \"versionId\": 2}','2026-09-26T17:50:02.853'),(136,1,'ACTION','PUBLISH','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:50:02.919'),(137,1,'ACTION','UPDATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"已发布版本不可修改，当前状态：PUBLISHED\"}','2026-09-26T17:50:02.982'),(138,1,'ACTION','DELETE','ACTION_TEMPLATE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:50:03.065'),(139,1,'ACTION','OFFLINE','ACTION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:50:03.128'),(140,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:50:36.361'),(141,1,'ACTION','CREATE','ACTION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"name\": \"probe\", \"difficulty\": \"EASY\", \"targetPartCode\": \"SHOULDER\"}}','2026-09-26T17:50:36.438'),(142,1,'ACTION','CREATE','ACTION_VERSION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"actionType\": \"ROM\", \"poseEngineId\": \"e@v1\"}, \"actionId\": 6}','2026-09-26T17:50:36.502'),(143,1,'ACTION','CREATE','ACTION_TEMPLATE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"sourceType\": \"ALGO\", \"keyframeIndex\": \"[0,45,89]\", \"featureSequence\": \"[[0.01,0.12],[0.03,0.22]]\"}, \"versionId\": 3}','2026-09-26T17:50:36.565'),(144,1,'ACTION','DELETE','ACTION_TEMPLATE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:50:36.687'),(145,1,'ACTION','OFFLINE','ACTION',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T17:50:36.747'),(146,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T20:38:47.307'),(147,1,'FILE','PRESIGN','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"文件类型 text/plain 不在业务附件用途允许范围 [application/pdf, image/png, image/jpeg, video/mp4, image/webp, image/gif] 内\"}','2026-09-26T20:38:47.442'),(148,1,'FILE','PRESIGN','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"文件大小 105906176 字节超过业务附件用途允许上限 104857600 字节\"}','2026-09-26T20:38:47.523'),(149,1,'FILE','PRESIGN','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"purpose\": \"ATTACHMENT\", \"fileName\": \"M3-guide.pdf\", \"fileSize\": 39, \"contentType\": \"application/pdf\"}}','2026-09-26T20:38:47.953'),(150,1,'FILE','COMPLETE','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"文件 SHA-256 校验不一致，已拒绝入库\"}','2026-09-26T20:38:48.146'),(151,1,'FILE','COMPLETE','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"上传会话不存在或已过期，请重新申请上传地址\"}','2026-09-26T20:38:48.207'),(152,1,'FILE','PRESIGN','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"purpose\": \"ATTACHMENT\", \"fileName\": \"M3-guide.pdf\", \"fileSize\": 39, \"contentType\": \"application/pdf\"}}','2026-09-26T20:38:48.269'),(153,1,'FILE','COMPLETE','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"sha256\": \"e377a0b561c527dad0e160c0b5751935bb975a8babd262ad69fee742e1847718\"}, \"uploadId\": \"61953a53ab5d4e77a9574f21bb6aa60a\"}','2026-09-26T20:38:48.395'),(154,1,'FILE','DELETE','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T20:38:48.593'),(155,1,'FILE','PRESIGN','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"purpose\": \"ATTACHMENT\", \"fileName\": \"cover.png\", \"fileSize\": 39, \"contentType\": \"image/png\"}}','2026-09-26T20:38:48.705'),(156,1,'FILE','COMPLETE','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"sha256\": \"e377a0b561c527dad0e160c0b5751935bb975a8babd262ad69fee742e1847718\"}, \"uploadId\": \"1690fd3c268847d799eccc46814df5a7\"}','2026-09-26T20:38:48.821'),(157,1,'FILE','COMPLETE','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"上传会话不存在或已过期，请重新申请上传地址\"}','2026-09-26T20:38:48.876'),(158,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T20:43:36.453'),(159,1,'CONTENT','CREATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"关联康复部位不能为空\"}','2026-09-26T20:43:36.651'),(160,1,'CONTENT','CREATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"关联康复部位编码不存在：NOTEXIST\"}','2026-09-26T20:43:36.720'),(161,1,'CONTENT','CREATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"title\": \"膝关节术后居家训练指南\", \"summary\": \"术后康复训练指导，含动作演示与注意事项。\", \"attachments\": [{\"fileId\": 2, \"sortNo\": 0, \"attachmentType\": \"COVER\"}], \"contentBody\": \"请按医嘱完成每日训练，出现疼痛立即停止。\", \"contentType\": \"ARTICLE\", \"targetGroup\": \"术后0-6周患者\", \"targetPartCode\": \"KNEE\"}}','2026-09-26T20:43:36.907'),(162,1,'FILE','DELETE','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"文件已被 1 条公共内容附件引用，请先移除引用\"}','2026-09-26T20:43:37.224'),(163,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"驳回必须填写理由\"}','2026-09-26T20:43:37.290'),(164,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"comment\": \"封面图片与正文不符，请重新上传\", \"decision\": \"REJECT\"}, \"contentId\": 1}','2026-09-26T20:43:37.362'),(165,1,'CONTENT','UPDATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"title\": \"膝关节术后居家训练指南 - REVISED\", \"summary\": \"术后康复训练指导，含动作演示与注意事项。\", \"attachments\": [{\"fileId\": 2, \"sortNo\": 0, \"attachmentType\": \"COVER\"}], \"contentBody\": \"请按医嘱完成每日训练，出现疼痛立即停止。\", \"contentType\": \"ARTICLE\", \"targetGroup\": \"术后0-6周患者\", \"targetPartCode\": \"KNEE\"}, \"contentId\": 1}','2026-09-26T20:43:37.512'),(166,1,'USER','CREATE','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"创建患者账号时性别不能为空\"}','2026-09-26T20:43:37.732'),(167,NULL,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"message\": \"用户名或密码错误，第 1 次\"}','2026-09-26T20:43:38.030'),(168,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"comment\": \"ok\", \"decision\": \"APPROVE\"}, \"contentId\": 1}','2026-09-26T20:43:38.194'),(169,1,'CONTENT','UPDATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"仅草稿状态的内容可编辑，当前状态：已发布\"}','2026-09-26T20:43:38.410'),(170,1,'CONTENT','DELETE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"仅未发布草稿可删除，已发布内容请使用下架，当前状态：PUBLISHED\"}','2026-09-26T20:43:38.465'),(171,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"仅草稿或待审核内容可驳回，当前状态：PUBLISHED\"}','2026-09-26T20:43:38.535'),(172,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"comment\": \"内容已过期，统一下架\", \"decision\": \"OFFLINE\"}, \"contentId\": 1}','2026-09-26T20:43:38.603'),(173,1,'CONTENT','CREATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"title\": \"draft-to-delete\", \"summary\": \"s\", \"contentBody\": \"b\", \"contentType\": \"GUIDE\", \"targetPartCode\": \"LUMBAR\"}}','2026-09-26T20:43:38.866'),(174,1,'CONTENT','DELETE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T20:43:38.934'),(175,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T20:48:51.344'),(176,1,'CONTENT','CREATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"关联康复部位不能为空\"}','2026-09-26T20:48:51.731'),(177,1,'CONTENT','CREATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"关联康复部位编码不存在：NOTEXIST\"}','2026-09-26T20:48:51.799'),(178,1,'CONTENT','CREATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"失效时间不能早于生效时间\"}','2026-09-26T20:48:51.859'),(179,1,'CONTENT','CREATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"title\": \"膝关节术后居家训练指南\", \"summary\": \"术后康复训练指导，含动作演示与注意事项。\", \"attachments\": [{\"fileId\": 2, \"sortNo\": 0, \"attachmentType\": \"COVER\"}], \"contentBody\": \"请按医嘱完成每日训练，出现疼痛立即停止。\", \"contentType\": \"ARTICLE\", \"targetGroup\": \"术后0-6周患者\", \"targetPartCode\": \"KNEE\"}}','2026-09-26T20:48:52.019'),(180,1,'FILE','DELETE','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"文件已被 2 条公共内容附件引用，请先移除引用\"}','2026-09-26T20:48:52.321'),(181,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"驳回必须填写理由\"}','2026-09-26T20:48:52.389'),(182,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"comment\": \"封面图片与正文不符，请重新上传\", \"decision\": \"REJECT\"}, \"contentId\": 3}','2026-09-26T20:48:52.456'),(183,1,'CONTENT','UPDATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"title\": \"膝关节术后居家训练指南 - REVISED\", \"summary\": \"术后康复训练指导，含动作演示与注意事项。\", \"attachments\": [{\"fileId\": 2, \"sortNo\": 0, \"attachmentType\": \"COVER\"}], \"contentBody\": \"请按医嘱完成每日训练，出现疼痛立即停止。\", \"contentType\": \"ARTICLE\", \"targetGroup\": \"术后0-6周患者\", \"targetPartCode\": \"KNEE\"}, \"contentId\": 3}','2026-09-26T20:48:52.597'),(184,1,'USER','CREATE','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"gender\": \"MALE\", \"realName\": \"M3Pat\", \"roleCode\": \"PATIENT\", \"username\": \"m3pat01\", \"birthDate\": 631123200000}}','2026-09-26T20:48:52.805'),(185,17,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T20:48:53.161'),(186,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"comment\": \"ok\", \"decision\": \"APPROVE\"}, \"contentId\": 3}','2026-09-26T20:48:53.348'),(187,1,'CONTENT','UPDATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"仅草稿状态的内容可编辑，当前状态：已发布\"}','2026-09-26T20:48:53.611'),(188,1,'CONTENT','DELETE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"仅未发布草稿可删除，已发布内容请使用下架，当前状态：PUBLISHED\"}','2026-09-26T20:48:53.667'),(189,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"仅草稿或待审核内容可驳回，当前状态：PUBLISHED\"}','2026-09-26T20:48:53.726'),(190,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"comment\": \"内容已过期，统一下架\", \"decision\": \"OFFLINE\"}, \"contentId\": 3}','2026-09-26T20:48:53.789'),(191,1,'CONTENT','CREATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"title\": \"draft-to-delete\", \"summary\": \"s\", \"contentBody\": \"b\", \"contentType\": \"GUIDE\", \"targetPartCode\": \"LUMBAR\"}}','2026-09-26T20:48:54.059'),(192,1,'CONTENT','DELETE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T20:48:54.130'),(193,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T21:01:50.288'),(194,1,'FILE','PRESIGN','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"purpose\": \"ATTACHMENT\", \"fileName\": \"IMG_0341.JPG\", \"fileSize\": 7597097, \"contentType\": \"image/jpeg\"}}','2026-09-26T21:03:44.125'),(195,1,'FILE','COMPLETE','FILE',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"sha256\": \"e15c07664c28f2a9e56c53a8086f7107b050631433bf11c2baf1fc64f23b3992\"}, \"uploadId\": \"a3906048f4324f4baeee6b88bb3faa44\"}','2026-09-26T21:03:44.413'),(196,1,'CONTENT','CREATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"title\": \"肩周居家训练-浏览器走查\", \"summary\": \"浏览器走查摘要\", \"attachments\": [{\"fileId\": 3, \"sortNo\": 0, \"attachmentType\": \"IMAGE\"}], \"contentBody\": \"这是一个关于肩周炎患者居家康复训练的测试内容，包含多个康复动作和注意事项，用于验证系统的功能完整性。\", \"contentType\": \"ARTICLE\", \"targetGroup\": \"肩周炎患者\", \"targetPartCode\": \"SHOULDER\"}}','2026-09-26T21:04:31.420'),(197,1,'CONTENT','UPDATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"title\": \"肩周居家训练-浏览器走查\", \"summary\": \"浏览器走查摘要\", \"attachments\": [{\"fileId\": 3, \"sortNo\": 0, \"attachmentType\": \"IMAGE\"}], \"contentBody\": \"这是一个关于肩周炎患者居家康复训练的测试内容，包含多个康复动作和注意事项，用于验证系统的功能完整性。\", \"contentType\": \"ARTICLE\", \"targetGroup\": \"肩周炎患者\", \"targetPartCode\": \"SHOULDER\"}, \"contentId\": 5}','2026-09-26T21:06:33.375'),(198,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"comment\": \"浏览器走查驳回测试：封面图需替换\", \"decision\": \"REJECT\"}, \"contentId\": 5}','2026-09-26T21:12:23.045'),(199,1,'CONTENT','UPDATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"title\": \"肩周居家训练-浏览器走查-修订\", \"summary\": \"浏览器走查摘要\", \"attachments\": [{\"fileId\": 3, \"sortNo\": 0, \"attachmentType\": \"IMAGE\"}], \"contentBody\": \"这是一个关于肩周炎患者居家康复训练的测试内容，包含多个康复动作和注意事项，用于验证系统的功能完整性。\", \"contentType\": \"ARTICLE\", \"targetGroup\": \"肩周炎患者\", \"targetPartCode\": \"SHOULDER\"}, \"contentId\": 5}','2026-09-26T21:13:18.268'),(200,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"decision\": \"APPROVE\"}, \"contentId\": 5}','2026-09-26T21:13:59.009'),(201,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"decision\": \"OFFLINE\"}, \"contentId\": 5}','2026-09-26T21:14:49.562'),(202,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T21:32:04.500'),(203,1,'CONTENT','DELETE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T21:32:04.796'),(204,1,'CONTENT','DELETE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T21:32:04.866'),(205,1,'CONTENT','DELETE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T21:32:04.932'),(206,1,'CONTENT','CREATE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"title\": \"回归测试内容\", \"summary\": \"regression\", \"contentBody\": \"body\", \"contentType\": \"ARTICLE\", \"targetPartCode\": \"SHOULDER\"}}','2026-09-26T21:32:05.208'),(207,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T21:32:51.562'),(208,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"decision\": \"APPROVE\"}, \"contentId\": 6}','2026-09-26T21:32:51.740'),(209,1,'CONTENT','DELETE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"已发布内容请先下架再删除，当前状态：PUBLISHED\"}','2026-09-26T21:32:51.804'),(210,1,'CONTENT','AUDIT','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"decision\": \"OFFLINE\"}, \"contentId\": 6}','2026-09-26T21:32:51.870'),(211,1,'CONTENT','DELETE','CONTENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T21:32:51.935'),(212,17,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T21:32:52.337'),(213,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T22:07:48.161'),(214,17,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T22:07:48.546'),(215,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T22:56:38.286'),(216,17,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T22:56:38.660'),(217,1,'NOTICE','CREATE','ANNOUNCEMENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"scope\": \"THERAPIST\", \"title\": \"therapist-only-225638\", \"content\": \"th\"}}','2026-09-26T22:56:39.104'),(218,1,'NOTICE','PUBLISH','ANNOUNCEMENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"endTime\": 1798732799000, \"startTime\": 1767196800000}, \"announcementId\": 1}','2026-09-26T22:56:39.187'),(219,1,'NOTICE','PUBLISH','ANNOUNCEMENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"公告不存在：99999999\"}','2026-09-26T22:56:40.182'),(220,1,'NOTICE','DELETE','ANNOUNCEMENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"已发布公告请先下架再删除\"}','2026-09-26T22:56:40.318'),(221,1,'DICT','DELETE','REHAB_PART_DICT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"该部位已被 4 个动作引用，无法删除\"}','2026-09-26T22:56:40.526'),(222,1,'DICT','CREATE','REHAB_PART_DICT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"dto\": {\"code\": \"M4T5638\", \"name\": \"M4test-225638\", \"sortNo\": 999, \"enabled\": true, \"groupCode\": \"OVERALL\"}}','2026-09-26T22:56:40.623'),(223,1,'DICT','DELETE','REHAB_PART_DICT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T22:56:40.735'),(224,1,'DICT','DELETE','REHAB_PART_DICT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"部位编码不存在：NOPE99\"}','2026-09-26T22:56:40.808'),(225,1,'BACKUP','CREATE','DB_BACKUP',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T22:56:41.115'),(226,1,'BACKUP','RESTORE','DB_BACKUP',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"reason\": \"m4-restore-test\", \"backupId\": 1, \"snapshotId\": 2}','2026-09-26T22:56:42.682'),(227,1,'BACKUP','RESTORE_SNAPSHOT','DB_BACKUP',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"reason\": \"m4-restore-test\", \"snapshotId\": 2, \"sourceBackupId\": 1}','2026-09-26T22:56:42.687'),(228,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T22:59:46.001'),(229,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T23:00:33.976'),(230,1,'NOTICE','CREATE','ANNOUNCEMENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"scope\": \"ALL\", \"title\": \"M4-ann-file\", \"content\": \"all-scope-body\"}}','2026-09-26T23:00:34.044'),(231,1,'NOTICE','CREATE','ANNOUNCEMENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"scope\": \"ALL\", \"title\": \"M4-ann-cnpath\", \"content\": \"all-scope-body\"}}','2026-09-26T23:00:34.107'),(232,1,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T23:13:16.122'),(233,17,'AUTH','LOGIN','USER_ACCOUNT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T23:13:16.481'),(234,1,'NOTICE','CREATE','ANNOUNCEMENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"scope\": \"THERAPIST\", \"title\": \"therapist-only-231316\", \"content\": \"th\"}}','2026-09-26T23:13:17.977'),(235,1,'NOTICE','PUBLISH','ANNOUNCEMENT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"request\": {\"endTime\": 1798732799000, \"startTime\": 1767196800000}, \"announcementId\": 4}','2026-09-26T23:13:18.052'),(236,1,'NOTICE','PUBLISH','ANNOUNCEMENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"公告不存在：99999999\"}','2026-09-26T23:13:19'),(237,1,'NOTICE','DELETE','ANNOUNCEMENT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"已发布公告请先下架再删除\"}','2026-09-26T23:13:19.117'),(238,1,'DICT','DELETE','REHAB_PART_DICT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"该部位已被 4 个动作引用，无法删除\"}','2026-09-26T23:13:19.294'),(239,1,'DICT','CREATE','REHAB_PART_DICT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS','{\"dto\": {\"code\": \"M4T1316\", \"name\": \"M4test-231316\", \"sortNo\": 999, \"enabled\": true, \"groupCode\": \"OVERALL\"}}','2026-09-26T23:13:19.377'),(240,1,'DICT','DELETE','REHAB_PART_DICT',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T23:13:19.457'),(241,1,'DICT','DELETE','REHAB_PART_DICT',NULL,'0:0:0:0:0:0:0:1',NULL,'FAIL','{\"error\": \"部位编码不存在：NOPE99\"}','2026-09-26T23:13:19.513'),(242,1,'BACKUP','CREATE','DB_BACKUP',NULL,'0:0:0:0:0:0:0:1',NULL,'SUCCESS',NULL,'2026-09-26T23:13:19.772')
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `patient_profile`
-- >>> STATEMENT <<<
CREATE TABLE `patient_profile` (
  `patient_profile_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '患者档案ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户账号ID',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '真实姓名',
  `gender` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '性别：M男/F女/U未知',
  `birth_date` date NOT NULL COMMENT '出生日期',
  `height_cm` decimal(5,2) DEFAULT NULL COMMENT '身高(cm)',
  `weight_kg` decimal(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `medical_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '既往病史',
  `allergy_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '过敏史',
  `contraindication` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '禁忌症',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (`patient_profile_id`),
  UNIQUE KEY `uk_patient_user` (`user_id`),
  CONSTRAINT `fk_patient_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `chk_patient_gender` CHECK ((`gender` in (_utf8mb4'M',_utf8mb4'F',_utf8mb4'U'))),
  CONSTRAINT `chk_patient_height` CHECK (((`height_cm` is null) or (`height_cm` > 0))),
  CONSTRAINT `chk_patient_weight` CHECK (((`weight_kg` is null) or (`weight_kg` > 0)))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者档案'
-- >>> STATEMENT <<<
INSERT INTO `patient_profile` (`patient_profile_id`,`user_id`,`real_name`,`gender`,`birth_date`,`height_cm`,`weight_kg`,`medical_history`,`allergy_info`,`contraindication`,`created_at`,`updated_at`,`deleted`) VALUES (6,17,'M3Pat','M','1990-01-01',NULL,NULL,NULL,NULL,NULL,'2026-09-26T20:48:52.797','2026-09-26T20:48:52.797',0)
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `patient_rehab_part`
-- >>> STATEMENT <<<
CREATE TABLE `patient_rehab_part` (
  `patient_rehab_part_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '登记ID',
  `patient_id` bigint unsigned NOT NULL COMMENT '患者用户ID(user_account.user_id)',
  `rehab_part_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '康复部位编码',
  `side` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BILATERAL' COMMENT '侧别 LEFT/RIGHT/BILATERAL',
  `primary_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否主康复部位 1是0否',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/RESOLVED',
  `diagnosed_at` date DEFAULT NULL COMMENT '确诊或受损日期',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (`patient_rehab_part_id`),
  KEY `idx_pp_patient_status` (`patient_id`,`status`),
  KEY `idx_pp_part_side` (`patient_id`,`rehab_part_code`,`side`),
  KEY `idx_pp_part` (`rehab_part_code`),
  CONSTRAINT `fk_pp_part` FOREIGN KEY (`rehab_part_code`) REFERENCES `rehab_part_dict` (`code`),
  CONSTRAINT `fk_pp_patient` FOREIGN KEY (`patient_id`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `chk_pp_primary` CHECK ((`primary_flag` in (0,1))),
  CONSTRAINT `chk_pp_side` CHECK ((`side` in (_utf8mb4'LEFT',_utf8mb4'RIGHT',_utf8mb4'BILATERAL')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者康复部位（多对多）'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `patient_therapist`
-- >>> STATEMENT <<<
CREATE TABLE `patient_therapist` (
  `binding_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '绑定关系ID',
  `patient_id` bigint unsigned NOT NULL COMMENT '患者用户ID',
  `therapist_id` bigint unsigned NOT NULL COMMENT '治疗师用户ID',
  `relation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关系类型：PRIMARY主治/SECONDARY协作',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态：ACTIVE/ENDED',
  `start_at` datetime(3) NOT NULL COMMENT '绑定开始时间',
  `end_at` datetime(3) DEFAULT NULL COMMENT '绑定结束时间',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人用户ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`binding_id`),
  UNIQUE KEY `uk_binding_id_pair` (`binding_id`,`patient_id`,`therapist_id`),
  KEY `idx_binding_patient` (`patient_id`,`status`),
  KEY `idx_binding_therapist` (`therapist_id`,`status`),
  KEY `fk_binding_creator` (`created_by`),
  CONSTRAINT `fk_binding_creator` FOREIGN KEY (`created_by`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `fk_binding_patient` FOREIGN KEY (`patient_id`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `fk_binding_therapist` FOREIGN KEY (`therapist_id`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `chk_binding_period` CHECK (((`end_at` is null) or (`end_at` >= `start_at`)))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者治疗师关系'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `plan_daily_task`
-- >>> STATEMENT <<<
CREATE TABLE `plan_daily_task` (
  `task_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `plan_item_id` bigint unsigned NOT NULL COMMENT '计划项ID',
  `task_date` date NOT NULL COMMENT '任务日期',
  `target_sets` int NOT NULL COMMENT '目标组数（生成任务时快照）',
  `target_reps` int DEFAULT NULL COMMENT '目标次数（生成任务时快照）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态：PENDING/IN_PROGRESS/COMPLETED/SKIPPED/EXPIRED',
  `skip_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '跳过原因',
  `started_at` datetime(3) DEFAULT NULL COMMENT '开始时间',
  `completed_at` datetime(3) DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_task_item_date` (`plan_item_id`,`task_date`),
  KEY `idx_task_date_status` (`task_date`,`status`),
  CONSTRAINT `fk_task_item` FOREIGN KEY (`plan_item_id`) REFERENCES `plan_item` (`plan_item_id`),
  CONSTRAINT `chk_task_target_reps` CHECK (((`target_reps` is null) or (`target_reps` >= 0))),
  CONSTRAINT `chk_task_target_sets` CHECK ((`target_sets` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日训练任务'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `plan_item`
-- >>> STATEMENT <<<
CREATE TABLE `plan_item` (
  `plan_item_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '计划项ID',
  `plan_id` bigint unsigned NOT NULL COMMENT '计划ID',
  `action_version_id` bigint unsigned NOT NULL COMMENT '动作版本ID',
  `sequence_no` int NOT NULL COMMENT '动作顺序号',
  `target_sets` int NOT NULL COMMENT '目标组数',
  `target_reps` int DEFAULT NULL COMMENT '目标次数/组',
  `target_amplitude` decimal(6,2) DEFAULT NULL COMMENT '目标幅度(度)',
  `duration_sec` int DEFAULT NULL COMMENT '单组时长(秒)',
  `rest_sec` int DEFAULT NULL COMMENT '组间休息(秒)',
  `instruction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '动作指导语',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (`plan_item_id`),
  UNIQUE KEY `uk_plan_item_sequence` (`plan_id`,`sequence_no`),
  KEY `fk_item_version` (`action_version_id`),
  CONSTRAINT `fk_item_plan` FOREIGN KEY (`plan_id`) REFERENCES `rehab_plan` (`plan_id`),
  CONSTRAINT `fk_item_version` FOREIGN KEY (`action_version_id`) REFERENCES `action_version` (`action_version_id`),
  CONSTRAINT `chk_item_duration` CHECK (((`duration_sec` is null) or (`duration_sec` >= 0))),
  CONSTRAINT `chk_item_reps` CHECK (((`target_reps` is null) or (`target_reps` >= 0))),
  CONSTRAINT `chk_item_rest` CHECK (((`rest_sec` is null) or (`rest_sec` >= 0))),
  CONSTRAINT `chk_item_sequence` CHECK ((`sequence_no` > 0)),
  CONSTRAINT `chk_item_sets` CHECK ((`target_sets` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划动作项'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `plan_schedule`
-- >>> STATEMENT <<<
CREATE TABLE `plan_schedule` (
  `schedule_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '日程ID',
  `plan_id` bigint unsigned NOT NULL COMMENT '计划ID',
  `weekday` tinyint NOT NULL COMMENT '星期几：1周一 ... 7周日',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`schedule_id`),
  UNIQUE KEY `uk_schedule_day` (`plan_id`,`weekday`),
  CONSTRAINT `fk_schedule_plan` FOREIGN KEY (`plan_id`) REFERENCES `rehab_plan` (`plan_id`),
  CONSTRAINT `chk_schedule_weekday` CHECK ((`weekday` between 1 and 7))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划执行日程'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `rehab_part_dict`
-- >>> STATEMENT <<<
CREATE TABLE `rehab_part_dict` (
  `code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '康复部位编码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '康复部位名称',
  `group_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '部位大类：UPPER上肢/LOWER下肢/SPINE脊柱/OVERALL全身综合',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：1是0否',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`code`),
  UNIQUE KEY `uk_part_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='康复部位字典'
-- >>> STATEMENT <<<
INSERT INTO `rehab_part_dict` (`code`,`name`,`group_code`,`sort_no`,`enabled`,`created_at`,`updated_at`) VALUES ('ANKLE','踝部','LOWER',60,1,'2026-09-24T17:33:21.432','2026-09-24T17:33:21.432'),('CERVICAL','颈椎','SPINE',70,1,'2026-09-24T17:33:21.432','2026-09-24T17:33:21.432'),('ELBOW','肘部','UPPER',20,1,'2026-09-24T17:33:21.432','2026-09-24T17:33:21.432'),('HIP','髋部','LOWER',40,1,'2026-09-24T17:33:21.432','2026-09-24T17:33:21.432'),('KNEE','膝部','LOWER',50,1,'2026-09-24T17:33:21.432','2026-09-24T17:33:21.432'),('LUMBAR','腰椎','SPINE',80,1,'2026-09-24T17:33:21.432','2026-09-24T17:33:21.432'),('OVERALL','全身综合','OVERALL',90,1,'2026-09-24T17:33:21.432','2026-09-24T17:33:21.432'),('SHOULDER','肩部','UPPER',10,1,'2026-09-24T17:33:21.432','2026-09-24T17:33:21.432'),('WRIST','腕部','UPPER',30,1,'2026-09-24T17:33:21.432','2026-09-24T17:33:21.432')
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `rehab_plan`
-- >>> STATEMENT <<<
CREATE TABLE `rehab_plan` (
  `plan_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `patient_id` bigint unsigned NOT NULL COMMENT '患者用户ID',
  `therapist_id` bigint unsigned NOT NULL COMMENT '治疗师用户ID',
  `binding_id` bigint unsigned NOT NULL COMMENT '患者治疗师绑定ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '计划名称',
  `rehab_part_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '康复部位编码',
  `version_no` int NOT NULL COMMENT '计划版本号（调整计划时递增）',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `frequency_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '频次类型：DAILY每日/WEEKLY每周/CUSTOM自定义',
  `frequency_value` int DEFAULT NULL COMMENT '频次值（如每周N次）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态：ACTIVE/COMPLETED/SUSPENDED/CANCELLED',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (`plan_id`),
  UNIQUE KEY `uk_plan_version` (`patient_id`,`name`,`version_no`),
  KEY `idx_plan_patient_status` (`patient_id`,`status`),
  KEY `idx_plan_binding` (`binding_id`),
  KEY `fk_plan_therapist` (`therapist_id`),
  KEY `fk_plan_part` (`rehab_part_code`),
  KEY `fk_plan_binding_pair` (`binding_id`,`patient_id`,`therapist_id`),
  CONSTRAINT `fk_plan_binding_pair` FOREIGN KEY (`binding_id`, `patient_id`, `therapist_id`) REFERENCES `patient_therapist` (`binding_id`, `patient_id`, `therapist_id`),
  CONSTRAINT `fk_plan_part` FOREIGN KEY (`rehab_part_code`) REFERENCES `rehab_part_dict` (`code`),
  CONSTRAINT `fk_plan_patient` FOREIGN KEY (`patient_id`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `fk_plan_therapist` FOREIGN KEY (`therapist_id`) REFERENCES `user_account` (`user_id`),
  CONSTRAINT `chk_plan_dates` CHECK ((`end_date` >= `start_date`)),
  CONSTRAINT `chk_plan_version` CHECK ((`version_no` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='康复计划'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `therapist_profile`
-- >>> STATEMENT <<<
CREATE TABLE `therapist_profile` (
  `therapist_profile_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '治疗师档案ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户账号ID',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '真实姓名',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职称',
  `specialty` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专长领域',
  `license_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执业证书编号',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '个人简介',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (`therapist_profile_id`),
  UNIQUE KEY `uk_therapist_user` (`user_id`),
  UNIQUE KEY `uk_therapist_license` (`license_no`),
  CONSTRAINT `fk_therapist_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='治疗师档案'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `training_error_log`
-- >>> STATEMENT <<<
CREATE TABLE `training_error_log` (
  `error_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '纠错日志ID',
  `record_id` bigint unsigned NOT NULL COMMENT '训练记录ID',
  `rule_id` bigint unsigned DEFAULT NULL COMMENT '触发的规则ID（NULL为非规则类错误）',
  `dimension` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评估维度',
  `phase` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作阶段',
  `error_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '错误类型：AMPLITUDE不足/SPEED过快等',
  `joint_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关节名称',
  `current_value` decimal(8,3) DEFAULT NULL COMMENT '实测值',
  `expected_value` decimal(8,3) DEFAULT NULL COMMENT '期望值',
  `deviation` decimal(8,3) DEFAULT NULL COMMENT '偏差值',
  `severity` tinyint NOT NULL COMMENT '严重等级',
  `frame_no` int DEFAULT NULL COMMENT '发生帧号',
  `prompt_text` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '纠错提示文案（快照）',
  `error_at` datetime(3) NOT NULL COMMENT '错误发生时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`error_id`),
  KEY `idx_error_record` (`record_id`),
  KEY `idx_error_dimension` (`dimension`,`severity`),
  KEY `fk_error_rule` (`rule_id`),
  CONSTRAINT `fk_error_record` FOREIGN KEY (`record_id`) REFERENCES `training_record` (`record_id`),
  CONSTRAINT `fk_error_rule` FOREIGN KEY (`rule_id`) REFERENCES `action_rule` (`action_rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练纠错日志'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `training_record`
-- >>> STATEMENT <<<
CREATE TABLE `training_record` (
  `record_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `daily_task_id` bigint unsigned NOT NULL COMMENT '每日任务ID',
  `action_version_id` bigint unsigned NOT NULL COMMENT '动作版本快照（提交时从 plan_item 复制，不随后续版本变更变化）',
  `started_at` datetime(3) NOT NULL COMMENT '训练开始时间',
  `ended_at` datetime(3) NOT NULL COMMENT '训练结束时间',
  `total_reps` int NOT NULL COMMENT '总次数',
  `correct_reps` int NOT NULL COMMENT '标准次数',
  `total_score` decimal(5,2) NOT NULL COMMENT '综合得分(0-100)',
  `score_detail` json NOT NULL COMMENT '评分明细（各维度得分）',
  `avg_rom` decimal(6,2) DEFAULT NULL COMMENT '平均关节活动度(度)',
  `peak_angle` decimal(6,2) DEFAULT NULL COMMENT '峰值角度(度)',
  `trough_angle` decimal(6,2) DEFAULT NULL COMMENT '谷值角度(度)',
  `peak_frame_no` int DEFAULT NULL COMMENT '峰值帧号',
  `trough_frame_no` int DEFAULT NULL COMMENT '谷值帧号',
  `hold_ratio` decimal(5,4) DEFAULT NULL COMMENT '保持达标率(0-1)',
  `template_similarity` decimal(5,4) DEFAULT NULL COMMENT '与标准模板相似度(0-1)',
  `valid_frame_ratio` decimal(5,4) DEFAULT NULL COMMENT '有效帧占比(0-1)',
  `pose_repaired_ratio` decimal(5,4) DEFAULT NULL COMMENT '关键点修复帧占比(0-1)',
  `stability_score` decimal(5,2) DEFAULT NULL COMMENT '稳定性得分(0-100)',
  `symmetry_score` decimal(5,2) DEFAULT NULL COMMENT '对称性得分(0-100)',
  `pose_quality` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '姿态质量：GOOD/FAIR/POOR',
  `pose_engine_id` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '姿态引擎标识（分析时快照）',
  `client_request_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '客户端请求ID（幂等防重）',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_record_idempotency` (`daily_task_id`,`client_request_id`),
  KEY `idx_record_task_time` (`daily_task_id`,`created_at`),
  KEY `idx_record_version_time` (`action_version_id`,`created_at`),
  CONSTRAINT `fk_record_task` FOREIGN KEY (`daily_task_id`) REFERENCES `plan_daily_task` (`task_id`),
  CONSTRAINT `fk_record_version` FOREIGN KEY (`action_version_id`) REFERENCES `action_version` (`action_version_id`),
  CONSTRAINT `chk_record_reps` CHECK (((`total_reps` >= 0) and (`correct_reps` between 0 and `total_reps`))),
  CONSTRAINT `chk_record_score` CHECK ((`total_score` between 0 and 100)),
  CONSTRAINT `chk_record_time` CHECK ((`ended_at` >= `started_at`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练记录'
-- >>> STATEMENT <<<
DROP TABLE IF EXISTS `user_account`
-- >>> STATEMENT <<<
CREATE TABLE `user_account` (
  `user_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户账号ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录名',
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BCrypt密码密文',
  `role_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色：PATIENT/THERAPIST/ADMIN',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1启用0禁用',
  `last_login_at` datetime(3) DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  CONSTRAINT `chk_user_deleted` CHECK ((`deleted` in (0,1))),
  CONSTRAINT `chk_user_role` CHECK ((`role_code` in (_utf8mb4'PATIENT',_utf8mb4'THERAPIST',_utf8mb4'ADMIN'))),
  CONSTRAINT `chk_user_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号'
-- >>> STATEMENT <<<
INSERT INTO `user_account` (`user_id`,`username`,`password_hash`,`role_code`,`phone`,`status`,`last_login_at`,`created_at`,`updated_at`,`deleted`) VALUES (1,'admin','$2a$10$Kp/PcyWvNL1G5qnH40xm6ew4.kywekDG5NXhZEdXxr7xa3D0RjCey','ADMIN',NULL,1,'2026-09-26T23:13:15.873','2026-09-24T17:33:21.434','2026-09-26T23:13:16.114',0),(17,'m3pat01','$2a$10$QoV50P/LAYkSa0pP5NnTW.rNweBvk0Cja1q87rBTKVlI7PPUumAYe','PATIENT',NULL,1,'2026-09-26T23:13:16.239','2026-09-26T20:48:52.789','2026-09-26T23:13:16.478',0)
-- >>> STATEMENT <<<
SET FOREIGN_KEY_CHECKS=1
