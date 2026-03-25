# WARNING
### Beware of site [audiveris.com](https://audiveris.com/)!

```diff
- The site https://audiveris.com  (note the `.com` extension)
-  has nothing to do with Audiveris and seems to be a fraudulent site.
```

![](https://github.com/Audiveris/audiveris.github.io/blob/master/assets/images/audiveris.com.png)

```diff
- The site is aesthetically pleasing
-  and looks like an advertisement for Audiveris software.
- However, users report that links redirect to pages
-  dedicated to cryptocurrencies, sports betting, etc.
- It has all the hallmarks of a phishing site…
```

![](https://github.com/Audiveris/docs/blob/master/images/SplashLogo.png)
Logo crafted by [Katka](https://www.facebook.com/katkastreetart/)

# Audiveris - Open-source Optical Music Recognition

The goal of an OMR application is to allow the end-user to transcribe a score image into
its symbolic counterpart.
This opens the door to its further use by many kinds of digital processing such as
playback, music edition, searching, republishing, etc.

The Audiveris application is built around the tight integration of two main components:
an OMR engine and an OMR editor.
- The OMR engine combines many techniques, depending on the type of entities to be recognized
-- *ad-hoc* methods for lines, image morphological closing for beams, external OCR for texts,
template matching for heads, neural network for all other fixed-size shapes.   
Significant progresses have been made, especially regarding poor-quality scores,
but experience tells us that a 100% recognition ratio is simply out of reach in many cases.
- The OMR editor thus comes into play to overcome engine weaknesses in convenient ways.
The user can preselect processing switches to adapt the OMR engine before launching the
transcription of the current score.
Then the remaining mistakes can generally be quickly fixed
via the manual editing of a few music symbols.

## Key characteristics

* Good recognition efficiency on real-world quality scores (as those seen on the [IMSLP][imslp] site)
* Effective support for large scores (with up to hundreds of pages)
* Convenient user-oriented interface to detect and correct most OMR errors
* Available on Windows, Linux and macOS
* Open source

The core of engine music information (OMR data) is fully documented and made publicly available,
either directly via XML-based `.omr` project files or via the Java API of this software.   
Audiveris comes with an integrated exporter to write (a subset of) this OMR data into
[MusicXML][musicxml] 4.0 format.
In the future, other exporters are expected to build upon OMR data to support other target formats.

## Stable releases

On a rather regular basis, typically every 6 to 12 months, a new release is made available
on the dedicated [Audiveris Releases][releases] page.

The goal of a release is to provide significant improvements, well tested and integrated,
resulting in a software as easy as possible to install and use.

Since the release 5.5, an installer is provided for each of the main OSes
(**Windows**, **Linux** and **macOS**) and comes with a pre-installed Java Runtime Environment (JRE).
You can download any installer file from the **Assets** section, at the end of the chosen release:

| OS name | Installer file extension |
| :---    | :---   |
| Windows | `.msi` |
| Linux   | `.deb` |
| macOS   | `.dmg` |

Additionally for **Linux**, a _flatpak_ package, also with a suitable JRE included,
can be installed from the [Flathub] site.

See installers details in the handbook [installation] section.

## Development versions

The Audiveris project is developed on GitHub, the site you are reading.  
Any one can clone, build and run this software. 
The needed tools are `git`, `gradle` and a Java Development Kit (`jdk`),
as described in the handbook [sources section][sources].

There are two main branches in the Audiveris project:
- the `master` branch is the GitHub default branch;
we use it for releases, and only for them.
- the `development` branch is the one where all developments continuously take place;
Periodically, when a release is to be made, we merge the development branch into the master branch.

See details in the [Wiki article][workflow] dedicated to the chosen development workflow.

## Further Information

- For users: the Audiveris [User Handbook][handbook].
- For developers: the [Project Structure][project] and the [Wiki][audiveris-wiki] set of articles.

[audiveris-wiki]: https://github.com/Audiveris/audiveris/wiki
[Flathub]:        https://flathub.org/apps/org.audiveris.audiveris
[handbook]:       https://audiveris.github.io/audiveris/
[imslp]:          https://imslp.org/
[installation]:   https://audiveris.github.io/audiveris/_pages/tutorials/install/binaries/
[musicxml]:       http://www.musicxml.com/
[project]:        project-structure.md
[releases]:       https://github.com/Audiveris/audiveris/releases
[sources]:        https://audiveris.github.io/audiveris/_pages/tutorials/install/sources/
[workflow]:       https://github.com/Audiveris/audiveris/wiki/Git-Workflow
