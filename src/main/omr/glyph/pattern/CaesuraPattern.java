//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  C a e s u r a P a t t e r n                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.score.entity.OldMeasure;
import omr.score.entity.OldSystemPart;
import omr.score.entity.ScoreSystem;

import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

/**
 * Class {@code CaesuraPattern} checks that a caesura in a measure
 * is not surrounded with chords.
 *
 * @author Hervé Bitteur
 */
public class CaesuraPattern
        extends GlyphPattern
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CaesuraPattern.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new CaesuraPattern object.
     *
     * @param system the system to process
     */
    public CaesuraPattern (SystemInfo system)
    {
        super("Caesura", system);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int nb = 0;
        ScoreSystem scoreSystem = system.getScoreSystem();

        for (Glyph glyph : system.getGlyphs()) {
            if ((glyph.getShape() != Shape.CAESURA) || glyph.isManualShape()) {
                continue;
            }

            Point center = glyph.getAreaCenter();
            OldSystemPart part = scoreSystem.getPartAt(center);
            OldMeasure measure = part.getMeasureAt(center);

            if (!measure.getChords().isEmpty()) {
                if (glyph.isVip() || logger.isDebugEnabled()) {
                    logger.info("Cancelled caesura #{}", glyph.getId());
                }

                glyph.setShape(null);
                nb++;
            }
        }

        return nb;
    }
}
