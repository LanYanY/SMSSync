# Android 短信同步客户端

## 功能
- 监听系统短信（Android 8+）。
- 自动提取 4~8 位验证码并通过 MQTT 发布。
- 接收其他 Android 设备发布的验证码并复制到剪贴板。
- 使用前台服务保持 MQTT 长连接，支持开机自动拉起服务。

## 兼容性
- `minSdk 26`（Android 8.0）
- 纯 Java/Kotlin 层实现，无 ABI 限制（可运行于 32 位设备）

## 构建 APK
1. 安装 Android SDK（Platform 34 / Build-tools 34+）。
2. 在项目根目录执行：

```bash
cd android-client
./gradlew assembleDebug
```

APK 输出：
`app/build/outputs/apk/debug/app-debug.apk`

## 首次运行
1. 打开 App，配置 `MQTT Broker` 与 `app-id`。
2. 点击“启动后台同步”并保持前台通知运行。
3. 授予短信权限（RECEIVE_SMS/READ_SMS）。

## 后台常驻与稳定连接
- 点击“启动后台同步”后会启动前台服务并显示常驻通知。
- 点击“停止后台同步”可主动断开。
- 重启设备后通过 `BOOT_COMPLETED` 自动拉起服务并重连。
- 建议在系统电池优化中将本应用设为“不受限制”，避免被后台清理。

## Release 构建
- 本地 Android APK：`./gradlew assembleDebug`
- 仓库已提供 GitHub Actions 发布流水线：`.github/workflows/release.yml`（打 tag `v*` 后自动构建 APK+EXE 并发布 Release）。
