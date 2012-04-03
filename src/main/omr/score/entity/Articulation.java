//----------------------------------------------------------------------------//
//                                                                            //
//                          A r t i c u l a t i o n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

/**
 * Class {@code Articulation} represents an articulation event, a special
 * notation.
 * This should apply to:
 * <pre>
 * accent               standard
 * strong-accent        standard
 * staccato             standard
 * tenuto               standard
 * detached-legato      nyi
 * staccatissimo        standard
 * spiccato             nyi
 * scoop                nyi
 * plop                 nyi
 * doit                 nyi
 * falloff              nyi
 * breath-mark          nyi ???
 * caesura              nyi ???
 * stress               nyi
 * unstress             nyi
 * other-articulation   nyi
 * </pre>
 *
 * @author Hervé Bitteur
 */
public class Articulation
    extends AbstractNotation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Articulation.class);

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // Articulation //
    //--------------//
    /**
     * Creates a new instance of Articulation event
     *
     * @param measure measure that contains this mark
     * @param point location of mark
     * @param chord the chord related to the mark
     * @param glyph the underlying glyph
     */
    public Articulation (Measure    measure,
                         PixelPoint point,
                         Chord      chord,
                         Glyph      glyph)
    {
        super(measure, point, chord, glyph);
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // populate //
    //----------//
    /**
     * Used by SystemTranslator
     *
     * @param glyph underlying glyph
     * @param measure measure where the mark is located
     * @param point location for the mark
     */
    public static void populate (Glyph      glyph,
                                 Measure    measure,
                                 PixelPoint point)
    {
        // An Articulation relates to the note below or above on the same time slot
        Chord chord = measure.getEventChord(point);

        if (chord != null) {
            // Check vertical distance between chord and articulation
            PixelPoint head = chord.getHeadLocation();
            PixelPoint tail = chord.getTailLocation();
            int        dy = Math.min(
                Math.abs(head.y - point.y),
                Math.abs(tail.y - point.y));
            double     normedDy = measure.getScale()
                                         .pixelsToFrac(dy);

            if (normedDy <= constants.maxArticulationDy.getValue()) {
                glyph.setTranslation(
                    new Articulation(measure, point, chord, glyph));

                return;
            }
        }

        // Incorrect articulation
        if (logger.isFineEnabled()) {
            logger.info("Deassign articulation " + glyph);
        }

        glyph.setShape(null, Evaluation.ALGORITHM);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /**
         * Maximum dy between articulation and chord
         */
        Scale.Fraction maxArticulationDy = new Scale.Fraction(
            3,
            "Maximum dy between articulation and chord");
    }
}
