# Trickplay 重建插件

插件源码、测试、安装说明和独立构建 Action 已迁移至：

**[XDorz/jellyfin-plugin-trickplay-rebuild](https://github.com/XDorz/jellyfin-plugin-trickplay-rebuild)**

Findroid 在电影／单集详情页提供管理员专用的“重建预览图”按钮，
通过 Jellyfin 自身鉴权调用插件接口，不触发元数据刷新。

当前插件 1.0.1 支持 Jellyfin **12.0 和 12.1 / .NET 10**。将插件包中的 `TrickplayRebuild`
目录放到 LinuxServer 容器的 `/config/data/plugins/` 后重启 Jellyfin。

[安装和接口说明](https://github.com/XDorz/jellyfin-plugin-trickplay-rebuild/blob/main/README.md) ·
[验证记录及限制](https://github.com/XDorz/jellyfin-plugin-trickplay-rebuild/blob/main/VALIDATION.md)
