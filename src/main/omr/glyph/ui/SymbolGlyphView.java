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
 * @author Herv&eacute; Bitteur
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

    //-----------------//
    // contextSelected //
    //-----------------//
    private boolean contextSelected = false;
    @Override
        public void contextSelected (MouseEvent e,
                                     Point pt)
    {
        if (logger.isDebugEnabled()) {
            logger.debug ("SymbolGlyphView contextSelected");
        }

        contextSelected = true;
        List<Glyph> glyphs = pane.getCurrentGlyphs();

        // To display point information
        if (glyphs.size() == 0) {
            pointSelected(e, pt);
        }

        if (e == null) {
            // Not an interactive selection, so let's get out now
            return;
        }

        if (glyphs.size() > 0) {
            if (glyphs.size() == 1) {
                pane.getPopup().updateForGlyph(glyphs.get(0));
                pane.getPopup().getPopup().show(this, e.getX(), e.getY());
            } else if (glyphs.size() > 1) {
                pane.getPopup().updateForGlyphs(glyphs);
            }
            // Show the popup menu
            pane.getPopup().getPopup().show(this, e.getX(), e.getY());
        } else {
            // Popup with no glyph selected ?
        }
        contextSelected = false;
    }

    //---------------//
    // glyphSelected //
    //---------------//
    /**
     * Processing to be performed on the selected glyph.
     *
     * @param glyph the selected glyph, which may be null
     * @param pt the designated point
     */
    @Override
        protected void glyphSelected (Glyph glyph,
                                      Point pt)
    {
        ///logger.info(getClass() + " glyphSelected " + glyph);

        // Process the selected glyph
        List<Glyph> glyphs = pane.getCurrentGlyphs();
        if (!contextSelected) {
            if (glyphs.size() > 0) {
                glyphs.clear();
            }
            if (glyph != null) {
                glyphs.add(glyph);
            }
            pane.getEvaluatorsPanel().evaluate(glyph);
        } else {
            if (glyphs.size() == 0) {
                if (glyph != null) {
                    glyphs.add(glyph);
                }
                pane.getEvaluatorsPanel().evaluate(glyph);
             }
        }
    }

    //------------//
    // glyphAdded //
    //------------//
    /**
     * Addition of a glyph to a collection of selected glyphs. Il the
     * collection already contains that glyph, the glyph is in fact removed
     * of the collection
     *
     * @param glyph the to-be-added glyph, which may be null
     * @param pt the designated point
     */
    protected void glyphAdded (Glyph glyph,
                               Point pt)
    {
        if (logger.isDebugEnabled()) {
            logger.debug ("SymbolGlyphView glyphAdded");
        }

        if (glyph != null) {
            // Add to or remove from the collection of selected glyphs
            List<Glyph> glyphs = pane.getCurrentGlyphs();
            if (glyphs.contains(glyph)) {
                glyphs.remove(glyph);
            } else {
                glyphs.add(glyph);
            }
        }
        pane.getEvaluatorsPanel().evaluate(glyph);
    }

    //-------------------//
    // setFocusRectangle //
    //-------------------//
    @Override
        public void setFocusRectangle (Rectangle rect)
    {
        ///logger.info(getClass() + " setFocusRectangle " + rect);

        // Notify observers about rectangle information
        super.setFocusRectangle(rect);

        // Retrieve glyphs for this rectangle
        pane.setCurrentGlyphs(sheet.lookupGlyphs(rect));
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
