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

{: .highlight }
Nota: There used to be a section on "*Natural signs in key signature*" and a section on "*Key signature change*"
to report limitations on these topics.
Starting with the 5.8 release, these limitations no longer exist.
See details on the [Key Signature](../guides/ui/ui_tools/key.md) chapter.

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

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

