# SheetManager 类逐行详解

## 文件信息

当前 Audiveris 代码库中**没有 `SheetManager.java`** 这个独立文件。Sheet 的管理职责分布在以下类中：

| 职责 | 实现类 | 文件路径 |
|------|--------|----------|
| Sheet 的创建/加载/保存 | `Book` (静态方法 `loadBook`) + `SheetStub.getSheet()` | `Book.java`, `SheetStub.java` |
| Sheet 的 UI 管理 | `StubsController` | `sheet/ui/StubsController.java` |
| Sheet 的步骤处理 | `SheetStub.reachStep()` | `SheetStub.java` |

## 为什么没有 SheetManager

Audiveris 的设计将 Sheet 的管理职责**委托给其上级和代理对象**：

```
BookManager (全局管理者)
  └── Book (书的集合)
        └── SheetStub (轻量代理，常驻内存)
              └── Sheet (重量级对象，按需加载)
```

- `Book` 负责 `createStubs()`、`loadBook()` 等生命周期操作
- `SheetStub` 负责 `getSheet()`（懒加载）、`storeSheet()`、`swapSheet()`、`reachStep()` 等
- 这种设计避免了需要一个单独的 "SheetManager" 类

---

## 核心 Sheet 管理功能在各类的分布

### 1. Sheet 创建 — `Book.createStubs()`

**Book.java:549-566**

```java
public void createStubs ()
{
    final ImageLoading.Loader loader = ImageLoading.getLoader(path);
    if (loader != null) {
        final int imageCount = loader.getImageCount();
        loader.dispose();
        for (int i = 1; i <= imageCount; i++) {
            stubs.add(new SheetStub(this, i));  // 创建轻量 stub
        }
    }
}
```

→ 不立即创建 Sheet，只创建 SheetStub（享元模式）。

### 2. Sheet 加载 — `SheetStub.getSheet()`

**SheetStub.java:934-1009**

```java
public Sheet getSheet ()
{
    if (sheet != null) return sheet;          // 内存中有，直接返回

    synchronized (this) {
        if (sheet != null) return sheet;      // 双重检查锁定

        if (!isDone(OmrStep.LOAD)) {
            return sheet = new Sheet(this, null, false);  // 未处理过，从头创建
        }

        // 已处理过，从 .omr ZIP 反序列化
        book.getLock().lock();
        sheetFile = book.openSheetFolder(number).resolve(Sheet.getSheetFileName(number));
        try (InputStream is = Files.newInputStream(sheetFile)) {
            sheet = Sheet.unmarshal(is);       // JAXB 反序列化
        }
        sheetFile.getFileSystem().close();
        book.getLock().unlock();

        sheet.afterReload(this);               // 重建瞬态引用
        return sheet;
    }
}
```

### 3. Sheet 保存 — `SheetStub.storeSheet()`

**SheetStub.java:1617-1636**

```java
public void storeSheet () throws Exception
{
    if (isModified() || isUpgraded()) {
        book.getLock().lock();
        Path root = ZipFileSystem.open(bookPath);
        book.storeBookInfo(root);              // 保存 book.xml
        Path sheetFolder = root.resolve(INTERNALS_RADIX + getNumber());
        sheet.store(sheetFolder, null);         // 保存 sheetNNN.xml + PNG
        root.getFileSystem().close();
        book.getLock().unlock();
    }
}
```

### 4. Sheet 交换 — `SheetStub.swapSheet()`

**SheetStub.java:1646-1675**

```java
public void swapSheet ()
{
    if (isModified() || isUpgraded()) {
        storeSheet();              // 先保存
    }
    if (sheet != null) {
        sheet = null;              // 释放 Sheet 对象（GC可回收）
        Memory.gc();               // 触发 GC
    }
    // UI 标记更新
}
```

→ 这是内存管理的核心：处理完一个 Sheet 后释放内存，需要时再加载。

---

## 等效功能对照表（如果存在 SheetManager 的责任）

| 功能 | 实际实现位置 |
|------|-------------|
| Sheet 实例的创建 | `SheetStub.getSheet()` 懒加载 |
| Sheet 的持久化 | `Sheet.store()` + `SheetStub.storeSheet()` |
| Sheet 的 JAXB 上下文 | `Sheet.getJaxbContext()` |
| Sheet 文件的路径计算 | `Sheet.getSheetFileName(number)` |
| Sheet 的处理步骤推进 | `SheetStub.reachStep()` → `doOneStep()` |
| Sheet 的 UI 标签管理 | `StubsController` |
| Sheet 的版本检查 | `Versions.check()` |
| Sheet 的参数管理 | `SheetStub.initParameters()` → `BookParams` → `SheetParams` |

---

## 设计意图分析

将 Sheet 管理分散到 `Book` + `SheetStub` + `StubsController` 的设计意图：

1. **单一职责**: Book 管"集合"层面的逻辑，SheetStub 管"代理"层面的逻辑
2. **懒加载核心**: Stub 作为常驻内存的轻量代理，使得 Book 可以包含大量页面而不消耗太多内存
3. **分离 UI 与数据**: `StubsController` 专注 UI 层（标签显示、选中状态），不参与数据层
