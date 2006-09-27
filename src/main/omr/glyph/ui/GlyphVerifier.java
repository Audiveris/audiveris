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
    private GlyphBrowser glyphBrowser;

    /** The panel in charge of the glyphs selection */
    private GlyphSelector glyphSelector = new GlyphSelector();

    /** The panel in charge of the shapes selection */
    private ShapeSelector shapeSelector = new ShapeSelector();

    /** The panel in charge of the sheets selection */
    private SheetSelector sheetSelector = new SheetSelector();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // GlyphVerifier //
    //---------------//
    /**
     * Create an instance of Glyph Verifier
     */
    private GlyphVerifier ()
    {
        glyphBrowser = new GlyphBrowser(this);

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

        // Sheets
        SortedSet<String> sheetSet = new TreeSet<String>();

        for (String gName : glyphNames) {
            File file = new File(gName);
            sheetSet.add(file.getParent());
        }

        sheetSelector.populateWith(sheetSet);
        sheetSelector.selectAll();
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
        builder.add(sheetSelector, cst.xy(1, r));
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
        implements ActionListener
    {
        protected JButton cancelAll = new JButton("Cancel All");
        protected JButton load = new JButton("Load");
        protected JButton selectAll = new JButton("Select All");
        protected JLabel  cardinal = new JLabel(
            "* No item selected *",
            SwingConstants.CENTER);
        protected List    list = new List(
            5, // nb of rows
            true); // multipleMode allowed ?

        public Selector (String title)
        {
            super(title);

            // Precise action to be specified in each subclass
            load.addActionListener(this);

            // To be informed of (de)selections
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

    //---------------//
    // GlyphSelector //
    //---------------//
    private class GlyphSelector
        extends Selector
    {
        //---------------//
        // GlyphSelector //
        //---------------//
        public GlyphSelector ()
        {
            super("Glyph Selector");
        }

        //-----------------//
        // actionPerformed //
        //-----------------//
        // Triggered by the load button
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            String[]           sheets = sheetSelector.list.getSelectedItems();
            String[]           shapes = shapeSelector.list.getSelectedItems();
            Collection<String> shapeList = Arrays.asList(shapes);

            // Debug
            if (logger.isFineEnabled()) {
                logger.fine("Glyph Selector. Got Sheets:");

                for (String fName : sheets) {
                    logger.fine(fName);
                }

                logger.fine("Glyph Selector. Got Shapes:");

                for (String shapeName : shapes) {
                    logger.fine(shapeName);
                }
            }

            // Populate with all possible glyphs
            list.removeAll();

            for (String sheetName : sheets) {
                File dir = new File(repository.getSheetsFolder(), sheetName);

                // Add proper glyphs files from this directory
                for (File file : repository.getSheetGlyphs(dir)) {
                    String shapeName = radixOf(file.getName());

                    if (shapeList.contains(shapeName)) {
                        list.add(dir.getName() + "/" + file.getName());
                    }
                }
            }

            updateCardinal();
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
    // ShapeSelector //
    //---------------//
    private class ShapeSelector
        extends Selector
    {
        public ShapeSelector ()
        {
            super("Shape Selector");
        }

        // Triggered by load button
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            // To avoid duplicates, and to get a sorted list
            SortedSet<String> shapeSet = new TreeSet<String>();

            // Populate with shape names found in selected sheets
            for (String sheetName : sheetSelector.list.getSelectedItems()) {
                File dir = new File(repository.getSheetsFolder(), sheetName);

                // Add all glyphs files from this directory
                for (File file : repository.getSheetGlyphs(dir)) {
                    shapeSet.add(radixOf(file.getName()));
                }
            }

            populateWith(shapeSet);
        }
    }

    //---------------//
    // SheetSelector //
    //---------------//
    private class SheetSelector
        extends Selector
    {
        public SheetSelector ()
        {
            super("Sheet Selector");
        }

        // Triggered by load button
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            // Populate with all existing sheets
            list.removeAll();

            for (File file : repository.getSheetDirectories()) {
                list.add(file.getName());
            }

            updateCardinal();
        }
    }
}
