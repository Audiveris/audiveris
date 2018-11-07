//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S k e l e t o n                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.image.ImageUtil;
import org.audiveris.omr.sheet.PageCleaner;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.ui.util.ItemRenderer;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code Skeleton} handles the skeleton structure used for slurs and segments
 * retrieval, including the navigation along the skeleton.
 * <p>
 * We use special color values to record information directly within the skeleton buffer.
 *
 * @author Hervé Bitteur
 */
public class Skeleton
        implements ItemRenderer
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Skeleton.class);

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
     * Headings.
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
    /** Delta abscissa, per heading. 0 1 2. 3. 4 . 5 . 6 . 7. 8 */
    static final int[] dxs = new int[]{0, 1, 1, 1, 0, -1, -1, -1, 0};

    /** Delta ordinate, per heading. 0 1. 2. 3. 4. 5. 6 . 7 . 8 */
    static final int[] dys = new int[]{0, -1, 0, 1, 1, 1, 0, -1, -1};

    /** Headings to scan, according to last heading. */
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

    /** Map (Dx,Dy) -> Heading. */
    static final int[][] deltaToDir = new int[][]{
        {7, 6, 5}, // x:-1, y: -1, 0, +1
        {8, 0, 4}, // x: 0, y: -1, 0, +1
        {1, 2, 3} //  x:+1, y: -1, 0, +1
    };

    /** Vertical headings: south & north. */
    static final int[] vertDirs = new int[]{4, 8};

    /** Horizontal headings: east & west. */
    static final int[] horiDirs = new int[]{2, 6};

    /** Side headings: verticals + horizontals. */
    static final int[] sideDirs = new int[]{2, 4, 6, 8};

    /** Diagonal headings: ne, se, sw, nw. */
    static final int[] diagDirs = new int[]{1, 3, 5, 7};

    /** All headings. */
    static final int[] allDirs = new int[]{2, 4, 6, 8, 1, 3, 5, 7};

    private static final Color ARC_SLUR = Color.RED;

    private static final Color ARC_LINE = Color.BLUE;

    private static final Color ARC_LAMBDA = Color.LIGHT_GRAY;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** The skeleton buffer. */
    public ByteProcessor buf;

    /** Map of relevant arcs (end points &rarr; arc). */
    public final Map<Point, Arc> arcsMap = new LinkedHashMap<Point, Arc>();

    /** Map of void arcs (pivot &rarr; arc(s)). */
    public final Map<Point, List<Arc>> voidArcsMap = new LinkedHashMap<Point, List<Arc>>();

    /** List of arcs end points, with no junction, ordered by abscissa. */
    public final List<Point> arcsEnds = new ArrayList<Point>();

    /** Map of non crossable erased inters. */
    private Map<SystemInfo, List<Inter>> nonCrossables;

    /** Map of crossable erased inters. */
    private Map<SystemInfo, List<Inter>> crossables;

    /** Map of erased (seed) glyphs. */
    private Map<SystemInfo, List<Glyph>> erasedSeeds;

    /**
     * Creates a new Skeleton object.
     *
     * @param sheet related sheet
     */
    public Skeleton (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //---------------//
    // buildSkeleton //
    //---------------//
    /**
     * Generate the skeleton from page binary image.
     * <p>
     * Since this skeleton is meant for curves (slurs, wedges, endings) we can limit processing past
     * some reasonable distance from staves (both in vertical and horizontal directions).
     * <p>
     * We must keep track of erased shapes at system level.<ul>
     * <li>Notes and beams cannot be crossed by a curve.</li>
     * <li>Bar lines, connections and stems can be crossed by a curve.
     * Perhaps another specific background value could be used?</li>
     * </ul>
     *
     * @return the skeleton image (in parallel of setting the skeleton buffer)
     */
    public BufferedImage buildSkeleton ()
    {
        // First, get a skeleton of binary image
        Picture picture = sheet.getPicture();

        ///ByteProcessor buffer = picture.getSource(Picture.SourceKey.NO_STAFF);
        ByteProcessor buffer = picture.getSource(Picture.SourceKey.BINARY);
        buffer = (ByteProcessor) buffer.duplicate();
        buffer.skeletonize();

        BufferedImage img = buffer.getBufferedImage();

        // Erase good shapes of each system, both non-crossables and crossables
        Graphics2D g = img.createGraphics();
        CurvesCleaner cleaner = new CurvesCleaner(buffer, g, sheet);

        // Non-crossable inters
        nonCrossables = cleaner.eraseShapes(
                Arrays.asList(
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

        // Crossable inters
        crossables = cleaner.eraseShapes(Arrays.asList(Shape.THICK_BARLINE, Shape.THIN_BARLINE,
                                                       Shape.THIN_CONNECTOR, Shape.THICK_CONNECTOR,
                                                       Shape.LEDGER, Shape.STEM));

        // Erase vertical seeds (?)
        ///erasedSeeds = eraser.eraseGlyphs(Arrays.asList(Shape.VERTICAL_SEED));
        //
        // Erase regions too far froms staves
        cleaner.eraseDistantRegions();

        // Build buffer
        buffer = new ByteProcessor(img);
        buffer.threshold(127);

        // Keep a copy on disk?
        if (constants.keepSkeleton.isSet()) {
            ImageUtil.saveOnDisk(img, sheet.getId() + ".skl");
        }

        buf = buffer;

        return img;
    }

    //--------//
    // getDir //
    //--------//
    /**
     * Report the precise heading that goes from point 'from' to point 'to'.
     *
     * @param from p1
     * @param to   p2
     * @return heading p1 &rarr; p2
     */
    public static int getDir (Point from,
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
    public static boolean isJunction (int pix)
    {
        return (pix >= JUNCTION) && (pix <= (JUNCTION + 10));
    }

    //---------------------//
    // isJunctionProcessed //
    //---------------------//
    /**
     * Tell whether the pixel value indicates a junction point already processed.
     *
     * @param pix pixel gray value
     * @return true if junction already processed
     */
    public static boolean isJunctionProcessed (int pix)
    {
        return pix == JUNCTION_PROCESSED;
    }

    //-------------//
    // isProcessed //
    //-------------//
    /**
     * Tell whether the pixel value indicates an end point of an arc already processed.
     *
     * @param pix pixel gray value
     * @return true if arc already processed
     */
    public static boolean isProcessed (int pix)
    {
        return (pix >= PROCESSED) && (pix < (PROCESSED + 10));
    }

    //--------//
    // isSide //
    //--------//
    /**
     * Tell whether the provided heading is a side one (Horizontal or Vertical).
     *
     * @param dir provided heading
     * @return true if horizontal or vertical
     */
    public static boolean isSide (int dir)
    {
        return (dir % 2) == 0;
    }

    //------------//
    // addVoidArc //
    //------------//
    /**
     * Add a void arc (reduced to its junctions points) into the specific void arcs map.
     *
     * @param arc the void arc to register
     */
    public void addVoidArc (Arc arc)
    {
        for (boolean rev : new boolean[]{true, false}) {
            Point junctionPt = arc.getJunction(rev);
            List<Arc> arcs = voidArcsMap.get(junctionPt);

            if (arcs == null) {
                voidArcsMap.put(junctionPt, arcs = new ArrayList<Arc>());
            }

            arcs.add(arc);
        }
    }

    //----------//
    // getPixel //
    //----------//
    /**
     * Report pixel value at (x, y) location
     *
     * @param x abscissa
     * @param y ordinate
     * @return pixel value
     */
    public int getPixel (int x,
                         int y)
    {
        return buf.get(x, y);
    }

    //-------------//
    // renderItems //
    //-------------//
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

        //        // Render artificial junction points (for vertical parts)
        //        for (Point p : arcsPivots) {
        //            g.setColor(Color.MAGENTA);
        //            g.fillOval(p.x, p.y, 1, 1);
        //        }
    }

    //----------//
    // setPixel //
    //----------//
    /**
     * Set pixel value at provided location
     *
     * @param x   abscissa
     * @param y   ordinate
     * @param val pixel value to set
     */
    public void setPixel (int x,
                          int y,
                          int val)
    {
        buf.set(x, y, val);
    }

    //-----------------//
    // getErasedInters //
    //-----------------//
    /**
     * Report the collection of erased inters, with provided crossable characteristic
     *
     * @param crossable true for crossable, false for non-crossable
     * @return the desired erased inters
     */
    Map<SystemInfo, List<Inter>> getErasedInters (boolean crossable)
    {
        return crossable ? crossables : nonCrossables;
    }

    //----------------//
    // getErasedSeeds //
    //----------------//
    /**
     * @return the erasedSeeds
     */
    Map<SystemInfo, List<Glyph>> getErasedSeeds ()
    {
        return erasedSeeds;
    }

    //----------//
    // setColor //
    //----------//
    /**
     * Paint the arc with a color that indicates its type of arc.
     *
     * @param arc the arc to paint
     * @param g   graphics context
     */
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

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {

        private final Constant.Boolean keepSkeleton = new Constant.Boolean(false,
                                                                           "Should we store skeleton images on disk?");

        private final Constant.Boolean useHeader = new Constant.Boolean(true,
                                                                        "Should we erase the header at staff start");

        private final Scale.Fraction systemVerticalMargin = new Scale.Fraction(2.0,
                                                                               "Margin erased above & below system header area");

        private final Scale.Fraction maxDxFromStaff = new Scale.Fraction(1,
                                                                         "Maximum horizontal gap from any staff");

        private final Scale.Fraction maxDyFromStaff = new Scale.Fraction(10,
                                                                         "Maximum vertical gap from any staff");
    }

    //---------------//
    // CurvesCleaner //
    //---------------//
    /**
     * Class {@code CurvesCleaner} erases shapes and glyphs to prepare curves retrieval.
     * <p>
     * The specificity of this cleaner is to keep track of non-crossable inters that have been
     * erased.
     * Typically, notes and beams are such inters that cannot be crossed by a slur.
     * They are used in the {@link CurvesBuilder} processing when trying to extend a slur arc: an
     * extension is not considered if the resulting slur would cross such non-crossable area.
     */
    private static class CurvesCleaner
            extends PageCleaner
    {

        /**
         * Creates a new CurvesEraser object.
         *
         * @param buffer page buffer
         * @param g      graphics context on buffer
         * @param sheet  related sheet
         */
        public CurvesCleaner (ByteProcessor buffer,
                              Graphics2D g,
                              Sheet sheet)
        {
            super(buffer, g, sheet);
        }

        //---------------------//
        // eraseDistantRegions //
        //---------------------//
        /**
         * In the provided image, erase the areas that lie too far from staves.
         * <p>
         * We define an area as large as image, then remove enlarged staves areas, and finally print
         * the remaining area with white color.
         */
        public void eraseDistantRegions ()
        {
            final Scale scale = sheet.getScale();
            final int maxDx = scale.toPixels(constants.maxDxFromStaff);
            final int maxDy = scale.toPixels(constants.maxDyFromStaff);
            final Area sheetArea = new Area(new Rectangle(buffer.getWidth(), buffer.getHeight()));

            for (Staff staff : sheet.getStaffManager().getStaves()) {
                Rectangle staffRect = null;

                for (VerticalSide vSide : VerticalSide.values()) {
                    LineInfo line = staff.getLine(vSide);

                    for (HorizontalSide hSide : HorizontalSide.values()) {
                        int x = staff.getAbscissa(hSide);
                        int y = line.yAt(x);

                        if (staffRect == null) {
                            staffRect = new Rectangle(x, y, 0, 0);
                        } else {
                            staffRect.add(x, y);
                        }
                    }
                }

                staffRect.grow(maxDx, maxDy);

                Area staffArea = new Area(staffRect);
                sheetArea.subtract(staffArea);
            }

            g.fill(sheetArea);
        }

        //-------------//
        // eraseShapes //
        //-------------//
        /**
         * Erase from image graphics all instances of provided shapes and return the
         * "erased" inter instances per system.
         *
         * @param shapes (input) the shapes to look for
         * @return the corresponding erased inter instances per system
         */
        public Map<SystemInfo, List<Inter>> eraseShapes (Collection<Shape> shapes)
        {
            final Map<SystemInfo, List<Inter>> erasedMap = new TreeMap<SystemInfo, List<Inter>>();

            for (SystemInfo system : sheet.getSystems()) {
                final SIGraph sig = system.getSig();
                final List<Inter> erased = new ArrayList<Inter>();
                erasedMap.put(system, erased);

                for (Inter inter : sig.vertexSet()) {
                    if (!inter.isRemoved() && shapes.contains(inter.getShape())) {
                        if (canHide(inter)) {
                            erased.add(inter);
                        }
                    }
                }

                // Erase the inters
                for (Inter inter : erased) {
                    inter.accept(this);
                }

                // Erase system header?
                if (constants.useHeader.isSet()) {
                    eraseSystemHeader(system, constants.systemVerticalMargin);
                }
            }

            return erasedMap;
        }
    }
}
