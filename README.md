![](https://github.com/Audiveris/docs/blob/master/images/SplashLogo.png)

# Audiveris - Open-source Optical Music Recognition

The goal of an OMR application is to allow the end-user to transcribe a score image into
its symbolic counterpart.
This opens the door to its further use by many kinds of digital processing such as
playback, music edition, searching, republishing, etc.

Audiveris application is built around the tight integration of two main components:
an OMR engine and an OMR editor.
- The OMR engine combines many techniques, depending on the type of entities to be recognized
-- ad-hoc methods for lines, image morphological closing for beams, external OCR for texts,
template matching for heads, neural network for all other fixed-size shapes.   
Significant progresses have been made, especially regarding poor-quality scores,
but experience tells us that 100% recognition ratio is simply out of reach in many cases.
- The OMR editor thus comes into play to overcome engine weaknesses in convenient ways.
The user can preselect processing switches to adapt the OMR engine before launching transcription
of the current score.
Then the remaining mistakes can usually be quickly fixed via manual edition of a few music symbols.

## Key characteristics
* Good recognition efficiency on real-world quality scores (as seen on [IMSLP][imslp] site)
* Effective support for large scores (with up to hundreds of pages)
* Convenient user-oriented interface to detect and correct most OMR errors
* Available on Windows, Linux and MacOS
* Open source

The core of engine music information (OMR data) is fully documented and made publicly available,
either directly via XML-based `.omr` project files or via the Java API of this software.   
Audiveris comes with an integrated exporter to write (a subset of) this OMR data into
[MusicXML][musicxml] 4.0 format.
In the future, other exporters are expected to build upon OMR data to support other target formats.

## Installation

- For **Windows** only, an installer is provided.
- For **Windows**, **MacOS**, **Linux** and **ArchLinux**, you can download and build from sources:
    - Either from default "*master*" branch,
    - Or, preferably, from "*development*" branch.
    See details in the dedicated [Wiki article][workflow].

Using the installer or building from sources, in both cases you will need two additional
components:
1. Java environment (Java 17 minimum)
2. Tesseract language files (for version 4 and up)

Please read further information in [HandBook installation][installation] section.

## Further Information

Users and Developers are advised to read Audiveris [User Handbook][handbook],
and the more general [Wiki][audiveris-wiki] set of articles.

## Releases

All releases are available on [Audiveris Releases][releases] page.

[audiveris-wiki]: https://github.com/Audiveris/audiveris/wiki
[handbook]:       https://audiveris.github.io/audiveris/
[imslp]:          https://imslp.org/
[installation]:   https://audiveris.github.io/audiveris/_pages/install/README/
[musicxml]:       http://www.musicxml.com/
[releases]:       https://github.com/Audiveris/audiveris/releases
[workflow]:       https://github.com/Audiveris/audiveris/wiki/Git-Workflow
