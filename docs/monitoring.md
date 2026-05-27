# Spring AI Lab - Prometheus & Grafana 监控配置

> 配合 Actuator + Micrometer + Prometheus 实现指标采集和可视化。

---

## 1. 依赖配置

在应用的 `pom.xml` 中启用 Prometheus 指标导出：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

## 2. 应用配置

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
        step: 30s
    tags:
      application: ${spring.application.name:spring-ai-lab}
```

启用 Lab 指标导出：

```yaml
spring:
  ai:
    lab:
      observation:
        enabled: true
        token-tracking: true
        latency-tracking: true
        metrics-export: true
        export-prefix: ai_lab
```

---

## 3. Prometheus 配置

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-ai-lab'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          application: 'spring-ai-lab'
```

---

## 4. Grafana Dashboard

导入 `grafana-dashboard.json` 即可使用预置 AI Lab 监控看板。

### 看板包含以下面板：

| 面板 | 指标 | 描述 |
|------|------|------|
| **总 Token 消耗** | `ai_lab_tokens_total` | 全模型 Token 消耗总量 |
| **错误总数** | `ai_lab_errors_total` | 框架内错误发生次数 |
| **文档加载成功率** | `ai_lab_documents_loaded_success / (success + failed)` | 文档 ETL 成功率 |
| **向量入库成功率** | `ai_lab_vectors_stored_*` | 向量入库成功/失败 |
| **活跃会话数** | `ai_lab_active_sessions` | 当前活跃的对话会话数 |
| **JVM 内存** | `jvm_memory_used_bytes` | JVM 内存使用 |
| **HTTP 请求延迟** | `http_server_requests_seconds` | HTTP 接口响应延迟 |

---

## 5. 快速启动

```bash
# 1. 启动 Prometheus
docker run -d -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus

# 2. 启动 Grafana
docker run -d -p 3000:3000 grafana/grafana

# 3. 导入 Dashboard
# 打开 http://localhost:3000 → 导入 grafana-dashboard.json
```
