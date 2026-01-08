# =========== 第一阶段：打包构建 (Build Stage) ===========
FROM node:22 AS builder

WORKDIR /app

# 1. 复制依赖描述文件
COPY package*.json ./

# 2. 安装依赖 (使用淘宝源会快一点，或者直接 npm install)
RUN npm install --registry=https://registry.npmmirror.com

# 3. 复制源代码并打包
COPY . .
RUN npm run build

# =========== 第二阶段：Nginx 部署 (Production Stage) ===========
FROM nginx:alpine

# 把第一阶段打包好的 dist 文件夹，复制到 Nginx 的默认网页目录
COPY --from=builder /app/dist /usr/share/nginx/html

# (可选) 如果你需要自定义 Nginx 配置来解决 Vue 路由 404 问题，可以在这里 COPY 你的 nginx.conf
# COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# 启动 Nginx，并且关闭后台运行模式(daemon off)以便 Docker 监控
CMD ["nginx", "-g", "daemon off;"]