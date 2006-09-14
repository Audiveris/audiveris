//-----------------------------------------------------------------------//
//                                                                       //
//                      S y m b o l s B u i l d e r                      //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphBuilder;
import omr.glyph.GlyphModel;
import omr.glyph.GlyphInspector;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphNetwork;
import omr.glyph.Shape;
import omr.lag.RunBoard;
import omr.lag.ScrollLagView;
import omr.lag.SectionBoard;
import omr.selection.SelectionHint;
import omr.selection.SelectionTag;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.ui.Board;
import omr.ui.BoardsPane;
import omr.ui.PixelBoard;
import omr.util.Logger;

import static omr.selection.SelectionTag.*;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Class <code>SymbolsBuilder</code> defines a UI pane from which all
 * symbol processing actions can be launched and checked.
 *
 * <dl>
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>VERTICAL_GLYPH (flagged with GLYPH_INIT hint)
 * </ul>
 * </dl>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SymbolsBuilder
    extends GlyphModel
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(SymbolsBuilder.class);

    /** Color for hiding unknown glyphs when filter is ON */
    public static final Color hiddenColor = Color.white;

    //~ Instance variables ------------------------------------------------

    // Repository of known glyphs
    private final GlyphRepository repository = GlyphRepository.getInstance();

    // Glyph builder
    private final GlyphBuilder builder;

    // Glyph inspector
    private final GlyphInspector inspector;

    // Sheet with Loaded glyphs
    private final Sheet sheet;

    // Related Lag view
    private final GlyphLagView view;

    // Popup menu related to glyph selection
    private final GlyphMenu glyphMenu;

    // Pointer to glyph board
    private final SymbolGlyphBoard glyphBoard;

    // The entity used for display focus
    private ShapeFocusBoard focus;

    //~ Constructors ------------------------------------------------------

    //----------------//
    // SymbolsBuilder //
    //----------------//
    /**
     * Create a view in the sheet assembly tabs, dedicated to the display
     * and handling of glyphs
     *
     * @param sheet the sheet whose glyphs are considered
     */
    public SymbolsBuilder(Sheet sheet)
    {
        super(sheet.getVerticalLag());

        this.sheet = sheet;

        // Allocation of components
        view = new MyView(lag);
        view.setLocationSelection(sheet.getSelection(SelectionTag.PIXEL));

        focus = new ShapeFocusBoard
            (sheet, view, this,
             new ActionListener() {
                 public void actionPerformed(ActionEvent e) {
                     view.colorizeAllGlyphs();
                 }
             });

        glyphMenu = new GlyphMenu(this, focus,
                sheet.getSelection(GLYPH_SET));

        glyphBoard = new SymbolGlyphBoard(this,
                                          sheet.getFirstSymbolId(),
                                          lag,
                                          sheet.getSelection(VERTICAL_GLYPH),
                                          sheet.getSelection(VERTICAL_GLYPH_ID),
                                          sheet.getSelection(GLYPH_SET));

        final String unit = "GlyphPane";
        BoardsPane boardsPane = new BoardsPane
            (sheet, view,
             new PixelBoard(unit),
             new RunBoard(unit,
                          sheet.getSelection(VERTICAL_RUN)),
             new SectionBoard(unit,
                              lag.getLastVertexId(),
                              sheet.getSelection(VERTICAL_SECTION),
                              sheet.getSelection(VERTICAL_SECTION_ID)),
             glyphBoard,
             new ActionsBoard(sheet, this),
             focus,
             new EvaluationBoard(sheet,
                                 this,
                                 true,  // useButtons
                                 view,
                                 sheet.getSelection(VERTICAL_GLYPH)));

        // Link with glyph builder & glyph inspector
        builder = sheet.getGlyphBuilder();
        inspector = sheet.getGlyphInspector();
        builder.setBoard(glyphBoard);

        // Create a hosting pane for the view
        ScrollLagView slv = new ScrollLagView(view);
        sheet.getAssembly().addViewTab("Glyphs", slv, boardsPane);
    }

    //~ Methods -----------------------------------------------------------

    //------------------//
    // assignGlyphShape //
    //------------------//
    @Override
        public void assignGlyphShape(Glyph glyph,
                                     Shape shape)
    {
        if (glyph != null) {
            // First, do assign the shape to the glyph
            glyph.setShape(shape);

            // Remember the latest shape assigned
            if (shape != null) {
                latestShapeAssigned = shape;
            }

            // Update immediately the glyph info as displayed
            sheet.getSelection(VERTICAL_GLYPH).setEntity
                (glyph,
                 SelectionHint.GLYPH_MODIFIED);
        }
    }

    //----------------//
    // assignSetShape //
    //----------------//
    @Override
        public void assignSetShape(List<Glyph> glyphs,
                                   Shape       shape,
                                   boolean     compound)
    {
        if (glyphs != null && glyphs.size() > 0) {
            if (compound) {
                // Build & insert a compound
                Glyph glyph = builder.buildCompound(glyphs);
                builder.insertCompound(glyph, glyphs);
                assignGlyphShape(glyph, shape);
            } else {
                int noiseNb = 0;
                for (Glyph glyph : glyphs) {
                    if (glyph.getShape() != Shape.NOISE) {
                        assignGlyphShape(glyph, shape);
                    } else {
                        noiseNb ++;
                    }
                }
                if (noiseNb > 0) {
                    logger.info(noiseNb + " noise glyphs skipped");
                }
            }
        }
    }

    //--------------------//
    // deassignGlyphShape //
    //--------------------//
    @Override
        public void deassignGlyphShape (Glyph glyph)
    {
        Shape shape = glyph.getShape();
        logger.info("Deassign a " + shape + " symbol");

        // Processing depends on shape at hand
        switch (shape) {
        case THICK_BAR_LINE :
        case THIN_BAR_LINE :
            sheet.getBarsBuilder().deassignBarGlyph(glyph);
            assignGlyphShape(glyph, null);
            break;

        case COMBINING_STEM :
            cancelStems(Collections.singletonList(glyph));
            break;

        default :
            assignGlyphShape(glyph, null);
            break;
        }
    }
    //------------------//
    // deassignSetShape //
    //------------------//
    @Override
        public void deassignSetShape(List<Glyph> glyphs)
    {
        // First phase, putting the stems apart
        List<Glyph> stems = new ArrayList<Glyph>();
        List<Glyph> glyphsCopy = new ArrayList(glyphs);
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

        view.colorizeAllGlyphs();  // TBI
    }

    //-------------//
    // cancelStems //
    //-------------//
    /**
     * Cancel one or several stems, turning them back to just a set of
     * sections, and rebuilding glyphs from their member sections together
     * with the neighbouring non-assigned sections
     *
     * @param stems a list of stems
     */
    public void cancelStems (List<Glyph> stems)
    {
        /**
         * To remove a stem, several infos need to be modified : shape from
         * COMBINING_STEM to null, result from STEM to null, and the Stem
         * must be removed from system list of stems.
         *
         * The stem glyph must be removed (as well as all other
         * non-recognized glyphs that are connected to the former stem)
         *
         * Then, re-glyph extraction from sections when everything is ready
         * (GlyphBuilder). Should work on a micro scale : just the former
         * stem and the neighboring (non-assigned) glyphs.
         */

        Set<SystemInfo> systems = new HashSet<SystemInfo>();
        for (Glyph stem : stems) {
            SystemInfo system = sheet.getSystemAtY(stem.getContourBox().y);
            inspector.removeGlyph(stem, system, /* cutSections => */ true);
            systems.add(system);
        }

        // Extract brand new glyphs from impacted systems
        for (SystemInfo system : systems) {
            inspector.extractNewSystemGlyphs(system);
        }

        // Update the UI
        refresh();
    }

    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the UI display (reset the model values of all spinners,
     * update the colors of the glyphs)
     */
    public void refresh ()
    {
        glyphBoard.resetSpinners();
        view.colorizeAllGlyphs();
    }

    //~ Methods private ---------------------------------------------------

    //---------------//
    // createMenuBar //
    //---------------//
    private JMenuBar createMenuBar()
    {
        // Menus in the frame
        JMenuBar menuBar = new JMenuBar();
        JMenuItem item;

        // Tools menu
        JMenu toolMenu = new JMenu("Tools");
        menuBar.add(toolMenu);

        // Focus mode
        JMenu focusMenu = new JMenu("Focus");
        Shape.addShapeItems
            (focusMenu,
             new ActionListener() {
                 public void actionPerformed(ActionEvent e) {
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

    //-----------//
    // MyLagView //
    //-----------//
    private class MyView
        extends GlyphLagView
    {
        //~ Constructors --------------------------------------------------

        private MyView (GlyphLag lag)
        {
            super(lag, null, SymbolsBuilder.this);
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

        //~ Methods -------------------------------------------------------

        //-------------//
        // renderItems //
        //-------------//
        @Override
            protected void renderItems (Graphics g)
        {
            // Render all sheet physical info known so far
            sheet.render(g, getZoom());

            // Normal display of selected items
            super.renderItems(g);
        }

        //-----------------//
        // contextSelected //
        //-----------------//
        @Override
            public void contextSelected (MouseEvent e,
                                         Point pt)
        {
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity();

            // To display point information
            if (glyphs == null || glyphs.size() == 0) {
                pointSelected(e, pt);
                glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // modified?
            }

            if (glyphs!= null && glyphs.size() > 0) {
                if (glyphs.size() == 1) {
                    glyphMenu.updateForGlyph(glyphs.get(0));
                } else if (glyphs.size() > 1) {
                    glyphMenu.updateForGlyphSet(glyphs);
                }
                // Show the popup menu
                glyphMenu.getPopup().show(this, e.getX(), e.getY());
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
            deassignGlyphShape(glyph);
            refresh();
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
            case ALL:
                super.colorizeGlyph(glyph);
                break;

            case KNOWN:
                if (glyph.isKnown()) {
                    super.colorizeGlyph(glyph);
                } else {
                    super.colorizeGlyph(glyph, hiddenColor);
                }
                break;

            case UNKNOWN:
                if (glyph.isKnown()) {
                    super.colorizeGlyph(glyph, hiddenColor);
                } else {
                    super.colorizeGlyph(glyph);
                }
                break;
            }
        }
    }
}
