---
layout: default
title: Audiveris Handbook
nav_order: 0
has_children: true
has_toc: false
---
# Audiveris Handbook
{: .no_toc }

This documentation applies to release {{ site.audiveris_version }} and later.

---
Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}
---

## Intended audience

This handbook is meant for the Audiveris end-user.
To ease the reading for a newcomer as for a more advanced user, it is organized
as a progressive sequence of chapters.

It is just a user manual; a true developer documentation is still to be written.
Some material is already made available, through the
Audiveris [Wiki](https://github.com/Audiveris/audiveris/wiki)
and through the description of [.omr file format](./outputs/omr.md), to ease the software
learning curve for any potential developer.

## Overview

When questioned about Audiveris, ChatGPT answered:
> Audiveris is an open source Optical Music Recognition (OMR) software. It is used to scan sheet music and convert it into a machine-readable format, such as MusicXML or MIDI. This allows the music to be edited, played back, and shared digitally.

Not that bad! But let's try to describe it on our own...

Audiveris is an open source software, published under the
[AGPL](https://en.wikipedia.org/wiki/GNU_Affero_General_Public_License) V3 license.

It is an **O**ptical **M**usic **R**ecognition (OMR) application,
featuring an OMR engine coupled with an interactive editor.

It recognizes music from digitized images and converts it into
computer-readable symbolic format.
This enables further music processing by any external notation editor,
notably: digital playback, transposition and arrangement.

Audiveris  provides outputs in two main digital formats: its own OMR format and the
standard MusicXML format.
* [OMR](https://github.com/Audiveris/audiveris/wiki/Project-Structure) is the
Audiveris specific format, a documented open source format based on XML.
* [MusicXML](http://usermanuals.musicxml.com/MusicXML/MusicXML.htm) is  a *de facto* standard.
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
Audiveris MusicXML output.
They can even be directly connected via simple [plugins](./advanced/plugins.md)
to ease data transfer from Audiveris to these external editors.

## Handbook content

1. [Installation](./install/README.md):
How to install or build the program.

1. [Quick tour](./quick/README.md):
A very brief tour, just to introduce a minimal usage of the software.

1. [Main features](./main/README.md):
Thorough description of software main features.

1. [UI](./ui.md):
Main tools and typical examples to correct OMR outputs.

1. [Specific features](./specific/README.md):
Specific features recently added to Audiveris.

1. [Advanced features](./advanced/README.md):
Features only relevant for an advanced usage of Audiveris.

1. [References](./references.md):
Not meant to be read from A to Z, but when a specific item needs to be checked.

1. [Updates](./updates.md):
History of major software updates.
