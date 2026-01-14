import pika
import json
import os
import io
import shutil
import tempfile
import requests
from minio import Minio
from minio.error import S3Error
from ultralytics import YOLO
import jpype
import jpype.imports

# 1. 配置
MODEL_PATH = 'best.pt'
QUEUE_NAME = 'medical_diagnosis_queue'
RESULT_QUEUE = 'diagnosis_results_queue'
DOWNLOAD_TIMEOUT = 30

# 🌟🌟🌟 核心修改 1：网络配置 🌟🌟🌟

# Docker 内部的主机名 (对应 docker-compose.yml 里的 service name)
# 如果你的 MinIO 服务名叫 minio，就填 minio；如果是 medical-minio，就填 medical-minio
DOCKER_MINIO_HOST = "medical-minio"

# 内部上传用的地址 (Python -> MinIO)
MINIO_INTERNAL_ENDPOINT = f"{DOCKER_MINIO_HOST}:9000"

# 外部前端用的地址 (浏览器 -> MinIO)
MINIO_EXTERNAL_ENDPOINT = "http://localhost:9000"

# --- MinIO 配置 ---
MINIO_CLIENT = Minio(
    MINIO_INTERNAL_ENDPOINT, # 🌟 上传走内网
    access_key="minioadmin",
    secret_key="minioadmin",
    secure=False
)
BUCKET_NAME = "yicheng-medical"

def upload_to_minio(local_file_path, task_id, file_name="result.jpg"):
    try:
        object_name = f"doctor/annotated/{task_id}/{file_name}"

        MINIO_CLIENT.fput_object(
            BUCKET_NAME,
            object_name,
            local_file_path,
            content_type="image/jpeg"
        )
        # 🌟 返回给前端的地址必须是 localhost，否则浏览器打不开
        return f"{MINIO_EXTERNAL_ENDPOINT}/{BUCKET_NAME}/{object_name}"
    except Exception as e:
        print(f" [!] MinIO 上传失败: {str(e)}")
        return None

# --- Java 支持 ---
def init_jpype():
    if not jpype.isJVMStarted():
        jvm_path = jpype.getDefaultJVMPath()
        jpype.startJVM(jvm_path, "-ea")

def parse_java_serialized_map(byte_data):
    try:
        init_jpype()
        from java.io import ByteArrayInputStream, ObjectInputStream
        byte_stream = ByteArrayInputStream(byte_data)
        obj_stream = ObjectInputStream(byte_stream)
        java_map = obj_stream.readObject()
        obj_stream.close()
        py_dict = {}
        for key in java_map.keySet():
            py_dict[str(key)] = str(java_map.get(key))
        return py_dict
    except Exception as e:
        raise ValueError(f"解析Java序列化数据失败：{str(e)}")

# 🌟🌟🌟 核心修改 2：URL 强制转换函数 🌟🌟🌟
def download_image_from_url(image_url):
    try:
        # Java 发来的是 http://localhost:9000/...
        # Docker 内部不认识 localhost，必须转换成 http://medical-minio:9000/...
        internal_url = image_url
        if "localhost" in internal_url:
            internal_url = internal_url.replace("localhost", DOCKER_MINIO_HOST)
        elif "127.0.0.1" in internal_url:
            internal_url = internal_url.replace("127.0.0.1", DOCKER_MINIO_HOST)

        print(f" [🔍下载调试] 原始: {image_url} -> 修正: {internal_url}")

        response = requests.get(
            internal_url, # 🌟 用修正后的地址下载
            timeout=DOWNLOAD_TIMEOUT,
            allow_redirects=True,
            headers={"User-Agent": "Mozilla/5.0"}
        )
        response.raise_for_status()

        # 自动补全后缀，防止文件名没有后缀导致 YOLO 报错
        suffix = os.path.splitext(image_url)[-1]
        if not suffix or len(suffix) > 5: # 如果后缀太长或者没有
            suffix = '.jpg'

        tmp_file = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
        tmp_file.write(response.content)
        tmp_file.close()
        return tmp_file.name
    except Exception as e:
        raise Exception(f"下载网络图片失败：{str(e)}")

# 2. 加载模型
print(f"正在加载 YOLOv11 诊断模型: {MODEL_PATH}...")
model = YOLO(MODEL_PATH)

