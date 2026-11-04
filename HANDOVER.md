# 画布 · 接手文档

> 面向下一个维护者。读完这篇，你应该能运行、修改、构建和发布这个项目。
>
> 最后更新：2026-11-04 · v0.3.0

## 0. 一分钟版本

- **是什么**：手机优先、单文件、零依赖的节点画布。纸感界面、本地保存、多画布、嵌套分组。
- **仓库**：https://github.com/STUTTEFRFIX/huabu-canvas
- **在线预览**：https://stuttefrfix.github.io/huabu-canvas/
- **最新 APK**：https://github.com/STUTTEFRFIX/huabu-canvas/releases/latest
- **技术栈**：原生 HTML/CSS/JavaScript + Canvas 2D；Android 端是 WebView 壳，无 Gradle。
- **许可证**：MIT。
- **当前状态**：v0.3.0，26 项自测全部通过，CI 与 Pages 正常。

先做这三件事：

1. 浏览器打开 `index.html`，确认基本交互；
2. 地址后加 `?selftest=1`，应看到 26 / 26；
3. 按第 9 节构建一次 APK。

---

## 1. 问题（为什么做这个项目）

这个项目不是「把 Project Graph 搬到手机」，而是解决一个更具体的问题：

> **想画一张关系图时，掏出手机就能画，不用开电脑；而且要像纸一样安静、长期看眼睛不累。**

因此有几个明确取舍：

| 要 | 不要 |
| --- | --- |
| 打开即用，单文件，离线 | 账号、云同步、协作服务端 |
| 手机触屏优先 | 桌面多窗口、鼠标悬停、修饰键 |
| 暖纸色、低对比、细线 | 高饱和、发光、重阴影、玻璃拟态 |
| 节点、连线、嵌套分组 | 富文本、LaTeX、音视频、扩展市场 |
| 自动保存、撤销、多画布 | 复杂工程管理、版本树 |

成功标准只有一条：**自己下次想画图时，愿不愿意打开它。**

---

## 2. 最终形态

- 一个 `index.html` 包含全部 Web 功能，直接打开即可运行。
- Canvas 2D 绘制无限画布；相机支持 1%–12800% 缩放。
- 节点是浅色纸片，连线是轻微弯曲的墨线，选中是细黛青框。
- 双击空白新建节点并就地输入；长按拖动框选；双指缩放平移。
- 多画布：顶部「画布 1」切换、保存、新建；每张画布独立保存节点/连线/分组/视野。
- 嵌套分组：自动包住子内容，可折叠、整组拖动、拖入/拖出换归属。
- 自动保存到 localStorage；JSON 导入导出。
- Android APK：WebView 壳，本地 assets，无网络权限。

完整操作见 [README](README.md)。

---

## 3. 过程（项目是怎么长成现在这样的）

### 3.1 版本时间线

| 版本 | 做了什么 | 关键点 |
| --- | --- | --- |
| v0.1.0 | 单文件可交互原型、纸感视觉、Android WebView 壳、首个 APK | 先把「能画、能存、能打开」跑通 |
| v0.1.1 | 修复缩小到约 30% 时文字消失 | 移除 `fontSize < 5` 的硬阈值 |
| v0.2.0 | 多画布：切换、保存、新建 | 索引 + 每画布独立文档；旧单文档自动迁移 |
| v0.3.0 | 长按框选多选、嵌套分组、挤压关系 | 分组自动包围 + 拖入/拖出换归属；26 项自测 |

### 3.2 开发中真正卡住过的问题

