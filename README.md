1

![](https://github.com/Audiveris/docs/blob/master/images/SplashLogo.png)

2

[[https://github.com/Audiveris/docs/blob/master/images/SplashLogo.png|Audiveris Logo]]

# Audiveris - Open-source Optical Music Recognition (v5.2)

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
[MusicXML][musicxml] 3.0 format.
Other exporters are expected to build upon OMR data to support other target formats.

## Installing and running (Windows only)

Refer to HandBook [Binaries][binaries] section.

## Building and running (Windows, MacOS and Linux)

Refer to HandBook [Sources][sources] section.

NOTA for GitHub users:
- Audiveris "*master*" branch is updated only when a new release is published.
- Instead, Audiveris development happens continuously in "*development*" branch, so checkout and pull
this *development* branch to get and build the latest version.
- See details in this dedicated [Wiki article][workflow].

## Further Information

Users and Developers are advised to read the specific [User Handbook for 5.2 release][handbook],
and the more general [Wiki][audiveris-wiki] set of articles.

## Releases

All releases are available on [Audiveris Releases][releases] page.

The most recent stable version is release 5.2, published on June 2021.

[audiveris-wiki]: https://github.com/Audiveris/audiveris/wiki
[workflow]:       https://github.com/Audiveris/audiveris/wiki/Git-Workflow
[audiveris-eg]:   htps://github.com/Audiveris/audiveris-eg
[musicxml]:       http://www.musicxml.com/
[imslp]:          https://imslp.org/
[handbook]:       https://audiveris.github.io/audiveris/_pages/index-5.2/
[binaries]:       https://audiveris.github.io/audiveris/_pages/install/binaries/
[sources]:        https://audiveris.github.io/audiveris/_pages/install/sources/
[releases]:       https://github.com/Audiveris/audiveris/releases
