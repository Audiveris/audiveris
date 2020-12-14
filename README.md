# audiveris

This repository contains source code for the latest generation of Audiveris Optical
Music Recognition (OMR) application.

```diff
! You are looking at the "head-classifier" branch which experiments a new kind of classifier,
! focused on note heads.
!
! To run this Audiveris branch, you first need to download the head-classifier trained model,
! a zip file that weighs about 2MB.
! It is not kept under source management but made available separately (see link below this note).
!
! Simply download the "head-classifier.zip" file and put it (unexpanded) in Audiveris "res" folder.
```
Trained model for head-classifier is available at [head-classifier.zip](https://drive.google.com/open?id=1AyaI-qG1taPxTKodAfSxigGOBI11_71v) location.

## Releases

All releases are available on [Audiveris Releases][releases] page.

The most recent stable version is release 5.1, published on December 13, 2018.

## Main features

Derived from Audiveris [earlier generation][audiveris-eg] (4.x versions),
which was limited to fast processing
of small high-quality scores in memory, this repository (starting with Audiveris 5.x versions) is
significantly more ambitious:

* Good recognition efficiency on real-world quality scores (as seen on [IMSLP][imslp] site)
* Effective support for large scores (with up to hundreds of pages)
* Convenient user-oriented interface to detect and correct most OMR errors
* Openness to external access
* Available on Windows, Linux and MacOS

The core of engine music information (OMR data) is fully documented and made publicly available,
either directly via XML-based `.omr` project files or via the Java API of this software:

* Audiveris includes an integrated exporter, which can write a subset of OMR data into
[MusicXML][musicxml] 3.0 format.
* Other exporters are expected to build upon this OMR data to support other target formats.

## Installing and running, for Windows only

Refer to HandBook [Binaries][binaries] section.

## Building and running, for Windows, MacOS, Linux and ArchLinux

Refer to HandBook [Sources][sources] section.

## Further Information

Users and Developers are advised to read the specific [User Handbook for 5.1 release][handbook],
and the more general [Wiki][audiveris-wiki] set of articles.

[audiveris-wiki]: https://github.com/Audiveris/audiveris/wiki
[audiveris-eg]:   htps://github.com/Audiveris/audiveris-eg
[musicxml]:       http://www.musicxml.com/
[imslp]:          https://imslp.org/
[handbook]:       https://bacchushlg.gitbooks.io/audiveris-5-1/content/
[binaries]:       https://bacchushlg.gitbooks.io/audiveris-5-1/content/install/binaries.html
[sources]:        https://bacchushlg.gitbooks.io/audiveris-5-1/content/install/sources.html
[releases]:       https://github.com/Audiveris/audiveris/releases