| 问题 | 原因 | 解决 |
| --- | --- | --- |
| 缩小后文字整段消失 | 渲染时 `fontSize < 5` 直接跳过 | 去掉阈值，让文字绘制到亚像素极限 |
| 新建节点提交后位置偏移 | 编辑时宽度 200，提交后缩到实际宽度，左上角没动 | 新建节点提交时保持「以落点为中心」 |
| 多画布切换后数据丢失风险 | 只有一个 `node-canvas-doc-v1` key | 索引 + `node-canvas-doc-v2:<id>`；旧 key 迁移 |
| 嵌套分组在触屏上点不中 | 节点、内层分组、外层分组重叠，没有悬停/右键 | 标题栏拖动 + 点空白选组 + 选框 + 节点菜单选父级 |
| 节点拖不出分组 | 分组自动包住节点，节点永远在框内 | 拖动期间把被拖节点从分组包围盒排除，松手再算归属 |
| Android 顶栏被状态栏压住 | WebView 自己加 padding 不影响网页视口 | 外层 `FrameLayout` 承担 insets，网页 safe-area 置零 |
| 每次构建 APK 签名都变 | keystore 放在会被清理的 `build/` | 移到 `android/debug.keystore` 并保留 |

### 3.3 为什么是单文件

- 手机文件管理器里点开就能用，不需要服务器、安装依赖。
- 备份、迁移、发到网上都只是一个文件。
- 代价是文件较长（三千多行）；如果以后功能继续膨胀，再拆模块，不建议现在就拆。

### 3.4 为什么没有用 Tauri / React

- 目标是手机浏览器与一个极薄的 Android 壳；Tauri 的移动端多窗口、子进程、插件能力与桌面端差异大，收益低。
- 画布是纯 Canvas 2D，没有复杂组件树；原生 DOM + 一个小型状态层已经够用。
- 少一层框架，APK 只有约 40 KB。

---

## 4. 代码地图

```
.
├── index.html                 # Web 应用：样式、画布、存储、交互、自测
├── README.md                  # 用户文档
├── HANDOVER.md                # 本文件
├── preview.png                # README 预览图
├── android/
│   ├── AndroidManifest.xml
│   ├── build.sh               # aapt2 + javac + d8 + apksigner 手动构建
│   ├── res/                   # 纸色主题、自适应图标
│   └── src/.../MainActivity.java
└── .github/workflows/android.yml
```

### 4.1 数据模型

```ts
doc = {
  nodes: [{ id, x, y, w, h, text, color, parentId }],
  edges: [{ id, from, to }],
  groups: [{ id, title, parentId, collapsed }],
}

cam = { x, y, scale } // scale: 0.01 ~ 128
```

- 节点和分组都用世界坐标；没有父子相对坐标。
- `parentId` 只指向分组；分组可以多级嵌套。
- 分组没有自己的 x/y/w/h，大小每次从子内容推导（见 `computeGroupBounds`）。
- 边只连节点，不参与分组归属。

### 4.2 存储

| key | 内容 |
| --- | --- |
| `node-canvas-index-v2` | `{ activeId, items: [{ id, name, updatedAt }] }` |
| `node-canvas-doc-v2:<id>` | 单张画布的 `{ nodes, edges, groups, camera }` |
| `node-canvas-doc-v1` | 旧版单文档，只用于首次迁移 |

- 每次改动 debounce 300ms 保存当前画布，并更新索引。
- `pagehide` 时立即保存。
- `initializeCanvases` 处理：索引加载、缺失文档回退、旧文档迁移、首次种子数据。

### 4.3 渲染顺序

```
纸纹/点阵
→ 分组填充与边框、分组标题
→ 连线（隐藏节点的边跳过）
→ 节点（隐藏节点跳过）
→ 折叠分组的小纸片
→ 选择框（节点/分组）
→ 框选虚线框
```

- 分组包围盒：`computeGroupBounds()` 递归计算；折叠子分组按小纸片尺寸参与父级包围盒。
- 节点/分组隐藏：沿 `parentId` 向上找是否有 `collapsed` 分组。
- 缩放 1% 时箭头会隐藏；节点文字继续绘制。

### 4.4 输入与选择

- 输入统一由 `onPointerDown/Move/Up` 处理，兼容鼠标、触摸、笔。
- 选择状态是 `selectedIds: Set`，可同时包含节点和分组。
- 节点命中优先于分组；分组标题条是拖动/长按热区。
- 长按：
  - 在节点上 → 节点菜单；
  - 在分组标题上 → 分组菜单；
  - 在空白/分组空白上 → 按住拖动进入框选；按住松开打开空白菜单。
