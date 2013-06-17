//----------------------------------------------------------------------------//
//                                                                            //
//                       B e a m H o o k P a t t e r n                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;

/**
 * Class {@code BeamHookPattern} removes beam hooks for which the
 * related stem has no beam on the same horizontal side of the stem
 * and on the same vertical end of the stem.
 *
 * @author Hervé Bitteur
 */
public class BeamHookPattern
        extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            BeamHookPattern.class);

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // BeamHookPattern //
    //-----------------//
    /**
     * Creates a new BeamHookPattern object.
     *
     * @param system the system to process
     */
    public BeamHookPattern (SystemInfo system)
    {
        super("BeamHook", system);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int nb = 0;

        for (Glyph hook : system.getGlyphs()) {
            if ((hook.getShape() != Shape.BEAM_HOOK) || hook.isManualShape()) {
                continue;
            }

            if (hook.getStemNumber() != 1) {
                if (hook.isVip() || logger.isDebugEnabled()) {
                    logger.info(
                            "{} stem(s) for beam hook #{}",
                            hook.getStemNumber(),
                            hook.getId());
                }

                hook.setShape(null);
                nb++;
            } else {
                if (hook.isVip() || logger.isDebugEnabled()) {
                    logger.info("Checking hook #{}", hook.getId());
                }

                Glyph stem = null;
                HorizontalSide side = null;

                for (HorizontalSide s : HorizontalSide.values()) {
                    side = s;
                    stem = hook.getStem(s);

                    if (stem != null) {
                        break;
                    }
                }

                int hookDy = hook.getCentroid().y - stem.getCentroid().y;

                // Look for other stuff on the stem
                Rectangle stemBox = system.stemBoxOf(stem);
                boolean beamFound = false;

                for (Glyph g : system.lookupIntersectedGlyphs(
                        stemBox,
                        stem,
                        hook)) {
                    // We look for a beam on the same stem side
                    if ((g.getStem(side) == stem)) {
                        Shape shape = g.getShape();

                        if (ShapeSet.Beams.contains(shape)
                            && (shape != Shape.BEAM_HOOK)) {
                            if (hook.isVip() || logger.isDebugEnabled()) {
                                logger.info(
                                        "Confirmed beam hook #{}",
                                        hook.getId());
                            }

                            beamFound = true;

                            break;
                        }
                    }
                }

                if (!beamFound) {
                    // Deassign this hook w/ no beam neighbor
                    if (hook.isVip() || logger.isDebugEnabled()) {
                        logger.info("Cancelled beam hook #{}", hook.getId());
                    }

                    hook.setShape(null);
                    nb++;
                }
            }
        }

        return nb;
    }
}
