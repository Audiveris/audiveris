# Packaging

The `packaging` sub-project is in charge of building the OS-dependent Audiveris installer
for the OS the project is being run upon.

The GitHub workflow named `draft-release.yml` drives the building of installers for various OSes,
collects all the installers, adds a PDF version of Audiveris handbook,
and creates a draft release with all these assets.

## Most relevant files

<pre>
.
├── .github
│   └── workflows
│       └── draft-release.yml   // Workflow to draft a release
│
├── app
│   └── build.gradle            // Building Audiveris application
│
├── docs
│   └── pdf
│       ├── pdf-build.sh        // Building the PDF version of handbook
│       └── pdf-nav-style.css   // Styling for the PDF table of contents
│
└── packaging
    ├── assets                  // Images used by README.md
    ├── README.md               // This file
    ├── build.gradle            // Building an OS-dependent installer
    └── dev
        └── omr.properties      // Mapping the .omr extension to Audiveris application
</pre>

## Building an installer

Each created installer includes a (shrunk) Java Runtime Environment (JRE), the Audiveris application and all the
dependent libraries. It contains no pre-installed language file for the Tesseract library.

This approach is based on two Java tools:
- [`jlink`](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jlink.html)
which assembles a set of JRE  modules and their dependencies into a custom runtime image,
- [`jpackage`](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html)
which packages a Java application into a platform-specific self-contained executable.

