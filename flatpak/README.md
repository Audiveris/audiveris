
# New structure for the Audiveris project

This is a preliminary documentation on how the Audiveris project is now structured.
It has evolved from a aingle project to a tree of subprojects for better modularity.

## Motivations and actions

- Divide the old monolith project into more modular sub-projects, each with a well-defined objective.
- Prohibit any duplication of information (such as the required Java version) to avoid inconsistencies.
- Use high-level standard tools (such as Gradle predefined tasks or the flatpakGradleGenerator plugin)
rather than having to develop and maintain *ad hoc* procedures.

## Subprojects

Below is a simplified tree view, focused on the main files.
<pre>
.
├── app
│   ├── build.gradle
│   ├── build
│   │   └── ... dependencies.json
│   ├── dev
│   │   ├── icons
│   │   │   └── ... // many .png icon images
│   │   ├── scripts
│   │   │   ├── custom-unixStartScript.txt
│   │   │   └── custom-windowsStartScript.txt
│   │   └── tessdata
│   │       └── eng.traineddata
│   └── src
│       └── ... // a whole tree of main and test .java files
├── dev // this folder could be removed
│   ├── flathub // the old Git submodule
│   │   └── ...
│   ├── flatpak // no longer used
│   │   ├── ...
│   │   ├── README.md // To be merged with flatpak/README.md
│   │   ├── languages.sh
│   │   └── create-flatpak-dependencies.py
│   └── icon-50.gif
├── flatpak
│   ├── README.md // this file!
│   ├── build.gradle
│   ├── dev
│   │   └── org.audiveris.audiveris.template.yml    // Template for flatpak-builder manifest
│   ├── res                                         // Resources to be copied to flathub
│   │   ├── org.audiveris.audiveris.desktop
│   │   └── org.audiveris.audiveris.metainfo.xml
│   └── flathub // new Git submodule. To be populated by initFlathub task as follows:
│       ├── dependencies.json                       // Dependencies manifest generated from app subproject
│       ├── org.audiveris.audiveris.desktop         // Copied from flatpak/res
│       ├── org.audiveris.audiveris.metainfo.xml    // Copied from flatpak/res
│       └── org.audiveris.audiveris.yml             // Global manifest for flatpak-builder, from dev template
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradle.properties
├── schemas
│   ├── build.gradle
│   └── ... // material for generating the schema-based documentation
├── settings.gradle
└── windows-installer
    ├── build.gradle
    └── ... // material for generating the Windows installer
</pre>

We can find:
- The sub-projects folders, each with its own ``build.gradle`` pilot file: 
    - **``app``**: the application (which was formerly located at root level)
    - **``schemas``**: the generation of schema-based documentation
    - **``windows-installer``**: the generation of Windows installer
    - **``flatpak``**: the generation of Linux installer
- The global Gradle components:
    - ``gradle-wrapper.properties``: definition of the desired gradle version
    - ``gradle.properties``: definition of the main variables
    - ``settings.properties``: definition of project structure
- The former **``dev``** folder is considered for removal, once its content has been dispatched to the relevant sub-projects.

## Definition sources

| Information | Source file path | Variable | Example Value|
| :---        | :--- | :---     | :---    |
| Gradle | gradle/wrapper/wrapper.properties | ``distributionUrl`` | https\://services.gradle.org/distributions/gradle-8.7-all.zip |
| Gradle Sha | gradle/wrapper/wrapper.properties | ``distributionSha256Sum`` | 194717442575a6f96e1c1befa2c30e9a4fc90f701d7aee33eb879b79e7ff05c0 |
| Java version | gradle.properties |  ``theMinJavaVersion`` | 21 |
| Tesseract langs version  | gradle.properties| ``theTessdataTag`` | 4.1.0 |
| Chosen Xslt processor | gradle.properties | ``theXsltTransformer`` | XsltProc |


## Building flatpak

The generation of Flatpak components is trigged by the following command,
entered from the Audiveris root folder:
``` sh
$ ./gradlew -q :flatpak:buildFlatpak
```

This means to launch the task ``buildFlatpak`` within the sub-project ``flatpak``.
Perhaps ``buildFlatpak`` should be better named ``build``, but for the time being this prevents any unwanted build.

This task does two things:
1. First, the ``initFlathub`` task is run to populate the flatpak/flathub folder
    1. The ``genDependencies`` task makes sure that Audiveris Java classes (``:app:classes``) have been compiled
    and then run the ``:app:flatpakGradleGenerator`` plugin to generate the app/build/``dependencies.json`` file
    which is then copied to the flatpak/flathub folder.
    2. The ``genManifest`` task builds the flatpak/flathub/``org.audiveris.audiveris.yml`` manifest by expanding
    the template flatpak/dev/``org.audiveris.audiveris.template.yml`` file with the variables
    read in gradle/wrapper/``wrapper.properties`` and ``gradle.properties``.      
2. Second, and only if the host OS name is "linux", it launches ``flatpak-builder`` as follows:
``` sh
    commandLine('flatpak-builder', 
        '--verbose',                            // option to set verbosity level
        '--force-clean',                        // option to empty the output directory
        'build',                                // relative path to the output directory to write
        'org.audiveris.audiveris.yml')          // relative path to the manifest file to read
```

### The application tag

IMPORTANT: the application tag, for example `5.4`, used by the `genManifest` task,
is essential for the flatpak builder to retrieve the precise commit of the Audiveris project.

This tag is today hard-coded in flatpak/``build.gradle`` file, within the `genManifest` task.  
Fortunately, we can always manually modify (delete + add) a tag on GitHub.

This means that we don't have to modify the hard-coded value, because by *moving* the tag
we can simply make it point to the most recent commit.

### Partial build

If we want to just populate the `flatpak/flathub` folder,
instead of calling the `:flatpak:buildFlatpak` task (which can run only on Linux),
we can directly call the `:flatpak:initFlathub` sub-task which can run on any OS,
to retrieve the dependencies and generate the manifest:.

```sh
$ ./gradlew -q :flatpak:initFlathub

hostOS: windows-x86_64
targetOS: windows-x86_64
theMinJavaVersion: 21
theTessdataTag: 4.1.0
theXsltTransformer: XsltProc
programBuild: ca3933a3e
app. Generating dependencies
app. Dependencies generated
Copying dependencies.json
Generating Flatpak manifest
Copying resources to flathub
```

The dependencies are retrieved according to a targetOS which is by default set to the hostOS.  
But we can define a specific targetOS via the Gradle property `targetOS`.

This allows to generate the flathub Linux content, from a Windows host for example:

```sh
# First, remove the previous dependencies.json file in the `app` sub-project
rm  app/build/dependencies.json

# Then generate the new material
$ ./gradlew -q -PtargetOS=linux-x86_64 :flatpak:initFlathub

hostOS: windows-x86_64
property targetOS: linux-x86_64
targetOS: linux-x86_64
theMinJavaVersion: 21
theTessdataTag: 4.1.0
theXsltTransformer: XsltProc
programBuild: ca3933a3e
app. Generating dependencies
app. Dependencies generated
Copying dependencies.json
Generating Flatpak manifest
Copying resources to flathub
```






