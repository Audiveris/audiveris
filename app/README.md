# App

The `app` sub-project handles the Audiveris application.

The Gradle tasks allow to:
- build the application
- run or debug the application
- generate its JavaDoc

## Relevant files

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
    ├── config                                  // Examples for user customization
    │   ├── plugins.xml                         // Plugins example
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

## Unique definitions

### `settings.gradle`

Modifiable definitions:
| Variable name | Variable role | Current value |
| :--- | :--- | :--- | 
| `rootProject.name` | Project name | Audiveris |


### `app/build.gradle`

Modifiable definitions:
| Variable name | Variable role | Current value |
| :--- | :--- | :--- | 

Modifiable definitions:
| Variable name | Variable role | Current value |
| :--- | :--- | :--- | 

| Variable name | Variable role | Example value |
| :--- | :--- | :--- | 
| `project.version` | Audiveris version | 5.6.1 |
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
