---
layout: default
title: Building from sources
nav_order: 2
parent: Installation
---
## Building from sources (Windows, Linux, MacOS)
{: .no_toc }

Table of contents
{: .no_toc .text-delta }
1. TOC
{:toc}
---

### Dependencies

* [Java Development Kit (JDK)][1]: version 11 (higher numbers may work, to be confirmed).
  Audiveris 5.2 runs only on 64-bit architectures.

* [Git](https://git-scm.com): version control system.

* [Tesseract OCR][2]: Audiveris engine delegates to Tesseract software the recognition of any
text item (lyrics, title, directions, part names, etc...) and you need Tesseract language files
for this OCR to work properly.  
Pick up at least the english (`eng`) language data.
Other languages can be installed too, like `deu`, `ita`, `fra`, etc.
Please check the [Tesseract guide][3] for further details and make sure to grab language data
for Tesseract **3.04** rather than 4.0.  
Note that you can still run Audiveris without any Tesseract language file, you will simply get a
warning at launch time, and of course any text recognition will not be effective.  
Language installation depends on your OS. Here are examples to add Russian language (`rus`):
  - Linux-Ubuntu: `sudo apt-get install tesseract-ocr-rus`
  - MacOS: `sudo port install tesseract-rus`
  - Windows:
    1. Download russian language data from
    [https://github.com/tesseract-ocr/langdata/tree/master/rus](https://github.com/tesseract-ocr/langdata/tree/master/rus)
    into `c:\Program Files (x86)\tesseract-ocr\tessdata`
    2. Make sure the environment variable `TESSDATA_PREFIX` contains the full path to the parent of
    `tessdata` folder, that is `c:\Program Files (x86)\tesseract-ocr\`.  

  At runtime, you can specify which languages should be tried by the OCR software.
  By default, these are `deu`, `eng`, `ita`, `fra` as specified by the application constant
  `org.audiveris.omr.text.Language.defaultSpecification`.  
  You can modify this default by changing the constant value either interactively
  (using the menu `Tools|Options`) or in batch
  (using `-option org.audiveris.omr.text.Language.defaultSpecification=ita+fra+eng+deu+rus)`.  
  You can also, using the `Book | Set Book Parameters` menu, change the language(s) default or
  override it at book and even sheet levels.
* [FreeType library][4]: Unix-like platforms (including MacOS) need FreeType in your $PATH to handle
those specific PDFs that contain vector graphics.
Fortunately, every known Unix-like OS distribution already contains a package for FreeType.

### Download, build and run
To download Audiveris project, use the following command in a directory of your choice:

```sh
git clone https://github.com/Audiveris/audiveris.git
```

This will create a sub-directory named "audiveris" in your current directory and populate it with
project material (notably source code and build procedure).

Now move to this "audiveris" project directory:

```sh
cd audiveris
```

Once in this `audiveris` project directory, you can select the branch you want.   
By default, you are on `master`branch.  
To use for example the `development`branch with its latest features, use:
```sh
git checkout development
```
You can build the software via the command:
```sh
# (Linux & Mac, or Cygwin terminal under Windows)
./gradlew build
```
```sh
# (Windows terminal)
gradlew.bat build
```

You can run the software, as GUI tool, via the command:

```sh
# (Linux & Mac, or Cygwin terminal under Windows)
./gradlew run
```
```sh
# (Windows terminal)
gradlew.bat run
```

Please note that all these commands use **gradle wrapper** (`gradlew`) which, behind the scene,
takes care of getting and launching the proper gradle version.

### Alternative run

The gradle-based run, as described above, makes sure that all your potential modifications are
compiled before the application is launched.
This is the preferred approach for a developer.

However, if you don't modify the code and merely want to launch the (un-modified)
application you don't need to go through gradle for each and every run.

Because, once you have built the software with the gradle `build` command as stated above,
you now have a `build/distributions` folder in the repository root with tarred/zipped libraries
(`Audiveris.tar` and `Audiveris.zip`).

Simply extract either one of the archives:

```sh
# Either extract the .tar archive
tar -xf build/distributions/Audiveris.tar
```

```sh
# Or extract the .zip archive
unzip build/distributions/Audiveris.zip
```

Then, you can repeatedly run audiveris from those files:
```sh
# Run audiveris (append arguments if so needed):
java -cp "Audiveris/lib/*" Audiveris
```

[1]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[2]: https://github.com/tesseract-ocr/tesseract
[3]: https://github.com/tesseract-ocr/tesseract/wiki
[4]: https://www.freetype.org
