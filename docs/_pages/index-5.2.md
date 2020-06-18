---
layout: default
title: Audiveris Handbook 5.2
nav_order: 0
has_children: true
---
# Audiveris Handbook 5.2

Applies to Version 5.2

## Major updates of 5.2 over 5.1
{: .d-inline-block }

New
{: .label .label-green }

- User Interface
   - Ability to move and resize symbols
   - Snapping note heads to staff line/ledgers and stems
   - Repetitive input
   - Ability to merge/split parts via brace handling
   - Ability to merge systems
   - Ability to merge/split chords
   - Support for key signature cancel (key made of natural signs)
   - Improved support for chord names
   - Better handling of sentences, chord names, lyrics
   - Support for high DPI devices
   - Internationalization of all menus and dialogs
- Engine
   - Support for 1-line percussion staves
   - Detection of 4-line and 6-line tablatures
   - New algorithm for time slots and voices
   - Support for implicit tuplets
   - Support for merged grand staff configuration
   - Support for cross heads
   - Improved text dispatching between systems
- Project
   - Automated checks for update on GitHub
   - Documentation handled on GitHub Pages
- Java
   - Support of Java 11

For the end-user, the bulk of updates over previous release concerns the UI
part and especially the ability to correct the outputs of OMR engine.
Therefore, most of handbook modifications are located in the
[Manual edition](/edition/README.md) chapter.

## Intended audience
This handbook is meant for Audiveris user.
To ease the reading for a new comer as for a more advanced user, it is organized
as a progressive sequence of chapters.

It is just a user manual, not a developer manual, although some material is made available through
Audiveris [Wiki](https://github.com/Audiveris/audiveris/wiki) to ease the software learning curve
for any potential developer.

## Handbook structure

1. [Overview](/overview.md):
The purpose of Audiveris OMR software.

2. [Installation](/install/README.md):
How to install or build the program.

3. [Quick tour](/quick/README.md):
A very early tour, just to introduce a minimal usage of the software.

4. [Main features](/main/README.md):
Thorough description of software main features.

5. [UI tools](/ui_tools/README.md):
Tools to correct OMR outputs (SIGNIFICANTLY ENHANCED)

6. [UI in action](/edition/README.md):
Examples of typical correction scenarios (TO BE REWORKED)

7. [Advanced features](/advanced/README.md):
Features only relevant for an advanced usage of Audiveris.

8. [References](/references.md):
Not meant to be read from A to Z, just a collection of descriptions.
