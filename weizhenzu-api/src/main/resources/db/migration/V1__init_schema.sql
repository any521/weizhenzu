-- =====================================================
-- 味真足外卖订餐系统 - V1 初始化脚本
-- 共 29 张表
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. C 端用户
-- ----------------------------
CREATE TABLE `t_user` (
  `id`              BIGINT       NOT NULL COMMENT '用户ID',
  `username`        VARCHAR(50)  DEFAULT NULL COMMENT '用户名（唯一，可选）',
  `phone`           VARCHAR(64)  NOT NULL COMMENT '手机号（加密存储）',
  `phone_hash`      VARCHAR(64)  NOT NULL COMMENT '手机号哈希（用于查询）',
  `password`        VARCHAR(100) DEFAULT NULL COMMENT '密码（BCrypt，可选）',
  `nickname`        VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `avatar`          VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `gender`          TINYINT      DEFAULT 0 COMMENT '性别：0未知 1男 2女',
  `birthday`        DATE         DEFAULT NULL COMMENT '生日',
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `real_name`       VARCHAR(50)  DEFAULT NULL COMMENT '实名（加密）',
  `id_card`         VARCHAR(100) DEFAULT NULL COMMENT '身份证（加密）',
  `level`           INT          NOT NULL DEFAULT 1 COMMENT '会员等级',
  `points`          INT          NOT NULL DEFAULT 0 COMMENT '积分',
  `balance`         DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '余额',
  `last_login_at`   DATETIME     DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip`   VARCHAR(50)  DEFAULT NULL COMMENT '最后登录IP',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by`      BIGINT       DEFAULT NULL,
  `updated_by`      BIGINT       DEFAULT NULL,
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  `version`         INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone_hash` (`phone_hash`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='C端用户';

-- ----------------------------
-- 2. 商家
-- ----------------------------
CREATE TABLE `t_merchant` (
  `id`              BIGINT       NOT NULL,
  `phone`           VARCHAR(64)  NOT NULL,
  `phone_hash`      VARCHAR(64)  NOT NULL,
  `password`        VARCHAR(100) NOT NULL,
  `name`            VARCHAR(100) NOT NULL,
  `logo`            VARCHAR(500) DEFAULT NULL,
  `category_id`     BIGINT       NOT NULL,
  `description`     VARCHAR(500) DEFAULT NULL,
  `notice`          VARCHAR(500) DEFAULT NULL,
  `contact_name`    VARCHAR(50)  DEFAULT NULL,
  `contact_phone`   VARCHAR(20)  DEFAULT NULL,
  `province`        VARCHAR(50)  DEFAULT NULL,
  `city`            VARCHAR(50)  DEFAULT NULL,
  `district`        VARCHAR(50)  DEFAULT NULL,
  `address`         VARCHAR(200) DEFAULT NULL,
  `longitude`       DECIMAL(10,7) DEFAULT NULL,
  `latitude`        DECIMAL(10,7) DEFAULT NULL,
  `delivery_radius` INT          DEFAULT 3000,
  `min_order_amount` DECIMAL(10,2) DEFAULT 0.00,
  `delivery_fee`    DECIMAL(10,2) DEFAULT 0.00,
  `packing_fee`     DECIMAL(10,2) DEFAULT 0.00,
  `open_time`       VARCHAR(100) DEFAULT NULL,
  `is_open`         TINYINT      NOT NULL DEFAULT 1,
  `status`          TINYINT      NOT NULL DEFAULT 0,
  `audit_remark`    VARCHAR(500) DEFAULT NULL,
  `rating`          DECIMAL(2,1) DEFAULT 5.0,
  `month_sales`     INT          DEFAULT 0,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by`      BIGINT       DEFAULT NULL,
  `updated_by`      BIGINT       DEFAULT NULL,
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  `version`         INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone_hash` (`phone_hash`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_geo` (`longitude`, `latitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家';

-- ----------------------------
-- 3. 商家类目
-- ----------------------------
CREATE TABLE `t_merchant_category` (
  `id`          BIGINT       NOT NULL,
  `name`        VARCHAR(50)  NOT NULL,
  `icon`        VARCHAR(500) DEFAULT NULL,
  `sort`        INT          DEFAULT 0,
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  `version`     INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家类目';

-- ----------------------------
-- 4. 菜品
-- ----------------------------
CREATE TABLE `t_dish` (
  `id`              BIGINT       NOT NULL,
  `merchant_id`     BIGINT       NOT NULL,
  `category_id`     BIGINT       NOT NULL,
  `name`            VARCHAR(100) NOT NULL,
  `description`     VARCHAR(500) DEFAULT NULL,
  `image`           VARCHAR(500) DEFAULT NULL,
  `images`          VARCHAR(1000) DEFAULT NULL,
  `price`           DECIMAL(10,2) NOT NULL,
  `original_price`  DECIMAL(10,2) DEFAULT NULL,
  `stock`           INT          NOT NULL DEFAULT 999,
  `month_sales`     INT          DEFAULT 0,
  `total_sales`     INT          DEFAULT 0,
  `rating`          DECIMAL(2,1) DEFAULT 5.0,
  `tags`            VARCHAR(200) DEFAULT NULL,
  `spicy`           TINYINT      DEFAULT 0,
  `status`          TINYINT      NOT NULL DEFAULT 1,
  `sort`            INT          DEFAULT 0,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  `version`         INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_merchant` (`merchant_id`, `status`),
  KEY `idx_category` (`category_id`),
  KEY `idx_merchant_sort` (`merchant_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品';

-- ----------------------------
-- 5. 菜品分类
-- ----------------------------
CREATE TABLE `t_dish_category` (
  `id`           BIGINT       NOT NULL,
  `merchant_id`  BIGINT       NOT NULL,
  `name`         VARCHAR(50)  NOT NULL,
  `sort`         INT          DEFAULT 0,
  `status`       TINYINT      NOT NULL DEFAULT 1,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  `version`      INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_sort` (`merchant_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品分类';

-- ----------------------------
-- 6. 菜品规格
-- ----------------------------
CREATE TABLE `t_dish_spec` (
  `id`           BIGINT       NOT NULL,
  `dish_id`      BIGINT       NOT NULL,
  `name`         VARCHAR(50)  NOT NULL,
  `price_diff`   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `stock`        INT          NOT NULL DEFAULT 999,
  `status`       TINYINT      NOT NULL DEFAULT 1,
  `sort`         INT          DEFAULT 0,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dish` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品规格';

-- ----------------------------
-- 7. 收货地址
-- ----------------------------
CREATE TABLE `t_address` (
  `id`           BIGINT       NOT NULL,
  `user_id`      BIGINT       NOT NULL,
  `contact_name` VARCHAR(50)  NOT NULL,
  `contact_phone` VARCHAR(20) NOT NULL,
  `province`     VARCHAR(50)  NOT NULL,
  `city`         VARCHAR(50)  NOT NULL,
  `district`     VARCHAR(50)  NOT NULL,
  `detail`       VARCHAR(200) NOT NULL,
  `longitude`    DECIMAL(10,7) DEFAULT NULL,
  `latitude`     DECIMAL(10,7) DEFAULT NULL,
  `tag`          VARCHAR(20)  DEFAULT NULL,
  `is_default`   TINYINT      NOT NULL DEFAULT 0,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址';

-- ----------------------------
-- 8. 购物车
-- ----------------------------
CREATE TABLE `t_cart` (
  `id`           BIGINT       NOT NULL,
  `user_id`      BIGINT       NOT NULL,
  `merchant_id`  BIGINT       NOT NULL,
  `dish_id`      BIGINT       NOT NULL,
  `dish_name`    VARCHAR(100) NOT NULL,
  `dish_image`   VARCHAR(500) DEFAULT NULL,
  `spec_id`      BIGINT       DEFAULT NULL,
  `spec_name`    VARCHAR(100) DEFAULT NULL,
  `unit_price`   DECIMAL(10,2) NOT NULL,
  `quantity`     INT          NOT NULL,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_merchant` (`user_id`, `merchant_id`),
  KEY `idx_user_dish` (`user_id`, `dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车';

-- ----------------------------
-- 9. 订单主表
-- ----------------------------
CREATE TABLE `t_order` (
  `id`                 BIGINT       NOT NULL,
  `order_no`           VARCHAR(32)  NOT NULL,
  `user_id`            BIGINT       NOT NULL,
  `merchant_id`        BIGINT       NOT NULL,
  `address_id`         BIGINT       DEFAULT NULL,
  `address_snapshot`   JSON         DEFAULT NULL,
  `status`             TINYINT      NOT NULL DEFAULT 0,
  `pay_status`         TINYINT      NOT NULL DEFAULT 0,
  `item_count`         INT          NOT NULL DEFAULT 0,
  `total_amount`       DECIMAL(10,2) NOT NULL,
  `packing_fee`        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `delivery_fee`       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `merchant_discount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `platform_discount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `coupon_amount`      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `pay_amount`         DECIMAL(10,2) NOT NULL,
  `user_coupon_id`     BIGINT       DEFAULT NULL,
  `remark`             VARCHAR(200) DEFAULT NULL,
  `pay_type`           TINYINT      DEFAULT NULL,
  `pay_time`           DATETIME     DEFAULT NULL,
  `merchant_accept_time` DATETIME   DEFAULT NULL,
  `rider_take_time`    DATETIME     DEFAULT NULL,
  `deliver_time`       DATETIME     DEFAULT NULL,
  `complete_time`      DATETIME     DEFAULT NULL,
  `cancel_time`        DATETIME     DEFAULT NULL,
  `cancel_reason`      VARCHAR(200) DEFAULT NULL,
  `delivery_man_id`    BIGINT       DEFAULT NULL,
  `delivery_task_id`   BIGINT       DEFAULT NULL,
  `expected_time`      DATETIME     DEFAULT NULL,
  `is_rated`           TINYINT      NOT NULL DEFAULT 0,
  `source`             TINYINT      NOT NULL DEFAULT 1,
  `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`            TINYINT      NOT NULL DEFAULT 0,
  `version`            INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_merchant_status` (`merchant_id`, `status`),
  KEY `idx_rider_status` (`delivery_man_id`, `status`),
  KEY `idx_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

-- ----------------------------
-- 10. 订单明细
-- ----------------------------
CREATE TABLE `t_order_item` (
  `id`            BIGINT       NOT NULL,
  `order_id`      BIGINT       NOT NULL,
  `order_no`      VARCHAR(32)  NOT NULL,
  `dish_id`       BIGINT       NOT NULL,
  `dish_name`     VARCHAR(100) NOT NULL,
  `dish_image`    VARCHAR(500) DEFAULT NULL,
  `spec_id`       BIGINT       DEFAULT NULL,
  `spec_name`     VARCHAR(100) DEFAULT NULL,
  `unit_price`    DECIMAL(10,2) NOT NULL,
  `quantity`      INT          NOT NULL,
  `subtotal`      DECIMAL(10,2) NOT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细';

-- ----------------------------
-- 11. 订单状态流转日志
-- ----------------------------
CREATE TABLE `t_order_log` (
  `id`           BIGINT       NOT NULL,
  `order_id`     BIGINT       NOT NULL,
  `order_no`     VARCHAR(32)  NOT NULL,
  `from_status`  TINYINT      DEFAULT NULL,
  `to_status`    TINYINT      NOT NULL,
  `operator_type` TINYINT     NOT NULL,
  `operator_id`  BIGINT       DEFAULT NULL,
  `remark`       VARCHAR(500) DEFAULT NULL,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态流转日志';

-- ----------------------------
-- 12. 支付记录
-- ----------------------------
CREATE TABLE `t_payment` (
  `id`             BIGINT       NOT NULL,
  `payment_no`     VARCHAR(32)  NOT NULL,
  `order_id`       BIGINT       NOT NULL,
  `order_no`       VARCHAR(32)  NOT NULL,
  `user_id`        BIGINT       NOT NULL,
  `amount`         DECIMAL(10,2) NOT NULL,
  `pay_type`       TINYINT      NOT NULL,
  `status`         TINYINT      NOT NULL DEFAULT 0,
  `third_party_no` VARCHAR(64)  DEFAULT NULL,
  `pay_url`        VARCHAR(1000) DEFAULT NULL,
  `paid_time`      DATETIME     DEFAULT NULL,
  `expire_time`    DATETIME     NOT NULL,
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_order` (`order_id`),
  KEY `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录';

-- ----------------------------
-- 13. 支付流水
-- ----------------------------
CREATE TABLE `t_payment_log` (
  `id`           BIGINT       NOT NULL,
  `payment_id`   BIGINT       NOT NULL,
  `payment_no`   VARCHAR(32)  NOT NULL,
  `event`        VARCHAR(50)  NOT NULL,
  `request`      TEXT         DEFAULT NULL,
  `response`     TEXT         DEFAULT NULL,
  `status`       TINYINT      NOT NULL,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_payment` (`payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付流水';

-- ----------------------------
-- 14. 退款单
-- ----------------------------
CREATE TABLE `t_refund` (
  `id`             BIGINT       NOT NULL,
  `refund_no`      VARCHAR(32)  NOT NULL,
  `order_id`       BIGINT       NOT NULL,
  `order_no`       VARCHAR(32)  NOT NULL,
  `payment_id`     BIGINT       NOT NULL,
  `user_id`        BIGINT       NOT NULL,
  `merchant_id`    BIGINT       NOT NULL,
  `amount`         DECIMAL(10,2) NOT NULL,
  `reason`         VARCHAR(200) NOT NULL,
  `status`         TINYINT      NOT NULL DEFAULT 0,
  `third_party_no` VARCHAR(64)  DEFAULT NULL,
  `audit_remark`   VARCHAR(500) DEFAULT NULL,
  `auditor_id`     BIGINT       DEFAULT NULL,
  `refund_time`    DATETIME     DEFAULT NULL,
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order` (`order_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款单';

-- ----------------------------
-- 15. 退款流水
-- ----------------------------
CREATE TABLE `t_refund_log` (
  `id`           BIGINT       NOT NULL,
  `refund_id`    BIGINT       NOT NULL,
  `refund_no`    VARCHAR(32)  NOT NULL,
  `event`        VARCHAR(50)  NOT NULL,
  `request`      TEXT         DEFAULT NULL,
  `response`     TEXT         DEFAULT NULL,
  `status`       TINYINT      NOT NULL,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_refund` (`refund_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款流水';

-- ----------------------------
-- 16. 骑手
-- ----------------------------
CREATE TABLE `t_delivery_man` (
  `id`              BIGINT       NOT NULL,
  `phone`           VARCHAR(64)  NOT NULL,
  `phone_hash`      VARCHAR(64)  NOT NULL,
  `password`        VARCHAR(100) NOT NULL,
  `name`            VARCHAR(50)  NOT NULL,
  `avatar`          VARCHAR(500) DEFAULT NULL,
  `id_card`         VARCHAR(100) DEFAULT NULL,
  `real_name`       VARCHAR(50)  DEFAULT NULL,
  `status`          TINYINT      NOT NULL DEFAULT 0,
  `on_duty`         TINYINT      NOT NULL DEFAULT 0,
  `longitude`       DECIMAL(10,7) DEFAULT NULL,
  `latitude`        DECIMAL(10,7) DEFAULT NULL,
  `location_time`   DATETIME     DEFAULT NULL,
  `total_orders`    INT          DEFAULT 0,
  `month_orders`    INT          DEFAULT 0,
  `rating`          DECIMAL(2,1) DEFAULT 5.0,
  `balance`         DECIMAL(10,2) DEFAULT 0.00,
  `last_login_at`   DATETIME     DEFAULT NULL,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  `version`         INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone_hash` (`phone_hash`),
  KEY `idx_status_duty` (`status`, `on_duty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='骑手';

-- ----------------------------
-- 17. 配送任务
-- ----------------------------
CREATE TABLE `t_delivery_task` (
  `id`              BIGINT       NOT NULL,
  `task_no`         VARCHAR(32)  NOT NULL,
  `order_id`        BIGINT       NOT NULL,
  `order_no`        VARCHAR(32)  NOT NULL,
  `delivery_man_id` BIGINT       DEFAULT NULL,
  `merchant_id`     BIGINT       NOT NULL,
  `merchant_name`   VARCHAR(100) DEFAULT NULL,
  `merchant_address` VARCHAR(200) DEFAULT NULL,
  `merchant_lng`    DECIMAL(10,7) DEFAULT NULL,
  `merchant_lat`    DECIMAL(10,7) DEFAULT NULL,
  `user_address`    VARCHAR(200) DEFAULT NULL,
  `user_lng`        DECIMAL(10,7) DEFAULT NULL,
  `user_lat`        DECIMAL(10,7) DEFAULT NULL,
  `user_phone`      VARCHAR(20)  DEFAULT NULL,
  `status`          TINYINT      NOT NULL DEFAULT 0,
  `fee`             DECIMAL(10,2) NOT NULL,
  `distance`        INT          DEFAULT NULL,
  `grab_time`       DATETIME     DEFAULT NULL,
  `arrive_time`     DATETIME     DEFAULT NULL,
  `pickup_time`     DATETIME     DEFAULT NULL,
  `deliver_time`    DATETIME     DEFAULT NULL,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_no` (`task_no`),
  KEY `idx_rider_status` (`delivery_man_id`, `status`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配送任务';

-- ----------------------------
-- 18. 优惠券模板
-- ----------------------------
CREATE TABLE `t_coupon` (
  `id`              BIGINT       NOT NULL,
  `name`            VARCHAR(100) NOT NULL,
  `type`            TINYINT      NOT NULL,
  `amount`          DECIMAL(10,2) DEFAULT NULL,
  `threshold`       DECIMAL(10,2) DEFAULT 0.00,
  `discount`        DECIMAL(3,2) DEFAULT NULL,
  `max_discount`    DECIMAL(10,2) DEFAULT NULL,
  `total_count`     INT          NOT NULL,
  `received_count`  INT          NOT NULL DEFAULT 0,
  `used_count`      INT          NOT NULL DEFAULT 0,
  `per_limit`       INT          NOT NULL DEFAULT 1,
  `valid_type`      TINYINT      NOT NULL,
  `valid_start`     DATETIME     DEFAULT NULL,
  `valid_end`       DATETIME     DEFAULT NULL,
  `valid_days`      INT          DEFAULT NULL,
  `scope`           TINYINT      NOT NULL DEFAULT 1,
  `scope_ids`       VARCHAR(500) DEFAULT NULL,
  `status`          TINYINT      NOT NULL DEFAULT 1,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板';

-- ----------------------------
-- 19. 用户优惠券
-- ----------------------------
CREATE TABLE `t_user_coupon` (
  `id`            BIGINT       NOT NULL,
  `user_id`       BIGINT       NOT NULL,
  `coupon_id`     BIGINT       NOT NULL,
  `coupon_name`   VARCHAR(100) NOT NULL,
  `coupon_snapshot` JSON       DEFAULT NULL,
  `status`        TINYINT      NOT NULL DEFAULT 0,
  `order_id`      BIGINT       DEFAULT NULL,
  `used_time`     DATETIME     DEFAULT NULL,
  `valid_start`   DATETIME     NOT NULL,
  `valid_end`     DATETIME     NOT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_coupon` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券';

-- ----------------------------
-- 20. 评价
-- ----------------------------
CREATE TABLE `t_review` (
  `id`            BIGINT       NOT NULL,
  `order_id`      BIGINT       NOT NULL,
  `order_no`      VARCHAR(32)  NOT NULL,
  `user_id`       BIGINT       NOT NULL,
  `merchant_id`   BIGINT       NOT NULL,
  `delivery_man_id` BIGINT     DEFAULT NULL,
  `rating`        TINYINT      NOT NULL,
  `taste_score`   TINYINT      DEFAULT NULL,
  `packing_score` TINYINT      DEFAULT NULL,
  `delivery_score` TINYINT     DEFAULT NULL,
  `content`       VARCHAR(500) DEFAULT NULL,
  `images`        VARCHAR(2000) DEFAULT NULL,
  `tags`          VARCHAR(500) DEFAULT NULL,
  `anonymous`     TINYINT      NOT NULL DEFAULT 0,
  `merchant_reply` VARCHAR(500) DEFAULT NULL,
  `merchant_reply_time` DATETIME DEFAULT NULL,
  `status`        TINYINT      NOT NULL DEFAULT 1,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_merchant` (`merchant_id`, `status`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价';

-- ----------------------------
-- 21. 站内消息
-- ----------------------------
CREATE TABLE `t_message` (
  `id`           BIGINT       NOT NULL,
  `user_id`      BIGINT       NOT NULL,
  `user_type`    TINYINT      NOT NULL,
  `title`        VARCHAR(100) NOT NULL,
  `content`      VARCHAR(500) NOT NULL,
  `type`         TINYINT      NOT NULL,
  `biz_type`     VARCHAR(50)  DEFAULT NULL,
  `biz_id`       BIGINT       DEFAULT NULL,
  `is_read`      TINYINT      NOT NULL DEFAULT 0,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `user_type`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内消息';

-- ----------------------------
-- 22. 管理员
-- ----------------------------
CREATE TABLE `t_admin` (
  `id`            BIGINT       NOT NULL,
  `username`      VARCHAR(50)  NOT NULL,
  `password`      VARCHAR(100) NOT NULL,
  `real_name`     VARCHAR(50)  DEFAULT NULL,
  `avatar`        VARCHAR(500) DEFAULT NULL,
  `phone`         VARCHAR(64)  DEFAULT NULL,
  `email`         VARCHAR(100) DEFAULT NULL,
  `role_id`       BIGINT       DEFAULT NULL,
  `status`        TINYINT      NOT NULL DEFAULT 1,
  `last_login_at` DATETIME     DEFAULT NULL,
  `last_login_ip` VARCHAR(50)  DEFAULT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  `version`       INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员';

-- ----------------------------
-- 23. 角色
-- ----------------------------
CREATE TABLE `t_role` (
  `id`          BIGINT       NOT NULL,
  `name`        VARCHAR(50)  NOT NULL,
  `code`        VARCHAR(50)  NOT NULL,
  `description` VARCHAR(200) DEFAULT NULL,
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色';

-- ----------------------------
-- 24. 权限
-- ----------------------------
CREATE TABLE `t_permission` (
  `id`          BIGINT       NOT NULL,
  `parent_id`   BIGINT       NOT NULL DEFAULT 0,
  `name`        VARCHAR(50)  NOT NULL,
  `code`        VARCHAR(100) NOT NULL,
  `type`        TINYINT      NOT NULL,
  `path`        VARCHAR(200) DEFAULT NULL,
  `icon`        VARCHAR(100) DEFAULT NULL,
  `sort`        INT          DEFAULT 0,
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限';

-- ----------------------------
-- 25. 角色权限关联
-- ----------------------------
CREATE TABLE `t_role_permission` (
  `id`            BIGINT NOT NULL,
  `role_id`       BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联';

-- ----------------------------
-- 26. 操作日志
-- ----------------------------
CREATE TABLE `t_operation_log` (
  `id`           BIGINT       NOT NULL,
  `operator_type` TINYINT     NOT NULL,
  `operator_id`  BIGINT       DEFAULT NULL,
  `module`       VARCHAR(50)  NOT NULL,
  `action`       VARCHAR(50)  NOT NULL,
  `method`       VARCHAR(200) DEFAULT NULL,
  `params`       TEXT         DEFAULT NULL,
  `result`       TINYINT      DEFAULT NULL,
  `error_msg`    VARCHAR(1000) DEFAULT NULL,
  `ip`           VARCHAR(50)  DEFAULT NULL,
  `user_agent`   VARCHAR(500) DEFAULT NULL,
  `cost_ms`      INT          DEFAULT NULL,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operator` (`operator_type`, `operator_id`),
  KEY `idx_module` (`module`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';

-- ----------------------------
-- 27. 短信日志
-- ----------------------------
CREATE TABLE `t_sms_log` (
  `id`           BIGINT       NOT NULL,
  `phone`        VARCHAR(20)  NOT NULL,
  `code`         VARCHAR(10)  DEFAULT NULL,
  `scene`        VARCHAR(50)  NOT NULL,
  `content`      VARCHAR(200) DEFAULT NULL,
  `channel`      VARCHAR(50)  DEFAULT NULL,
  `status`       TINYINT      NOT NULL,
  `fail_reason`  VARCHAR(200) DEFAULT NULL,
  `ip`           VARCHAR(50)  DEFAULT NULL,
  `expire_time`  DATETIME     NOT NULL,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_phone_scene` (`phone`, `scene`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信日志';

-- ----------------------------
-- 28. 幂等记录
-- ----------------------------
CREATE TABLE `t_idempotent` (
  `id`           BIGINT       NOT NULL,
  `key`          VARCHAR(100) NOT NULL,
  `value`        VARCHAR(500) DEFAULT NULL,
  `expire_time`  DATETIME     NOT NULL,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`key`),
  KEY `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='幂等记录';

-- ----------------------------
-- 29. 商家结算单
-- ----------------------------
CREATE TABLE `t_settlement` (
  `id`             BIGINT       NOT NULL,
  `settle_no`      VARCHAR(32)  NOT NULL,
  `merchant_id`    BIGINT       NOT NULL,
  `period_start`   DATE         NOT NULL,
  `period_end`     DATE         NOT NULL,
  `order_count`    INT          NOT NULL,
  `order_amount`   DECIMAL(10,2) NOT NULL,
  `platform_fee`   DECIMAL(10,2) NOT NULL,
  `settle_amount`  DECIMAL(10,2) NOT NULL,
  `status`         TINYINT      NOT NULL DEFAULT 0,
  `settle_time`    DATETIME     DEFAULT NULL,
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settle_no` (`settle_no`),
  KEY `idx_merchant_period` (`merchant_id`, `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家结算单';

SET FOREIGN_KEY_CHECKS = 1;
