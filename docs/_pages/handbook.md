---
layout: default
title: Audiveris Handbook
nav_order: 0
has_children: false
has_toc: false
---
# Audiveris Handbook
{: .no_toc }

This documentation applies to release {{ site.audiveris_version }} and later.

---
Table of Contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

## Intended audience

This handbook is meant for the Audiveris end-user.
To ease the reading for a newcomer as for a more advanced user, it is organized
as a progressive sequence of chapters.

It is just a user manual; a true developer documentation is still to be written.
Some material is already made available, through the
Audiveris [Wiki] and through the description of the
[`.omr` file format](./reference/outputs/omr.md),
to ease the software learning curve for any potential developer.

## Overview

When questioned about Audiveris in December 2023, ChatGPT answered:

> Audiveris is an open-source optical music recognition (OMR) software. OMR technology is designed to recognize printed music notation from scanned images or photos and convert it into a digital format that can be edited and played back. Audiveris specifically focuses on converting scanned sheet music into MusicXML format.

> The software is developed in Java and is available for various operating systems, including Windows, macOS, and Linux. It can be used to digitize printed sheet music, making it easier to edit, share, and playback on digital devices.


Not that bad at all! But let's try to describe it on our own...

Audiveris is an open source software, published under the
[AGPL] V3 license.

It is an **O**ptical **M**usic **R**ecognition (OMR) application,
featuring an OMR engine coupled with an interactive editor.

It recognizes music from digitized images and converts it into
computer-readable symbolic format.
This enables further music processing by any external notation editor,
notably: digital playback, transposition and arrangement.

Audiveris  provides outputs in two main digital formats: its own OMR format and the
standard MusicXML format.
* [OMR] is the Audiveris specific format, a documented open source format based on XML.
* [MusicXML] is  a *de facto* standard.
It has been designed for score interchange and is today
supported as input/output by almost every music notation program.

Not every kind of sheet music can be handled. Audiveris engine has been designed
to process scores written in the _Common Western Music Notation_ (CWMN)
with the following limitations:
* Handwritten scores aren't supported (only printed scores are),
* Only common musical symbols are supported.

Because the accuracy of the OMR engine is still far from perfection,
the Audiveris application provides a graphical user interface specifically focused
on quick verification and manual correction of the OMR outputs.

External sophisticated music editors, such as MuseScore or Finale, can be used on
the Audiveris MusicXML output.
They can even be directly connected via simple [plugins](./guides/advanced/plugins.md)
to ease the information handover from Audiveris to these external editors.

## Handbook structure

This documentation has been restructured for the 5.4 release, according to the principles
of the [DIVIO] Documentation System, around four different functions:

- [Tutorials](./tutorials/README.md) -- To get started
    - Installation of Audiveris application
    - Quick tour from a score capture image to its playback by a music sequencer
    - Introduction to the main concepts covered by the OMR process
    - Presentation of the graphical user interface
    
- [How-to guides](./guides/README.md) -- For precise tasks
    - Frequent tasks like setting book parameters and driving the OMR pipeline 
    - Operating in batch through the command line interface
    - Using the graphical interface to edit the OMR results
    - Processing of specific musical items
    - Advanced tasks like sampling symbols and training the neural glyph classifier

- [Reference](./reference/README.md)  -- Comprehensive technical descriptions  
    - Menus, boards, folders, outputs, editors
    - Known limitations, history of updates
    
- [Explanation](./explanation/README.md) -- How it works
    - Steps internals


[AGPL]:     https://en.wikipedia.org/wiki/GNU_Affero_General_Public_License
[DIVIO]:    https://docs.divio.com/documentation-system/
[MusicXML]: http://usermanuals.musicxml.com/MusicXML/MusicXML.htm
[OMR]:      https://github.com/Audiveris/audiveris/wiki/Project-Structure
[.omr]:     ./reference/outputs/omr.md
[Wiki]:     https://github.com/Audiveris/audiveris/wiki
