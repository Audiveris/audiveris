//----------------------------------------------------------------------------//
//                                                                            //
//                     H i d d e n S l u r P a t t e r n                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;


import omr.log.Logger;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Implement;


/**
 * Class {@code HiddenSlurPattern} processes the significant glyphs 
 * which have not been assigned a shape, looking for a slur inside.
 *
 * @author Hervé Bitteur
 */
public class HiddenSlurPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        HiddenSlurPattern.class);

    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // HiddenSlurPattern //
    //-------------------//
    /**
     * Creates a new HiddenSlurPattern object.
     * @param system the containing system
     */
    public HiddenSlurPattern (SystemInfo system)
    {
        super("HiddenSlur", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        SlurInspector inspector = system.getSlurInspector();
        int                  successNb = 0;
        final double         minGlyphWeight = constants.minGlyphWeight.getValue();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isKnown() ||
                glyph.isManualShape() ||
                (glyph.getNormalizedWeight() < minGlyphWeight)) {
                continue;
            }

            if (glyph.isVip()) {
                logger.info("Running HiddenSlur on glyph#" + glyph.getId());
            }

            // Pickup a long thin section as seed
            // Aggregate others progressively
            if (inspector.trimSlur(glyph) != null)
                successNb++;
        }

        return successNb;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.AreaFraction minGlyphWeight = new Scale.AreaFraction(
            0.5,
            "Minimum normalized glyph weight to lookup a slur section");
    }
}
