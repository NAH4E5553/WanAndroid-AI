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
