//-----------------------------------------------------------------------//
//                                                                       //
//                           G l y p h P a n e                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphBuilder;
import omr.glyph.GlyphDirectory;
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
 * Class <code>GlyphPane</code> defines a UI pane from which all glyph
 * processing actions can be launched and checked.
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
public class GlyphPane
    implements GlyphDirectory
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(GlyphPane.class);

    //~ Instance variables ------------------------------------------------

    // Repository of known glyphs
    private final GlyphRepository repository = GlyphRepository.getInstance();

    // Glyph builder
    private final GlyphBuilder builder;

    // Glyph inspector
    private final GlyphInspector inspector;

    // Sheet with Loaded glyphs
    private final Sheet sheet;

    // Lag of vertical runs
    private final GlyphLag vLag;

    // Related Lag view
    private final GlyphLagView view;

    // Popup menu related to glyph selection
    private final GlyphMenu popup;

    // Pointer to related custom board
    private final Board customBoard;

    // Pointer to glyph board
    private final SymbolGlyphBoard glyphBoard;

    // The entity used for display focus
    private ShapeFocus focus;

    // Latest shape assigned if any
    private transient Shape latestShapeAssigned;

    //~ Constructors ------------------------------------------------------

    //-----------//
    // GlyphPane //
    //-----------//
    /**
     * Create a view in the sheet assembly tabs, dedicated to the display
     * and handling of glyphs
     *
     * @param sheet the sheet whose glyphs are considered
     */
    public GlyphPane(Sheet sheet)
    {
        this.sheet = sheet;
        vLag = sheet.getVerticalLag();

        // Allocation of components
        view = new MyView();
        view.setLocationSelection(sheet.getSelection(SelectionTag.PIXEL));

        focus = new ShapeFocus(sheet, view, this);
        popup = new GlyphMenu(this, focus,
                sheet.getSelection(GLYPH_SET));

        // The UI combo
        customBoard = new CustomBoard("GlyphPane Custom Board");
        customBoard.getComponent().setVisible(true);

        glyphBoard = new SymbolGlyphBoard(this,
                                          sheet.getFirstSymbolId(),
                                          vLag,
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
                              vLag.getLastVertexId(),
                              sheet.getSelection(VERTICAL_SECTION),
                              sheet.getSelection(VERTICAL_SECTION_ID)),
             glyphBoard,
             new ActionsBoard(sheet, this),
             customBoard,
             new EvaluationBoard(sheet,
                                 this,
                                 view,
                                 sheet.getSelection(VERTICAL_GLYPH)));

        // Link with glyph builder & glyph inspector
        builder = sheet.getGlyphBuilder();
        inspector = sheet.getGlyphInspector();
        builder.setBoard(glyphBoard);

        // Create a hosting pane for the view
        ScrollLagView slv = new ScrollLagView(view);
        sheet.getAssembly().addViewTab("Glyphs", slv, boardsPane);

        // First evaluation -> NOISEs
        GlyphNetwork.getInstance().guessSheet
            (sheet, GlyphInspector.getSymbolMaxGrade());
    }

    //~ Methods -----------------------------------------------------------

    //-------------//
    // assignShape //
    //-------------//
    /**
     * Assign a shape to a glyph or a collection of glyphs. If "asGuessed"
     * is true, this assignment is performed only if the guess of the glyph
     * at hand corresponds to the shape to be assigned.
     *
     * <p>The concerned glyphs are contained in the Glyph Set selection
     *
     * @param shape the shape to be assigned
     * @param asGuessed flag to restrain assignment
     * @param compound flag to build one compound, rather than assign each
     * individual
     */
    public void assignShape(Shape shape,
                            boolean asGuessed,
                            boolean compound)
    {
        List<Glyph> currentGlyphs
            = (List<Glyph>) sheet.getSelection(GLYPH_SET).getEntity();

        if (currentGlyphs.size() > 0) {
            if (asGuessed) {            // Confirmation
                for (Glyph glyph : currentGlyphs) {
                    if (glyph.getGuess() == shape) {
                        setShape(glyph, shape, /* updateUI => */ false);
                    }
                }
                focus.setCurrent(shape);
            } else {                    // Forcing
                if (compound) {
                    // Build & insert a compound
                    Glyph glyph = builder.buildCompound(currentGlyphs);
                    builder.insertCompound(glyph, currentGlyphs);
                    setShape(glyph, shape, /* updateUI => */ false);
                    // Update (new) glyph contours
                    currentGlyphs.clear();
                    currentGlyphs.add(glyph);
                } else {
                    int noiseNb = 0;
                    for (Glyph glyph : currentGlyphs) {
                        if (glyph.getShape() != Shape.NOISE) {
                            setShape(glyph, shape, /* updateUI => */ true);
                        } else {
                            noiseNb ++;
                        }
                    }
                    if (noiseNb > 0) {
                        logger.info(noiseNb + " noise glyphs skipped");
                    }
                }
                refresh();
            }
        }
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

    //-----------//
    // getEntity //
    //-----------//
    /**
     * Retrieve a glyph knowing its id
     *
     * @param id the glyph id
     * @return the corresponding glyph, or null if not found
     */
    public Glyph getEntity(Integer id)
    {
        return vLag.getGlyph(id);
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
        focus.colorizeAllGlyphs();
    }

    //~ Methods package private -------------------------------------------

    //--------------//
    // getGlyphMenu //
    //--------------//
    /**
     * Give access to the GlyphMenu, in order to customize its content
     *
     * @return the GlyphMenu
     */
    GlyphMenu getGlyphMenu()
    {
        return popup;
    }

    //----------//
    // setShape //
    //----------//
    /**
     * Actually assign a {@link omr.glyph.Shape} to a glyph, update the
     * data model of the various spinners accordingly, and update the glyph
     * board info.
     *
     * @param glyph the glyph impacted
     * @param shape  the assigned shape
     * @param updateUI should the user interface be updated ?
     */
    public void setShape(Glyph glyph,
                         Shape shape,
                         boolean updateUI)
    {
        glyph.setShape(shape);

        // Remember the latest shape assigned
        if (shape != null) {
            latestShapeAssigned = shape;
        }

        // Update spinners models
        int id = glyph.getId();
        if (glyph.isKnown()) {
            glyphBoard.assignGlyph(id);
        } else {
            glyphBoard.deassignGlyph(id);
        }

        if (updateUI) {
            // Update immediately the glyph info as displayed
            sheet.getSelection(VERTICAL_GLYPH).setEntity
                    (glyph,
                    SelectionHint.GLYPH_INIT);

            // And the color of the glyph as well
            if (shape != null) {
                focus.setCurrent(shape);
            } else {
                glyph.setGuess(null);
            }
        }
    }

    //------------------------//
    // getLatestShapeAssigned //
    //------------------------//
    /**
     * Report the latest non null shape that was assigned, or null if none
     *
     * @return latest shape assigned, or null if none
     */
    Shape getLatestShapeAssigned()
    {
        return latestShapeAssigned;
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
                     focus.setCurrent(Shape.valueOf(source.getText()));
                 }
             });
        toolMenu.add(focusMenu);

        // Neural Network
        JMenu networkMenu = new JMenu("Network");
        toolMenu.add(networkMenu);

