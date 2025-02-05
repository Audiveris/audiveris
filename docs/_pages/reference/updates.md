---
layout: default
title: Updates
parent: Reference
nav_order: 9
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
{: .text-epsilon }
- TOC
{:toc}
---

## 5.4
(Feb 2025)

- User Interface
  - New feature to handle the local collection of OCR languages and download additional ones
   on demand from the GitHub Tesseract site.
  - Creation of the Binarization adjustment board to dynamically adjust the binarization
    filters and settings. Contributed by [Michael Porter].
  - Past the SYMBOLS step, the manual removal of any `Inter` now triggers a dynamic rebuilding of glyphs,
   as if the removed `Inter` had never existed.
  - Improvement and extension of default/book/sheet parameters (interline, barline)
  - Ability to display some inters in jumbo mode to ease a visual inspection
  -- by default this applies to augmentation dots.
  - Ability to gracefully stop the current book processing at the next step end.
  - Ability to clear the log window -- the log file remains intact.
  - Waiting message for long loading of book or sheet.
  - New `Preferences` dialog with new policy for the selection of output folder.
- Engine
  - Improved handling of implicit tuplets.
  - Support for metronome mark.
  - The recognition of fermata no longer requires separate recognitions of fermata-arc and fermata-dot.
  - The processing of an input image with no white margin around the staves is now possible.
  - Staff lines are now less impacted by isolated chunks.
- Project
  - A Linux Flatpak installer, gathering the needed libraries including proper Java environment,
    is provided on FlatHub. Contributed by [Martin Wilck] and [Jan-Willem Harmannij].
  - The Windows installer pre-populates the user `config`/`tessdata` folder
    with the `eng` Tesseract language.
  - For licence reasons, JPodRenderer had to be replaced by Apache PDFBox to load PDF images.
- Documentation
  - Global handbook restructuring.
  - Support for a PDF version of handbook.
  - Re-reading of the whole documentation. Contributed by [Brian Boe], native American-English speaker!
- Java
  - In `Audiveris.bat` (used by the Windows installer) and `Audiveris` start scripts, 
    the Java version is checked before the application is launched.
  - Support of Java 21.
  - Upgrade to Gradle 8.5 to support Java 21.
  - Removal of all deprecated features such as JGoodies PanelBuilder, Observer/Observable, 
    class.newInstance(), etc.

## 5.3
(Jun 28, 2023)

- User Interface
  - Editing of staff geometry, at individual line and global staff levels
  - User management of score logical parts, and their mapping to sheet physical parts
  - User editing of newly supported features (multi-measure rests, octave shifts, etc)
- Engine
  - Support for drums unpitched notation. Contributed by [Brian Boe].
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

## 5.2
(Jan 18, 2022)

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
   - Chords popup menu offers "Next In Voice" relation to better guide voice/slot assignments
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
   - Support for rest chords interleaved within beam head chords
   - Support for non measure-long whole rests
   - Support for implicit tuplets
   - Support for merged grand staff configuration
   - Support for cross heads
   - Improved tie slurs detection on staff, across systems and across pages
   - Plugin mechanism upgraded for multi-movement exports
- Project
   - Automated checks for update on GitHub
   - Documentation handled on GitHub Pages
   - How to use Gimp to improve input. Contributed by [Baruch Hoffart].
   - How to enlarge low resolution scans. Contributed by [Ram Kromberg].
- Java
   - Support of Java 11
   - Refined dependencies on Java EE, JAXB, etc away from Java 8

## 5.1
(Dec 13, 2018)

- User Interface
  - Visual separation of shared heads
  - User assignment of fixed-shape symbols
  - Ability to modify scale parameters
- Engine
  - Augmentation dots apply to shared heads
- Project
  - Windows binary installers (32 and 64). Contributed by [Baruch Hoffart].
- Java
   - Support of Java 8

## 5.0

- Engine
  - Creation of `.omr` project files
- Project
  - Migration to GitHub & Gradle. Contributed by [Maxim Poliakovski].

## 4.2

