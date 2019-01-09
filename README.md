# audiveris

This repository contains source code for the latest generation of Audiveris Optical
Music Recognition (OMR) application.

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

## Specific features for V6

### Full-Page Classifier service

For the full page classifier, clone the [Detection-Service-repository][detection-service]
to a directory of your choice:

```sh
git clone https://github.com/tuggeluk/Detection_Service.git
```

This classifier needs [Python 2/3][python], including the modules:
numpy, tensorflow, pandas, pillow and flask.
To install those, execute:

```sh
pip install numpy tensorflow pandas pillow flask
```

To be able to run the neural network, a strong nvidia graphics card with CUDA support is required.

After that, you need to have the full page classifier model in the root directory of the detection
service.
For example:

```
Detection_Service
├── classifier
├── class_utils
├── demo
├── models
├── main.py
├── ...
└── trained_models_18_may
    ├── RefineNet-Res101.data-00000-of-00001
    ├── RefineNet-Res101.index
    └── RefineNet-Res101.meta
```

Now, the detection service can be started with:

```py
python main.py
```

### Patch Classifier

To be able to run the patch classifier, you need to stow the model to either the training folder:

```sh
%appdata%\AudiverisLtd\audiveris\data\train # Windows
~/Library/AudiverisLtd/audiveris/train # Mac
~/.local/share/AudiverisLtd/audiveris/train # Linux/Unix
```

Or to the resource folder(`./res` in the audiveris repo).

The file has to match the name `patch-classifier.h5` exactly.

## Further Information

Users and Developers are advised to read the specific [User Handbook for 5.1 release][handbook],
and the more general [Wiki][audiveris-wiki] set of articles.

[audiveris]:            https://github.com/Audiveris
[audiveris-eg]:         htps://github.com/Audiveris/audiveris-eg
[audiveris-wiki]:       https://github.com/Audiveris/audiveris/wiki
[binaries]:             https://bacchushlg.gitbooks.io/audiveris-5-1/content/install/binaries.html
[handbook]:             https://bacchushlg.gitbooks.io/audiveris-5-1/content/
[imslp]:                https://imslp.org/
[detection-service]:    https://github.com/tuggeluk/Detection_Service
[musicxml]:             http://www.musicxml.com/
[python]:               https://www.python.org/downloads/
[releases]:             https://github.com/Audiveris/audiveris/releases
[sources]:              https://bacchushlg.gitbooks.io/audiveris-5-1/content/install/sources.html
