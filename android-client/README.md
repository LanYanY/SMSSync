# Android 短信同步客户端

## 功能
- 监听系统短信（Android 5+）。
- 自动提取 4~8 位验证码并通过 MQTT 发布。
- 接收其他 Android 设备发布的验证码并复制到剪贴板。

## 兼容性
- `minSdk 21`（Android 5.0）
- 纯 Java/Kotlin 层实现，无 ABI 限制（可运行于 32 位设备）

## 构建 APK
1. 安装 Android SDK（Platform 34 / Build-tools 34+）。
2. 在项目根目录执行：

```bash
cd android-client
gradle assembleDebug
```

APK 输出：
`app/build/outputs/apk/debug/app-debug.apk`

## 首次运行
1. 打开 App，配置 `MQTT Broker` 与 `app-id`。
2. 点击“连接并开始同步”。
3. 授予短信权限（RECEIVE_SMS/READ_SMS）。