- Distribution Changes:
  - Installation: Major OSes Windows and Linux and machine architectures x86 and x64 are supported via dedicated installers. The installers take care of all Audiveris dependencies (Java runtime, C++ runtime, musical font, etc).
  - Nota: Support for macOS has been dropped from the scope of this release to avoid further delays.
  - OCR: A few selected languages are pre-installed with Audiveris distribution (deu, eng, fra, ita). Additional languages can be supported by downloading the related trained data from the dedicated Tesseract web page.
  - NetBeans: A pre-populated nbproject folder provides NetBeans support out-of-the-box.
- New Features:
  - OCR: Tesseract V3.02 has been integrated in place of oldish V2.04 version. This much more powerful engine has led to a global redesign of text handling within Audiveris. There is now a dedicated TEXTS step which performs a layout analysis on each whole system image and transcribes the identified textual items. Note also that several languages can be selected at the same time.
  - Binarization: Extracting foreground pixels from the background has long been performed using a global threshold on pixel gray value. Even images with non-uniform illumination can now be processed with an adaptive filter which takes into account the neighborhood of the current pixel.
  - Glyph recognition: The major part of neural network input consists in moments which capture glyph key characteristics. Former Hu geometric moments have been replaced by ART moments (Angular Radial Transform, as used by MPEG-7) which are less sensible to noise.
  - Plugins: Audiveris MusicXML output can be "piped" to external softwares such as score editors or MIDI sequencers, through a flexible plugin mechanism. Consequently, these features have been removed from Audiveris application.
- Bug Fixes:
  - PDF input: Several free Java libraries have been tested (PDFRenderer for a long time, then JPedal and PDFBox) but none was really satisfactory. Hence support for PDF input is now delegated to a Ghostscript sub-process, a fully functional and perennial solution.
- Other Changes:
  - Doc: A comprehensive Handbook is now available from Audiveris web page, as well as the API JavaDoc of the current release. The former installation tab is now merged with the first chapter of the handbook.
  - Wiki: The online Audiveris Wiki contains detailed documentation about how to process each score of the set of examples available on MakeMusic/Recordare site. It is also used to gather information about evolutions being considered for Audiveris software.

## 4.1

- Distribution Changes:
  - Several installation files have been published, all using the 4.1beta core name. This reflects the status of continuous development rather than stable release of the software.
- New Features:
  - Filaments: They are long glyphs representing the core of either horizontal or vertical lines (staff lines candidates and barlines candidates respectively). These filaments are formalized in natural splines, which are sequences of Bézier curves with continuity up to the second derivative.
  - Grid: The staff lines and barlines are connected into a grid of sometimes rather wavy lines. The grid itself is taken as the referential for all the other glyphs, whatever the potential skew or other distortion of the image, and thus saving the need for any pre-processing. Moreover, one can on demand easily build and save a "dewarped" version of the initial image.
  - Scale: Additional key informations are derived from run length histograms (jitter on staff line thickness and spacing, typical beam height, whether the image is music or not).
  - Systems: The boundary between two consecutive systems is now a broken line, resulting from the incremental inclusion of glyphs into their nearest system.
  - Training: Besides full sheets taken as training samples, the user can select a mode that takes every manual assignment as a new training sample.
  - Symbols: The HEAD_AND_FLAG family of compound symbols no longer exists, thanks to an aggressive strategy in glyph split pattern.
- Known Issues:
  - OCR: We are still stuck to the old Tesseract version (2.04). The new Tesseract generation (3.x) has been out for more than one year now but still lacks a Java connection under Windows.
- Other Changes:
  - Time: All time values, such as offsets within a measure, are computed using rational values, which makes them independent of the score divisions value.

## 4.0

- New Features:
  - Display: The main application window has been simplified. Only two views are now shown for each sheet: Picture (focused on input image) and Data (focused on items detected). We no longer have separate windows for sheet and score. The score elements are displayed in a translucid manner on top of the sheet glyphs they represent, in order to visually catch any discrepency. Separate voices can be displayed each in a specific color
  - Every other window (Log, Errors, Boards) can be displayed or hidden, and each individual board can be selected at will
  - Font: Former symbol bitmaps have been dropped for the use of a TrueType music font (Stoccata.ttf then MusicalSymbols.ttf). This allows endless zooming of displays and printouts with no loss of quality
  - The font is even used to build artificial symbols used for initial training of the neural network
  - Print: Ability to print the resulting score into a PDF file
  - Multi-page: Multi-page images (using PDF or TIFF format) can be transcribed to multi-page scores in memory
  - A disk-based prototype version, using a map/reduce approach, allows to combine existing MusicXML pages into a single score

