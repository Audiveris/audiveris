//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             M u l t i p l e R e s t s B u i l d e r                            //
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
package org.audiveris.omr.sheet.beam;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.dynamic.Filament;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.Lags;
import org.audiveris.omr.lag.Section;
import static org.audiveris.omr.run.Orientation.HORIZONTAL;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.BarFilamentBuilder;
import org.audiveris.omr.sheet.grid.StaffPeak;
import org.audiveris.omr.sheet.grid.StaffProjector;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.MultipleRestInter;
import org.audiveris.omr.sig.inter.VerticalSerifInter;
import org.audiveris.omr.sig.relation.MultipleRestSerifRelation;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class <code>MultipleRestsBuilder</code> is in charge at system level to detect among the
 * freshly created beams, which ones are in fact multiple rests.
 *
 * @author Hervé Bitteur
 */
public class MultipleRestsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(MultipleRestsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** Containing sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Projections of system staves. */
    private final Map<Staff, StaffProjector> projectors = new HashMap<>();

    /** Scale-dependent global parameters. */
    private final Parameters params;

    /** Suitable candidates found for multiple rest in this system. */
    private final Map<BeamInter, Map<HorizontalSide, StaffPeak>> found = new LinkedHashMap<>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new MultipleRestsBuilder object.
     *
     * @param system the dedicated system
     */
    public MultipleRestsBuilder (SystemInfo system)
    {
        this.system = system;

        sheet = system.getSheet();
        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // createInters //
    //--------------//
    private void createInters ()
    {
        final SIGraph sig = system.getSig();
        final double halfLine = sheet.getScale().getFore() / 2.0;
        final int maxWidth = getMaxSerifWidth();
        final List<Section> allSections = getSectionsByWidth(maxWidth);
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();
        final BarFilamentBuilder filamentBuilder = new BarFilamentBuilder(sheet);

        for (Entry<BeamInter, Map<HorizontalSide, StaffPeak>> entry : found.entrySet()) {
            final BeamInter beam = entry.getKey();

            // Create multiple rest
            final MultipleRestInter multipleRest = new MultipleRestInter(
                    beam.getGrade(),
                    beam.getMedian(),
                    beam.getHeight());
            sig.addVertex(multipleRest);
            multipleRest.setStaff(system.getStaffAtOrAbove(beam.getCenter()));

            // Create and link each vertical serif
            final Map<HorizontalSide, StaffPeak> sidePeaks = entry.getValue();
            for (HorizontalSide side : HorizontalSide.values()) {
                final StaffPeak peak = sidePeaks.get(side);
                final Filament filament = filamentBuilder.buildFilament(peak, 0, allSections);
                final Glyph glyph = glyphIndex.registerOriginal(filament.toGlyph(null));
                final double x = peak.getStart() + (peak.getWidth() / 2d);
                final Line2D median = new Line2D.Double(
                        x,
                        peak.getTop() - halfLine + 0.5,
                        x,
                        peak.getBottom() + halfLine + 0.5);
                final VerticalSerifInter serif = new VerticalSerifInter(
                        glyph,
                        peak.getImpacts(),
                        median,
                        (double) peak.getWidth());
                sig.addVertex(serif);

                final MultipleRestSerifRelation rel = new MultipleRestSerifRelation();
                rel.setRestSide(side);
                sig.addEdge(multipleRest, serif, rel);
            }

            logger.debug("Detected {}", multipleRest);
            // NOTA: Measure number will be handled later
        }
    }

    //------------------//
    // getMaxSerifWidth //
    //------------------//
    /**
     * Retrieve the maximum width among all serif peaks.
     *
     * @return the maximum width of any serif (in this system)
     */
    private int getMaxSerifWidth ()
    {
        int maxWidth = 0;

        for (Map<HorizontalSide, StaffPeak> sidePeaks : found.values()) {
            for (StaffPeak peak : sidePeaks.values()) {
                maxWidth = Math.max(maxWidth, peak.getWidth());
            }
        }

        return maxWidth;
    }

    //--------------//
    // getProjector //
    //--------------//
    private StaffProjector getProjector (Staff staff)
    {
        StaffProjector projector = projectors.get(staff);

        if (projector == null) {
            projectors.put(staff, projector = new StaffProjector(sheet, staff, null));
        }

        return projector;
    }

    //--------------------//
    // getSectionsByWidth //
    //--------------------//
    /**
     * Select relevant sections for serif sticks in this system.
     * <p>
     * Both vertical and horizontal sections are OK if they are not wider than the maximum allowed.
     * The collection is sorted on abscissa.
     *
     * @param maxWidth maximum section horizontal width
     * @return the abscissa-sorted list of compliant sections
     */
    private List<Section> getSectionsByWidth (int maxWidth)
    {
        final List<Section> sections = new ArrayList<>();
        final Lag hLag = sheet.getLagManager().getLag(Lags.HLAG);
        final Lag vLag = sheet.getLagManager().getLag(Lags.VLAG);
        final Rectangle systemBox = system.getBounds();

        for (Lag lag : Arrays.asList(vLag, hLag)) {
            for (Section section : lag.getEntities()) {
                if ((section.getLength(HORIZONTAL) <= maxWidth) && section.getBounds().intersects(
                        systemBox)) {
                    sections.add(section);
                }
            }
        }

        Collections.sort(sections, Section.byAbscissa);

        return sections;
    }

    //---------------//
    // getSerifPeaks //
    //---------------//
    /**
     * Check presence of serif's on left and right side of the multiple rest "beam".
     * <p>
     * A vertical serif is not exactly a stem:
     * <ul>
     * <li>its width can be larger than a stem width
     * <li>its height is about half the staff height, smaller than a typical stem
     * </ul>
     * Strategy is to use the StaffProjector around the left side and the right side of the beam.
     * A derivative hi-lo should point to the serif abscissa range, and then we have to check that
     * serif pixels are located around staff mid line.
     *
     * @param beam  the beam candidate
     * @param staff the surrounding staff
     * @return the serif peak found at each beam side
     */
    private Map<HorizontalSide, StaffPeak> getSerifPeaks (BeamInter beam,
                                                          Staff staff)
    {
        final Map<HorizontalSide, StaffPeak> serifPeaks = new EnumMap<>(HorizontalSide.class);
        final StaffProjector projector = getProjector(staff);
        final Rectangle bounds = beam.getBounds();
        final int addedChunk = (int) Math.rint(
                beam.getHeight() - staff.getMidLine().getThickness());

        for (HorizontalSide side : HorizontalSide.values()) {
            final int x = (side == HorizontalSide.LEFT) ? bounds.x : bounds.x + bounds.width - 1;
            final List<StaffPeak> peaks = projector.processMultiRestSide(x, side, addedChunk);
            logger.debug("{} {} {}", beam, side, peaks);

            if (!peaks.isEmpty()) {
                final StaffPeak p = peaks.get(side == HorizontalSide.LEFT ? peaks.size() - 1 : 0);
                serifPeaks.put(side, p);
            }
        }

        return serifPeaks;
    }

    //---------//
    // process //
    //---------//
    /**
     * Check every beam as a potential multiple rest.
     * <ul>
     * <li>Minimum beam width
     * <li>Beam must be stuck on staff mid line
     * <li>Presence of proper vertical serifs on either horizontal side
     * </ul>
     * NOTA: At this point in time, we cannot yet check the presence above the staff of
     * the indicated number of measures concerned.
     */
    public void process ()
    {
        final SIGraph sig = system.getSig();
        final List<Inter> beams = sig.inters(BeamInter.class);

        for (Inter bi : beams) {
            final BeamInter beam = (BeamInter) bi;

            // Long enough?
            if (beam.getBounds().width < params.minLength) {
                continue;
            }

            final Point center = beam.getCenter();
            final Staff staff = system.getStaffAtOrAbove(center);
            if ((staff == null) || staff.isTablature()) {
                continue;
            }

            // Stuck horizontally on staff mid line?
            double p1 = staff.pitchPositionOf(beam.getMedian().getP1());
            if (Math.abs(p1) > constants.maxAbsolutePitch.getValue()) {
                continue;
            }

            double p2 = staff.pitchPositionOf(beam.getMedian().getP2());
            if (Math.abs(p2) > constants.maxAbsolutePitch.getValue()) {
                continue;
            }

            final Map<HorizontalSide, StaffPeak> sidePeaks = getSerifPeaks(beam, staff);
            if (sidePeaks.size() != 2) {
                continue;
            }

            // All tests are OK
            found.put(beam, sidePeaks);
        }

        if (!found.isEmpty()) {
            createInters();
            system.getSig().deleteInters(found.keySet());
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
                0.2,
                "Maximum absolute pitch position of beam center");

        private final Scale.Fraction minLength = new Scale.Fraction(
                4.0,
                "Minimum multirest length");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class <code>Parameters</code> gathers all pre-scaled constants.
     */
    private static class Parameters
    {
        final int minLength;

        Parameters (Scale scale)
        {
            minLength = scale.toPixels(constants.minLength);
        }
    }
}
