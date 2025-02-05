---
layout: default
title: Pop-up menu
parent: Menus
nav_order: 10
---
# Pop-up menu
{: .no_toc }

![](../../assets/images/popup_menu.png)

This pop-up menu appears via a mouse right-click in a sheet view,
and is represented by the ``≡`` symbol in this handbook.

It displays _contextual_ information and actions that depend on the sheet latest processing step,
the current location in sheet view and the selected items if any.

Some of its content also depends on user-selected
[Advanced Topics](../../guides/main/preferences.md#advanced-topics) in the Preferences dialog.

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

## Chords
Depending on the chord(s) currently selected, this sub-menu allows to modify their configuration
regarding time slot and voice.

See the chapter on [Chords editing](../../guides/ui/ui_tools/chords.md).

## Inters
If at least one `Inter` instance has been selected, this sub-menu lists the selected Inters,
ordered by decreasing grade.

It also allows to delete `Inter` instance(s) or relation(s).

## Glyphs
If at least one Glyph instance has been selected, the _compound_ glyph
(dynamically built by merging all selected glyphs) appears in this sub-menu.

It allows to assign a specific shape (via a created `Inter`) to the selected glyph.

## Slot #n @offset
If the `RHYTHMS` step has been reached, this sub-menu relates to the closest time-slot
in the containing measure stack.

For this time slot, we can:
* **Dump chords**: list all the chords starting on this slot.
* **Dump voices**: list all the voices with chords starting on this slot.

## Measure #n
If current location lies within a measure, this sub-menu provides actions upon the selected measure.

A measure is delimited horizontally by the left and right bar-lines and vertically by
the top and bottom staves of the containing part.

Depending on which steps have already been performed, we can:
* **Dump stack voices**: for the containing vertical stack of measures, display a kind of strip
with time slots in abscissa and voices in ordinate.
* **Dump measure voices**: same display but limited to the containing measure.
* **Reprocess rhythm**: force re-computation of rhythm data (slots and voices) in the current measure.
* **Merge on right**: merge the current measure with the next measure on right.

Example of voices dump:
```
MeasureStack#2
    |0       |1/16    |3/16    |1/4     |3/8     |7/16    |1/2
--- P1
V 1 |Ch#2325 ==================|Ch#2326 |Ch#2828 |Ch#2327 |1/2
V 5 |Ch#2347 |Ch#2348 |Ch#2349 |Ch#2350 |Ch#2351 =========|1/2
```

## Staff #n
This sub-menu appears if the current location is within a staff height
(even beyond staff horizontal limits).

We can edit the whole staff or one of its lines (see [Staff Editing](../../guides/ui/ui_tools/staff_editing.md)):
- **Edit staff**: Manual editing of all staff lines as a whole
- **Edit lines**: Manual editing of one staff line in isolation

We can also display vertical projections of the whole staff and of the staff header,
provided that the ``PLOTS`` advanced topic has been selected.

## Part #n
Allows to manually assign a logical part to this physical part.  
See the section on part [Manual mapping](../../guides/specific/logical_parts.md#manual-mapping).

## System #n
If the current system is not the last one in sheet, this allows to **Merge with system below**.

## Page #n
Allows to **Reprocess rhythm**, that is to force re-computation of rhythm data for the whole page.

## Score #n
Allows to manage all the logical parts in the containing score.  
See the chapter on [logical part management](../../guides/specific/logical_parts.md)

## Extraction
Extracts a rectangular portion (or whole) of the underlying binary image and save it to disk.
This is meant for sharing or further analysis.

Limitation: The rectangular area selection is effective only from the `Binary` tab,
not from the `Data` tab.
