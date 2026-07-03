<div align="center">

# 🌟 知光 ZhiGuang

**知识获取与分享社区平台**

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-green?logo=spring)]()
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.3-blue?logo=spring)]()
[![License](https://img.shields.io/badge/license-MIT-blue.svg)]()

</div>

##  项目简介

知光是一个知识社区平台，支持发布知识帖文、点赞/收藏、关注取关、首页 Feed 流展示、全文搜索与 AI 智能问答。项目各模块针对 **高并发** 与 **高可用** 场景进行了充分的设计与实现。

- **后端仓库**：[zhiguang_be](https://github.com/qingchunuser/zhiguang-be-main.git)
- **前端仓库**：[zhiguang_fe](https://github.com/qingchunuser/zhiguang-fe-main.git)

## 🏗️ 技术栈

| 层级 | 技术 |
|------|------|
| **语言 & 框架** | Java 21、Spring Boot 3.2、Spring Security、MyBatis |
| **AI 能力** | Spring AI、DeepSeek、RAG（检索增强生成） |
| **数据存储** | MySQL、Redis、Elasticsearch |
| **消息队列** | Apache Kafka |
| **缓存** | Caffeine（本地）+ Redis（分布式） |
| **对象存储** | 阿里云 OSS |
| **数据同步** | Alibaba Canal（binlog 订阅） |
| **分布式** | Redisson（分布式锁/限流） |
| **前端** | React + Vite |

##  核心模块与亮点

###  认证系统
- 基于 Spring Security 的 **JWT 双令牌**认证（RS256 签名）
- 15 分钟 Access Token + 7 天 Refresh Token，Redis 白名单管理
- 支持即时令牌撤销，兼顾安全与性能

###  计数系统
- Redis SDS **二进制紧凑计数**，Lua 脚本原子更新
- 笔记维度（点赞/收藏）+ 用户维度（关注/粉丝）
- 采样一致性校验与**自愈重建**机制

###  发布系统
- 渐进式发布流程，图片/视频/Markdown 文档存入 OSS
- 后端签发预签名 URL + 前端直传，节省带宽
- 接入 **DeepSeek AI** 一键生成文章摘要

###  用户关系系统
- 一主多从 + **事件驱动**架构
- Outbox 模式：同事务写入关注表 + Outbox 表
- Canal 订阅 binlog → Kafka 异步更新粉丝表、计数、列表缓存

###  点赞系统
- **异步写 + 写聚合**：Kafka 批量消费应对高并发写入
- 分片位图实现幂等判重，异常时按需重建
- Kafka "灾难回放"兜底，保证最终一致性

###  Feed 流
- **三级缓存**：Caffeine → Redis 页面缓存 → Redis 片段缓存
- 自定义 HotKey 探测，热点数据按层级延长缓存
- 随机抖动抗雪崩 + **Single-Flight** 锁避免并发回源风暴

###  搜索系统
- 基于 Elasticsearch 的全文检索与标签过滤
- `function_score` 融合 BM25 相关性与业务权重排序
- `search_after` 游_cursor 分页，深分页稳定
- Completion Suggester 实现低延迟前缀联想

###  AI 问答系统（RAG）
- 基于 Spring AI + Elasticsearch 向量库构建 RAG 流程
- 用户提问 → 索引检查 → 向量检索 → Prompt 构造 → 大模型流式生成
- 合理分块、幂等删除、预索引优化，降低首次提问等待时间

##  项目结构



##  快速开始

### 环境要求
- Java 21+
- MySQL 8.0+
- Redis 7.0+
- Apache Kafka
- Elasticsearch 8.x+
- 阿里云 OSS（可选）

### 运行步骤
1. 克隆项目
   git clone https://github.com/qingchunuser/zhiguang-be-main.git
2. 配置 `application.yml`（数据库、Redis、Kafka、ES 等连接信息）
3. 执行 `db/zhiguang.sql` 初始化数据库
4. 进入项目目录
   cd zhiguang-be-main
5. 启动项目
   ./mvnw spring-boot:run
6. 访问项目
   http://localhost:8080/
