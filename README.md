<div align="center">
  <h1>🏥 Smart Care AI - 智慧养老与医疗影像诊断系统</h1>

  <p>
    基于 <b>Spring Boot 3</b> + <b>Vue 3</b> + <b>YOLOv11</b> 的现代化养老院管理平台<br>
    集成 <b>RabbitMQ</b> 异步解耦与 <b>MinIO</b> 对象存储，实现高效的医学影像 AI 辅助诊断
  </p>

  <p>
    <img src="https://img.shields.io/badge/Spring%20Boot-3.x-green?style=flat-square&logo=springboot" alt="Spring Boot">
    <img src="https://img.shields.io/badge/Vue.js-3.x-4FC08D?style=flat-square&logo=vuedotjs" alt="Vue">
    <img src="https://img.shields.io/badge/Element%20Plus-2.x-409EFF?style=flat-square&logo=element" alt="Element Plus">
    <img src="https://img.shields.io/badge/Python-3.9+-3776AB?style=flat-square&logo=python" alt="Python">
    <img src="https://img.shields.io/badge/YOLO-v11-FF9E0F?style=flat-square" alt="YOLOv11">
    <img src="https://img.shields.io/badge/RabbitMQ-Messaging-FF6600?style=flat-square&logo=rabbitmq" alt="RabbitMQ">
    <img src="https://img.shields.io/badge/MinIO-Storage-C72C48?style=flat-square&logo=minio" alt="MinIO">
    <img src="https://img.shields.io/badge/Docker-Container-2496ED?style=flat-square&logo=docker" alt="Docker">
  </p>
</div>

---

## 📖 项目简介 (Introduction)

本项目是一个针对现代养老机构设计的综合管理系统，旨在解决传统养老管理效率低下及医疗资源匮乏的问题。

系统不仅包含完善的**床位管理、人员档案、健康监控**等 ERP 功能，更创新性地引入了**AI 辅助医疗诊断模块**。利用 **YOLOv11** 深度学习模型，对老年人的医学影像（CT/X光）进行自动化病灶检测，辅助医生快速出具诊断报告。

系统采用**前后端分离**架构，后端利用 **RabbitMQ** 实现了 Java 业务系统与 Python AI 推理服务的**跨语言异步解耦**，并使用 **MinIO** 作为统一的非结构化数据存储中心。

## 🏗 系统架构 (System Architecture)

系统采用微服务设计思想，核心业务流程如下：

```mermaid
graph LR
    User[前端/医生] -- 上传影像 --> Java[Spring Boot 核心服务]
    Java -- 存储原图 --> MinIO[(MinIO 对象存储)]
    Java -- 1.发送诊断任务 --> MQ1{RabbitMQ 任务队列}
    
    subgraph Docker Environment
        MQ1 --> PyConsumer[Python 消费者 (YOLOv11 推理)]
        PyConsumer -- 读取原图/上传标注图 --> MinIO
        PyConsumer -- 2.发送诊断结果 --> MQ2{RabbitMQ 结果队列}
        MQ2 --> PyResult[Python 结果缓存服务]
        PyResult -- 缓存结果 --> Redis[(Redis)]
        PyResult -- 提供查询接口 --> FastAPI[FastAPI 接口]
    end
    
    User -- 轮询/查询结果 --> FastAPI
```

注：当前版本使用 FastAPI 作为临时的结果查询网关，未来计划将结果消费逻辑收敛回 Java 端，实现全链路闭环及 WebSocket 推送。

## ✨ 核心功能 (Key Features)

### 🩺 AI 智慧诊断 (AI Diagnosis)
- **异步推理架构**：利用 RabbitMQ 削峰填谷，支持高并发下的医学影像处理，避免阻塞主业务线程。
- **YOLOv11 集成**：部署在 Docker 中的 Python 消费者自动拉取任务，利用 YOLOv11 模型进行毫秒级病灶检测与标注。
- **云端影像存储**：所有医学影像（原图/标注图）均存储于 MinIO，支持海量非结构化数据存储。

### 🛏️ 可视化床位管理 (Visualized Bed Management)
- **影院式选座**：摒弃传统的表格管理，前端采用类似“电影院选座”的图形化界面。
- **状态实时流转**：直观展示床位状态（空闲、占用、维修中），支持拖拽式分配与调整。

### 📊 数据可视化看板 (Dashboard)
- **多维度统计**：基于 ECharts + Redis 缓存，实时展示入住老人趋势、健康比例、护工配比等关键指标。
- **高性能缓存**：利用 Redis 缓存热点统计数据，减轻 MySQL 聚合查询压力。

### 🛡️ 权限与安全 (Security & RBAC)
- **精细化权限控制**：基于 AOP + 自定义注解实现 RBAC 模型，区分 **Admin、医生、护工、普通用户** 四种角色。
- **无状态认证**：采用 JWT Token 进行身份校验，结合 ThreadLocal 实现用户上下文的安全流转。

### 🚀 性能优化 (Performance)
- **多线程断点下载**：针对 DICOM 等大容量医学影像，实现 Java 端的多线程分片下载器，显著提升加载速度。
- **Docker 容器化**：Python 推理环境、Redis、RabbitMQ、MinIO 等中间件全量 Docker 化部署（基于 OrbStack），环境统一，易于扩展。

## 🛠️ 技术栈 (Tech Stack)

| 类别 | 技术框架 | 说明 |
| :--- | :--- | :--- |
| **后端 (Java)** | Spring Boot 3 | 核心业务框架 |
| **前端** | Vue 3 + Element Plus | 现代化 UI 交互 |
| **AI 推理** | Python + YOLOv11 | 深度学习目标检测 |
| **消息队列** | RabbitMQ | 跨语言异步通信 |
| **存储** | MySQL 8 + Redis | 关系型数据与缓存 |
| **文件存储** | MinIO | 分布式对象存储 |
| **部署** | Docker + OrbStack | 容器化运行环境 |



## 🚀 快速开始 (Getting Started)

### 环境要求
- JDK 17+
- Node.js 16+
- Docker & Docker Compose (推荐 OrbStack)
- Python 3.9+ (如果本地运行 AI 服务)

   
