# SystemInfo 类逐行详解

## 文件信息

- **路径**: `app/src/main/java/org/audiveris/omr/sheet/SystemInfo.java`
- **核心定位**: SystemInfo 代表一个**乐谱系统**（System），即乐谱中的一行。它包含该行内所有五线谱（StaffInfo）、小节线、音符等信息。

## 在持久化流程中的角色

SystemInfo 是 Sheet 序列化内容的核心组成部分，存储在 `sheetNNN.xml` 中。它被 `Page` 引用，Page 被 `Sheet` 引用。

---

## 核心字段说明

| 字段 | 类型 | JAXB注解 | 作用 |
|------|------|----------|------|
| `id` | `int` | `@XmlAttribute` | 系统在 Sheet 内的唯一标识 |
| `firstStaff` | `StaffInfo` | `@XmlElement` | 系统内第一个五线谱 |
| `lastStaff` | `StaffInfo` | — | 系统内最后一个五线谱（不直接序列化） |
| `parts` | `List<PartInfo>` | `@XmlElement(name="part")` | 乐器部分列表 |
| `left` | `int` | `@XmlAttribute` | 系统左边界（x坐标） |
| `right` | `int` | `@XmlAttribute` | 系统右边界（x坐标） |
| `width` | `int` | — | 系统宽度（计算得出） |

---

## 核心方法

### `buildRef()` — 构建系统引用

用于构造跨页引用时的标识对象，返回 `SystemRef`（包含 id、firstLine、lastLine 等信息）。

### 序列化细节

SystemInfo 被 Page 的 `@XmlElement(name="system")` 引用：

```java
// Page.java 中
@XmlElement(name = "system")
private final List<SystemInfo> systems = new ArrayList<>();
```

---

## 关联关系

```
Sheet
  └── List<Page>
        └── List<SystemInfo>         ← SystemInfo 在此
              ├── List<StaffInfo>    ─── 五线谱（通过 firstStaff/lastStaff 引用）
              ├── List<PartInfo>     ─── 乐器部分
              ├── List<Barline>      ─── 小节线
              └── SIGraph (部分)     ─── 系统级的符号解释
```
