# Smart Medical & Elderly Care Platform (智慧医养管理平台)

![SpringBoot](https://img.shields.io/badge/SpringBoot-3.0-green.svg) ![Vue](https://img.shields.io/badge/Vue-3.0-brightgreen.svg) ![YOLO](https://img.shields.io/badge/AI-YOLOv8-blue.svg)

## 📖 项目简介
本项目是一套面向高端医养结合机构的综合管理平台，旨在解决传统护理中**档案碎片化**、**资源调度低效**及**医学影像数据孤岛**问题。
系统集成了 **YOLOv8** 深度学习模型，实现了从老人档案管理、床位智能调度到**AI 跌倒检测/违规预警**的全流程闭环。

## 🚀 核心亮点
- **🏗 多模态存储架构**：基于 **MinIO** 构建影像数据湖，结合 Redis 实现 GB 级医学影像的**分片上传**与**断点续传**，解决弱网传输难题。
- **⚡ 异步削峰解耦**：设计 **RabbitMQ** 异步任务编排，将 Java 业务与 Python AI 推理服务解耦，保障高并发下的系统响应速度。
- **👁️ AI 智能识别**：集成 **YOLOv8** 模型，对无人机/监控视频流进行实时分析，支持违规建筑、跌倒等异常事件的**实时报警**。
- **🛡️ 全栈安全体系**：构建 **JWT + ThreadLocal** 无状态鉴权，配合 **RBAC** 模型与 **AOP** 审计，防御纵向越权攻击。

## 🛠 技术栈
- **后端**：Spring Boot 3, MyBatis-Plus, RabbitMQ, Redis, MinIO
- **前端**：Vue 3, Element Plus, Axios, ECharts
- **AI**：Python 3, Ultralytics YOLOv8, OpenCV

## 📂 目录结构