- 框选释放时选中与选框相交的可见节点（`nodesInMarquee`）。
- 节点拖动使用 `draggingNodeIds` 从分组包围盒暂时排除；`finishNodeDrag(true)` 松手时重新计算归属。
- 分组拖动移动全部后代节点；点击分组空白只选中分组。

### 4.5 撤销

- 每次提交动作保存整份 `{nodes, edges, groups}` 快照，最多 100 步。
- 相机不进入撤销快照；撤销不会移动视野。
- 新建节点 + 首次输入合并为一条历史；空文本提交会删除并回退那一步。

### 4.6 Android 壳

- `MainActivity` 创建 `FrameLayout` + `WebView`，加载 `file:///android_asset/index.html`。
- 通过 `setOnApplyWindowInsetsListener` 给根布局加状态栏/导航栏 padding；网页里用 `body.android` 把 safe-area 变量置零，避免双重留白。
- 暴露 `HuabuAndroid` JS 接口：
  - `copyText(text)` → 系统剪贴板；
  - `saveText(filename, text)` → Android 10+ 写入 `下载/画布/`，旧系统写应用外部目录。
- `WebChromeClient` 处理 `confirm/alert`，保证「清空画布」确认可用。
- 构建脚本用 aapt2/javac/d8/apksigner，不依赖 Gradle。

---

## 5. 引用项目（Project Graph）

参考项目：https://github.com/graphif/project-graph

### 5.1 借鉴了什么

| Project Graph | 画布 |
| --- | --- |
| Canvas 2D 无限画布 / 相机 | 同思路，重写为更小的实现 |
| `Section` 嵌套分组、自动包围子内容 | 分组模型与自动挤压 |
| 折叠分组隐藏后代 | 折叠小纸片 + 父级重新包围 |
| 拖动分组带动子内容 | 拖动标题带动全部后代节点 |
| 键盘生长节点树 | 没有照搬：手机无键盘，改为双击空白 + 框选 + 工具栏 |
| `.prg` 自包含工程文件 | 没有照搬：改用单画布 JSON + localStorage 索引 |
| GPL-3.0 应用 | 本项目为独立实现，MIT |

重点参考的代码位置（只读参考，未复制）：

- `app/src/core/stage/stageObject/entity/Section.tsx`：`children`、`parentSection`、`adjustLocationAndSize`、`move` 递归移动、折叠隐藏。
- `app/src/core/stage/stageManager/StageManager.tsx`：从序列化数据重建分组树、选择直接父级、归一化多父关系。
- `app/src/core/render/canvas2d/entityRenderer/section/SectionRenderer.tsx`：嵌套分组细边框、折叠块、标题渲染。

### 5.2 没有照搬什么

- 没有复制 Project Graph 的任何源码；画布的代码、视觉、交互、存储格式都是重新实现的。
- 没有做桌面端的碰撞推挤物理、多父归一化、手动调整分组大小、多窗口、Tauri 壳。
- 没有做键盘树、富文本、LaTeX、音视频、扩展系统、协作。
- 本项目是 MIT；如果未来要直接复制 GPL-3.0 代码，整个项目需要重新评估许可证。

---

## 6. 接手后先做什么

1. 打开 `index.html`，把 README 里的操作过一遍。
2. 跑 `?selftest=1`，确认 26 / 26。
3. 看第 4 节的代码地图，重点读：
   - `computeGroupBounds`
   - `onPointerDown / onPointerMove / onPointerUp`
   - `scheduleLongPress`
   - `initializeCanvases / saveNow / applyCanvasData`
4. 构建一次 APK（第 9 节）。
5. 在真机上覆盖安装一次，确认旧数据还在、分组能创建。
6. 改功能前先看第 8 节「常见改动入口」。

---

## 7. 已知限制与下一步

### 已知限制

