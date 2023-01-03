//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               M e a s u r e N u m b e r I n t e r                              //
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.MultipleRestNumberRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>MeasureNumberInter</code> represents the number of measures for a multiple rest,
 * linked via a MultipleRestNumberRelation.
 *
 * @see MultipleRestInter
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "measure-number")
public class MeasureNumberInter
        extends AbstractNumberInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(MeasureNumberInter.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new MeasureNumberInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public MeasureNumberInter (Glyph glyph,
                               Shape shape,
                               Double grade)
    {
        super(glyph, shape, grade);
    }

    /**
     * Creates a new MeasureNumberInter object.
     *
     * @param glyph underlying glyph
     * @param value numerical value
     * @param grade evaluation value
     */
    public MeasureNumberInter (Glyph glyph,
                               Integer value,
                               Double grade)
    {
        super(glyph, value, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private MeasureNumberInter ()
    {
        super((Glyph) null, (Integer) null, 0.0);
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

    //------------//
    // searchRest //
    //------------//
    /**
     * Look for a multiple rest compatible with provided number location.
     *
     * @param point location of measure number
     * @param staff target staff
     * @return the multiple rest found or null
     */
    public static MultipleRestInter searchRest (Point point,
                                                Staff staff)
    {
        return null;
    }

    //------------------//
    // createValidAdded //
    //------------------//
    /**
     * (Try to) create a MeasureNumberInter with a related multiple rest.
     *
     * @param glyph underlying glyph
     * @param shape precise shape (a TIME shape)
     * @param grade evaluation value
     * @param staff related staff
     * @return the created instance or null if failed
     */
    public static MeasureNumberInter createValidAdded (Glyph glyph,
                                                       Shape shape,
                                                       double grade,
                                                       Staff staff)
    {
        final Point centroid = glyph.getCentroid();

        // Look for a suitable multiple rest below
        final Link link = lookupLink(centroid, staff.getSystem());
        if (link == null) {
            return null;
        }

        final SIGraph sig = staff.getSystem().getSig();
        final MeasureNumberInter number = new MeasureNumberInter(glyph, shape, grade);
        number.setStaff(staff);
        sig.addVertex(number);

        final MultipleRestInter multipleRest = (MultipleRestInter) link.partner;
        sig.addEdge(multipleRest, number, link.relation);

        return number;
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "NUMBER_" + getValue();
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final Link link = lookupLink(getCenter(), system);

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between MeasureNumberInter location and a MultipleRestInter nearby.
     *
     * @param center location of number
     * @param system the containing system
     * @return the link found or null
     */
    private static Link lookupLink (Point2D center,
                                    SystemInfo system)
    {
        final List<Inter> multipleRests = system.getSig().inters(MultipleRestInter.class);
        Collections.sort(multipleRests, Inters.byAbscissa); // Not really needed
        if (multipleRests.isEmpty()) {
            return null;
        }

        final Staff theStaff = system.getStaffAtOrBelow(center);
        if (theStaff == null) {
            return null;
        }

        final double pitch = theStaff.pitchPositionOf(center);
        if (Math.abs(pitch) > constants.maxAbsolutePitch.getValue()) {
            return null;
        }

        for (Inter mRest : multipleRests) {
            if (mRest.getStaff() == theStaff) {
                final Rectangle mRestBox = mRest.getBounds();

                if (center.getX() >= mRestBox.x
                            && center.getX() < mRestBox.x + mRestBox.width) {
                    final MultipleRestNumberRelation rel = new MultipleRestNumberRelation();
                    return new Link(mRest, rel, false);
                }
            }
        }

        return null;
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
                "Maximum absolute pitch value for a measure number");
    }
}
