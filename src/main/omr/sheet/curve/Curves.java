//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           C u r v e s                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Sheet;
import omr.sheet.ui.ImageView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.ScrollImageView;

import omr.sig.inter.SegmentInter;

import omr.ui.BoardsPane;
import omr.ui.util.ItemRenderer;

import omr.util.IntUtil;
import omr.util.Navigable;
import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code Curves} is the platform used to handle several kinds of curves (slurs,
 * wedges, endings) by walking along the arcs of a sheet skeleton.
 * <p>
 * We have to visit each pixel of the buffer, detect junction points and arcs departing or arriving
 * at junction points.
 *
 * @author Hervé Bitteur
 */
public class Curves
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Curves.class);

    private static final List<Point> breakPoints = getBreakPoints();

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** View on skeleton, if any. */
    private MyView view;

    /** Underlying skeleton. */
    private final Skeleton skeleton;

    /** Line segments found. */
    private final List<SegmentInter> segments = new ArrayList<SegmentInter>();

    /** Registered item renderers, if any. */
    private final Set<ItemRenderer> itemRenderers = new LinkedHashSet<ItemRenderer>();

    /** Builder for slurs (also used to evaluate arcs). */
    private SlursBuilder slursBuilder;

    //~ Constructors -------------------------------------------------------------------------------
    //--------//
    // Curves //
    //--------//
    /**
     * Creates a new Curves object.
     *
     * @param sheet the related sheet
     */
    public Curves (Sheet sheet)
    {
        this.sheet = sheet;

        skeleton = new Skeleton(sheet);
        itemRenderers.add(skeleton);

        BufferedImage img = skeleton.buildSkeleton();

        // Display skeleton buffer?
        if ((Main.getGui() != null) && constants.displayCurves.isSet()) {
            view = new Curves.MyView(img);
            sheet.getAssembly().addViewTab(
                    "Curves",
                    new ScrollImageView(sheet, view),
                    new BoardsPane(new PixelBoard(sheet)));
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // buildCurves //
    //-------------//
    /**
     * Build all curves out of the image skeleton, by appending arcs.
     */
    public void buildCurves ()
    {
        // Retrieve junctions.
        StopWatch watch = new StopWatch("Curves");
        watch.start("Junctions retrieval");
        new JunctionRetriever(skeleton).scanImage();

        // Scan arcs between junctions
        slursBuilder = new SlursBuilder(this);
        watch.start("Arcs retrieval");
        new ArcRetriever(this).scanImage();

        // Retrieve slurs from arcs
        itemRenderers.add(slursBuilder);
        watch.start("buildSlurs");
        slursBuilder.buildSlurs();

        // Retrieve segments from arcs
        SegmentsBuilder segmentsBuilder = new SegmentsBuilder(this);
        itemRenderers.add(segmentsBuilder);
        watch.start("buildSegments");
        segmentsBuilder.buildSegments();

        // Build wedges out of segments
        WedgesBuilder wedgesBuilder = new WedgesBuilder(this);
        watch.start("buildWedges");
        wedgesBuilder.buildWedges();

        // Build endings out of segments
        EndingsBuilder endingsBuilder = new EndingsBuilder(this);
        watch.start("buildEndings");
        endingsBuilder.buildEndings();

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //------------//
    // checkBreak //
    //------------//
    /**
     * Debug method to break on a specific arc.
     *
     * @param arc current arc being processed
     */
    public void checkBreak (Arc arc)
    {
        if (arc == null) {
            return;
        }

        for (Point pt : breakPoints) {
            if (pt.equals(arc.getEnd(false)) || pt.equals(arc.getEnd(true))) {
                view.selectPoint(arc.getEnd(true));
                logger.info("Curve break on {}", arc);

                break;
            }
        }
    }

    //-------------//
    // getSegments //
    //-------------//
    /**
     * @return the segments retrieved
     */
    public List<SegmentInter> getSegments ()
    {
        return segments;
    }

    //----------//
    // getSheet //
    //----------//
    public Sheet getSheet ()
    {
        return sheet;
    }

    //-------------//
    // getSkeleton //
    //-------------//
    /**
     * @return the skeleton
     */
    public Skeleton getSkeleton ()
    {
        return skeleton;
    }

    //-----------------//
    // getSlursBuilder //
    //-----------------//
    public SlursBuilder getSlursBuilder ()
    {
        return slursBuilder;
    }

    //-------------//
    // selectPoint //
    //-------------//
    /**
     * Debugging feature which forces view focus on provided point
     *
     * @param point the new focus point
     */
    public void selectPoint (Point point)
    {
        view.selectPoint(point);
    }

    //----------------//
    // getBreakPoints //
    //----------------//
    /**
     * Debugging method to define image points that arc processing should break upon.
     *
     * @return the collection of points, perhaps empty
     */
    private static List<Point> getBreakPoints ()
    {
        List<Point> points = new ArrayList<Point>();

        try {
            String str = constants.breakPointCoordinates.getValue();
            List<Integer> ints = IntUtil.parseInts(str);
            int count = ints.size();

            if ((count % 2) != 0) {
                logger.warn("Odd number of coordinates for break points: {}", str);
            }

            for (int i = 0; i < (count / 2); i++) {
                points.add(new Point(ints.get(2 * i), ints.get((2 * i) + 1)));
            }

            if (!points.isEmpty()) {
                logger.info("Curve break points: {}", points);
            }
        } catch (Exception ignored) {
        }

        return points;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Constant.Boolean displayCurves = new Constant.Boolean(
                false,
                "Should we display the view on curves?");

        Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        Constant.String breakPointCoordinates = new Constant.String(
                "",
                "(Debug) Comma-separated coordinates of curve break points if any");
    }

    //--------//
    // MyView //
    //--------//
    /**
     * View dedicated to skeleton arcs.
     */
    private class MyView
            extends ImageView
    {
        //~ Constructors ---------------------------------------------------------------------------

        public MyView (BufferedImage image)
        {
            super(image);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected void renderItems (Graphics2D g)
        {
            // Global sheet renderers
            for (ItemRenderer renderer : sheet.getItemRenderers()) {
                renderer.renderItems(g);
            }

            // Curves renderers
            for (ItemRenderer renderer : itemRenderers) {
                renderer.renderItems(g);
            }
        }
    }
}
