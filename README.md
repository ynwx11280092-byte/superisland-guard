# 超级岛防盗守卫（SuperIslandGuard）

一个面向 **澎湃 4 / HyperOS 4** 的 **LSPosed 模块**：检测到进入小米「应用锁」页面
（`com.miui.securitycenter/com.miui.applicationlock.AppLockActivity`）时，触发**前置摄像头
明示式抓拍 / 录像**，并用 **HyperIsland 的超级岛方案**弹出含「时间 / 数量」的通知。
支持**柔光 / 液态玻璃**两种 UI 切换，可配置时间或数量，并针对后台做了省电优化。

> ⚠️ **使用边界**：本项目是「明示式防盗取证」——拍摄全程有**快门声 + 全屏闪光 +
> 常驻通知 + 超级岛通知 + 系统相机隐私绿点**等可见提示，**仅限安装在你自己的设备上**
> 用于防丢 / 防盗。请勿把它改装成隐蔽拍摄工具，那在你所在地区通常违法。

---

## 1. 方案原理（数据流）

```
小米安全中心进程 (com.miui.securitycenter)
   └─ LSPosed 注入 MainHook → AppLockHook hook Activity.onResume
        └─ 检测到 AppLockActivity 进入
             └─ ContentResolver.call → TriggerProvider (本应用)
                  └─ GuardService（常驻守护，specialUse 前台类型）
                       └─ 检查开关 + 相机权限
                            └─ 拉起 CaptureService（camera 前台类型）
                                 ├─ CaptureEngine 打开前置摄像头
                                 │    ├─ 连续抓拍：每 intervalSec 一张，直到 maxCount / maxDurationSec
                                 │    └─ 录像：无音频 MP4，直到 maxDurationSec
                                 ├─ 可见提示：快门声 + 全屏闪光 + 常驻「正在取证」通知
                                 ├─ Notifier 发标准通知 → HyperIsland 渲染成原生超级岛
                                 └─ IslandOverlay 独立岛兜底（柔光 / 液态玻璃）
```

