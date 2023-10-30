---
layout: default
title: Installing binaries
nav_order: 3
parent: Installation
---
# Installing binaries
{: .no_toc }

Table of contents
{: .no_toc .text-delta }
1. TOC
{:toc}
---

## Windows

### Installation

The Audiveris installer for Windows is hosted on
[https://github.com/Audiveris/audiveris/releases](https://github.com/Audiveris/audiveris/releases).

Installer versions are named "Audiveris_Setup-X.Y.Z-windows-x86_64.exe" where X.Y.Z values
refer to the related release.

During installation, you will be prompted to associate the `.omr` file extension
(which represents an Audiveris Book) with Audiveris software.

After installation, the Windows start menu should contain a sub-menu named `Audiveris`:

![](../assets/images/windows_start_menu.png)

#### Additional actions

{: .warning }
Do not forget two additional actions:  
    - [Java environment](./java.md)  
    - [OCR languages](./languages.md)

#### 64-bit architecture

The installer is built for a 64-bit architecture.

If a 32-bit version is really needed for whatever reason (perhaps because your equipment is too old),
you have to fall back using old Audiveris 5.1 installers which were available for both 32 and 64-bit
architectures.

### Uninstallation

To uninstall the program, you can simply select `Uninstall` in the Audiveris start menu.

The uninstaller will optionally keep the configuration files of the program.
So, if you re-install this program or a new version later, you will find the same settings
that were used before uninstallation.

{: .note }
You may not always see the `Uninstall` item under Audiveris in the Windows start menu.  
This is reportedly a new Windows behavior, which now recommends to open `Windows Settings`
(keyboard shortcut is `Windows + I`), then look in `Apps & features` section for the Audiveris item
and there press the `Uninstall` button.

## Linux

### Flatpak
Audiveris can be easily installed on a large variety of Linux distributions with the flatpak package from Flathub: [https://flathub.org/apps/org.audiveris.audiveris](https://flathub.org/apps/org.audiveris.audiveris). This package only supports x86_64 architecture for now.

## MacOS
No installer yet.
