---
layout: default
title: User editing
parent: How-to guides
nav_order: 2
has_toc: false
---
# User editing

In the transcription process made by the OMR engine, some elements may not be recognized
correctly for various reasons, such as poor image quality or over-crowded score, to name a few.

There is a continuous effort going on to improve the OMR engine but on many examples
a completely error free transcription is just out of reach.
It is thus globally more efficient to complement the OMR engine with a convenient graphical
editor so that the end-user can easily fix most of the OMR errors.

While Audiveris 5.1 release provided a basic editor limited to the drag & drop of fixed-size
symbols, the 5.2 release added a comprehensive editor to modify nearly any kind of symbol.

The Audiveris editor does not try to compete with music editors like MuseScore, Finale or Sibelius.
Those are sophisticated high-level editors meant for composers, arrangers or publishers
who need a tool to create, arrange, transpose music, etc.

Instead, Audiveris is focused on the OMR process, which attempts to transcribe an existing
score image to its symbolic representation.
In particular, it tries to remain as close as possible to the original layout, including any image
skew or warping.

## Tutorial videos

As an introduction, you can watch Audiveris 5.1 user editor in action
thanks to [Baruch Hoffart] who recorded this
[tutorial video](https://www.youtube.com/watch?v=718iy10sKV4&feature=youtu.be).  

For those who understand French, we highly recommend the YouTube channel run by [Dominique Verriere].
It is an impressive collection of detailed tutorials about Audiveris and MuseScore.
Here is a direct link to his [Audiveris videos].

## Documentation content

1. [UI foundations](./ui_foundations/README.md): Editable OMR data,
   how to select and inspect it.
2. [UI tools](./ui_tools/README.md): How to modify OMR data,
  beginning with the general tools to add/edit/remove both Inters and Relations.  
  Then more ad-hoc tools are presented to address specific cases.
3. [UI examples](./ui_examples/README.md): Complete editing sessions
   on representative input scores.

The best way to learn an editor is certainly by practicing rather than studying a long
documentation.  
So, you could start by reading **Foundations** and then discover **Examples**.
Then, you could go back to the **Tools** of interest for you, at your own pace.

[Baruch Hoffart]:     https://github.com/Bacchushlg
[Dominique Verriere]: https://www.youtube.com/@DominiqueVerriere
[Audiveris videos]:   https://www.youtube.com/playlist?list=PLZNad9Wu-U5FHwu02qGFA_18Ebsz6Ifm8
