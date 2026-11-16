# SIMHuber (Android)

结合 **VoHive2** 与 **SIMHub** 核心能力的多功能 Android 工程（Kotlin + Jetpack Compose + Material 3）。

## 已包含模块

1. **USB CCID 智能卡驱动 (`UsbCcidDriver.kt`)**
   - 基于 Android USB Host API (`Class 0x0B`) 实现 `PC_to_RDR_IccPowerOn`、`PC_to_RDR_XfrBlock`、自动 `61 XX` `GET RESPONSE`、读取 `EF_ICCID` 与 `USIM AUTHENTICATE (00 88 00 81)`。
2. **eSIM LPAd 管理引擎 (`LpadEngine.kt`)**
   - 支持 eSTKme Air / 5ber / Xesim / 9eSIM 的 `ISD-R` 选择、Profile 启用/禁用/删除 APDU 构造、`LPA:1$SM-DP+$MatchingID` 解析与原始 APDU 调试台。
3. **VoHive2 远程中继与 MCC 分流 (`VoHiveRelayClient.kt`)**
   - 支持局域网 / Tailscale 对接 OpenStick VoHive2 节点（`/api/status` 探测），以及按 MCC/MNC 自动匹配 SOCKS5/HTTP 上游代理、Cloudflare TURN 与 TUN 开关。
4. **VoWiFi / IMS 引擎与前台保活 (`VoWifiEngine.kt` / `VoWifiForegroundService.kt`)**
   - 3GPP ePDG/IMS 域名构造、IKEv2 + EAP-AKA + IMS 注册状态机、+86/紧急呼叫拦截、实时 Journal 日志流。
5. **短信、通话与卡片账本 (`SimHuberRepository.kt`)**
   - 本地 JSON 持久化存储、多卡槽切换、短信收发记录与套餐保号到期账本。

## 编译生成 APK

### 方式一：GitHub Actions 自动云编译（推荐）
1. 将本工程推送到你的 GitHub 仓库（已内置 `.github/workflows/android-build.yml`）。
2. 在仓库 **Actions** 页面运行 `Build SIMHuber Android APK`，构建完成后在 **Artifacts** 下载 `SIMHuber-debug-apk`。

### 方式二：本地 Android Studio 编译
1. 使用 Android Studio（JDK 17 + Android SDK 34）打开工程根目录。
2. 等待 Gradle Sync 完成后点击 **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**。