//         // Add a record action
//         item = networkMenu.add(recordAction);
//         item.setToolTipText("Record known glyphs");

        return menuBar;
    }

    //-------------//
    // CustomBoard // -----------------------------------------------------
    //-------------//
    private class CustomBoard
        extends Board
    {
        public CustomBoard(String name)
        {
            super(Board.Tag.CUSTOM, name);

            FormLayout layout = new FormLayout
                ("pref",
                 "pref");

            PanelBuilder builder = new PanelBuilder(layout, getComponent());
            builder.setDefaultDialogBorder();

            CellConstraints cst = new CellConstraints();

            int r = 1;                  // --------------------------------
            builder.add(focus, cst.xy (1, r));
        }
    }

    //-----------//
    // MyLagView //
    //-----------//
    private class MyView
        extends GlyphLagView
    {
        //~ Constructors --------------------------------------------------

        private MyView ()
        {
            super(vLag, null, GlyphPane.this);
            setName("GlyphPane-View");

            // Current glyphs
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
            if (glyphs.size() == 0) {
                pointSelected(e, pt);
                glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // modified?
            }

            if (glyphs.size() > 0) {
                GlyphMenu menu = getGlyphMenu();
                if (glyphs.size() == 1) {
                    menu.updateForGlyph(glyphs.get(0));
                } else if (glyphs.size() > 1) {
                    menu.updateForGlyphs(glyphs);
                }
                // Show the popup menu
                menu.getPopup().show(this, e.getX(), e.getY());
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
            Shape shape = glyph.getShape();
            logger.info("Deassign a " + shape + " symbol");

            // Processing depends on shape at hand
            switch (shape) {
            case THICK_BAR_LINE :
            case THIN_BAR_LINE :
                sheet.getBarsBuilder().deassignBarGlyph(glyph);
                setShape(glyph, null, /* UpdateUI => */ true);
                refresh();
                break;

            case COMBINING_STEM :
                cancelStems(Collections.singletonList(glyph));
                break;

            default :
                setShape(glyph, null, /* UpdateUI => */ true);
                refresh();
                break;
            }
        }
    }
}
