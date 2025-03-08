---
layout: default
title: Text
parent: UI tools
nav_order: 13
---
# Text
{: .no_toc }

The recognition of textual elements is delegated to the Tesseract OCR library.

The `TEXTS` step runs the OCR on the current sheet.
We can also manually run the OCR on a selected collection of glyphs
or even drag n' drop text items from the `Shape` board.

Running the OCR results in one or several text words gathered in sentences,
which we can further modify manually, in terms of:
- textual _content_,
- _type_ of words and sentences,
- _role_ of every sentence.

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

## TEXTS step

The `TEXTS` step runs the OCR on the whole sheet image
and tries to assign to each OCR'd item its content, type and role.

This engine step is influenced by three options available in the {{site.book_parameters }} menu:
- [ ] Support for chord names
- [x] Support for lyrics (assumed to be located below the related staff)
- [ ] Support for lyrics even located above staff

Chord names and lyrics are special items; this is the reason why their recognition must be
explicitly selected to avoid collateral damages of the OMR engine when they are not desired.

On the other hand, the metronome marks, thanks to their recognizable structuring,
don't require the setting of any specific option.

## Manual OCR

The OCR can also be launched manually on a glyph(s) selection by pressing one of the
buttons provided in the `Texts` palette of the Shape board:
* The `text` button
* The `lyric` button,
* The `metronome` button,

![](../../../assets/images/Texts_palette.png)

There are separate buttons because lyric items have a behavior significantly different from
other text items -- especially the gap between words can be much wider.
And the metronome is a specific item on its own.

By manually choosing one button or another, we clearly specify the desired result type
-- and thus the sentence role -- of the OCR operation.

We can as well drag n' drop items from the same `Texts` palette.  
In this case, no OCR is performed, and we have to manually enter every word content.

## Sentence vs. Words

A Sentence `Inter` is an ensemble of one or several Word `Inter`(s):

* A **Word** handles its textual content and location.
  Word sub-classes (ChordName, LyricItem, BeatUnit) handle additional data.  
  The word ___content___ is modifiable by the user:  

  ![](../../../assets/images/word_text_edited.png)

* A **Sentence** is a sequence of words.  
  (We can easily navigate from a selected word to its containing sentence
  via the `ToEnsemble` button of the `Inter` board).  
  The sentence ___content___ is defined as the concatenation of the contents of its words members.
  Except for the metronome case, this sentence content is not modifiable directly,
  but rather via its words members.  
  The sentence ___role___ is modifiable by the user.

  ![](../../../assets/images/sentence_role_edited.png)

A sentence role can be set to any value among:
- UnknownRole
- ___Lyrics___
- ___ChordName___
- Title
- Direction
- Number
- PartName
- Creator
- CreatorArranger
- CreatorComposer
- CreatorLyricist
- Rights
- EndingNumber
- EndingText
- ___Metronome___

Since the 5.2 release, in all cases, we can manually modify the sentence role afterwards,
from any role to any other role.

## Plain Sentence

A "plain" sentence is any sentence which is assigned a role different
from Lyrics, ChordName and Metronome.

Following an OCR recognition (`Texts` step or manual OCR), the role of each resulting 
_plain_ sentence is precised.
Based on a bunch of heuristics, the engine tries to further distinguish between plain roles
like: direction, part name , title, composer, lyricist, etc.

## Chord Name

A chord name is a musical symbol which names and describes the related chord.

For example:
`C`, `D7`, `F♯`, `B♭min`, `Em♭5`, `G6/B`, `Gdim`, `F♯m7`, `Em♭5`, `D7♯5`, `Am7♭5`,
`A(9)`, `BMaj7/D♯`.

{: .important }
As of this writing, the Audiveris engine is not yet able to recognize chord names that include true
sharp (``♯``) or flat (``♭``) characters.
Perhaps one day, we will succeed in training Tesseract OCR on this text content.  
For the time being, Audiveris is able to recognize such chord names when these characters have
been replaced (by OCR "mistake", or by manual modification) by more "usual" characters:    
    - ``'#'`` (number) as replacement for ``'♯'`` (sharp),  
    - ``'b'`` (lowercase b) as replacement for ``'♭'`` (flat).

When we OCR a chord name word, Audiveris may be able to decode it as a chord name and thus wrap
it within a chord name sentence.

If Audiveris has failed, we can still force the chord name role (at the sentence level) and
type in the missing `b` or `#` characters if so needed (at the word level).
The chord name will then be decoded on-the-fly with its new textual content.

Note we don't have to manually enter the true sharp or flat signs.
Entering them via their Unicode value is a bit tricky and, in the end, useless.    
Instead, when text has been recognized or assigned as a chord name, its internal `b`
or `#` characters are automatically replaced by their true alteration signs.    
For example, we can type "Bb" then press `Enter` and the chord name will be translated and
displayed as "B♭".

## Lyric Line

A lyric line is a sentence composed of lyric items.

When selected, the `Inter` board displays additional data:
* Voice number,
* Verse number,
* Location with respect to staff.

![](../../../assets/images/lyrics_data.png)

Each syllable (lyric item) is usually linked to a related chord, either above or below.    
But it is not always obvious whether the text concerns the staff above or below
nor is it always clear which voice is concerned.

If a syllable is not linked to the correct chord, we can modify this link manually by dragging
from the syllable to the suitable chord.
This will update on-the-fly the line data (voice, verse, location).

## Metronome mark

Since the 5.4 release, the [metronome](../../specific/metronome.md) marks
can be automatically recognized.  
We can also edit them afterwards and even create new marks from scratch.

A metronome mark is a sentence composed of words.  
One of its words is special as it contains not textual characters but music characters.
This word is the BeatUnit word.

Editing the metronome is detailed in this [specific section](../../specific/metronome.md#manual-editing).
