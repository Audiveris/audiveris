---
layout: default
title: UI Principles
parent: UI Tools
nav_order: 1
---
## UI Principles
{: .no_toc }

For Audiveris, music is something very graphical, a 2D view of music symbols
(staff lines, bar lines, note heads, stems, flags, beams, alterations, ...)
with some "geometrical" relations between them (a note head and its stem, an alteration sign
and the altered head, a lyric item and its related chord, a slur and its embraced heads, ...).

The pixels of the score image are displayed in the background and serve as guidelines for the
interactive user.
Some pixels may have been gathered into **Glyph** instances by the OMR engine, but this
"_segmentation_", as it is called, is not always relevant, especially on poor quality images.

The entities that really matter for the final music content are the **Inter** (interpretation)
instances and the **Relation** instances that formalize a relationship between two Inter instances,
resulting in the **SIG** (Symbol Interpretation Graph) of each system.  
If you are still unfamiliar with these notions of pixel / glyph / inter / relation / sig, please
have a look at the dedicated [Main Entities](../main/entities.md) section.

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

### Entities

We can think of Audiveris OMR as a process that takes an image as input and produces
as output a structured collection of music symbols.

* The OMR _engine_ is one actor, which moves forward from step to step,
creating a bunch of entities.
* The OMR _user_ is another actor, which can interactively modify some entities,
and decide how to move the engine.

The resulting entities are divided into two categories:

1.  **Containers**.
They are built at the beginning of the sheet processing and define the global structure of the
sheet musical content, organized in a hierarchical structure of containers:
    - *staff lines*, gathered in ...
    - *staves*, gathered in ...
    - *parts*, gathered in ...
    - *systems*, gathered in ...
    - *pages*, gathered in ...
    - *scores*

    Starting with 5.2 release, the user can partly modify this hierarchy, by splitting and merging
    systems and parts.

2.  **Inters**.
They represent candidate interpretations, formalized by `Inter` instances, handled
within each system by a graph (`SIG`) of Inter vertices linked by Relation edges.
Within each system SIG, it's the struggle for life, since only the strongest inters
survive in the end.   
There is no structure within the Inter instances of a SIG, just `Relation` instances possibly set
between a pair of Inter instances.  
Starting with 5.2 release, all these Inter and Relation instances can be modified interactively.

    Inter objects are everything except the static containers mentioned above:   
    - Barline, ledger, clef, key signature, time signature, chord, head, alteration,
    augmentation dot, stem, beam, rest, flag, slur, sentence, word, etc... are examples of Inters.
    - An _InterEnsemble_ is an Inter composed of other Inter instances.
    This composition is formalized by a `Containment` relation between the ensemble inter and
    each of the contained Inter instances.  
    These ensembles can be:
      - A Chord which contains one or several note heads or one rest.
      - A Sentence which contains words.
-- A LyricLine is a special kind of Sentence, composed of LyricItem's --
      - A Key which contains key alter signs.
      - A TimePair which contains a numerator number and a denominator number.

Measures are hybrid, since they are not Inters but depend on barlines which are Inters.

Beside Inter and Relation entities, glyphs, which are just sets of foreground pixels, can be used
to create new Inters.
Hence, Glyph instances are also entities that the user can select and transcribe to Inter entities.

Audiveris editor has been designed to work on the SIG data model,
that is essentially to play directly with Inter and Relation instances.

### Actions

The OMR engine works in batch, or more precisely in a sequence of some 20 mini-batches since each
of the 20 sheet steps is a mini-batch.
You, as an interacting user, can get in only at the end of a mini-batch.


Most of the user corrections can be done at the end of a sheet transcription
(at the final `PAGE` step of the engine pipeline).
Some corrections are more effective at earlier moments, experience will tell you.  
Here below are the most interesting stopping points:

*   **REDUCTION** step is where all candidate note heads are combined with candidate stems and
beams and then reduced to come up with reliable notes (this does not include any flag or rest,
which are addressed in later SYMBOLS step).  
It is a key moment in the engine pipeline, because these "reliable" notes will never be called into
question by the following steps.
So much that their underlying pixels will not be presented to the symbols classifier during the
`SYMBOLS` step.
This includes a "dilation" margin ring around each note head, something that the user cannot select
as a glyph in any subsequent selection.  
So, if some of these notes are false positives, then it's much more efficient to correct them
immediately _before_ the REDUCTION step (for example at end of STEMS step).

*   **TEXTS** step is another key moment.
It aims at retrieving all the textual items in the image and assigning them a role (such as title,
part name, lyrics).
The engine output for the `TEXT` step depends heavily on an external program (Tesseract OCR) on
which Audiveris has little control.
It is hard for the OMR engine to decide if an item is really a piece of text, or if it should also
be considered as a possible music symbol.  
Thus, it is efficient to manually remove text false positives at end of this TEXT step to let
their pixels compete within the SYMBOLS step.

*   **PAGE** step is where the OMR engine ends on a sheet and where you can observe and correct the
final result.

## Interaction with OMR engine

Following any user manual action, OMR data is immediately updated depending on the impacted steps,
the sheet view is updated to reflect the new status of OMR elements directly or indirectly modified,
and also to indicate any "_abnormal_" situations detected on-the-fly by the OMR engine.
* For example, a black note head with no compatible stem nearby will appear in red.
* Similarly, a whole measure may have its background colored in pink to indicate a rhythm problem.

## Tasks

#### Task sequence

Whenever you modify an entity in some way, this task is recorded as such, perhaps with some
joint tasks, into an atomic "_task sequence_" which is then performed on the targeted entities.

This sequence may have consequences on the results of steps already performed, in which case their
results are immediately updated.

A task sequence can be pretty large. For example:

*   You can select a dozen of inters and ask for the removal of the whole set.
*   You can remove an inter ensemble (sentence, chord, ...), this triggers the removal of all the
members of the ensemble: words of the sentence, heads and stem of the head-chord, etc.
*   You can remove the last member of an ensemble, this also removes the now-empty ensemble.

The key point here is that any "task sequence" is **atomic**.
You can do it, **undo** and **redo** it at will, with no limits.
Only moving the engine forward (or pseudo backward) from one step to another clears the user
task history.


#### Undo

Undo cancels the last task sequence (whether it was a do or a redo) with all its consequences.

*   Press the **Undo** button ![](../assets/images/undo.png) on tool bar,
*   Or type `CTRL`-`Z` (`Command`-`Z` for MacOS)

#### Redo

Redo re-performs the last un-done task sequence.

*   Press the **Redo** button ![](../assets/images/redo.png) on tool bar,
*   Or type `SHIFT`-`CTRL`-`Z` (`SHIFT`-`Command`-`Z` for MacOS).
