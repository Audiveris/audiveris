//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S k e l e t o n                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.image.ImageUtil;

import omr.score.ui.PageEraser;

import omr.sheet.Picture;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.ui.util.ItemRenderer;

import omr.util.Navigable;

import ij.process.ByteProcessor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code Skeleton} handles the skeleton structure used for slurs and wedges
 * retrieval.
 *
 * @author Hervé Bitteur
 */
public class Skeleton
        implements ItemRenderer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    /** Color for a foreground pixel discarded. */
    static final int HIDDEN = 230;

    /** Color for a foreground pixel already processed. */
    static final int PROCESSED = 180;

    /** Color for a foreground pixel part of arc. */
    static final int ARC = 140;

    /** Color for a foreground junction pixel. */
    static final int JUNCTION = 80;

    /** Color for a foreground junction pixel already processed. */
    static final int JUNCTION_PROCESSED = 90;

    /**
     * Directions.
     * <pre>
     * +-----+-----+-----+
     * |     |     |     |
     * |  7  |  8  |  1  |
     * |     |     |     |
     * +-----+-----+-----+
     * |     |     |     |
     * |  6  |  0  |  2  |
     * |     |     |     |
     * +-----+-----+-----+
     * |     |     |     |
     * |  5  |  4  |  3  |
     * |     |     |     |
     * +-----+-----+-----+
     * </pre>
     */
    /** Delta abscissa, per direction. ... 0. 1. 2. 3. 4 . 5 . 6 . 7. 8 */
    static final int[] dxs = new int[]{0, 1, 1, 1, 0, -1, -1, -1, 0};

    /** Delta ordinate, per direction. ... 0 . 1. 2. 3. 4. 5. 6 . 7. 8 */
    static final int[] dys = new int[]{0, -1, 0, 1, 1, 1, 0, -1, -1};

    /** Directions to scan, according to last direction. */
    static final int[][] scans = new int[][]{
        {2, 4, 6, 8, 1, 3, 5, 7}, // 0
        {2, 8, 1, 3, 7}, // 1
        {2, 4, 8, 1, 3}, // 2
        {2, 4, 1, 3, 5}, // 3
        {2, 4, 6, 3, 5}, // 4
        {4, 6, 3, 5, 7}, // 5
        {4, 6, 8, 5, 7}, // 6
        {6, 8, 1, 5, 7}, // 7
        {2, 6, 8, 1, 7} //  8
    };

    /** Map (Dx,Dy) -> Direction. */
    static final int[][] deltaToDir = new int[][]{
        {7, 6, 5}, // x:-1, y: -1, 0, +1
        {8, 0, 4}, // x: 0, y: -1, 0, +1
        {1, 2, 3} //  x:+1, y: -1, 0, +1
    };

    /** Vertical directions: south & north. */
    static final int[] vertDirs = new int[]{4, 8};

    /** Horizontal directions: east & west. */
    static final int[] horiDirs = new int[]{2, 6};

    /** Side directions: verticals + horizontals. */
    static final int[] sideDirs = new int[]{2, 4, 6, 8};

    /** Diagonal directions: ne, se, sw, nw. */
    static final int[] diagDirs = new int[]{1, 3, 5, 7};

    /** All directions. */
    static final int[] allDirs = new int[]{2, 4, 6, 8, 1, 3, 5, 7};

    private static final Color ARC_SLUR = Color.RED;

    private static final Color ARC_LINE = Color.BLUE;

    private static final Color ARC_LAMBDA = Color.LIGHT_GRAY;

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** The skeleton buffer. */
    public ByteProcessor buf;

    /** Map of relevant arcs. (end points -> arc) */
    public final Map<Point, Arc> arcsMap = new LinkedHashMap<Point, Arc>();

    /** List of arcs end points, with no junction, by abscissa. */
    public final List<Point> arcsEnds = new ArrayList<Point>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Skeleton object.
     *
     * @param sheet related sheet
     */
    public Skeleton (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getPixel //
    //----------//
    public int getPixel (int x,
                         int y)
    {
        return buf.get(x, y);
    }

    @Override
    public void renderItems (Graphics2D g)
    {
        // Render seeds
        for (Arc arc : arcsMap.values()) {
            setColor(arc, g);

            for (Point p : arc.getPoints()) {
                g.fillRect(p.x, p.y, 1, 1);
            }
        }
    }

    //----------//
    // setPixel //
    //----------//
    public void setPixel (int x,
                          int y,
                          int val)
    {
        buf.set(x, y, val);
    }

    //--------//
    // getDir //
    //--------//
    /**
     * Report the precise direction that goes from 'from' to 'to'.
     *
     * @param from p1
     * @param to   p2
     * @return direction p1 -> p2
     */
    static int getDir (Point from,
                       Point to)
    {
        int dx = to.x - from.x;
        int dy = to.y - from.y;

        return deltaToDir[1 + dx][1 + dy];
    }

    //------------//
    // isJunction //
    //------------//
    /**
     * Tell whether the pixel value indicates a junction point.
     *
     * @param pix pixel gray value
     * @return true if junction
     */
    static boolean isJunction (int pix)
    {
        return (pix >= JUNCTION) && (pix <= (JUNCTION + 10));
    }

    //---------------------//
    // isJunctionProcessed //
    //---------------------//
    /**
     * Tell whether the pixel value indicates a junction point
     * already processed.
     *
     * @param pix pixel gray value
     * @return true if junction already processed
     */
    static boolean isJunctionProcessed (int pix)
    {
        return pix == JUNCTION_PROCESSED;
    }

    //-------------//
    // isProcessed //
    //-------------//
    /**
     * Tell whether the pixel value indicates an end point of an arc
     * already processed.
     *
     * @param pix pixel gray value
     * @return true if arc already processed
     */
    static boolean isProcessed (int pix)
    {
        return (pix >= PROCESSED) && (pix < (PROCESSED + 10));
    }

    //--------//
    // isSide //
    //--------//
    /**
     * Tell whether the provided direction is a side one (H or V).
     *
     * @param dir provided direction
     * @return true if horizontal or vertical
     */
    static boolean isSide (int dir)
    {
        return (dir % 2) == 0;
    }

    //---------------//
    // buildSkeleton //
    //---------------//
    /**
     * Generate the skeleton from page binary image.
     *
     * @return the skeleton buffer
     */
    BufferedImage buildSkeleton ()
    {
        // First, get a skeleton of binary image
        Picture picture = sheet.getPicture();

        ///ByteProcessor buffer = picture.getSource(Picture.SourceKey.BINARY);
        ByteProcessor buffer = picture.getSource(Picture.SourceKey.STAFF_LINE_FREE);
        buffer = (ByteProcessor) buffer.duplicate();
        buffer.skeletonize();

        BufferedImage img = buffer.getBufferedImage();

        // Erase good shapes of each system
        Graphics2D g = img.createGraphics();
        PageEraser eraser = new PageEraser(g, sheet);
        eraser.eraseShapes(
                Arrays.asList(
                        Shape.THICK_BARLINE,
                        Shape.THIN_BARLINE,
                        Shape.THIN_CONNECTION,
                        Shape.THICK_CONNECTION,
                        Shape.STEM,
                        Shape.WHOLE_NOTE,
                        Shape.WHOLE_NOTE_SMALL,
                        Shape.NOTEHEAD_BLACK,
                        Shape.NOTEHEAD_BLACK_SMALL,
                        Shape.NOTEHEAD_VOID,
                        Shape.NOTEHEAD_VOID_SMALL,
                        Shape.BEAM,
                        Shape.BEAM_HOOK,
                        Shape.BEAM_SMALL,
                        Shape.BEAM_HOOK_SMALL));

        // Erase vertical seeds
        for (SystemInfo system : sheet.getSystems()) {
            eraser.eraseGlyphs(system.lookupShapedGlyphs(Shape.VERTICAL_SEED));
        }

        // Build buffer
        buffer = new ByteProcessor(img);
        buffer.threshold(127);

        // Keep a copy on disk?
        if (constants.keepSkeleton.isSet()) {
            ImageUtil.saveOnDisk(img, sheet.getPage().getId() + ".skl");
        }

        buf = buffer;

        return img;
    }

    private void setColor (Arc arc,
                           Graphics2D g)
    {
        if (arc.getShape() == ArcShape.SLUR) {
            g.setColor(ARC_SLUR);
        } else if (arc.getShape() == ArcShape.LINE) {
            g.setColor(ARC_LINE);
        } else {
            g.setColor(ARC_LAMBDA);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Boolean keepSkeleton = new Constant.Boolean(
                false,
                "Should we store skeleton images on disk?");
    }
}
