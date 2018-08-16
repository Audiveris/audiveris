# audiveris

This repository contains source code for the latest generation of Audiveris optical
music recognition (OMR) engine.

## Main features

As opposed to Audiveris [earlier generation][6], which was a stand-alone OMR application composed
of an engine and a (limited) user interface, this repository is focused on the OMR engine.

The internals of the OMR engine are made publicly available, either directly by XML-based ".omr" 
project files or via the Java API of this software.

The engine can directly export data using [MusicXML][8] 3.0 format, via an integrated exporter.
Other exporters could build upon the engine to support other target formats.

NOTA: The engine provides a small integrated UI which is meant for the developer to analyze, 
tune or train the various parts of the engine, but not to correct the final score.
Full GUIs, meant for the end-user, are expected to be provided by external editors.

## Building and running

First of all, you'll need the following dependencies installed and working from
the command line:

+ [Java Development Kit (JDK) version 7 or 8 (version 8 is recommended; version 9 is not yet supported)][1].
  **Please ensure you're running a 64-bit JVM. Audiveris doesn't support a 32-bit
  JVM because deeplearning4j is 64-bit only.**
+ [Git](https://git-scm.com) version control system.

Besides the above mentioned tools you'll need to have Tesseract language files for
[Tesseract OCR][2] to work properly. Please keep in mind that Tesseract is mandatory
for both building and running Audiveris. __It's currently not possible to use
Audiveris without Tesseract.__

You'll need at least the english language data. Other required languages can be
installed, too. Please check [this guide][3] for further details.

Moreover, opening PDFs containing vector graphics on Unix-like platforms
(including the Mac) requires [FreeType library][4] to be available in your $PATH.
Fortunately, every known OS distribution already contains a package for FreeType.

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

### Arch Linux
For the AUR(Arch User Repository) there exist two packages which can be installed by hand(or with your AUR-helper of trust). One is [`audiveris`](https://aur.archlinux.org/packages/audiveris) which uses the [`5.1.0-rc` release](https://github.com/Audiveris/audiveris/releases/tag/5.1.0-rc) and the other one is [`audiveris-git`](https://aur.archlinux.org/packages/audiveris-git) which tracks the `master` branch. To install, simply execute:
```bash
git clone https://aur.archlinux.org/audiveris.git
#git clone https://aur.archlinux.org/audiveris-git.git
cd audiveris
makepkg -fsri
```
Or with an AUR-helper(pikaur/yaourt/...):
```bash
pikaur -S audiveris
#pikaur -S audiveris-git
```

## Further Information

Users and Developers are encouraged to read our [Wiki][5].

[1]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[2]: https://github.com/tesseract-ocr/tesseract
[3]: https://github.com/tesseract-ocr/tesseract/wiki
[4]: https://www.freetype.org
[5]: https://github.com/Audiveris/audiveris/wiki
[6]: https://github.com/Audiveris/audiveris-eg
[7]: https://github.com/Audiveris
[8]: http://www.musicxml.com/
