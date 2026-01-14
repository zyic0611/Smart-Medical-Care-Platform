import pika
import json
import uvicorn
from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
import threading
import time
import os

# 配置
RESULT_QUEUE = 'diagnosis_results_queue'
# 存储诊断结果（生产环境建议用 Redis，内存缓存重启会丢）
diagnosis_cache = {}

# 初始化 FastAPI
app = FastAPI(title="医疗影像诊断API")


# 🌟 必须加上 CORS 否则前端 9090 转 9091 会报错
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 启动 RabbitMQ 消费者线程
def consume_results():
    while True: # 🌟 添加外部循环，实现断线重连
        try:
            print(f" [MQ] 尝试连接到 RabbitMQ: rabbitmq...")
            connection = pika.BlockingConnection(pika.ConnectionParameters(
                host='rabbitmq',
                heartbeat=600,
                blocked_connection_timeout=300
            ))
            channel = connection.channel()
            channel.queue_declare(queue=RESULT_QUEUE, durable=True)

            def callback(ch, method, properties, body):
                try:
                    result = json.loads(body.decode('utf-8'))
                    task_id = result.get('task_id')
                    if task_id:
                        diagnosis_cache[task_id] = result
                        print(f" [缓存] 任务 {task_id} 结果已更新")
                    ch.basic_ack(delivery_tag=method.delivery_tag)
                except Exception as e:
                    print(f" [报错] 解析消息失败: {str(e)}")
                    ch.basic_ack(delivery_tag=method.delivery_tag)

            channel.basic_consume(queue=RESULT_QUEUE, on_message_callback=callback)
            print(" [*] 结果缓存消费者已启动，正在监听...")
            channel.start_consuming()
        except Exception as e:
            print(f" [警告] MQ 连接失败或中断，5秒后重试... 错误: {str(e)}")
            time.sleep(5) # 🌟 核心：避免 MQ 没启动时进程疯狂报错崩溃

@app.get("/api/diagnosis/result")
async def get_diagnosis_result(taskId: str = Query(...)):
    result = diagnosis_cache.get(taskId)

    if not result:
        return {
            "code": 200,
            "msg": "processing",
            "data": None
        }

    return {
        "code": 200,
        "msg": "success",
        "data": result
    }

# 🌟🌟🌟 核心修改在这里 🌟🌟🌟
# 不要把线程启动写在 main 里，而是注册为 FastAPI 的启动事件
@app.on_event("startup")
async def startup_event():
    print(" [系统] FastAPI 正在启动，准备开启 MQ 消费者线程...")
    # 启动消费者线程
    consume_thread = threading.Thread(target=consume_results, daemon=True)
    consume_thread.start()

if __name__ == "__main__":


    # 2. 启动 FastAPI
    # 🌟 核心：在 Docker 中 host 必须是 0.0.0.0，--no-reload 避免重复初始化 JVM
    uvicorn.run(app, host="0.0.0.0", port=9091, access_log=True)