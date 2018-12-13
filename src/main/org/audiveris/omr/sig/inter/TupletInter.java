//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T u p l e t I n t e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.DurationFactor;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.TupletsBuilder;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.relation.ChordTupletRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

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
        extends AbstractInter
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TupletInter.class);

    // Factor lazily computed
    private DurationFactor durationFactor;

    /** Base duration. Lazily computed. */
    private Rational baseDuration;

    /**
     * Creates a new {@code TupletInter} object.
     *
     * @param glyph the tuplet glyph
     * @param shape TUPLET_THREE or TUPLET_SIX
     * @param grade the inter quality
     */
    public TupletInter (Glyph glyph,
                        Shape shape,
                        double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, shape, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private TupletInter ()
    {
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-------//
    // added //
    //-------//
    /**
     * Add it from containing stack and/or measure.
     *
     * @see #remove(boolean)
     */
    @Override
    public void added ()
    {
        super.added();

        MeasureStack stack = sig.getSystem().getStackAt(getCenter());

        if (stack != null) {
            stack.addInter(this);
        }

        setAbnormal(true); // No chord linked yet
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        SortedSet<AbstractChordInter> embraced = TupletsBuilder.getEmbracedChords(
                this,
                getChords());
        setAbnormal(embraced == null);

        return isAbnormal();
    }

    //-----------------//
    // getBaseDuration //
    //-----------------//
    /**
     * Report the chord duration (without dot) on which tuplet modification applies.
     *
     * @return base duration
     */
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
        List<AbstractChordInter> list = new ArrayList<>();

        for (Relation tcRel : sig.getRelations(this, ChordTupletRelation.class)) {
            list.add((AbstractChordInter) sig.getOppositeInter(this, tcRel));
        }

        Collections.sort(list, Inters.byAbscissa);

        return Collections.unmodifiableList(list);
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
    // getStaff //
    //----------//
    @Override
    public Staff getStaff ()
    {
        if (staff == null) {
            // Chord -> Tuplet
            for (Relation ctRel : sig.getRelations(this, ChordTupletRelation.class)) {
                AbstractChordInter chord = (AbstractChordInter) sig.getOppositeInter(this, ctRel);

                if (chord.getStaff() != null) {
                    return staff = chord.getStaff();
                }
            }
        }

        return staff;
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

    //--------//
    // remove //
    //--------//
    /**
     * Remove it from containing stack and/or measure.
     *
     * @param extensive true for non-manual removals only
     * @see #added()
     */
    @Override
    public void remove (boolean extensive)
    {
        MeasureStack stack = sig.getSystem().getStackAt(getCenter());

        if (stack != null) {
            stack.removeInter(this);
        }

        super.remove(extensive);
    }

    //-------------//
    // searchLinks //
    //-------------//
    /**
     * {@inheritDoc}
     * <p>
     * Specifically, look for chords to link with this tuplet.
     *
     * @return chords link, perhaps empty
     */
    @Override
    public Collection<Link> searchLinks (SystemInfo system,
                                         boolean doit)
    {
        MeasureStack stack = system.getStackAt(getCenter());
        Collection<Link> links = new TupletsBuilder(stack).lookupLinks(this);

        if (doit) {
            for (Link link : links) {
                link.applyTo(this);
            }
        }

        return links;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
    }

    //-------------//
    // createValid //
    //-------------//
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
    public static TupletInter createValid (Glyph glyph,
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

        List<Inter> nearby = Inters.intersectedInters(systemChords, GeoOrder.BY_ABSCISSA, luBox);

        if (nearby.isEmpty()) {
            logger.debug("Discarding isolated tuplet candidate glyph#{}", glyph.getId());

            return null;
        }

        return new TupletInter(glyph, shape, grade);
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

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction maxTupletChordDx = new Scale.Fraction(
                3,
                "Maximum abscissa gap between tuplet and closest chord");

        private final Scale.Fraction maxTupletChordDy = new Scale.Fraction(
                2.5,
                "Maximum ordinate gap between tuplet and closest chord");
    }
}
