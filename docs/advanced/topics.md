## Advanced Topics

Only standard features are visible by default in Audiveris user interface.

Some advanced features are gathered by `Topics`, that the end user can explicitly activate.

This is done via the `Tools | Advanced Topics...` pulldown menu.

![](/assets/advanced_topics.png)

### Input Step

This box allows to define which step is automatically performed on an input file
(regardless whether the image is opened or dropped).

### Default Plugin

This allows to interactively choose a new default plugin among the declared ones,
since by default the first declared plugin is set as the default one
(See [Plugins](plugins.md) section).

### Advanced Topics

 Each of these topics can gather several related features.

* `SAMPLES` deals with sample repositories and classifier training.
* `ANNOTATIONS` deals with production of symbol annotations.
* `PLOTS` deals with display of plots for scale, stem, staff or header projections.
* `SPECIFIC_VIEWS` deals with specific sheet tabs (staff free, staff-line glyphs).
* `SPECIFIC_ITEMS` deals with display of specific items on views (attachments, glyph axis, ...)
* `WINDOW_LAYOUT` deals with layout of main window (such as hiding boards column),
* `DEBUG` deals with many debug features (notably browsing a book hierarchy).


 Note that an __application restart__ is needed to take any modified selection into account,
 because of the various impacts this implies on UI elements.
