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

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphModel;
import omr.glyph.GlyphSection;

import omr.lag.ScrollLagView;
import omr.lag.VerticalOrientation;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import static omr.selection.SelectionHint.*;
import omr.selection.SelectionTag;

import omr.ui.Board;
import omr.ui.field.SField;
import omr.ui.util.Panel;
import omr.ui.util.UILookAndFeel;
import omr.ui.view.LogSlider;
import omr.ui.view.Rubber;
import omr.ui.view.Zoom;

import omr.util.Implement;
import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

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

    private static final Constants constants = new Constants();
    private static final Logger    logger = Logger.getLogger(
        GlyphVerifier.class);

    /** To differentiate the exit action */
    private static boolean standAlone = false;

    /** The unique instance */
    private static GlyphVerifier INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** Local glyph selection */
    private Selection localGlyphSelection = new Selection(
        SelectionTag.TRAINING_GLYPH);

    /** Local section selection */
    private Selection localSectionSelection = new Selection(
        SelectionTag.HORIZONTAL_SECTION);

    /** Local run selection */
    private Selection localRunSelection = new Selection(
        SelectionTag.HORIZONTAL_RUN);

    /** Local pixel selection */
    private Selection localPixelSelection = new Selection(SelectionTag.PIXEL);

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
        frame = new JFrame();
        frame.setTitle("Glyph Verifier");

        glyphBrowser = new GlyphBrowser();

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

    //----------//
    // getFrame //
    //----------//
    /**
     * Report the UI frame of glyph verifier
     *
     * @return the related frame
     */
    public JFrame getFrame ()
    {
        return frame;
    }

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

    //----------------//
    // dumpSelections //
    //----------------//
    /**
     * Dump the current state of all (local) selections
     */
    public void dumpSelections ()
    {
        System.out.println("\nSelections for GlyphVerifier :");

        localPixelSelection.dump();
        localGlyphSelection.dump();
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

        // Browser
        glyphBrowser.navigator.loadAction.actionPerformed(null);
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

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
        extends ConstantSet
    {
        Constant.Boolean confirmDeletions = new Constant.Boolean(
            true,
            "Should user confirm each glyph deletion" +
            " from training material");

        Constants ()
        {
            initialize();
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

    //----------//
    // Selector //
    //----------//
    /**
     * Class <code>Selector</code> defines the common properties of sheet, shape
     * and glyph selectors. Each selector is made of a list of names, which can
     * be selected and deselected at will.
     */
    private abstract class Selector
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

    //--------------//
    // GlyphBrowser //
    //--------------//
    /**
     * Class <code>GlyphBrowser</code> gathers a navigator to move between
     * selected glyphs, a glyph board for glyph details, and a display for
     * graphical glyph lag view
     */
    private class GlyphBrowser
        extends JPanel
    {
        Dimension    modelSize;
        Display      display;

        // Glyph designated by simple mouse pointing. This is just a trick to
        // highlight the glyph. See index variable instead.
        Glyph        pointedGlyph;

        // Hosting GlyphLag
        GlyphLag     vLag;
        GlyphLagView view;
        Navigator    navigator = new Navigator();
        Rectangle    modelRectangle;
        GlyphBoard   board;

        // Array of glyphs file names
        String[] names;

        // This defines the current glyph, designated by whatever means. It is
        // an index in the 'names' array.
        int glyphIndex;

        //--------------//
        // GlyphBrowser //
        //--------------//
        GlyphBrowser ()
        {
            // Layout
            setLayout(new BorderLayout());
            resetBrowser();
            add(getLeftPanel(), BorderLayout.WEST);
        }

        //--------------//
        // resetBrowser //
        //--------------//
        public void resetBrowser ()
        {
            if (vLag != null) {
                // Placeholder for some cleanup
                localPixelSelection.deleteObserver(vLag);
                localSectionSelection.deleteObserver(vLag);
            }

            // Reset model
            vLag = new GlyphLag(new VerticalOrientation());
            vLag.setName("TrainingLag");
            vLag.setSectionSelection(localSectionSelection);
            localSectionSelection.addObserver(vLag);
            vLag.setRunSelection(localRunSelection);

            // Input
            localPixelSelection.addObserver(vLag);

            // Output
            vLag.setLocationSelection(localPixelSelection);
            vLag.setGlyphSelection(localGlyphSelection);

            // Reset display
            if (display != null) {
                remove(display);
            }

            display = new Display();
            add(display, BorderLayout.CENTER);
        }

        //--------------//
        // getLeftPanel //
        //--------------//
        private JPanel getLeftPanel ()
        {
            // Basic glyph model
            GlyphModel glyphModel = new BasicGlyphModel();

            // Specific glyph board
            board = new GlyphBoard("TrainingBoard", glyphModel);
            board.setInputSelectionList(
                Collections.singletonList(localGlyphSelection));
            board.boardShown();
            board.getDeassignButton()
                 .setToolTipText("Remove that glyph from training material");

            FormLayout      layout = new FormLayout("pref", "pref,pref,pref");
            PanelBuilder    builder = new PanelBuilder(layout);
            CellConstraints cst = new CellConstraints();
            builder.setDefaultDialogBorder();

            builder.add(navigator.getComponent(), cst.xy(1, 1));
            builder.add(board.getComponent(), cst.xy(1, 2));

            return builder.getPanel();
        }

        //-------------//
        // deleteGlyph //
        //-------------//
        private void deleteGlyph ()
        {
            // Delete glyph designated by glyphIndex
            String gName = names[glyphIndex];
            Glyph  glyph = repository.getGlyph(gName);

            // User confirmation is required ?
            if (constants.confirmDeletions.getValue()) {
                if (JOptionPane.showConfirmDialog(
                    null,
                    "Delete glyph '" + gName + "' ?") != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Shrink names array
            String[] old = names;
            names = new String[old.length - 1];
            System.arraycopy(old, 0, names, 0, glyphIndex);
            System.arraycopy(
                old,
                glyphIndex + 1,
                names,
                glyphIndex,
                old.length - glyphIndex - 1);

            // Update model & display
            repository.removeGlyph(gName);

            for (GlyphSection section : glyph.getMembers()) {
                section.delete();
            }

            // Update the Glyph selector also !
            glyphSelector.delete(gName);

            // Set next index ?
            if (glyphIndex < names.length) {
                navigator.setIndex(glyphIndex, GLYPH_INIT); // Next
            } else {
                glyphIndex--;
                navigator.setIndex(glyphIndex, GLYPH_INIT); // Prev/Reset
            }

            // Perform file deletion
            File file = new File(repository.getSheetsFolder(), gName);

            if (file.delete()) {
                logger.info("Glyph " + gName + " deleted");
            } else {
                logger.warning("Could not delete file " + file);
            }
        }

        //---------//
        // Display //
        //---------//
        private class Display
            extends JPanel
        {
            LogSlider     slider;
            Rubber        rubber;
            ScrollLagView slv;
            Zoom          zoom;

            Display ()
            {
                if (view != null) {
                    localPixelSelection.deleteObserver(view);
                }

                view = new MyView();
                modelRectangle = new Rectangle();
                modelSize = new Dimension(0, 0);
                slider = new LogSlider(2, 5, LogSlider.VERTICAL, -3, 4, 0);
                zoom = new Zoom(slider, 1); // Default ratio set to 1
                rubber = new Rubber(view, zoom);
                rubber.setMouseMonitor(view);
                view.setZoom(zoom);
                view.setRubber(rubber);
                slv = new ScrollLagView(view);

                // Layout
                setLayout(new BorderLayout());
                add(slider, BorderLayout.WEST);
                add(slv.getComponent(), BorderLayout.CENTER);
            }
        }

        //--------//
        // MyView //
        //--------//
        private class MyView
            extends GlyphLagView
        {
            public MyView ()
            {
                super(vLag, null, null);

                setName("GlyphVerifier-View");

                setLocationSelection(localPixelSelection);
                localPixelSelection.addObserver(this);
                localGlyphSelection.addObserver(this);
            }

            //---------------//
            // deassignGlyph //
            //---------------//
            @Override
            public void deassignGlyph (Glyph glyph)
            {
                deleteGlyph();          // Current glyph
            }

            //-------------//
            // renderItems //
            //-------------//
            @Override
            public void renderItems (Graphics g)
            {
                Zoom z = getZoom();

                // Mark the current glyph
                if (pointedGlyph != null) {
                    g.setColor(Color.black);
                    g.setXORMode(Color.darkGray);
                    pointedGlyph.renderBoxArea(g, z);
                }
            }

            //--------//
            // update //
            //--------//
            /**
             * Call-back triggered on selection notification.  We forward glyph
             * information.
             *
             * @param selection the notified Selection
             * @param hint potential notification hint
             */
            @Override
            public void update (Selection     selection,
                                SelectionHint hint)
            {
                // Keep normal view behavior (rubber, etc...)
                super.update(selection, hint);

                // Additional tasks
                switch (selection.getTag()) {
                case PIXEL :

                    if ((hint == LOCATION_ADD) || (hint == LOCATION_INIT)) {
                        Rectangle rect = (Rectangle) selection.getEntity();

                        if (rect != null) {
                            if ((rect.width > 0) || (rect.height > 0)) {
                                // Look for enclosed glyphs
                            } else {
                                Glyph glyph = glyphLookup(rect);
                                localGlyphSelection.setEntity(glyph, hint);
                            }
                        }
                    }

                    break;

                case TRAINING_GLYPH :

                    Glyph glyph = (Glyph) selection.getEntity();

                    if ((hint == GLYPH_INIT) || (hint == GLYPH_MODIFIED)) {
                        // Display glyph contour
                        if (glyph != null) {
                            locationSelection.setEntity(
                                glyph.getContourBox(),
                                hint);
                        }
                    }

                    break;

                default :
                }
            }

            //-------------//
            // glyphLookup //
            //-------------//
            private Glyph glyphLookup (Rectangle rect)
            {
                // Brute force
                if (names != null) {
                    for (int i = 0; i < names.length; i++) {
                        String gName = names[i];
                        Glyph  glyph = repository.getGlyph(gName);

                        if (glyph.getLag() == vLag) {
                            for (GlyphSection section : glyph.getMembers()) {
                                // Swap of x & y,  this is a vertical lag
                                if (section.contains(rect.y, rect.x)) {
                                    pointedGlyph = glyph;

                                    return glyph;
                                }
                            }
                        }
                    }
                }

                pointedGlyph = null;

                return null;
            }
        }

        //-----------//
        // Navigator //
        //-----------//
        private class Navigator
            extends Board
        {
            LoadAction loadAction = new LoadAction();
            JButton    load = new JButton(loadAction);
            JButton    all = new JButton("All");
            JButton    next = new JButton("Next");
            JButton    prev = new JButton("Prev");
            JTextField name = new SField(
                false, // editable
                "File where glyph is stored");

            //-----------//
            // Navigator //
            //-----------//
            Navigator ()
            {
                super(Board.Tag.CUSTOM, "Glyph-Navigator");

                defineLayout();

                all.addActionListener(
                    new ActionListener() {
                            public void actionPerformed (ActionEvent e)
                            {
                                for (String gName : names) {
                                    loadGlyph(gName);
                                }

                                if (names.length > 0) {
                                    setIndex(0, GLYPH_INIT);
                                }
                            }
                        });

                prev.addActionListener(
                    new ActionListener() {
                            public void actionPerformed (ActionEvent e)
                            {
                                setIndex(glyphIndex - 1, GLYPH_INIT);
                            }
                        });

                next.addActionListener(
                    new ActionListener() {
                            public void actionPerformed (ActionEvent e)
                            {
                                setIndex(glyphIndex + 1, GLYPH_INIT);
                            }
                        });

                load.setToolTipText("Load the selected glyphs");
                all.setToolTipText("Display all glyphs");
                prev.setToolTipText("Go to previous glyph");
                next.setToolTipText("Go to next glyph");

                all.setEnabled(false);
                prev.setEnabled(false);
                next.setEnabled(false);
            }

            //----------//
            // setIndex //
            //----------//
            public void setIndex (int glyphIndex,
                                  SelectionHint hint)
            {
                GlyphBrowser.this.glyphIndex = glyphIndex;
                pointedGlyph = null;

                Glyph glyph = null;

                if (glyphIndex >= 0) {
                    String gName = names[glyphIndex];
                    name.setText(gName);

                    // Load the glyph if needed
                    glyph = loadGlyph(gName);

                    // Extend view model size if needed
                    Rectangle box = glyph.getContourBox();
                    modelRectangle = modelRectangle.union(box);

                    Dimension newSize = modelRectangle.getSize();

                    if (!newSize.equals(modelSize)) {
                        modelSize = newSize;
                        view.setModelSize(modelSize);
                    }
                } else {
                    name.setText("");
                }

                localGlyphSelection.setEntity(glyph, hint);

                // Update buttons
                all.setEnabled(names.length > 0);
                prev.setEnabled(glyphIndex > 0);
                next.setEnabled(glyphIndex < (names.length - 1));
            }

            public Glyph loadGlyph (String gName)
            {
                Glyph glyph = repository.getGlyph(gName);

                if (glyph.getLag() != vLag) {
                    glyph.setLag(vLag);

                    for (GlyphSection section : glyph.getMembers()) {
                        section.getViews()
                               .clear();
                        vLag.addVertex(section); // Trick!
                        section.setGraph(vLag);
                        section.complete();
                    }

                    view.colorizeGlyph(glyph);
                }

                return glyph;
            }

            //--------------//
            // defineLayout //
            //--------------//
            private void defineLayout ()
            {
                CellConstraints cst = new CellConstraints();
                FormLayout      layout = Panel.makeFormLayout(4, 3);
                PanelBuilder    builder = new PanelBuilder(
                    layout,
                    getComponent());
                builder.setDefaultDialogBorder();

                int r = 1; // --------------------------------
                builder.addSeparator("Navigator", cst.xyw(1, r, 9));
                builder.add(load, cst.xy(11, r));

                r += 2; // --------------------------------
                builder.add(all, cst.xy(3, r));
                builder.add(prev, cst.xy(7, r));
                builder.add(next, cst.xy(11, r));

                r += 2; // --------------------------------

                JLabel file = new JLabel("File", SwingConstants.RIGHT);
                builder.add(file, cst.xy(1, r));

                name.setHorizontalAlignment(JTextField.LEFT);
                builder.add(name, cst.xyw(3, r, 9));
            }

            //------------//
            // LoadAction //
            //------------//
            private class LoadAction
                extends AbstractAction
            {
                public LoadAction ()
                {
                    super("Load");
                }

                @Implement(ActionListener.class)
                public void actionPerformed (ActionEvent e)
                {
                    names = glyphSelector.list.getSelectedItems();

                    prev.setEnabled(false);
                    next.setEnabled(false);

                    // Reset lag & display
                    resetBrowser();

                    // Set navigator on first glyph, if any
                    if (names.length > 0) {
                        setIndex(0, GLYPH_INIT);
                    }
                }
            }
        }

        //-----------------//
        // BasicGlyphModel //
        //-----------------//
        private class BasicGlyphModel
            extends GlyphModel
        {
            public BasicGlyphModel()
            {
                super(null, vLag);
            }

            @Override
                public void deassignGlyphShape (Glyph glyph)
            {
                deleteGlyph();          // Current glyph
            }
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
