---
layout: default
title: Principles
grand_parent: User Edition
parent: UI Foundations
nav_order: 1
---

## Principles
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

We can think of Audiveris OMR as a process that takes an image as input and produces
as output a structured collection of music symbols.

* The OMR _engine_ is one actor, which moves forward from step to step,
creating a bunch of entities.
* The OMR _user_ is another actor, which can interactively modify some entities,
and decide how to move the engine.

The resulting objects are divided into two categories:

1.  **Containers**.
They are built at the beginning of the sheet processing and define the global structure of the
sheet musical content, organized in a hierarchical structure of containers:
    - *staff lines*, gathered in ...
    - *staves*, gathered in ...
    - *parts*, gathered in ...
    - *systems*, gathered in ...
    - *pages*, gathered in ...
    - *scores*

    Starting with 5.2 release, the user can partly modify this hierarchy, by splitting and merging systems, parts and measures.

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
      - A BeamGroup which contains several parallel beams.
      - A StaffBarline which is a horizontal group of simple Barlines.

Measures are hybrid, since they are not Inters but depend on barlines which are Inters.

Beside Inter and Relation entities, glyphs, which are just sets of foreground pixels, can be used
to create new Inters.
Hence, Glyph instances are also entities that the user can select and transcribe to Inter entities.

Audiveris editor has been designed to work on the SIG data model,
that is essentially to play directly with Inter and Relation instances.
