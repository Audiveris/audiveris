# Structure of the Audiveris project

This is a documentation on how the Audiveris project is now structured.
It has evolved from a aingle project to a tree of Gradle sub-projects for better modularity.

## Motivations and actions

- Divide the old monolith project into more modular sub-projects, each with a well-defined objective.
- Prohibit any duplication of information (such as the required Java version) to avoid inconsistencies.
- Use high-level standard tools (such as Gradle predefined tasks and plugins)
rather than having to develop and maintain *ad hoc* procedures.
- Delegate to GitHub workflow actions the building, packaging and releasing of the project artifacts.

## Folders

There is one folder per sub-project, each with its own `build.gradle` pilot file

| Sub-project     | Purpose       | Documentation |
| :---            | :---          | :---          | 
| **`app`**       | The application | [app/README.md](./app/README.md) |
| **`docs`**      | Building handbook in web and pdf formats | [docs/README.md](./docs/README.md) |
| **`packaging`** | Building all OS-specific installers | [packaging/README.md](./packaging/README.md) |
| **`flatpak`**   | Building Linux/Flatpak | [flatpak/README.md](./flatpak/README.md) |
| **`schemas`**   | Building the schemas-based documentation  | [schemas/README.md](./schemas/README.md) |

## Tree

Below is a simplified overall tree structure, centered on the main files.
The order is not strictly alphabetical, in order to group the entities by role.

<pre>
.
├── .github
│   └── workflows
│       ├── build-and-test.yml
│       ├── draft-release.yml
│       └── flatpak.yml
│
├── README.md                           // This file!
│
├── settings.gradle                     // Definition of the project structure
│
├── gradle
│   └── wrapper
├── gradle.properties                   // Definition of 'the' main variables
│
├── app
│   ├── build.gradle
│   └── src
│       └── ... // a whole tree of main and test .java files
│
├── docs
│   ├── build.gradle
│   ├── _config.yml
│   ├── _pages
│   └── pdf
│       ├── pdf-build.sh
│       └── pdf-nav-style.css
│
├── packaging
│   ├── build.gradle
│   └── dev
│       └── omr.properties
│
├── flatpak
│   ├── build.gradle
│   ├── dev
│   │   └── org.audiveris.audiveris.template.yml    // Template for flatpak-builder manifest
│   ├── res
│   │   ├── org.audiveris.audiveris.desktop
│   │   └── org.audiveris.audiveris.metainfo.xml
│   └── flathub                                     // Git submodule
│
└── schemas
    ├── build.gradle
    └── xs3p.xsl
</pre>

## Information sources

Below are the *unique* locations, relative to the project root, where the Audiveris key parameters are defined
and can be modified manually.

### `settings.gradle`

Gradle definition of project structure.

| Variable name | Variable role | Current value |
| :---          | :---          | :---          | 
| `rootProject.name` | Project name | Audiveris |


### gradle/wrapper/`gradle-wrapper.properties`

Definition of the desired Gradle version.

| Variable name | Variable role | Current value |
| :---          | :---          | :---          | 
| `distributionUrl` | URL for Gradle wrapper | https\://services.gradle.org/distributions/gradle-8.7-all.zip |
| `distributionSha256Sum`| SHA256 for Gradle wrapper | 194717442575a6f96e1c1befa2c30e9a4fc90f701d7aee33eb879b79e7ff05c0 |

### `gradle.properties`

Definition of 'the' main variables.

| Variable name | Variable role | Current value |
| :---          | :---          | :---          | 
| `theMinJavaVersion` | Minimum Java version  | 21 |
| `theTessdataTag`| Tesseract languages version | 4.1.0 |
| `theXsltTransformer` | Xslt processor. <br>Used for schemas-documentation | XsltProc |

### app/`build.gradle`

Definition of Audiveris precise version.

| Variable name | Variable role | Current value |
| :---          | :---          | :---          | 
| `project.version` | Audiveris precise version. <br> Format: Major.minor.suffix | 5.6.1 |

### docs/`_config.yml`

Definitions for the handbook documentation.

Note: It should be possible to determine these values ​​automatically from the variables listed above.
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
But unfortunately, as of this writing, these values must be entered manually...
=======
But currently, these values must be entered manually...
>>>>>>> be8671dde (Project documentation based on sub-projects README.md files)
=======
But unfortunately, as of this writing, these values must be entered manually...
>>>>>>> b25bd8a62 (Doc for the app sub-project)
=======
But currently, these values must be entered manually...
>>>>>>> be8671dde (Project documentation based on sub-projects README.md files)
=======
But unfortunately, as of this writing, these values must be entered manually...
>>>>>>> b25bd8a62 (Doc for the app sub-project)

| Variable name | Variable role | Current value |
| :---          | :---          | :---          | 
| `audiveris_functional_version` | Audiveris functional version for handbook.<br>Format: Major.minor | 5.6 |
| `master_java_version` | Minimum Java version for the master branch | 21 |
