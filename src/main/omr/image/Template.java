//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          T e m p l a t e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.math.GeoUtil;
import omr.math.TableUtil;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Class {@code Template} implements a template to be used for matching evaluation on a
 * distance transform image.
 * <p>
 * There are several topics to consider in a template specification (see {@link Key} class):<dl>
 * <dt><b>Base shape</b></dt>
 * <dd>Supported shapes are NOTEHEAD_BLACK, NOTEHEAD_VOID and WHOLE_NOTE and their small (cue)
 * counterparts</dd>
 * <dt><b>Lines</b></dt>
 * <dd>Staff lines and/or ledgers can cross the symbol in the middle, so regions for top or bottom
 * lines are disabled for template matching. </dd>
 * <dt><b>Stems</b></dt>
 * <dd>Experience has shown that stem presence or absence is not reliable, so potential stem regions
 * are disabled for template matching.</dd>
 * <dt><b>Size</b></dt>
 * <dd>Either standard size or small size (for cues and grace notes).</dd>
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
    /** Template key. */
    private final Key key;

    /** Collection of key points defined for this template. */
    private final Collection<PixelDistance> keyPoints;

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
    //----------//
    // Template //
    //----------//
    /**
     * Creates a new Template object with a provided set of points.
     *
     * @param key          the template specification key
     * @param width        template width
     * @param height       template height
     * @param symbolBounds bounds of symbol within template
     * @param keyPoints    the set of defining points
     */
    public Template (Key key,
                     int width,
                     int height,
                     Rectangle symbolBounds,
                     List<PixelDistance> keyPoints)
    {
        this.key = key;
        this.keyPoints = new ArrayList<PixelDistance>(keyPoints);
        this.width = width;
        this.height = height;
        this.symbolBounds = new Rectangle(symbolBounds);
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

        System.out.println("Template " + key + ":");

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
     * Evaluate this template at location (x,y) in provided distanceImage.
     *
     * @param x         location abscissa
     * @param y         location ordinate
     * @param anchor    the anchor kind to use for (x,y), null for upper left
     * @param distances the distance transform image to search
     * @return the quadratic average distance computed on all key positions
     */
    public double evaluate (int x,
                            int y,
                            Anchor anchor,
                            DistanceTable distances)
    {
        // Offsets to apply to location?
        if (anchor != null) {
            Point offset = getOffset(anchor);

            if (offset != null) {
                x -= offset.x;
                y -= offset.y;
            } else {
                logger.error("No {} anchor defined for {} template", anchor, key);
            }
        }

        // Loop through template key positions and read related distance.
        // Compute the mean value on all distances read
        final int imgWidth = distances.getWidth();
        final int imgHeight = distances.getHeight();
        final double foreWeight = constants.foreWeight.getValue();
        final double backWeight = constants.backWeight.getValue();
        double weights = 0; // Sum of weights
        double total = 0; // Sum of square weighted distances

        for (PixelDistance pix : keyPoints) {
            int nx = x + pix.x;
            int ny = y + pix.y;

            // Ignore tested point if located out of image
            if ((nx >= 0) && (nx < imgWidth) && (ny >= 0) && (ny < imgHeight)) {
                double weight = (pix.d > 0) ? backWeight : foreWeight;
                double dist = weight * (distances.getValue(nx, ny) - pix.d);
                total += (dist * dist);
                weights += weight;
            }
        }

        double res = Math.sqrt(total) / (weights * distances.getNormalizer());

        return res;
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
     * @param box    absolute positioning of template box in global image
     * @param image  global image to be read
     * @param margin margin added around glyph (specified in pixels)
     * @return the collection of foreground pixels, relative to template box.
     */
    public List<Point> getForegroundPixels (Rectangle box,
                                            ByteProcessor image,
                                            int margin)
    {
        final List<Point> fores = getForegroundPixels(box, image);

        if (margin <= 0) {
            return fores;
        }

        // Populate an enlarged buffer with these foreground pixels
        final Rectangle bufBox = new Rectangle(box);
        bufBox.grow(margin, margin);

        final ByteProcessor buf = new ByteProcessor(bufBox.width, bufBox.height);
        buf.invert();

        for (Point p : fores) {
            buf.set(p.x + margin, p.y + margin, 0);
        }

        // Dilate the foreground areas
        for (int i = 0; i < margin; i++) {
            buf.dilate();
        }

        // Retrieve the new collection of foreground pixels
        fores.clear();

        for (int y = 0; y < bufBox.height; y++) {
            for (int x = 0; x < bufBox.width; x++) {
                if (buf.get(x, y) == 0) {
                    fores.add(new Point(x - margin, y - margin));
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

    //--------//
    // getKey //
    //--------//
    /**
     * Report the specification key of the template.
     *
     * @return the template specification key
     */
    public Key getKey ()
    {
        return key;
    }

    //-----------//
    // getOffset //
    //-----------//
    @Override
    public Point getOffset (Anchor anchor)
    {
        Point offset = offsets.get(anchor);

        if (offset == null) {
            logger.error("No offset defined for anchor {} in template {}", anchor, key);
        }

        return offset;
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
     * Report the symbol bounds when positioning anchor
     * at location (x,y).
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

        sb.append(" ").append(key);

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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----//
    // Key //
    //-----//
    /**
     * Key to define all template specifications.
     */
    public static class Key
    {
        //~ Instance fields ------------------------------------------------------------------------

        public final Shape shape;

        /** Middle hasLine or not. */
        public final boolean hasLine;

        //~ Constructors ---------------------------------------------------------------------------
        public Key (Shape shape,
                    boolean line)
        {
            this.shape = shape;
            this.hasLine = line;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean equals (Object obj)
        {
            if (!(obj instanceof Key)) {
                return false;
            } else {
                Key that = (Key) obj;

                return (shape == that.shape) && (hasLine == that.hasLine);
            }
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;
            hash = (37 * hash) + Objects.hashCode(this.shape);
            hash = (37 * hash) + (this.hasLine ? 1 : 0);

            return hash;
        }

        /**
         * Key is formatted as shape[-hasLine].
         *
         * @return unique key name
         */
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(shape);

            if (hasLine) {
                sb.append("-LINE");
            }

            return sb.toString();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Ratio smallRatio = new Constant.Ratio(
                0.67,
                "Global ratio applied to small (cue/grace) templates");

        final Constant.Ratio foreWeight = new Constant.Ratio(
                1.0,
                "Weight assigned to template foreground pixels");

        final Constant.Ratio backWeight = new Constant.Ratio(
                2.0,
                "Weight assigned to template background pixels");
    }
}
