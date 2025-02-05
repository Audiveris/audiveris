---
layout: default
title: UI tools
parent: User editing
nav_order: 2
has_children: true
---
# UI tools
{: .no_toc }

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

## Interaction with the OMR engine

The OMR engine works as a sequence of 20 sheet steps, a step being a kind of mini-batch.  
We, as an interacting user, can get in only at the end of a step.  
[When a sheet first appears with its Data tab, the OMR engine has already performed the LOAD and
BINARY steps.]

Following any user manual action, OMR data is immediately updated depending on the impacted
steps, the sheet view is updated to reflect the new status of OMR elements directly or
indirectly modified, and also to indicate any "_abnormal_" situations detected on-the-fly by
the OMR engine.
* For example, a black note head with no compatible stem nearby, or vice versa a stem with no
  linked note head, will appear in red.
  Such a situation can happen temporarily while items are being manually created or edited,
  one after the other.
* Similarly, a whole measure may have its background colored in pink to signal a rhythm problem.

## Tasks

### Task sequence

Whenever we modify an entity in some way, this task is recorded as such, perhaps with some
joint tasks, into a "_task sequence_" which is then performed on the targeted entities.

A task sequence can be pretty large. For example:

*   We can select a dozen inters and ask for the removal of the whole set.
*   We can remove an inter ensemble; this triggers the removal of all the
members of the ensemble: words of a sentence, heads of a head-chord, etc.
*   Removing the last remaining member of an ensemble also removes the now-empty ensemble.

A task sequence is _indivisible_, but we can **do** it, **undo** and **redo** it at will,
with no limits.
Only moving the engine forward (or pseudo backward) from one step to another clears the user
task history.


### Undo

The ``Undo`` command cancels the last task sequence (whether it was a do or a redo)
with all its consequences.

*   We press the **Undo** button ![](../../../assets/images/undo.png) on the tool bar,
*   Or type `Ctrl+Z` (`Command+Z` for macOS)

### Redo

The ``Redo`` command re-performs the last un-done task sequence.

*   We press the **Redo** button ![](../../../assets/images/redo.png) on the tool bar,
*   Or type `Ctrl+Shift+Z` (`Command+Shift+Z` for macOS).

## When to interact?

Most user corrections can be done at the end of a sheet transcription
(at the final `PAGE` step of the engine pipeline).

Some corrections are more effective at earlier moments; experience will tell us.  
Here are interesting stopping points, presented in chronological order:

* **SCALE** step is an interesting stop if the beams are questionable.  
That is, if we get a message saying that the beam thickness value is just "extrapolated",
we must be careful!
This tells us that beam-related data could not reach the quorum needed to infer a reliable
thickness value.  
So, let's measure the actual beam thickness on our own (using the Pixel Board) and modify
the beam scale if needed (see the [Sheet scale](../../main/sheet_scale.md) section).

* **GRID** step is where staff lines, then staves and systems are detected.  
Hence, this is the most convenient point to interact if we need to manually modify lines
and staves (see the [Staff editing](./staff_editing.md) section).

* **REDUCTION** step is where all candidate note heads are combined with candidate stems and
beams and then reduced to come up with reliable notes
-- this does not include any flag or rest, which are addressed later in the SYMBOLS step.  
It is a key moment in the engine pipeline, because these "reliable" notes will never be called into
question by the following steps.
So much so that their underlying pixels will not be presented to the glyph classifier during the
`SYMBOLS` step.
So, if some of these notes are false positives, then it's much more efficient to correct them
immediately, at the end of the REDUCTION step.

* **TEXTS** step is another key moment.  
It aims at retrieving all the textual items in the image and assigning them a role (such as title,
part name, lyrics).
The engine output for the `TEXT` step depends heavily on an external program (Tesseract OCR) 
which Audiveris has little control upon.
It is hard for the OMR engine to decide if an item is really a piece of text, or if it should also
be considered as a possible music symbol.  
Thus, it is efficient to manually remove text false positives at the end of this TEXT step to let
their pixels be taken into account by the SYMBOLS step.

* **PAGE** step is where the OMR engine ends on a sheet and where we can observe and correct the
final results.
