//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        R e s t I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code RestInter} represents a rest note.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "rest")
public class RestInter
        extends AbstractNoteInter
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(RestInter.class);

    /**
     * Creates a new RestInter object.
     *
     * @param glyph underlying glyph
     * @param shape rest shape
     * @param grade evaluation value
     * @param staff the related staff
     * @param pitch the rest pitch
     */
    public RestInter (Glyph glyph,
                      Shape shape,
                      Double grade,
                      Staff staff,
                      Double pitch)
    {
        super(glyph, null, shape, grade, staff, pitch);
    }

    /**
     * No-arg constructor meant for JAXB (and dummy measure).
     */
    protected RestInter ()
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

    //----------//
    // getChord //
    //----------//
    /**
     * Report the containing rest chord.
     *
     * @return containing rest chord
     */
    @Override
    public RestChordInter getChord ()
    {
        return (RestChordInter) getEnsemble();
    }

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel)
    {
        // Standard addition task for this rest
        final List<UITask> tasks = new ArrayList<>(super.preAdd(cancel));

        // Wrap this rest within a rest chord
        final RestChordInter restChord = new RestChordInter(null);
        restChord.setManual(true);
        restChord.setStaff(staff);

        tasks.add(new AdditionTask(
                staff.getSystem().getSig(),
                restChord,
                getBounds(),
                Arrays.asList(new Link(this, new Containment(), true))));

        return tasks;
    }

    //-------------//
    // createValid //
    //-------------//
    /**
     * (Try to) create a Rest inter.
     * <p>
     * Most rests, whatever their shape, lie very close to staff middle line.
     * Rests can lie far from staff middle line only when they are horizontally inserted between
     * head-chords that also lie outside of staff height.
     * The exception is the whole rest for which there should be no neighbors.
     * <p>
     * Also, a rest cannot be too close abscissa-wise to a head-chord.
     *
     * @param glyph            underlying glyph
     * @param shape            precise shape
     * @param grade            evaluation value
     * @param system           the related system
     * @param systemHeadChords abscissa-ordered list of head-chords in this system
     * @return the created instance or null if failed
     */
    public static RestInter createValid (Glyph glyph,
                                         Shape shape,
                                         double grade,
                                         SystemInfo system,
                                         List<Inter> systemHeadChords)
    {
        // Determine pitch according to glyph centroid
        final Point centroid = glyph.getCentroid();
        final Rectangle glyphBox = glyph.getBounds();

        // Rest positioning
        Staff restStaff = null;
        Double restPitch = null;
        List<Inter> measureChords = null;

        // First, check ordinate
        StaffLoop:
        for (final Staff staff : system.getStavesAround(centroid)) {
            final Measure measure = staff.getPart().getMeasureAt(centroid);

            if (measure == null) {
                continue;
            }

            // All head-chords in measure
            final int left = measure.getAbscissa(HorizontalSide.LEFT, staff);
            final int right = measure.getAbscissa(HorizontalSide.RIGHT, staff);
            measureChords = Inters.inters(
                    systemHeadChords,
                    new Predicate<Inter>()
            {
                @Override
                public boolean test (Inter inter)
                {
                    if (inter.getStaff() != staff) {
                        return false;
                    }

                    final Point center = inter.getCenter();

                    return (center.x >= left) && (center.x <= right);
                }
            });

            // Pitch value WRT staff
            final double measuredPitch = staff.pitchPositionOf(centroid);

            // Good rest, close to staff middle?
            if (Math.abs(measuredPitch) <= constants.suspiciousPitchPosition.getValue()) {
                restStaff = staff;
                restPitch = measuredPitch;

                break;
            }

            // Not so good rest, look for head chords nearby (in the same staff measure)
            if (shape != Shape.WHOLE_REST) {
                for (Inter inter : measureChords) {
                    if (GeoUtil.yOverlap(inter.getBounds(), glyphBox) > 0) {
                        restStaff = staff;
                        restPitch = measuredPitch;

                        break StaffLoop;
                    }
                }
            } else {
                // Special case for WHOLE_REST
                Staff closestStaff = system.getClosestStaff(centroid);

                if (staff == closestStaff) {
                    boolean overlap = false;

                    for (Inter inter : measureChords) {
                        if (GeoUtil.yOverlap(inter.getBounds(), glyphBox) > 0) {
                            overlap = true;
                        }
                    }

                    if (!overlap) {
                        restStaff = staff;
                        restPitch = measuredPitch;

                        break;
                    }
                }
            }
        }

        // Check vertical position is OK
        if (restStaff == null) {
            if (glyph.isVip() || logger.isDebugEnabled()) {
                logger.info("Discarded isolated rest candidate glyph#{}", glyph.getId());
            }

            return null; // Failure
        }

        // Check horizontal position WRT head-chords
        final int minDx = system.getSheet().getScale().toPixels(constants.minInterChordDx);
        final Point2D restCenter = GeoUtil.center2D(glyphBox);
        final Rectangle fatBox = new Rectangle(glyphBox);
        fatBox.grow(minDx, 0);

        for (Inter chord : measureChords) {
            if (fatBox.intersects(chord.getBounds())) {
                double dx = chord.getCenter2D().getX() - restCenter.getX();

                if (Math.abs(dx) < minDx) {
                    if (glyph.isVip() || logger.isDebugEnabled()) {
                        logger.info("Discarded stuck rest candidate glyph#{}", glyph.getId());
                    }

                    return null; // Failure
                }
            }
        }

        // Everything is OK
        RestInter restInter = new RestInter(glyph, shape, grade, restStaff, restPitch);
        restStaff.addNote(restInter);

        return restInter;
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Double suspiciousPitchPosition = new Constant.Double("PitchPosition",
                                                                                    2.0,
                                                                                    "Maximum absolute pitch position for a rest to avoid additional checks");

        private final Scale.Fraction minInterChordDx = new Scale.Fraction(0.5,
                                                                          "Minimum horizontal delta between two chords");
    }
}
