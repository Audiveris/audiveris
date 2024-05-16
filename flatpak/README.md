
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

Below is a simplified tree view, focused on the main source files.
<pre>
.
├── app
│   ├── build.gradle
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
│   ├── flathub // a Git submodule
│   │   └── ...
│   ├── flatpak // no longer used
│   │   ├── ...
│   │   ├── README.md
│   │   ├── languages.sh
│   │   └── create-flatpak-dependencies.py
│   └── icon-50.gif
├── flatpak
│   ├── build.gradle
│   ├── dev
│   │   ├── add-tessdata-prefix.sed
│   │   └── org.audiveris.audiveris.template.yml
│   ├── org.audiveris.audiveris.desktop
│   ├── org.audiveris.audiveris.metainfo.xml
│   └── README.md // this file!
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

In this "work in progress", the generation of Flatpak components is trigged by the following command, entered from Audiveris root folder:
``` sh
./gradlew -q :app:buildFlatpak
```

This means to launch the task ``buildFlatpak`` within the sub-project ``flatpak``.
Perhaps ``buildFlatpak`` should be better named ``build``, but for the time being this prevents any unwanted build.

This task, if needed, launches the following tasks:
1. The ``genLanguages`` task builds the flatpak/build/``lang_sources.yml`` file by browsing the app/dev/tessdata/``*.traineddata`` files.
2. The ``genManifest`` task builds the flatpak/build/``org.audiveris.audiveris.yml`` manifest by expanding
the template flatpak/dev/``org.audiveris.audiveris.template.yml`` file with the variables
read in gradle/wrapper/``wrapper.properties`` and ``gradle.properties``.  
Nota: the application tag, for example ``5.4-alpha-1``, is today hard-coded in flatpak/``build.gradle`` file,
but might be better defined in ``gradle.properties``. To be investigated.
3. The ``genDependencies`` task makes sure that Audiveris Java classes (``:app:classes``) have been compiled
and then run the ``:app:flatpakGradleGenerator`` plugin to generate the app/build/``dependencies.json`` file.
4. Finally, the ``buildFlatpak`` Gradle task launches ``flatpak-builder`` as follows:
``` sh
    commandLine('flatpak-builder', 
        '--verbose', 
        '--state-dir=build/.flatpak-builder',   // option to define the state-storing directory
        '--force-clean',                        // option to empty the output directory
        'build/output',                         // relative path to the output directory to write
        'build/org.audiveris.audiveris.yml')    // relative path to the manifest file to read
```






