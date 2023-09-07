---
layout: default
title: Updates
nav_order: 8
has_children: false
---
# Software updates
{: .no_toc }

As detailed in the [Git Workflow](https://github.com/Audiveris/audiveris/wiki/Git-Workflow) article
on Audiveris Wiki:

- Audiveris development is performed on the specific GitHub "**development**" branch.   
  It is thus continuously available for anyone that pulls and builds the software from source.

- The default "**master**" branch is updated only for software releases.   
  At this time only, a new release is created and uploaded to the [Releases section](https://github.com/Audiveris/audiveris/releases) on GitHub site.

---
Table of contents
{: .text-delta }

1. TOC
{:toc}
---

### 5.4 (on-going)

- User Interface
  - Ability to display some inters (augmentation dots by default) in jumbo mode
  - Ability to stop current book processing at next step end
  - Ability to clear the log window  
- Documentation
  - Support for a PDF version of Audiveris Handbook

### 5.3

- User Interface
  - Editing of staff geometry, at individual line and global staff levels
  - User management of score logical parts, and their mapping to sheet physical parts
  - User editing of newly supported features (multi-measure rests, octave shifts, etc)
- Engine
  - Support for drums unpitched notation
  - Support of several musical font families for better template matching
  - Support of several text font families
  - Support for multi-measure rests
  - Support for measure repeats for 1, 2 and 4 measures
  - Support for octave shifts (single and multi-line shifts)
  - Support for endings with no left leg, with default number generation
  - Support for two populations of beam height and head size
  - Support for beam thickness specification
  - Support for fingering and plucking
- Project
  - Use of Tesseract 5.x in legacy mode
  - Support of MusicXML 4.0
  - Generation of Schemas documentation
- Java
  - Support of Java 17

### 5.2

- User Interface
   - Ability to move and resize symbols
   - Snapping note heads to staff line/ledgers and stems
   - Repetitive input
   - Ability to merge/split books
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
