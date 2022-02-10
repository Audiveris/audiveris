---
layout: default
title: Updates
nav_order: 8
has_children: false
---
# Software updates
{: .no_toc }

As detailed in [Git Workflow](https://github.com/Audiveris/audiveris/wiki/Git-Workflow) article
on Audiveris Wiki:

- Audiveris development is performed on specific GitHub "**development**" branch.   
  It is thus continuously available for anyone that pulls and builds the software from source.

- The default "**master**" branch is updated only for software releases.   
  At this time only, a new release is created and uploaded to the [Releases section](https://github.com/Audiveris/audiveris/releases) on GitHub site.

---
Table of contents
{: .text-delta }

1. TOC
{:toc}
---

### 5.3
{: .d-inline-block }
on-going...
{: .label .label-yellow }

- User Interface
  - Edition of staff geometry, at line and global levels
- Engine
  - Better ledgers handling
  - Support for two populations of beam height
- Project
  - Support of MusicXML 4.0
  - Generation of Schemas documentation
- Java
  - Support of Java 17

### 5.2

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
   - Refined dependencies on Java EE, JAXB, etc away from Java 8

### 5.1

- User Interface
  - Visual separation of shared heads
  - User assignment of fixed-shape symbols
  - Ability to modify scale parameters
- Engine
  - Augmentation dots apply to shared heads
- Project
  - Windows binary installers (32 and 64)
- Java
   - Support of Java 8

### 5.0

- Engine
  - Creation of `.omr` project files
