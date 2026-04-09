# 智光项目 Docker 部署指南

## 📋 目录结构

```
zhiguang_be-main/
├── .env                    # 环境变量配置文件
├── docker-compose.yml      # Docker Compose 编排文件
├── docker/
│   └── mysql/
│       └── init.sql        # 数据库初始化脚本
├── src/                    # 源代码
├── pom.xml                 # Maven 配置
└── README_DOCKER.md        # 本文件
```

## 🚀 快速开始

### 1. 前置要求

- 安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)（Windows/Mac）
- 或安装 [Docker Engine](https://docs.docker.com/engine/install/) + [Docker Compose](https://docs.docker.com/compose/install/)（Linux）
- 确保 Docker 版本 >= 20.10，Docker Compose 版本 >= 2.0

### 2. 启动所有中间件服务

在项目根目录执行：

```bash
# 启动所有服务（后台运行）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 3. 等待服务就绪

首次启动需要下载镜像，可能需要几分钟。可以通过以下命令检查服务健康状态：

```bash
# 查看所有容器状态
docker-compose ps

# 查看特定服务日志
docker-compose logs mysql
docker-compose logs redis
docker-compose logs kafka
docker-compose logs elasticsearch
```

所有服务健康检查通过后，即可使用。

### 4. 访问服务

| 服务 | 地址 | 说明 |
|------|------|------|
| MySQL | `localhost:3306` | 用户名: root, 密码: 123456 |
| Redis | `localhost:6379` | 无密码 |
| Kafka | `localhost:9092` | Bootstrap Servers |
| Zookeeper | `localhost:2181` | Kafka 依赖 |
| Elasticsearch | `http://localhost:9200` | REST API |
| Canal | `localhost:11111` | Binlog 监听 |

### 5. 启动 Spring Boot 应用

#### 方式一：IDEA 直接运行

修改 `application.yml` 中的连接地址为 Docker 服务名：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zhiguang?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
  data:
    redis:
      host: localhost
      port: 6379
  kafka:
    bootstrap-servers: localhost:9092
  elasticsearch:
    uris: http://localhost:9200

canal:
  host: localhost
  port: 11111
```

然后在 IDEA 中运行 `ZhiGuangApplication`。

#### 方式二：Maven 命令行运行

```bash
mvn spring-boot:run
```

## 🔧 常用命令

### 服务管理

```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 重启某个服务
docker-compose restart mysql

# 查看服务状态
docker-compose ps

# 查看实时日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f kafka
```

### 数据管理

```bash
# 停止并删除所有容器、网络、卷（⚠️ 会清除所有数据）
docker-compose down -v

# 仅停止容器，保留数据卷
docker-compose down

# 重新创建某个服务
docker-compose up -d --force-recreate mysql
```

### 进入容器

```bash
# 进入 MySQL 容器
docker exec -it zhiguang-mysql bash

# 进入 MySQL 命令行
docker exec -it zhiguang-mysql mysql -uroot -p123456

# 进入 Redis 容器
docker exec -it zhiguang-redis redis-cli

# 进入 Kafka 容器
docker exec -it zhiguang-kafka bash

# 进入 Elasticsearch 容器
docker exec -it zhiguang-elasticsearch bash
```

### Kafka 操作

```bash
# 创建 Topic
docker exec -it zhiguang-kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic test \
  --partitions 3 \
  --replication-factor 1

# 查看所有 Topic
docker exec -it zhiguang-kafka kafka-topics --list \
  --bootstrap-server localhost:9092

# 查看 Topic 详情
docker exec -it zhiguang-kafka kafka-topics --describe \
  --bootstrap-server localhost:9092 \
  --topic test

# 消费消息
docker exec -it zhiguang-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic test \
  --from-beginning

# 生产消息
docker exec -it zhiguang-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic test
```

### Elasticsearch 操作

```bash
# 检查集群健康状态
curl http://localhost:9200/_cluster/health?pretty

# 查看所有索引
curl http://localhost:9200/_cat/indices?v

# 查看节点信息
curl http://localhost:9200/_cat/nodes?v
```

## ⚙️ 配置说明

### 环境变量 (.env)

所有敏感信息和可配置项都在 `.env` 文件中管理：

```bash
# MySQL 配置
MYSQL_ROOT_PASSWORD=123456
MYSQL_DATABASE=zhiguang

# Redis 配置
REDIS_PORT=6379

# Kafka 配置
KAFKA_PORT=9092

# Elasticsearch 配置
ES_JVM_HEAP_SIZE=2g  # ES 堆内存大小
```

修改 `.env` 后，需要重启服务：

```bash
docker-compose down
docker-compose up -d
```

### 数据持久化

所有数据都存储在 Docker Volume 中，即使容器删除，数据也不会丢失：

- `mysql-data`: MySQL 数据
- `redis-data`: Redis 数据
- `kafka-data`: Kafka 数据
- `es-data`: Elasticsearch 数据
- `zookeeper-data`: Zookeeper 数据
- `canal-data`: Canal 日志

## 🐛 常见问题

### 1. 端口冲突

如果提示端口已被占用，修改 `.env` 文件中的端口号：

```bash
MYSQL_PORT=3307  # 改为其他端口
REDIS_PORT=6380
```

然后重启：

```bash
docker-compose down
docker-compose up -d
```

### 2. Elasticsearch 启动失败

ES 需要较大的内存，确保 Docker 分配了足够的资源：

- Windows/Mac: Docker Desktop -> Settings -> Resources -> Memory (建议 >= 4GB)
- 修改 `.env` 中的 `ES_JVM_HEAP_SIZE=1g` 降低内存需求

### 3. Kafka 连接失败

确保等待 Kafka 完全启动（约 30-60 秒），查看日志：

```bash
docker-compose logs -f kafka
```

看到类似 `Kafka Server started` 表示启动成功。

### 4. 数据库连接失败

检查 MySQL 是否启动完成：

```bash
docker-compose logs mysql
```

看到 `ready for connections` 表示启动成功。

### 5. 重新初始化数据库

如果需要重新执行 `init.sql`：

```bash
# 删除 MySQL 数据卷
docker-compose down -v

# 重新启动
docker-compose up -d
```

⚠️ **警告**: 这会清除所有数据！

### 6. Canal 连接 MySQL 失败

确保 MySQL 开启了 binlog：

```bash
docker exec -it zhiguang-mysql mysql -uroot -p123456 -e "SHOW VARIABLES LIKE 'log_bin';"
```

如果未开启，需要在 `docker-compose.yml` 中添加：

```yaml
command:
  - --log-bin=mysql-bin
  - --binlog-format=ROW
  - --server-id=1
```

## 📝 开发建议

### 1. 本地开发流程

```bash
# 1. 启动所有中间件
docker-compose up -d

# 2. 等待服务就绪
docker-compose ps

# 3. 在 IDEA 中运行 Spring Boot 应用

# 4. 开发完成后停止服务
docker-compose down
```

### 2. 清理无用资源

```bash
# 删除停止的容器
docker container prune

# 删除未使用的镜像
docker image prune

# 删除未使用的卷
docker volume prune

# 一键清理所有
docker system prune -a --volumes
```

### 3. 备份数据

```bash
# 备份 MySQL 数据
docker exec zhiguang-mysql mysqldump -uroot -p123456 zhiguang > backup.sql

# 恢复 MySQL 数据
docker exec -i zhiguang-mysql mysql -uroot -p123456 zhiguang < backup.sql
```

## 🎯 下一步

1. ✅ 启动所有中间件服务
2. ✅ 确认服务健康状态
3. ✅ 配置 Spring Boot 应用连接地址
4. ✅ 运行应用程序
5. ✅ 开始开发

## 📞 技术支持

如有问题，请查看：
- Docker 官方文档: https://docs.docker.com/
- Docker Compose 文档: https://docs.docker.com/compose/
- 项目 Issues: [GitHub Issues](your-repo-url/issues)
