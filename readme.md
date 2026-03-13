# AirDrop Plus / SMS Sync

仓库目前包含两套能力：
1. 原有 AirDrop Plus（iOS 快捷指令 + Windows 文件/剪贴板互传，HTTP）。
2. 新增短信验证码同步系统（Android + Windows，MQTT）。

---

## 新增：多平台短信验证码同步（Android + Windows）

### 架构
- 使用公开 MQTT 服务端（默认示例 `broker.emqx.io`）。
- Android 端：监听短信 -> 提取验证码 -> 发布到 `sms-sync/{app-id}/sms`。
- Windows 端：订阅同一主题 -> 自动复制验证码到剪贴板。
- Android 端同时订阅主题，可接收其他 Android 上传的验证码并复制到本机剪贴板。

### 目录
- `android-client/`：Android Studio 工程，可构建 APK。
- `windows-client/`：Python Windows 客户端。

### 快速开始
#### Windows
```bash
cd windows-client
pip install -r requirements.txt
python sms_sync_client.py --app-id your-room-id --broker broker.emqx.io --port 1883
```

#### Android
```bash
cd android-client
./gradlew assembleDebug
```
生成 APK：`android-client/app/build/outputs/apk/debug/app-debug.apk`

> 注：当前仓库未提交二进制 APK 文件，需在本地 Android SDK 环境构建。

---

## 原项目说明（AirDrop Plus）

用于 iOS 设备和 Windows 电脑之间进行文件传输，基于 HTTP，需要配合快捷指令使用。

### 依赖

```txt
python==3.10.6
flask==3.0.0
win10toast==0.9
psutil==5.9.6
pyinstaller==6.2.0
windows_toasts==1.0.1
```
