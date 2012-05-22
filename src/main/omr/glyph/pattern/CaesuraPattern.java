//----------------------------------------------------------------------------//
//                                                                            //
//                        C a e s u r a P a t t e r n                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2012. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import java.util.logging.Level;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;

import omr.sheet.SystemInfo;

/**
 * Class {@code CaesuraPattern} checks that a caesura in a measure
 * is not surrounded with chords.
 *
 * @author Hervé Bitteur
 */
public class CaesuraPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(CaesuraPattern.class);

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // CaesuraPattern //
    //----------------//
    /**
     * Creates a new CaesuraPattern object.
     * @param system the system to process
     */
    public CaesuraPattern (SystemInfo system)
    {
        super("Caesura", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int         nb = 0;
        ScoreSystem scoreSystem = system.getScoreSystem();

        for (Glyph glyph : system.getGlyphs()) {
            if ((glyph.getShape() != Shape.CAESURA) || glyph.isManualShape()) {
                continue;
            }

            PixelPoint center = glyph.getAreaCenter();
            SystemPart part = scoreSystem.getPartAt(center);
            Measure    measure = part.getMeasureAt(center);

            if (!measure.getChords()
                        .isEmpty()) {
                if (glyph.isVip() || logger.isFineEnabled()) {
                    logger.info("Cancelled caesura #{0}", glyph.getId());
                }

                glyph.setShape(null);
                nb++;
            }
        }

        return nb;
    }
}
