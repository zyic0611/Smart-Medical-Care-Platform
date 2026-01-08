# =========== 第一阶段：编译构建 (Build Stage) ===========
# 使用官方 Maven 镜像，别名命名为 builder
FROM maven:3.8-openjdk-17 AS builder

# 设置工作目录
WORKDIR /app

# 1. 先只复制 pom.xml 下载依赖 (利用 Docker 缓存，改代码不重新下包)
COPY pom.xml .
# 这一步可能会有点慢，建议配置阿里云 Maven 镜像源，或者耐心等待
RUN mvn dependency:go-offline -B

# 2. 复制所有源代码
COPY src ./src

# 3. 打包 (跳过测试用例，节省时间)
RUN mvn clean package -DskipTests

# =========== 第二阶段：运行环境 (Runtime Stage) ===========
# 使用更小的 JDK 运行时镜像
FROM eclipse-temurin:17-jdk-jammy

# 设置工作目录
WORKDIR /app

# 从第一阶段把编译好的 jar 包复制过来
# 注意：这里假设 target 下只有一个 jar 包，如果有多个需要指定具体名字
COPY --from=builder /app/target/*.jar app.jar


EXPOSE 9090

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]