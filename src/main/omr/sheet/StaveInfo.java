//-----------------------------------------------------------------------//
//                                                                       //
//                           S t a v e I n f o                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.math.Line;
import omr.ui.Zoom;

import java.awt.*;
import java.util.List;

/**
 * Class <code>StaveInfo</code> handles the physical details of a stave
 * with its lines.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class StaveInfo
        implements java.io.Serializable
{
    //~ Instance variables ------------------------------------------------

    // For debug only
    private int id;

    // Lines of the set
    private List<LineInfo> lines;

    // Left extrema
    private int left;

    // Right extrema
    private int right;

    // Top of stave related area
    private int areaTop = -1;

    // Bottom of stave related area
    private int areaBottom = -1;

    // Scale specific to this stave, since various scales in a page may
    // exhibit different scales.
    private Scale scale;

    //~ Constructors ------------------------------------------------------

    //-----------//
    // StaveInfo //
    //-----------//
    /**
     * Create info about a staff, with its contained staff lines
     *
     * @param left abscissa of the left side
     * @param right abscissa of the right side
     * @param scale scale detected for this stave
     * @param lines the collection of contained stave lines
     */
    public StaveInfo (int left,
                      int right,
                      Scale scale,
                      List<LineInfo> lines)
    {
        this.left = left;
        this.right = right;
        this.scale = scale;
        this.lines = lines;
    }

    //~ Methods -----------------------------------------------------------

    //---------------//
    // setAreaBottom //
    //---------------//
    /**
     * Define the ordinate of the bottom of the stave area
     *
     * @param val bottom ordinate
     */
    public void setAreaBottom (int val)
    {
        areaBottom = val;
    }

    //---------------//
    // getAreaBottom //
    //---------------//
    /**
     * Selector for bottom of area
     *
     * @return area bottom
     */
    public int getAreaBottom ()
    {
        return areaBottom;
    }

    //------------//
    // setAreaTop //
    //------------//
    /**
     * Define the ordinate for top of stave area
     *
     * @param val top ordinate
     */
    public void setAreaTop (int val)
    {
        areaTop = val;
    }

    //------------//
    // getAreaTop //
    //------------//
    /**
     * Selector for area top ordinate
     *
     * @return area top ordinate
     */
    public int getAreaTop ()
    {
        return areaTop;
    }

    //--------------//
    // getFirstLine //
    //--------------//
    /**
     * Report the first line in the series
     *
     * @return the first line
     */
    public LineInfo getFirstLine ()
    {
        return lines.get(0);
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the mean height of the stave, between first and last line
     *
     * @return the mean stave height
     */
    public int getHeight ()
    {
        return getScale().interline() * 4;
    }

    //-------------//
    // getLastLine //
    //-------------//
    /**
     * Report the last line in the series
     *
     * @return the last line
     */
    public LineInfo getLastLine ()
    {
        return lines.get(lines.size() - 1);
    }

    //---------//
    // getLeft //
    //---------//
    /**
     * Report the abscissa of the left side of the stave
     *
     * @return the left abscissa
     */
    public int getLeft ()
    {
        return left;
    }

    //----------//
    // getLines //
    //----------//
    /**
     * Report the list of line areas
     *
     * @return the list of lines in this stave
     */
    public List<LineInfo> getLines ()
    {
        return lines;
    }

    //----------//
    // setLines //
    //----------//
    /**
     * For Castor
     *
     * @param lines the list of lines in this stave
     */
    public void setLines (List<LineInfo> lines)
    {
        this.lines = lines;
    }

    //----------//
    // getRight //
    //----------//
    /**
     * Report the abscissa of the right side of the stave
     *
     * @return the right abscissa
     */
    public int getRight ()
    {
        return right;
    }

    //----------//
    // getScale //
    //----------//
    /**
     * Report the specific stave scale, which may have a different
     * interline value than the page average.
     *
     * @return the stave scale
     */
    public Scale getScale ()
    {
        return scale;
    }

    //----------//
    // setScale //
    //----------//
    /**
     * For Castor
     *
     * @param scale the stave scale
     */
    public void setScale (Scale scale)
    {
        this.scale = scale;
    }

    //---------//
    // cleanup //
    //---------//
    /**
     * Forward the cleaning order to each of the stave lines
     */
    public void cleanup ()
    {
        for (LineInfo line : lines) {
            line.cleanup();
        }
    }

    //------//
    // dump //
    //------//
    /**
     * A utility meant for debugging
     */
    public void dump ()
    {
        System.out.println("StaveInfo" + id + " left=" + left + " right="
                           + right);
        int i = 0;

        for (LineInfo line : lines) {
            System.out.println(" LineInfo" + i++ + " " + line.toString());
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Paint the stave lines.
     *
     * @param g the graphics context
     * @param z the display zoom
     */
    public void render (Graphics g,
                        Zoom z)
    {
        // Check with the clipping region
        Rectangle clip = g.getClipBounds();
        Line firstLine = getFirstLine().getLine();
        Line lastLine = getLastLine().getLine();

        if ((firstLine == null) || (lastLine == null)) {
            return;
        }

        final double xl = (double) left;
        final double xr = (double) right;

        // Check that top of stave is visible
        final int yTopLeft = z.scaled(firstLine.yAt(xl) + 0.5);
        final int yTopRight = z.scaled(firstLine.yAt(xr) + 0.5);

        if ((clip.y + clip.height) < Math.max(yTopLeft, yTopRight)) {
            return;
        }

        // Check that bottom of stave is visible
        final int yBottomLeft = z.scaled(lastLine.yAt(xl) + 0.5);
        final int yBottomRight = z.scaled(lastLine.yAt(xr) + 0.5);

        if (clip.y > Math.max(yBottomLeft, yBottomRight)) {
            return;
        }

        // Paint each horizontal line in the set
        for (LineInfo line : lines) {
            line.render(g, z, left, right);
        }

        // Left vertical line
        final int xLeft = z.scaled(xl + 0.5);
        g.drawLine(xLeft, yTopLeft, xLeft, yBottomLeft);

        // Right vertical line
        final int xRight = z.scaled(xr + 0.5);
        g.drawLine(xRight, yTopRight, xRight, yBottomRight);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return a string based on main parameters
     */
    public String toString ()
    {
        return "{Stave id=" + id + " left=" + left + " right=" + right
               + " scale=" + scale.interline() + "}";
    }
}
