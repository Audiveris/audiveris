
# Schemas

The `schemas` sub-project handles the generation of the technical documentation about the Book, Sheet and PlayList XML files.

## Purpose

Audiveris can export a transcribed score into the *de facto* standard MusicXML format for an easy
exchange with other music applications.
But this MusicXML data is only a part of the OMR data produced by Audiveris transcription process.

Audiveris stores all its transcription data about a given Book into a dedicated project file,
which is just a Zip archive with a `.omr` extension.

Internally, the archive contains one central `book.xml` file and as many `sheet#N.xml` files as
there are sheets in the book, together with one black & white `BINARY.png` image file per sheet.
In addition, there can be other images, such as `GRAY.png` files if so desired.

For example, the processing of a 2-page input image will typically result in the following project
file structure:
```
├── book.xml
├── sheet#1
│   ├── BINARY.png
│   └── sheet#1.xml
└── sheet#2
    ├── BINARY.png
    └── sheet#2.xml
```

As explained in
[HandBook .omr files](https://audiveris.github.io/audiveris/_pages/outputs/omr/) section,
we provide for each of the two kinds of XML files (`book.xml` and `sheet#N.xml`) a documentation
set made of:
- a `.xsd` formal schema description
- a `.html` user documentation

This *"Schemas documentation"*, as it is called, is packed as a ZIP archive published in
[Audiveris Releases](https://github.com/Audiveris/audiveris/releases) section
on GitHub site.
Typical archive name is `Audiveris_Schemas_Doc-X.Y.Z.zip`, where X.Y.Z is the release version.

## Generation

This section describes how a developer can (re)generate this documentation locally,
using the `schemas` Gradle sub-project.

**Prequisite**:  
 Beside the usual software set (JDK(17), Git and Gradle),
you need to have `xsltproc` software installed.  
If not, you can still generate the `.xsd` schema definitions, but you cannot generate the related
`.html` files.

To install `xsltproc`:
* on Windows:  
  Visit Igor Zlatkovic's website: http://www.zlatkovic.com/libxml.en.html  
  From there, and or each of the 3 needed binaries, `libxslt`, `lixml2` and `iconv`:
    * Download and expand the archive
    * Include the `bin` directory in your `Path` environment variable

* on Linux:
    ```bash
    sudo apt-get update -y
    sudo apt-get install -y xsltproc
  ```

Now that `xsltproc` is up and running, here are the steps to perform on Audiveris project:

1. If not yet done, clone Audiveris project from GitHub to your local machine
```bash
    git clone https://github.com/Audiveris/audiveris.git audiveris
```
2. Move to the `audiveris` directory just created
```bash
    cd audiveris
```
3. From this directory, launch the `genSchemaDoc` Gradle task
```bash
    # Windows
    # Nota: clean task is not mandatory
    gradlew.bat clean genSchemaDoc
```
```bash
    # Linux (or Cygwin under Windows)
    # Nota: clean task is not mandatory
    ./gradlew clean genSchemaDoc
```

The results are now available in `schemas/build/output` folder:
```
$ ls -goR schemas/build/output
schemas/build/output:
total 2468
-rwxrwxr-x+ 1  192563 Jan 24 20:18 Book.html
-rwxrwxr-x+ 1   30763 Jan 24 20:18 Book.xsd
-rwxrwxr-x+ 1   36549 Jan 24 20:18 PlayList.html
-rwxrwxr-x+ 1    4489 Jan 24 20:18 PlayList.xsd
-rwxrwxr-x+ 1 2021257 Jan 24 20:18 Sheet.html
-rwxrwxr-x+ 1  201065 Jan 24 20:18 Sheet.xsd
drwxrwxr-x+ 1       0 Jan 24 20:18 doc-files
-rwxrwxr-x+ 1   17099 Jan 24 20:18 index.html

schemas/build/output/doc-files:
total 840
-rwxrwxr-x+ 1  34784 Jan 24 20:18 BeamAcrossMeasureBreak.png
-rwxrwxr-x+ 1   3267 Jan 24 20:18 BeamRest.png
-rwxrwxr-x+ 1  55832 Jan 24 20:18 Book-vs-Score.png
-rwxrwxr-x+ 1 107749 Jan 24 20:18 Book-vs-Sheet.png
-rwxrwxr-x+ 1 151813 Jan 24 20:18 EndingAcrossSystems.png
-rwxrwxr-x+ 1  14542 Jan 24 20:18 EndingWithNoBarlineOnStart.png
-rwxrwxr-x+ 1  77236 Jan 24 20:18 ImageTransforms.png
-rwxrwxr-x+ 1    906 Jan 24 20:18 KeySignatures.png
-rwxrwxr-x+ 1  51639 Jan 24 20:18 Measure-vs-MeasureStack.png
-rwxrwxr-x+ 1  30460 Jan 24 20:18 PlayList.png
-rwxrwxr-x+ 1  14916 Jan 24 20:18 Sentence_Hierarchy.png
-rwxrwxr-x+ 1 122952 Jan 24 20:18 SheetBinding.png
-rwxrwxr-x+ 1   4407 Jan 24 20:18 Slot.png
-rwxrwxr-x+ 1  27185 Jan 24 20:18 Slots.png
-rwxrwxr-x+ 1  91606 Jan 24 20:18 Step.png
-rwxrwxr-x+ 1  35781 Jan 24 20:18 schemas_doc_zip.png
```

And these results are already packed in a zip archive located in
`schemas/build/distributions` folder:
```
$ ls -goR schemas/build/distributions
schemas/build/distributions:
total 900
-rwxrwxr-x+ 1 919398 Jan 24 20:18 Audiveris_Schemas_Doc-5.3-alpha.zip
```
