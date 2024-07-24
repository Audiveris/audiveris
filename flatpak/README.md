
# New structure for Audiveris project

This is a preliminary documentation on how the Audiveris project is meant to be structured.
It has evolved from a aingle project to a tree of subprojects for better modularity.

I still have difficulties with the dev/flathub git submodule.
The presented structure may not be the final one, this README is meant to be a description of the
current and not perfect structure.

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
│   │       ├── deu.traineddata
│   │       ├── eng.traineddata
│   │       ├── fra.traineddata
│   │       └── ita.traineddata
│   └── src
│       └── ... // a whole tree of main and test .java files
├── dev // this folder could be removed?
│   ├── flathub // the old Git submodule
│   │   └── ...
│   ├── flatpak // no longer used
│   │   ├── ...
│   │   ├── README.md
│   │   ├── languages.sh
│   │   └── create-flatpak-dependencies.py
│   └── icon-50.gif
├── flatpak
│   ├── README.md // this file!
│   ├── build.gradle
│   ├── dev
│   │   └── org.audiveris.audiveris.template.yml    // Template for flatpak-builder manifest
│   ├── res                                         // Resources to be copied to flathub
│   │   ├── add-tessdata-prefix.sed
│   │   ├── org.audiveris.audiveris.desktop
│   │   └── org.audiveris.audiveris.metainfo.xml
│   ├── README.md // this file!
│   └── flathub // new Git submodule. To be populated as follows:
│       ├── dependencies.json               // Dependencies manifest generated from app subproject
│       ├── dependencies                    // Offline dependencies
│       │   ├── ...
│       │   └── ... // Needed libraries
│       │
│       ├── lang_sources.yml                // Languages manifest generated from app/dev/tessdata
│       ├── deu.traineddata
│       ├── eng.traineddata
│       ├── fra.traineddata
│       ├── ita.traineddata
│       │
│       ├── add-tessdata-prefix.sed
│       ├── org.audiveris.audiveris.desktop
│       ├── org.audiveris.audiveris.metainfo.xml
│       │
│       ├── org.audiveris.audiveris.yml     // Global manifest for flatpak-builder
│       │
│       └── ... // Perhaps other build artifacts
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

In this "work in progress", the generation of Flatpak components is trigged by the following command,
entered from the Audiveris root folder:
``` sh
./gradlew -q :flatpak:buildFlatpak
```

This means to launch the task ``buildFlatpak`` within the sub-project ``flatpak``.
Perhaps ``buildFlatpak`` should be better named ``build``, but for the time being this prevents any unwanted build.

This task does two things:
1. First, the ``initFlathub`` task is run to populate the flatpak/flathub folder
    1. The ``genLanguages`` task builds the flatpak/flathub/``lang_sources.yml`` file by browsing the app/dev/tessdata/``*.traineddata`` files.
    2. The ``genManifest`` task builds the flatpak/flathub/``org.audiveris.audiveris.yml`` manifest by expanding
    the template flatpak/dev/``org.audiveris.audiveris.template.yml`` file with the variables
    read in gradle/wrapper/``wrapper.properties`` and ``gradle.properties``.  
    TODO: the application tag, for example ``5.4-alpha-2``, is today hard-coded in flatpak/``build.gradle`` file,
    but might be better defined in ``gradle.properties``. To be investigated.
    3. The ``genDependencies`` task makes sure that Audiveris Java classes (``:app:classes``) have been compiled
    and then run the ``:app:flatpakGradleGenerator`` plugin to generate the app/build/``dependencies.json`` file
    which is then copied to the flatpak/flathub folder.
4. Second, it launches ``flatpak-builder`` as follows:
``` sh
    commandLine('flatpak-builder', 
        '--verbose',                            // option to set verbosity level
        '--force-clean',                        // option to empty the output directory
        'build',                                // relative path to the output directory to write
        'org.audiveris.audiveris.yml')          // relative path to the manifest file to read
```






