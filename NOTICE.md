# 来源说明

本项目的 Gradle Wrapper 复制自 CoolMallKotlin（Joker.X，MIT），构建约定的组织方式与主题 Token 参考该项目。
来源版本：cf5029bfc2dc1a196ba4503787311e388b2a91a4。
CI 变更分类脚本与 OpenCodeReview 工作流参考该来源的对应配置，在新项目中按 debug/release、架构检查与新业务规则改造。
保留 LICENSE 中原始版权和许可声明。商城业务、图片、签名与第三方 SDK 不在复用范围内。

Gradle 与各第三方依赖分别受其自身许可约束。

OCR 日志适配基于 Alibaba open-code-review，固定来源提交为
8d023aafcec05f8ba5628fca3eaba88078e5d201，Copyright 2026 alibaba/open-code-review Contributors。
准备脚本按 SHA-256 验证并生成修改版 Action/评论辅助脚本，新增输出抑制、基线加载和临时结果清理；不改变上游项目。
上游使用 Apache-2.0，完整许可见 .github/licenses/open-code-review-LICENSE。

2026-09-12 基线迁移扩大了适配范围（来源版本不变，MIT/Joker.X）：
- 来源 Result/ResultHandler 职责 → core/result/DataResult 与 core/data/WanResponseMapper；协议/展示职责分离。
- 来源 BaseNetWorkListViewModel → core/common/PagingController、PagedUiState、BaseNetworkListViewModel；改为服务端游标、原子状态和组合适配。
- 来源 BaseNetWorkListView、RefreshContent、PageLoading/EmptyNetwork/EmptyData/LoadMore → core/ui/NetworkListPage、NetworkStates；保留 WanAndroid 视觉及 Material 手势。
- 来源 AppScaffold、AppListItem → core/ui 同名组件；裁剪未使用参数、明确 padding、可选点击语义。
- 来源 Color/Shape/Size/Theme 组织方式 → WanColor/WanShape/WanSpacing/WanTheme；保留 WanAndroid 四套完整浅深色值和 DataStore。
- 来源 NavigationService、AppNavigator、Feature Graph → NavigationDispatcher、带 entry ID 的 AppRoute、各 Feature Graph；删除全局服务与点击缓存，增加 Host token 校验。
这些是基于来源职责与代码进行的适配/优化，不宣称文件逐字一致。完整原始路径、实际调用和差异见 docs/基线差异清单.md。原 MIT 版权和许可继续保留于 LICENSE。

2026-09-12 在线阅读沿用同一来源版本：
- feature/common 的 util/WebUrlPolicy.kt、util/WebViewSecurity.kt → feature/article/ReaderUrlPolicy.kt、ReaderWebView.kt；适配多来源 HTTPS 文章、外部跳转确认及 WebView 生命周期。
- feature/common 的 view/WebScreen.kt、viewmodel/WebViewModel.kt → feature/article/ArticleScreen.kt、ArticleViewModel.kt；复用页面分层、进度与释放职责，状态收敛为单一快照并增加过期回调、超时及安全 URL 恢复保护。
不复制商城原生桥、业务凭据或 SDK。
