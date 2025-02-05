---
layout: default
title: Limitations
parent: Reference
nav_order: 8
---
# Limitations
{: .no_toc }

This section presents the known cases that current Audiveris does not handle properly.
These are "known limitations", which should be addressed in future releases.

It is important that the end user be aware of these cases to avoid wasting time on them
with the current release.
Apart from these cases, you can file a bug report or a request for enhancement on
[https://github.com/Audiveris/audiveris/issues](https://github.com/Audiveris/audiveris/issues).

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

## Natural signs in key signature

![](../assets/images/hybrid_key.png)

In the current data model, a key signature is assumed to contain only sharp signs or only flat signs.
There is no room in them yet for natural signs.

Such natural signs are just "courtesy" signs for the reader, and can be ignored by OMR.

Note however that, since the current engine expects sharps-only or flats-only signatures,
the simple presence of natural signs will likely impede correct key recognition.
In this case, we will have to manually enter the correct key (without the naturals).

Since 5.3, we can manually insert a natural-only key signature, a kind of "cancel key".
See this capability detailed in the [Naturals section](../guides/ui/ui_tools/key.md#naturals).

## Key signature change

![](../assets/images/curtesy_key.png)

A key signature generally appears at the beginning of a staff, within what Audiveris calls the staff
"header" (a sequence of: clef, optional key signature, optional time signature).
Generally, the engine correctly handles a key signature in the header, simply because it knows
precisely where to look.

But later down the staff, a key change may appear.
And this new key is not yet handled by the engine.

Such a change often appear in a "courtesy measure", located at the end of the staff and containing no
music note.
The purpose of a courtesy measure is simply to warn the human reader that a new key will occur at
the next system start.
It is thus harmless for the OMR engine to ignore this warning.

Apart from this courtesy case, the user may have to manually enter the missing key change on every
staff.

## Opposed Stems

We can have two head chords with up and down stems that are located in such a way that they seem
to be merged, as follows:

![](../assets/images/opposed_stems.png)

The OMR engine may detect just one long stem, instead of two aligned ones.
The problem is that this single stem has some heads attached near the middle of the stem,
but no head on any stem end.
At the next reduction, this stem will be discarded, and the now isolated heads as well.

This error can be fixed by manually inserting separate standard stems (and the related heads).

## ChordName

Recognition of chord names can still be impeded by the presence of sharp (``♯``) or flat (``♭``) 
characters which Tesseract OCR cannot handle correctly.

As a workaround, we can manually replace them by number (``#``) and lowercase ``b`` characters.
But the real solution should come from the training of Tesseract OCR on these embedded alteration
signs.

See further description in the [Text Chord Name](../guides/ui//ui_tools/text.md#chord-name) section.

## Tuplets

Tuplets other than triplets and 6-tuplets are not supported, even manually.

## Roman numeral

Some input files represent chords using Roman numerals.

These are recognized, but not yet exported to MusicXML.