The implementation is driven in the `packaging/build.gradle` file by the Gradle task named `jpackage`
provided by a dedicated [jpackage-gradle-plugin](https://github.com/petr-panteleyev/jpackage-gradle-plugin)
with its specific `window`, `mac` and `linux` blocks, as follows:
1. `customJre`: task to package only the needed JRE modules
(resulting in a reduction of about 75% of the installer file size)
2. `collectJars`: task to collect the `audiveris.jar` just built, with all the jars it depends on
3. `jpackage` with os-specific actions:
    - for `Windows`: The installer type is `MSI` by default.  
    We could ask for a Windows console, if so needed. This option is OFF by default.
    - for `Linux`: The installer type is `DEB` by default.  
    The installer name conveys the Ubuntu version, that is either `20.04` or `22.04` as of this writing.
    - for `macOS`: The installer type is `DMG` by default.  
    The `macConvertIcons` task uses `imagemagick` and `iconutil` utilities
    to generate a set of Audiveris icons with differents sizes.
4. Finally, rename the resulting installer file according to:
    - the Audiveris version,
    - the OS name,
    - the OS version if needed,
    - the machine architecture,
    - the installer type.

## Packaging and publishing

### Drafting a release

The Github workflow file `draft-release.yml` launches the job `build-installer`
(which runs the `jpackage` Gradle task described above) in parallel on several machines.
As of this writing, these machines are:

| Name           | Version | Architecture |
| :---           | :---    | :--- | 
| ubuntu-latest  | 24.04   | x86_64 |
| ubuntu-22.04   | 22.04   | x86_64 | 
| windows-latest | 10      | x86_64 |
| macos-latest   | 14      | arm64  | 
| macos-13       | 13      | x86_64 |

It also launches in parallel the job `build-handbook-pdf` to generate the PDF version of the handbook.

Then, the job `global-upload` collects all the produced artifacts to create a draft release.

![](./assets/draft-release.png)

### Editing the release

The draft is now present in the [Releases section](https://github.com/Audiveris/audiveris/releases)
of the Audiveris repository, but since it is a draft, it is visible only by the repository authors.

![](./assets/edit-release.png)

We have to enter the editing mode to manually adjust this draft:
1. Set or choose a tag
2. Rename the release
3. Write comments about the main features of the release
4. Perhaps set it as a pre-release
5. Or set it as the latest release
6. Finally press the `Update release` button

At this point in time, the release is now fully visible and even referenced as the latest release.

Something like that:

![](./assets/latest-release.png)

## Publishing on Windows Community repository

Once the release draft has been edited and published as the latest release on GitHub,
a new Latest Release is defined in Audiveris repository.

This triggers the action defined
in file [.github/workflows/publish-winget.yml](../.github/workflows/publish-winget.yml)
and named "Publish on Windows repository".

The action uses the secret named `WINGET_PAT`.

The action does in sequence:
1. Download the latest version of `wingetcreate.exe`
2. Retrieve the descriptor of Audiveris latest release
3. Retrieve the download URL of the (first) Windows installer among the release assets
4. Use `wingetcreate.exe` to update and submit the winget manifest for Audiveris

A pull request like [this one](https://github.com/microsoft/winget-pkgs/pull/347694) is automatically posted on the `github/microsoft/winget-pkgs` repository,
waiting for someone to review and approve it.



## Publishing on Scoop Extras bucket

### Automated update
Once the release draft has been edited and published as the latest release on GitHub,
a new Latest Release is defined in Audiveris repository.

"Automagically", the related Scoop manifest will shortly get updated on Scoop Extras bucket,
thanks to the "***autoupdate***" feature within this manifest.  
Scoop documentation is not very clear on this, but my understanding is that, at some pace, 
the installers referenced in Scoop manifests are checked and updated if needed.

Therefore, there is nothing to do manually.  
The rest of this section is useful only to write the *very first* manifest version.

### Manual first version

The file `.github/workflows/build-scoop.yml` defines a GitHub action to be run manually.
The purpose of this action is to build a Scoop manifest that references the Audiveris latest release.
The sole artifact of this action can be downloaded at the provided URL:

![](./assets/scoop-artifact-download.png)

The downloaded archive `scoop-manifest.zip` contains the file `audiveris.json` which is the Scoop manifest.

Here is the example of the `audiveris.json` for the 5.10.0 release:
```json
{
    "version": "5.10.0",
    "description": "Optical Music Recognition",
    "homepage": "https://github.com/Audiveris/audiveris",
    "license": "AGPL-3.0-or-later",
    "architecture": {
        "64bit": {
            "url": "https://github.com/Audiveris/audiveris/releases/download/5.10.0/Audiveris-5.10.0-windowsConsole-x86_64.msi",
            "hash": "sha256:74857d40a41dd5ce5b2f419dc1c6a844f58e542a2b2454e658956ad1aca33953"
        }
    },
    "extract_dir": "Audiveris",
    "bin": "Audiveris.exe",
    "shortcuts": [
        [
            "Audiveris.exe",
            "Audiveris"
        ]
    ],
    "checkver": {
        "github": "https://github.com/Audiveris/audiveris"
    },
    "autoupdate": {
        "architecture": {
            "64bit": {
                "url": "https://github.com/Audiveris/audiveris/releases/download/$version/Audiveris-$version-windowsConsole-x86_64.msi"
            }
        }
    }
}
```

Note that this manifest references the Windows installer with the "Console" option
for a good scoop integration.

According to the [Scoop contributing documentation](https://github.com/ScoopInstaller/.github/blob/main/.github/CONTRIBUTING.md), we can now:
1. Fork the Scoop **Extras** repository at [https://github.com/ScoopInstaller/Extras](https://github.com/ScoopInstaller/Extras)
2. Add the `audiveris.json` file into the `bucket` folder
3. Submit a pull request, with a specific title:
```
If it's a new manifest, use <app name>: Add version <version>.  
If it's an update to an existing manifest, use <app name>@<version>: <small description>.
```

## Personal Access Tokens

To submit PR via workflow actions, we need a personal access token.
- `publish-winget.yml` uses `secrets.WINGET_PAT`
- `flatpak.yml` uses `secrets.FLATPAK_PAT` (or perhaps `secrets.FLATHUB_TOKEN` ?)

### Create

On github site:
- Click on user icon, top right, `Open user navigation menu`
- Select `Settings`
- Select `Developer settings` at the very bottom
- Personal access tokens
- Tokens (classic)
- Generate new token (classic), for general use
- Note field: enter a name for this token, e.g. FLATHUB_TOKEN
- Select scopes (`repo` and `workflow`)
- Click on button `Generate token`
- Copy the token value to the clipboard

### Save

To save the token value on Audiveris site, [https://github.com/Audiveris/audiveris](https://github.com/Audiveris/audiveris):
- Repository Settings
- Secrets and variables
- Actions
- Repository secrets
- Click on button `New repository secret`
- Name: typically FLATHUB_TOKEN 
- Secret: Insert the token value from the clipboard
- Press button `Add secret`


