//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           U I U t i l                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui.util;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.WellKnowns;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import static java.awt.Frame.ICONIFIED;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * Class {@code UIUtil} gathers utilities related to User Interface
 *
 * @author Hervé Bitteur and Brenton Partridge
 */
public abstract class UIUtil
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(UIUtil.class);

    /** Ratio to be applied on all font size. */
    public static final float GLOBAL_FONT_RATIO = constants.globalFontRatio.getValue().floatValue();

    /**
     * Customized border for tool buttons, to use consistently in all UI components.
     */
    private static Border toolBorder;

    /**
     * Listener which disposes a window being closed.
     */
    public static final WindowListener closeWindow = new WindowAdapter()
    {
        @Override
        public void windowClosing (WindowEvent e)
        {
            e.getWindow().dispose();
        }
    };

    //--------------------//
    // adjustDefaultFonts //
    //--------------------//
    /**
     * Adjust default font names and sizes according to user choices.
     */
    public static void adjustDefaultFonts ()
    {
        // Control font
        final Font defaultFont = new Font(constants.defaultFontName.getValue(),
                                          Font.PLAIN,
                                          adjustedSize(constants.defaultFontSize.getValue()));
        final Font controlFont = defaultFont;
        UIManager.put("Button.font", controlFont);
        UIManager.put("CheckBox.font", controlFont);
        UIManager.put("ComboBox.font", controlFont);
        UIManager.put("DesktopIcon.font", controlFont);
        UIManager.put("IconButton.font", controlFont); // ???
        UIManager.put("Label.font", controlFont);
        UIManager.put("List.font", controlFont); // ???
        UIManager.put("ProgressBar.font", controlFont);
        UIManager.put("RadioButton.font", controlFont);
        UIManager.put("Slider.font", controlFont);
        UIManager.put("Spinner.font", controlFont);
        UIManager.put("TabbedPane.font", controlFont);
        UIManager.put("ToggleButton.font", controlFont);

        // Menu font
        final Font menuFont = defaultFont;
        UIManager.put("CheckBoxMenuItem.font", menuFont);
        UIManager.put("Menu.font", menuFont);
        UIManager.put("MenuBar.font", menuFont);
        UIManager.put("MenuItem.font", menuFont);
        UIManager.put("PopupMenu.font", menuFont);
        UIManager.put("RadioButtonMenuItem.font", menuFont);
        UIManager.put("ToolBar.font", menuFont);

        // User font
        final Font userFont = defaultFont;
        UIManager.put("EditorPane.font", userFont);
        UIManager.put("FormattedTextField.font", userFont);
        UIManager.put("PasswordField.font", userFont);
        UIManager.put("Table.font", userFont);
        UIManager.put("TableHeader.font", userFont);
        UIManager.put("TextArea.font", userFont);
        UIManager.put("TextField.font", userFont);
        UIManager.put("TextPane.font", userFont);
        UIManager.put("Tree.font", userFont);

        // Window title (was Dialog font)
        final Font windowTitleFont = defaultFont;
        UIManager.put("ColorChooser.font", windowTitleFont);
        UIManager.put("OptionPane.font", windowTitleFont);
        UIManager.put("Panel.font", windowTitleFont);
        UIManager.put("ScrollPane.font", windowTitleFont);
        UIManager.put("Viewport.font", windowTitleFont);

        // Miscellaneous fonts
        UIManager.put("ToolTip.font", new Font(
                      constants.defaultFontName.getValue(),
                      Font.ITALIC,
                      adjustedSize(constants.defaultFontSize.getValue())));
        UIManager.put("TitledBorder.font", new Font(
                      constants.defaultFontName.getValue(),
                      Font.BOLD,
                      adjustedSize(constants.defaultFontSize.getValue())));
    }

    //--------------------//
    // adjustDefaultTexts //
    //--------------------//
    /**
     * Adjust default texts according to user chosen locale.
     */
    public static void adjustDefaultTexts ()
    {

        final ResourceMap resources = Application.getInstance().getContext().getResourceMap(
                UIUtil.class);

        // OptionPane texts
        final String[] keys = new String[]{
            "OptionPane.inputDialogTitle",
            "OptionPane.messageDialogTitle",
            "OptionPane.titleText",
            "OptionPane.cancelButtonText",
            "OptionPane.noButtonText",
            "OptionPane.yesButtonText"
        };

        for (String key : keys) {
            final String localizedString = resources.getString(key);

            if (localizedString != null) {
                UIManager.put(key, localizedString);
            }
        }
    }

    //--------------//
    // adjustedSize //
    //--------------//
    /**
     * Report the size multiplied by global font ratio.
     *
     * @param size provided size
     * @return adjusted size
     */
    public static int adjustedSize (double size)
    {
        return (int) Math.rint(size * GLOBAL_FONT_RATIO);
    }

    //--------------------//
    // complementaryColor //
    //--------------------//
    /**
     * Report the complementary of provided color.
     *
     * @param color provided color
     * @return the reverse color
     */
    public static Color complementaryColor (Color color)
    {
        return new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue());
    }

    //------------------//
    // directoryChooser //
    //------------------//
    /**
     * Let the user select a directory.
     *
     * @param parent   the parent component for the dialog
     * @param startDir the starting directory
     * @param title    specific dialog title if any, null otherwise
     * @return the chosen directory, or null
     */
    public static File directoryChooser (Component parent,
                                         File startDir,
                                         String title)
    {
        //        String oldMacProperty = System.getProperty("apple.awt.fileDialogForDirectories", "false");
        //        System.setProperty("apple.awt.fileDialogForDirectories", "true");
        OmrFileFilter filter = new OmrFileFilter("Directories", new String[]{})
        {
            @Override
            public boolean accept (File f)
            {
                return (f.isDirectory());
            }
        };

        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES); // FILES needed if dir doesn't exist
        fc.addChoosableFileFilter(filter); // To display directories list
        fc.setFileFilter(filter); // To display this list by default
        ///fc.setAcceptAllFileFilterUsed(false); // Don't display the AllFiles list

        if (title != null) {
            fc.setDialogTitle(title);
        }

        ///
        if (startDir != null) {
            // Pre-select the proposed directory
            File parentDir = startDir.getParentFile();
            fc.setCurrentDirectory(parentDir);
            fc.setSelectedFile(startDir);
        }

        final int result = fc.showSaveDialog(parent);

        //        System.setProperty("apple.awt.fileDialogForDirectories", oldMacProperty);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        } else {
            return null;
        }
    }

    //---------------//
    // enableActions //
    //---------------//
    /**
     * Given a list of actions, set all these actions (whether they
     * derive from AbstractAction or AbstractButton) enabled or not,
     * according to the bool parameter provided.
     *
     * @param actions list of actions to enable/disable as a whole
     * @param bool    true for enable, false for disable
     */
    public static void enableActions (Collection<?> actions,
                                      boolean bool)
    {
        for (Object next : actions) {
            if (next instanceof AbstractAction) {
                ((Action) next).setEnabled(bool);
            } else if (next instanceof AbstractButton) {
                ((Component) next).setEnabled(bool);
            } else {
                logger.warn("Neither Button nor Action : {}", next);
            }
        }
    }

    //-------------//
    // fileChooser //
    //-------------//
    /**
     * A replacement for standard JFileChooser, to allow better look and feel on the Mac
     * platform.
     *
     * @param save      true for a SAVE dialog, false for a LOAD dialog
     * @param parent    the parent component for the dialog, if any
     * @param startFile default file, or just default directory, or null
     * @param filter    a filter to be applied on files
     * @return the selected file, or null
     */
    public static File fileChooser (boolean save,
                                    Component parent,
                                    File startFile,
                                    OmrFileFilter filter)
    {
        return fileChooser(save, parent, startFile, filter, null);
    }

    //-------------//
    // fileChooser //
    //-------------//
    /**
     * A replacement for standard JFileChooser, to allow better look and feel on the Mac
     * platform.
     *
     * @param save      true for a SAVE dialog, false for a LOAD dialog
     * @param parent    the parent component for the dialog, if any
     * @param startFile default file, or just default directory, or null
     * @param filter    a filter to be applied on files
     * @param title     a specific dialog title or null
     * @return the selected file, or null
     */
    public static File fileChooser (boolean save,
                                    Component parent,
                                    File startFile,
                                    OmrFileFilter filter,
                                    String title)
    {
        File file = null;

        if (WellKnowns.MAC_OS_X) {
            if ((parent == null) && (org.audiveris.omr.OMR.gui != null)) {
                parent = org.audiveris.omr.OMR.gui.getFrame();
            }

            Component parentFrame = parent;

            if (parentFrame != null) {
                while (parentFrame.getParent() != null) {
                    parentFrame = parentFrame.getParent();
                }
            }

            try {
                final FileDialog fd = new FileDialog((Frame) parentFrame);

                if (startFile != null) {
                    fd.setDirectory(
                            startFile.isDirectory() ? startFile.getPath() : startFile.getParent());
                }

                fd.setMode(save ? FileDialog.SAVE : FileDialog.LOAD);
                fd.setFilenameFilter(filter);

                if (title == null) {
                    title = save ? "Saving: " : "Loading: ";
                    title += filter.getDescription();
                }

                fd.setTitle(title);
                fd.setVisible(true);

                String fileName = fd.getFile();
                String dir = fd.getDirectory();

                if ((dir != null) && (fileName != null)) {
                    String fullName = dir + WellKnowns.FILE_SEPARATOR + fileName;
                    file = new File(fullName);
                }
            } catch (ClassCastException e) {
                logger.warn("no ancestor is Frame");
            }
        } else {
            final JFileChooser fc = new JFileChooser();
            // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6317789
            //            final JFileChooser fc = new JFileChooser()
            //            {
            //                @Override
            //                public void updateUI ()
            //                {
            //                    putClientProperty("FileChooser.useShellFolder", false);
            //                    super.updateUI();
            //                }
            //            };
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

            // Pre-select the directory, and potentially the file to save to
            if (startFile != null) {
                if (startFile.isDirectory()) {
                    fc.setCurrentDirectory(startFile);
                } else {
                    File parentFile = startFile.getParentFile();
                    fc.setCurrentDirectory(parentFile);
                    fc.setSelectedFile(startFile);
                }
            }

            fc.addChoosableFileFilter(filter);
            fc.setFileFilter(filter);

            if (title != null) {
                fc.setDialogTitle(title);
            }

            int result = save ? fc.showSaveDialog(parent) : fc.showOpenDialog(parent);

            if (result == JFileChooser.APPROVE_OPTION) {
                file = fc.getSelectedFile();
            }
        }

        return file;
    }

    //---------------//
    // getToolBorder //
    //---------------//
    /**
     * Report a standard tool border entity, which is a blank border.
     *
     * @return the standard tool border
     */
    public static Border getToolBorder ()
    {
        if (toolBorder == null) {
            toolBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2);
        }

        return toolBorder;
    }

    //---------------//
    // getSplitSpace //
    //---------------//
    /**
     * Report the available space for a child component within first JSplitPane ancestor,
     * taking into account the JSplitPane size, the divider location and size and all
     * the insets of ancestors until the JSplitPane.
     *
     * @param comp the child component
     * @return the available space (width for a horizontal JSplitPane, height for a vertical one).
     *         Null, if no JSplitPane ancestor could be found.
     */
    public static Integer getSplitSpace (JComponent comp)
    {
        if (comp == null) {
            return null;
        }

        synchronized (comp.getTreeLock()) {
            // First find the JSplitPane ancestor and the used side of the split
            Container ancestor = comp.getParent();
            Component child = comp;

            while (ancestor != null && !(ancestor instanceof JSplitPane)) {
                child = ancestor;
                ancestor = child.getParent();
            }

            if (ancestor == null) {
                return null; // No JSplitPane container found
            }

            final JSplitPane sp = (JSplitPane) ancestor;
            final int divLoc = sp.getDividerLocation();
            final int divSize = sp.getDividerSize();
            final boolean first = (sp.getLeftComponent() == child);

            // Now compute available space
            int space;

            if (first) {
                space = divLoc;
            } else {
                if (sp.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                    space = sp.getSize().width;
                } else {
                    space = sp.getSize().height;
                }

                space -= (divLoc + divSize);
            }

            space -= cumulateInsets(comp, sp, sp.getOrientation());
            logger.debug("space: {}", space);

            return space;
        }
    }

    //----------//
    // htmlLink //
    //----------//
    /**
     * Report the HTML fragment for an active link to the provided URL string.
     *
     * @param url URL as a string
     * @return HTML text
     */
    public static String htmlLink (String url)
    {
        StringBuilder sb = new StringBuilder();

        final int size = UIUtil.adjustedSize(constants.urlFontSize.getValue());
        final String name = constants.defaultFontName.getValue();
        sb.append("<style> body ")
                .append("{font-family: ").append(name).append(';')
                .append(" font-size: ").append(size).append("px;")
                .append("} </style>");

        sb.append("<A HREF=\"").append(url).append("\">").append(url).append("</A>");

        return sb.toString();
    }

    //-------------//
    // insertTitle //
    //-------------//
    /**
     * Insert a pseudo-item, to be used as a menu title.
     *
     * @param menu the containing menu
     * @param text the title text
     */
    public static void insertTitle (JMenu menu,
                                    String text)
    {
        JMenuItem title = new JMenuItem(text);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setEnabled(false);
        menu.add(title);
        menu.addSeparator();
    }

    //----------//
    // minimize //
    //----------//
    /**
     * Minimize the provided frame.
     *
     * @param frame the frame to minimize to icon
     * @see #unMinimize(JFrame)
     */
    public static void minimize (JFrame frame)
    {
        int state = frame.getExtendedState();
        state &= ICONIFIED;
        frame.setExtendedState(state);
    }

    //-------------//
    // pathChooser //
    //-------------//
    /**
     * A replacement for standard JFileChooser, to allow better look and feel on the Mac
     * platform.
     *
     * @param save      true for a SAVE dialog, false for a LOAD dialog
     * @param parent    the parent component for the dialog, if any
     * @param startPath default path, or just default directory, or null
     * @param filter    a filter to be applied on files
     * @return the selected file, or null
     */
    public static Path pathChooser (boolean save,
                                    Component parent,
                                    Path startPath,
                                    OmrFileFilter filter)
    {
        return pathChooser(save, parent, startPath, filter, null);
    }

    //-------------//
    // pathChooser //
    //-------------//
    /**
     * A replacement for standard JFileChooser, to allow better look and feel on the Mac
     * platform.
     *
     * @param save      true for a SAVE dialog, false for a LOAD dialog
     * @param parent    the parent component for the dialog, if any
     * @param startPath default path, or just default directory, or null
     * @param filter    a filter to be applied on files
     * @param title     a specific dialog title or null
     * @return the selected file, or null
     */
    public static Path pathChooser (boolean save,
                                    Component parent,
                                    Path startPath,
                                    OmrFileFilter filter,
                                    String title)
    {
        File file = fileChooser(save, parent, startPath.toFile(), filter, null);

        if (file != null) {
            return file.toPath();
        }

        return null;
    }

    //-------------------------//
    // setAbsoluteDashedStroke //
    //-------------------------//
    /**
     * Similar to {@link #setAbsoluteStroke(java.awt.Graphics, float)} but for a dashed
     * stroke.
     *
     * @param g     the current graphics context
     * @param width the absolute stroke width desired
     * @return the previous stroke
     */
    public static Stroke setAbsoluteDashedStroke (Graphics g,
                                                  float width)
    {
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform AT = g2.getTransform();
        double ratio = AT.getScaleX();
        Stroke oldStroke = g2.getStroke();
        Stroke stroke = new BasicStroke(
                width / (float) ratio,
                BasicStroke.CAP_SQUARE,
                BasicStroke.JOIN_MITER,
                10.0f,
                new float[]{3.0f},
                0.0f);
        g2.setStroke(stroke);

        return oldStroke;
    }

    //-------------------//
    // setAbsoluteStroke //
    //-------------------//
    /**
     * Whatever the current scaling of a graphic context, set the stroke to the desired
     * absolute width, and return the saved stroke for later restore.
     *
     * @param g     the current graphics context
     * @param width the absolute stroke width desired
     * @return the previous stroke
     */
    public static Stroke setAbsoluteStroke (Graphics g,
                                            float width)
    {
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform AT = g2.getTransform();
        double ratio = AT.getScaleX();
        Stroke oldStroke = g2.getStroke();
        Stroke stroke = new BasicStroke(width / (float) ratio);
        g2.setStroke(stroke);

        return oldStroke;
    }

    //-----------------//
    // suppressBorders //
    //-----------------//
    /**
     * Browse the whole hierarchy of components and nullify their borders.
     *
     * @param container top of hierarchy
     */
    public static void suppressBorders (Container container)
    {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JComponent) {
                suppressBorders((JComponent) comp);
            } else if (comp instanceof Container) {
                suppressBorders((Container) comp);
            }
        }
    }

    //-----------------//
    // suppressBorders //
    //-----------------//
    /**
     * Nullify the border of this JComponent, as well as its subcomponents.
     *
     * @param comp top of hierarchy
     */
    public static void suppressBorders (JComponent comp)
    {
        if (!(comp instanceof JButton) && !(comp instanceof JToggleButton)) {
            comp.setBorder(null);
        }

        suppressBorders((Container) comp);
    }

    //------------//
    // unMinimize //
    //------------//
    /**
     * Restore a frame, possibly minimized, to the front (either normal or maximized)
     *
     * @param frame the frame to show
     * @see #minimize(JFrame)
     */
    public static void unMinimize (JFrame frame)
    {
        int state = frame.getExtendedState();

        if ((state & ICONIFIED) != 0) {
            state &= ~ICONIFIED;
        }

        frame.setExtendedState(state);

        frame.setVisible(true);
        frame.toFront();
    }

    //--------------------//
    // getDefaultFontSize //
    //--------------------//
    /**
     * Report the font size used by default when no ratio is applied.
     *
     * @return the default font size in application
     */
    public static int getDefaultFontSize ()
    {
        return constants.defaultFontSize.getValue();
    }

    //--------------------//
    // getGlobalFontRatio //
    //--------------------//
    /**
     * Report the current global font ratio.
     *
     * @return current global font ratio
     */
    public static double getGlobalFontRatio ()
    {
        return constants.globalFontRatio.getValue();
    }

    //--------------------//
    // setGlobalFontRatio //
    //--------------------//
    /**
     * Set value for global font ratio.
     *
     * @param ratio value to set
     */
    public static void setGlobalFontRatio (double ratio)
    {
        if (ratio != constants.globalFontRatio.getValue()) {
            constants.globalFontRatio.setValue(ratio);
            logger.info("Global font ratio: {} at next restart", ratio);
        }
    }

    //-----------------------//
    // getMaxGlobalFontRatio //
    //-----------------------//
    /**
     * Report the maximum allowed value for global font ratio.
     *
     * @return maximum allowed value
     */
    public static double getMaxGlobalFontRatio ()
    {
        return constants.maxGlobalFontRatio.getValue();
    }

    //-----------------------//
    // getMinGlobalFontRatio //
    //-----------------------//
    /**
     * Report the minimum allowed value for global font ratio.
     *
     * @return minimum allowed value
     */
    public static double getMinGlobalFontRatio ()
    {
        return constants.minGlobalFontRatio.getValue();
    }

    /** Not meant to be instantiated. */
    private UIUtil ()
    {
    }

    //----------------//
    // cumulateInsets //
    //----------------//
    /**
     * Cumulate the insets of all ancestors of the provided component, until the provided
     * ancestor (included) is reached.
     *
     * @param comp     the provided component
     * @param ancestor the target ancestor
     * @param split    either HORIZONTAL_SPLIT or VERTICAL_SPLIT
     * @return the cumulated insets on split orientation
     */
    private static int cumulateInsets (JComponent comp,
                                       JComponent ancestor,
                                       int split)
    {
        int sum = 0;

        Container parent = comp.getParent();

        while (parent != null) {
            Insets insets = parent.getInsets();
            logger.debug("{} {}", parent.getName(), insets);

            switch (split) {
            case JSplitPane.VERTICAL_SPLIT:
                sum += insets.top;
                sum += insets.bottom;

                break;
            case JSplitPane.HORIZONTAL_SPLIT:
                sum += insets.left;
                sum += insets.right;
                break;
            }

            if (parent == ancestor) {
                break;
            }

            parent = parent.getParent();
        }

        logger.debug("cumulateInsets: {}", sum);
        return sum;
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio minGlobalFontRatio = new Constant.Ratio(
                1.0,
                "Minimum value for global font ratio");

        private final Constant.Ratio maxGlobalFontRatio = new Constant.Ratio(
                2.0,
                "Maximum value for global font ratio");

        private final Constant.Ratio globalFontRatio = new Constant.Ratio(
                1.0,
                "Ratio to be applied on every font size (restart needed)");

        private final Constant.Integer defaultFontSize = new Constant.Integer(
                "Points",
                12,
                "Default font size");

        private final Constant.Integer urlFontSize = new Constant.Integer(
                "Points",
                9,
                "Font size for URL");

        private final Constant.String defaultFontName = new Constant.String(
                "Arial",
                "Default font name (e.g. Arial, Dialog, Segoe UI, ...");
    }
}
