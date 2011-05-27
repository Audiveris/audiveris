//----------------------------------------------------------------------------//
//                                                                            //
//                         T a r g e t B u i l d e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.common.PixelPoint;

import omr.sheet.Sheet;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.Warp;
import javax.media.jai.WarpGrid;

/**
 * Class {@code TargetBuilder} is in charge of building a perfect definition
 * of target systems, staves and lines as well as the dewarp grid that allows to
 * transform the original image in to the perfect image.
 *
 * @author Hervé Bitteur
 */
public class TargetBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TargetBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final Sheet sheet;

    /** Companion in charge of staff lines */
    private final LinesRetriever linesRetriever;

    /** Companion in charge of bar lines */
    private final BarsRetriever barsRetriever;

    /** Target width */
    private int targetWidth;

    /** Target height */
    private int targetHeight;

    /** Transform from initial point to deskewed point */
    private AffineTransform at;

    /** The target page */
    private TargetPage targetPage;

    /** All target lines */
    private List<TargetLine> allTargetLines = new ArrayList<TargetLine>();

    /** The dewarp grid */
    private Warp dewarpGrid;

    /** Source points */
    private List<Point2D> srcPoints = new ArrayList<Point2D>();

    /** Destination points */
    private List<Point2D> dstPoints = new ArrayList<Point2D>();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // TargetBuilder //
    //---------------//
    /**
     * Creates a new TargetBuilder object.
     *
     * @param sheet DOCUMENT ME!
     * @param linesRetriever DOCUMENT ME!
     * @param barsRetriever DOCUMENT ME!
     */
    public TargetBuilder (Sheet          sheet,
                          LinesRetriever linesRetriever,
                          BarsRetriever  barsRetriever)
    {
        this.sheet = sheet;
        this.linesRetriever = linesRetriever;
        this.barsRetriever = barsRetriever;
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getDewarpGrid //
    //---------------//
    /**
     * @return the dewarpGrid
     */
    public Warp getDewarpGrid ()
    {
        return dewarpGrid;
    }

    //-----------//
    // buildInfo //
    //-----------//
    public void buildInfo ()
    {
        buildTarget();
        buildWarpGrid();
    }

    //    //--------//
    //    // deskew //
    //    //--------//
    //    /**
    //     * Apply rotation OPPOSITE to the measured global angle and use the new
    //     * origin
    //     *
    //     * @param pt the initial (skewed) point
    //     * @return the deskewed point
    //     */
    //    private PixelPoint deskew (Point2D pt)
    //    {
    //        Point2D p = at.transform(pt, null);
    //
    //        return new PixelPoint(
    //            (int) Math.rint(p.getX()),
    //            (int) Math.rint(p.getY()));
    //    }

    //------------------//
    // renderDewarpGrid //
    //------------------//
    /**
     * Render the grid used to dewarp the sheet image
     * @param g the graphic context
     * @param useSource true to render the source grid, false to render the
     * destination grid
     */
    public void renderDewarpGrid (Graphics g,
                                  boolean  useSource)
    {
        if (!constants.displayGrid.getValue()) {
            return;
        }

        List<Point2D> points = useSource ? srcPoints : dstPoints;
        int           radius = 1;
        g.setColor(Color.RED);

        for (Point2D pt : points) {
            g.fillRect(
                (int) Math.rint(pt.getX()) - radius,
                (int) Math.rint(pt.getY()) - radius,
                2 * radius,
                2 * radius);
        }
    }

    //-------------//
    // buildTarget //
    //-------------//
    /**
     * Build a perfect definition of target page, systems, staves and lines.
     *
     * We apply a rotation on every top-left corner
     */
    private void buildTarget ()
    {
        logger.info("Sheet " + sheet.getDimension());
        logger.info("Global slope: " + linesRetriever.getGlobalSlope());

        // Set up rotation + origin translation
        computeDeskew();

        // Target page parameters
        targetPage = new TargetPage(targetWidth, targetHeight);

        // Target systems parameters
        TargetSystem prevTargetSystem = null;

        for (SystemFrame system : barsRetriever.getSystems()) {
            StaffInfo  firstStaff = system.getFirstStaff();
            LineInfo   firstLine = firstStaff.getFirstLine();
            PixelPoint rightPoint = firstLine.getRightPoint();
            int        sysRight = rightPoint.x;

            // Make sure right side of this new system is kept consistent
            // with right side of end of the previous system if any
            if (prevTargetSystem != null) {
                sysRight += (prevTargetSystem.right -
                            prevTargetSystem.info.getLastStaff().getLastLine().getRightPoint().x);
            }

            TargetSystem targetSystem = new TargetSystem(
                system,
                firstLine.getLeftPoint().y,
                sysRight - (rightPoint.x - firstLine.getLeftPoint().x),
                sysRight);
            targetPage.systems.add(targetSystem);

            // Target staff parameters
            for (StaffInfo staff : system.getStaves()) {
                TargetStaff targetStaff = new TargetStaff(
                    staff,
                    staff.getFirstLine()
                         .getLeftPoint().y,
                    targetSystem);
                targetSystem.staves.add(targetStaff);

                // Target line parameters
                int lineIdx = -1;

                for (LineInfo line : staff.getLines()) {
                    lineIdx++;

                    // Enforce perfect staff interline
                    TargetLine targetLine = new TargetLine(
                        line,
                        targetStaff.top +
                        (staff.getSpecificScale().interline() * lineIdx),
                        targetStaff);
                    allTargetLines.add(targetLine);
                    targetStaff.lines.add(targetLine);
                }
            }

            prevTargetSystem = targetSystem;
        }
    }

    //---------------//
    // buildWarpGrid //
    //---------------//
    private void buildWarpGrid ()
    {
        int xStep = sheet.getInterline();
        int xNumCells = (int) Math.ceil(sheet.getWidth() / (double) xStep);
        int yStep = sheet.getInterline();
        int yNumCells = (int) Math.ceil(sheet.getHeight() / (double) yStep);

        for (int ir = 0; ir <= yNumCells; ir++) {
            for (int ic = 0; ic <= xNumCells; ic++) {
                Point2D dst = new Point2D.Double(ic * xStep, ir * yStep);
                dstPoints.add(dst);

                Point2D src = sourceOf(dst);
                srcPoints.add(src);
            }
        }

        float[] warpPositions = new float[srcPoints.size() * 2];
        int     i = 0;

        for (Point2D p : srcPoints) {
            warpPositions[i++] = (float) p.getX();
            warpPositions[i++] = (float) p.getY();
        }

        dewarpGrid = new WarpGrid(
            0,
            xStep,
            xNumCells,
            0,
            yStep,
            yNumCells,
            warpPositions);
    }

    //---------------//
    // computeDeskew //
    //---------------//
    private void computeDeskew ()
    {
        double globalSlope = linesRetriever.getGlobalSlope();
        double deskewAngle = -Math.atan(globalSlope);
        at = AffineTransform.getRotateInstance(deskewAngle);

        // Compute topLeft origin translation
        int     w = sheet.getWidth();
        int     h = sheet.getHeight();
        Point2D topRight = at.transform(new Point(w, 0), null);
        Point2D bottomLeft = at.transform(new Point(0, h), null);
        Point2D bottomRight = at.transform(new Point(w, h), null);
        double  dx = 0;
        double  dy = 0;

        if (deskewAngle <= 0) { // Counter-clockwise deskew
            targetWidth = (int) Math.ceil(bottomRight.getX());
            dy = -topRight.getY();
            targetHeight = (int) Math.ceil(bottomLeft.getY() + dy);
        } else { // Clockwise deskew
            dx = -bottomLeft.getX();
            targetWidth = (int) Math.ceil(topRight.getX() + dx);
            targetHeight = (int) Math.ceil(bottomRight.getY());
        }

        at.translate(dx, dy);
    }

    //----------//
    // sourceOf //
    //----------//
    /**
     * This key method provides the source point (in original sheet image)
     * that corresponds to a given destination point (in target dewarped image).
     *
     * The strategy is to stay consistent with the staff lines nearby which
     * are used as grid references.
     *
     * @param dst the given destination point
     * @return the corresponding source point
     */
    private Point2D sourceOf (Point2D dst)
    {
        double     dstX = dst.getX();
        double     dstY = dst.getY();

        // Retrieve north & south lines, if any
        TargetLine northLine = null;
        TargetLine southLine = null;

        for (TargetLine line : allTargetLines) {
            if (line.y <= dstY) {
                northLine = line;
            } else {
                southLine = line;

                break;
            }
        }

        // Case of image top: no northLine
        if (northLine == null) {
            return southLine.sourceOf(dst);
        }

        // Case of image bottom: no southLine
        if (southLine == null) {
            return northLine.sourceOf(dst);
        }

        // Normal case: use y barycenter between projections sources
        Point2D srcNorth = northLine.sourceOf(dstX);
        Point2D srcSouth = southLine.sourceOf(dstX);
        double  yRatio = (dstY - northLine.y) / (southLine.y - northLine.y);

        return new Point2D.Double(
            ((1 - yRatio) * srcNorth.getX()) + (yRatio * srcSouth.getX()),
            ((1 - yRatio) * srcNorth.getY()) + (yRatio * srcSouth.getY()));
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean displayGrid = new Constant.Boolean(
            true,
            "Should we display the dewarp grid?");
    }
}
