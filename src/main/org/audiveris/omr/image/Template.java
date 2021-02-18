//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          T e m p l a t e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.image;

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.math.TableUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.InterlineScale;
import org.audiveris.omr.util.ByteUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class {@code Template} implements a template to be used for matching evaluation on a
 * distance transform image.
 * <p>
 * There are several topics to consider in a template specification:
 * <dl>
 * <dt><b>Base shape</b></dt>
 * <dd>Supported shapes are NOTEHEAD_BLACK, NOTEHEAD_VOID and WHOLE_NOTE and possibly their small
 * (cue) counterparts</dd>
 * <dt><b>Size</b></dt>
 * <dd>Either standard size or small size (for cues and grace notes).</dd>
 * <dt><b>Lines and Stems</b></dt>
 * <dd>These regions will be neutralized in the distance table, hence the templates don't have to
 * cope with them.</dd>
 * </dl>
 * <p>
 * <img alt="Template diagram" src="doc-files/Template.png">
 *
 * @author Hervé Bitteur
 */
public class Template
        implements Anchored
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Template.class);

    /** Ratio applied to small symbols (cue / grace). */
    public static final double smallRatio = constants.smallRatio.getValue();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Template shape. */
    private final Shape shape;

    /** Scaling factor. */
    private final int pointSize;

    /** Collection of key points defined for this template. */
    private final List<PixelDistance> keyPoints;

    /** Template width. (perhaps larger than symbol width) */
    private final int width;

    /** Template height. (perhaps larger than symbol height) */
    private final int height;

    /** Symbol slim bounds relative to template. (perhaps a bit smaller than symbol bounds) */
    private final Rectangle slimBounds;

    /**
     * Offsets to defined anchors.
     * An offset is defined as the translation from template upper left corner
     * to the precise anchor location in the symbol.
     */
    private final Map<Anchor, Point2D> offsets = new EnumMap<>(Anchor.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Template object with a provided set of points.
     *
     * @param shape      the template specified shape
     * @param pointSize  scaling factor
     * @param width      template width
     * @param height     template height
     * @param keyPoints  the set of defining points
     * @param slimBounds symbol slim bounds WRT template bounds
     */
    public Template (Shape shape,
                     int pointSize,
                     int width,
                     int height,
                     List<PixelDistance> keyPoints,
                     Rectangle slimBounds)
    {
        this.shape = shape;
        this.pointSize = pointSize;
        this.keyPoints = new ArrayList<>(keyPoints);
        this.width = width;
        this.height = height;
        this.slimBounds = slimBounds;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // dump //
    //------//
    /**
     * Print this template on standard output.
     */
    public void dump ()
    {
        int[][] vals = new int[width][height];

        for (int[] col : vals) {
            for (int y = 0; y < col.length; y++) {
                col[y] = -1;
            }
        }

        for (PixelDistance pix : keyPoints) {
            vals[pix.x][pix.y] = (int) Math.round(pix.d);
        }

        final String yFormat = TableUtil.printAbscissae(width, height, 3);

        for (int y = 0; y < height; y++) {
            System.out.printf(yFormat, y);

            for (int x = 0; x < width; x++) {
                int val = vals[x][y];

                if (val != -1) {
                    System.out.printf("%3d", val);
                } else {
                    System.out.print("  .");
                }
            }

            System.out.println();
        }

        for (Entry<Anchor, Point2D> entry : offsets.entrySet()) {
            System.out.println(entry.getKey() + " => " + entry.getValue());
        }
    }

    //----------//
    // evaluate //
    //----------//
    /**
     * Evaluate this template at location (x,y) in provided distances table.
     *
     * @param x         pivot location abscissa
     * @param y         pivot location ordinate
     * @param anchor    pivot offset if any, WRT template upper left
     * @param distances the distance table to use
     * @return the weighted average distance computed on all template key positions
     */
    public double evaluate (int x,
                            int y,
                            Anchor anchor,
                            DistanceTable distances)
    {
        final Point ul = upperLeft(x, y, anchor);

        // Loop through template key positions and read related distance.
        // Compute the mean value on all distances read
        final int imgWidth = distances.getWidth();
        final int imgHeight = distances.getHeight();
        final double foreWeight = constants.foreWeight.getValue();
        final double backWeight = constants.backWeight.getValue();
        final double holeWeight = constants.holeWeight.getValue();
        double weights = 0; // Sum of weights
        double total = 0; // Sum of weighted distances

        for (PixelDistance pix : keyPoints) {
            int nx = ul.x + pix.x;
            int ny = ul.y + pix.y;

            // Ignore tested point if located out of image
            if ((nx >= 0) && (nx < imgWidth) && (ny >= 0) && (ny < imgHeight)) {
                int actualDist = distances.getValue(nx, ny);

                // Ignore neutralized locations in distance table
                if (actualDist != ChamferDistance.VALUE_UNKNOWN) {
                    // pix.d < 0 for expected hole, expected negative distance to nearest foreground
                    // pix.d == 0 for expected foreground, 0 distance
                    // pix.d > 0 for expected background, expected distance to nearest foreground
                    double weight = (pix.d == 0) ? foreWeight : ((pix.d > 0) ? backWeight
                            : holeWeight);
                    double expected = (pix.d == 0) ? 0 : 1;
                    double actual = (actualDist == 0) ? 0 : 1;
                    double dist = Math.abs(actual - expected);

                    total += (weight * dist);
                    weights += weight;
                }
            }
        }

        if (weights == 0) {
            return Double.MAX_VALUE; // Safer
        }

        return total / weights;
    }

    //--------------//
    // evaluateHole //
    //--------------//
    /**
     * Evaluate only the <b>hole</b> part of template, by measuring ratio of actual white
     * over expected white in template hole.
     *
     * @param x         pivot location abscissa
     * @param y         pivot location ordinate
     * @param anchor    pivot offset if any, WRT template upper left
     * @param distances the distance table to use
     * @return ratio of actual white pixels over expected hole pixels
     */
    public double evaluateHole (int x,
                                int y,
                                Anchor anchor,
                                DistanceTable distances)
    {
        final Point ul = upperLeft(x, y, anchor);

        // Loop through template key positions and read related distance.
        // Compute the mean value on all distances read
        final int imgWidth = distances.getWidth();
        final int imgHeight = distances.getHeight();
        int expectedHoles = 0; // Expected number of white pixels in hole
        int actualHoles = 0; // Actual number of white pixels in hole

        for (PixelDistance pix : keyPoints) {
            int nx = ul.x + pix.x;
            int ny = ul.y + pix.y;

            // Ignore tested point if located out of image
            if ((nx >= 0) && (nx < imgWidth) && (ny >= 0) && (ny < imgHeight)) {
                int actualDist = distances.getValue(nx, ny);

                // Ignore neutralized locations in distance table
                if (actualDist != ChamferDistance.VALUE_UNKNOWN) {
                    // pix.d < 0 for expected hole, expected negative distance to nearest foreground
                    if (pix.d < 0) {
                        expectedHoles++;

                        if (actualDist != 0) {
                            actualHoles++;
                        }
                    }
                }
            }
        }

        if (expectedHoles == 0) {
            return 0;
        } else {
            return (double) actualHoles / expectedHoles;
        }
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Report the template bounds knowing the symbol box.
     *
     * @param sBox the slim symbol box
     * @return the template box
     */
    public Rectangle getBounds (Rectangle sBox)
    {
        return new Rectangle(sBox.x - slimBounds.x, sBox.y - slimBounds.y, width, height);
    }

    //-------------//
    // getBoundsAt //
    //-------------//
    public Rectangle getBoundsAt (int x,
                                  int y,
                                  Anchor anchor)
    {
        final Point offset = getOffset(anchor);

        return new Rectangle(x - offset.x, y - offset.y, width, height);
    }

    //-------------//
    // getBoundsAt //
    //-------------//
    @Override
    public Rectangle2D getBoundsAt (double x,
                                    double y,
                                    Anchor anchor)
    {
        final Point2D offset = getOffset(anchor);

        return new Rectangle2D.Double(x - offset.getX(), y - offset.getY(), width, height);
    }

    //---------------------//
    // getForegroundPixels //
    //---------------------//
    /**
     * Collect the image foreground pixels located under the template foreground areas.
     *
     * @param tplBox absolute positioning of template box in global image
     * @param image  global image to be read
     * @return the collection of foreground pixels, relative to template box.
     */
    public List<Point> getForegroundPixels (Rectangle tplBox,
                                            ByteProcessor image)
    {
        final int imgWidth = image.getWidth();
        final int imgHeight = image.getHeight();
        final List<Point> fores = new ArrayList<>();

        for (PixelDistance pix : keyPoints) {
            if (pix.d != 0) {
                continue;
            }

            int nx = tplBox.x + pix.x;
            int ny = tplBox.y + pix.y;

            if ((nx >= 0) && (nx < imgWidth) && (ny >= 0) && (ny < imgHeight)) {
                // Check if we have some image foreground there
                int val = image.get(nx, ny);

                if (val == 0) {
                    fores.add(new Point(pix.x, pix.y));
                }
            }
        }

        return fores;
    }

    //---------------------//
    // getForegroundPixels //
    //---------------------//
    /**
     * Collect the image foreground pixels located under the template foreground areas,
     * with some additional margin.
     *
     * @param tplBox  absolute positioning of template box in global image
     * @param image   global image to be read
     * @param dilated true for applying dilation before processing
     * @return the collection of foreground pixels, relative to template box.
     */
    public List<Point> getForegroundPixels (Rectangle tplBox,
                                            ByteProcessor image,
                                            boolean dilated)
    {
        final List<Point> fores = getForegroundPixels(tplBox, image);

        if (!dilated) {
            return fores;
        }

        int dilation = InterlineScale.toPixels(pointSize / 4, constants.dilation);

        // Populate an enlarged buffer with these foreground pixels
        final Rectangle bufBox = new Rectangle(tplBox);
        bufBox.grow(dilation, dilation);

        final ByteProcessor buf = new ByteProcessor(bufBox.width, bufBox.height);
        ByteUtil.raz(buf); //buf.invert();

        for (Point p : fores) {
            buf.set(p.x + dilation, p.y + dilation, 0);
        }

        // Dilate the foreground areas
        for (int i = 0; i < dilation; i++) {
            buf.dilate();
        }

        // Retrieve the new collection of foreground pixels
        fores.clear();

        for (int y = 0; y < bufBox.height; y++) {
            for (int x = 0; x < bufBox.width; x++) {
                if (buf.get(x, y) == 0) {
                    fores.add(new Point(x - dilation, y - dilation));
                }
            }
        }

        return fores;
    }

    //-----------//
    // getHeight //
    //-----------//
    @Override
    public int getHeight ()
    {
        return height;
    }

    //--------------//
    // getKeyPoints //
    //--------------//
    /**
     * Report the collection of defined keyPoints.
     *
     * @return the keyPoints
     */
    public List<PixelDistance> getKeyPoints ()
    {
        return Collections.unmodifiableList(keyPoints);
    }

    //-----------//
    // getOffset //
    //-----------//
    @Override
    public Point getOffset (Anchor anchor)
    {
        final Point2D offset = offsets.get(anchor);

        // We have to round abscissa in symmetrical manner around slim rectangle
        switch (anchor) {
        case MIDDLE_LEFT:
        case LEFT_STEM:
        case BOTTOM_LEFT_STEM:
            return new Point((int) Math.round(offset.getX() - 0.5 - 0.001),
                             (int) Math.round(offset.getY() - 0.5));
        case MIDDLE_RIGHT:
        case RIGHT_STEM:
        case TOP_RIGHT_STEM:
            return new Point((int) Math.round(offset.getX() - 0.5 + 0.001),
                             (int) Math.round(offset.getY() - 0.5));
        default:
        case CENTER:
            return new Point((int) Math.round(offset.getX() - 0.5),
                             (int) Math.round(offset.getY() - 0.5));
        }
    }

    //-------------//
    // getOffset2D //
    //-------------//
    /**
     * Report the precise template offset as a Point2D for the provided anchor.
     *
     * @param anchor provided anchor
     * @return precise offset, relative to template upper left corner.
     */
    public Point2D getOffset2D (Anchor anchor)
    {
        return offsets.get(anchor);
    }

    //------------//
    // getOffsets //
    //------------//
    public Map<Anchor, Point2D> getOffsets ()
    {
        return Collections.unmodifiableMap(offsets);
    }

    //--------------//
    // getPointSize //
    //--------------//
    /**
     * Report the pointSize for this template.
     *
     * @return pointSize value
     */
    public int getPointSize ()
    {
        return pointSize;
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the specified shape of the template.
     *
     * @return the template shape
     */
    public Shape getShape ()
    {
        return shape;
    }

    //---------------//
    // getSlimBounds //
    //---------------//
    public Rectangle getSlimBounds ()
    {
        return new Rectangle(slimBounds);
    }

    //-----------------//
    // getSlimBoundsAt //
    //-----------------//
    /**
     * Report the symbol slim bounds when positioning anchor at location (x,y).
     *
     * @param x      abscissa for anchor
     * @param y      ordinate for anchor
     * @param anchor chosen anchor
     * @return the corresponding slim symbol bounds
     */
    public Rectangle getSlimBoundsAt (int x,
                                      int y,
                                      Anchor anchor)
    {
        final Rectangle tplBox = getBoundsAt(x, y, anchor);
        final Rectangle slimBox = getSlimBounds();
        slimBox.translate(tplBox.x, tplBox.y);

        return slimBox;
    }

    //----------//
    // getWidth //
    //----------//
    @Override
    public int getWidth ()
    {
        return width;
    }

    //-----------//
    // putOffset //
    //-----------//
    @Override
    public final void putOffset (Anchor anchor,
                                 double dx,
                                 double dy)
    {
        offsets.put(anchor, new Point2D.Double(dx, dy));
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('{');

        sb.append(shape);

        sb.append(" w:").append(width).append(",h:").append(height);
        sb.append(" keyPoints:").append(keyPoints.size());

        if ((slimBounds.width != width) || (slimBounds.height != height)) {
            sb.append("\n slim:").append(slimBounds);
        }

        for (Entry<Anchor, Point2D> entry : offsets.entrySet()) {
            final Point2D offset = entry.getValue();
            sb.append("\n ").append(entry.getKey()).append(PointUtil.toString(offset));
        }

        return sb.append("}").toString();
    }

    //-----------//
    // upperLeft //
    //-----------//
    /**
     * Report template upper-left location, knowing anchor location.
     *
     * @param x      pivot abscissa in image
     * @param y      pivot ordinate in image
     * @param anchor chosen anchor
     * @return template upper-left corner in image
     */
    private Point upperLeft (int x,
                             int y,
                             Anchor anchor)
    {
        // Offset to apply to location?
        if (anchor != null) {
            final Point offset = getOffset(anchor);
            return new Point(x - offset.x, y - offset.y);
        }

        return new Point(x, y);
    }

    //----------//
    // impactOf //
    //----------//
    /**
     * Convert a matching distance to an impact value.
     *
     * @param distance matching distance
     * @return resulting impact
     */
    public static double impactOf (double distance)
    {
        return 1 - (distance / maxDistanceHigh());
    }

    //-----------------//
    // maxDistanceHigh //
    //-----------------//
    /**
     * Report the high maximum distance, used to compute grades.
     *
     * @return high maximum
     */
    public static double maxDistanceHigh ()
    {
        return constants.maxDistanceHigh.getValue();
    }

    /**
     * Report the low maximum distance, used to keep candidates.
     *
     * @return low maximum
     */
    //----------------//
    // maxDistanceLow //
    //----------------//
    public static double maxDistanceLow ()
    {
        return constants.maxDistanceLow.getValue();
    }

    /**
     * Report the really bad distance, used to stop any matching test.
     *
     * @return really bad distance
     */
    //-------------------//
    // reallyBadDistance //
    //-------------------//
    public static double reallyBadDistance ()
    {
        return constants.reallyBadDistance.getValue();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio smallRatio = new Constant.Ratio(
                0.67,
                "Global ratio applied to small (cue/grace) templates");

        private final Constant.Ratio foreWeight = new Constant.Ratio(
                5.0, // Was 1.0, now modified for cross heads
                "Weight assigned to template foreground pixels");

        private final Constant.Ratio backWeight = new Constant.Ratio(
                1.0,
                "Weight assigned to template exterior background pixels");

        private final Constant.Ratio holeWeight = new Constant.Ratio(
                4.0, // Was 1.0, now modified for cross heads
                "Weight assigned to template interior background pixels");

        private final Scale.Fraction dilation = new Scale.Fraction(
                0.15,
                "Dilation applied on a note head to be erased");

        private final Constant.Double maxDistanceHigh = new Constant.Double(
                "distance",
                0.5,
                "Maximum matching distance");

        private final Constant.Double maxDistanceLow = new Constant.Double(
                "distance",
                0.40,
                "Good matching distance");

        private final Constant.Double reallyBadDistance = new Constant.Double(
                "distance",
                1.0,
                "Really bad matching distance");
    }
}
