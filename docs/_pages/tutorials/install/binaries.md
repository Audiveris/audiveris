---
layout: default
title: Installing from binaries
nav_order: 2
parent: Installation
---
# Installing from binaries
{: .no_toc }

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---
## Installers
{: .d-inline-block }
new in 5.5
{: .label .label-yellow }

Since version 5.5, Audiveris has provided installers for Windows, Linux and macOS.

These installers are based on the same structure:
1. The application comes with its own Java Runtime Environment (JRE).
Therefore, there is no need for the user to install a specific JRE.
2. The application comes with *no* pre-installed OCR languages,
but offers a [runtime dialog box](../../guides/main/languages.md) allowing the user to install
any desired OCR language(s).
The application is then responsible for picking up the right version of the language files
on the Tesseract site and for installing them in the user environment.

The installers can be downloaded from the assets section of a recent release
available on the Audiveris [releases page](https://github.com/Audiveris/audiveris/releases).

The name of each installer file is formatted as:
```
 Audiveris-<version>-<OS>-<Architecture>
```

For example, the assets of the [5.7.0 release](https://github.com/Audiveris/audiveris/releases/tag/5.7.0)
contain these files:

| File name | Size | Role | Option | Architecture |
| :---      | :--- | :--- | :--- | :--- |
| **Audiveris-5.7.0-macosx-arm64.dmg** | 66.5 MB | macOS installer|     | arm64 |
| **Audiveris-5.7.0-macosx-x86_64.dmg** | 68.2 MB | macOS installer |   | x86_64 |
| **Audiveris-5.7.0-ubuntu22.04-x86_64.deb** | 62.7 MB | Linux installer | 22.04 | x86_64 |
| **Audiveris-5.7.0-ubuntu24.04-x86_64.deb** | 62.5 MB | Linux installer | 24.04 | x86_64 |
| **Audiveris-5.7.0-windows-x86_64.msi** | 66.3 MB   | Windows installer |       | x86_64 |
| **Audiveris-5.7.0-windowsConsole-x86_64.msi** | 66.3 MB   | Windows installer | Console | x86_64 |
| **Audiveris_Handbook-5.7.0.pdf** | 12.9 MB   | PDF manual |  |  |
| **Audiveris_Schemas_Doc-5.7.0.pdf** | 1 MB   | Developer doc |  |  |

Remarks:
- for **macOS**, two different installers are provided:
  - One for the `arm64` architecture
  - One for the `x86_64` architecture
- for **Linux**, installers are provided for two Ubuntu versions:
  - Old `22.04`
  - New `24.04`
- for **Windows**, two installers are provided:
  - One without console, which displays just the graphic user interface. This is the recommended variant.
  - One with console, where a Terminal window is launched together with the graphic user interface.
  This variant can be useful to display error messages.
- The **PDF** version of the Audiveris **handbook** can be useful if you don't have permanent Internet access,
as its content is identical to the [online version](https://audiveris.github.io/audiveris/).
- The **Schemas_Doc** is meant for the developers. It documents the entities `Book`, `Sheet` and `PlayList` which compose the Audiveris `.omr` project files.

The downloaded installer file will be used to install the application in the target OS,
as detailed in the following sections.

{: .note :}
Once the application is launched, OCR languages can be downloaded directly from within the Audiveris application.
See the [OCR languages](../../guides/main/languages.md) section.

Audiveris used to provide a **Linux/Flatpak** package which can be installed directly from the
[Flathub](https://flathub.org/apps/org.audiveris.audiveris) site.
However, the future of this Flatpak package is not clear, due to a lack of skill and/or manpower.


## Windows

### Installation

Double-click the `Audiveris-<version>-windows-x86_64.msi` file in your `Downloads` folder (or wherever it’s saved).

| Action                                                                                                                            | Dialog                                              |
| :---                                                                                                                              | :---                                                |
| A double-click has opened the `Audiveris Setup` dialog box                                                                        | ![](../../assets/images/windows_install.png)        |
| The license agreement is displayed. If you agree, tick the checkbox and then click on `Next`                                      | ![](../../assets/images/windows_license.png)        |
| The default installation folder can be changed if desired                                                                         | ![](../../assets/images/windows_install_folder.png) |
| By default, the installer creates a shortcut on the desktop and one in the Windows start menu                                     | ![](../../assets/images/windows_shortcuts.png)      |
| The installation is now ready                                                                                                     | ![](../../assets/images/windows_install_do.png)     |
| You may get an alert from Windows User Account Control, since Audiveris is not from a "known" publisher. Click on `Yes` to allow. | ![](../../assets/images/windows_uac.png)            |
| The installation is finished                                                                                                      | ![](../../assets/images/windows_install_finish.png) |

### Running

The application can be launched in a variety of ways:

| Action                                                                                                                                        | Illustration                                                       |
| :---                                                                                                                                          | :---                                                               |
| Opening the Windows **start menu** and selecting the Audiveris icon                                                                           | ![](../../assets/images/windows_start_menu.png)                    |
| Double-clicking on the Audiveris icon located on Windows **desktop**                                                                          | ![](../../assets/images/windows_desktop_shortcut.png)              |
| Double-clicking on a **`.omr` file**, since the `.omr` file extension (which represents a  Book) is associated with the Audiveris application | ![](../../assets/images/windows_book_file.png)                     |
| In a terminal window, entering a **command**  refering to the program location                                                                | "C:\Program Files\Audiveris\Audiveris.exe" `<potential arguments>` |

The very first time Audiveris is launched some anti-virus software may get in the way:

| Event | Illustration |
| :---  | :---         |
| The anti-virus has detected a suspicious file | ![](../../assets/images/windows_suspicious.png) |
| The file looks safe, end of the check.  | ![](../../assets/images/windows_safe.png) |

### Notes

Some users have encountered the error "***Failed to launch JVM***" and it is reported [here](https://github.com/Audiveris/audiveris/issues/818).

This is reportedly related to something called "Assistive Technology".
It is documented in this article [https://www.papercut.com/kb/Main/AssistiveTechnologynotfound](https://www.papercut.com/kb/Main/AssistiveTechnologynotfound)
which provides a solution, copied *verbatim* below.

The best fix for this problem is to disable the Access Bridge technology in the Java configuration files. To do this, follow these steps:

1. Navigate to C:\Users\<username>
2. Open the file called .accessibility.properties using a standard text editor (Notepad is fine)
3. Find the following lines in the config:
⋅⋅⋅⋅* assistive_technologies=com.sun.java.accessibility.AccessBridge
⋅⋅⋅⋅* screen_magnifier_present=true
4. Add a hashmark (#) in front of theses lines
5. Save your changes to the file

### Uninstallation

To uninstall the program, open `Windows Settings`
(keyboard shortcut is `Windows + I`), and select the `Apps & features` section.

| Action                                                                                  | Illustration                                   |
| :---                                                                                    | :---                                           |
| In `Apps & features`, look for the Audiveris item and then press the `Uninstall` button | ![](../../assets/images/windows_uninstall.png) |

## Linux

### Installation

Remark: A double-click on the `.deb` installer file would result in the opening of the `App Center`
which would report on Audiveris as being potentially unsafe, etc.

Instead, in a terminal, use a command like:
```sh
$ sudo apt install /path/to/Audiveris-<version>-ubuntu<osversion>-x86_64.deb
```

This installs the application in the target folder:
> /opt/audiveris/

Which is organized as follows:
<pre>
/opt/audiveris
 ├── bin
 │   └── Audiveris
 ├── lib
 │   └── ...
 └── share
     └── ...
</pre>

### Running

In a terminal, use a command like:
```sh
$ /opt/audiveris/bin/Audiveris <potential arguments>
```

### Uninstallation

In a terminal, use one of these commands:
```sh
# To just uninstall
$ sudo apt remove audiveris

# To remove the application, including configuration files
$ sudo apt purge audiveris
```

## Linux/Flatpak installer

### Installation

The Audiveris installer for Linux uses the Flatpak utility and is hosted on the standard Flathub repository.

On the [Flathub site](https://flathub.org/), you can enter "audiveris" in the search field.
Or you can go directly to the [https://flathub.org/apps/org.audiveris.audiveris](https://flathub.org/apps/org.audiveris.audiveris) page.

For a manual install, you can use:
```sh
$ flatpak install flathub org.audiveris.audiveris
```
### Running

To launch the application, run the command:
```sh
$ flatpak run org.audiveris.audiveris
```

### Uninstallation

TODO


## macOS installer

This section explains how to install and run the Audiveris application on macOS using the provided DMG installer. Since the installer is not signed with an Apple Developer certificate, you'll need to adjust your macOS privacy settings to allow it to run.

### Installation

1. **Obtain the proper DMG File for your architecture**
    - Download the proper `.dmg` file, that is either  `Audiveris-<version>-macosx-arm64.dmg` or  `Audiveris-<version>-macosx-x86_64.dmg`, from the source (e.g., a contributor or repository release).

2. **Open the DMG**
    - Double-click the chosen `.dmg` file. This mounts the installer as a virtual disk on your desktop or in Finder.

3. **Install the Application**
    - Inside the mounted DMG, you’ll see `Audiveris.app`. Drag this file to your **Applications** folder to install it.
    - Once copied, you can eject the DMG by clicking the eject icon next to it in Finder or dragging it to the trash.

### Running

Since the app is not signed, macOS will block it by default. To allow it to run, follow these steps:

1. **Launch the App**
    - Go to your **Applications** folder and double-click `Audiveris.app`.
    - You’ll likely see a warning: *"“Audiveris” cannot be opened because it is from an unidentified developer."*

2. **Adjust Privacy Settings**
    - Open **System Preferences** (or **System Settings** on macOS Ventura and later):
        - Click the Apple menu (&#63743;) > **System Preferences** > **Security & Privacy** > **General** tab.
    - At the bottom, you’ll see a message: *“Audiveris” was blocked from use because it is not from an identified developer.*
    - Click **"Open Anyway"** to allow the app to run.

3. **Launch the App Again**
    - Double-click `Audiveris.app` again. You may see one final prompt asking for confirmation—click **"Open"**.
    - The app should now launch successfully.

### Notes

- **Unsigned App**: The lack of a signature is due to the installer not being created with an Apple Developer account. This is a one-time adjustment; once approved, macOS will remember your choice.
- **Troubleshooting**: If the app still won’t open, ensure you’ve completed the adjust privacy settings step. For persistent issues, contact the provider or check the Audiveris documentation.

### Uninstallation

TODO
