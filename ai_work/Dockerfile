# 1. 🌟 关键修改：锁定使用 Debian 12 (bookworm) 版本
# 这样能确保软件包名称稳定，不会莫名其妙消失
FROM python:3.9-slim-bookworm

# 设置工作目录
WORKDIR /app

# 2. 🌟 关键修改：替换源并安装依赖
# - 把 libgl1-mesa-glx 改为 libgl1
# - bookworm 源里包含 openjdk-17，所以这里可以直接用
RUN sed -i 's/deb.debian.org/mirrors.ustc.edu.cn/g' /etc/apt/sources.list.d/debian.sources && \
    apt-get update && apt-get install -y \
    openjdk-17-jre-headless \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# 设置 JAVA_HOME (Bookworm 里的路径)
ENV JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"

# 复制依赖并安装
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 复制所有代码
COPY . .

# 默认指令
CMD ["python", "medical_ai_api.py"]