## 3.4
(Dec 14, 2012)

- Distribution Changes:
  - Libraries: All the external jars (23 as of this writing) needed to rebuild and/or run Audiveris are now provided in a dedicated /lib folder available in the download area. A developer can still pick up a newer jar version from the Internet.
  - Player: The XenoPlay MusicXML player has been replaced by a better player, named Zong!
- New Features:
  - Bench data: To allow the analysis of multiple batch runs, and compare the recognition efficiency, each sheet processing can log key data in a dedicated file. For the same purpose, time-out values can be defined for script or step processing.
  - Bar Lines: The user can now interactively assign / deassign a bar line that defines parts, thus recreating the systems from scratch.
  - Constants: All application constants can now be set from the CLI with the -option keyword. This complements the ability to set them from the Tools - Options UI menu.
  - Dots: Support for double dots, ability to assign the role of any dot (augmentation, repeat bar line, staccato)
  - Horizontals: Horizontal entities (ledgers, endings) can now be forced or deassigned manually.
  - MIDI Player: The MIDI playback is now driven from a separate console window, borrowed from Zong! player.
  - OCR: Tesseract OCR is now available under both Windows and Linux.
  - Score: From a dedicated Shape palette, the user can Drag n' Drop a (virtual) glyph to either the score view or the sheet view, thus injecting entities directly into the score structure.
  - ScoreView: The zoom of the score view can now be adjusted at will, thanks to a slider and better symbol bitmap definitions. A next version will replace them with the use of Stocatta true-type font.
  - Time Signature: The user can now enter any custom time signature, defining numerator and denominator values explicitly.
  - Time Slots: Within a measure, the time slots are meant to gather notes that begin at the same moment in time. The user can now choose at the score level the policy for determining the time slots, either through stem alignment or through note head alignment.
  - Tuplets: 6-tuplets are now supported, as well as tuplets mixing beamed notes with other notes (flagged notes, rests, ...).
  - UI: A new board (Shape palette) is available. It allows drag n' drop for entity injection, easier navigation through shape ranges, and shape assignment by double-click.
  - UI: All boards now have an expand / collapse mechanism, thus allowing to save room in the column of boards.
- Bug Fixes:
  - Player: The Zong! Player is now more tolerant with respect to measure defects. It no longer throws an exception whenever the notes durations within a measure are not consistent with the measure expected duration.
  - Player Data: The data part of Zong player is now provided as a resource in a dedicated jar file, thus allowing the launching of Audiveris from any location of your computer.
  - Exception handler have been removed from all unitary tests, so that the results are clearly seen as successes or failures
- Known Issues:
  - Virtual Glyphs: For the time being, the (virtual) glyphs created by direct injection cannot be moved or resized once they have been dropped from the Shape palette to their target view. However, they can be deleted and re-injected (this workaround addresses a move but not a resize).
- Other Changes:
  - Images: Support for most pixel sizes.
  - Lyrics: Much better handling of lyric text pieces, with the ability for the user to enter extension sign or to split words with a space. The OCR can process several text lines as a whole, which often leads to better results.
  - Symbols: The symbols bitmap definitions (in the /symbol folder) have been refined with at least a 16-pixel interline definition, resulting in better display notably in score view.
  - Tiff: Images are forwarded to Tesseract OCR by memory, avoiding temporary files


[Baruch Hoffart]:         https://github.com/Bacchushlg
[Brian Boe]:              https://github.com/brian-math
[Jan-Willem Harmannij]:   https://github.com/jwharm
[Martin Wilck]:           https://github.com/mwilck
[Maxim Poliakovski]:      maximumspatium@googlemail.com
[Michael Porter]:         https://github.com/mgporter
[Ram Kromberg]:           https://github.com/RamKromberg