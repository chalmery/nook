# Nook

基于 JavaFX 的跨平台播客客户端。

## 功能

- **RSS 订阅管理** — 输入播客 RSS 地址即可订阅，自动解析单集列表
- **音频播放** — 基于 VLC 引擎，支持播放/暂停、倍速（0.5x–2.0x）、音量调节、进度拖拽
- **订阅持久化** — 订阅列表自动保存至 `~/.nook/subscriptions.json`

## 技术栈

| 组件 | 方案 |
|------|------|
| UI | JavaFX + AtlantaFX (PrimerLight 主题) |
| 音频 | VLCJ (libvlc) |
| RSS 解析 | Rome |
| 序列化 | Gson |
| 构建 | Maven + javafx-maven-plugin |

## 环境要求

- JDK 23+
- Maven 3.9+
- VLC 3.x 或 4.x

### 安装 VLC

**Fedora / RHEL**
```bash
sudo dnf install vlc
```

**Debian / Ubuntu**
```bash
sudo apt install vlc
```

**macOS**
```bash
brew install vlc
```

**Windows**
从 [videolan.org](https://www.videolan.org/vlc/) 下载安装，确保 VLC 安装目录在 `PATH` 中。

## 运行

```bash
git clone <repo-url>
cd nook
mvn javafx:run
```

## 项目结构

```
src/main/java/top/yangcc/
├── NookApp.java              # 应用入口
├── model/
│   ├── Podcast.java          # 播客数据模型
│   └── Episode.java          # 单集数据模型
├── rss/
│   └── RssParser.java        # RSS 解析
├── player/
│   └── AudioPlayer.java      # VLC 播放器封装
├── service/
│   └── SubscriptionManager.java  # 订阅管理 + 持久化
└── ui/
    ├── MainLayout.java       # 三栏主布局
    ├── SidebarView.java      # 左侧订阅列表
    ├── EpisodeListView.java  # 中间单集列表
    ├── DetailView.java       # 右侧单集详情
    ├── PlayerBar.java        # 底部播放栏
    └── AddSubscriptionDialog.java  # 添加订阅对话框
```

## 字体配置

跨平台字体栈，首个存在的字体生效：

| 平台 | 字体 |
|------|------|
| Windows | Microsoft YaHei (微软雅黑) |
| macOS | PingFang SC (苹方) |
| Linux | Noto Sans CJK SC (思源黑体) |

## 待实现

- [ ] 播客封面图片展示
- [ ] 发现 / 热门推荐
- [ ] 离线下载
- [ ] 播放历史
- [ ] 章节导航
- [ ] 设置页面
