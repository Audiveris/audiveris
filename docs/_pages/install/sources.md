---
layout: default
title: Building from sources
nav_order: 4
parent: Installation
---
## Building from sources (Windows, MacOS, Linux, ArchLinux)
{: .no_toc }

{: .note }
For GitHub users:
- Audiveris "*master*" branch is updated only when a new release is published.
- Instead, Audiveris development happens continuously in "*development*" branch, so checkout and pull
this *development* branch to get and build the latest version.
- See workflow details in this dedicated [Wiki article][workflow].

Table of contents
{: .no_toc .text-delta }
1. TOC
{:toc}
---

### Dependencies

* [Git][git]: version control system.

* [Gradle][gradle]: build tool.

* [Java Development Kit (JDK)][jdk]: version {{ site.java_version }}
 (higher numbers may work, to be confirmed).
  Audiveris {{ site.audiveris_version }} runs only on 64-bit architectures.

* Tesseract OCR: The Tesseract *libraries* are automatically pulled as Gradle dependencies,
but you will need the Tesseract *language files* for this OCR to work properly.  
Please check [OCR languages](./languages.md) section.

* [FreeType library][freetype]: Unix-like platforms (including MacOS) need FreeType in your $PATH
to handle those specific PDFs that contain vector graphics.  
Fortunately, every known Unix-like OS distribution already contains a package for FreeType.

### Download, build and run
To download the Audiveris project, use the following command in a directory of your choice:

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
To use the `development`branch with its latest features, use:
```sh
git checkout development

# To make sure you grab even the latest updates:
git pull --all
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

Please note that all these commands use **gradle wrapper** (`gradlew`) which, behind the scenes,
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

[freetype]: https://www.freetype.org
[git]:      https://git-scm.com
[gradle]:   https://gradle.org
[jdk]:      http://www.oracle.com/technetwork/java/javase/downloads/index.html
[workflow]: https://github.com/Audiveris/audiveris/wiki/Git-Workflow
