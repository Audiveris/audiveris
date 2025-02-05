---
layout: default
title: System merge
parent: UI tools
nav_order: 7
---
# System merge
{: .no_toc }

In the Audiveris ``GRID`` step, detected staves are gathered into systems, based on barlines found on
the left side of the staves.

In a poor quality score image, many black pixels may have disappeared, sometimes leading to broken
barlines.

In the example image below, the leading left barline has been damaged, resulting in a wrong
detection of systems by the OMR engine.

| Left barline broken | Resulting grid before fix | Resulting grid after fix |
| ---| --- | --- |
| ![](../../../assets/images/system_broken.png) | ![](../../../assets/images/system_broken_before.png) |   ![](../../../assets/images/system_broken_after.png) |

Since the 5.2 release, we can manually fix this problem.

We point at the upper system portion, and via the right-click {{ site.popup_system }} menu
we select "_Merge with system below_".

![](../../../assets/images/system_merge.png)

This is a key operation, so we need to confirm the detailed prompt:

![](../../../assets/images/system_merge_prompt.png)

And it's done: a connector was created between the two barline portions and the two system
portions merged.

We can still undo/redo the operation.
