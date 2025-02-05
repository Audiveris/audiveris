//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      N u m b e r I n t e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.TimeTopBottomRelation;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.LinkTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.VerticalSide;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>NumberInter</code> is meant to temporarily convey an integer value.
 * <p>
 * It is assumed that the context (such as the final DnD location) will ultimately indicate
 * whether we are dealing with a {@link TimeNumberInter} or a {@link MeasureCountInter}.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "number")
public class NumberInter
        extends AbstractNumberInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(NumberInter.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private NumberInter ()
    {
        super((Glyph) null, (Integer) null, 0.0);
    }

    /**
     * Creates a new NumberInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public NumberInter (Glyph glyph,
                        Shape shape,
                        Double grade)
    {
        super(glyph, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

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

        if (staff != null) {
            if (staff.isTablature()) {
                return false;
            }

            // If very likely to be a time number, we snap ordinate to target pitch: -2 or +2
            if (isInside(staff, getBounds())) {
                final Point center = getCenter();
                final double pitch = staff.pitchPositionOf(center);
                final double y = staff.pitchToOrdinate(center.getX(), pitch < 0 ? -2 : 2);
                dropLocation.y = (int) Math.rint(y);

                // Final call with refined dropLocation
                super.deriveFrom(symbol, sheet, font, dropLocation);
            }
        }

        return true;
    }

    //----------------//
    // linkAndConvert //
    //----------------//
    /**
     * Link and convert this NumberInter instance to
     * either a MeasureCountInter instance linked to a MultipleRestInter/MeasureRepeatInter
     * or a TimeNumberInter instance linked to a paired TimeNumberInter.
     */
    public void linkAndConvert ()
    {
        if (isVip()) {
            logger.info("VIP linkAndConvert for {}", this);
        }

        final Collection<Link> links = searchLinks(sig.getSystem());

        if (!links.isEmpty()) {
            final Link link = links.iterator().next(); // There should be just one link

            if ((link.partner instanceof MultipleRestInter)
                    || (link.partner instanceof MeasureRepeatInter)) {
                // Use a MeasureCountInter
                final MeasureCountInter mc = new MeasureCountInter(glyph, shape, getGrade());
                mc.setStaff(link.partner.getStaff());
                sig.addVertex(mc);
                link.applyTo(mc);
            } else if (link.partner instanceof TimeNumberInter other) {
                // Use a TimeNumberInter
                final VerticalSide vSide = link.outgoing ? VerticalSide.TOP : VerticalSide.BOTTOM;
                final TimeNumberInter tn = new TimeNumberInter(glyph, shape, getGrade(), vSide);
                tn.setStaff(other.getStaff());
                sig.addVertex(tn);
                link.applyTo(tn);

                // Create the time pair ensemble?
                if (other.getEnsemble() == null) {
                    final TimePairInter pair = new TimePairInter(null, null);
                    pair.setStaff(other.getStaff());
                    sig.addVertex(pair);
                    sig.addEdge(pair, other, new Containment());
                    sig.addEdge(pair, tn, new Containment());
                } else {
                    final TimePairInter pair = (TimePairInter) other.getEnsemble();
                    sig.addEdge(pair, tn, new Containment());
                }
            }
        } else {
            logger.debug("No time, rest or repeat sign linked to {}", this);
        }
    }

    //--------//
    // preAdd //
    //--------//
    /**
     * {@inheritDoc }
     * <p>
     * This is rather tricky, since this temporary NumberInter instance is to be replaced
     * by an instance of a more relevant class, once the target is known.
     * Staff is assumed to be known.
     */
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel,
                                          Wrapper<Inter> toPublish)
    {
        // Standard addition task for this number
        final SystemInfo system = staff.getSystem();
        final SIGraph theSig = system.getSig();
        final List<UITask> tasks = new ArrayList<>();
        final Collection<Link> links = searchLinks(system);

        if (links.isEmpty()) {
            // Perhaps a brand new orphan time number
            final VerticalSide vSide = timeNumberSide(system);

            if (vSide != null) {
                final TimeNumberInter tn = new TimeNumberInter(glyph, shape, getGrade(), vSide);
                tn.setManual(true);
                tn.setStaff(system.getClosestStaff(getCenter()));
                tasks.add(new AdditionTask(theSig, tn, getBounds(), /* empty */ links));
                toPublish.value = tn;
            }
        } else {
            final Link link = links.iterator().next();

            if ((link.partner instanceof MultipleRestInter)
                    || (link.partner instanceof MeasureRepeatInter)) {
                // Use a MeasureCountInter
                final MeasureCountInter mc = new MeasureCountInter(glyph, shape, getGrade());
                mc.setStaff(link.partner.getStaff());
                mc.setManual(true);
                toPublish.value = mc;
                tasks.add(new AdditionTask(theSig, mc, getBounds(), links));
            } else if (link.partner instanceof TimeNumberInter other) {
                // Use a TimeNumberInter
                final VerticalSide vSide = link.outgoing ? VerticalSide.TOP : VerticalSide.BOTTOM;
                final TimeNumberInter tn = new TimeNumberInter(glyph, shape, getGrade(), vSide);
                tn.setStaff(other.getStaff());
                tn.setManual(true);
                toPublish.value = tn;
                tasks.add(new AdditionTask(theSig, tn, getBounds(), links));

                // Create the time pair ensemble?
                if (other.getEnsemble() == null) {
                    final TimePairInter pair = new TimePairInter(null, null);
                    pair.setStaff(other.getStaff());
                    pair.setManual(true);
                    tasks.add(
                            new AdditionTask(
                                    theSig,
                                    pair,
                                    null,
                                    Arrays.asList(new Link(tn, new Containment(), true))));
                    tasks.add(new LinkTask(theSig, pair, other, new Containment()));
                } else {
                    final TimePairInter pair = (TimePairInter) other.getEnsemble();
                    tasks.add(new LinkTask(theSig, pair, tn, new Containment()));
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
     * Specifically, various links are searched for: time partial signature, multi-measure rest,
     * measure repeat sign.
     */
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final Rectangle box = getBounds();
        final Point center = getCenter();
        final Staff theStaff = system.getClosestStaff(center);

        if ((theStaff != null) && !theStaff.isTablature()) {
            if (isInside(theStaff, box)) {
                // If located "inside" staff, this must be a TimeNumberInter.
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
                // Otherwise, it can be a MeasureCountInter and we look into the staff nearby
                // for a multi-measure rest or a measure repeat sign
                return MeasureCountInter.lookupLinks(shape, getCenter(), system);
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
        final Staff theStaff = system.getClosestStaff(center);

        if (!theStaff.isTablature() && theStaff.contains(center)) {
            final double pp = theStaff.pitchPositionOf(center);
            if (TimeNumberInter.isPitchValid(pp)) {
                return pp < 0 ? VerticalSide.TOP : VerticalSide.BOTTOM;
            }
        }

        return null;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a NumberInter.
     * <p>
     * We simply check that glyph is located vertically not too far from staff.
     *
     * @param glyph underlying glyph
     * @param shape precise shape (a TIME shape)
     * @param grade evaluation value
     * @param staff related staff
     * @return the created instance or null if failed
     */
    public static NumberInter create (Glyph glyph,
                                      Shape shape,
                                      double grade,
                                      Staff staff)
    {
        if (staff.isTablature()) {
            return null;
        }

        final Point centroid = glyph.getCentroid();
        final double pitch = staff.pitchPositionOf(centroid);

        if (Math.abs(pitch) > constants.maxAbsolutePitch.getValue()) {
            return null;
        }

        final NumberInter number = new NumberInter(glyph, shape, grade);
        number.setStaff(staff);
        number.setAbnormal(true);

        return number;
    }

    //----------//
    // isInside //
    //----------//
    /**
     * Check whether the provided item bounds is "inside" staff height and likely to represent
     * a time number.
     * For a 1-line staff, "inside" means vertically very close to the line.
     *
     * @param staff  the related staff
     * @param bounds the item bounds
     * @return true if considered inside
     */
    private static boolean isInside (Staff staff,
                                     Rectangle bounds)
    {
        final Rectangle box = new Rectangle(bounds); // Otherwise bounds would be modified
        final Point center = GeoUtil.center(box);

        if (staff.isOneLineStaff()) {
            // Containment is tricky for a 1-line staff
            // Bounds for a partial time are likely to nearly touch the mid-line
            // as opposed to a measure count likely to be located farther away
            final double midY = staff.getMidLine().yAt(center.getX());
            final Scale scale = staff.getSystem().getSheet().getScale();
            final double maxDy = scale.toPixels(constants.oneLineMaxTimeDy);
            box.grow(0, (int) Math.rint(maxDy));

            return box.contains(new Point2D.Double(center.getX(), midY));
        } else {
            return staff.contains(center);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Double maxAbsolutePitch = new Constant.Double(
                "pitch",
                10,
                "Maximum absolute pitch value for a number");

        private final Scale.Fraction oneLineMaxTimeDy = new Scale.Fraction(
                0.25,
                "Maximum vertical gap between a partial time and a 1-line staff");
    }
}
