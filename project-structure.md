# New structure for the Audiveris project

This is a documentation on how the Audiveris project is now structured.
It has evolved from a aingle project to a tree of subprojects for better modularity.

## Motivations and actions

- Divide the old monolith project into more modular sub-projects, each with a well-defined objective.
- Prohibit any duplication of information (such as the required Java version) to avoid inconsistencies.
- Use high-level standard tools (such as Gradle predefined tasks or the flatpakGradleGenerator plugin)
rather than having to develop and maintain *ad hoc* procedures.

## Tree

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
├── docs
│   ├── _config.yml
│   ├── _includes
│   │   └── toc_heading_custom.html
│   ├── _pages
│   │   ├── assets
│   │   │   └── images
│   │   ├── explanation
│   │   ├── guides
│   │   ├── handbook.md
│   │   ├── reference
│   │   └── tutorials
│   ├── _sass
│   │   └── custom
│   │       └── custom.scss
│   ├── _site
│   ├── favicon.ico
│   ├── index.md
│   └── pdf
│       ├── pdf-build.sh
│       └── pdf-nav-style.css
├── flatpak
│   ├── README.md
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

## Folders

- One folder per sub-project, each with its own `build.gradle` pilot file: 
    - **`app`**: the application (which was formerly located at root level)
    - **`schemas`**: the generation of schema-based documentation
    - **`windows-installer`**: the generation of Windows installer
    - **`flatpak`**: the generation of Linux installer
- The **`docs`** folder dedicated to the generation of documentations:
    - `_pages`: the handbook content
    - `pdf` : the generation of handbook PDF version

## Properties

The global Gradle components are defined in these files:
- `gradle-wrapper.properties`: definition of the desired gradle version
- `gradle.properties`: definition of the main variables
- `settings.properties`: definition of project structure

