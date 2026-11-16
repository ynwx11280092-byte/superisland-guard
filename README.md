# 超级岛防盗守卫（SuperIslandGuard）

一个面向 **澎湃 4 / HyperOS 4** 的 **LSPosed 模块**：检测到进入小米「应用锁」页面
（`com.miui.securitycenter/com.miui.applicationlock.AppLockActivity`）时，触发**前置摄像头
明示式抓拍 / 录像**，并把结果**直接投到 HyperOS 系统原生超级岛**（非悬浮窗），显示
「张数 / 秒数」。可配置时间或数量，后台省电，界面展示 Root / LSPosed 状态。

> ⚠️ **使用边界**：本项目是「明示式防盗取证」——拍摄全程有**快门声 + 全屏闪光 +
> 常驻通知 + 原生超级岛 + 系统相机隐私绿点**等可见提示，**仅限安装在你自己的设备上**
> 用于防丢 / 防盗。请勿改装成隐蔽拍摄工具，那在你所在地区通常违法。

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
                                 ├─ CaptureEngine 打开前置摄像头（抓拍/录像）
                                 ├─ 可见提示：快门声 + 全屏闪光 + 震动
                                 └─ Notifier 用官方 hyperisland_kit 发「焦点通知」
                                      └─ HyperOS SystemUI 原生渲染成超级岛
```

**原生超级岛（关键）**：不再用悬浮窗。通过官方库
[`io.github.d4viddf:hyperisland_kit`](https://repo1.maven.org/maven2/io/github/d4viddf/hyperisland_kit/)
（HyperIsland 同款）构建「焦点通知」——它携带 `miui.focus.param` 等系统级 extras，
由 HyperOS 的 SystemUI **原生**把通知渲染到超级岛；设备不支持时自动降级为普通通知。

---

## 2. 方案效果（装好后的表现）

1. 进入**应用锁**解锁界面的一瞬间，前置摄像头开始工作；
2. 屏幕**闪一下白屏**、响起**快门声**、震动一次，顶部**原生超级岛**弹出
   「正在明示取证拍摄 · 已抓拍 1 张 · 用时 0 秒」，随拍摄实时刷新张数/秒数；
3. 通知栏出现**常驻「正在明示取证拍摄」**通知，系统顶部显示**相机隐私绿点**；
4. 达到**数量上限**（默认 8 张）或**时间上限**（默认 30 秒）后自动停止；
5. 照片写入公共相册 `DCIM/SuperIslandGuard/`，录像写 MP4。

---

## 3. 目录结构

```
superisland-guard/
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── build.sh / .github/workflows/build.yml
└── app/src/main/
    ├── AndroidManifest.xml          # LSPosed 标记 + 双服务
    ├── assets/xposed_init
    ├── res/ ...                     # 布局/字符串/偏好设置
    └── java/com/guard/superisland/
        ├── MainHook.kt              # Xposed 入口
        ├── TriggerProvider.kt       # 触发 + hookAlive 上报
        ├── hook/AppLockHook.kt      # 应用锁检测
        ├── service/GuardService.kt  # 常驻守护（省电）
        ├── service/CaptureService.kt# 抓拍服务（可见提示）
        ├── camera/CaptureEngine.kt  # 前置摄像头 Camera2
        ├── notify/Notifier.kt       # 原生超级岛通知（hyperisland_kit）
        ├── config/GuardConfig.kt    # 时间/数量等配置
        ├── ui/MainActivity.kt       # 设置 + 状态卡
        └── util/ ...                # 闪光/存储/广播/开机/RootStatus
```

---

## 4. 构建

需要 **JDK 17+**。任选其一：

- **一键脚本**：`chmod +x build.sh && ./build.sh`（自动装 JDK17 + Gradle 8.7 + SDK，产 debug APK）
- **Android Studio**：打开目录 → Build APK
- **GitHub Actions**：推到仓库自动构建，产物在 Actions → Artifacts

产物：`app/build/outputs/apk/debug/app-debug.apk`。

---

## 5. 安装与激活（LSPosed）

1. 安装 `app-debug.apk`；
2. **LSPosed 管理器 → 模块** 启用「超级岛防盗守卫」；
3. **作用域**勾选 `com.miui.securitycenter`（检测用）；
4. 重启（或重启安全中心进程）使 hook 生效；
5. 打开本应用授权：**相机**、**通知**；如需全屏闪光，点「开启悬浮窗权限」；
6. **焦点通知权限**（上原生岛需要）：HyperOS 设置 → 通知 → 本应用 → 打开
   「焦点通知 / 超级岛」；未开启时自动降级为普通通知；
7. 省电设置里把本应用设为「无限制」，避免被 HyperOS 后台杀掉；
8. 点「立即测试抓拍一张」验证。

---

## 6. 配置项（可指定时间 / 数量）

| 配置 | 默认 | 说明 |
| --- | --- | --- |
| 启用守护 | 开 | 总开关 |
| 取证方式 | 连续抓拍 | 抓拍 或 录像 |
| 抓拍间隔 | 2s | 每张照片间隔（抓拍模式） |
| 数量上限 | 8 张 | 达到即停 |
| 时间上限 | 30s | 到点即停（抓拍/录像共用） |
| 屏幕闪光 | 开 | 全屏白闪可见提示 |
| 快门声 | 开 | 系统快门声 |
| 存入相册 | 开 | 写 `DCIM/SuperIslandGuard/` |

---

## 7. 省电优化（已做的设计）

- **空闲零功耗**：`GuardService` 只挂低优先级常驻通知，不开摄像头、不持唤醒锁；
- **按需开摄像头**：仅触发时才拉起 `CaptureService`（`camera` 前台类型），结束 `stopSelf()`
  立即释放摄像头；
- **唤醒锁限时**：只在实际抓拍期间持有 `PARTIAL_WAKE_LOCK`，按「时间上限 + 10s」封顶自动释放；
- **无音频录像**：不采麦克风，更省电也更克制；
- **单后台线程**：相机操作都在 `HandlerThread`，主线程零负担。

---

## 8. 已知限制 / 免责声明

- 原生超级岛依赖 HyperOS 的「焦点通知」机制；未授予焦点通知权限或非 HyperOS 设备时，
  自动降级为普通通知（仍保留全部明示提示）。
- HyperOS 4 的 AppLockActivity 类名若变更，需同步更新 `AppLockHook.TARGET_ACTIVITY`。
- 全屏闪光需要悬浮窗权限；被拒绝时仍保留快门声 + 通知 + 相机绿点。
- 本项目仅供设备所有者对**自己的设备**做防盗取证，请遵守当地法律。

---

## 9. 许可证

本项目源码按 MIT 协议提供。原生岛构建库
[`hyperisland_kit`](https://github.com/d4viddf/hyperisland-kit) 版权归其作者所有。
