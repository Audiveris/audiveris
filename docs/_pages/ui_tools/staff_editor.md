---
layout: default
title: Staff Edition
grand_parent: User Edition
parent: UI Tools
nav_order: 6
---
## Staff Edition
{: .no_toc }

During `GRID` step, the OMR engine strives to detect sequences of equally spaced,
long and thin horizontal filaments.
These filaments are then converted to staff lines and their underlying pixels are erased.

For this to work correctly, the score image must exhibit enough "long and thin" filaments.  
Of course, there are musical symbols lying on these staves, typically beam symbols, which may hide
the underlying staff lines.
Generally, the engine can interpolate -- or even extrapolate -- the holes found in image regions
with such broken filaments.

If the engine result is not satisfactory, we can manually correct these staves via staff
edition, which is provided in two different modes: `Global` mode and `Lines` mode.
The former operates on the staff as a whole, the latter on any line separately.

To enter staff edition, we make a right-click within a staff area, in the popup menu select the
`Staff#n...` sub-menu and then either `Edit staff` or `Edit lines` item.

---
Table of contents
{: .text-delta }

1. TOC
{:toc}
---

### Lines mode

In this mode, we can modify the different lines of the staff individually.

| Input view | Output view |
| :---: | :---: |
| ![](../assets/images/staff_lines_wrong.png) | ![](../assets/images/staff_lines_uneven.png)  |

In the example above, on the left-side picture, we can see that pixels at the beginning of the staff
have been left over (these are the horizontal sections displayed in pink color).
These non-erased pixels may impede further detection and recognition of symbols, because they will
be considered as parts of the symbol to recognize, here a C clef.

This was certainly due to a wrong extrapolation of staff lines, which is more visible on the
right-side picture. The staff lines, as detected, are not evenly spaced near the barline.

To fix this, preferably right after `GRID` step, we enter the staff edition in `lines` mode.
![](../assets/images/staff_lines_handles.png)

Each line of the staff now exhibits its own sequence of handles, which we can press and drag to modify
the staff line locally.

In this `Lines` mode, the handles can move only vertically.
In the case at hand, we could slightly drag some left handles up or down.
We can notice that the pink sections disappear when they get crossed by a staff line.

![](../assets/images/staff_lines_ok.png)

Clicking outside of any handle completes the edition.  
We can always undo/redo the operation.

In short, this mode is meant to finely adjust the vertical location of any line portion.
If we want to move the staff limits horizontally, then we need to use the `global` mode instead.

{: .note }
The number of handles can vary from one line to the other, as we can see on the picture.
This number depends on how "wavy" was the line detected by the engine.
It has no negative impact on the edition capabilities.

### Global mode

In this mode, we can move staff portions vertically and/or horizontally, and doing so we modify
all staff lines as a whole.

| Input view | Output view |
| :---: | :---: |
| ![](../assets/images/staff_wrong.png) | ![](../assets/images/staff_too_short.png)  |

In the example above, the upper staff is so crowded with heads, stems and beams that the engine
could not detect enough line portions.

So much that many pink sections are still visible where staff lines should be, and that the staff
got even truncated on its right side.

To fix this, we enter the staff edition in `global` mode.

![](../assets/images/staff_handles.png)

As opposed to the `lines` mode, we get handles only along the staff middle line.
This is enough to move the whole staff:
- Side handles can move vertically **and horizontally**,
- Other handles can move only vertically.

So, we press the right-most handle and drag it horizontally to the right until it reaches
the right barline and vertically so that most if not all pink sections disappear.  
We then release the mouse.

![](../assets/images/staff_handles_ok.png)

We still have a couple of pink sections to fix on lines 2 and 3. This is a job for the `lines` mode.
We simply right-click in the staff area, and select the `lines` mode.

![](../assets/images/staff_handles_nearly.png)

We slightly drag down two handles, et voilà!:

![](../assets/images/staff_handles_perfect.png)

{: .highlight }
A final remark: Since moving a handle is often a matter of very few pixels, we may find more
convenient to move a handle via the **keyboard**:
While keeping `Alt` key pressed, we can use the 4 arrow keys to move the selected handle
one pixel at a time in the desired direction.
