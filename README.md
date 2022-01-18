```diff
-- -------------------------------------------------------------------------------------------------
-- WARNING
-- Audiveris development is performed on "development" branch, while default "master" branch
-- is reserved for releases.
--
-- On "development" branch, you will need jdk-17.0.1
--
-- Due to recent closing of jcenter, a needed component (JPodRenderer v5.6) is no longer available,
-- it has been temporarily replaced by JPodRenderer v5.5.1
-- -------------------------------------------------------------------------------------------------
```

![](https://github.com/Audiveris/docs/blob/master/images/SplashLogo.png)

# Audiveris - Open-source Optical Music Recognition (v5.2.x)

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
In the future, other exporters are expected to build upon OMR data to support other target formats.

## Installing binaries (Windows)

On GitHub [Releases][releases] page, an Audiveris 5.2 installer is available for Windows.

It takes care of the whole installation, including needed libraries like Tesseract OCR,
but assumes that you already have a suitable Java Runtime Environment installed.   
At least Java version 11 is required.
You can download it from Oracle at [https://www.oracle.com/fr/java/technologies/javase/jdk11-archive-downloads.html](https://www.oracle.com/fr/java/technologies/javase/jdk11-archive-downloads.html).   
Java versions above 11 *may* work, but have not been tested yet.

And since Oracle provides Java 11 and above only for 64-bit architectures, the same restriction
applies to Audiveris 5.2 as well.
Should you really need a 32-bit Audiveris version, you can still use old Audiveris 5.1 version
from the [Releases][releases] page.

For further installation details, please refer to HandBook [Binaries][binaries] section.

## Building from sources (Windows, MacOS and Linux)

**NOTA** for GitHub users:
- Audiveris "*master*" branch is updated only when a new release is published.
- Instead, Audiveris development happens continuously in "*development*" branch, so checkout and pull
this *development* branch to get and build the latest version.
- See workflow details in this dedicated [Wiki article][workflow].

You will need the typical tools: git, gradle and Java Development Kit (JDK) 11.

All libraries, including Tesseract OCR libraries, will get pulled as Gradle dependencies
but you will have to download Tesseract language data files.  

And we have to make it clear, because the same issues are posted again and again:
1. Audiveris needs the **old 3.04 Tesseract language files**,
new 4.x Tesseract is not suitable for detecting and processing text on music images.
2. No Tesseract executable needs to be installed, since Tesseract is used via **libraries**.
3. You can have other Tesseract versions installed, they will not impede Audiveris behavior,
provided that Audiveris can find its needed Tesseract language files
(typically via **TESSDATA_PREFIX** environment variable).

For further building details, please refer to HandBook [Sources][sources] section.

## Further Information

Users and Developers are advised to read the specific [User Handbook for 5.2 release][handbook],
and the more general [Wiki][audiveris-wiki] set of articles.

## Releases

All releases are available on [Audiveris Releases][releases] page.

[audiveris-wiki]: https://github.com/Audiveris/audiveris/wiki
[workflow]:       https://github.com/Audiveris/audiveris/wiki/Git-Workflow
[audiveris-eg]:   htps://github.com/Audiveris/audiveris-eg
[musicxml]:       http://www.musicxml.com/
[imslp]:          https://imslp.org/
[handbook]:       https://audiveris.github.io/audiveris/_pages/index-5.2/
[binaries]:       https://audiveris.github.io/audiveris/_pages/install/binaries/
[sources]:        https://audiveris.github.io/audiveris/_pages/install/sources/
[releases]:       https://github.com/Audiveris/audiveris/releases
[win-installer]:  https://github.com/Audiveris/audiveris/releases/download/5.2.1/Audiveris_Setup-5.2.1-windows-x86_64.exe
