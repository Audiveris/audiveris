//-----------------------------------------------------------------------//
//                                                                       //
//                     S y m b o l G l y p h V i e w                     //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphLagView;
import omr.glyph.Shape;
import omr.sheet.Sheet;
import omr.ui.Zoom;
import omr.util.Logger;

import java.awt.*;
import java.awt.event.*;
import java.util.Collections;
import java.util.List;

/**
 * Class <code>SymbolGlyphView</code> is a {@link GlyphLagView}
 * specifically meant to be used in conjunction with the other UI glyph
 * entities such as the instances of {@link GlyphPane}, {@link
 * SymbolGlyphBoard}, etc... So its implementation consists mainly in
 * overridden methods to allow updating of these other relevant entities.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SymbolGlyphView
    extends GlyphLagView
{
    private static Logger logger = Logger.getLogger(SymbolGlyphView.class);

    //~ Instance variables ------------------------------------------------

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

        // Use light gray color for past successful entities
        sheet.colorize(lag, viewIndex, Color.lightGray);
    }

    //~ Methods -----------------------------------------------------------

    //-------------//
    // renderItems //
    //-------------//
    @Override
        public void renderItems (Graphics g)
    {
        Zoom z = getZoom();

        // Render all physical info known so far
        sheet.render(g, z);

        // Mark the current glyphs
        g.setColor(Color.black);
        for (Glyph glyph : pane.getCurrentGlyphs()) {
            g.setXORMode(Color.white);
            glyph.renderContour(g, z);
        }
    }

    //---------------//
    // pointSelected //
    //---------------//
    @Override
        public void pointSelected (MouseEvent e,
                                   Point pt)
    {
        // To display point information
        pointUpdated(e, pt);

        if (e == null) {
            // Not an interactive selection, so let's get out now
            return;
        }

        List<Glyph> glyphs = pane.getCurrentGlyphs();
        if (glyphs.size() == 1) {
            // Show the popup menu
            pane.getPopup().updateForGlyph(glyphs.get(0));
            pane.getPopup().show(this, e.getX(), e.getY());
        }
    }

    //--------------//
    // pointUpdated //
    //--------------//
    @Override
        public void pointUpdated (MouseEvent e,
                                  Point pt)
    {
        super.pointUpdated(e, pt);

        final Glyph glyph = sheet.lookupGlyph(pt);

        pane.getCurrentGlyphs().clear();
        if (glyph != null) {
            pane.getCurrentGlyphs().add(glyph);
        }

        pane.getEvaluatorsPanel().evaluate(glyph);
    }

    //-------------------//
    // rectangleSelected //
    //-------------------//
    @Override
        public void rectangleSelected (MouseEvent e,
                                       Rectangle rect)
    {
        super.rectangleSelected(e, rect);

        // Retrieve glyphs for this rectangle
        List<Glyph> glyphs = sheet.lookupGlyphs(rect);

        if (glyphs.size() > 0) {
            //logger.info(glyphs.size() + " glyph(s) selected");

            // Customize popup menu to process the list
            pane.setCurrentGlyphs(glyphs);
            if (e != null) {
                pane.getPopup().updateForGlyphs(glyphs);
                pane.getPopup().show(this, e.getX(), e.getY());
            }
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
}
