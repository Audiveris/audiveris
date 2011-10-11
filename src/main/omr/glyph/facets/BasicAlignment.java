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

import omr.glyph.Glyphs;

import omr.lag.Section;

import omr.log.Logger;

import omr.math.BasicLine;
import omr.math.Line;
import omr.math.LineUtilities;

import omr.run.Orientation;
import static omr.run.Orientation.*;
import omr.run.Run;

import omr.score.common.PixelRectangle;

import omr.sheet.Scale;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

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

    /** Absolute slope of the line wrt abscissa axis */
    protected Double slope;

    /** Absolute beginning point */
    protected Point2D startPoint;

    /** Absolute ending point */
    protected Point2D stopPoint;

    //~ Constructors -----------------------------------------------------------

    /**
     * Create a new BasicAlignment object
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
    public final double getAspect (Orientation orientation)
    {
        PixelRectangle box = glyph.getContourBox();

        if (orientation == HORIZONTAL) {
            return (double) box.width / (double) box.height;
        } else {
            return (double) box.height / (double) box.width;
        }
    }

    //-----------------//
    // setEndingPoints //
    //-----------------//
    public void setEndingPoints (Point2D pStart,
                                 Point2D pStop)
    {
        glyph.invalidateCache();
        this.startPoint = pStart;
        this.stopPoint = pStop;

        computeLine();

        // Enlarge contour box if needed
        PixelRectangle box = glyph.getContourBox();
        box.add(pStart);
        box.add(pStop);
        glyph.setContourBox(box);
    }

    //---------------//
    // getFirstStuck //
    //---------------//
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
    public int getIntPositionAt (double      coord,
                                 Orientation orientation)
    {
        return (int) Math.rint(getPositionAt(coord, orientation));
    }

    //--------------------------//
    // getInvertedSlope //
    //--------------------------//
    public double getInvertedSlope ()
    {
        checkLine();

        return (stopPoint.getX() - startPoint.getX()) / (stopPoint.getY() -
                                                        startPoint.getY());
    }

    //--------------//
    // getLastStuck //
    //--------------//
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
    public final int getLength (Orientation orientation)
    {
        PixelRectangle box = glyph.getContourBox();

        if (orientation == HORIZONTAL) {
            return box.width;
        } else {
            return box.height;
        }
    }

    //-----------------//
    // getLine //
    //-----------------//
    public Line getLine ()
    {
        checkLine();

        return line;
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
        if (isRatherVertical()) {
            return (int) Math.rint(
                (getStartPoint()
                     .getX() + getStopPoint()
                                   .getX()) / 2.0);
        } else {
            return (int) Math.rint(
                (getStartPoint()
                     .getY() + getStopPoint()
                                   .getY()) / 2.0);
        }
    }

    //---------------//
    // getPositionAt //
    //---------------//
    public double getPositionAt (double      coord,
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

    //------------------//
    // isRatherVertical //
    //------------------//
    public boolean isRatherVertical ()
    {
        if (slope == null) {
            computeLine();
        }

        return Math.abs(slope) > (Math.PI / 4);
    }

    //------------------//
    // getSlope //
    //------------------//
    public double getSlope ()
    {
        if (slope == null) {
            checkLine();

            slope = (stopPoint.getY() - startPoint.getY()) / (stopPoint.getX() -
                                                             startPoint.getX());
        }

        return slope;
    }

    //---------------//
    // getStartPoint //
    //---------------//
    public Point2D getStartPoint ()
    {
        checkLine();

        return startPoint;
    }

    //--------------//
    // getStopPoint //
    //--------------//
    public Point2D getStopPoint ()
    {
        checkLine();

        return stopPoint;
    }

    //--------------//
    // getThickness //
    //--------------//
    public final int getThickness (Orientation orientation)
    {
        PixelRectangle box = glyph.getContourBox();

        if (orientation == HORIZONTAL) {
            return box.height;
        } else {
            return box.width;
        }
    }

    //----------------//
    // getThicknessAt //
    //----------------//
    public double getThicknessAt (double      coord,
                                  Orientation orientation)
    {
        return Glyphs.getThicknessAt(coord, orientation, glyph);
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
        System.out.println("   line=" + getLine());
        System.out.println("   dist=" + getMeanDistance());
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
    public void renderLine (Graphics2D g)
    {
        if (!glyph.getContourBox()
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

        PixelRectangle box = glyph.getContourBox();

        // We have a problem if glyph is just 1 pixel: no computable slope!
        if (glyph.getWeight() <= 1) {
            startPoint = stopPoint = box.getLocation();
            slope = 0d; // Why not? we just need a value.

            return;
        }

        slope = line.getSlope();

        Point2D tl = box.getLocation();
        Point2D tr = new Point2D.Double(tl.getX() + box.width, tl.getY());
        Point2D bl = new Point2D.Double(tl.getX(), tl.getY() + box.height);
        Point2D br = new Point2D.Double(tr.getX(), bl.getY());

        if (isRatherVertical()) {
            // Use line intersections with top & bottom box sides
            Point2D p3 = new Point2D.Double(line.xAtY(tl.getY()), tl.getY());
            Point2D p4 = new Point2D.Double(line.xAtY(bl.getY()), bl.getY());
            startPoint = LineUtilities.intersection(tl, tr, p3, p4);
            stopPoint = LineUtilities.intersection(bl, br, p3, p4);
        } else {
            // Use line intersections with left & right box sides
            Point2D p3 = new Point2D.Double(tl.getX(), line.yAtX(tl.getX()));
            Point2D p4 = new Point2D.Double(tr.getX(), line.yAtX(tr.getX()));
            startPoint = LineUtilities.intersection(tl, bl, p3, p4);
            stopPoint = LineUtilities.intersection(tr, br, p3, p4);
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
            "Width of probing window to retrieve Glyph ordinate");
    }
}
