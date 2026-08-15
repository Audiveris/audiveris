# BookManager 类逐行详解

## 文件信息

- **路径**: `app/src/main/java/org/audiveris/omr/sheet/BookManager.java`
- **行数**: 759 行
- **核心定位**: **单例管理类**，实现 `OmrEngine` 接口，负责管理所有 Book 实例的生命周期、输入/输出路径配置、历史记录。

## 在持久化流程中的角色

BookManager 是**持久化的调度中心**：
- 它是 `OMR.engine` 的实例（`Main.java:247`）
- 负责 Book 的创建、加载、保存路径计算
- 管理最近打开的文件历史
- 配置输出方式（Opus/非Opus、压缩/非压缩）

---

## 核心字段说明

| 字段 | 类型 | 作用 | 行号 |
|------|------|------|------|
| `books` | `Map<Path, Book>` | 所有已加载 Book 实例的映射表（key=绝对路径） | 128 |
| `aliasPatterns` | `AliasPatterns` | 别名模式匹配器 | 131 |
| `imageHistory` | `PathHistory` | 图像文件历史（最近打开） | 137 |
| `bookHistory` | `SheetPathHistory` | 书文件历史（最近加载/保存） | 143 |

---

## 核心方法逐行详解

### `getInstance()` — 获取单例

**行号**: 597-606

```java
public static BookManager getInstance ()
{
    return LazySingleton.INSTANCE;  // 静态内部类实现懒加载单例
}
```

### `loadInput(Path inputPath)` — 从输入图像加载 Book

**行号**: 268-291

```java
public Book loadInput (Path inputPath)
```

| 行号 | 代码 | 释义 |
|------|------|------|
| 271 | `Book book = new Book(inputPath)` | 根据图像路径创建 Book 实例 |
| 274-281 | `AliasPatterns` 检查 | 查找别名，如有则应用 |
| 284 | `book.setModified(true)` | 新书标记为已修改 |
| 285 | `book.setDirty(true)` | 需要更新乐谱 |
| 287 | `addBook(book)` | 加入引擎管理 |
| 289 | `getImageHistory().add(inputPath)` | 记录到历史 |

### `loadBook(Path bookPath)` — 从 .omr 加载 Book

**行号**: 250-263

```java
public Book loadBook (Path bookPath)
```

| 行号 | 代码 | 释义 |
|------|------|------|
| 253 | `Book.loadBook(bookPath)` | 调用 Book 的静态加载方法 |
| 255-258 | 加载成功后 | 设置 bookPath、加入引擎管理 |
| 259 | `getBookHistory().remove(...)` | 从历史中移除（后续会重新添加在关闭时） |

### `addBook(Book book)` — 注册 Book

**行号**: 159-171

```java
public void addBook (Book book)
```

| 行号 | 代码 | 释义 |
|------|------|------|
| 166-167 | 选择 key | 优先使用 bookPath，其次使用 inputPath |
| 169 | `books.put(path.toAbsolutePath(), book)` | 以绝对路径为 key 存入 Map |

### `removeBook(Book book, Integer sheetNumber)` — 移除 Book

**行号**: 304-324

```java
public synchronized boolean removeBook (Book book, Integer sheetNumber)
```

| 行号 | 代码 | 释义 |
|------|------|------|
| 312-313 | `if bookPath != null` | 添加到书历史后从 Map 移除 |
| 317-318 | `if inputPath != null` | 以 inputPath 为 key 从 Map 移除 |

### 路径计算系列方法

这些静态方法是 BookManager 的核心，决定了文件存放在哪里：

| 方法 | 行号 | 返回路径 |
|------|------|----------|
| `getDefaultSavePath(book)` | 571-578 | `bookFolder/radix.omr` |
| `getDefaultBookFolder(book)` | 485-511 | 有 bookPath 则用其父目录，否则按配置选择 |
| `getDefaultExportPathSansExt(book)` | 522-529 | `bookFolder/radix` |
| `getDefaultPrintPath(book)` | 553-560 | `bookFolder/radix-print.pdf` |
| `getActualPath(targetPath, defaultPath)` | 404-427 | 选择实际路径并确保父目录存在 |
| `getBaseFolder()` | 437-446 | CLI 输出目录或配置的 baseFolder |

**关键逻辑** (`getDefaultBookFolder` 行号 485-511):

```
CLI指定输出目录 → 用 CLI 目录
useInputBookFolder → 用输入文件所在目录
useSeparateBookFolders → baseFolder/radix/
否则 → baseFolder/
```

### 配置查询方法

| 方法 | 行号 | 返回值 |
|------|------|--------|
| `useOpus()` | 662-669 | 是否使用 Opus 导出 |
| `useCompression()` | 641-648 | 是否使用 mxl 压缩格式 |
| `useSignature()` | 693-696 | 是否添加 ProxyMusic 签名 |

---

## 与其他类的关联关系

```
OmrEngine (接口)
  ↑ 实现
BookManager (单例) ─── 管理 ─── List/Map<Book>
  │
  ├── AliasPatterns           ─── 别名匹配
  ├── PathHistory             ─── 图像文件历史
  └── SheetPathHistory        ─── 书文件历史
```

---

## 单例实现分析

使用**静态内部类**实现线程安全的懒加载单例：

```java
// 行号 755-758
private static class LazySingleton {
    static final BookManager INSTANCE = new BookManager();
}
```

这种方法（Initialization-on-demand holder idiom）的优势：
- 线程安全（由 ClassLoader 保证）
- 懒加载（仅在首次访问时创建）
- 无需 `synchronized`
