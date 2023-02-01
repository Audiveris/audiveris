//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              A b s t r a c t N u m b e r I n t e r                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.TimeTopBottomRelation;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.LinkTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.NumberSymbol;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.BOTTOM;
import static org.audiveris.omr.util.VerticalSide.TOP;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.Wrapper;
import org.audiveris.omrdataset.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class <code>AbstractNumberInter</code> is an abstract inter with a integer value.
 * <p>
 * Concrete subclasses are defined for:
 * <ul>
 * <li>{@link TimeNumberInter} value in upper or lower part of a time signature,
 * <li>{@link MeasureCountInter} to specify the count of measures above a {@link MultipleRestInter}
 * or above a {@link SimileMarkInter} for four bars (this latter case can be ignored).
 * <li>An ending number in a volta (to be confirmed)
 * </ul>
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractNumberInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractNumberInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Integer value for the number. */
    @XmlAttribute
    protected Integer value;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AbstractNumberInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public AbstractNumberInter (Glyph glyph,
                                Shape shape,
                                Double grade)
    {
        super(glyph, null, shape, grade);
        this.value = (shape != null) ? valueOf(shape) : null;
    }

    /**
     * Creates a new AbstractNumberInter object.
     *
     * @param glyph underlying glyph
     * @param value numerical value
     * @param grade evaluation value
     */
    public AbstractNumberInter (Glyph glyph,
                                Integer value,
                                Double grade)
    {
        super(glyph, null, null, grade);

        if (value != null) {
            this.value = value; // Copy
        }
    }

    /**
     * Creates a new AbstractNumberInter object.
     *
     * @param bounds bounding box of the number
     * @param shape  precise shape
     * @param grade  evaluation value
     */
    public AbstractNumberInter (Rectangle bounds,
                                Shape shape,
                                Double grade)
    {
        super(null, bounds, shape, grade);
        this.value = (shape != null) ? valueOf(shape) : null;
    }

    /**
     * Creates a new AbstractNumberInter object.
     *
     * @param bounds bounding box of the number
     * @param value  numerical value
     * @param grade  evaluation value
     */
    public AbstractNumberInter (Rectangle bounds,
                                Integer value,
                                Double grade)
    {
        super(null, bounds, null, grade);

        if (value != null) {
            this.value = value; // Copy
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //--------//
    // create //
    //--------//
    /**
     * (Try to) create instance of proper sub-class of AbstractNumberInter
     * (TimeNumberInter, MeasureCountInter, ...).
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param staff related staff
     * @return the created instance or null if failed
     */
    public static AbstractNumberInter create (Glyph glyph,
                                              Shape shape,
                                              double grade,
                                              Staff staff)
    {
        AbstractNumberInter number;

        // TimeNumber?
        number = TimeNumberInter.create(glyph, shape, grade, staff);
        if (number != null) {
            return number;
        }

        // MeasureNumber?
        number = MeasureCountInter.createValidAdded(glyph, shape, grade, staff);
        if (number != null) {
            return number;
        }

        // Nothing
        return null;
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "NUMBER_" + value;
    }

    //----------------//
    // getShapeSymbol //
    //----------------//
    @Override
    public ShapeSymbol getShapeSymbol (MusicFamily family)
    {
        return new NumberSymbol(shape, family, value);
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the integer value of this symbol
     *
     * @return the integer value
     */
    public Integer getValue ()
    {
        return value;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + value;
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Set a new integer value to this inter.
     *
     * @param value the new value
     */
    public void setValue (Integer value)
    {
        this.value = value;

        // Update containing time pair if any
        if (sig != null) {
            for (Relation rel : sig.getRelations(this, Containment.class)) {
                final Inter ens = sig.getEdgeSource(rel);
                if (ens instanceof TimePairInter pair) {
                    pair.invalidateCache();
                }
            }
        }
    }

    //---------//
    // valueOf //
    //---------//
    /**
     * Report the integer value for the provided shape
     *
     * @param shape shape to test
     * @return supported integer value or IllegalArgumentException is thrown
     */
    protected static int valueOf (Shape shape)
    {
        return switch (shape) {
        case NUMBER_CUSTOM, TIME_ZERO -> 0;
        case TIME_ONE -> 1;
        case TIME_TWO -> 2;
        case TIME_THREE -> 3;
        case TIME_FOUR -> 4;
        case TIME_FIVE -> 5;
        case TIME_SIX -> 6;
        case TIME_SEVEN -> 7;
        case TIME_EIGHT -> 8;
        case TIME_NINE -> 9;
        case TIME_TWELVE -> 12;
        case TIME_SIXTEEN -> 16;

        default -> throw new IllegalArgumentException("No integer value defined for " + shape);
        };
    }

    //---------//
    // valueOf //
    //---------//
    protected static int valueOf (OmrShape omrShape)
    {
        return switch (omrShape) {
        case timeSig0 -> 0;
        case timeSig1 -> 1;
        case timeSig2 -> 2;
        case timeSig3 -> 3;
        case timeSig4 -> 4;
        case timeSig5 -> 5;
        case timeSig6 -> 6;
        case timeSig7 -> 7;
        case timeSig8 -> 8;
        case timeSig9 -> 9;
        case timeSig12 -> 12;
        case timeSig16 -> 16;

        default -> throw new IllegalArgumentException("No integer value defined for " + omrShape);
        };
    }

    //-------------//
    // NumberInter //
    //-------------//
    /**
     * This class is a concrete class, meant to temporarily convey an integer value.
     * <p>
     * It is assumed that the context (such as the final DnD location) will ultimately indicate
     * whether we are dealing with a {@link TimeNumberInter} or a {@link MeasureCountInter}.
     */
    public static class NumberInter
            extends AbstractNumberInter
    {

        public NumberInter (Glyph glyph,
                            Shape shape,
                            Double grade)
        {
            super(glyph, shape, grade);
        }

        //------------//
        // deriveFrom //
        //------------//
        @Override
        public boolean deriveFrom (ShapeSymbol symbol,
                                   Sheet sheet,
                                   MusicFont font,
                                   Point dropLocation)
        {
            // First call needed to get bounds
            super.deriveFrom(symbol, sheet, font, dropLocation);

            // If within staff height, we snap ordinate to target pitch: -2 or +2
            if (staff != null) {
                final Point center = getCenter();

                if (staff.contains(center)) {
                    final double pitch = staff.pitchPositionOf(center);
                    final double y = staff.pitchToOrdinate(center.getX(), pitch < 0 ? -2 : 2);
                    dropLocation.y = (int) Math.rint(y);

                    // Final call with refined dropLocation
                    super.deriveFrom(symbol, sheet, font, dropLocation);
                }
            }

            return true;
        }

        //--------//
        // preAdd //
        //--------//
        /**
         * This is rather tricky, since this temporary NumberInter instance is to be replaced
         * by an instance of a more relevant class, once the target is known.
         */
        @Override
        public List<? extends UITask> preAdd (WrappedBoolean cancel,
                                              Wrapper<Inter> toPublish)
        {
            // Standard addition task for this number
            final SystemInfo system = staff.getSystem();
            final SIGraph sig = system.getSig();
            final List<UITask> tasks = new ArrayList<>();

            final Collection<Link> links = searchLinks(system);

            if (links.isEmpty()) {
                // Perhaps a brand new orphan time number
                final VerticalSide vSide = timeNumberSide(system);
                if (vSide != null) {
                    final TimeNumberInter tn = new TimeNumberInter(glyph, shape, getGrade(), vSide);
                    tn.setManual(true);
                    tn.setStaff(system.getStaffAtOrBelow(getCenter()));
                    tasks.add(new AdditionTask(sig, tn, getBounds(), /* empty */ links));
                    toPublish.value = tn;
                }
            } else {
                final Link link = links.iterator().next();
                if (link.partner instanceof MultipleRestInter rest) {
                    // Use a MeasureCountInter
                    final MeasureCountInter mn = new MeasureCountInter(glyph, shape, getGrade());
                    mn.setManual(true);
                    mn.setStaff(rest.getStaff());
                    toPublish.value = mn;
                    tasks.add(new AdditionTask(sig, mn, getBounds(), links));
                } else if (link.partner instanceof TimeNumberInter other) {
                    // Use a TimeNumberInter
                    final TimeNumberInter tn = new TimeNumberInter(
                            glyph,
                            shape,
                            getGrade(),
                            link.outgoing ? TOP : BOTTOM);
                    tn.setManual(true);
                    tn.setStaff(other.getStaff());
                    toPublish.value = tn;

                    tasks.add(new AdditionTask(sig, tn, getBounds(), links));

                    // Create the time pair ensemble?
                    if (other.getEnsemble() == null) {
                        final TimePairInter pair = new TimePairInter(null, null);
                        pair.setManual(true);
                        pair.setStaff(other.getStaff());
                        tasks.add(
                                new AdditionTask(
                                        sig,
                                        pair,
                                        null,
                                        Arrays.asList(new Link(tn, new Containment(), true))));
                        tasks.add(new LinkTask(sig, pair, other, new Containment()));
                    } else {
                        final TimePairInter pair = (TimePairInter) other.getEnsemble();
                        tasks.add(new LinkTask(sig, pair, tn, new Containment()));
                    }
                }
            }

            return tasks;
        }

        //-------------//
        // searchLinks //
        //-------------//
        /**
         * {@inheritDoc}.
         * <p>
         * Specifically, various links are searched for: time partial signature, multi-measure rest.
         */
        @Override
        public Collection<Link> searchLinks (SystemInfo system)
        {
            final Point center = getCenter();
            final Staff theStaff = system.getStaffAtOrBelow(center);

            if (theStaff != null) {
                if (theStaff.contains(center)) {
                    // If located within a staff height, this can only be a TimeNumberInter.
                    // We simply look for a time partner above or below.
                    final double pp = theStaff.pitchPositionOf(center);
                    final int y = (int) Math.rint(theStaff.pitchToOrdinate(center.x, -pp));
                    final Point p = new Point(center.x, y);
                    final List<Inter> numbers = system.getSig().inters(TimeNumberInter.class);
                    for (Inter tn : numbers) {
                        if (tn.getBounds().contains(p)) {
                            final TimeTopBottomRelation rel = new TimeTopBottomRelation();
                            return Collections.singleton(new Link(tn, rel, pp < 0));
                        }
                    }
                } else {
                    // Otherwise, it can be a MeasureCountInter and we look into the staff below
                    final Link link = MeasureCountInter.lookupLink(getCenter(), system);

                    if (link != null)
                        return Collections.singleton(link);
                }
            }

            return Collections.emptyList();
        }

        //----------------//
        // timeNumberSide //
        //----------------//
        /**
         * Purely based on location within staff.
         *
         * @param system the containing system
         * @return vertical side if OK, null otherwise
         */
        private VerticalSide timeNumberSide (SystemInfo system)
        {
            final Point center = getCenter();
            final Staff theStaff = system.getStaffAtOrBelow(center);

            if (theStaff.contains(center)) {
                final double pp = theStaff.pitchPositionOf(center);
                if (TimeNumberInter.isPitchValid(pp)) {
                    return pp < 0 ? TOP : BOTTOM;
                }
            }

            return null;
        }
    }
}
