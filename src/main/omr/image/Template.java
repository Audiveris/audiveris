//----------------------------------------------------------------------------//
//                                                                            //
//                                T e m p l a t e                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import omr.glyph.Shape;

import omr.math.TableUtil;

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
 * Class {@code Template} implements a template to be used for
 * matching evaluation on a distance transform image.
 *
 * <p>
 * There are several topics to consider in a template specification
 * (see {@link Key} class):<dl>
 *
 * <dt><b>Base shape</b></dt>
 * <dd>Supported shapes are NOTEHEAD_BLACK, NOTEHEAD_VOID and WHOLE_NOTE.</dd>
 *
 * <dt><b>Half</b></dt>
 * <dd>Height is constrained by interhasLine. But to cope with variation in symbol
 width, we support separate detection of LEFT and RIGHT halves.</dd>
 *
 * <dt><b>Lines</b></dt>
 * <dd>Staff hasLines and/or ledgers can be stuck to symbol, so we consider NONE,
 LINE_TOP, LINE_MIDDLE, LINE_BOTTOM and LINE_DOUBLE (top & bottom) for all
 * supported shapes</dd>
 *
 * <dt><b>Stems</b></dt>
 * <dd>Except for WHOLE_NOTE shape, symbols have stems portions stuck to them,
 * so for a given half we consider NONE (for WHOLE_NOTE), LINE_TOP, LINE_BOTTOM
 * and
 * LINE_DOUBLE.</dd>
 *
 * <dt><b>Size</b></dt>
 * <dd>Either standard size or small size (for cues and grace notes).</dd>
 *
 * </dl>
 *
 * @author Hervé Bitteur
 */
public class Template
        implements Anchored
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            Template.class);

    //~ Enumerations -----------------------------------------------------------
    public enum Lines
    {
        //~ Enumeration constant initializers ----------------------------------

        /** No hasLine at all. (for standard sizes) */
        LINE_NONE,
        /** Only one hasLine above. */
        LINE_TOP,
        /** Only one hasLine in the middle. (For even pitches) */
        LINE_MIDDLE,
        /** Only one hasLine below. */
        LINE_BOTTOM,
        /** Two hasLines, one above and one below. */
        LINE_DOUBLE;
    }

    public enum Stems
    {
        //~ Enumeration constant initializers ----------------------------------

        /** No stem at all. (for wholes) */
        STEM_NONE,
        /** Stem on left, lower half. */
        STEM_LEFT_BOTTOM,
        /** Stem on left, full length. */
        STEM_LEFT,
        /** Stem on right, upper half. */
        STEM_RIGHT_TOP,
        /** Stem on right, full length. */
        STEM_RIGHT,
        /** Lower stem half on left and upper stem half on right. */
        STEM_DOUBLE;
    }

    //~ Instance fields --------------------------------------------------------
    /** Template key. */
    private final Key key;

    /** Collection of key points defined for this template. */
    private final Collection<PixelDistance> keyPoints;

    /** Template width. */
    private final int width;

    /** Template height. */
    private final int height;

    /** Offsets to anchors, if any. */
    private final Map<Anchor, Point> offsets = new EnumMap<Anchor, Point>(
            Anchor.class);

    //~ Constructors -----------------------------------------------------------
    //----------//
    // Template //
    //----------//
    /**
     * Creates a new Template object from a provided set of points.
     *
     * @param key       the template specification key
     * @param width     template width
     * @param height    template height
     * @param keyPoints the set of defining points
     */
    public Template (Key key,
                     int width,
                     int height,
                     List<PixelDistance> keyPoints)
    {
        this.key = key;
        this.keyPoints = new ArrayList<PixelDistance>(keyPoints);
        this.width = width;
        this.height = height;

        // Define common basic anchors
        addAnchor(Anchor.CENTER, 0.5, 0.5);
        addAnchor(Anchor.TOP_LEFT, 0, 0);
        addAnchor(Anchor.MIDDLE_LEFT, 0, 0.5);
        addAnchor(Anchor.BOTTOM_LEFT, 0, 1);

        ///dump();
    }

    //~ Methods ----------------------------------------------------------------
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
                new Point(
                (int) Math.rint(xRatio * width),
                (int) Math.rint(yRatio * height)));
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
     * Evaluate this template at location (x,y) in provided
     * distanceImage.
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
                logger.warn(
                        "No {} anchor defined for {} template",
                        anchor,
                        key);
            }
        }

        // Loop through template key positions and read related distance.
        // Compute the mean value on all distances read
        double total = 0;

        for (PixelDistance pix : keyPoints) {
            double dist = distances.getValue(x + pix.x, y + pix.y) - pix.d;
            total += (dist * dist);
        }

        double res = Math.sqrt(total) / (keyPoints.size() * distances.getNormalizer());

        return res;
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
            logger.error(
                    "No offset defined for anchor {} in template {}",
                    anchor,
                    key);
        }

        return offset;
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
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        sb.append(" ")
                .append(key);

        for (Entry<Anchor, Point> entry : offsets.entrySet()) {
            sb.append(" ")
                    .append(entry.getKey())
                    .append(":(")
                    .append(entry.getValue().x)
                    .append(",")
                    .append(entry.getValue().y)
                    .append(")");
        }

        sb.append(" keyPoints:")
                .append(keyPoints.size());

        sb.append("}");

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----//
    // Key //
    //-----//
    /**
     * Key to define all template specifications.
     */
    public static class Key
    {
        //~ Instance fields ----------------------------------------------------

        public final Shape shape;

        /** Middle hasLine or not. */
        public final boolean hasLine;

        //~ Constructors -------------------------------------------------------
        public Key (Shape shape,
                    boolean line)
        {
            this.shape = shape;
            this.hasLine = line;
        }

        //~ Methods ------------------------------------------------------------
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
            hash = 37 * hash + Objects.hashCode(this.shape);
            hash = 37 * hash + (this.hasLine ? 1 : 0);
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
}
