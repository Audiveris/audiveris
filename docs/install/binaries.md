## Installing binaries

### Windows

#### Installation

Windows installers for Audiveris are available in
[https://github.com/Audiveris/audiveris/releases](https://github.com/Audiveris/audiveris/releases)
where you can find executables like:

* Audiveris_Setup-5.1.0-windows-x86.exe _(installer for 32-bit JRE)_
* Audiveris_Setup-5.1.0-windows-x86_64.exe _(installer for 64-bit JRE)_

BEWARE: Before jumping to the downloading and running of any of these installers,
please pay attention to the following paragraphs about needed Java runtime,
your environment (32 or 64 bits) and the optional support of additional languages.

**Java**:
These installers perform all necessary installation except for the Java Runtime Environment (JRE).
If you don't have a suitable JRE yet, you must install one, for instance from:   
[http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html)  
Make sure to install Java **version 8**.
Audiveris will currently not work with newer versions (9 and later).

**32-bit vs 64-bit**: Audiveris software is coded in Java but uses some external binaries
(Leptonica and Tesseract today, plus certainly ND4J tomorrow).
Mind the fact that, because of these binaries, **the JRE and installer architectures must match**.
Otherwise Audiveris will fail to run, with error messages saying for example that `jnilept` library
cannot be loaded.  
To check your existing java environment, you can use the command: `java -version` from a terminal
window.

**OCR languages**: The installers will automatically pre-install Tesseract support for 4 languages:
`eng`, `deu`, `ita` and `fra`.  
You can download additional languages from
[Tesseract 3.04 tessdata repository](https://github.com/tesseract-ocr/tessdata/tree/3.04.00).
Pay attention to the fact that Audiveris is not yet compatible with version 4.0 which is the most
recent version of Tesseract, **it requires Tesseract version 3.04**.  
Note that besides downloading additional language(s), you will have to select them at runtime.
This is detailed in Tesseract OCR article within the [Building from sources](sources.md) section.

**.omr extension**: During installation, you will be prompted to associate the `.omr` file extension
(which represents an Audiveris Book) to Audiveris software.

After installation, your Windows start menu should contain a submenu as follows.
By default, this submenu is named `Audiveris` for the 64-bit version and `Audiveris32` for the
32-bit version.

`Audiveris`

* `Audiveris` _(to launch Audiveris)_
* `Uninstall` _(to remove this program)_ (Nota: this menu item is not always available)
* `Website` _(to browse Audiveris web site)_

#### Uninstallation

To uninstall the program you can simply select `Uninstall` in Audiveris start menu.

The uninstaller will optionally keep the configuration files of the program.
So, if you re-install this program or a new version later, you will find the same settings that you
had used before uninstallation.

NOTA: You may not always see the `Uninstall` item under Audiveris in Windows start menu.
This is reportedly a new Windows behavior, which now recommends to open Windows Settings
(keyboard shortcut is `Windows + I`), then look in `Apps & features` section for the Audiveris item
and there press the `Uninstall` button.

### Linux
None.

### Mac-OS
None.
