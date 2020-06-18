---
layout: default
title: System
parent: UI Tools
nav_order: 11
---
## System
{: .no_toc }

In Audiveris GRID step, detected staves are gathered into systems, based on barlines found on
the left side of the staves.

In a poor quality score image, many black pixels may have disappeared, sometimes leading to broken
barlines.

Unfortunately, in the example below, the leading left barline was damaged, resulting in a wrong
detection of systems.

| left barline broken | resulting grid before | resulting grid after |
| ---| --- | --- |
| ![](../assets/images/system_broken.png) | ![](../assets/images/system_broken_before.png) |   ![](../assets/images/system_broken_after.png) |

Starting with 5.2 release, you can manually fix this problem.

Point at the upper system portion, and via the right-click popup menu, navigate to `System#n ...`
and click on "_Merge with system below_".

![](../assets/images/system_merge.png)

This is a key operation, so you need to confirm the detailed prompt:

![](../assets/images/system_merge_prompt.png)

And it's done: a connector was created between the two barline portions and the two system
portions merged.

You can still undo/redo the operation.
