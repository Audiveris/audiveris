//----------------------------------------------------------------------------//
//                                                                            //
//                            B e a m s B u i l d e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.math.Barycenter;
import omr.math.BasicLine;
import omr.math.GeoUtil;
import omr.math.Line;
import omr.math.Line.NonInvertibleLineException;

import omr.run.Run;

import omr.sig.BeamInter;
import omr.sig.SIGraph;

import omr.util.Navigable;
import omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code BeamsBuilder} is in charge, at system info level, of
 * retrieving the possible beam interpretations.
 *
 * @author Hervé Bitteur
 */
public class BeamsBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            BeamsBuilder.class);

    //~ Instance fields --------------------------------------------------------
    /** The dedicated system */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    private final SIGraph sig;

    /** The related sheet */
    @Navigable(false)
    private final Sheet sheet;

    /** Sheet scale */
    @Navigable(false)
    private final Scale scale;

    /** Scale-dependent constants. */
    private final Parameters params;

    /** Spots for this system. */
    private List<Glyph> spots;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // BeamsBuilder //
    //--------------//
    /**
     * Creates a new BeamsBuilder object.
     *
     * @param system the dedicated system
     */
    public BeamsBuilder (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();
        sheet = system.getSheet();
        scale = sheet.getScale();
        params = new Parameters(sheet.getScale());
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // buildBeams //
    //------------//
    /**
     * Find possible interpretations of beams among system spots.
     */
    public void buildBeams ()
    {
        spots = getSpots();

        // Look for beam candidates
        List<Glyph> beams = new ArrayList<Glyph>();
        logger.debug(
                "{}S#{} spots: {}",
                sheet.getLogPrefix(),
                system.getId(),
                spots.size());

        GlyphLoop:
        for (Glyph glyph : spots) {
            if (glyph.isVip()) {
                logger.info("BINGO buildbeams {}", glyph);
            }

            final Rectangle box = glyph.getBounds();
            final Line glyphLine = glyph.getLine();

            // Minimum width
            if (box.width < params.minBeamWidth) {
                continue GlyphLoop;
            }

            // Maximum slope. Does this work OK with double beam of diff length?
            try {
                if (Math.abs(glyphLine.getSlope()) > params.maxBeamSlope) {
                    continue GlyphLoop;
                }
            } catch (Exception ignored) {
                continue GlyphLoop; // Line is vertical!
            }

            // Check straight lines from north and south borders
            int sumPoints = 0;
            double sumDist = 0;

            final Map<VerticalSide, List<BasicLine>> borders;
            borders = new TreeMap<VerticalSide, List<BasicLine>>();

            for (VerticalSide side : VerticalSide.values()) {
                List<BasicLine> lines = getBorderLines(glyph, side);
                borders.put(side, lines);

                for (BasicLine border : lines) {
                    double xMid = (border.getMinAbscissa()
                                   + border.getMaxAbscissa()) / 2;

                    try {
                        int dy = (int) Math.rint(
                                border.yAtX(xMid) - glyphLine.yAtX(xMid));
                        logger.debug(
                                "Beam#{} {} dy:{} points:{} dist:{} slope:{}",
                                glyph.getId(),
                                side,
                                dy,
                                border.getNumberOfPoints(),
                                String.format("%.2f", border.getMeanDistance()),
                                String.format("%.2f", border.getSlope()));
                    } catch (NonInvertibleLineException ignored) {
                        continue GlyphLoop;
                    }

                    sumPoints += border.getNumberOfPoints();
                    sumDist += (border.getMeanDistance() * border.getNumberOfPoints());
                }
            }

            final double globalDist = sumDist / sumPoints;
            logger.debug(
                    "Beam#{} globalDist:{}",
                    glyph.getId(),
                    String.format("%.2f", globalDist));

            if (globalDist > params.maxDistanceToBorder) {
                continue GlyphLoop;
            }

            final int topCount = borders.get(VerticalSide.TOP)
                    .size();
            final int botCount = borders.get(VerticalSide.BOTTOM)
                    .size();

            if (topCount != botCount) {
                // Check cases like this one!
                logger.debug(
                        "Strange beam(s) at glyph#{} topBorders:{} bottomBorders:{}",
                        glyph.getId(),
                        topCount,
                        botCount);

                continue GlyphLoop;
            }

            // Check borders are rather parallel (similar angle)
            double globalDeltaSlope = 0;

            for (int i = 0; i < topCount; i++) {
                double top = borders.get(VerticalSide.TOP)
                        .get(i)
                        .getSlope();
                double bottom = borders.get(VerticalSide.BOTTOM)
                        .get(i)
                        .getSlope();
                double deltaSlope = Math.abs(bottom - top);

                if (deltaSlope > params.maxDeltaSlope) {
                    logger.debug("Beam#{} Non parallel borders", glyph.getId());

                    continue GlyphLoop;
                }

                globalDeltaSlope += deltaSlope;
            }

            globalDeltaSlope /= topCount;

            // Compute grade for beam candidate
            double distImpact = 1 - (globalDist / params.maxDistanceToBorder);
            double widthImpact = Math.min(
                    1,
                    (box.width - params.minBeamWidth) / params.minLargeBeamWidth);
            double deltaSlopeImpact = 1
                                      - (globalDeltaSlope / params.maxDeltaSlope);

            double grade = (distImpact + widthImpact + deltaSlopeImpact) / 3;

            // Build each precise beam path
            for (int i = 0; i < topCount; i++) {
                BasicLine top = borders.get(VerticalSide.TOP)
                        .get(i);
                Line2D north = new Line2D.Double(
                        top.getMinAbscissa(),
                        top.yAtX(top.getMinAbscissa()),
                        top.getMaxAbscissa(),
                        top.yAtX(top.getMaxAbscissa()));
                BasicLine bottom = borders.get(VerticalSide.BOTTOM)
                        .get(i);
                Line2D south = new Line2D.Double(
                        bottom.getMinAbscissa(),
                        bottom.yAtX(bottom.getMinAbscissa()),
                        bottom.getMaxAbscissa(),
                        bottom.yAtX(bottom.getMaxAbscissa()));
                sig.addVertex(new BeamInter(glyph, grade, north, south));
            }

            glyph.setShape(Shape.BEAM); // For visual check
            beams.add(glyph);
        }

        logger.debug(
                "{}S#{} beams: {}",
                sheet.getLogPrefix(),
                system.getId(),
                beams.size());
    }

    //-------------//
    // drawBorders //
    //-------------//
    /**
     * Really dirty hack.
     *
     * @param glyph
     * @param g
     */
    public void drawBorders (Glyph glyph,
                             Graphics2D g)
    {
        Rectangle box = glyph.getBounds();

        for (VerticalSide side : VerticalSide.values()) {
            try {
                for (BasicLine line : getBorderLines(glyph, side)) {
                    if (line.getNumberOfPoints() > 1) {
                        g.drawLine(
                                box.x,
                                line.yAtX(box.x)
                                + ((side == VerticalSide.BOTTOM) ? 1 : 0),
                                box.x + box.width,
                                line.yAtX(box.x + box.width)
                                + ((side == VerticalSide.BOTTOM) ? 1 : 0));
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    //----------------//
    // getBorderLines //
    //----------------//
    /**
     * Compute the lines that approximates the border on desired side.
     * There can be several lines on a given side if the glyph represents a
     * double beam or larger. So, we group top section borders by beam height
     * increments, and do the same for bottom section borders.
     *
     * @param glyph spot to analyze
     * @param side  TOP or BOTTOM
     * @return the computed line(s)
     */
    private List<BasicLine> getBorderLines (Glyph glyph,
                                            VerticalSide side)
    {
        // All sections are vertical, retrieve their top (or bottom) border
        final Rectangle glyphBox = glyph.getBounds();
        final Line glyphLine = glyph.getLine();
        final int beamHeight = scale.getMainBeam();
        final int xMin = glyphBox.x;
        final int xMax = (glyphBox.x + glyphBox.width) - 1;
        List<SectionBorder> sectionBorders = new ArrayList<SectionBorder>();

        for (Section section : glyph.getMembers()) {
            final BasicLine sectionLine = new BasicLine();

            int x = section.getFirstPos();

            for (Run run : section.getRuns()) {
                if ((x >= xMin) && (x <= xMax)) {
                    if (side == VerticalSide.TOP) {
                        sectionLine.includePoint(x, run.getStart());
                    } else {
                        sectionLine.includePoint(x, run.getStop());
                    }
                }

                x++;
            }

            // Discard too narrow sections
            if (sectionLine.getNumberOfPoints() >= 3) {
                final Rectangle box = section.getBounds();
                final int center = GeoUtil.centerOf(box).x;
                final int y = sectionLine.yAtX(center);
                final int dy = y - glyphLine.yAtX(center);
                sectionBorders.add(new SectionBorder(section, sectionLine, dy));
            }
        }

        Collections.sort(sectionBorders);

        // Retrieve groups of dy values, roughly separated by beam height
        // Each group will correspond to a beam
        final double delta = beamHeight * 0.75;
        final List<BasicLine> beamBorders = new ArrayList<BasicLine>();
        Barycenter dys = new Barycenter();
        BasicLine line = null;

        for (SectionBorder border : sectionBorders) {
            ///logger.info("{}", border);
            if ((dys.getWeight() > 0) && ((border.dy - dys.getY()) <= delta)) {
                dys.include(border.line.getNumberOfPoints(), 0, border.dy);
            } else {
                beamBorders.add(line = new BasicLine());
                dys = new Barycenter();
                dys.include(border.line.getNumberOfPoints(), 0, border.dy);
            }

            line.includeLine(border.line);
        }

        return beamBorders;
    }

    //----------//
    // getSpots //
    //----------//
    private List<Glyph> getSpots ()
    {
        // Spots for this system
        final List<Glyph> spots = new ArrayList<Glyph>();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == Shape.BEAM_SPOT) {
                spots.add(glyph);
            }
        }

        return spots;
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        final Scale.Fraction minBeamWidth = new Scale.Fraction(
                1.7,
                "Minimum width for a beam");

        final Scale.Fraction minLargeBeamWidth = new Scale.Fraction(
                4.0,
                "Minimum width for a large beam");

        final Constant.Double maxBeamTangent = new Constant.Double(
                "tangent",
                1.0,
                "Maximum absolute tangent value for a beam angle");

        final Constant.Double maxDeltaTangent = new Constant.Double(
                "tangent",
                0.06,
                "Maximum delta slope between top and bottom of beam");

        final Scale.Fraction maxDistanceToBorder = new Scale.Fraction(
                0.08,
                "Maximum mean distance to average beam border");

    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all pre-scaled constants.
     */
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        final int minBeamWidth;

        final double minLargeBeamWidth;

        final double maxBeamSlope;

        final double maxDeltaSlope;

        final double maxDistanceToBorder;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            minBeamWidth = scale.toPixels(constants.minBeamWidth);
            minLargeBeamWidth = scale.toPixels(constants.minLargeBeamWidth);
            maxBeamSlope = constants.maxBeamTangent.getValue();
            maxDeltaSlope = constants.maxDeltaTangent.getValue();
            maxDistanceToBorder = scale.toPixelsDouble(
                    constants.maxDistanceToBorder);

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }

    //---------------//
    // SectionBorder //
    //---------------//
    private static class SectionBorder
            implements Comparable<SectionBorder>
    {
        //~ Instance fields ----------------------------------------------------

        final Section section;

        final BasicLine line;

        final int dy;

        //~ Constructors -------------------------------------------------------
        public SectionBorder (Section section,
                              BasicLine line,
                              int dy)
        {
            this.section = section;
            this.line = line;
            this.dy = dy;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public int compareTo (SectionBorder that)
        {
            // Sort by increasing ordinate
            return Integer.compare(this.dy, that.dy);
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{");
            sb.append(getClass().getSimpleName());

            sb.append(" dy:")
                    .append(dy);
            sb.append(" lg:")
                    .append(line.getNumberOfPoints());
            sb.append(" line:")
                    .append(line);
            sb.append(" section:")
                    .append(section);

            sb.append("}");

            return sb.toString();
        }
    }
}
