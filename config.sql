--工作时间配置表
--存储管理员配置的全局工作时间（开始/结束时间、夜间时间段）。

CREATE TABLE t_work_time (
                             id               BIGINT      PRIMARY KEY AUTO_INCREMENT,
                             work_start_hour  INT         NOT NULL COMMENT '工作日开始小时 0-23',
                             work_end_hour    INT         NOT NULL COMMENT '工作日结束小时 0-23',
                             night_start_hour INT         NOT NULL COMMENT '夜间开始小时 0-23',
                             night_end_hour   INT         NOT NULL COMMENT '夜间结束小时 0-23',
                             created_at       DATETIME    DEFAULT CURRENT_TIMESTAMP,
                             updated_at       DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

--信任网段配置表
--用于维护可信网段
CREATE TABLE t_network_zone (
                                id         BIGINT       PRIMARY KEY AUTO_INCREMENT,
                                name       VARCHAR(100) NOT NULL COMMENT '名称',
                                cidr       VARCHAR(64)  NOT NULL COMMENT 'CIDR 例如 10.0.0.0/8',
                                type       VARCHAR(32)  NOT NULL COMMENT 'INTERNAL / BRANCH / VPN / GUEST',
                                risk_level VARCHAR(32)  NOT NULL COMMENT 'LOW / MEDIUM / HIGH',
                                created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
                                updated_at DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                UNIQUE KEY uk_cidr (cidr)
);
-- --用户属性表
--
-- CREATE TABLE t_user_group (
--                               id       BIGINT       PRIMARY KEY AUTO_INCREMENT,
--                               user_id  VARCHAR(128) NOT NULL,
--                               group_id BIGINT       NOT NULL,
--                               UNIQUE KEY uk_user_group (user_id, group_id),
--                               CONSTRAINT fk_user_group_role_group FOREIGN KEY (group_id)
--                                   REFERENCES t_role_group(id)
--
-- );
--
-- --部门表
-- --维护部门信息（departmentId / 部门名称），供用户属性表引用。
--
--
-- -- 用户属性表（对齐 UserAttributes）
-- --用于存用户等级、风险评分等动态属性
--
-- CREATE TABLE t_user_attributes (
--                                    id               BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID（int64）',
--                                    user_id          BIGINT       NOT NULL COMMENT '用户 ID（int64，对齐 userId）',
--                                    department_id    INT          DEFAULT NULL COMMENT '部门 ID（departmentId）',
--                                    level            VARCHAR(64)  DEFAULT NULL COMMENT '用户等级（level）',
--                                    risk_score       FLOAT        DEFAULT NULL COMMENT '风险评分（risk_score）',
--                                    last_evaluate_at DATETIME     DEFAULT NULL COMMENT '最后评估时间（last_evaluate_at）',
--                                    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP,
--                                    updated_at       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--                                    CONSTRAINT fk_user_attr_department FOREIGN KEY (department_id)
--                                        REFERENCES t_department(id)
-- );
--角色组定义表
--用于定义系统的角色组。

CREATE TABLE t_role_group (
                              id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
                              group_name  VARCHAR(100) NOT NULL COMMENT '角色组名称',
                              description VARCHAR(255) COMMENT '描述',
                              created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
                              updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              UNIQUE KEY uk_group_name (group_name)
);

--用户角色组映射表
--用于记录用户属于哪些角色组（多对多关系）。

CREATE TABLE t_user_group (
                              id       BIGINT       PRIMARY KEY AUTO_INCREMENT,
                              user_id  VARCHAR(128) NOT NULL COMMENT '用户 ID',
                              group_id BIGINT       NOT NULL COMMENT '角色组 ID',
                              UNIQUE KEY uk_user_group (user_id, group_id),
                              CONSTRAINT fk_user_group_role_group FOREIGN KEY (group_id)
                                  REFERENCES t_role_group(id)
);

--资源定义表
--系统中的资源（API、页面、服务等）。

CREATE TABLE t_resource (
                            id         BIGINT       PRIMARY KEY AUTO_INCREMENT,
                            name       VARCHAR(255) NOT NULL COMMENT '资源名称',
                            type       VARCHAR(64)  NOT NULL COMMENT '类型：HTTP / DB / MQ 等',
                            created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
-- URL 与资源匹配表
--负责用于访问时隐藏真实资源 URL。
CREATE TABLE t_resource_url_mapping (
                                        id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
                                        resource_id BIGINT       NOT NULL COMMENT '资源 ID',
                                        url VARCHAR(512) NOT NULL,
                                        http_method VARCHAR(16)  DEFAULT NULL COMMENT 'HTTP 方法 GET/POST 等',
                                        UNIQUE KEY uk_res_url (resource_id, url, http_method),
                                        CONSTRAINT fk_res_url_resource FOREIGN KEY (resource_id)
                                            REFERENCES t_resource(id)
);

--资源组表
--把资源按业务逻辑进行归类。

CREATE TABLE t_resource_group (
                                  id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
                                  name        VARCHAR(100) NOT NULL COMMENT '资源组名称',
                                  description VARCHAR(255) COMMENT '描述',
                                  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
                                  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

--资源组成员表（含资源重要程度）
--用于配置资源在某个资源组中的重要程度等级（0–3）。
CREATE TABLE t_resource_group_item (
                                       id                BIGINT      PRIMARY KEY AUTO_INCREMENT,
                                       resource_group_id BIGINT      NOT NULL COMMENT '资源组 ID',
                                       resource_id       BIGINT      NOT NULL COMMENT '资源 ID',
                                       importance_level  TINYINT     NOT NULL COMMENT '资源重要程度 0-3',
                                       UNIQUE KEY uk_rg_resource (resource_group_id, resource_id),
                                       CONSTRAINT fk_rg_item_group FOREIGN KEY (resource_group_id)
                                           REFERENCES t_resource_group(id),
                                       CONSTRAINT fk_rg_item_resource FOREIGN KEY (resource_id)
                                           REFERENCES t_resource(id)
);

-- 角色组对资源/资源组可访问的最大等级配置
--记录角色组对于资源或资源组的最大可访问等级（0–3）。
CREATE TABLE t_group_resource_max_level (
                                            id                BIGINT      PRIMARY KEY AUTO_INCREMENT,
                                            group_id          BIGINT      NOT NULL COMMENT '角色组 ID',
                                            target_type       VARCHAR(32) NOT NULL COMMENT 'RESOURCE / RESOURCE_GROUP',
                                            resource_id       BIGINT      DEFAULT NULL COMMENT '当 target_type=RESOURCE 时有效',
                                            resource_group_id BIGINT      DEFAULT NULL COMMENT '当 target_type=RESOURCE_GROUP 时有效',
                                            max_level         TINYINT     NOT NULL COMMENT '角色组的最大权限等级 0-3',
                                            UNIQUE KEY uk_group_target (group_id, target_type, resource_id, resource_group_id),
                                            CONSTRAINT fk_grm_group          FOREIGN KEY (group_id)          REFERENCES t_role_group(id),
                                            CONSTRAINT fk_grm_resource       FOREIGN KEY (resource_id)       REFERENCES t_resource(id),
                                            CONSTRAINT fk_grm_resource_group FOREIGN KEY (resource_group_id) REFERENCES t_resource_group(id)
);

--用户实际权限表
--用于存储动态计算后的实际可用等级。
CREATE TABLE t_user_effective_permission (
                                             id                BIGINT      PRIMARY KEY AUTO_INCREMENT,
                                             user_id           VARCHAR(128) NOT NULL COMMENT '用户 ID',
                                             group_id          BIGINT       NOT NULL COMMENT '角色组 ID',
                                             resource_group_id BIGINT       NOT NULL COMMENT '资源组 ID',
                                             effective_level   TINYINT      NOT NULL COMMENT '实际可用权限等级 0-3',
                                             source_score      FLOAT          DEFAULT NULL COMMENT '用于计算的 trust score 或风险评分',
                                             updated_at        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                             UNIQUE KEY uk_user_group_resource (user_id, group_id, resource_group_id),
                                             CONSTRAINT fk_uep_group          FOREIGN KEY (group_id)          REFERENCES t_role_group(id),
                                             CONSTRAINT fk_uep_resource_group FOREIGN KEY (resource_group_id) REFERENCES t_resource_group(id)
);

