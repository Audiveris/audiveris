# audiveris-ng

This repository contains source code for the new generation of Audiveris optical
music recognition software.

## CAUTION!

**The Audiveris project is currently transitioning its code base from Kenai.com to
Github. This will take approx. two weeks. We won't be able to accept pull
requests until this transition is finished (this message will disappear then).
Thank you for your patience!**

## Building and running

First of all, you'll need the following dependencies installed and working from
the command line:

+ [Java Development Kit (JDK) version 7 or later (version 8 is recommended)][1].
  **Please ensure you're running a 64-bit JVM. Audiveris doesn't support a 32-bit
  JVM because deeplearning4j is 64-bit only.**
+ [Git](https://git-scm.com) version control system.
+ [Gradle command line tool](https://gradle.org) for building Audiveris from source

Besides the above mentioned tools you'll need to have Tesseract language files for
[Tesseract OCR][2] to work properly. Please keep in mind that Tesseract is mandatory
for both building and running Audiveris. __It's currently not possible to use
Audiveris without Tesseract.__

You'll need at least the english language data. Other required languages can be
installed, too. Please check [this guide][3] for further details.

Moreover, opening PDFs containing vector graphics on Unix-like platforms
(including the Mac) requires [FreeType library][4] to be available in your $PATH.
Fortunately, every known OS distribution already contains a package for FreeType.

To build audiveris-ng from source, run the following command from the source code
directory:

`gradle build`

To run audiveris-ng as GUI tool, just issue

`gradle run`

## Developers guide

Developers are encouraged to read our (still incomplete) [developers documentation][5].

[1]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[2]: https://github.com/tesseract-ocr/tesseract
[3]: https://github.com/tesseract-ocr/tesseract/wiki
[4]: https://www.freetype.org
[5]: https://github.com/Audiveris/audiveris-v5/wiki