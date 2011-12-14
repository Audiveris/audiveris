//----------------------------------------------------------------------------//
//                                                                            //
//                         H e a d B a s e d S l o t                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.facets.Glyph;

import omr.math.Population;

import omr.score.common.PixelPoint;

/**
 * Class {@code HeadBasedSlot} is a slot whose position is based on the
 * abscissae of the contained note heads.
 *
 * @author Hervé Bitteur
 */
public class HeadBasedSlot
    extends Slot
{
    //~ Instance fields --------------------------------------------------------

    /** Abscissae of all glyphs */
    private Population xPop = new Population();

    /** Ordinates of all glyphs */
    private Population yPop = new Population();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // HeadBasedSlot //
    //---------------//
    /**
     * Creates a new HeadBasedSlot object.
     * @param measure the containing measure
     */
    public HeadBasedSlot (Measure measure)
    {
        super(measure);
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // getX //
    //------//
    /**
     * Report the abscissa of this slot.
     * @return the slot abscissa, wrt the containing system (and not measure)
     */
    public int getX ()
    {
        if (refPoint == null) {
            if (xPop.getCardinality() > 0) {
                refPoint = new PixelPoint(
                    (int) Math.rint(xPop.getMeanValue()),
                    (int) Math.rint(yPop.getMeanValue()));
            }
        }

        return refPoint.x;
    }

    //----------//
    // addGlyph //
    //----------//
    /**
     * Insert a glyph (supposedly from a chord) into this slot, 
     * invalidating the internal computed data.
     * @param glyph the glyph to insert
     */
    @Override
    public void addGlyph (Glyph glyph)
    {
        super.addGlyph(glyph);

        PixelPoint sysPt = glyph.getLocation();
        xPop.includeValue(sysPt.x);
        yPop.includeValue(sysPt.y);

        refPoint = null;
    }
}
