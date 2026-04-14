# Video Streaming Platform

## Nền Tảng Phát Video Trực Tuyến Với Hệ Thống Gợi Ý Thông Minh

---

### Kiến Trúc
Hệ thống sử dụng kiến trúc **Microservices** với 6 services chính:

| Service | Tech Stack | Port |
|---------|-----------|------|
| API Gateway | Spring Cloud Gateway | 8080 |
| User Service | Spring Boot 3 | 8081 |
| Video Catalog Service | Spring Boot 3 | 8082 |
| Transcoding Service | Spring Boot 3 + FFmpeg | 8083 |
| Interaction Service | Spring Boot 3 | 8084 |
| Recommendation Service | Python / FastAPI | 8085 |
| Frontend | React.js + TailwindCSS | 3000 |

### Infrastructure
- **Database:** PostgreSQL 14
- **Cache:** Redis 7
- **Message Broker:** Apache Kafka
- **Object Storage:** MinIO (S3 compatible)
- **Deployment:** Docker + Docker Compose

### Quick Start

```bash
# 1. Khởi động infrastructure
docker-compose up -d postgres redis zookeeper kafka minio

# 2. Khởi động tất cả services
docker-compose up --build -d

# 3. Truy cập
Frontend: http://localhost:3000
API Gateway: http://localhost:8080
MinIO Console: http://localhost:9001
```
