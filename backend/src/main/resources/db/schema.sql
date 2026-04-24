CREATE DATABASE IF NOT EXISTS mydb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mydb;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nickname VARCHAR(100) COMMENT '昵称',
    storage_limit BIGINT DEFAULT 10737418240 COMMENT '存储限制（字节，默认10GB）',
    storage_used BIGINT DEFAULT 0 COMMENT '已使用存储（字节）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父文件夹ID，0表示根目录',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) COMMENT '文件存储路径（仅文件有）',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    file_type VARCHAR(50) COMMENT '文件类型：folder-文件夹，其他为MIME类型',
    extension VARCHAR(20) COMMENT '文件扩展名',
    is_folder TINYINT DEFAULT 0 COMMENT '是否为文件夹：0-文件，1-文件夹',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除（回收站）',
    delete_time DATETIME COMMENT '删除时间',
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

CREATE TABLE IF NOT EXISTS shares (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分享ID',
    file_id BIGINT NOT NULL COMMENT '文件ID',
    user_id BIGINT NOT NULL COMMENT '分享者ID',
    share_code VARCHAR(20) NOT NULL UNIQUE COMMENT '分享码',
    password VARCHAR(255) COMMENT '访问密码（加密）',
    expire_time DATETIME COMMENT '过期时间，null表示永久有效',
    view_count INT DEFAULT 0 COMMENT '访问次数',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_share_code (share_code),
    INDEX idx_file_id (file_id),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享表';

INSERT INTO users (username, password, nickname) VALUES ('admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员');
