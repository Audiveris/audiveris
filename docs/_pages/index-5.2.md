---
layout: default
title: Audiveris Handbook 5.2
nav_order: 0
has_children: true
has_toc: false
---
# Audiveris Handbook 5.2
{: .no_toc }

This documentation applies to release 5.2 and later.

Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}
---

## Intended audience

This handbook is meant for Audiveris end-user.
To ease the reading for a new comer as for a more advanced user, it is organized
as a progressive sequence of chapters.

It is just a user manual, a true developer documentation is still to be written.
Some material is already made available through
Audiveris [Wiki](https://github.com/Audiveris/audiveris/wiki) to ease the software learning
curve for any potential developer.

## Overview

Audiveris is an open source software, published under the
[AGPL](https://en.wikipedia.org/wiki/GNU_Affero_General_Public_License) V3 license.

It is an **O**ptical **M**usic **R**ecognition (OMR) application,
featuring an OMR engine coupled with a dedicated editor.

It recognizes music from digitized images and converts it into
computer-readable symbolic format.
This enables further music processing by any external notation editor,
most notably: digital playback, transposition and arrangement.

Audiveris  provides outputs in two main digital formats: its OMR format and
standard MusicXML format.
* [OMR](https://github.com/Audiveris/audiveris/wiki/Project-Structure) is the
Audiveris specific format, a documented open source format based on XML.
* [MusicXML](http://usermanuals.musicxml.com/MusicXML/MusicXML.htm) has been
designed for score interchange and is supported as input/output by almost every
music notation program today.

Not any kind of sheet music can be handled. Audiveris engine has been designed
to process scores written in the _Common Western Music Notation_ (CWMN)
with the following limitations:
* Handwritten scores aren't supported (only printed scores are),
* Only common musical symbols are supported.

Because the accuracy of OMR engine is still far from perfection,
Audiveris application provides a graphical user interface specifically focused
on quick verification and manual correction of the OMR outputs.

More sophisticated music editors, such as MuseScore or Finale, can be used on
Audiveris MusicXML output.
They can even be directly connected via simple [plugins](/advanced/plugins.md).

## Major updates of 5.2 over 5.1

- User Interface
   - Ability to move and resize symbols
   - Snapping note heads to staff line/ledgers and stems
   - Repetitive input
   - Ability to merge/split parts via brace handling
   - Ability to merge systems
   - Ability to merge/split chords
   - Ability to modify voice or time slot for chords
   - Support for compound notes (head + stem)
   - Support for key signature cancel (key made of natural signs)
   - Improved support for chord names
   - Better handling of sentences, chord names, lyrics
   - Ability to correct beam thickness scale
   - Ability to limit book processing on selected sheets
   - Support for high DPI devices
   - Internationalization of all menus
- Engine
   - Better handling of poor-quality scores
   - Support for 1-line percussion staves
   - Detection of 4-line and 6-line tablatures
   - New algorithm for time slots and voices
   - Support for non measure-long whole rests
   - Support for implicit tuplets
   - Support for merged grand staff configuration
   - Support for cross heads
   - Plugin mechanism upgraded for multi-movement exports
- Project
   - Automated checks for update on GitHub
   - Documentation handled on GitHub Pages
- Java
   - Support of Java 11

For the end-user, the bulk of updates over previous release concerns the user interface
part and especially the ability to correct the outputs of OMR engine.
Therefore, most of handbook modifications are located in the
[UI chapter](/ui.md).

## Handbook content

1. [Installation](/install/README.md):
How to install or build the program.

1. [Quick tour](/quick/README.md):
A very early tour, just to introduce a minimal usage of the software.

1. [Main features](/main/README.md):
Thorough description of software main features.

1. [UI](/ui.md):
Main tools and typical examples to correct OMR outputs.

1. [Advanced features](/advanced/README.md):
Features only relevant for an advanced usage of Audiveris.

1. [References](/references.md):
Not meant to be read from A to Z, but when a specific item needs to be checked.
