# Yc Highlight Setting

一个 IntelliJ 平台插件，自动将代码高亮级别（Highlight）从 **All Problems** 改为 **Syntax**，跳过检查（Inspection），从而提升 IDE 性能。

A plugin that automatically changes the code highlighting level (Highlight) from "All Problems" to "Syntax" to improve IDE performance.

插件主页：[Yc Highlight Setting - JetBrains Marketplace](https://plugins.jetbrains.com/plugin/29028-yc-highlight-setting)

## 功能说明

IDEA 中逐文件的高亮级别设置（`HighlightingSettingsPerFile`）默认为 **All Problems**，在大型项目中会显著拖慢编辑器。本插件在项目启动及文件打开时，自动将目标文件的高亮级别设置为 **Syntax**（跳过 Inspection），无需手动逐个文件修改。

## 工作原理

- 项目启动时（`postStartupActivity`），对已打开的文件应用高亮设置
- 监听 `FileEditorManagerListener`，对后续新打开的文件应用高亮设置
- 通过 `setHighlightingSettingForRoot(psiFile, SKIP_INSPECTION)` 设置高亮级别
- 由于该调用会触发同步的 write action，统一通过 `invokeLater` 延迟到 write-safe 上下文中执行

## 支持的文件类型

以下扩展名的文件会被处理：

- `java`
- `kt`
- `xml`

## 环境要求

- JDK 17
- IntelliJ Platform：基于 `2024.1.7`（IC）构建

## 兼容性

- 最低版本：`241`（IntelliJ IDEA 2024.1）
- 最高版本：`262.*`

## 构建与运行

```bash
# 构建插件
./gradlew buildPlugin

```

构建产物位于 `build/distributions/`。

## 项目结构

```
src/main/kotlin/yc39/ycHighlightSetting/
    SetSyntaxHighlightingStartup.kt   # 核心逻辑：启动活动 + 文件打开监听
src/main/resources/META-INF/
    plugin.xml                        # 插件配置
```

## 相关链接

- [插件配置文件说明](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html)

## 许可证

见 [LICENSE](LICENSE)。
