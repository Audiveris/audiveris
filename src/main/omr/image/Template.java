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

import omr.math.TableUtil;

import omr.ui.symbol.Alignment;
import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
         * Used for WHOLE based symbols.
         */
        CENTER,
        /**
         * X at left stem, Y at center.
         * Used for VOID HEAD based symbols.
         */
        LEFT_STEM,
        /** X at right stem, Y at center.
         * Used for VOID HEAD based symbols.
         */
        RIGHT_STEM;

    }

    //~ Instance fields --------------------------------------------------------
    /** Template name, for debugging mainly. */
    private final String name;

    /** Collection of key points defined for this template. */
    private final Collection<PixDistance> keyPoints;

    /** Template width. */
    private final int width;

    /** Template height. */
    private final int height;

    /** Offsets to anchors, if any. */
    private final Map<Anchor, Point> offsets = new HashMap<Anchor, Point>();

    /** Image for nice drawing, if any. */
    private final ShapeSymbol symbol;

    //~ Constructors -----------------------------------------------------------
    //----------//
    // Template //
    //----------//
    /**
     * Creates a new Template object from a provided set of points.
     *
     * @param name      the template name
     * @param width     template width
     * @param height    template height
     * @param keyPoints the set of defining points.
     * @param symbol    the symbol to use for drawing, or null
     */
    public Template (String name,
                     int width,
                     int height,
                     List<PixDistance> keyPoints,
                     ShapeSymbol symbol)
    {
        this.name = name;
        this.keyPoints = new ArrayList<PixDistance>(keyPoints);
        this.width = width;
        this.height = height;
        this.symbol = symbol;

        setAnchor(Anchor.CENTER, 0.5, 0.5);
    }

    //~ Methods ----------------------------------------------------------------
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

        for (PixDistance pix : keyPoints) {
            vals[pix.x][pix.y] = (int) Math.rint(pix.d);
        }

        System.out.println("Template " + name + ":");

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
                            double[][] distances)
    {
        // Offsets to apply to location?
        if (anchor != null) {
            Point offset = offsets.get(anchor);

            if (offset != null) {
                x -= offset.x;
                y -= offset.y;
            } else {
                logger.warn(
                        "No {} anchor defined for {} template",
                        anchor,
                        name);
            }
        }

        // Loop through template key positions and read related distance.
        // Compute the mean value on all distances read
        double total = 0;

        for (PixDistance pix : keyPoints) {
            double dist = distances[x + pix.x][y + pix.y] - pix.d;
            total += (dist * dist);
        }

        return total / keyPoints.size();
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

            for (PixDistance pix : keyPoints) {
                if (pix.d == 0) {
                    g.fillRect((x + pix.x) - d, (y + pix.y) - d, side, side);
                }
            }
        }
    }

    //-----------//
    // setAnchor //
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
    public final void setAnchor (Anchor anchor,
                                 double xRatio,
                                 double yRatio)
    {
        offsets.put(
                anchor,
                new Point(
                (int) Math.rint(xRatio * width),
                (int) Math.rint(yRatio * height)));
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
                .append(name);

        for (Entry<Anchor, Point> entry : offsets.entrySet()) {
            sb.append(" ")
                    .append(entry.getKey())
                    .append(":(")
                    .append(entry.getValue().x)
                    .append(",")
                    .append(entry.getValue().y)
                    .append(")");
        }

        sb.append(" count:")
                .append(keyPoints.size());

        for (PixDistance pix : keyPoints) {
            sb.append(" (")
                    .append(pix.x)
                    .append(",")
                    .append(pix.y);

            if (pix.d != 0) {
                sb.append(",")
                        .append(String.format("%.1f", pix.d));
            }

            sb.append(")");
        }

        sb.append("}");

        return sb.toString();
    }
}
