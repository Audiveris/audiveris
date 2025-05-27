# App

The `app` sub-project handles the Audiveris application.

The Gradle tasks allow to:
- build the application
- run or debug the application
- generate its JavaDoc

## Most relevant files

<pre>
.
├── settings.gradle                             // Gradle project structure
│
├── gradle.properties                           // Definition of all 'the' variables
│
├── gradle                                      // Gradle wrapper
│   └── wrapper                                 // ...
│       ├── gradle-wrapper.jar                  // ...
│       └── gradle-wrapper.properties           // ...
├── gradlew                                     // ...
├── gradlew.bat                                 // ...
│
└── app
    ├── README.md                               // This file!
    ├── build.gradle                            // The Gradle sub-project building file
    │   
    ├── config-examples                         // Examples for user customization
    │   ├── plugins.xml                         // Plugins examples
    │   └── user-actions.xml                    // User actions examples
    │   
    ├── dev                                     // Items for development
    │   ├── icons                               // Icons for user interface
    │   │   └── crystal
    │   │       ├── 22x22                       // Small icons
    │   │       └── 32x32                       // Large icons
    │   ├── scripts
    │   │   ├── custom-unixStartScript.txt      // Template for Unix script
    │   │   └── custom-windowsStartScript.txt   // Template for Windows script
    │   └── tessdata
    │       └── eng.traineddata                 // [No longer used]
    │   
    ├── res                                     // Runtime resources
    │   ├── Bravura.otf                         // Music font
    │   ├── FinaleJazz.otf                      // ...
    │   ├── FinaleJazzText.otf                  // ...
    │   ├── JazzPerc.ttf                        // ...
    │   ├── Leland.otf                          // ...
    │   ├── MusicalSymbols.ttf                  // ...
    │   ├── Primus.ttf                          // ...
    │   │
    │   ├── all-icons-256.ico                   // Audiveris icon
    │   ├── icon-256.ico                        // ...
    │   ├── icon-256.png                        // ...
    │   ├── icon-50.gif                         // ...
    │   ├── icon-50.png                         // ...
    │   ├── icon-64.jpg                         // ...
    │   ├── icon-64.png                         // ...
    │   ├── splash.bmp                          // Splash screen
    │   ├── splash.png                          // ...
    │   │
    │   ├── basic-classifier.zip                // Trained glyph classifier
    │   │
    │   ├── ISO639-3.xml                        // 3-letter codes for OCR languages
    │   ├── alias-patterns.xml                  // Patterns for renaming input file names
    │   ├── drum-set.xml                        // Default mapping for drum instruments
    │   ├── logback-elements.xml                // Definition of logging appenders
    │   ├── logback.xml                         // Default loggers configuration
    │   └── system-actions.xml                  // Default User Interface actions
    │   
    ├── src
    │   ├── main
    │   │   └── java                            // All main Java sources
    │   └── test
    │       └── java                            // All test Java sources
    │   
    └── build                                   // Outputs

</pre>

## Variable definitions

Please refer to the [Information Sources at project level](../project-structure.md#information-sources) which apply to all sub-projects.

### `app/build.gradle`

These are the variables for `app/build.gradle` file.

Only the first one (`project.version`) is meant to be manually modified here:

| Variable name | Variable role | Example value |
| :--- | :--- | :--- | 
| `project.version` | Audiveris version | 5.6.1 |

The other variables are automatically retrieved from other sources.
They are listed here for information only:

| Variable name | Variable role | Example value |
| :--- | :--- | :--- | 
| `project.ext.hostOSName` | Host OS name | windows | 
| `project.ext.hostOSArch` | Host OS architecture | x86_64 | 
| `project.ext.hostOS` | Host OS full ID | windows-x86_64 | 
| `project.ext.targetOS` | Target OS full ID | `project.ext.hostOS` by default | 
| `project.ext.theMinJavaVersion` | Java version | 21 | 
| `project.ext.theTessdataTag` | Tesseract languages tag | 4.1.0 | 
| `project.ext.programName` | Program name | Audiveris | 
| `project.ext.programId` | Program id | audiveris | 
| `project.ext.programVersion` | Program version | `project.version` | 
| `project.ext.companyName` | Company name | Audiveris Ltd. | 
| `project.ext.companyId` | Company ID | AudiverisLtd | 
| `project.ext.mainClass` | Name of main class | `project.ext.programName` | 
| `project.ext.commit` | Full hash of latest commit | 9315e943438c603a0b6520526d01a1d7fe7df2fa | 
| `project.ext.programBuild` | Short hash of latest commit | 9315e9434 | 

## The `config-examples` folder

These files are **NOT** read at runtime.
They are just examples that could be customized and placed in the user `config` folder.

## The `dev` folder

These are development items.

### `dev/icons`

This ensemble of Crystal icons is loaded as a runtime resource, next to the Java classes,
to represent the various user interface actions.

Such as:
![](./dev/icons/crystal/32x32/actions/1leftarrow.png)
![](./dev/icons/crystal/32x32/actions/1rightarrow.png)
![](./dev/icons/crystal/32x32/mimetypes/pdf.png)
![](./dev/icons/crystal/32x32/mimetypes/gettext.png)
### `dev/scripts`

These files are script templates, that are used to check at launch time if the Java runtime environment (JRE) fits the application requirements..

Note: This is useful only when the user launches the application via its `Audiveris` or `Audiveris.bat` command files.
It is no longer useful when using the new Audiveris installers
since those carry the needed JRE with them.

### `dev/tessdata`

It was used to provide the default language data file for Tesseract.

It is no longer used, since these data files can now be downloaded at runtime.
But it is kept for a potential future use, in some Continuous Integration actions.

## The `res` folder

These are resources that must be available at runtime.

### Music font files

All files with a `.otf` or `.ttf` extension, such as `Bravura.otf`,
define a specific music font meant for display, for note head template matching,
and for symbol recognition.

### Icon files

These icons represent the Audiveris icon in different sizes and formats.

![](./res/icon-64.png)

### Splash files

They represent the Audiveris logo in different formats.

![](./res/splash.png)

### `basic-classifier.zip`

This is the result of the training of Audiveris neural network on fixed size symbols.

It is used at runtime to infer the most probable symbols for a given glyph.

It can be overridden by another `basic-classifier.zip` file, located in the user `config/train` folder.

### `.xml` files

These are definition files, managed by the developer, and read at runtime.

In most cases, the end user can override them, by putting a corresponding
file with the same name in the user `config` folder.

| File name | Role | Overridable by |
| :--- | :--- | :--- | 
| `ISO639-3.xml`         | Table of 3-letter codes for OCR languages | - |
| `alias-patterns.xml`   | Patterns for renaming input file names | config/`alias-patterns.xml` |
| `drum-set.xml`         | Mapping for drum instruments | config/`drum-set.xml` |
| `logback-elements.xml` | Logging appenders | config/`logback-elements.xml` |
| `logback.xml`          | Loggers configuration | config/`logback.xml` |
| `system-actions.xml`   | User Interface actions | config/`user-actions.xml` |
