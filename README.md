# WanAndroid-AI

包名：`com.personal.wanandroid`。独立于 CoolMallKotlin 的 Android 多模块项目。

## 当前阶段

**阶段 2：第一版功能开发中。不是第一版完整功能交付。**

已建立：版本目录、Convention Plugins、Hilt、Navigation3 根导航、四套浅深主题、主题偏好持久化、首页/专题/我的入口、网络 DTO/Service/DataSource/Repository、Room 分表与导出 Schema、基础架构检查和 CI 配置。首页已接入真实文章列表与最新 5 条问答轮播；“查看更多”进入支持分页的每日一问列表。当前共有 47 个单元测试。

阶段 1 曾完成 Debug 与未签名 Release 构建、13 个单元测试及完整检查。当前分支改动已通过 Debug 构建、47 个单元测试、Spotless、架构检查和 Lint；Release 尚未针对本次改动复验。用户已反馈此前首页、主题选择及最新文章元信息样式在真机完成基本验证；本次问答轮播和列表页尚待真机验证，无障碍等专项仍待验证。

界面上的“待接入”是明确的占位状态，不是网络加载失败或真实数据。
首页文章列表已完成本地实现和单元测试，并由用户反馈已完成真机基本验证；每日一问轮播和分页列表已完成本地实现及测试，待真机验证。搜索交互、专题状态、WebView 阅读、Cookie 登录、收藏、历史页面和正文缓存均未完成。
多配色主题选择已完成本地实现、自动化验证和用户真机基本验证，提供四套配色与三种显示模式，默认“石板蓝 + 跟随系统”；无障碍等设备专项结果仍待逐项记录。

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
首次远端 CI 的六项检查均已通过；main 已要求 PR、分支同步、讨论解决及六项必需检查，并禁止管理员绕过、强推和删除。
OCR 工作流、规则与日志安全适配器已就绪；经用户确认代码上下文外发和费用边界后，仓库已启用 OCR，并在 PR #1 完成首次真实模型审查，结果为无发现。OCR 仍是辅助检查，不是必需合并门槛。
本工程只保留 debug/release；不使用来源商城的 devDebug/prodRelease 任务。

## 文档

- [开发规则](AGENTS.md)
- [架构与迁移方案](docs/架构与迁移方案.md)
- [第一版验收清单](docs/第一版验收清单.md)
- [验证记录](docs/验证记录.md)
- [主题选择开发方案](docs/主题选择开发方案.md)
- [来源说明](NOTICE.md)
- [CI 与 OCR 配置及验证指南](.github/CI_GUIDE.md)
- [CI/OCR 本地适配验证记录](docs/CI-OCR本地适配记录.md)

## 下一阶段

真机验证每日一问的四秒纵向轮播、后台/Tab 暂停、单条静止、查看更多分页和点击路由；通过后开发在线阅读，再接搜索。不要将占位页面算作已完成的功能。
