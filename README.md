# audiveris

This repository contains source code for the latest generation of Audiveris optical
music recognition (OMR) application.

## Main features

Derived from Audiveris [earlier generation][6] (4.x versions), which was limited to fast processing
of small high-quality scores in memory, this repository (starting with Audiveris 5.x versions) is
significantly more ambitious:
* Good recognition efficiency on real-world quality scores (as seen on [IMSLP][10] site)
* Effective support for large scores (with up to hundreds of pages)
* Convenient user-oriented interface to detect and correct most OMR errors
* Openness to external access
* Available on Windows, Linux and MacOS

The core of engine music information (OMR data) is fully documented and made publicly available,
either directly via XML-based `.omr` project files or via the Java API of this software:
* Audiveris includes an integrated exporter, which can write a subset of OMR data into [MusicXML][8]
  3.0 format.
* Other exporters are expected to build upon this OMR data to support other target formats.

## Installing and running (Windows only)

TBD:------------------------------------------------------------------------------  
TBD: _Update this section once Windows installer gets available!_  
TBD:------------------------------------------------------------------------------

## Building and running (Windows, Linux, MacOS)

First of all, you'll need the following dependencies installed and working from
the command line:
* [Java Development Kit (JDK)][1], version 7 or 8 (preferred), but not 9 or 10 yet.
  Audiveris 5.1 can run on both 32-bit and 64-bit architectures.
* [Git](https://git-scm.com) version control system.

Beside the above mentioned tools, you'll need Tesseract language files for
[Tesseract OCR][2] to work properly.
Please keep in mind that Tesseract is mandatory
for both building and running Audiveris.
__It is currently not possible to use Audiveris without Tesseract.__  
You'll need at least the english (`eng`) language data.
Other languages can be installed too, like `deu`, `ita`, `fra`, etc.
Please check the [Tesseract guide][3] for further details.

Moreover, opening PDFs containing vector graphics on Unix-like platforms
(including the Mac) requires [FreeType library][4] to be available in your $PATH.
Fortunately, every known Unix-like OS distribution already contains a package for FreeType.

To download Audiveris project, use the following command in a directory of your choice:

`git clone https://github.com/Audiveris/audiveris.git`

This will create a sub-directory named "audiveris" in your current directory and populate it with
project material (notably source code and build procedure).

Now move to this "audiveris" project directory:

`cd audiveris`

Once in this "audiveris" project directory, you can:

* Build the software via the command:

    `./gradlew build` (Linux & Mac)

    `gradlew.bat build` (Windows)

* Run the software, as GUI tool, via the command:

    `./gradlew run` (Linux & Mac)

    `gradlew.bat run` (Windows)

## Further Information

Users and Developers are advised to read the specific [User Handbook for 5.1][9],
and the more general [Wiki][5].

[1]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[2]: https://github.com/tesseract-ocr/tesseract
[3]: https://github.com/tesseract-ocr/tesseract/wiki
[4]: https://www.freetype.org
[5]: https://github.com/Audiveris/audiveris/wiki
[6]: https://github.com/Audiveris/audiveris-eg
[7]: https://github.com/Audiveris
[8]: http://www.musicxml.com/
[9]: https://bacchushlg.gitbooks.io/audiveris-5-1/content/
[10]: https://imslp.org/
