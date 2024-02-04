# Flatpak generation and Flathub submission

This directory contains helper scripts for creating a flatpak package
and optionally uploading it to the [audiveris flathub
repository](https://github.com/flathub/org.audiveris.Audiveris).

## The flathub submodule

The audiveris git tree now contains a submodule for the flathub code
under `dev/flathub`. To initialize this submodule, run

    git submodule init
	git submodule update

By tracking this as a git submodule rather than keeping the code
directly in the audiveris git tree, it is possible to push directly
to the flathub repository when a new version must be created.

The contents of the flathub directory are as follows:

* The [flatpak manifest](https://manpages.debian.org/testing/flatpak-builder/flatpak-manifest.5.en.html), 
  `org.audiveris.audiveris.yml`,
* The [appstream meta data](https://freedesktop.org/software/appstream/docs/chap-Metadata.html), 
  `org.audiveris.audiveris.metainfo.xml`,
* A linux XDG desktop file, `org.audiveris.audiveris.desktop`,
* A small sed script (`add-tessdata-prefix.sed`) that fixes the path for the
  tesseract files in the flatpak environment,
* A patch (`0001-add-local-dependencies-mirror.patch`) to enable building
  with **gradle** offline, using a local mirror of java dependencies,
* Optionally, a `flathub.json` file with flathub build instructions,
* A list of tesseract language files to include in the build
  (`lang_sources.yml`)
* A list of Java dependencies to download from Maven and other repositories
  (`dependencies.yml`),
* A shell script (`mkgradlerepo.sh`) that creates the local mirror of Java
  dependencies from the sources downloaded by flatpak in the build process.

The last three files are auto-generated from the audiveris sources, using
scripts in the `dev/flatpak` subdirectory.

## Helper scripts in the flatpak subdirectory

Flatpak requires that all build dependencies must be downloaded before the
actual build, which runs offline. The subdirectory `dev/flatpak` contains
scripts for auto-generation of files in the flathub directory.

### Updating the list of Java dependencies

Run `./dev/flatpak/create-flatpak-dependencies.py`. This will start a
**gradle** build of Audiveris, saving artifacts under `dev/flatpak/gradle`,
and create a list of download URLs and SHA1 sums for flatpak in the
`dependencies.yml` file in the flathub directory.
Moreover, the script also creates the `mkgradlerepo.sh` script, which will
run during the flatpak build process and create a repository with the
structure required by **gradle** from the downloaded dependencies.

### Changing the list of supported languages

**Note:** Doing this isn't necessary unless the list of languages must be changed.

The flatpak has a built-in set of Tesseract language files for text
recognition support. By default, the list of languages is English (`eng`),
French (`fra`), German (`deu`) and Italian (`ita`).
The helper script `languages.sh` can be used to create a flatpak
source list with the desired Tesseract files. To run the script:

    cd dev/flatpak
    ./languages.sh --interactive

Type `?` to show the available and currently configured languages. Add a
language by entering  either the number or the abbreviation of the language
you wish to add. When done selecting, simply press ENTER. The script will
download the language files, determine the URLs and hashes for the
flatpak build, and create an updated `lang_sources.yml` file in the
flathub directory.

If you know which languages to configure, simply the script like this

	./languages.sh ita fra

## Workflow for Flathub submission

Update the list of dependencies as shown above, and if desired, update
the list of languages.

    git -C dev/flathub status
	
will show if anything has changed wrt the current flathub code. If not,
there's probably nothing to do.

**Important:** The dependency generation always uses the currently checked-out
audiveris sources. But flatpak builds the sources from the `tag` and `commit`
hardcoded in the manifest `org.audiveris.audiveris.yml`:

    sources:
      - type: git
        url: https://github.com/Audiveris/audiveris
        tag: 5.4-alpha
        commit: 698b2bb69512ca8b61747630e6c0843bb3b6eda0

If necessary, adapt the tag and commit fields such that the list of
dependencies generated from current `HEAD` matches the revision given in
the manifest; otherwise the flatpak build may fail.

If the revision in the manifest is updated, an update of the
version history in the `org.audiveris.audiveris.metainfo.xml` may also be
necessary.

After applying these changes in the `dev/flathub` subdirectory, create
commits both in the submodule and in the top directory:

    git -C dev/flathub commit -a -m 'Recreated dependencies, updated to 5.4-alpha'
    git commit -m 'flathub: updated to 5.4-alpha' .gitmodules

At this point, it is strongly recommended to perform a flatpak test build to
make sure that the dependency generation worked and all meta data are formally
correct:

    cd dev/flathub
    flatpak-builder --force-clean build org.audiveris.audiveris.yml

If this this step succeeds, the flathub code can now be pushed to the flathub
repository:

    git -C dev/flathub push origin HEAD:my-new-branch
	
From this branch, a github PR can be created against either the `development` or the
`master` branch of the main flathub, where the flathub build bot will pick
it up. Build results can be inspected on the [buildbot page](https://buildbot.flathub.org/).
