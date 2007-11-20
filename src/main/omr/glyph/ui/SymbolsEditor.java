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

import omr.score.entity.Note;
import omr.score.visitor.SheetPainter;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionTag;
import static omr.selection.SelectionTag.*;

import omr.sheet.Sheet;

import omr.ui.BoardsPane;
import omr.ui.PixelBoard;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>SymbolsEditor</code> defines a UI pane from which all symbol
 * processing actions can be launched and their results checked.
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

    /** Pointer to glyph board */
    private SymbolGlyphBoard glyphBoard;

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
     */
    public SymbolsEditor (Sheet          sheet,
                          SymbolsBuilder symbolsBuilder)
    {
        this.sheet = sheet;
        this.symbolsBuilder = symbolsBuilder;

        // Link with glyph builder
        glyphsBuilder = sheet.getGlyphsBuilder();

        view = new MyView(symbolsBuilder.getLag());
        view.setLocationSelection(
            sheet.getSelection(SelectionTag.SHEET_RECTANGLE));

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

        glyphMenu = new GlyphMenu(
            sheet,
            symbolsBuilder,
            evaluator,
            focus,
            sheet.getSelection(VERTICAL_GLYPH),
            sheet.getSelection(GLYPH_SET));

        final String unit = sheet.getRadix() + ":SymbolsEditor";

        glyphBoard = new SymbolGlyphBoard(
            unit + "-SymbolGlyphBoard",
            symbolsBuilder,
            0,
            sheet.getSelection(VERTICAL_GLYPH),
            sheet.getSelection(VERTICAL_GLYPH_ID),
            sheet.getSelection(GLYPH_SET));

        BoardsPane    boardsPane = new BoardsPane(
            sheet,
            view,
            new PixelBoard(unit),
            new RunBoard(unit, sheet.getSelection(VERTICAL_RUN)),
            new SectionBoard(
                unit,
                symbolsBuilder.getLag().getLastVertexId(),
                sheet.getSelection(VERTICAL_SECTION),
                sheet.getSelection(VERTICAL_SECTION_ID)),
            glyphBoard,
            focus,
            new EvaluationBoard(
                unit + "-Evaluation-ActiveBoard",
                symbolsBuilder,
                sheet.getSelection(VERTICAL_GLYPH),
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

    //-----------//
    // MyLagView //
    //-----------//
    private class MyView
        extends GlyphLagView
    {
        private MyView (GlyphLag lag)
        {
            super(lag, null, null, symbolsBuilder, null);
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
                // Update the popup menu according to selected glyphs
                glyphMenu.updateMenu();

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

        //------------//
        // updateMenu //
        //------------//
        /**
         * On reception of GLYPH_SET information, we build a transient compound
         * glyph which is then dispatched
         *
         * @param selection the notified selection
         * @param hint the processing hint if any
         */
        @Override
        public void update (Selection     selection,
                            SelectionHint hint)
        {
            ///logger.info(getName() + " updateMenu. " + selection + " hint=" + hint);

            // Default lag view behavior, including specifics
            super.update(selection, hint);

            switch (selection.getTag()) {
            case GLYPH_SET :

                List<Glyph> glyphs = (List<Glyph>) selection.getEntity(); // Compiler warning

                if ((glyphs != null) && (glyphs.size() > 1)) {
                    Glyph compound = glyphsBuilder.buildCompound(glyphs);
                    glyphSelection.setEntity(
                        compound,
                        SelectionHint.GLYPH_TRANSIENT);
                }

                break;

            default :
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
