---
layout: default
title: Building from sources
nav_order: 3
parent: Installation
---
# Building from sources (Windows, macOS, Linux, ArchLinux)
{: .no_toc }

{: .note }
> For GitHub users:
> - Audiveris "***master***" branch is updated only when a new release is published.
> - Instead, Audiveris development happens continuously in the "***development***" branch,
> so pull and checkout this *development* branch to get and build the latest version.
> - See workflow details in the dedicated [Wiki article][workflow].

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

## Tools needed

* [Git][git]: version control system.

* [Gradle][gradle]: build tool.

* [Java Development Kit (JDK)][jdk]: version {{ site.master_java_version }}
 (higher numbers may work, to be confirmed).  
  This is the minimum Java version needed for the *master* branch.
  The *development* branch may require a higher version, please
  check on the Audiveris [project site](https://github.com/Audiveris/audiveris/tree/development?tab=readme-ov-file#development-versions).  
  Audiveris {{ site.audiveris_version }} runs only on 64-bit architectures.

* [FreeType library][freetype]: Unix-like platforms (including macOS) need FreeType in our $PATH
to handle those specific PDFs that contain vector graphics.  
Fortunately, every known Unix-like OS distribution already contains a package for FreeType.

* Tesseract OCR: The Tesseract *libraries* are automatically pulled as Gradle dependencies,
but we will need some Tesseract *language files* for this OCR to work properly.  
Please check the [OCR languages](../../guides/main/languages.md) section.

## Clone, build and run
To clone the Audiveris project, we can use the following command in a directory of our choice:

```sh
git clone https://github.com/Audiveris/audiveris.git
```

This will create a sub-directory named "audiveris" in our current directory and populate it with
project material (notably source code and build procedure).

Now we move to this "audiveris" project directory:

```sh
cd audiveris
```

Once in this `audiveris` project directory, we can select the branch we want.   
By default, we are on `master` branch.  
To use the `development` branch with its latest features, we can use:

```sh
git checkout development

# And to make sure we grab even the latest updates:
git pull --all
```
We can build the software via the command:
```sh
# (Linux & Mac, or Cygwin terminal under Windows)
./gradlew build
```
```sh
# (Windows terminal)
gradlew.bat build
```

We can run the software, with its graphical interface, via the command:

```sh
# (Linux & Mac, or Cygwin terminal under Windows)
./gradlew run
```
```sh
# (Windows terminal)
gradlew.bat run
```

Please note that all these commands use the **gradle wrapper** (`gradlew`) which,
behind the scenes, takes care of getting and launching the proper gradle version.

## Alternative run

The gradle-based run, as described above, makes sure that all our potential modifications are
compiled before the application is launched.
This is the preferred approach for a developer.

However, if we don't modify the code and merely want to launch the 
application we don't need to go through gradle for each and every run.

Because, once we have built the software with the gradle `build` command as stated above,
we now have a `app/build/distributions` subfolder in the project root with tarred/zipped libraries:

```sh
# This content corresponds to the 5.5.0 version
app/build/distributions/
├── app-5.5.0.tar
└── app-5.5.0.zip
```

We can simply extract either one of these archives into a folder of our choice:

```sh
# Either extract the .tar archive
tar -xf app/build/distributions/app-5.5.0.tar
```

```sh
# Or extract the .zip archive
unzip app/build/distributions/app-5.5.0.zip
```

Then, we can repeatedly run Audiveris from those files,
appending arguments as desired (`-help` is just used as an example):

```sh
# (Linux & Mac, or Cygwin terminal under Windows)
./app-5.5.0/bin/Audiveris -help
```

```sh
# (Windows terminal)
app-5.5.0\bin\Audiveris.bat -help
```

[freetype]: https://www.freetype.org
[git]:      https://git-scm.com
[gradle]:   https://gradle.org
[jdk]:      http://www.oracle.com/technetwork/java/javase/downloads/index.html
[workflow]: https://github.com/Audiveris/audiveris/wiki/Git-Workflow
