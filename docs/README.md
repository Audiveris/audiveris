# Docs

The `docs` sub-project is dedicated to the Audiveris handbook,
which is the essential documentation meant for the end user.

The same content is delivered in two formats:
- a web format,
- a PDF format.

This README file is intended for the developer.
It describes how to edit the Audiveris handbook and how to generate the PDF format.

## Formats

### The web format

Technically, the handbook is written as a hierarchy of Markdown files.
These files are rendered as HTML pages and delivered on-line from the GitHub web site.

The latest web format is available [here][web-latest].

Via any web browser, the user can:
- Access the documentation on-line,
- Navigate through its hierarchy (via the side navigation pane),
- Search the documentation for matching words (via the "Search Audiveris Pages" field),
- Read desired web pages interactively.

### The PDF format

As opposed to the web format, the PDF format is made of a single downloadable PDF file.

The PDF file is organized as follows:
1. A hierarchical table of content, with hyperlinks to the related chapters,
2. All the pages, one chapter after the other,
3. Possibly an index of major entities (this feature is not yet implemented).

The latest PDF format can be downloaded from [here][pdf-latest].

With the PDF format, the user can:
- Read the documentation off-line,
- Navigate through its hierarchy (via the table of contents),
- Print it on paper,
- Convert it to spoken audio (via some "text-to-speech" application).

## Most relevant files

<pre>
.
├── .github
│   └── workflows
│       └── draft-release.yml
│
└── docs
    ├── _includes
    │   └── toc_heading_custom.html     // Default label and style for a table of contents block
    ├── _pages
    │   ├── assets
    │   │   └── images                  // All image files 
    │   ├── handbook.md                 // Handbook home page
    │   ├── tutorials                   // Part 1: To get started
    │   ├── guides                      // Part 2: For precise tasks
    │   ├── reference                   // Part 3: Comprehensive technical descriptions
    │   └── explanation                 // Part 4: How it works
    ├── _sass
    │   └── custom
    │       └── custom.scss             // Customized styles
    ├── _site                           // The generated web site
    │
    ├── build
    │   └── pdf
    │       └── Audiveris_Handbook.pdf  // The locally generated PDF format
    ├── pdf                         
    │   ├── pdf-build.sh                // Commands to build the PDF format
    │   └── pdf-nav-style.css           // Styles for the PDF table of contents
    │
    ├── _config.yml                     // Jekyll configuration settings
    ├── 404.html                        // Custom page for broken link
    ├── build.gradle                    // Gradle task calling the PDF build commands
    ├── favicon.ico                     // Favorite icon for the browser
    ├── Gemfile                         // Ruby dependencies
    │
    ├── index.md                        // Introduction to Audiveris documentations
    └── README.md                       // This file!
</pre>

## Editing the handbook content

The handbook web site uses the [Just the Docs](https://just-the-docs.com/) theme for [Jekyll](https://jekyllrb.com/).

As much as possible, the various `.md` files are organized among the 4 parts:
tutorials, how-to guides, reference and explanation.

The file `_config.yml` gathers the configuration settings.
The developer should pay attention to two site variables defined in this file:
- `audiveris_functional_version`: should reflect the functional version of Audiveris (e.g. 5.6) to which the manual applies.
- `master_java_version`: should reflect the Java version (e.g. 21) needed for building the `master` branch.

"Visual Studio Code" is a convenient editor to work interactively on this documentation.

## Deployment of the web format

### Locally

Assuming [Ruby](https://www.ruby-lang.org/en/downloads/) and the [Jekyll gem](https://jekyllrb.com/) are installed,
the developer can have the local web site updated any time a source file changes.

```sh
# Into the docs folder
$> cd docs

# Launch Jekyll 
$> bundle exec jekyll serve --incremental
```

From that point on, the local site can be accessed via a browser pointing to [http://localhost:4000/audiveris/](http://localhost:4000/audiveris/).

The purpose of the local generator is to have a good preview of the web rendering,
based on the current files being edited, without resorting to GitHub site.

### On GitHub

Since Audiveris is a public repository on GitHub, its documentation is readily available
on [https://audiveris.github.io/audiveris/](https://audiveris.github.io/audiveris/).

Via the standard `pages-build-deployment` GitHub workflow,
this remote site is automatically updated any time some new content is pushed
to the `docs` folder of the `master` branch.

## Generating the PDF format

The PDF generation is implemented by the dedicated `pdf-build.sh` bash file, 
derived from [hamoid / justTheDocsToPDF.bash](https://gist.github.com/hamoid) work.

It uses standard Linux utilities: curl, grep, head, perl, sed, tail, echo, cat.

It also uses the [prince](https://www.princexml.com/) software,
assumed to be installed, which can be freely used for non-commercial use.

| File | Purpose |
| :--- | :--- |
| `docs/pdf/pdf-build.sh`       | Command file |
| `docs/pdf/pdf-nav-style.css`  | Style file used by the table of contents |
| `docs/build/`                 | Folder where the results are generated |

### Locally

The file `docs/build.gradle` can drive the PDF generation via two possible tasks:
- `handbookPdf` task which gets the web content from Audiveris project on GitHub site  (https://audiveris.github.io)
- `handbookPdfLocalhost` task which gets the web content from a local Jekyll generator (http://localhost:4000)

We can also directly call the `pdf/pdf-build.sh` command file from within the `docs` folder.
The optional `localhost` argument would retrieve the web content locally instead of the remote GitHub site.

### On GitHub

It is generally more convenient to generate and package the PDF format any time a new Audiveris version is released.

The `draft-release.yml` GitHub workflow file, in addition to creating the desired installers,
generates the PDF format of the handbook, and bundles all artifacts as release assets.

It does so by directly calling the `pdf/pdf-build.sh` command file from within the `docs` folder.

[pdf-latest]:   https://github.com/Audiveris/audiveris/releases/latest/download/Audiveris_Handbook.pdf
[web-latest]:   https://audiveris.github.io/audiveris/_pages/handbook/