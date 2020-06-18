---
layout: default
title: UI Tools
nav_order: 4
has_children: true
---
# UI Tools

In the transcription process made by the OMR engine, some elements may not be recognized
correctly for various reasons, such as poor image quality or over-crowded score, to name a few.
We continuously strive to improve the OMR engine but on many examples a 100% recognition ratio is
just out of reach.

It is thus globally more efficient to complement the OMR engine with a convenient graphical
editor so that the end-user can easily fix most of OMR errors.

Audiveris 5.1 release provided a basic editor limited to the drag n' drop of fixed-size symbols.
The 5.2 release now offers a comprehensive editor to modify any kind of symbol, whether fixed or
varying in size, whether detected by the engine or manually inserted by the user, etc.

## Audiveris editor

It is important to realize that Audiveris editing features cannot compare
with music editors like MuseScore, Finale or Sibelius, to name a few.

* Those are sophisticated high-level editors meant for composers, arrangers or publishers
who need a tool to create, arrange, transpose music, etc.

* Instead, Audiveris is focused on the OMR process, which attempts to transcribe an existing
score image to its symbolic representation.  
Fidelity to the original score image is of paramount importance.
To this end, Audiveris tries to remain as close as possible to the original layout.
This even includes to stick to any original image distortions (such as skew or warping).
