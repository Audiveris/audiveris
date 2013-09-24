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

import omr.ui.symbol.Alignment;
import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class {@code Template} implements a template to be used for
 * matching evaluation on a distance transform image.
 *
 * @author Hervé Bitteur
 */
public class Template
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            Template.class);

    //~ Enumerations -----------------------------------------------------------
    /** Specifies a reference location within template. */
    public enum Anchor
    {
        //~ Enumeration constant initializers ----------------------------------

        /**
         * Area Center.
         */
        CENTER,
        /**
         * Upper left corner.
         */
        TOP_LEFT,
        /**
         * Middle left corner.
         */
        MIDDLE_LEFT,
        /**
         * Lower left corner.
         */
        BOTTOM_LEFT,
        /**
         * X at left stem, Y at top.
         */
        TOP_LEFT_STEM,
        /**
         * X at left stem, Y at middle.
         */
        LEFT_STEM,
        /**
         * X at left stem, Y at bottom.
         */
        BOTTOM_LEFT_STEM,
        /**
         * X at right stem, Y at top.
         */
        TOP_RIGHT_STEM,
        /**
         * X at right stem, Y at middle.
         */
        RIGHT_STEM,
        /**
         * X at right stem, Y at bottom.
         */
        BOTTOM_RIGHT_STEM;

    }

    //~ Instance fields --------------------------------------------------------
    /** Template shape. */
    private final Shape shape;

    /** Collection of key points defined for this template. */
    private final Collection<PixelDistance> keyPoints;

    /** Template width. */
    private final int width;

    /** Template height. */
    private final int height;

    /** Offsets to anchors, if any. */
    private final Map<Anchor, Point> offsets = new EnumMap<Anchor, Point>(
            Anchor.class);

    /** Image for nice drawing, if any. */
    private final ShapeSymbol symbol;

    //~ Constructors -----------------------------------------------------------
    //----------//
    // Template //
    //----------//
    /**
     * Creates a new Template object from a provided set of points.
     *
     * @param shape     the template shape
     * @param width     template width
     * @param height    template height
     * @param keyPoints the set of defining points.
     * @param symbol    the symbol to use for drawing, or null
     */
    public Template (Shape shape,
                     int width,
                     int height,
                     List<PixelDistance> keyPoints,
                     ShapeSymbol symbol)
    {
        this.shape = shape;
        this.keyPoints = new ArrayList<PixelDistance>(keyPoints);
        this.width = width;
        this.height = height;
        this.symbol = symbol;

        // Define common anchors
        addAnchor(Anchor.CENTER, 0.5, 0.5);
        addAnchor(Anchor.TOP_LEFT, 0, 0);
        addAnchor(Anchor.MIDDLE_LEFT, 0, 0.5);
        addAnchor(Anchor.BOTTOM_LEFT, 0, 1);
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // addAnchor //
    //-----------//
    /**
     * Assign a relative offset for an anchor type.
     *
     * @param anchor the anchor type
     * @param xRatio the abscissa offset from upper left corner, specified as
     *               ratio of template width
     * @param yRatio the ordinate offset from upper left corner, specified as
     *               ratio of template height
     */
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
     * Evaluate this template at location (x,y) in provided
     * distanceImage.
     *
     * @param x         location abscissa
     * @param y         location ordinate
     * @param anchor    the anchor kind to use for (x,y), null for upper left
     * @param distances the distance transform image to search
     * @return the SQUARE of quadratic average distance computed on all key
     *         positions
     */
    public double evaluate (int x,
                            int y,
                            Anchor anchor,
                            Table distances)
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
                        shape);
            }
        }

        // Loop through template key positions and read related distance.
        // Compute the mean value on all distances read
        double total = 0;

        for (PixelDistance pix : keyPoints) {
            double dist = (distances.getValue(x + pix.x, y + pix.y) - pix.d) / 3d;
            total += (dist * dist);
        }

        return total / keyPoints.size();
    }

    //----------//
    // getBoxAt //
    //----------//
    /**
     * Report the template bounds when positioning template anchor at
     * location (x,y).
     *
     * @param x      abscissa for anchor
     * @param y      ordinate for anchor
     * @param anchor chosen template anchor
     * @return the corresponding bounds
     */
    public Rectangle getBoxAt (int x,
                               int y,
                               Anchor anchor)
    {
        final Point offset = getOffset(anchor);

        return new Rectangle(x - offset.x, y - offset.y, width, height);
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the template height
     *
     * @return the maximum ordinate of key points
     */
    public int getHeight ()
    {
        return height;
    }

    //-----------//
    // getOffset //
    //-----------//
    /**
     * Report the offset from template upper left corner to the
     * provided anchor.
     *
     * @param anchor the desired anchor
     * @return the corresponding offset (vector from UL to anchor)
     */
    public Point getOffset (Anchor anchor)
    {
        return offsets.get(anchor);
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the underlying shape of the template.
     *
     * @return the template shape
     */
    public Shape getShape ()
    {
        return shape;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the template width
     *
     * @return the maximum abscissa of key points
     */
    public int getWidth ()
    {
        return width;
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the template at location (x,y) in provided graphics
     * environment.
     *
     * @param x         abscissa of template location
     * @param y         ordinate of template location
     * @param g         graphics environment
     * @param musicFont the (properly scaled) music font to be used
     */
    public void render (int x,
                        int y,
                        Graphics2D g,
                        MusicFont musicFont)
    {
        if (symbol != null) {
            // Use the nice image built with MusicFont
            symbol.paintSymbol(
                    g,
                    musicFont,
                    new Point(x, y),
                    Alignment.TOP_LEFT);
        } else {
            // Simplistic approach, just draw the foreground key points...
            final int d = 1;
            final int side = 1 + (2 * d);

            for (PixelDistance pix : keyPoints) {
                if (pix.d == 0) {
                    g.fillRect((x + pix.x) - d, (y + pix.y) - d, side, side);
                }
            }
        }
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
                .append(shape);

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

//        for (PixelDistance pix : keyPoints) {
//            sb.append(" (")
//                    .append(pix.x)
//                    .append(",")
//                    .append(pix.y);
//
//            if (pix.d != 0) {
//                sb.append(",")
//                        .append(String.format("%.1f", pix.d));
//            }
//
//            sb.append(")");
//        }

        sb.append("}");

        return sb.toString();
    }
}
