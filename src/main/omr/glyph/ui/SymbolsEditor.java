//----------------------------------------------------------------------------//
//                                                                            //
//                         S y m b o l s E d i t o r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.Evaluator;
import omr.glyph.Glyph;
import omr.glyph.GlyphBuilder;
import omr.glyph.GlyphInspector;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphModel;
import omr.glyph.GlyphNetwork;
import omr.glyph.Shape;

import omr.lag.RunBoard;
import omr.lag.ScrollLagView;
import omr.lag.SectionBoard;

import omr.score.visitor.SheetPainter;

import omr.selection.SelectionTag;
import static omr.selection.SelectionTag.*;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.ui.BoardsPane;
import omr.ui.PixelBoard;

import omr.util.Logger;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

/**
 * Class <code>SymbolsEditor</code> defines a UI pane from which all symbol
 * processing actions can be launched and checked.
 *
 * <dl>
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>VERTICAL_GLYPH (flagged with GLYPH_INIT hint)
 * </ul>
 * </dl>
 *
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SymbolsEditor
    extends GlyphModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SymbolsEditor.class);

    /** Color for hiding unknown glyphs when filter is ON */
    public static final Color hiddenColor = Color.white;

    //~ Instance fields --------------------------------------------------------

    /** Evaluator to check for NOISE glyphs */
    private final Evaluator evaluator = GlyphNetwork.getInstance();

    /** Glyph builder */
    private final GlyphBuilder builder;

    /** Glyph inspector */
    private final GlyphInspector inspector;

    /** Related Lag view */
    private final GlyphLagView view;

    /** Popup menu related to glyph selection */
    private final GlyphMenu glyphMenu;

    /** Repository of known glyphs */
    private final GlyphRepository repository = GlyphRepository.getInstance();

    /** Pointer to glyph board */
    private final SymbolGlyphBoard glyphBoard;

    /** The entity used for display focus */
    private final ShapeFocusBoard focus;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // SymbolsEditor //
    //---------------//
    /**
     * Create a view in the sheet assembly tabs, dedicated to the display and
     * handling of glyphs
     *
     * @param sheet the sheet whose glyphs are considered
     */
    public SymbolsEditor (Sheet sheet)
    {
        super(sheet, sheet.getVerticalLag());

        // Allocation of components
        view = new MyView(lag);
        view.setLocationSelection(sheet.getSelection(SelectionTag.PIXEL));

        focus = new ShapeFocusBoard(
            sheet,
            view,
            this,
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        view.colorizeAllGlyphs();
                    }
                });

        glyphMenu = new GlyphMenu(this, focus, sheet.getSelection(GLYPH_SET));

        glyphBoard = new SymbolGlyphBoard(
            "Editor-SymbolGlyphBoard",
            this,
            sheet.getFirstSymbolId(),
            sheet.getSelection(VERTICAL_GLYPH),
            sheet.getSelection(VERTICAL_GLYPH_ID),
            sheet.getSelection(GLYPH_SET));

        final String unit = "GlyphPane";
        BoardsPane   boardsPane = new BoardsPane(
            sheet,
            view,
            new PixelBoard(unit),
            new RunBoard(unit, sheet.getSelection(VERTICAL_RUN)),
            new SectionBoard(
                unit,
                lag.getLastVertexId(),
                sheet.getSelection(VERTICAL_SECTION),
                sheet.getSelection(VERTICAL_SECTION_ID)),
            glyphBoard,
            new ActionsBoard(sheet, this),
            focus,
            new EvaluationBoard(
                "Evaluation-ActiveBoard",
                this,
                sheet.getSelection(VERTICAL_GLYPH),
                sheet,
                view));

        // Link with glyph builder & glyph inspector
        builder = sheet.getGlyphBuilder();
        inspector = sheet.getGlyphInspector();

        // Create a hosting pane for the view
        ScrollLagView slv = new ScrollLagView(view);
        sheet.getAssembly()
             .addViewTab("Glyphs", slv, boardsPane);
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // assignGlyphShape //
    //------------------//
    /**
     * Assign a Shape to a glyph, but preventing to assign a non-noise shape to
     * a noise glyph
     *
     * @param glyph the glyph to be assigned
     * @param shape the assigned shape, which may be null
     */
    @Override
    public void assignGlyphShape (Glyph glyph,
                                  Shape shape)
    {
        if (glyph != null) {
            if ((shape == Shape.NOISE) || evaluator.isBigEnough(glyph)) {
                super.assignGlyphShape(glyph, shape);
            } else {
                logger.warning(
                    "Attempt to assign " + shape + " to a tiny glyph");
            }
        }
    }

    //----------------//
    // assignSetShape //
    //----------------//
    /**
     * Assign a shape to a set of glyphs, either to each glyph individually, or
     * to a compound glyph built from the glyph set
     *
     * @param glyphs the collection of glyphs
     * @param shape the shape to be assigned
     * @param compound flag to indicate a compound is desired
     */
    @Override
    public void assignSetShape (List<Glyph> glyphs,
                                Shape       shape,
                                boolean     compound)
    {
        if ((glyphs != null) && (glyphs.size() > 0)) {
            if (compound) {
                // Build & insert a compound
                Glyph glyph = builder.buildCompound(glyphs);
                builder.insertCompound(glyph, glyphs);
                assignGlyphShape(glyph, shape);
            } else {
                int              noiseNb = 0;
                ArrayList<Glyph> glyphsCopy = new ArrayList<Glyph>(glyphs);

                for (Glyph glyph : glyphsCopy) {
                    if (glyph.getShape() != Shape.NOISE) {
                        assignGlyphShape(glyph, shape);
                    } else {
                        noiseNb++;
                    }
                }

                if (noiseNb > 0) {
                    logger.info(noiseNb + " noise glyphs skipped");
                }

                sheet.getSelection(SelectionTag.GLYPH_SET)
                     .setEntity(glyphsCopy, null);
            }
        }
    }

    //-------------//
    // cancelStems //
    //-------------//
    /**
     * Cancel one or several stems, turning them back to just a set of sections,
     * and rebuilding glyphs from their member sections together with the
     * neighbouring non-assigned sections
     *
     * @param stems a list of stems
     */
    public void cancelStems (List<Glyph> stems)
    {
        /**
         * To remove a stem, several infos need to be modified : shape from
         * COMBINING_STEM to null, result from STEM to null, and the Stem must
         * be removed from system list of stems.
         *
         * The stem glyph must be removed (as well as all other non-recognized
         * glyphs that are connected to the former stem)
         *
         * Then, re-glyph extraction from sections when everything is ready
         * (GlyphBuilder). Should work on a micro scale : just the former stem
         * and the neighboring (non-assigned) glyphs.
         */
        Set<SystemInfo> systems = new HashSet<SystemInfo>();

        for (Glyph stem : stems) {
            SystemInfo system = sheet.getSystemAtY(stem.getContourBox().y);
            inspector.removeGlyph(stem, system, /* cutSections => */
                                  true);
            assignGlyphShape(stem, null);
            systems.add(system);
        }

        // Extract brand new glyphs from impacted systems
        for (SystemInfo system : systems) {
            inspector.extractNewSystemGlyphs(system);
        }

        // Update the UI
        refresh();
    }

    //--------------------//
    // deassignGlyphShape //
    //--------------------//
    /**
     * De-assign the shape of a glyph
     *
     * @param glyph the glyph to deassign
     */
    @Override
    public void deassignGlyphShape (Glyph glyph)
    {
        Shape shape = glyph.getShape();

        // Processing depends on shape at hand
        switch (shape) {
        case THICK_BAR_LINE :
        case THIN_BAR_LINE :
            sheet.getBarsBuilder()
                 .deassignGlyphShape(glyph);

            break;

        case COMBINING_STEM :
            logger.info("Deassigning a Stem as glyph " + glyph.getId());
            cancelStems(Collections.singletonList(glyph));

            break;

        case NOISE :
            logger.info("Skipping Noise as glyph " + glyph.getId());

            break;

        default :
            logger.info(
                "Deassigning a " + shape + " symbol as glyph " + glyph.getId());
            assignGlyphShape(glyph, null);

            break;
        }
    }

    //------------------//
    // deassignSetShape //
    //------------------//
    /**
     * Deassign all the glyphs of the provided collection
     *
     * @param glyphs the collection of glyphs to deassign
     */
    @Override
    public void deassignSetShape (List<Glyph> glyphs)
    {
        // First phase, putting the stems apart
        List<Glyph> stems = new ArrayList<Glyph>();
        List<Glyph> glyphsCopy = new ArrayList<Glyph>(glyphs);

        for (Glyph glyph : glyphsCopy) {
            if (glyph.getShape() == Shape.COMBINING_STEM) {
                stems.add(glyph);
            } else if (glyph.isKnown()) {
                deassignGlyphShape(glyph);
            }
        }

        // Second phase dedicated to stems, if any
        if (stems.size() > 0) {
            cancelStems(stems);
        }

        view.colorizeAllGlyphs(); // TBI

        sheet.getSelection(SelectionTag.GLYPH_SET)
             .setEntity(glyphsCopy, null);
    }

    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the UI display (reset the model values of all spinners, update
     * the colors of the glyphs)
     */
    public void refresh ()
    {
        view.colorizeAllGlyphs();
    }

    //---------------//
    // createMenuBar //
    //---------------//
    private JMenuBar createMenuBar ()
    {
        // Menus in the frame
        JMenuBar  menuBar = new JMenuBar();
        JMenuItem item;

        // Tools menu
        JMenu toolMenu = new JMenu("Tools");
        menuBar.add(toolMenu);

        // Focus mode
        JMenu focusMenu = new JMenu("Focus");
        Shape.addShapeItems(
            focusMenu,
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        JMenuItem source = (JMenuItem) e.getSource();
                        focus.setCurrentShape(Shape.valueOf(source.getText()));
                    }
                });
        toolMenu.add(focusMenu);

        // Neural Network
        JMenu networkMenu = new JMenu("Network");
        toolMenu.add(networkMenu);

        return menuBar;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // MyLagView //
    //-----------//
    private class MyView
        extends GlyphLagView
    {
        private MyView (GlyphLag lag)
        {
            super(lag, null, SymbolsEditor.this);
            setName("GlyphPane-View");

            // Current glyph
            glyphSelection = sheet.getSelection(SelectionTag.VERTICAL_GLYPH);
            glyphSelection.addObserver(this);

            // Current glyph set
            glyphSetSelection = sheet.getSelection(SelectionTag.GLYPH_SET);
            glyphSetSelection.addObserver(this);

            // Use light gray color for past successful entities
            sheet.colorize(lag, viewIndex, Color.lightGray);
        }

        //-------------------//
        // colorizeAllGlyphs //
        //-------------------//
        /**
         * Colorize all the glyphs of the sheet
         */
        public void colorizeAllGlyphs ()
        {
            for (SystemInfo system : sheet.getSystems()) {
                for (Glyph glyph : system.getGlyphs()) {
                    colorizeGlyph(glyph);
                }
            }

            repaint();
        }

        //---------------//
        // colorizeGlyph //
        //---------------//
        public void colorizeGlyph (Glyph glyph)
        {
            switch (focus.getFilter()) {
            case ALL :
                super.colorizeGlyph(glyph);

                break;

            case KNOWN :

                if (glyph.isKnown()) {
                    super.colorizeGlyph(glyph);
                } else {
                    super.colorizeGlyph(glyph, hiddenColor);
                }

                break;

            case UNKNOWN :

                if (glyph.isKnown()) {
                    super.colorizeGlyph(glyph, hiddenColor);
                } else {
                    super.colorizeGlyph(glyph);
                }

                break;
            }
        }

        //-----------------//
        // contextSelected //
        //-----------------//
        @Override
        public void contextSelected (MouseEvent e,
                                     Point      pt)
        {
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // Compiler warning

            // To display point information
            if ((glyphs == null) || (glyphs.size() == 0)) {
                pointSelected(e, pt);
                glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // Compiler warning
            }

            if ((glyphs != null) && (glyphs.size() > 0)) {
                if (glyphs.size() == 1) {
                    glyphMenu.updateForGlyph(glyphs.get(0));
                } else if (glyphs.size() > 1) {
                    glyphMenu.updateForGlyphSet(glyphs);
                }

                // Show the popup menu
                glyphMenu.getPopup()
                         .show(this, e.getX(), e.getY());
            } else {
                // Popup with no glyph selected ?
            }
        }

        //---------------//
        // deassignGlyph //
        //---------------//
        @Override
        public void deassignGlyph (Glyph glyph)
        {
            super.deassignGlyph(glyph);
            refresh();
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        protected void renderItems (Graphics g)
        {
            // Render all sheet physical info known so far
            sheet.accept(new SheetPainter(g, getZoom()));

            // Normal display of selected items
            super.renderItems(g);
        }
    }
}
