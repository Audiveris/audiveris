# Audiveris Window Resizing Fix (Wayland/Tiling Window Managers)

This document outlines the changes implemented to address window resizing issues in Audiveris, particularly when running under Wayland-based tiling window managers like **niri**.

## Problem Description
In certain window management environments, Java/Swing applications may fail to correctly re-calculate their internal layout when the window frame is scaled. This results in the window frame changing size while the internal content remains at its original dimensions.

## Implementation Details

### 1. Centralized Workaround in `UIUtil.java`
A new static method `addResizeWorkaround(JFrame frame)` was added to `app/src/main/java/org/audiveris/omr/ui/util/UIUtil.java`. This method attaches a `ComponentListener` that aggressively forces a re-layout and re-validation of the entire window hierarchy whenever a resize event is detected.

```java
/**
 * Add a ComponentListener to the provided frame to force revalidation on resize.
 * This is a workaround for resizing issues in some tiling window managers (e.g. niri).
 *
 * @param frame the frame to be guarded
 */
public static void addResizeWorkaround (final JFrame frame)
{
    frame.addComponentListener(new ComponentAdapter()
    {
        @Override
        public void componentResized (ComponentEvent e)
        {
            SwingUtilities.invokeLater( () -> {
                // Force complete re-layout of the frame hierarchy
                frame.getRootPane().revalidate();
                frame.getContentPane().revalidate();
                frame.invalidate();
                frame.validate();
                frame.getContentPane().validate();
                frame.repaint();
            });
        }
    });
}
```

### 2. Integration into Major UI Components
The workaround was integrated into the following key UI components to ensure consistent behavior across the application:

*   **Main Application Window**: Applied in `app/src/main/java/org/audiveris/omr/ui/MainGui.java` during the startup sequence.
*   **Book Browser**: Applied in `app/src/main/java/org/audiveris/omr/sheet/ui/BookBrowser.java` when the browser frame is initialized.
*   **Classifier Trainer**: Applied in `app/src/main/java/org/audiveris/omr/classifier/ui/Trainer.java` for the training interface.
*   **Sample Browser**: Applied in `app/src/main/java/org/audiveris/omr/classifier/ui/SampleBrowser.java` for browsing training samples.
*   **Symbol Ripper**: Applied in `app/src/main/java/org/audiveris/omr/ui/symbol/SymbolRipper.java` for the standalone font ripping utility.
*   **Shape Color Chooser**: Applied in `app/src/main/java/org/audiveris/omr/glyph/ui/ShapeColorChooser.java` for the color configuration interface.

### 3. Usage & Environment Configuration
For optimal results in Wayland/tiling environments, it is recommended to run Audiveris with the following environment variable:

```bash
_JAVA_AWT_WM_NONREPARENTING=1 ./gradlew run
```

This setting helps the Java AWT system correctly perceive window dimensions in environments where windows are not traditionally "reparented" by the window manager.
