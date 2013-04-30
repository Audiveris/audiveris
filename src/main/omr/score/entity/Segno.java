//----------------------------------------------------------------------------//
//                                                                            //
//                                 S e g n o                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.facets.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import omr.score.common.PixelPoint;
import omr.score.visitor.ScoreVisitor;

/**
 * Class {@code Segno} represents a segno event
 *
 * @author Hervé Bitteur
 */
public class Segno
    extends AbstractDirection
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Segno.class);
    
    //~ Constructors -----------------------------------------------------------

    //-------//
    // Segno //
    //-------//
    /**
     * Creates a new instance of Segno event
     *
     * @param measure measure that contains this mark
     * @param point location of mark
     * @param chord the chord related to the mark, if any
     * @param glyph the underlying glyph
     */
    public Segno (Measure    measure,
                  PixelPoint point,
                  Chord      chord,
                  Glyph      glyph)
    {
        super(measure, point, chord, glyph);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // populate //
    //----------//
    /**
     * Used by SystemTranslator to allocate the segno marks
     *
     * @param glyph underlying glyph
     * @param measure measure where the mark is located
     * @param point location for the mark
     */
    public static void populate (Glyph      glyph,
                                 Measure    measure,
                                 PixelPoint point)
    {
        if (glyph.isVip()) {
            logger.info("Segno. populate {}", glyph.idString());
        }

        Slot slot = measure.getClosestSlot(point);

        if (slot != null) {
            glyph.setTranslation(
                new Segno(measure, point, slot.getChordJustBelow(point), glyph));
        }
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }
}
