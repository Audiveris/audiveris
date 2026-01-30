//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              L e g g e d I n t e r s B u i l d e r                             //
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
package org.audiveris.omr.sheet.curve;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.dynamic.FilamentFactory;
import org.audiveris.omr.glyph.dynamic.StraightFilament;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.lag.Sections;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class <code>LeggedIntersBuilder</code> is the basis for building legged inters
 * (endings or rehearsals).
 * <ul>
 * <li>Ending: 1 horizontal segment and 1 or 2 vertical legs
 * <li>Rehearsal : 2 horizontal segments and 2 vertical legs
 * </ul>
 *
 * @author Hervé Bitteur
 */
public abstract class LeggedIntersBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(EndingsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The related sheet. */
    @Navigable(false)
    protected final Sheet sheet;

    /** Curves environment. */
    protected final Curves curves;

    /** Scale-dependent parameters. */
    protected final Parameters params;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new LeggedIntersBuilder object.
     *
     * @param curves curves environment
     */
    public LeggedIntersBuilder (Curves curves)
    {
        this.curves = curves;
        sheet = curves.getSheet();
        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // lookupLeg //
    //-----------//
    /**
     * Look for a vertical leg near the chosen point on the horizontal segment.
     *
     * @param seg     the horizontal segment
     * @param segPt   chosen point on segment
     * @param bottomY max ordinate for leg bottom
     * @param system  related system
     * @return the best seed found or null if none.
     */
    protected StraightFilament lookupLeg (SegmentInfo seg,
                                          Point segPt,
                                          int bottomY,
                                          SystemInfo system)
    {
        final Rectangle box = new Rectangle(
                segPt.x - params.maxLegXGap,
                segPt.y + params.legYMargin,
                2 * params.maxLegXGap,
                bottomY - segPt.y - (2 * params.legYMargin));

        final Set<Section> sections = Sections.intersectedSections(
                box,
                system.getVerticalSections());

        // Adjust factory parameters
        final Scale scale = sheet.getScale();
        final FilamentFactory<StraightFilament> factory = new FilamentFactory<>(
                scale,
                sheet.getFilamentIndex(),
                VERTICAL,
                StraightFilament.class);
        factory.setMaxThickness(
                (int) Math.ceil(sheet.getScale().getMaxStem() * constants.stemRatio.getValue()));
        factory.setMaxOverlapDeltaPos(constants.maxOverlapDeltaPos);
        factory.setMaxOverlapSpace(constants.maxOverlapSpace);
        factory.setMaxCoordGap(constants.maxCoordGap);

        if (system.getId() == 1) {
            factory.dump("LeggedIntersBuilder factory");
        }

        // Retrieve candidates
        final List<StraightFilament> filaments = factory.retrieveFilaments(sections);

        // Purge filaments
        for (Iterator<StraightFilament> it = filaments.iterator(); it.hasNext();) {
            final StraightFilament fil = it.next();

            if ((fil.getLength(VERTICAL) < params.minLegLow) //
                    || ((fil.getStartPoint().getY() - segPt.y) > params.maxLegYGap)) {
                it.remove();
            }
        }

        if (filaments.isEmpty()) {
            return null;
        }

        // Choose the seed whose top end is closest to segment end
        StraightFilament bestFil = null;
        double bestDistSq = Double.MAX_VALUE;

        for (StraightFilament filament : filaments) {
            Point2D top = filament.getStartPoint();
            double dx = top.getX() - segPt.getX();
            double dy = top.getY() - segPt.getY();
            double distSq = (dx * dx) + (dy * dy);

            if ((bestFil == null) || (bestDistSq > distSq)) {
                bestDistSq = distSq;
                bestFil = filament;
            }
        }

        return bestFil;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction minLengthLow = new Scale.Fraction(
                6,
                "Low minimum ending length");

        private final Scale.Fraction minLengthHigh = new Scale.Fraction(
                10,
                "High minimum ending length");

        private final Scale.Fraction minLegLow = new Scale.Fraction( //
                1.0,
                "Low minimum leg length");

        private final Scale.Fraction minGapFromStaff = new Scale.Fraction(
                2.5,
                "Minimum vertical gap between ending line and staff below");

        private final Scale.Fraction legYMargin = new Scale.Fraction(
                0.25,
                "Vertical margin for leg lookup area");

        private final Scale.Fraction maxLegXGap = new Scale.Fraction(
                0.5,
                "Maximum abscissa gap between ending and leg");

        private final Scale.Fraction maxLegYGap = new Scale.Fraction(
                0.5,
                "Maximum ordinate gap between ending and leg");

        private final Constant.Double maxSlope = new Constant.Double(
                "tangent",
                0.02,
                "Maximum ending slope");

        private final Constant.Ratio stemRatio = new Constant.Ratio(
                1.4,
                "Maximum leg thickness as ratio of stem thickness");

        private final Scale.LineFraction maxOverlapDeltaPos = new Scale.LineFraction(
                1.0,
                "Maximum delta position between two overlapping filaments");

        private final Scale.LineFraction maxOverlapSpace = new Scale.LineFraction(
                0.3,
                "Maximum space between overlapping filaments");

        private final Scale.Fraction maxCoordGap = new Scale.Fraction(
                0.5,
                "Maximum delta coordinate for a gap between filaments");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * All pre-scaled constants.
     */
    protected static class Parameters
    {
        final int minLengthLow;

        final int minLengthHigh;

        final int minLegLow;

        final int minGapFromStaff;

        final int legYMargin;

        final int maxLegXGap;

        final int maxLegYGap;

        final double maxSlope;

        Parameters (Scale scale)
        {
            minLengthLow = scale.toPixels(constants.minLengthLow);
            minLengthHigh = scale.toPixels(constants.minLengthHigh);
            minLegLow = scale.toPixels(constants.minLegLow);
            minGapFromStaff = scale.toPixels(constants.minGapFromStaff);
            legYMargin = scale.toPixels(constants.legYMargin);
            maxLegXGap = scale.toPixels(constants.maxLegXGap);
            maxLegYGap = scale.toPixels(constants.maxLegYGap);
            maxSlope = constants.maxSlope.getValue();
        }
    }
}
