//----------------------------------------------------------------------------//
//                                                                            //
//                        B a s i c A l i g n m e n t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.constant.ConstantSet;

import omr.glyph.Glyphs;

import omr.lag.Section;

import omr.math.Barycenter;
import omr.math.BasicLine;
import omr.math.Line;

import omr.run.Orientation;
import static omr.run.Orientation.*;
import omr.run.Run;

import omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Class {@code BasicAlignment} implements a basic handling of
 * Alignment facet
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
    private static final Logger logger = LoggerFactory.getLogger(
            BasicAlignment.class);

    //~ Instance fields --------------------------------------------------------
    /** Best (curved or straight) line equation */
    protected Line line;

    /** Absolute slope of the line wrt abscissa axis */
    protected Double slope;

    /** Absolute beginning point */
    protected Point2D startPoint;

    /** Absolute ending point */
    protected Point2D stopPoint;

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
    //-----------//
    // getAspect //
    //-----------//
    @Override
    public final double getAspect (Orientation orientation)
    {
        Rectangle box = glyph.getBounds();

        if (orientation == HORIZONTAL) {
            return (double) box.width / (double) box.height;
        } else {
            return (double) box.height / (double) box.width;
        }
    }

    //---------------//
    // getFirstStuck //
    //---------------//
    @Override
    public int getFirstStuck ()
    {
        int stuck = 0;

        for (Section section : glyph.getMembers()) {
            Run sectionRun = section.getFirstRun();

            for (Section sct : section.getSources()) {
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
    public int getIntPositionAt (double coord,
                                 Orientation orientation)
    {
        return (int) Math.rint(getPositionAt(coord, orientation));
    }

    //------------------//
    // getInvertedSlope //
    //------------------//
    @Override
    public double getInvertedSlope ()
    {
        checkLine();

        return (stopPoint.getX() - startPoint.getX()) / (stopPoint.getY()
                                                         - startPoint.getY());
    }

    //--------------//
    // getLastStuck //
    //--------------//
    @Override
    public int getLastStuck ()
    {
        int stuck = 0;

        for (Section section : glyph.getMembers()) {
            Run sectionRun = section.getLastRun();

            for (Section sct : section.getTargets()) {
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
    @Override
    public final int getLength (Orientation orientation)
    {
        Rectangle box = glyph.getBounds();

        if (orientation == HORIZONTAL) {
            return box.width;
        } else {
            return box.height;
        }
    }

    //---------//
    // getLine //
    //---------//
    @Override
    public Line getLine ()
    {
        checkLine();

        return line;
    }

    //-----------------//
    // getMeanDistance //
    //-----------------//
    @Override
    public double getMeanDistance ()
    {
        if (line == null) {
            computeLine();
        }

        return line.getMeanDistance();
    }

    //------------------//
    // getMeanThickness //
    //------------------//
    @Override
    public double getMeanThickness (Orientation orientation)
    {
        return (double) glyph.getWeight() / getLength(orientation);
    }

    //-----------//
    // getMidPos //
    //-----------//
    @Override
    public int getMidPos (Orientation orientation)
    {
        if (orientation == VERTICAL) {
            return (int) Math.rint(
                    (getStartPoint(orientation)
                    .getX() + getStopPoint(orientation)
                    .getX()) / 2.0);
        } else {
            return (int) Math.rint(
                    (getStartPoint(orientation)
                    .getY() + getStopPoint(orientation)
                    .getY()) / 2.0);
        }
    }

    //---------------//
    // getPositionAt //
    //---------------//
    @Override
    public double getPositionAt (double coord,
                                 Orientation orientation)
    {
        if (orientation == HORIZONTAL) {
            return getLine()
                    .yAtX(coord);
        } else {
            return getLine()
                    .xAtY(coord);
        }
    }

    //----------------------//
    // getRectangleCentroid //
    //----------------------//
    /**
     * Report the absolute centroid of all the glyph pixels found in the
     * provided absolute ROI
     *
     * @param absRoi the desired absolute region of interest
     * @return the absolute barycenter of the pixels found
     */
    @Override
    public Point2D getRectangleCentroid (Rectangle absRoi)
    {
        Barycenter barycenter = new Barycenter();

        for (Section section : glyph.getMembers()) {
            section.cumulate(barycenter, absRoi);
        }

        if (barycenter.getWeight() != 0) {
            return new Point2D.Double(barycenter.getX(), barycenter.getY());
        } else {
            return null;
        }
    }

    //---------------//
    // getStartPoint //
    //---------------//
    @Override
    public Point2D getStartPoint (Orientation orientation)
    {
        checkLine();

        if (orientation == Orientation.HORIZONTAL) {
            // Use left side
            if (startPoint.getX() <= stopPoint.getX()) {
                return startPoint;
            } else {
                return stopPoint;
            }
        } else {
            // Use top side
            if (startPoint.getY() <= stopPoint.getY()) {
                return startPoint;
            } else {
                return stopPoint;
            }
        }
    }

    //--------------//
    // getStopPoint //
    //--------------//
    @Override
    public Point2D getStopPoint (Orientation orientation)
    {
        checkLine();

        if (orientation == Orientation.HORIZONTAL) {
            // Use right side
            if (stopPoint.getX() >= startPoint.getX()) {
                return stopPoint;
            } else {
                return startPoint;
            }
        } else {
            // Use bottom side
            if (stopPoint.getY() >= startPoint.getY()) {
                return stopPoint;
            } else {
                return startPoint;
            }
        }
    }

    //--------------//
    // getThickness //
    //--------------//
    @Override
    public final int getThickness (Orientation orientation)
    {
        Rectangle box = glyph.getBounds();

        if (orientation == HORIZONTAL) {
            return box.height;
        } else {
            return box.width;
        }
    }

    //---------------//
    // getProbeWidth //
    //---------------//
    /**
     * Report the width of the window used to determine filament ordinate
     *
     * @return the scale-independent probe width
     */
    public static Scale.Fraction getProbeWidth ()
    {
        return constants.probeWidth;
    }

    //--------//
    // dumpOf //
    //--------//
    /**
     * Report glyph internal data
     */
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        if (startPoint != null) {
            sb.append(String.format("   start=%s%n", startPoint));
        }

        if (stopPoint != null) {
            sb.append(String.format("   stop=%s%n", stopPoint));
        }

        sb.append(String.format("   line=%s%n", getLine()));

        return sb.toString();
    }

    //----------//
    // getSlope //
    //----------//
    @Override
    public double getSlope ()
    {
        if (slope == null) {
            checkLine();

            slope = (stopPoint.getY() - startPoint.getY()) / (stopPoint.getX()
                                                              - startPoint.getX());
        }

        return slope;
    }

    //----------------//
    // getThicknessAt //
    //----------------//
    @Override
    public double getThicknessAt (double coord,
                                  Orientation orientation)
    {
        return Glyphs.getThicknessAt(coord, orientation, glyph);
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        line = null;
        slope = null;
        startPoint = stopPoint = null;
    }

    //------------//
    // renderLine //
    //------------//
    @Override
    public void renderLine (Graphics2D g)
    {
        if (!glyph.getBounds()
                .intersects(g.getClipBounds())) {
            return;
        }

        getLine(); // To make sure the line has been computed

        g.drawLine(
                (int) Math.rint(startPoint.getX()),
                (int) Math.rint(startPoint.getY()),
                (int) Math.rint(stopPoint.getX()),
                (int) Math.rint(stopPoint.getY()));
    }

    //-----------------//
    // setEndingPoints //
    //-----------------//
    @Override
    public void setEndingPoints (Point2D pStart,
                                 Point2D pStop)
    {
        glyph.invalidateCache();
        this.startPoint = pStart;
        this.stopPoint = pStop;

        computeLine();

        // Enlarge contour box if needed
        Rectangle box = glyph.getBounds();
        box.add(pStart);
        box.add(pStop);
        glyph.setContourBox(box);
    }

    //-----------//
    // checkLine //
    //-----------//
    /**
     * Make sure an approximating line is available
     */
    protected final void checkLine ()
    {
        if (line == null) {
            computeLine();
        }
    }

    //-------------//
    // computeLine //
    //-------------//
    protected void computeLine ()
    {
        line = new BasicLine();

        for (Section section : glyph.getMembers()) {
            line.includeLine(section.getAbsoluteLine());
        }

        Rectangle box = glyph.getBounds();

        // We have a problem if glyph is just 1 pixel: no computable slope!
        if (glyph.getWeight() <= 1) {
            startPoint = stopPoint = box.getLocation();
            slope = 0d; // Why not? we just need a value.

            return;
        }

        slope = line.getSlope();

        double top = box.y;
        double bot = (box.y + box.height) - 1;
        double left = box.x;
        double right = (box.x + box.width) - 1;

        if (isRatherVertical()) {
            // Use line intersections with top & bottom box sides
            startPoint = new Point2D.Double(line.xAtY(top), top);
            stopPoint = new Point2D.Double(line.xAtY(bot), bot);

            if (!line.isVertical()) {
                Point2D pLeft = new Point2D.Double(left, line.yAtX(left));
                Point2D pRight = new Point2D.Double(right, line.yAtX(right));

                if (line.getInvertedSlope() > 0) {
                    if (pLeft.getY() > startPoint.getY()) {
                        startPoint = pLeft;
                    }

                    if (pRight.getY() < stopPoint.getY()) {
                        stopPoint = pRight;
                    }
                } else {
                    if (pRight.getY() > startPoint.getY()) {
                        startPoint = pRight;
                    }

                    if (pLeft.getY() < stopPoint.getY()) {
                        stopPoint = pLeft;
                    }
                }
            }
        } else {
            // Use line intersections with left & right box sides
            startPoint = new Point2D.Double(left, line.yAtX(left));
            stopPoint = new Point2D.Double(right, line.yAtX(right));

            if (!line.isHorizontal()) {
                Point2D pTop = new Point2D.Double(line.xAtY(top), top);
                Point2D pBot = new Point2D.Double(line.xAtY(bot), bot);

                if (slope > 0) {
                    if (pTop.getX() > startPoint.getX()) {
                        startPoint = pTop;
                    }

                    if (pBot.getX() < stopPoint.getX()) {
                        stopPoint = pBot;
                    }
                } else {
                    if (pBot.getX() > startPoint.getX()) {
                        startPoint = pBot;
                    }

                    if (pTop.getX() < stopPoint.getX()) {
                        stopPoint = pTop;
                    }
                }
            }
        }
    }

    //------------------//
    // isRatherVertical //
    //------------------//
    /**
     * Report whether the angle of the approximating line is outside
     * the range [-PI/4 - +PI/4].
     *
     * @return true if rather vertical, false for rather horizontal
     */
    private boolean isRatherVertical ()
    {
        if (slope == null) {
            computeLine();
        }

        return Math.abs(slope) > (Math.PI / 4);
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
                "Width of probing window to retrieve Glyph ordinate");

    }
}
