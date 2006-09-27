//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h B r o w s e r                           //
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
import static omr.selection.SelectionTag.*;

import omr.ui.Board;
import omr.ui.field.SField;
import omr.ui.util.Panel;
import omr.ui.view.LogSlider;
import omr.ui.view.Rubber;
import omr.ui.view.Zoom;

import omr.util.Implement;
import omr.util.Logger;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>GlyphBrowser</code> gathers a navigator to move between selected
 * glyphs, a glyph board for glyph details, and a display for graphical glyph
 * lag view. This is a (package private) companion of {@link GlyphVerifier}.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
class GlyphBrowser
    extends JPanel
    implements ChangeListener
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger    logger = Logger.getLogger(
        GlyphBrowser.class);

    /**
     * Field constant <code>NO_INDEX</code> is a specific value {@value} to
     * indicate absence of index
     */
    private static final int NO_INDEX = -1;

    //~ Instance fields --------------------------------------------------------

    /** Reference of GlyphVerifier */
    private final GlyphVerifier verifier;

    /** Local pixel selection */
    private final Selection localPixelSelection = new Selection(PIXEL);

    /** Local section selection */
    private final Selection localSectionSelection = new Selection(
        HORIZONTAL_SECTION);

    /** Local run selection */
    private final Selection localRunSelection = new Selection(HORIZONTAL_RUN);

    /** Local glyph selection */
    private final Selection localGlyphSelection = new Selection(VERTICAL_GLYPH);

    /** Repository of known glyphs */
    private final GlyphRepository repository = GlyphRepository.getInstance();

    /** Size of the lag display */
    private Dimension modelSize;

    /** Contour of the lag display */
    private Rectangle modelRectangle;

    /** Composite display (view + zoom slider) */
    private Display display;

    /** Hosting GlyphLag */
    private GlyphLag vLag;

    /** The lag view */
    private GlyphLagView view;

    /** Population of glyphs file names */
    private List<String> names;

    /** Navigator instance to navigate through all glyphs names */
    private final Navigator navigator = new Navigator();

    /** Glyph board with ability to delete a training glyph */
    private GlyphBoard board;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // GlyphBrowser //
    //--------------//
    /**
     * Create an instance, with back-reference to GlyphVerifier
     *
     * @param verifier ref back to verifier
     */
    public GlyphBrowser (GlyphVerifier verifier)
    {
        this.verifier = verifier;

        // Layout
        setLayout(new BorderLayout());
        resetBrowser();
        add(buildLeftPanel(), BorderLayout.WEST);
    }

    //~ Methods ----------------------------------------------------------------

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

    //--------------//
    // stateChanged //
    //--------------//
    public void stateChanged (ChangeEvent e)
    {
        int selNb = verifier.getGlyphNames().length;
        navigator.loadAction.setEnabled(selNb > 0);
    }

    //----------------//
    // buildLeftPanel //
    //----------------//
    private JPanel buildLeftPanel ()
    {
        // Basic glyph model
        final GlyphModel glyphModel = new BasicGlyphModel();

        // Specific glyph board
        board = new GlyphBoard("TrainingBoard", glyphModel);
        board.setInputSelectionList(
            Collections.singletonList(localGlyphSelection));
        board.boardShown();
        board.getDeassignButton()
             .setToolTipText("Remove that glyph from training material");
        board.getDeassignButton()
             .setEnabled(false);

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
        int index = navigator.getIndex();

        if (index >= 0) {
            // Delete glyph designated by index
            String gName = names.get(index);
            Glyph  glyph = navigator.loadGlyph(gName);

            // User confirmation is required ?
            if (constants.confirmDeletions.getValue()) {
                if (JOptionPane.showConfirmDialog(
                    null,
                    "Delete glyph '" + gName + "' ?") != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Shrink names list
            names.remove(index);

            // Update model & display
            repository.removeGlyph(gName);

            for (GlyphSection section : glyph.getMembers()) {
                section.delete();
            }

            // Update the Glyph selector also !
            verifier.deleteGlyphName(gName);

            // Perform file deletion
            File file = new File(repository.getSheetsFolder(), gName);

            if (file.delete()) {
                logger.info("Glyph " + gName + " deleted");
            } else {
                logger.warning("Could not delete file " + file);
            }

            // Set new index ?
            if (index < names.size()) {
                navigator.setIndex(index, GLYPH_INIT); // Next
            } else {
                navigator.setIndex(index - 1, GLYPH_INIT); // Prev/None
            }
        } else {
            logger.warning("No selected glyph to delete!");
        }
    }

    //--------------//
    // resetBrowser //
    //--------------//
    private void resetBrowser ()
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

    //-----------------//
    // BasicGlyphModel // ------------------------------------------------------
    //-----------------//
    private class BasicGlyphModel
        extends GlyphModel
    {
        public BasicGlyphModel ()
        {
            super(null, vLag);
        }

        @Override
        public void deassignGlyphShape (Glyph glyph)
        {
            deleteGlyph(); // Using current glyph
        }
    }

    //---------//
    // Display // --------------------------------------------------------------
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
    // MyView // ---------------------------------------------------------------
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
            deleteGlyph(); // Current glyph
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        public void renderItems (Graphics g)
        {
            // Mark the current glyph
            int index = navigator.getIndex();

            if (index >= 0) {
                String gName = names.get(index);
                Glyph  glyph = navigator.loadGlyph(gName);
                g.setColor(Color.black);
                g.setXORMode(Color.darkGray);
                glyph.renderBoxArea(g, getZoom());
            }
        }

        //--------//
        // update //
        //--------//
        /**
         * Call-back triggered from (local) selection objects
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
            case PIXEL : // Triggered by mouse

                if (hint == LOCATION_INIT) {
                    Rectangle rect = (Rectangle) selection.getEntity();

                    if ((rect != null) &&
                        (rect.width == 0) &&
                        (rect.height == 0)) {
                        // Look for pointed glyph
                        navigator.setIndex(glyphLookup(rect), hint);
                    }
                }

                break;

            case VERTICAL_GLYPH : // Triggered by (local) lookup

                Glyph glyph = (Glyph) selection.getEntity();

                if (hint == GLYPH_INIT) {
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
        /**
         * Lookup for a glyph that is pointed by rectangle location. This is a
         * very specific glyph lookup, for which we cannot rely on GlyphLag
         * usual features. So we simply browse through the collection of glyphs
         * (names).
         *
         * @param rect location (upper left corner)
         * @return index in names collection if found, NO_INDEX otherwise
         */
        private int glyphLookup (Rectangle rect)
        {
            int index = -1;

            for (String gName : names) {
                index++;

                Glyph glyph = navigator.loadGlyph(gName);

                if (glyph.getLag() == vLag) {
                    for (GlyphSection section : glyph.getMembers()) {
                        // Swap x & y,  since this is a vertical lag
                        if (section.contains(rect.y, rect.x)) {
                            return index;
                        }
                    }
                }
            }

            return NO_INDEX; // Not found
        }
    }

    //-----------//
    // Navigator // ------------------------------------------------------------
    //-----------//
    /**
     * Class <code>Navigator</code> handles the navigation through the
     * collection of glyphs (names)
     */
    private final class Navigator
        extends Board
    {
        /** Current index in names collection (NO_INDEX if none) */
        private int nameIndex = NO_INDEX;

        // Navigation actions & buttons
        LoadAction loadAction = new LoadAction();
        JButton    load = new JButton(loadAction);
        JButton    all = new JButton("All");
        JButton    next = new JButton("Next");
        JButton    prev = new JButton("Prev");
        JTextField nameField = new SField(
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

                            setIndex(0, GLYPH_INIT); // To first
                        }
                    });

            prev.addActionListener(
                new ActionListener() {
                        public void actionPerformed (ActionEvent e)
                        {
                            setIndex(nameIndex - 1, GLYPH_INIT); // To prev
                        }
                    });

            next.addActionListener(
                new ActionListener() {
                        public void actionPerformed (ActionEvent e)
                        {
                            setIndex(nameIndex + 1, GLYPH_INIT); // To next
                        }
                    });

            load.setToolTipText("Load the selected glyphs");
            all.setToolTipText("Display all glyphs");
            prev.setToolTipText("Go to previous glyph");
            next.setToolTipText("Go to next glyph");

            loadAction.setEnabled(false);
            all.setEnabled(false);
            prev.setEnabled(false);
            next.setEnabled(false);
        }

        //----------//
        // setIndex //
        //----------//
        /**
         * Only method allowed to designate a glyph
         *
         * @param index index of new current glyph
         * @param hint related processing hint
         */
        public void setIndex (int           index,
                              SelectionHint hint)
        {
            nameIndex = index;

            Glyph glyph = null;

            if (index >= 0) {
                String gName = names.get(index);
                nameField.setText(gName);

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
                nameField.setText("");
            }

            localGlyphSelection.setEntity(glyph, hint);

            // Enable buttons according to glyph selection
            all.setEnabled(names.size() > 0);
            prev.setEnabled(index > 0);
            next.setEnabled((index >= 0) && (index < (names.size() - 1)));
        }

        //----------//
        // getIndex //
        //----------//
        /**
         * Report the current glyph index in the names collection
         *
         * @return the current index, which may be NO_INDEX
         */
        public final int getIndex ()
        {
            return nameIndex;
        }

        //-----------//
        // loadGlyph //
        //-----------//
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
            PanelBuilder    builder = new PanelBuilder(layout, getComponent());
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

            nameField.setHorizontalAlignment(JTextField.LEFT);
            builder.add(nameField, cst.xyw(3, r, 9));
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
                // Get a (shrinkable, to allow deletions) list of glyph names
                names = new ArrayList<String>(
                    Arrays.asList(verifier.getGlyphNames()));

                // Reset lag & display
                resetBrowser();

                // Set navigator on first glyph, if any
                if (names.size() > 0) {
                    setIndex(0, GLYPH_INIT);
                } else {
                    if (e != null) {
                        logger.warning("No glyphs selected in Glyph Selector");
                    }

                    all.setEnabled(false);
                    prev.setEnabled(false);
                    next.setEnabled(false);
                }
            }
        }
    }
}
