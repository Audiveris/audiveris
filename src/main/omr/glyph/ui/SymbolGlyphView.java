//-----------------------------------------------------------------------//
//                                                                       //
//                     S y m b o l G l y p h V i e w                     //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.ui.GlyphLagView;
import omr.glyph.Shape;
import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionTag;
import omr.sheet.Sheet;
import omr.ui.view.Zoom;
import omr.util.Logger;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class <code>SymbolGlyphView</code> is a {@link GlyphLagView}
 * specifically meant to be used in conjunction with the other UI glyph
 * entities such as the instances of {@link GlyphPane}, {@link
 * SymbolGlyphBoard}, etc... So its implementation consists mainly in
 * overridden methods to allow updating of these other relevant entities.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SymbolGlyphView
    extends GlyphLagView
{
    private static Logger logger = Logger.getLogger(SymbolGlyphView.class);

    //~ Instance variables ------------------------------------------------

    // Companion entities
    private final Sheet sheet;
    private final GlyphPane pane;

    //~ Constructors ------------------------------------------------------

    //-----------------//
    // SymbolGlyphView //
    //-----------------//
    /**
     * Create a instance of SymbolGlyphView to display the glyph lag.
     *
     * @param sheet the related sheet
     * @param lag the containing lag
     * @param pane the pane for access to other components
     */
    public SymbolGlyphView (Sheet     sheet,
                            GlyphLag  lag,
                            GlyphPane pane)
    {
        super(lag, null, pane);

        this.sheet = sheet;
        this.pane  = pane;

        setName("SymbolGlyphView");

        // Current glyphs, initialized to empty list
        glyphSetSelection = sheet.getSelection(SelectionTag.GLYPH_SET);
        glyphSetSelection.setEntity(new ArrayList<Glyph>(),
                                    null,
                                    /* notity => */ false);

        // Observe glyph set
        glyphSetSelection.addObserver(this);

        // Use light gray color for past successful entities
        sheet.colorize(lag, viewIndex, Color.lightGray);
    }

    //~ Methods -----------------------------------------------------------

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
    private boolean contextSelected = false;
    @Override
        public void contextSelected (MouseEvent e,
                                     Point pt)
    {
        if (logger.isFineEnabled()) {
            logger.fine ("SymbolGlyphView contextSelected");
        }

        contextSelected = true;
        List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity();

        // To display point information
        if (glyphs.size() == 0) {
            pointSelected(e, pt);
            glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // modified?
            ///logger.info("size=" + glyphs.size());
        }

        if (glyphs.size() > 0) {
            GlyphMenu menu = pane.getGlyphMenu();
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
        contextSelected = false;
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
            pane.setShape(glyph, null, /* UpdateUI => */ true);
            pane.refresh();
            break;

        case COMBINING_STEM :
            pane.cancelStems(Collections.singletonList(glyph));
            break;

        default :
            pane.setShape(glyph, null, /* UpdateUI => */ true);
            pane.refresh();
            break;
        }
        }

    //---------//
    // update  //
    //---------//
    @Override
        public void update(Selection selection,
                           SelectionHint hint)
    {
        ///logger.info("SymbolGlyphView. selection=" + selection + " hint=" + hint);

        // Default lag view behavior, including specifics
        super.update(selection, hint);

        switch (selection.getTag()) {
        case VERTICAL_GLYPH :
            Glyph glyph = (Glyph) selection.getEntity();

            // Process the selected glyph
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity();
            boolean modified = false;
            if (addingGlyph) {
                // Adding glyphs, nothing to be done here
                // This will be performed through glyphAdded()
            } else if (contextSelected) {
                // Single contextual glyph selection
                if (glyphs.size() == 0) {
                    if (glyph != null) {
                        glyphs.add(glyph);
                        modified = true;
                    }
                }
            } else {
                // Simple selection
                if (glyphs.size() > 0) {
                glyphs.clear();
                modified = true;
                }
                if (glyph != null) {
                    glyphs.add(glyph);
                    modified = true;
                }
            }
            if (modified) {
                glyphSetSelection.setEntity(glyphs, hint);
            }
            break;

        case GLYPH_SET :
            ///logger.info("GlyphSet modified: Repainting");
            repaint();
            break;

        default :
        }
    }
}
