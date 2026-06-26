# 味真足 - 后端基础设施快速启动

## 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 内存 ≥ 4GB

## 快速启动

```bash
# 在 weizhenzu 目录下执行
docker-compose up -d

# 查看运行状态
docker-compose ps

# 查看日志
docker-compose logs -f mysql
docker-compose logs -f redis
docker-compose logs -f rabbitmq
```

## 服务访问

| 服务 | 地址                     | 用户名 | 密码 |
| --- |------------------------| --- | --- |
| MySQL | localhost:3307         | weizhenzu | weizhenzu123456 |
| Redis | localhost:6379         | - | - |
| RabbitMQ Web | http://localhost:15672 | weizhenzu | weizhenzu123456 |
| RabbitMQ AMQP | localhost:5672         | weizhenzu | weizhenzu123456 |

## 停止与清理

```bash
# 停止服务（保留数据）
docker-compose down

# 停止并删除数据卷（清空所有数据）
docker-compose down -v
```

## 数据库初始化

数据库表结构由 Flyway 在应用启动时自动迁移，无需手动导入。

如需手动初始化，可执行：

```bash
# 进入 MySQL 容器
docker exec -it weizhenzu-mysql mysql -uweizhenzu -pweizhenzu123456 weizhenzu

# 执行迁移脚本
source /docker-entrypoint-initdb.d/V1__init_schema.sql
```

## 网络配置

所有服务运行在 `weizhenzu-net` 桥接网络中，应用容器可通过服务名访问：

- `mysql:3306`
- `redis:6379`
- `rabbitmq:5672`

## 故障排查

### MySQL 启动失败
- 检查端口 3306 是否被占用：`netstat -ano | findstr 3306`
- 删除数据卷重试：`docker-compose down -v && docker-compose up -d`

### Redis 连接被拒
- 确认容器健康：`docker-compose ps`
- 进入容器测试：`docker exec -it weizhenzu-redis redis-cli ping`

### RabbitMQ 管理台无法访问
- 等待 30s 让插件初始化
- 检查 15672 端口：`curl http://localhost:15672`
