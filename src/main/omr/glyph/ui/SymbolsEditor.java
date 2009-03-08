//----------------------------------------------------------------------------//
//                                                                            //
//                         S y m b o l s E d i t o r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.Evaluator;
import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphNetwork;
import omr.glyph.ui.SymbolsController;

import omr.lag.ui.RunBoard;
import omr.lag.ui.ScrollLagView;
import omr.lag.ui.SectionBoard;

import omr.log.Logger;

import omr.score.visitor.SheetPainter;

import omr.selection.GlyphEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.PixelBoard;

import omr.step.Step;

import omr.ui.BoardsPane;

import omr.util.Implement;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * Class <code>SymbolsEditor</code> defines a UI pane from which all symbol
 * processing actions can be launched and their results checked.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SymbolsEditor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SymbolsEditor.class);

    /** Color for hiding unknown glyphs when filter is ON */
    public static final Color hiddenColor = Color.white;

    //~ Instance fields --------------------------------------------------------

    /** Related instance of symbols builder */
    private final SymbolsController symbolsBuilder;

    /** Related sheet */
    private final Sheet sheet;

    /** Evaluator to check for NOISE glyphs */
    private final Evaluator evaluator = GlyphNetwork.getInstance();

    /** Related Lag view */
    private final GlyphLagView view;

    /** Popup menu related to glyph selection */
    private GlyphMenu glyphMenu;

    /** The entity used for display focus */
    private ShapeFocusBoard focus;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // SymbolsEditor //
    //---------------//
    /**
     * Create a view in the sheet assembly tabs, dedicated to the display and
     * handling of glyphs
     *
     * @param sheet the sheet whose glyphs are considered
     * @param symbolsModel the symbols model for this sheet
     */
    public SymbolsEditor (Sheet             sheet,
                          SymbolsController symbolsModel)
    {
        this.sheet = sheet;
        this.symbolsBuilder = symbolsModel;

        GlyphLag lag = symbolsModel.getLag();

        view = new MyView(lag);
        view.setLocationService(
            sheet.getSelectionService(),
            SheetLocationEvent.class);

        focus = new ShapeFocusBoard(
            sheet,
            view,
            symbolsModel,
            new ActionListener() {
                    @Implement(ActionListener.class)
                    public void actionPerformed (ActionEvent e)
                    {
                        view.colorizeAllGlyphs();
                    }
                });

        glyphMenu = new GlyphMenu(sheet, symbolsModel, evaluator, focus, lag);

        final String  unit = sheet.getRadix() + ":SymbolsEditor";

        BoardsPane    boardsPane = new BoardsPane(
            sheet,
            view,
            new PixelBoard(unit, sheet),
            new RunBoard(unit, lag),
            new SectionBoard(
                unit,
                symbolsModel.getLag().getLastVertexId(),
                lag),
            new SymbolGlyphBoard(unit + "-SymbolGlyphBoard", symbolsModel, 0),
            focus,
            new EvaluationBoard(
                unit + "-Evaluation-ActiveBoard",
                symbolsModel,
                sheet,
                view));

        // Create a hosting pane for the view
        ScrollLagView slv = new ScrollLagView(view);
        sheet.getAssembly()
             .addViewTab(Step.SYMBOLS, slv, boardsPane);
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the UI display (reset the model values of all spinners, updateMenu
     * the colors of the glyphs)
     */
    public void refresh ()
    {
        view.refresh();
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends GlyphLagView
    {
        //~ Constructors -------------------------------------------------------

        private MyView (GlyphLag lag)
        {
            super(lag, null, null, symbolsBuilder, null);
            setName("SymbolsEditor-MyView");

            // Use light gray color for past successful entities
            int viewIndex = lag.viewIndexOf(this);
            sheet.colorize(lag, viewIndex, Color.lightGray);
        }

        //~ Methods ------------------------------------------------------------

        //-----------------------//
        // asyncDeassignGlyphSet //
        //-----------------------//
        @Override
        public void asyncDeassignGlyphSet (Set<Glyph> glyphs)
        {
            super.asyncDeassignGlyphSet(glyphs);
        }

        //-------------------//
        // colorizeAllGlyphs //
        //-------------------//
        /**
         * Colorize all the glyphs of the sheet
         */
        @Override
        public void colorizeAllGlyphs ()
        {
            int viewIndex = lag.viewIndexOf(this);

            for (Glyph glyph : sheet.getActiveGlyphs()) {
                colorizeGlyph(viewIndex, glyph);
            }

            repaint();
        }

        //---------------//
        // colorizeGlyph //
        //---------------//
        @Override
        public void colorizeGlyph (int   viewIndex,
                                   Glyph glyph)
        {
            colorizeGlyph(
                viewIndex,
                glyph,
                focus.isDisplayed(glyph) ? glyph.getColor() : hiddenColor);
        }

        //-----------------//
        // contextSelected //
        //-----------------//
        @Override
        public void contextSelected (Point         pt,
                                     MouseMovement movement)
        {
            // Retrieve the selected glyphs
            Set<Glyph> glyphs = sheet.getVerticalLag()
                                     .getSelectedGlyphSet();

            // To display point information
            if ((glyphs == null) || (glyphs.size() == 0)) {
                pointSelected(pt, movement); // This may change glyph selection
                glyphs = sheet.getVerticalLag()
                              .getSelectedGlyphSet();
            }

            if ((glyphs != null) && !glyphs.isEmpty()) {
                // Update the popup menu according to selected glyphs
                glyphMenu.updateMenu();

                // Show the popup menu
                glyphMenu.getPopup()
                         .show(
                    this,
                    getZoom().scaled(pt.x),
                    getZoom().scaled(pt.y));
            } else {
                // Popup with no glyph selected ?
            }
        }

        //---------//
        // onEvent //
        //---------//
        /**
         * On reception of GLYPH_SET information, we build a transient compound
         * glyph which is then dispatched. Such glyph is always generated
         * (a null glyph if the set is null or empty, a simple glyph if the set
         * contains just one glyph, and a true compound glyph when the set
         * contains several glyphs)
         *
         * @param event the notified event
         */
        @Override
        public void onEvent (UserEvent event)
        {
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                // Default lag view behavior, including specifics
                super.onEvent(event);

                if (event instanceof GlyphSetEvent) {
                    GlyphSetEvent glyphsEvent = (GlyphSetEvent) event;
                    Set<Glyph>    glyphs = glyphsEvent.getData();
                    Glyph         compound = null;

                    if (glyphs != null) {
                        if (glyphs.size() > 1) {
                            try {
                                SystemInfo system = sheet.getSystemOf(glyphs);

                                if (system != null) {
                                    compound = system.buildCompound(glyphs);
                                }
                            } catch (IllegalArgumentException ex) {
                                // All glyphs do not belong to the same system
                                // No compound is allowed and displayed
                                logger.warning(
                                    "Glyphs from different systems " +
                                    Glyph.toString(glyphs));
                            }
                        } else if (glyphs.size() == 1) {
                            compound = glyphs.iterator()
                                             .next();
                        }
                    }

                    sheet.getVerticalLag()
                         .getSelectionService()
                         .publish(
                        new GlyphEvent(
                            this,
                            SelectionHint.GLYPH_TRANSIENT,
                            null,
                            compound));
                }
            } catch (Exception ex) {
                logger.warning(getClass().getName() + " onEvent error", ex);
            }
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        protected void renderItems (Graphics g)
        {
            // Render all sheet physical info known so far
            sheet.accept(new SheetPainter(g, false));

            // Normal display of selected items
            super.renderItems(g);
        }
    }
}
