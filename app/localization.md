# Localization

This document describes the current localization of the Audiveris application,
in order to make Audiveris accessible to anyone interested around the world.

- [First steps](#first-steps)
- [Concrete objectives](#concrete-objectives)
- [Implementation](#implementation)

## First steps

Since its inception, Audiveris has been developed in English
— including source code, comments, issue reports, pull requests, discussions, and so on.  
This choice was driven by practical considerations, aimed at facilitating contributions
of software engineers to this community-developed software.

Beyond the developers, the first users themselves used English terms to interact with the program.  
To make the application accessible to non-English speakers, a first step involved translating some menus
in the graphical user interface into French.

With the help of knowledgeable users, we recently decided to undertake a full translation into French.
Furthermore, this should validate the technical foundations for extending the application to other languages.

## Concrete objectives

A first objective is to define what needs to be translated,
knowing that we are focusing more on the end user than the developer.

- on the GUI (graphical user interface):
    - Menus (pull-down and popup menus)
    - Boards
    - Dialogs
    - Messages (levels ERROR, WARN, INFO)
- on the CLI (command line interface):
    - We must stick to the English keywords
- on the log (log window, log file)
    - We can stick to English for messages levels DEBUG and TRACE, since they are meant for the developer
- on print-outs
    - Same as for messages

## Implementation

### Interactive mode

When running in interactive mode,
Audiveris uses the ***BSAF*** (Better Swing Application Framework) library
to ease the building, event handling, localizable resources and persistency of Swing entities.
Its documentation is available [here][JSR296].

Regarding localization, this framework provides two interesting features that can be combined:
`ResourceMap` and `Action`.

#### ResourceMap

***ResourceMap*** is a map of the Swing resources defined for a particular Java class.  
For example, the Java class `InterBoard` contains this declaration:
```java
private static final ResourceMap resources = Application.getInstance().getContext().getResourceMap(InterBoard.class);
```
It allows to access the resources stored in the related `.properties` files
located in the sibling subfolder named `resources`:
```sh
./resources/InterBoard.properties
./resources/InterBoard_fr.properties
```
The former provides the parent resources (typically defined for the US locale),
while the latter provides the child resources (here for the FR locale).

When a precise resource key cannot be found in the child resources, the parent resources are then searched.

Typical usages of `ResourceMap`:

A ***direct*** use, here to dynamically assign the current text to a JLabel entity.
```java
    /** Output: lyrics above or below related note line. */
    private final JLabel aboveBelow = new JLabel();
    ...
    aboveBelow.setText(resources.getString(isAbove ? "above" : "below"));
```
For this to work correctly, the `./resources/interBoard_fr.properties` file must contain:
```
above = dessus
below = dessous
```

A ***global*** use, to inject resources to all the (named) Swing entities contained in a Component

```java
    /** The swing component of the board instance. */
    private final Panel component = new Panel();
    ...
    // Add many Swing entities within this component ...
    ...    
    // Resources injection
    resources.injectComponents(component); // Here is the magic!
```
For the injection to work, the framework uses the key suffixes `.text` and `.textToolTip`
as in this excerpt of `./resources/interBoard_fr.properties` file:
```
grade.text        = Qualité
grade.toolTipText = Intrinsèque / Contextuelle
```

Note we can use (simple) HTML tags in the strings, for example:
```
musicPane.toolTipText = <html>Ceci est un panneau de musique et texte mélés.\
                        <br>Utiliser un clic droit pour insérer une note.\
                        <br>Appuyer sur Entrée pour valider vos modifications.</html>
```

#### Action

***Action*** is a feature build on top of the notion of `ResourceMap`.
It is typically used via the `@Action` annotation added to a Java method.

Here in the class `BookActions.java`:

```java
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> exportBookAs (ActionEvent e)
    {
        ...
    }
```

and in the sibling file `./resources/BookActions_fr.properties`:
```
exportBookAs.Action.text = Exporter le document vers...
exportBookAs.Action.shortDescription = Exporter en MusicXML vers le fichier choisi
```

Note the suffixes are this time:
- `.Action.text`
- `.Action.shortDescription`


### Batch mode

In batch mode, the BSAF framework is not activated.

In this case, we rely on something lower than BSAF and its ResourceMap: a ResourceBundle

#### ResourceBundle

Typical declaration:

```java
ResourceBundle resources = ResourceUtil.getBundle(Shape.class);
```

`ResourceUtil` is a utility class defined in Audiveris to mimic the behavior of BSAF
when looking for class resources: it looks into the "resources" sub-folder
located as a sibling of the provided Java class.


Examples within the `Shape.java` class 
```java
    description = resources.getString(name() + ".text");
```
```java
    tip = resources.getString(name() + ".toolTipText");
```

and in the `./resources/Shape.properties` file:

```
DAL_SEGNO.text = Dal segno
DAL_SEGNO.toolTipText = D.S.: Repeat from the sign
```


## Work progress

### 24-aug-2026

First version of this documentation.

Start of French localization

Start of restructuring the localization mechanisms, regardless of the language.

### 14-oct-2026

We now have support close to completion for 3 languages: English, French, Romanian

This support is still limited to the GUI items.

Beyond the UI-oriented classes, thanks to the `ResourceUtil.getBundle()` method,
localization has been extended to the classes:
- `OmrStep`
- `ProcessingSwitch`
- `Shape`
- `ShapeSet`
- `TextRole` 

We can now clean up and rationalize the concepts and names used in localization,
as described here under.

#### Name

An entity has a `name`, which is a Java legal identifier composed of letters, digits and underscore.
This identifier is used in the Java source code and in the persistent files like the `.omr` files.  

In the source code, this name can be obtained via the expression:
`<entity>.getName()`  
-- unless a predefined `name()` method already exists
(as is the case for enumerations).

It is not impacted by localization but is often used as a **key** in properties files  
Example: `MORDENT_INVERTED` is one of the many items in the `Shape` enumeration.

#### Text

While the first UI implementations used to display the entity `name`,
we now have chosen to display a related **`text`**, whose precise value depends on the locale selected by the end user.  

In the source code, this text value can be obtained via the expression:
`<entity>.getText()`  

The actual value of this entity text is defined in the dedicated resource file,
using the mandatory `.text` suffix:

| Locale | File name | Statement|
| :---   | :---      | :---     |
| English | Shape.properties | MORDENT_INVERTED.text = Upper mordent |
| French | Shape_fr.properties | MORDENT_INVERTED.text = Mordant ascendant |
| Romanian | Shape_ro.properties | MORDENT_INVERTED.text = Mordent superior |

#### Tip

A user-friendly **`tip`** can sometimes by added to the entity,
meant to be displayed when the user remains on the displayed entity.

In the source code, this tip value can be obtained via the expression:
`<entity>.getTip()`  

The actual value of this entity tip is defined in the dedicated resource file,
using the mandatory `.toolTipText` suffix:

| Locale | File name | Statement|
| :---   | :---      | :---     |
| English | Shape.properties | MORDENT_INVERTED.toolTipText = Mordent with no vertical line |
| French | Shape_fr.properties | MORDENT_INVERTED.toolTipText = Mordant sans ligne verticale |
| Romanian | Shape_ro.properties | MORDENT_INVERTED.toolTipText = Mordent fără linie verticală |



---
[JSR296]:   https://www.oracle.com/technical-resources/articles/javase/swingappfr.html