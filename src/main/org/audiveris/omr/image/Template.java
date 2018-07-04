//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          T e m p l a t e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.TableUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.InterlineScale;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.OmrFont;
import org.audiveris.omr.ui.symbol.TemplateSymbol;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.util.ByteUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
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
 * There are several topics to consider in a template specification:<dl>
 * <dt><b>Base shape</b></dt>
 * <dd>Supported shapes are NOTEHEAD_BLACK, NOTEHEAD_VOID and WHOLE_NOTE and possibly their small
 * (cue) counterparts</dd>
 * <dt><b>Size</b></dt>
 * <dd>Either standard size or small size (for cues and grace notes).</dd>
 * <dt><b>Lines and Stems</b></dt>
 * <dd>These regions will be neutralized in the distance table, hence the templates don't have to
 * cope with them.</dd>
 * </dl>
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

    /** Underlying symbol. */
    private final TemplateSymbol symbol;

    /** Collection of key points defined for this template. */
    private final List<PixelDistance> keyPoints;

    /** Template width. (perhaps larger than the symbol width) */
    private final int width;

    /** Template height. (perhaps larger than the symbol height) */
    private final int height;

    /** Symbols bounds within template. (perhaps smaller than template bounds) */
    private final Rectangle symbolBounds;

    /**
     * Offsets to defined anchors.
     * An offset is defined as the translation from template upper left corner
     * to the precise anchor location in the symbol.
     */
    private final Map<Anchor, Point> offsets = new EnumMap<Anchor, Point>(Anchor.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Template object with a provided set of points.
     *
     * @param shape        the template specified shape
     * @param pointSize    scaling factor
     * @param symbol       underlying symbol
     * @param width        template width
     * @param height       template height
     * @param keyPoints    the set of defining points
     * @param symbolBounds symbol bounds
     */
    public Template (Shape shape,
                     int pointSize,
                     TemplateSymbol symbol,
                     int width,
                     int height,
                     List<PixelDistance> keyPoints,
                     Rectangle symbolBounds)
    {
        this.shape = shape;
        this.pointSize = pointSize;
        this.symbol = symbol;
        this.keyPoints = new ArrayList<PixelDistance>(keyPoints);
        this.width = width;
        this.height = height;
        this.symbolBounds = symbolBounds;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // addAnchor //
    //-----------//
    @Override
    public final void addAnchor (Anchor anchor,
                                 int dx,
                                 int dy)
    {
        offsets.put(anchor, new Point(dx, dy));
    }

    //---------------------//
    // buildDecoratedImage //
    //---------------------//
    /**
     * For easy visual checking, build a magnified template image with decorations.
     *
     * @param src source image
     * @return decorated magnified image
     */
    public BufferedImage buildDecoratedImage (BufferedImage src)
    {
        final int r = 50; // Magnifying ratio (rather arbitrary)

        TextFont textFont = new TextFont("SansSerif", Font.PLAIN, (int) Math.rint(r / 3.0));
        BufferedImage img = new BufferedImage(width * r, height * r, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Fill background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width * r, height * r);

        // Draw template symbol
        AffineTransform at = AffineTransform.getScaleInstance(r, r);
        g.drawImage(src, at, null);

        // Draw distances
        for (PixelDistance pix : keyPoints) {
            int d = (int) Math.rint(pix.d);

            if (d != 0) {
                g.drawRect(pix.x * r, pix.y * r, r, r);

                String str = Integer.toString(d);
                TextLayout layout = textFont.layout(str);
                Point location = new Point((pix.x * r) + (r / 2), (pix.y * r) + (r / 2));
                OmrFont.paint(g, layout, location, Alignment.AREA_CENTER);
            }
        }

        // Anchor locations
        g.setColor(Color.GREEN);

        for (Entry<Anchor, Point> entry : offsets.entrySet()) {
            final Point pt = entry.getValue();
            g.drawRoundRect(pt.x * r, pt.y * r, r, r, r / 2, r / 2);

            final Anchor anchor = entry.getKey();
            final String str = anchor.abbreviation().toLowerCase();
            final TextLayout layout = textFont.layout(str);

            if ((anchor == Anchor.LEFT_STEM) || (anchor == Anchor.RIGHT_STEM)) {
                Point location = new Point((pt.x * r) + (r / 2), (pt.y * r) + 2);
                OmrFont.paint(g, layout, location, Alignment.TOP_CENTER);
            } else {
                Point location = new Point((pt.x * r) + (r / 2), (pt.y * r) + (r - 1));
                OmrFont.paint(g, layout, location, Alignment.BASELINE_CENTER);
            }
        }

        g.dispose();

        // Put everything within a frame with x and y values
        BufferedImage frm = new BufferedImage(
                (width + 2) * r,
                (height + 2) * r,
                BufferedImage.TYPE_INT_RGB);
        g = frm.createGraphics();

        // Fill frame background
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, frm.getWidth(), frm.getHeight());

        g.drawImage(img, null, r, r);

        // Draw coordinate values
        g.setColor(Color.BLACK);

        for (int x = 0; x < width; x++) {
            String str = Integer.toString(x);
            TextLayout layout = textFont.layout(str);
            Point north = new Point(((x + 1) * r) + (r / 2), (r / 2));
            OmrFont.paint(g, layout, north, Alignment.AREA_CENTER);

            Point south = new Point(((x + 1) * r) + (r / 2), frm.getHeight() - (r / 2));
            OmrFont.paint(g, layout, south, Alignment.AREA_CENTER);
        }

        for (int y = 0; y < height; y++) {
            String str = Integer.toString(y);
            TextLayout layout = textFont.layout(str);
            Point west = new Point((r / 2), ((y + 1) * r) + (r / 2));
            OmrFont.paint(g, layout, west, Alignment.AREA_CENTER);

            Point east = new Point(frm.getWidth() - (r / 2), ((y + 1) * r) + (r / 2));
            OmrFont.paint(g, layout, east, Alignment.AREA_CENTER);
        }

        return frm;
    }

    //------//
    // dump //
    //------//
    public void dump ()
    {
        int[][] vals = new int[width][height];

        for (int x = 0; x < vals.length; x++) {
            int[] col = vals[x];

            for (int y = 0; y < col.length; y++) {
                col[y] = -1;
            }
        }

        for (PixelDistance pix : keyPoints) {
            vals[pix.x][pix.y] = (int) Math.rint(pix.d);
        }

        System.out.println("Template " + shape + ":");

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

        for (Entry<Anchor, Point> entry : offsets.entrySet()) {
            System.out.println(entry.getKey() + " => " + entry.getValue());
        }
    }

    //----------//
    // evaluate //
    //----------//
    /**
     * Evaluate this template at location (x,y) in provided distances table.
     *
     * @param x         location abscissa
     * @param y         location ordinate
     * @param anchor    the anchor kind to use for (x,y), null for upper left
     * @param distances the distance table to search
     * @return the weighted average distance computed on all key positions
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
                    double weight = (pix.d == 0) ? foreWeight : ((pix.d > 0) ? backWeight : holeWeight);
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
     * Evaluate hole of this template at location (x,y) in provided distances table.
     *
     * @param x         location abscissa
     * @param y         location ordinate
     * @param anchor    the anchor kind to use for (x,y), null for upper left
     * @param distances the distance table to search
     * @return the ratio of actual white pixels over expected ones
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
     * Report the bounds of descriptor knowing the symbol box.
     *
     * @param symBox the symbol box
     * @return the descriptor box
     */
    public Rectangle getBounds (Rectangle symBox)
    {
        Point center = GeoUtil.centerOf(symBox);

        return getBoundsAt(center.x, center.y, Anchor.CENTER);
    }

    //-------------//
    // getBoundsAt //
    //-------------//
    @Override
    public Rectangle getBoundsAt (int x,
                                  int y,
                                  Anchor anchor)
    {
        final Point offset = getOffset(anchor);

        return new Rectangle(x - offset.x, y - offset.y, width, height);
    }

    //---------------------//
    // getForegroundPixels //
    //---------------------//
    /**
     * Collect the image foreground pixels located under the template foreground areas.
     *
     * @param box   absolute positioning of template box in global image
     * @param image global image to be read
     * @return the collection of foreground pixels, relative to template box.
     */
    public List<Point> getForegroundPixels (Rectangle box,
                                            ByteProcessor image)
    {
        final int imgWidth = image.getWidth();
        final int imgHeight = image.getHeight();
        final List<Point> fores = new ArrayList<Point>();

        for (PixelDistance pix : keyPoints) {
            if (pix.d != 0) {
                continue;
            }

            int nx = box.x + pix.x;
            int ny = box.y + pix.y;

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
     * @param box     absolute positioning of template box in global image
     * @param image   global image to be read
     * @param dilated true for applying dilation before processing
     * @return the collection of foreground pixels, relative to template box.
     */
    public List<Point> getForegroundPixels (Rectangle box,
                                            ByteProcessor image,
                                            boolean dilated)
    {
        final List<Point> fores = getForegroundPixels(box, image);

        if (!dilated) {
            return fores;
        }

        int dilation = InterlineScale.toPixels(pointSize / 4, constants.dilation);

        // Populate an enlarged buffer with these foreground pixels
        final Rectangle bufBox = new Rectangle(box);
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
    // getPointSize //
    //--------------//
    public int getPointSize ()
    {
        return pointSize;
    }

    //--------------//
    // getKeyPoints //
    //--------------//
    /**
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
        Point offset = offsets.get(anchor);

        if (offset == null) {
            logger.error("No offset defined for anchor {} in template {}", anchor, shape);
        }

        return offset;
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

    //-----------//
    // getSymbol //
    //-----------//
    /**
     * @return the symbol
     */
    public TemplateSymbol getSymbol ()
    {
        return symbol;
    }

    //-----------------//
    // getSymbolBounds //
    //-----------------//
    /**
     * Report the symbol bounds <b>within</b> the template.
     *
     * @return the symbolBounds
     */
    public Rectangle getSymbolBounds ()
    {
        return symbolBounds;
    }

    //-------------------//
    // getSymbolBoundsAt //
    //-------------------//
    /**
     * Report the symbol bounds when positioning anchor at location (x,y).
     *
     * @param x      abscissa for anchor
     * @param y      ordinate for anchor
     * @param anchor chosen anchor
     * @return the corresponding symbol bounds
     */
    public Rectangle getSymbolBoundsAt (int x,
                                        int y,
                                        Anchor anchor)
    {
        Rectangle tplBox = getBoundsAt(x, y, anchor);
        Rectangle symBox = new Rectangle(symbolBounds);
        symBox.translate(tplBox.x, tplBox.y);

        return symBox;
    }

    //----------//
    // getWidth //
    //----------//
    @Override
    public int getWidth ()
    {
        return width;
    }

    //----------//
    // impactOf //
    //----------//
    /**
     * Convert a matching distance to an impact value.
     * <p>
     * TODO: Should we boost value for whole heads here (no support from stem is expected)?
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Template");

        sb.append(" ").append(shape);

        sb.append(" w:").append(width).append(",h:").append(height);

        if ((symbolBounds.width != width) || (symbolBounds.height != height)) {
            sb.append(" sym:").append(symbolBounds);
        }

        for (Entry<Anchor, Point> entry : offsets.entrySet()) {
            sb.append(" ").append(entry.getKey()).append(":(").append(entry.getValue().x).append(
                    ",").append(entry.getValue().y).append(")");
        }

        sb.append(" keyPoints:").append(keyPoints.size());

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // upperLeft //
    //-----------//
    private Point upperLeft (int x,
                             int y,
                             Anchor anchor)
    {
        // Offsets to apply to location?
        if (anchor != null) {
            Point offset = getOffset(anchor);

            if (offset != null) {
                x -= offset.x;
                y -= offset.y;
            } else {
                logger.error("No {} anchor defined for {} template", anchor, shape);
            }
        }

        return new Point(x, y);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio smallRatio = new Constant.Ratio(
                0.67,
                "Global ratio applied to small (cue/grace) templates");

        private final Constant.Ratio foreWeight = new Constant.Ratio(
                1.0,
                "Weight assigned to template foreground pixels");

        private final Constant.Ratio backWeight = new Constant.Ratio(
                1.0,
                "Weight assigned to template exterior background pixels");

        private final Constant.Ratio holeWeight = new Constant.Ratio(
                1.0,
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
