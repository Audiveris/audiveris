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
import java.io.File;
import java.util.Collections;

import javax.swing.*;

/**
 * Class <code>GlyphBrowser</code> gathers a navigator to move between
 * selected glyphs, a glyph board for glyph details, and a display for
 * graphical glyph lag view
 */
public class GlyphBrowser
    extends JPanel
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger    logger = Logger.getLogger(
        GlyphBrowser.class);

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
    private final Selection localGlyphSelection = new Selection(TRAINING_GLYPH);

    /** Repository of known glyphs */
    private final GlyphRepository repository = GlyphRepository.getInstance();

    // Comment ??
    private Dimension    modelSize;
    private Display      display;

    /** Glyph designated by simple mouse pointing. This is just a trick to
       highlight the glyph. See index variable instead */
    private Glyph pointedGlyph;

    /** Hosting GlyphLag */
    private GlyphLag vLag;
    private GlyphLagView view;
    private Navigator    navigator = new Navigator();
    private Rectangle    modelRectangle;
    private GlyphBoard   board;

    /** Array of glyphs file names */
    private String[] names;

    /** This defines the current glyph, designated by whatever means. It is an
       index in the 'names' array. */
    private int nameIndex;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // GlyphBrowser //
    //--------------//
    GlyphBrowser (GlyphVerifier verifier)
    {
        this.verifier = verifier;
        // Layout
        setLayout(new BorderLayout());
        resetBrowser();
        add(getLeftPanel(), BorderLayout.WEST);
        navigator.loadAction.actionPerformed(null);
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
        // Delete glyph designated by nameIndex
        String gName = names[nameIndex];
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
        System.arraycopy(old, 0, names, 0, nameIndex);
        System.arraycopy(
            old,
            nameIndex + 1,
            names,
            nameIndex,
            old.length - nameIndex - 1);

        // Update model & display
        repository.removeGlyph(gName);

        for (GlyphSection section : glyph.getMembers()) {
            section.delete();
        }

        // Update the Glyph selector also !
        verifier.deleteGlyphName(gName);

        // Set next index ?
        if (nameIndex < names.length) {
            navigator.setIndex(nameIndex, GLYPH_INIT); // Next
        } else {
            nameIndex--;
            navigator.setIndex(nameIndex, GLYPH_INIT); // Prev/Reset
        }

        // Perform file deletion
        File file = new File(repository.getSheetsFolder(), gName);

        if (file.delete()) {
            logger.info("Glyph " + gName + " deleted");
        } else {
            logger.warning("Could not delete file " + file);
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

    //-----------------//
    // BasicGlyphModel //
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
            deleteGlyph(); // Current glyph
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
            deleteGlyph(); // Current glyph
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
            case PIXEL : // Triggered by mouse

                if ((hint == LOCATION_ADD) || (hint == LOCATION_INIT)) {
                    Rectangle rect = (Rectangle) selection.getEntity();

                    if (rect != null) {
                        if ((rect.width > 0) || (rect.height > 0)) {
                            // Look for rectangle enclosed glyphs TBD
                        } else {
                            Glyph glyph = glyphLookup(rect);
                            localGlyphSelection.setEntity(glyph, hint);
                        }
                    }
                }

                break;

            case TRAINING_GLYPH : // Triggered by (local) lookup

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
                                nameIndex = i;
                                navigator.enableButtons();

                                return glyph;
                            }
                        }
                    }
                }
            }

            pointedGlyph = null;
            nameIndex = 0;

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
                            setIndex(nameIndex - 1, GLYPH_INIT);
                        }
                    });

            next.addActionListener(
                new ActionListener() {
                        public void actionPerformed (ActionEvent e)
                        {
                            setIndex(nameIndex + 1, GLYPH_INIT);
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
        public void setIndex (int           glyphIndex,
                              SelectionHint hint)
        {
            nameIndex = glyphIndex;
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
            enableButtons();
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

            name.setHorizontalAlignment(JTextField.LEFT);
            builder.add(name, cst.xyw(3, r, 9));
        }

        //---------------//
        // enableButtons //
        //---------------//
        private void enableButtons ()
        {
            all.setEnabled(names.length > 0);
            prev.setEnabled(nameIndex > 0);
            next.setEnabled(nameIndex < (names.length - 1));
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
                names = verifier.getGlyphNames();
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
}