def process_diagnosis(ch, method, properties, body):
    data = {"task_id": "unknown", "image_path": ""}
    tmp_image_path = None

    try:
        try:
            data = json.loads(body.decode('utf-8', errors='ignore'))
        except json.JSONDecodeError:
            print(" [提示] 消息格式非 JSON，尝试 Java 反序列化...")
            data = parse_java_serialized_map(body)

        task_id = data.get("task_id")
        image_path = data.get("image_path")

        if not task_id or not image_path:
            raise ValueError(f"任务参数不完整：task_id={task_id}")

        print(f" [x] 正在处理任务 {task_id} ...")

        if image_path.startswith(('http://', 'https://')):
            tmp_image_path = download_image_from_url(image_path)
            actual_image_path = tmp_image_path
        else:
            if not os.path.exists(image_path):
                raise FileNotFoundError(f"找不到本地文件: {image_path}")
            actual_image_path = image_path

        # C. 执行 YOLO 推理
        # 建议指定绝对路径，防止目录乱跑
        save_base_dir = "/app/static/annotated_images"
        results = model.predict(
            source=actual_image_path,
            save=True,
            conf=0.25,
            project=save_base_dir,
            name=task_id,
            exist_ok=True
        )

        # D. 提取检测结果
        diagnosis_results = []
        for r in results:
            if hasattr(r, 'boxes') and r.boxes is not None:
                for box in r.boxes:
                    cls_id = int(box.cls[0])
                    label = model.names[cls_id] if cls_id < len(model.names) else "未知"
                    conf = float(box.conf[0])
                    diagnosis_results.append({
                        "label": label,
                        "confidence": f"{conf:.2%}",
                        "box": box.xyxy[0].tolist()
                    })

        # E. 上传带标注的图片到 MinIO

        # 1. 获取 YOLO 结果目录
        save_dir = results[0].save_dir

        # 2. 找到生成的图片文件 (YOLO 可能会把 png 转成 jpg，所以不能只用原来的文件名)
        files = os.listdir(save_dir)
        if not files:
            raise Exception("YOLO 未生成结果图片")

        annotated_local_path = os.path.join(save_dir, files[0])
        print(f" [调试] 标注图片本地路径: {annotated_local_path}")

        # 3. 上传
        minio_url = upload_to_minio(annotated_local_path, task_id, "result.jpg")

        if not minio_url:
            raise Exception("文件上传至 MinIO 失败")

        response = {
            "task_id": task_id,
            "status": "success",
            "findings": diagnosis_results,
            "annotated_image_url": minio_url,
            "message": "诊断完成",
            "original_image_path": image_path
        }

        ch.basic_publish(
            exchange='',
            routing_key=RESULT_QUEUE,
            body=json.dumps(response, ensure_ascii=False)
        )
        print(f" [OK] 任务 {task_id} 处理成功。")

    except Exception as e:
        print(f" [!] 处理出错: {str(e)}")
        error_response = {
            "task_id": data.get("task_id", "unknown"),
            "status": "failed",
            "error": str(e),
            "findings": []
        }
        ch.basic_publish(
            exchange='',
            routing_key=RESULT_QUEUE,
            body=json.dumps(error_response, ensure_ascii=False)
        )
    finally:
        ch.basic_ack(delivery_tag=method.delivery_tag)

        if tmp_image_path and os.path.exists(tmp_image_path):
            try:
                os.unlink(tmp_image_path)
            except: pass

        # 清理 YOLO 临时目录
        try:
            local_task_dir = os.path.join("/app/static/annotated_images", str(task_id))
            if os.path.exists(local_task_dir):
                shutil.rmtree(local_task_dir)
        except: pass

def start_consumer():
    os.makedirs("/app/static/annotated_images", exist_ok=True)
    try:
        # 确保这里 host 是 rabbitmq (docker service name)
        connection = pika.BlockingConnection(pika.ConnectionParameters(
            host='rabbitmq', heartbeat=600
        ))
        channel = connection.channel()
        channel.queue_declare(queue=QUEUE_NAME, durable=True)
        channel.queue_declare(queue=RESULT_QUEUE, durable=True)
        channel.basic_qos(prefetch_count=1)
        channel.basic_consume(queue=QUEUE_NAME, on_message_callback=process_diagnosis)
        print(f" [*] AI 诊断服务启动成功...")
        channel.start_consuming()
    except Exception as e:
        print(f" [!] 启动失败: {str(e)}")
    finally:
        if jpype.isJVMStarted():
            jpype.shutdownJVM()

if __name__ == "__main__":
    start_consumer()