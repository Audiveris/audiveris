//----------------------------------------------------------------------------//
//                                                                            //
//                           F l a g P a t t e r n                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;

/**
 * Class {@code FlagPattern} removes flags for which the related stem
 * has no attached head (or at least some significant no-shape stuff).
 *
 * @author Hervé Bitteur
 */
public class FlagPattern
        extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            FlagPattern.class);

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // FlagPattern //
    //-------------//
    /**
     * Creates a new FlagPattern object.
     *
     * @param system the system to process
     */
    public FlagPattern (SystemInfo system)
    {
        super("Flag", system);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int nb = 0;

        for (Glyph flag : system.getGlyphs()) {
            if (!ShapeSet.Flags.contains(flag.getShape())
                || flag.isManualShape()) {
                continue;
            }

            if (flag.isVip() || logger.isDebugEnabled()) {
                logger.info("Checking flag #{}", flag.getId());
            }

            Glyph stem = flag.getStem(HorizontalSide.LEFT);

            if (stem == null) {
                if (flag.isVip() || logger.isDebugEnabled()) {
                    logger.info("No left stem for flag #{}", flag.getId());
                }

                flag.setShape(null);
                nb++;

                break;
            }

            // Look for other stuff on the stem, whatever the side
            Rectangle stemBox = system.stemBoxOf(stem);
            boolean found = false;

            for (Glyph g : system.lookupIntersectedGlyphs(stemBox, stem, flag)) {
                // We are looking for head (or some similar large stuff)
                Shape shape = g.getShape();

                if (ShapeSet.NoteHeads.contains(shape)
                    || ((shape == null)
                        && (g.getNormalizedWeight() >= constants.minStuffWeight.getValue()))) {
                    if (flag.isVip() || logger.isDebugEnabled()) {
                        logger.info("Confirmed flag #{}", flag.getId());
                    }

                    found = true;

                    break;
                }
            }

            if (!found) {
                // Deassign this flag w/ no head neighbor
                if (flag.isVip() || logger.isDebugEnabled()) {
                    logger.info("Cancelled flag #{}", flag.getId());
                }

                flag.setShape(null);
                nb++;
            }
        }

        return nb;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        //
        Scale.AreaFraction minStuffWeight = new Scale.AreaFraction(
                0.5,
                "Minimum weight for a stem stuff");

    }
}
