//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T u p l e t I n t e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.math.GeoOrder;
import omr.math.Rational;

import omr.sheet.DurationFactor;
import omr.sheet.Scale;
import omr.sheet.SystemInfo;
import omr.sheet.rhythm.Voice;

import omr.sig.SIGraph;
import omr.sig.relation.ChordTupletRelation;
import omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code TupletInter} represents a tuplet sign (3 or 6).
 * <p>
 * A tuplet inter cannot be assigned a staff immediately, since it may be located between staves and
 * not related to the closest one.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "tuplet")
public class TupletInter
        extends AbstractNotationInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TupletInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    // Factor lazily computed
    private DurationFactor durationFactor;

    /** Sequence of embraced chords. Lazily computed. */
    private List<AbstractChordInter> chords;

    /** Base duration. Lazily computed. */
    private Rational baseDuration;

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
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private TupletInter ()
    {
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
            logger.debug("Discarding isolated tuplet candidate glyph#{}", glyph.getId());

            return null;
        }

        return new TupletInter(glyph, shape, grade);
    }

    //-----------------//
    // getBaseDuration //
    //-----------------//
    public Rational getBaseDuration ()
    {
        if (baseDuration == null) {
            for (Relation rel : sig.getRelations(this, ChordTupletRelation.class)) {
                AbstractChordInter chord = (AbstractChordInter) sig.getOppositeInter(this, rel);
                Rational rawDur = chord.getDurationSansDotOrTuplet();

                if ((baseDuration == null) || (baseDuration.compareTo(rawDur) > 0)) {
                    baseDuration = rawDur;
                }
            }
        }

        return baseDuration;
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the sequence of chords embraced by this tuplet.
     *
     * @return the left to right sequence of chords
     */
    public List<AbstractChordInter> getChords ()
    {
        if (chords == null) {
            List<AbstractChordInter> list = new ArrayList<AbstractChordInter>();

            for (Relation tcRel : sig.getRelations(this, ChordTupletRelation.class)) {
                list.add((AbstractChordInter) sig.getOppositeInter(this, tcRel));
            }

            Collections.sort(list, Inter.byAbscissa);
            chords = Collections.unmodifiableList(list);
        }

        return chords;
    }

    //-------------------//
    // getDurationFactor //
    //-------------------//
    /**
     * @return the durationFactor
     */
    public DurationFactor getDurationFactor ()
    {
        if (durationFactor == null) {
            durationFactor = getFactor(shape);
        }

        return durationFactor;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        // Use the voice of the first chord embraced by the tuplet
        for (Relation rel : sig.getRelations(this, ChordTupletRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
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
                "Maximum abscissa gap between tuplet and closest chord");

        private final Scale.Fraction maxTupletChordDy = new Scale.Fraction(
                2.5,
                "Maximum ordinate gap between tuplet and closest chord");
    }
}
