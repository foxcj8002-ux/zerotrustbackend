#管理员配置工作时间表
CREATE TABLE t_work_time (
                                    id              BIGINT      PRIMARY KEY AUTO_INCREMENT,
                                    work_start_hour INT         NOT NULL COMMENT '工作日开始小时 0-23',
                                    work_end_hour   INT         NOT NULL COMMENT '工作日结束小时 0-23',
                                    night_start_hour INT        NOT NULL COMMENT '夜间开始小时 0-23',
                                    night_end_hour   INT        NOT NULL COMMENT '夜间结束小时 0-23',
                                    enabled         TINYINT(1)  NOT NULL DEFAULT 1,
                                    created_at      DATETIME    DEFAULT CURRENT_TIMESTAMP,
                                    updated_at      DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

#管理员配置公司网段表
CREATE TABLE t_network_zone (
                                id          BIGINT      PRIMARY KEY AUTO_INCREMENT,
                                name        VARCHAR(100) NOT NULL COMMENT '名称',
                                cidr        VARCHAR(64)  NOT NULL COMMENT 'CIDR 例如 10.0.0.0/8',
                                type        VARCHAR(32)  NOT NULL COMMENT 'INTERNAL/BRANCH/VPN/GUEST',
                                risk_level  VARCHAR(32)  NOT NULL COMMENT 'LOW/MEDIUM/HIGH',
                                enabled     TINYINT(1)   NOT NULL DEFAULT 1,
                                created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
                                updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


#管理员配置角色组表

CREATE TABLE t_role_group (
                              id          BIGINT PRIMARY KEY AUTO_INCREMENT,
                              group_name  VARCHAR(100) NOT NULL,
                              description VARCHAR(255),
                              enabled     TINYINT(1)   NOT NULL DEFAULT 1
);

#把用户分配到角色组（谁属于哪个组）——是“用户 ↔ 组”的关联表
CREATE TABLE t_user_group (
                              id       BIGINT PRIMARY KEY AUTO_INCREMENT,
                              user_id  VARCHAR(128) NOT NULL,
                              group_id BIGINT       NOT NULL,
                              UNIQUE KEY uk_user_group (user_id, group_id),
                              CONSTRAINT fk_user_group_role_group FOREIGN KEY (group_id)
                                  REFERENCES t_role_group(id)
);

#最大权限表
CREATE TABLE t_group_resource_max_level (
                                            id                BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            group_id          BIGINT       NOT NULL,
                                            target_type       VARCHAR(32)  NOT NULL COMMENT 'RESOURCE / RESOURCE_GROUP',
                                            resource_id       BIGINT       DEFAULT NULL,
                                            resource_group_id BIGINT       DEFAULT NULL,
                                            max_level_code    VARCHAR(32)  NOT NULL COMMENT 'FULL/MEDIUM/LOW/DISABLED',
                                            enabled           TINYINT(1)   NOT NULL DEFAULT 1,
                                            UNIQUE KEY uk_group_target (group_id, target_type, resource_id, resource_group_id)
);
#动态更新权限表
CREATE TABLE t_user_effective_permission (
                                             id               BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             user_id          VARCHAR(128) NOT NULL,
                                             group_id         BIGINT       NOT NULL,
                                             resource_group_id BIGINT      NOT NULL,
                                             effective_level  VARCHAR(32)  NOT NULL COMMENT 'FULL/MEDIUM/LOW/DISABLED',
                                             source_score     INT          DEFAULT NULL,
                                             updated_at       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                             UNIQUE KEY uk_user_group_resource (user_id, group_id, resource_group_id)
);