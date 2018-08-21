# audiveris

This repository contains source code for the latest generation of Audiveris optical
music recognition (OMR) engine.

## Main features

As opposed to Audiveris [earlier generation][audiveris-eg], which was a stand-alone OMR application composed
of an engine and a (limited) user interface, this repository is focused on the OMR engine.

The internals of the OMR engine are made publicly available, either directly by XML-based ".omr" 
project files or via the Java API of this software.

The engine can directly export data using [MusicXML][musicxml] 3.0 format, via an integrated exporter.
Other exporters could build upon the engine to support other target formats.

NOTA: The engine provides a small integrated UI which is meant for the developer to analyze, 
tune or train the various parts of the engine, but not to correct the final score.
Full GUIs, meant for the end-user, are expected to be provided by external editors.

## Building and running

First of all, you'll need the following dependencies installed and working from
the command line:

+ [Java Development Kit (JDK) version 7 or 8 (version 8 is recommended; version 9 is not yet supported)][java].
  **Please ensure you're running a 64-bit JVM. Audiveris doesn't support a 32-bit
  JVM because deeplearning4j is 64-bit only.**
+ [Git](https://git-scm.com) version control system.
+ [Gradle command line tool](https://gradle.org) for building Audiveris from source

Besides the above mentioned tools you'll need to have Tesseract language files for
[Tesseract OCR][tesseract] to work properly. Please keep in mind that Tesseract is mandatory
for both building and running Audiveris. __It's currently not possible to use
Audiveris without Tesseract.__

You'll need at least the english language data. Other required languages can be
installed, too. Please check [this guide][tesseract-wiki] for further details.

Moreover, opening PDFs containing vector graphics on Unix-like platforms
(including the Mac) requires [FreeType library][freetype] to be available in your `PATH` variable.
Fortunately, most OS distributions already contain a package for FreeType.

### Full Page Classifier

For the full page classifier, clone the [Detection-Service-repository][detection-service] to a directory of your choice:

```sh
git clone https://github.com/tuggeluk/Detection_Service.git
``` 

This classifier needs [Python 3][python], including the modules: numpy, tensorflow, pandas, pillow and flask. To install those, execute:

```sh
pip install numpy tensorflow pandas pillow flask
```

To be able to run the neural network, a strong nvidia graphics card with CUDA support is required.

After that, you need to have the full page classifier model in the root directory of the detection service. For example:

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

### Audiveris

To download the Audiveris project, use the following command in a directory of your choice:

```sh
git clone https://github.com/Audiveris/audiveris.git
``` 

This will create a sub-directory named "audiveris" in your current directory and populate it with
project material (notably source code and build procedure).

To be able to run the patch classifier, you need to stow the model to either the training folder:
```sh
%appdata%\AudiverisLtd\audiveris\data\train # Windows
~/Library/AudiverisLtd/audiveris/train # Mac
~/.local/share/AudiverisLtd/audiveris/train # Linux/Unix
```

Or the resource folder(`./res` in the audiveris repo). The file has to match the name `patch-classifier.h5` exactly.

Once inside the audiveris project directory, you can:

* Build the software via the command:

    ```sh
    gradle build
    ```
    
    This creates a `build` folder in the repository root, which amongst others contains a `distributions` folder, where the archived/zipped libraries are. To run audiveris from those files, execute:
    
    ```sh
    cd build/distributions
    # Extract the archive
    tar -xf Audiveris.tar
    #unzip Audiveris.zip
    # Run audiveris(Append arguments if needed)
    java -cp "Audiveris/lib/*" Audiveris
    ```

* Run the software, as GUI tool, via the command:

    ```sh
    gradle run
    ```
    
* Build JavaDoc via the command:

    ```sh
    gradle javadoc
    ```
    
    The generated JavaDoc is located at `build/docs/javadoc`.

## Further Information

Users and Developers are encouraged to read our [wiki][audiveris-wiki].

[java]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[tesseract]: https://github.com/tesseract-ocr/tesseract
[tesseract-wiki]: https://github.com/tesseract-ocr/tesseract/wiki
[freetype]: https://www.freetype.org
[audiveris-wiki]: https://github.com/Audiveris/audiveris/wiki
[audiveris-eg]: https://github.com/Audiveris/audiveris-eg
[audiveris]: https://github.com/Audiveris
[musicxml]: http://www.musicxml.com/
[detection-service]: https://github.com/tuggeluk/Detection_Service
[python]: https://www.python.org/downloads/