//----------------------------------------------------------------------------//
//                                                                            //
//                        B a s i c A l i g n m e n t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;

import omr.log.Logger;

import omr.math.BasicLine;
import omr.math.Line;

import omr.run.Orientation;
import omr.run.Run;

import omr.score.common.PixelPoint;

import omr.sheet.Scale;

import omr.stick.StickSection;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Class {@code BasicAlignment} implements a basic handling of Alignment facet
 *
 * @author Hervé Bitteur
 */
public class BasicAlignment
    extends BasicFacet
    implements GlyphAlignment
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BasicAlignment.class);

    //~ Instance fields --------------------------------------------------------

    /** Best (curved or straight) line equation */
    protected Line line;

    /** Absolute beginning point */
    protected Point2D pStart;

    /** Absolute ending point */
    protected Point2D pStop;

    //~ Constructors -----------------------------------------------------------

    /**
     * Create a new BasicAlignment object
     *
     * @param glyph our glyph
     */
    public BasicAlignment (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getAbsoluteLine //
    //-----------------//
    public Line getAbsoluteLine ()
    {
        return glyph.getLag()
                    .switchRef(getOrientedLine());
    }

    //------------------//
    // getAlienPixelsIn //
    //------------------//
    public int getAlienPixelsIn (Rectangle area)
    {
        int                      count = 0;
        final int                posMin = area.y;
        final int                posMax = (area.y + area.height) - 1;
        final List<GlyphSection> neighbors = glyph.getLag()
                                                  .getSectionsIn(area);

        for (GlyphSection section : neighbors) {
            // Keep only non-patch sections that are not part of the stick
            if (!section.isPatch() && (section.getGlyph() != glyph)) {
                int pos = section.getFirstPos() - 1; // Ordinate for horizontal,
                                                     // Abscissa for vertical

                for (Run run : section.getRuns()) {
                    pos++;

                    if (pos > posMax) {
                        break;
                    }

                    if (pos < posMin) {
                        continue;
                    }

                    int coordMin = Math.max(area.x, run.getStart());
                    int coordMax = Math.min(
                        (area.x + area.width) - 1,
                        run.getStop());

                    if (coordMax >= coordMin) {
                        count += (coordMax - coordMin + 1);
                    }
                }
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                "Stick" + glyph.getId() + " " + area + " getAlienPixelsIn=" +
                count);
        }

        return count;
    }

    //------------------//
    // getAliensAtStart //
    //------------------//
    public int getAliensAtStart (int dCoord,
                                 int dPos)
    {
        return getAlienPixelsIn(
            new Rectangle(
                getStart(),
                getStartingPos() - dPos,
                dCoord,
                2 * dPos));
    }

    //-----------------------//
    // getAliensAtStartFirst //
    //-----------------------//
    public int getAliensAtStartFirst (int dCoord,
                                      int dPos)
    {
        return getAlienPixelsIn(
            new Rectangle(getStart(), getStartingPos() - dPos, dCoord, dPos));
    }

    //----------------------//
    // getAliensAtStartLast //
    //----------------------//
    public int getAliensAtStartLast (int dCoord,
                                     int dPos)
    {
        return getAlienPixelsIn(
            new Rectangle(getStart(), getStartingPos(), dCoord, dPos));
    }

    //-----------------//
    // getAliensAtStop //
    //-----------------//
    public int getAliensAtStop (int dCoord,
                                int dPos)
    {
        return getAlienPixelsIn(
            new Rectangle(
                getStop() - dCoord,
                getStoppingPos() - dPos,
                dCoord,
                2 * dPos));
    }

    //----------------------//
    // getAliensAtStopFirst //
    //----------------------//
    public int getAliensAtStopFirst (int dCoord,
                                     int dPos)
    {
        return getAlienPixelsIn(
            new Rectangle(
                getStop() - dCoord,
                getStoppingPos() - dPos,
                dCoord,
                dPos));
    }

    //---------------------//
    // getAliensAtStopLast //
    //---------------------//
    public int getAliensAtStopLast (int dCoord,
                                    int dPos)
    {
        return getAlienPixelsIn(
            new Rectangle(getStop() - dCoord, getStoppingPos(), dCoord, dPos));
    }

    //-----------//
    // getAspect //
    //-----------//
    public double getAspect ()
    {
        return (double) getLength() / (double) getThickness();
    }

    //-----------------//
    // setEndingPoints //
    //-----------------//
    public void setEndingPoints (Point2D pStart,
                                 Point2D pStop)
    {
        glyph.invalidateCache();
        this.pStart = pStart;
        this.pStop = pStop;
    }

    //---------------//
    // isExtensionOf //
    //---------------//
    public boolean isExtensionOf (Stick that,
                                  int   maxDeltaCoord,
                                  int   maxDeltaPos)
    {
        GlyphLag lag = glyph.getLag();
        Point2D  thisStart = lag.oriented(this.getStartPoint());
        Point2D  thisStop = lag.oriented(this.getStopPoint());
        Point2D  thatStart = lag.oriented(that.getStartPoint());
        Point2D  thatStop = lag.oriented(that.getStopPoint());

        if (Math.abs(thisStop.getX() - thatStart.getX()) <= maxDeltaCoord) {
            // Case: this ... that
            if (Math.abs(thatStart.getY() - thisStop.getY()) <= maxDeltaPos) {
                return true;
            }
        }

        if (Math.abs(thatStop.getX() - thisStart.getX()) <= maxDeltaCoord) {
            // Case: that ... this
            if (Math.abs(thatStop.getY() - thisStart.getY()) <= maxDeltaPos) {
                return true;
            }
        }

        return false;
    }

    //-------------//
    // getFirstPos //
    //-------------//
    public int getFirstPos ()
    {
        return glyph.getOrientedBounds().y;
    }

    //---------------//
    // getFirstStuck //
    //---------------//
    public int getFirstStuck ()
    {
        int stuck = 0;

        for (GlyphSection section : glyph.getMembers()) {
            Run sectionRun = section.getFirstRun();

            for (GlyphSection sct : section.getSources()) {
                if (!sct.isGlyphMember() || (sct.getGlyph() != glyph)) {
                    stuck += sectionRun.getCommonLength(sct.getLastRun());
                }
            }
        }

        return stuck;
    }

    //------------------//
    // getIntPositionAt //
    //------------------//
    public int getIntPositionAt (double coord)
    {
        return (int) Math.rint(getPositionAt(coord));
    }

    //------------//
    // getLastPos //
    //------------//
    public int getLastPos ()
    {
        return (getFirstPos() + getThickness()) - 1;
    }

    //--------------//
    // getLastStuck //
    //--------------//
    public int getLastStuck ()
    {
        int stuck = 0;

        for (GlyphSection section : glyph.getMembers()) {
            Run sectionRun = section.getLastRun();

            for (GlyphSection sct : section.getTargets()) {
                if (!sct.isGlyphMember() || (sct.getGlyph() != glyph)) {
                    stuck += sectionRun.getCommonLength(sct.getFirstRun());
                }
            }
        }

        return stuck;
    }

    //-----------//
    // getLength //
    //-----------//
    public int getLength ()
    {
        return glyph.getOrientedBounds().width;
    }

    //-----------------//
    // getMeanDistance //
    //-----------------//
    public double getMeanDistance ()
    {
        if (line == null) {
            computeLine();
        }

        return line.getMeanDistance();
    }

    //-----------//
    // getMidPos //
    //-----------//
    public int getMidPos ()
    {
        if (getOrientedLine()
                .isVertical()) {
            // Fall back value
            return (int) Math.rint((getFirstPos() + getLastPos()) / 2.0);
        } else {
            return (int) Math.rint(
                getIntPositionAt((getStart() + getStop()) / 2.0));
        }
    }

    //-----------------//
    // getOrientedLine //
    //-----------------//
    public Line getOrientedLine ()
    {
        if (line == null) {
            computeLine();
        }

        return line;
    }

    //---------------//
    // getPositionAt //
    //---------------//
    public double getPositionAt (double coord)
    {
        return getOrientedLine()
                   .yAtX(coord);
    }

    //---------------//
    // getProbeWidth //
    //---------------//
    /**
     * Report the width of the window used to determine filament ordinate
     * @return the scale-independent probe width
     */
    public static Scale.Fraction getProbeWidth ()
    {
        return constants.probeWidth;
    }

    //----------//
    // getStart //
    //----------//
    public int getStart ()
    {
        return glyph.getOrientedBounds().x;
    }

    //---------------//
    // getStartPoint //
    //---------------//
    public Point2D getStartPoint ()
    {
        if (pStart == null) {
            pStart = glyph.getLag()
                          .absolute(
                new Point(getStart(), getIntPositionAt(getStart())));
        }

        return pStart;
    }

    //----------------//
    // getStartingPos //
    //----------------//
    public int getStartingPos ()
    {
        if ((getThickness() >= 2) && !getOrientedLine()
                                          .isVertical()) {
            return getIntPositionAt(getStart());
        } else {
            return getFirstPos() + (getThickness() / 2);
        }
    }

    //---------//
    // getStop //
    //---------//
    public int getStop ()
    {
        return (getStart() + getLength()) - 1;
    }

    //--------------//
    // getStopPoint //
    //--------------//
    public Point2D getStopPoint ()
    {
        if (pStop == null) {
            pStop = glyph.getLag()
                         .absolute(
                new Point(getStop(), getIntPositionAt(getStop())));
        }

        return pStop;
    }

    //----------------//
    // getStoppingPos //
    //----------------//
    public int getStoppingPos ()
    {
        if ((getThickness() >= 2) && !getOrientedLine()
                                          .isVertical()) {
            return getIntPositionAt(getStop());
        } else {
            return getFirstPos() + (getThickness() / 2);
        }
    }

    //--------------//
    // getThickness //
    //--------------//
    public int getThickness ()
    {
        return glyph.getOrientedBounds().height;
    }

    //----------------//
    // getThicknessAt //
    //----------------//
    /**
     * Report the stick mean thickness at the provided coordinate
     * @param coord the desired abscissa
     * @return the mean thickness measured, expressed in number of pixels.
     * Beware, this number will be zero if the probe falls entirely in a hole
     * between two sections.
     */
    public double getThicknessAt (double coord)
    {
        final Rectangle bounds = glyph.getOrientedBounds();
        Scale           scale = new Scale(glyph.getInterline());

        if ((coord < bounds.x) || (coord >= (bounds.x + bounds.width))) {
            logger.warning(this + " bounds:" + bounds + " coord:" + coord);
            throw new IllegalArgumentException(
                "Coordinate not within filament range");
        }

        // Use a large-enough collector
        final Rectangle roi = new Rectangle(
            (int) Math.rint(coord),
            bounds.y,
            0,
            bounds.height);
        final int       probeHalfWidth = scale.toPixels(constants.probeWidth) / 2;
        roi.grow(probeHalfWidth, 0);

        //        boolean[] matched = new boolean[roi.width];
        //        Arrays.fill(matched, false);
        //
        //        final PointsCollector collector = new PointsCollector(roi);
        //
        //        for (GlyphSection section : glyph.getMembers()) {
        //            Rectangle inter = roi.intersection(section.getOrientedBounds());
        //
        //            for (int c = (inter.x + inter.width) - 1; c >= inter.x; c--) {
        //                matched[c - roi.x] = true;
        //            }
        //
        //            section.cumulate(collector);
        //        }
        //
        //        int count = collector.getCount();
        //
        //        if (count == 0) {
        //            if (logger.isFineEnabled()) {
        //                logger.warning(
        //                    "Thickness " + this + " coord:" + coord + " nopoints");
        //            }
        //
        //            return 0;
        //        } else {
        //            // Return MEAN thickness on MATCHED probe width
        //            int width = 0;
        //
        //            for (boolean bool : matched) {
        //                if (bool) {
        //                    width++;
        //                }
        //            }
        //
        //            double thickness = (double) count / width;
        //
        //            if (logger.isFineEnabled()) {
        //                logger.fine(
        //                    this + " coord:" + coord + " pos:" +
        //                    (float) getPositionAt(coord) + " thickness:" + thickness);
        //            }
        //
        //            return thickness;
        //        }

        //
        Rectangle common = null;

        for (GlyphSection section : glyph.getMembers()) {
            Rectangle inter = roi.intersection(section.getOrientedBounds());

            if (common == null) {
                common = inter;
            } else {
                common = common.union(inter);
            }
        }

        return (common == null) ? 0 : common.height;
    }

    //------//
    // dump //
    //------//
    /**
     * Print out glyph internal data
     */
    @Override
    public void dump ()
    {
        System.out.println("   start=" + getStartPoint());
        System.out.println("   stop=" + getStopPoint());
        System.out.println("   line=" + getOrientedLine());
        System.out.println("   dist=" + getMeanDistance());
        System.out.println("   length=" + getLength());
        System.out.println(
            "   meanThickness=" + ((double) glyph.getWeight() / getLength()));
        System.out.println(
            "   straightness=" +
            ((getMeanDistance() * getMeanDistance()) / (Math.sqrt(
                glyph.getWeight()) * getLength())));
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        line = null;
        pStart = pStop = null;
    }

    //--------------//
    // overlapsWith //
    //--------------//
    public boolean overlapsWith (Stick other)
    {
        return Math.max(getStart(), other.getStart()) < Math.min(
            getStop(),
            other.getStop());
    }

    //------------//
    // renderLine //
    //------------//
    public void renderLine (Graphics2D g)
    {
        if (glyph.getContourBox()
                 .intersects(g.getClipBounds())) {
            getOrientedLine(); // To make sure the line has been computed

            int    halfLine = 0;
            Stroke stroke = g.getStroke();

            if (stroke instanceof BasicStroke) {
                halfLine = (int) Math.rint(
                    ((BasicStroke) stroke).getLineWidth() / 2);
            }

            Orientation orientation = glyph.getLag()
                                           .getOrientation();
            PixelPoint  start = orientation.absolute(
                new Point(
                    getStart() + halfLine,
                    (int) Math.rint(line.yAtX((double) getStart()))));
            PixelPoint  stop = orientation.absolute(
                new Point(
                    (getStop() + 1) - halfLine,
                    (int) Math.rint(line.yAtX((double) getStop() + 1))));
            g.drawLine(start.x, start.y, stop.x, stop.y);
        }
    }

    //---------//
    // getLine //
    //---------//
    protected Line getLine ()
    {
        return line;
    }

    //-------------//
    // computeLine //
    //-------------//
    protected void computeLine ()
    {
        line = new BasicLine();

        for (GlyphSection section : glyph.getMembers()) {
            StickSection ss = (StickSection) section;
            line.includeLine(ss.getLine());
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                line + " pointNb=" + line.getNumberOfPoints() +
                " meanDistance=" + (float) line.getMeanDistance());
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Scale.Fraction probeWidth = new Scale.Fraction(
            0.5,
            "Width of probing window to retrieve stick ordinate");
    }
}