- 分组自动命名（分组 1、分组 2…），没有重命名和颜色。
- 分组不支持手动拖拽缩放；这是有意设计，手机上不做小尺寸缩放手柄。
- 没有碰撞推挤物理；分组是「自动包围」而不是「物理挤压」。
- 没有图片节点、LaTeX、音视频、富文本、扩展、协作、PWA 离线缓存。
- 极低缩放（约 3% 以下）节点本身只有几像素，文字进入物理极限。
- APK 使用 debug 签名；正式分发前应换成自己的 release keystore。
- 真机中文输入法、软键盘避让、不同厂商 WebView 仍需持续验收。

### 建议的下一步

1. 分组重命名：长按标题 → 输入框；复用现有 `FormWindow` 或自定义小对话框。
2. 分组颜色：复用节点色板，给分组标题/边框加一点纸色。
3. 节点/分组搜索与定位。
4. 图片节点。
5. 如果要做成产品，再考虑 PWA、云同步、协作；不要在此之前引入后端。

---

## 8. 常见改动入口

| 想改什么 | 先看哪里 |
| --- | --- |
| 工具栏加按钮 | `index.html` 的 `<nav class="toolbar">`、`bindEvents`、`updateUI` |
| 加节点类型 | `normalizeNode`、`layoutNode`、`drawNodes`、`serializeDoc`、`snapshotDoc` |
| 改手势 | `onPointerDown/onPointerMove/onPointerUp`、`scheduleLongPress`、`marquee` |
| 改分组行为 | `computeGroupBounds`、`drawGroups`、`hitGroupTitle`、`openGroupMenu`、`finishNodeDrag` |
| 改存储 | `saveNow`、`loadCanvasData`、`applyCanvasData`、`initializeCanvases` |
| 改 Android 外壳 | `android/src/.../MainActivity.java`、`android/build.sh` |
| 改视觉 | `index.html` 顶部 CSS 变量、`COLORS`、`drawNodes/drawGroups` |
| 加自测 | `runSelfTest`，保持 `?selftest=1` 可用 |

注意事项：

- 改完运行 `node --check`（把 `<script>` 提出来）和 `?selftest=1`。
- 不要提交 `android/build/`、`android/assets/`、`android/debug.keystore`、`*.apk`。
- 分组相关改动至少验证：新建、嵌套、折叠、整组拖动、拖入/拖出、刷新后恢复、撤销。

---

## 9. 构建与发版

### 本地构建 APK

```sh
ANDROID_SDK=/path/to/android-sdk bash android/build.sh
```

输出：`android/build/huabu-0.3.0.apk`。

- 需要 JDK 17+、Android SDK platform 36、build-tools。
- 首次构建会在 `android/debug.keystore` 生成调试签名。**请保留这个文件**：删除后新 APK 无法覆盖安装旧版本。
- 脚本会优先使用 PATH 里的 aarch64 工具（Termux/手机环境），否则回退到 SDK build-tools。
- CI 在 `.github/workflows/android.yml`，每次 push 到 main 构建 artifact。

### 发版流程

1. 改版本号：
   - `android/AndroidManifest.xml` 的 `versionCode` / `versionName`
   - `android/build.sh` 的 `VERSION`
   - `README.md` 里的 APK 文件名
2. 跑自测，构建 APK，真机覆盖安装。
3. 提交并推送 main。
4. 创建 Release 并附 APK：

```sh
gh release create v0.3.0 android/build/huabu-0.3.0.apk \
  --repo STUTTEFRFIX/huabu-canvas \
  --title "画布 v0.3.0" \
  --notes-file release-notes.md
```

5. 检查 Release 资产与本地 SHA-256 一致；Pages 会自动部署 Web 版。
6. 保留 `android/debug.keystore` 的离线备份。

---

## 10. 联系与致谢

- 维护者：[@STUTTEFRFIX](https://github.com/STUTTEFRFIX)
- 参考项目：[graphif/project-graph](https://github.com/graphif/project-graph)（GPL-3.0）
- 本项目：[huabu-canvas](https://github.com/STUTTEFRFIX/huabu-canvas)（MIT）

如果这份文档和代码不一致，以代码和 `?selftest=1` 的结果为准；请顺手把文档改回来。
