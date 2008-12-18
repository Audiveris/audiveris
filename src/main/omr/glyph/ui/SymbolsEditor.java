//----------------------------------------------------------------------------//
//                                                                            //
//                         S y m b o l s E d i t o r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.Evaluator;
import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphNetwork;
import omr.glyph.GlyphsBuilder;
import omr.glyph.SymbolsBuilder;

import omr.lag.RunBoard;
import omr.lag.ScrollLagView;
import omr.lag.SectionBoard;

import omr.log.Logger;

import omr.score.entity.Note;
import omr.score.visitor.SheetPainter;

import omr.selection.GlyphEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.sheet.Sheet;
import omr.sheet.ui.PixelBoard;

import omr.ui.BoardsPane;

import omr.util.Implement;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

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
    private final SymbolsBuilder symbolsBuilder;

    /** Related sheet */
    private final Sheet sheet;

    /** Evaluator to check for NOISE glyphs */
    private final Evaluator evaluator = GlyphNetwork.getInstance();

    /** Glyphs builder */
    private final GlyphsBuilder glyphsBuilder;

    /** Related Lag view */
    private GlyphLagView view;

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
     * @param symbolsBuilder the symbols builder for this sheet
     */
    public SymbolsEditor (Sheet          sheet,
                          SymbolsBuilder symbolsBuilder)
    {
        this.sheet = sheet;
        this.symbolsBuilder = symbolsBuilder;

        GlyphLag lag = symbolsBuilder.getLag();

        // Link with glyph builder
        glyphsBuilder = sheet.getGlyphsBuilder();

        view = new MyView(lag);
        view.setLocationService(
            sheet.getEventService(),
            SheetLocationEvent.class);

        focus = new ShapeFocusBoard(
            sheet,
            view,
            symbolsBuilder,
            new ActionListener() {
                    @Implement(ActionListener.class)
                    public void actionPerformed (ActionEvent e)
                    {
                        view.colorizeAllGlyphs();
                    }
                });

        glyphMenu = new GlyphMenu(sheet, symbolsBuilder, evaluator, focus, lag);

        final String  unit = sheet.getRadix() + ":SymbolsEditor";

        BoardsPane    boardsPane = new BoardsPane(
            sheet,
            view,
            new PixelBoard(unit, sheet),
            new RunBoard(unit, lag),
            new SectionBoard(
                unit,
                symbolsBuilder.getLag().getLastVertexId(),
                lag),
            new SymbolGlyphBoard(unit + "-SymbolGlyphBoard", symbolsBuilder, 0),
            focus,
            new EvaluationBoard(
                unit + "-Evaluation-ActiveBoard",
                symbolsBuilder,
                sheet,
                view));

        // Create a hosting pane for the view
        ScrollLagView slv = new ScrollLagView(view);
        sheet.getAssembly()
             .addViewTab("Glyphs", slv, boardsPane);
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
        view.colorizeAllGlyphs();
        view.repaint();
    }

    //------------------//
    // showTranslations //
    //------------------//
    public void showTranslations (Collection<Glyph> glyphs)
    {
        for (Glyph glyph : glyphs) {
            for (Object entity : glyph.getTranslations()) {
                if (entity instanceof Note) {
                    Note note = (Note) entity;
                    logger.info(note + "->" + note.getChord());
                } else {
                    logger.info(entity.toString());
                }
            }
        }
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
            setName("SymbolsEditor-View");

            // Use light gray color for past successful entities
            sheet.colorize(lag, viewIndex, Color.lightGray);
        }

        //~ Methods ------------------------------------------------------------

        //-------------------//
        // colorizeAllGlyphs //
        //-------------------//
        /**
         * Colorize all the glyphs of the sheet
         */
        @Override
        public void colorizeAllGlyphs ()
        {
            for (Glyph glyph : sheet.getActiveGlyphs()) {
                colorizeGlyph(glyph);
            }

            repaint();
        }

        //---------------//
        // colorizeGlyph //
        //---------------//
        @Override
        public void colorizeGlyph (Glyph glyph)
        {
            colorizeGlyph(
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
            GlyphSetEvent glyphsEvent = (GlyphSetEvent) sheet.getVerticalLag()
                                                             .getLastEvent(
                GlyphSetEvent.class);
            List<Glyph>   glyphs = (glyphsEvent != null)
                                   ? glyphsEvent.getData() : null;

            // To display point information
            if ((glyphs == null) || (glyphs.size() == 0)) {
                pointSelected(pt, movement); // This may change glyph selection

                glyphsEvent = (GlyphSetEvent) sheet.getVerticalLag()
                                                   .getLastEvent(
                    GlyphSetEvent.class);
                glyphs = (glyphsEvent != null) ? glyphsEvent.getData() : null;
            }

            if ((glyphs != null) && (glyphs.size() > 0)) {
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

        //---------------//
        // deassignGlyph //
        //---------------//
        @Override
        public void deassignGlyph (Glyph glyph)
        {
            super.deassignGlyph(glyph);
            refresh();
        }

        //---------//
        // onEvent //
        //---------//
        /**
         * On reception of GLYPH_SET information, we build a transient compound
         * glyph which is then dispatched
         *
         * @param event the notified event
         */
        @Override
        public void onEvent (UserEvent event)
        {
            ///logger.info(getName() + " event:" + event);

            // Default lag view behavior, including specifics
            super.onEvent(event);

            if (event instanceof GlyphSetEvent) {
                GlyphSetEvent glyphsEvent = (GlyphSetEvent) event;
                List<Glyph>   glyphs = glyphsEvent.getData();

                if ((glyphs != null) && (glyphs.size() > 1)) {
                    Glyph compound = glyphsBuilder.buildCompound(glyphs);
                    sheet.getVerticalLag()
                         .publish(
                        new GlyphEvent(
                            this,
                            SelectionHint.GLYPH_TRANSIENT,
                            null,
                            compound));
                }
            }
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