**与 HyperIsland 的关系（“调用”方式）**：本模块不重复造 HyperIsland 那一大套
SystemUI hook，而是**把抓拍状态发成标准 Android 通知**。已安装的
[HyperIsland](https://github.com/1812z/HyperIsland) 会把通知拦截并渲染成**原生超级岛**，
其**柔光（Soft Glass）/ 液态玻璃（Liquid Glass）**材质由 HyperIsland 提供；本模块的
「岛 UI 风格」设置与之对应。未装 HyperIsland 时，用本模块自带的 `IslandOverlay`
轻量岛兜底（同样两种材质）。

---

## 2. 方案效果（装好后的实际表现）

1. 手机进入**应用锁**解锁界面的一瞬间，前置摄像头开始工作；
2. 屏幕**闪一下白屏**、响起**快门声**、震动一次，顶部弹出**超级岛**提示
   「已抓拍 1 张 · 0s」… 随着拍摄推进实时更新为「已抓拍 N 张 · T 秒」；
3. 通知栏出现**常驻「正在明示取证拍摄」**通知（张数/秒数实时变化），系统顶部显示
   **相机隐私绿点**；
4. 达到**数量上限**（默认 8 张）或**时间上限**（默认 30 秒）后自动停止，岛显示
   「取证完成：8 项 / 30s」后自动收起；
5. 照片写入公共相册 `DCIM/SuperIslandGuard/`，可在相册直接查看；录像模式写 MP4。

---

## 3. 目录结构

```
superisland-guard/
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── build.sh                      # 一键构建脚本
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml   # LSPosed 标记 + 权限 + 双服务
        ├── assets/xposed_init
        ├── res/ ...              # 字符串/主题/图标/偏好设置
        └── java/com/guard/superisland/
            ├── MainHook.kt              # Xposed 入口
            ├── TriggerProvider.kt       # 安全中心 → 本应用触发通道
            ├── hook/AppLockHook.kt      # 应用锁检测
            ├── service/GuardService.kt  # 常驻守护（省电）
            ├── service/CaptureService.kt# 抓拍服务（可见提示）
            ├── camera/CaptureEngine.kt  # 前置摄像头 Camera2 引擎
            ├── notify/Notifier.kt       # 通知 + HyperIsland 桥
            ├── island/IslandStyle.kt    # 柔光 / 液态玻璃材质
            ├── island/IslandOverlay.kt  # 独立岛兜底
            ├── config/GuardConfig.kt    # 时间/数量/主题等配置
            ├── ui/MainActivity.kt       # 设置界面
            └── util/ ...                # 闪光/存储/广播/开机
```

---

## 4. 构建

需要 **JDK 17+**（HyperIsland 用 JDK 21，本模块 17 即可）。任选其一：

### 方式 A：一键脚本（Ubuntu / 本机 proot）

```bash
cd superisland-guard
chmod +x build.sh
./build.sh
```

脚本会自动安装 JDK 17、下载 Gradle 8.7 与 Android cmdline-tools（`platforms;android-34`、
`build-tools;34.0.0`），然后产出可安装的 debug APK。

### 方式 B：手动（Android Studio / 已有 SDK）

1. 用 Android Studio 打开本目录，等 Gradle 同步；
2. `Build → Build APK(s)`，或命令行：
   ```bash
   gradle :app:assembleDebug      # 可安装（debug 签名）
   gradle :app:assembleRelease    # 未签名，需自行签名
   ```

产物路径：`app/build/outputs/apk/debug/app-debug.apk`。

---

## 5. 安装与激活（LSPosed）

1. 安装 `app-debug.apk`；
2. 打开 **LSPosed 管理器 → 模块**，启用「超级岛防盗守卫」；
3. **作用域**勾选 `com.miui.securitycenter`（检测用）和 `com.guard.superisland`（可选）；
4. 重启（或重启 SystemUI + 安全中心进程）使 hook 生效；
5. 打开本应用：
   - 授予**相机**权限、**通知**权限；
   - 点击「开启悬浮窗权限」授予**显示在其他应用上层**（闪光 + 独立岛兜底需要）；
   - 顺手在省电设置里把本应用设为「无限制」，避免被 HyperOS 后台杀掉；
6. 点「立即测试抓拍一张」验证。

---

## 6. 配置项（可指定时间 / 数量）

| 配置 | 默认 | 说明 |
| --- | --- | --- |
| 启用守护 | 开 | 总开关 |
| 取证方式 | 连续抓拍 | 抓拍 或 录像 |
| 抓拍间隔 | 2s | 每张照片间隔（抓拍模式） |
| 数量上限 | 8 张 | 达到即停 |
| 时间上限 | 30s | 到点即停（抓拍/录像共用） |
| 岛 UI 风格 | 液态玻璃 | 柔光 / 液态玻璃 |
| 屏幕闪光 | 开 | 全屏白闪可见提示 |
| 快门声 | 开 | 系统快门声 |
| 存入相册 | 开 | 写 `DCIM/SuperIslandGuard/` |

---

## 7. 省电优化（已做的设计）

- **空闲零功耗**：`GuardService` 只挂一个低优先级常驻通知，**不开摄像头、不持唤醒锁**；
- **按需开摄像头**：仅在触发时才拉起 `CaptureService`（`camera` 前台类型），结束后
  `stopSelf()` 立即释放摄像头；
- **唤醒锁限时**：只在实际抓拍期间持有 `PARTIAL_WAKE_LOCK`，且按「时间上限 + 10s」封顶，
  用 `acquire(timeout)` 自动释放；
- **无音频录像**：录像不采麦克风，更省电也更克制；
- **单后台线程**：所有相机操作在 `HandlerThread` 上，主线程零负担。

---

## 8. 已知限制 / 免责声明

- HyperOS 4 的 AppLockActivity 类名若在后续系统版本变更，需同步更新
  `AppLockHook.TARGET_ACTIVITY`；当前按 `com.miui.applicationlock.AppLockActivity` 适配。
- 「原生超级岛」渲染依赖 HyperIsland；未安装时走本模块的轻量岛兜底。
- 屏幕闪光 / 独立岛需要悬浮窗权限；被拒绝时仍保留快门声 + 通知 + 相机绿点提示。
- 本项目仅供设备所有者对**自己的设备**做防盗取证，请遵守当地法律。

---

## 9. 许可证

本项目源码按 MIT 协议提供；它独立实现，不包含 HyperIsland 代码。
HyperIsland 版权归 [1812z/HyperIsland](https://github.com/1812z/HyperIsland) 所有。
