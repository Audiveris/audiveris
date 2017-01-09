//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          T e m p l a t e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.TemplateSymbol;
import org.audiveris.omr.util.ByteUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
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
 * <dt><b>Lines & Stems</b></dt>
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
    private final int interline;

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
     * @param shape     the template specified shape
     * @param interline scaling factor
     * @param symbol    underlying symbol
     * @param width     template width
     * @param height    template height
     * @param keyPoints the set of defining points
     */
    public Template (Shape shape,
                     int interline,
                     TemplateSymbol symbol,
                     int width,
                     int height,
                     List<PixelDistance> keyPoints)
    {
        this.shape = shape;
        this.interline = interline;
        this.symbol = symbol;
        this.keyPoints = new ArrayList<PixelDistance>(keyPoints);
        this.width = width;
        this.height = height;

        symbolBounds = new Rectangle(symbol.getSymbolBounds(MusicFont.getFont(interline)));
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // addAnchor //
    //-----------//
    @Override
    public final void addAnchor (Anchor anchor,
                                 double xRatio,
                                 double yRatio)
    {
        offsets.put(
                anchor,
                new Point((int) Math.rint(xRatio * width), (int) Math.rint(yRatio * height)));
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
     * @return the quadratic average distance computed on all key positions
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
        double weights = 0; // Sum of weights
        double total = 0; // Sum of weighted square distances

        for (PixelDistance pix : keyPoints) {
            int nx = ul.x + pix.x;
            int ny = ul.y + pix.y;

            // Ignore tested point if located out of image
            if ((nx >= 0) && (nx < imgWidth) && (ny >= 0) && (ny < imgHeight)) {
                int tableDist = distances.getValue(nx, ny);

                // Ignore neutralized locations in distance table
                if (tableDist != ChamferDistance.VALUE_UNKNOWN) {
                    double weight = (pix.d > 0) ? backWeight : foreWeight;
                    weights += weight;

                    double dist = tableDist - pix.d;
                    total += (weight * (dist * dist)); // Square
                }
            }
        }

        return Math.sqrt(total / weights) / distances.getNormalizer();
    }

    //-----------------//
    // getDescLocation //
    //-----------------//
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
            if (pix.d > 0) {
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

        int dilation = new Scale(interline).toPixels(constants.dilation);

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
    // getInterline //
    //--------------//
    public int getInterline ()
    {
        return interline;
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
                2.0,
                "Weight assigned to template foreground pixels");

        private final Constant.Ratio backWeight = new Constant.Ratio(
                1.0,
                "Weight assigned to template background pixels");

        private final Scale.Fraction dilation = new Scale.Fraction(
                0.15,
                "Dilation applied on a note head to be erased");
    }
}
