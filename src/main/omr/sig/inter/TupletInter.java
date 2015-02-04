//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T u p l e t I n t e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.math.GeoOrder;

import omr.score.entity.DurationFactor;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;
import omr.sheet.Voice;

import omr.sig.SIGraph;
import omr.sig.relation.Relation;
import omr.sig.relation.TupletChordRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.List;

/**
 * Class {@code TupletInter} represents a tuplet sign (3 or 6).
 * <p>
 * A tuplet inter cannot be assigned a staff immediately, since it may be located between staves and
 * not related to the closest one.
 *
 * @author Hervé Bitteur
 */
public class TupletInter
        extends AbstractNotationInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TupletInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final DurationFactor durationFactor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TupletInter} object.
     *
     * @param glyph the tuplet glyph
     * @param shape TUPLET_THREE or TUPLET_SIX
     * @param grade the inter quality
     */
    private TupletInter (Glyph glyph,
                         Shape shape,
                         double grade)
    {
        super(glyph, glyph.getBounds(), shape, grade);
        durationFactor = getFactor(shape);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a tuplet inter, checking that there is at least one (head) chord
     * nearby.
     *
     * @param glyph        the candidate tuplet glyph
     * @param shape        TUPLET_THREE or TUPLET_SIX
     * @param grade        the interpretation quality
     * @param system       the related system
     * @param systemChords abscissa-ordered list of chords in this system
     * @return the create TupletInter or null
     */
    public static TupletInter create (Glyph glyph,
                                      Shape shape,
                                      double grade,
                                      SystemInfo system,
                                      List<Inter> systemChords)
    {
        Rectangle luBox = glyph.getBounds();
        Scale scale = system.getSheet().getScale();
        luBox.grow(
                scale.toPixels(constants.maxTupletChordDx),
                scale.toPixels(constants.maxTupletChordDy));

        List<Inter> nearby = SIGraph.intersectedInters(systemChords, GeoOrder.BY_ABSCISSA, luBox);

        if (nearby.isEmpty()) {
            logger.info("Discarding isolated tuplet candidate glyph#{}", glyph.getId());

            return null;
        }

        return new TupletInter(glyph, shape, grade);
    }

    //-------------------//
    // getDurationFactor //
    //-------------------//
    /**
     * @return the durationFactor
     */
    public DurationFactor getDurationFactor ()
    {
        return durationFactor;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        // Use the voice of the first chord embraced by the tuplet
        for (Relation rel : sig.getRelations(this, TupletChordRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }

    //-----------//
    // getFactor //
    //-----------//
    /**
     * Report the tuplet factor that corresponds to the tuplet sign
     *
     * @param glyph the tuplet sign
     * @return the related factor
     */
    private static DurationFactor getFactor (Shape shape)
    {
        switch (shape) {
        case TUPLET_THREE:
            return new DurationFactor(2, 3);

        case TUPLET_SIX:
            return new DurationFactor(4, 6);

        default:
            logger.error("Incorrect tuplet shape " + shape);

            return null;
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction maxTupletChordDx = new Scale.Fraction(
                3,
                "Maximum dx between tuplet and closest chord");

        private final Scale.Fraction maxTupletChordDy = new Scale.Fraction(
                3,
                "Maximum dy between tuplet and closest chord");
    }
}
