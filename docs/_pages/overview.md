---
layout: default
title: Overview
parent: Audiveris Handbook 5.2
nav_order: 1
---
# Overview

Audiveris is an open source software, published under the [AGPL](https://en.wikipedia.org/wiki/GNU_Affero_General_Public_License) V3
license.

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
