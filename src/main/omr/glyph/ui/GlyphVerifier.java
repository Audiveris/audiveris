//----------------------------------------------------------------------------//
//                                                                            //
//                         G l y p h V e r i f i e r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.Main;
import static omr.selection.SelectionHint.*;

import omr.ui.util.UILookAndFeel;

import omr.util.Implement;
import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.awt.List;
import java.awt.event.*;
import java.io.File;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

/**
 * Class <code>GlyphVerifier</code> provides a user interface to browse through
 * all glyphs recorded for evaluator training, and allow to visually check the
 * correctness of their assigned shape.
 *
 * <p>One, several or all recorded sheets can be selected.
 *
 * <p>Within the contained glyphs, one, several or all can be selected, the
 * selected glyphs can then be browsed in any direction.
 *
 * <p>The current glyph is displayed, with its appearance in a properly
 * translated GlyphLag view, and its characteristics in a dedicated panel. If
 * the user wants to discard the glyph, it can be removed from the repository of
 * training material.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphVerifier
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger   logger = Logger.getLogger(
        GlyphVerifier.class);

    /** To differentiate the exit action */
    private static boolean standAlone = false;

    /** The unique instance */
    private static GlyphVerifier INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** Repository of known glyphs */
    private final GlyphRepository repository = GlyphRepository.getInstance();

    /** The dedicated frame */
    private final JFrame frame;

    /** The panel in charge of the current glyph */
    private GlyphBrowser glyphBrowser = new GlyphBrowser(this);

    /** The panel in charge of the glyphs selection */
    private GlyphSelector glyphSelector = new GlyphSelector(glyphBrowser);

    /** The panel in charge of the shapes selection */
    private ShapeSelector shapeSelector = new ShapeSelector(glyphSelector);

    /** The panel in charge of the sheets (or icons folder) selection */
    private FolderSelector folderSelector = new FolderSelector(shapeSelector);

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // GlyphVerifier //
    //---------------//
    /**
     * Create an instance of Glyph Verifier
     */
    private GlyphVerifier ()
    {
        frame = new JFrame();
        frame.setTitle("Glyph Verifier");
        frame.getContentPane()
             .setLayout(new BorderLayout());
        frame.getContentPane()
             .add(getSelectorsPanel(), BorderLayout.NORTH);
        frame.getContentPane()
             .add(glyphBrowser, BorderLayout.CENTER);
        frame.pack();
        frame.setBounds(new Rectangle(20, 20, 1000, 600));
        frame.setVisible(true);

        if (standAlone) {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Give access to the single instance of this class
     *
     * @return the GlyphVerifier instance
     */
    public static GlyphVerifier getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new GlyphVerifier();
        }

        return INSTANCE;
    }

    //------------//
    // setVisible //
    //------------//
    /**
     * Make the UI frame visible or not
     *
     * @param bool true for visible, false for hidden
     */
    public void setVisible (boolean bool)
    {
        frame.setVisible(bool);
    }

    //----------------//
    // dumpSelections //
    //----------------//
    public void dumpSelections ()
    {
        glyphBrowser.dumpSelections();
    }

    //------//
    // main //
    //------//
    /**
     * Just to allow stand-alone testing of this class
     *
     * @param args not used
     */
    public static void main (String... args)
    {
        standAlone = true;

        // UI Look and Feel
        UILookAndFeel.setUI(null);

        new GlyphVerifier();
    }

    //--------//
    // verify //
    //--------//
    /**
     * Focus the verifier on a provided collection of glyphs (typically, such
     * glyphs that are not recognized, or mistaken, by the evaluators)
     *
     * @param glyphNames the names of the specific glyphs to inspect
     */
    public void verify (Collection<String> glyphNames)
    {
        // Glyphs
        glyphSelector.populateWith(glyphNames);
        glyphSelector.selectAll();

        // Shapes
        SortedSet<String> shapeSet = new TreeSet<String>();

        for (String gName : glyphNames) {
            File file = new File(gName);
            shapeSet.add(radixOf(file.getName()));
        }

        shapeSelector.populateWith(shapeSet);
        shapeSelector.selectAll();

        // Sheets / Icons folder
        SortedSet<String> folderSet = new TreeSet<String>();

        for (String gName : glyphNames) {
            File file = new File(gName);
            folderSet.add(file.getParent());
        }

        folderSelector.populateWith(folderSet);
        folderSelector.selectAll();
        
        // Load the first glyph in the browser
        glyphBrowser.loadGlyphNames();
    }

    //---------------//
    // getGlyphNames //
    //---------------//
    String[] getGlyphNames ()
    {
        return glyphSelector.list.getSelectedItems();
    }

    //-----------------//
    // deleteGlyphName //
    //-----------------//
    void deleteGlyphName (String gName)
    {
        // Remove entry from list
        glyphSelector.list.remove(gName);
    }

    //--------------//
    // getActualDir //
    //--------------//
    /**
     * Report the real directory (either the sheets directory or the icons
     * directory) that corresponds to a given folder name
     *
     * @param folder the folder name, such as 'batuque' or 'icons'
     * @return the concrete directory
     */
    private File getActualDir (String folder)
    {
        if (repository.isIconsFolder(folder)) {
            return Main.getIconsFolder();
        } else {
            return new File(repository.getSheetsFolder(), folder);
        }
    }

    //-------------------//
    // getSelectorsPanel //
    //-------------------//
    private JPanel getSelectorsPanel ()
    {
        FormLayout   layout = new FormLayout(
            "max(100dlu;pref),max(150dlu;pref),max(200dlu;pref):grow",
            "80dlu");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int             r = 1; // --------------------------------
        builder.add(folderSelector, cst.xy(1, r));
        builder.add(shapeSelector, cst.xy(2, r));
        builder.add(glyphSelector, cst.xy(3, r));

        return builder.getPanel();
    }

    //---------//
    // radixOf //
    //---------//
    private static String radixOf (String path)
    {
        int i = path.indexOf('.');

        if (i >= 0) {
            return path.substring(0, i);
        } else {
            return "";
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------//
    // Selector //
    //----------//
    /**
     * Class <code>Selector</code> defines the common properties of sheet, shape
     * and glyph selectors. Each selector is made of a list of names, which can
     * be selected and deselected at will.
     */
    private abstract static class Selector
        extends TitledPanel
        implements ActionListener, ChangeListener
    {
        /** Other entity interested in items selected by this selector */
        private ChangeListener listener;

        /** Change event, lazily created */
        private ChangeEvent changeEvent;

        // Buttons
        protected JButton cancelAll = new JButton("Cancel All");
        protected JButton load = new JButton("Load");
        protected JButton selectAll = new JButton("Select All");

        // Label
        protected JLabel cardinal = new JLabel(
            "* No item selected *",
            SwingConstants.CENTER);

        // List of items
        protected List list = new List(
            5, // nb of rows
            true); // multipleMode allowed ?

        /**
         * Create a selector
         *
         * @param title label for this selector
         * @param listener potential (external) listener for changes
         */

        //----------//
        // Selector //
        //----------//
        public Selector (String         title,
                         ChangeListener listener)
        {
            super(title);
            this.listener = listener;

            // Precise action to be specified in each subclass
            load.addActionListener(this);

            // To be informed of mouse (de)selections (not programmatic)
            list.addItemListener(
                new ItemListener() {
                        public void itemStateChanged (ItemEvent e)
                        {
                            updateCardinal(); // Brute force !!!
                        }
                    });

            // Same action whatever the subclass : select all items
            selectAll.addActionListener(
                new ActionListener() {
                        public void actionPerformed (ActionEvent e)
                        {
                            selectAll();
                        }
                    });

            // Same action whatever the subclass : deselect all items
            cancelAll.addActionListener(
                new ActionListener() {
                        public void actionPerformed (ActionEvent e)
                        {
                            for (int i = 0; i < list.getItemCount(); i++) {
                                list.deselect(i);
                            }

                            updateCardinal();
                        }
                    });

            JPanel buttons = new JPanel(new GridLayout(3, 1));
            buttons.add(load);
            buttons.add(selectAll);
            buttons.add(cancelAll);

            // All buttons are initially disabled
            load.setEnabled(false);
            selectAll.setEnabled(false);
            cancelAll.setEnabled(false);

            add(buttons, BorderLayout.WEST);
            add(list, BorderLayout.CENTER);
            add(cardinal, BorderLayout.SOUTH);
        }

        //--------------//
        // populateWith //
        //--------------//
        public void populateWith (Collection<String> names)
        {
            list.removeAll();

            for (String name : names) {
                list.add(name);
            }

            updateCardinal();
        }

        //-----------//
        // selectAll //
        //-----------//
        public void selectAll ()
        {
            for (int i = 0; i < list.getItemCount(); i++) {
                list.select(i);
            }

            updateCardinal();
        }

        //--------------//
        // stateChanged //
        //--------------//
        public void stateChanged (ChangeEvent e)
        {
            Selector selector = (Selector) e.getSource();
            int      selNb = selector.list.getSelectedItems().length;
            load.setEnabled(selNb > 0);
        }

        //----------------//
        // updateCardinal //
        //----------------//
        protected void updateCardinal ()
        {
            int selectNb = list.getSelectedItems().length;

            if (selectNb > 0) {
                cardinal.setText(selectNb + " item(s) selected");
            } else {
                cardinal.setText("* No item selected *");
            }

            // Buttons
            selectAll.setEnabled(list.getItemCount() > 0);
            cancelAll.setEnabled(list.getSelectedItems().length > 0);

            // Notify other entity
            if (listener != null) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }

                listener.stateChanged(changeEvent);
            }
        }
    }

    //-------------//
    // TitledPanel //
    //-------------//
    private static class TitledPanel
        extends JPanel
    {
        public TitledPanel (String title)
        {
            setBorder(
                BorderFactory.createTitledBorder(
                    new EtchedBorder(),
                    title,
                    TitledBorder.CENTER,
                    TitledBorder.TOP));
            setLayout(new BorderLayout());
            setMinimumSize(new Dimension(200, 200));
        }
    }

    //----------------//
    // FolderSelector // -------------------------------------------------------
    //----------------//
    private class FolderSelector
        extends Selector
    {
        public FolderSelector (ChangeListener listener)
        {
            super("Folder Selector", listener);
            load.setEnabled(true);
        }

        // Triggered by load button
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            list.removeAll();

            // First insert the dedicated icons folder
            list.add(Main.getIconsFolder().getName());

            // Then populate with all sorted existing sheets folders
            ArrayList<String> folders = new ArrayList<String>();

            for (File file : repository.getSheetDirectories()) {
                folders.add(file.getName());
            }

            Collections.sort(folders);

            for (String folder : folders) {
                list.add(folder);
            }

            updateCardinal();
        }
    }

    //---------------//
    // GlyphSelector // --------------------------------------------------------
    //---------------//
    private class GlyphSelector
        extends Selector
    {
        //---------------//
        // GlyphSelector //
        //---------------//
        public GlyphSelector (ChangeListener listener)
        {
            super("Glyph Selector", listener);
        }

        //-----------------//
        // actionPerformed // Triggered by the load button
        //-----------------//
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            String[]           folders = folderSelector.list.getSelectedItems();
            String[]           shapes = shapeSelector.list.getSelectedItems();
            Collection<String> shapeList = Arrays.asList(shapes);

            // Debug
            if (logger.isFineEnabled()) {
                logger.fine("Glyph Selector. Got Sheets:");

                for (String fName : folders) {
                    logger.fine(fName);
                }

                logger.fine("Glyph Selector. Got Shapes:");

                for (String shapeName : shapes) {
                    logger.fine(shapeName);
                }
            }

            if (shapes.length == 0) {
                logger.warning("No shapes selected in Shape Selector");
            } else {
                list.removeAll();

                // Populate with all possible glyphs, sorted by gName
                for (String folder : folders) {
                    // Add proper glyphs files from this directory
                    ArrayList<String> gNames = new ArrayList<String>();
                    File              dir = getActualDir(folder);

                    for (File file : repository.getGlyphsIn(dir)) {
                        String shapeName = radixOf(file.getName());

                        if (shapeList.contains(shapeName)) {
                            gNames.add(dir.getName() + "/" + file.getName());
                        }
                    }

                    Collections.sort(gNames);

                    for (String gName : gNames) {
                        list.add(gName);
                    }
                }

                updateCardinal();
            }
        }

        //--------//
        // delete //
        //--------//
        public void delete (String gName)
        {
            // Remove entry from list
            list.remove(gName);
        }
    }

    //---------------//
    // ShapeSelector // --------------------------------------------------------
    //---------------//
    private class ShapeSelector
        extends Selector
    {
        public ShapeSelector (ChangeListener listener)
        {
            super("Shape Selector", listener);
        }

        // Triggered by load button
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            // Populate with shape names found in selected sheets
            String[] sheetNames = folderSelector.list.getSelectedItems();

            if (sheetNames.length == 0) {
                logger.warning("No sheets selected in Sheet Selector");
            } else {
                // To avoid duplicates, and to get a sorted list
                SortedSet<String> shapeSet = new TreeSet<String>();

                for (String folder : folderSelector.list.getSelectedItems()) {
                    File dir = getActualDir(folder);

                    // Add all glyphs files from this directory
                    for (File file : repository.getGlyphsIn(dir)) {
                        shapeSet.add(radixOf(file.getName()));
                    }
                }

                populateWith(shapeSet);
            }
        }
    }
}
