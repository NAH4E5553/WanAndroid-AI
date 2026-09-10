# WanAndroid-AI

包名：`com.personal.wanandroid`。独立于 CoolMallKotlin 的 Android 多模块项目。

## 当前阶段

**阶段 1：架构与可编译骨架。不是第一版完整功能交付。**

已建立：版本目录、Convention Plugins、Hilt、Navigation3 根导航、深浅主题、首页/专题/我的入口、网络 DTO/Service/DataSource/Repository、Room 分表与导出 Schema、13 个单元测试、基础架构检查和 CI 配置。

本阶段 Debug 与未签名 Release 构建、13 个单元测试、Spotless、架构检查和 Lint（0 错误、23 条版本更新提示）通过。尚未真机运行。

界面上的“待接入”是明确的占位状态，不是网络加载失败或真实数据。
首页网络列表、问答轮播、搜索交互、专题状态、WebView 阅读、Cookie 登录、收藏、历史页面和正文缓存均未完成。

## 开发入口

1. Android Studio 打开本目录（不要打开或改动来源商城工程）。
2. 使用 JDK 17、Android SDK 36；本机 SDK 路径在被 Git 忽略的 local.properties。
3. 选择 app，运行 debug 变体。

```bash
./gradlew verifyArchitecture spotlessCheck :app:assembleDebug testDebugUnitTest lintDebug
```

仅格式化新工程：`./gradlew spotlessApply`。
Release 验证：`./gradlew :app:assembleRelease`，默认不配置正式签名。

CI 使用相同 Gradle 检查，另有配置回归测试和工作流语法校验；见 [.github/CI_GUIDE.md](.github/CI_GUIDE.md)。
origin 指向 https://github.com/NAH4E5553/WanAndroid-AI.git；初始版本包含工程骨架、CI/OCR 配置及验证文档。
远端 CI 结果以仓库 Actions 页面为准；本地验证不代表 GitHub CI 已通过，分支保护需另行配置。
OCR 工作流、规则与日志安全适配器已就绪，默认关闭；启用和真实模型验证需单独确认。
本工程只保留 debug/release；不使用来源商城的 devDebug/prodRelease 任务。

## 文档

- [开发规则](AGENTS.md)
- [架构与迁移方案](docs/架构与迁移方案.md)
- [第一版验收清单](docs/第一版验收清单.md)
- [验证记录](docs/验证记录.md)
- [来源说明](NOTICE.md)
- [CI 与 OCR 配置及验证指南](.github/CI_GUIDE.md)
- [CI/OCR 本地适配验证记录](docs/CI-OCR本地适配记录.md)

## 下一阶段

完成首页 UI → ViewModel → Repository 的真实请求链路，先覆盖请求竞争，再接问答循环、搜索与阅读；不要将占位页面算作已完成的功能。
