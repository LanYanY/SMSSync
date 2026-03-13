import argparse
import json
import re
import signal
import sys
import threading
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Optional

import paho.mqtt.client as mqtt
import pyperclip


CODE_PATTERN = re.compile(r"(?<!\d)(\d{4,8})(?!\d)")


@dataclass
class ClientConfig:
    broker: str
    port: int
    username: Optional[str]
    password: Optional[str]
    app_id: str
    device_id: str
    tls: bool


class SmsSyncClient:
    def __init__(self, cfg: ClientConfig):
        self.cfg = cfg
        self.client = mqtt.Client(client_id=f"win-{cfg.device_id}", clean_session=True)
        self.stop_event = threading.Event()
        self.last_payload = None

        if cfg.username:
            self.client.username_pw_set(cfg.username, cfg.password)
        if cfg.tls:
            self.client.tls_set()

        self.client.on_connect = self._on_connect
        self.client.on_message = self._on_message
        self.client.on_disconnect = self._on_disconnect

    @property
    def topic_sms(self) -> str:
        return f"sms-sync/{self.cfg.app_id}/sms"

    @property
    def topic_presence(self) -> str:
        return f"sms-sync/{self.cfg.app_id}/presence"

    def _on_connect(self, client, userdata, flags, rc):
        if rc != 0:
            print(f"[ERROR] MQTT连接失败: rc={rc}")
            return
        print("[INFO] MQTT 已连接")
        self.client.subscribe(self.topic_sms, qos=1)
        self.publish_presence("online")

    def _on_disconnect(self, client, userdata, rc):
        if rc != 0 and not self.stop_event.is_set():
            print("[WARN] 连接断开，等待自动重连...")

    def _on_message(self, client, userdata, msg):
        try:
            data = json.loads(msg.payload.decode("utf-8"))
        except Exception as exc:
            print(f"[WARN] 非法消息: {exc}")
            return

        payload_key = (data.get("source_device"), data.get("timestamp"), data.get("sms_body"))
        if payload_key == self.last_payload:
            return
        self.last_payload = payload_key

        code = data.get("code") or self.extract_code(data.get("sms_body", ""))
        source = data.get("source_device", "unknown")
        sms_body = data.get("sms_body", "")
        print(f"\n[SMS] 来自设备: {source}")
        print(f"[SMS] 时间: {data.get('timestamp')}")
        print(f"[SMS] 内容: {sms_body}")

        if code:
            pyperclip.copy(code)
            print(f"[OK] 检测到验证码: {code} (已复制到剪贴板)")
        else:
            pyperclip.copy(sms_body)
            print("[OK] 未检测到纯数字验证码，已复制全文到剪贴板")

    @staticmethod
    def extract_code(text: str) -> Optional[str]:
        match = CODE_PATTERN.search(text)
        return match.group(1) if match else None

    def publish_presence(self, status: str):
        payload = {
            "device_type": "windows",
            "device_id": self.cfg.device_id,
            "status": status,
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }
        self.client.publish(self.topic_presence, json.dumps(payload, ensure_ascii=False), qos=1, retain=False)

    def run(self):
        self.client.connect(self.cfg.broker, self.cfg.port, keepalive=60)
        self.client.loop_start()
        try:
            while not self.stop_event.is_set():
                time.sleep(0.2)
        finally:
            self.publish_presence("offline")
            time.sleep(0.1)
            self.client.loop_stop()
            self.client.disconnect()

    def stop(self, *_):
        self.stop_event.set()


def parse_args():
    parser = argparse.ArgumentParser(description="SMS MQTT Sync Windows Client")
    parser.add_argument("--broker", default="broker.emqx.io")
    parser.add_argument("--port", type=int, default=1883)
    parser.add_argument("--username", default=None)
    parser.add_argument("--password", default=None)
    parser.add_argument("--app-id", required=True, help="共享通道ID，同一系统设备需一致")
    parser.add_argument("--device-id", default="windows-main")
    parser.add_argument("--tls", action="store_true", help="启用TLS")
    return parser.parse_args()


def main():
    args = parse_args()
    cfg = ClientConfig(
        broker=args.broker,
        port=args.port,
        username=args.username,
        password=args.password,
        app_id=args.app_id,
        device_id=args.device_id,
        tls=args.tls,
    )

    app = SmsSyncClient(cfg)
    signal.signal(signal.SIGINT, app.stop)
    signal.signal(signal.SIGTERM, app.stop)

    print("[INFO] 启动 Windows 验证码同步客户端")
    print(f"[INFO] Broker={cfg.broker}:{cfg.port}, Topic=sms-sync/{cfg.app_id}/sms")
    app.run()
    print("[INFO] 已退出")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(0)
