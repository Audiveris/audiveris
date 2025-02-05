//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                M e a s u r e C o u n t I n t e r                               //
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
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.MeasureRepeatCountRelation;
import org.audiveris.omr.sig.relation.MultipleRestCountRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>MeasureCountInter</code> represents a count of measures
 * for a {@link MultipleRestInter} linked via a {@link MultipleRestCountRelation} or
 * for a {@link MeasureRepeatInter} linked via a {@link MeasureRepeatCountRelation}.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "measure-count")
public class MeasureCountInter
        extends AbstractNumberInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(MeasureCountInter.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private MeasureCountInter ()
    {
        super((Glyph) null, (Integer) null, 0.0);
    }

    /**
     * Creates a new MeasureCountInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public MeasureCountInter (Glyph glyph,
                              Shape shape,
                              Double grade)
    {
        super(glyph, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if this measure count is connected to a multiple rest or a measure repeat sign
        setAbnormal(
                !sig.hasRelation(this, MultipleRestCountRelation.class) //
                        && !sig.hasRelation(this, MeasureRepeatCountRelation.class));

        return isAbnormal();
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        return lookupLinks(shape, getCenter(), system);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------//
    // lookupLinks //
    //-------------//
    /**
     * Try to detect links between MeasureCountInter location and MultipleRestInter
     * or MeasureRepeatInter instances nearby.
     *
     * @param countShape measure count shape, perhaps null
     * @param center     location of number
     * @param system     the containing system
     * @return the links found, perhaps empty
     */
    public static List<Link> lookupLinks (Shape countShape,
                                          Point2D center,
                                          SystemInfo system)
    {
        final Staff theStaff = system.getClosestStaff(center);
        if ((theStaff == null) || theStaff.isTablature()) {
            return Collections.emptyList();
        }

        final double pitch = theStaff.pitchPositionOf(center);
        if (Math.abs(pitch) > constants.maxAbsolutePitch.getValue()) {
            return Collections.emptyList();
        }

        final SIGraph sig = system.getSig();
        final List<Link> links = new ArrayList<>();

        // Multiple measure rest?
        final List<Inter> multipleRests = sig.inters(MultipleRestInter.class);
        for (Inter mRest : multipleRests) {
            if (mRest.getStaff() == theStaff) {
                final Rectangle mRestBox = mRest.getBounds();

                if (center.getX() >= mRestBox.x && center.getX() < mRestBox.x + mRestBox.width) {
                    final MultipleRestCountRelation rel = new MultipleRestCountRelation();
                    links.add(new Link(mRest, rel, false));
                }
            }
        }

        // Measure repeat?
        final List<Inter> repeats = sig.inters(MeasureRepeatInter.class);
        final Integer countValue = valueOf(countShape);
        for (Inter inter : repeats) {
            final MeasureRepeatInter repeat = (MeasureRepeatInter) inter;
            if (repeat.getStaff() == theStaff) {
                final Rectangle simBox = repeat.getBounds();

                if (center.getX() >= simBox.x && center.getX() < simBox.x + simBox.width) {
                    // Check consistency between measure count and repeat sign slashes
                    if ((countValue != null) && (countValue != 0)) {
                        final Shape simShape = repeat.getShape();

                        if (simShape != null) {
                            final int simValue = simShape.getSlashCount();
                            if (simValue != countValue) {
                                logger.debug("Non consistent count:{} {}", countValue, repeat);
                                continue;
                            }
                        }
                    }

                    final MeasureRepeatCountRelation rel = new MeasureRepeatCountRelation();
                    links.add(new Link(repeat, rel, false));
                }
            }
        }

        return links;
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
