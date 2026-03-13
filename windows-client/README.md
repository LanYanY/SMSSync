# Windows 验证码同步客户端

## 功能
- 连接公开 MQTT Broker。
- 接收安卓端上传的短信内容与验证码。
- 自动提取 4~8 位数字验证码并复制到 Windows 剪贴板。
- 若无验证码则复制短信全文。

## 安装
```bash
pip install -r requirements.txt
```

## 运行
```bash
python sms_sync_client.py --app-id your-room-id --broker broker.emqx.io --port 1883 --device-id win-01
```

可选参数：
- `--username` / `--password`
- `--tls`（启用 TLS 时建议端口 8883）

## 打包 EXE
在 Windows 上执行：
```bash
pip install -r requirements.txt pyinstaller==6.11.1
pyinstaller -F -n SmsSyncWindows sms_sync_client.py
```
输出：`dist/SmsSyncWindows.exe`
