//----------------------------------------------------------------------------//
//                                                                            //
//                         T a r g e t B u i l d e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.ScoresManager;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;
import omr.sheet.SystemInfo;
import omr.sheet.picture.jai.JaiDewarper;

import omr.ui.Colors;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Class {@code TargetBuilder} is in charge of building a "perfect"
 * definition of target systems, staves and lines as well as the
 * dewarp grid that allows to transform the original image in to the
 * perfect image.
 *
 * @author Hervé Bitteur
 */
public class TargetBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(TargetBuilder.class);

    //~ Instance fields --------------------------------------------------------
    /** Related sheet */
    private final Sheet sheet;

    /** Target width */
    private double targetWidth;

    /** Target height */
    private double targetHeight;

    /** Transform from initial point to deskewed point */
    private AffineTransform at;

    /** The target page */
    private TargetPage targetPage;

    /** All target lines */
    private List<TargetLine> allTargetLines = new ArrayList<>();

    /** Source points */
    private List<Point2D> srcPoints = new ArrayList<>();

    /** Destination points */
    private List<Point2D> dstPoints = new ArrayList<>();

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // TargetBuilder //
    //---------------//
    /**
     * Creates a new TargetBuilder object.
     *
     * @param sheet the related sheet
     */
    public TargetBuilder (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // buildInfo //
    //-----------//
    public void buildInfo ()
    {
        buildTarget();

        JaiDewarper dewarper = new JaiDewarper(sheet);

        buildWarpGrid(dewarper);

        // Dewarp the initial image
        RenderedImage dewarpedImage = dewarper.dewarpImage();

        // Add a view on dewarped image?
        if (Main.getGui() != null) {
            sheet.getAssembly()
                    .addViewTab(
                    "Dewarped",
                    new ScrollView(new DewarpedView(dewarpedImage)),
                    null);
        }

        // Store dewarped image on disk
        if (constants.storeDewarp.getValue()) {
            storeImage(dewarpedImage);
        }
    }

    //---------------//
    // renderSystems //
    //---------------//
    /**
     * TODO: This should be done from a more central class
     *
     * @param g graphical context
     */
    public void renderSystems (Graphics2D g)
    {
        Scale scale = sheet.getScale();
        Skew skew = sheet.getSkew();
        // Make sure we are not painting changing data...
        if (scale == null || skew == null) {
            return;
        }

        double absDx = scale.toPixelsDouble(constants.systemMarkWidth);
        double absDy = skew.getSlope() * absDx;
        Stroke systemStroke = new BasicStroke(
                (float) scale.toPixelsDouble(constants.systemMarkStroke),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND);

        g.setStroke(systemStroke);
        g.setColor(Colors.SYSTEM_BRACKET);

        for (SystemInfo system : sheet.getSystems()) {
            for (HorizontalSide side : HorizontalSide.values()) {
                Point2D top = system.getFirstStaff()
                        .getFirstLine()
                        .getEndPoint(side);
                Point2D bot = system.getLastStaff()
                        .getLastLine()
                        .getEndPoint(side);

                // Draw something like a vertical bracket
                double dx = (side == LEFT) ? (-absDx) : absDx;
                double dy = (side == LEFT) ? (-absDy) : absDy;
                Path2D p = new Path2D.Double();
                p.moveTo(top.getX(), top.getY());
                p.lineTo(top.getX() + dx, top.getY() + dy);
                p.lineTo(bot.getX() + dx, bot.getY() + dy);
                p.lineTo(bot.getX(), bot.getY());
                g.draw(p);
            }
        }
    }

    //----------------//
    // renderWarpGrid //
    //----------------//
    /**
     * Render the grid used to dewarp the sheet image
     *
     * @param g         the graphic context
     * @param useSource true to renderAttachments the source grid, false to
     *                  renderAttachments the
     *                  destination grid
     */
    public void renderWarpGrid (Graphics g,
                                boolean useSource)
    {
        if (!constants.displayGrid.getValue()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        List<Point2D> points = useSource ? srcPoints : dstPoints;
        double radius = sheet.getScale()
                .toPixelsDouble(constants.gridPointSize);
        g2.setColor(Colors.WARP_POINT);

        Rectangle2D rect = new Rectangle2D.Double();

        for (Point2D pt : points) {
            rect.setRect(
                    pt.getX() - radius,
                    pt.getY() - radius,
                    2 * radius,
                    2 * radius);
            g2.fill(rect);
        }
    }

    //-------------//
    // buildTarget //
    //-------------//
    /**
     * Build a perfect definition of target page, systems, staves and
     * lines.
     *
     * We apply a rotation on every top-left corner
     */
    private void buildTarget ()
    {
        final Skew skew = sheet.getSkew();

        // Target page parameters
        targetPage = new TargetPage(targetWidth, targetHeight);

        TargetLine prevLine = null; // Latest staff line

        // Target system parameters
        for (SystemInfo system : sheet.getSystems()) {
            StaffInfo firstStaff = system.getFirstStaff();
            LineInfo firstLine = firstStaff.getFirstLine();
            Point2D dskLeft = skew.deskewed(firstLine.getEndPoint(LEFT));
            Point2D dskRight = skew.deskewed(firstLine.getEndPoint(RIGHT));

            if (prevLine != null) {
                // Preserve position relative to bottom left of previous system
                Point2D prevDskLeft = skew.deskewed(
                        prevLine.info.getEndPoint(LEFT));
                TargetSystem prevSystem = prevLine.staff.system;
                double dx = prevSystem.left - prevDskLeft.getX();
                double dy = prevLine.y - prevDskLeft.getY();
                dskLeft.setLocation(dskLeft.getX() + dx, dskLeft.getY() + dy);
                dskRight.setLocation(
                        dskRight.getX() + dx,
                        dskRight.getY() + dy);
            }

            TargetSystem targetSystem = new TargetSystem(
                    system,
                    dskLeft.getY(),
                    dskLeft.getX(),
                    dskRight.getX());
            targetPage.systems.add(targetSystem);

            // Target staff parameters
            for (StaffInfo staff : system.getStaves()) {
                dskLeft = skew.deskewed(staff.getFirstLine().getEndPoint(LEFT));

                if (prevLine != null) {
                    // Preserve inter-staff vertical gap
                    Point2D prevDskLeft = skew.deskewed(
                            prevLine.info.getEndPoint(LEFT));
                    dskLeft.setLocation(
                            dskLeft.getX(),
                            dskLeft.getY() + (prevLine.y - prevDskLeft.getY()));
                }

                TargetStaff targetStaff = new TargetStaff(
                        staff,
                        dskLeft.getY(),
                        targetSystem);
                targetSystem.staves.add(targetStaff);

                // Target line parameters
                int lineIdx = -1;

                for (LineInfo line : staff.getLines()) {
                    lineIdx++;

                    // Enforce perfect staff interline
                    TargetLine targetLine = new TargetLine(
                            line,
                            targetStaff.top
                            + (staff.getSpecificScale().getInterline() * lineIdx),
                            targetStaff);
                    allTargetLines.add(targetLine);
                    targetStaff.lines.add(targetLine);
                    prevLine = targetLine;
                }
            }
        }
    }

    //---------------//
    // buildWarpGrid //
    //---------------//
    private void buildWarpGrid (JaiDewarper dewarper)
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
        int i = 0;

        for (Point2D p : srcPoints) {
            warpPositions[i++] = (float) p.getX();
            warpPositions[i++] = (float) p.getY();
        }

        dewarper.createWarpGrid(
                0,
                xStep,
                xNumCells,
                0,
                yStep,
                yNumCells,
                warpPositions);
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
        double dstX = dst.getX();
        double dstY = dst.getY();

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
        double yRatio = (dstY - northLine.y) / (southLine.y - northLine.y);

        return new Point2D.Double(
                ((1 - yRatio) * srcNorth.getX()) + (yRatio * srcSouth.getX()),
                ((1 - yRatio) * srcNorth.getY()) + (yRatio * srcSouth.getY()));
    }

    //------------//
    // storeImage //
    //------------//
    private void storeImage (RenderedImage dewarpedImage)
    {
        String pageId = sheet.getPage().getId();
        File file = new File(
                ScoresManager.getInstance().getDefaultDewarpDirectory(),
                pageId + ".dewarped.png");

        try {
            String path = file.getCanonicalPath();
            ImageIO.write(dewarpedImage, "png", file);
            logger.info("Wrote {}", path);
        } catch (IOException ex) {
            logger.warn("Could not write {}", file);
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

        Constant.Boolean displayGrid = new Constant.Boolean(
                false,
                "Should we display the dewarp grid?");

        Scale.LineFraction gridPointSize = new Scale.LineFraction(
                0.2,
                "Size of displayed grid points");

        Scale.Fraction systemMarkWidth = new Scale.Fraction(
                2.0,
                "Width of system marks");

        Scale.LineFraction systemMarkStroke = new Scale.LineFraction(
                2.0,
                "Thickness of system marks");

        Constant.Boolean storeDewarp = new Constant.Boolean(
                false,
                "Should we store the dewarped image on disk?");

    }

    //--------------//
    // DewarpedView //
    //--------------//
    private class DewarpedView
            extends RubberPanel
    {
        //~ Instance fields ----------------------------------------------------

        private final AffineTransform identity = new AffineTransform();

        private final RenderedImage image;

        //~ Constructors -------------------------------------------------------
        public DewarpedView (RenderedImage image)
        {
            this.image = image;

            setModelSize(new Dimension(image.getWidth(), image.getHeight()));

            // Location service
            setLocationService(sheet.getLocationService());

            setName("DewarpedView");
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void render (Graphics2D g)
        {
            // Display the dewarped image
            g.drawRenderedImage(image, identity);

            // Display also the Destination Points
            renderWarpGrid(g, false);
        }
    }
}
