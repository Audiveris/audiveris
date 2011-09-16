//----------------------------------------------------------------------------//
//                                                                            //
//                      S t r a i g h t L i n e I n f o                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.GlyphSection;
import omr.glyph.facets.Stick;

import omr.math.Line;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.grid.LineInfo;

import omr.util.HorizontalSide;
import omr.util.Implement;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class <code>StraightLineInfo</code> implements the LineInfo interface with
 * a simple straight line, that should be sufficient for printed music.
 *
 * @author Hervé Bitteur
 */
public class StraightLineInfo
    implements LineInfo
{
    //~ Instance fields --------------------------------------------------------

    /** Related Builder */
    private final LineBuilder builder;

    /** Best line equation */
    private final Line line;

    /** Just a sequential id for debug */
    private final int id;

    /** Abscissa of left edge */
    private final int left;

    /** Abscissa of right edge */
    private final int right;

    /** Left point */
    private final PixelPoint leftPoint;

    /** Right point */
    private final PixelPoint rightPoint;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // StraightLineInfo //
    //------------------//
    /**
     * Create info about one (straight) staff line
     *
     * @param id the line id (debug)
     * @param left computed abscissa of the left line end
     * @param right computed abscissa of the right line end
     * @param builder the computing of the line
     * @param line the underlying straight line
     * */
    public StraightLineInfo (int         id,
                             int         left,
                             int         right,
                             LineBuilder builder,
                             Line        line)
    {
        this.id = id;
        this.left = left;
        this.right = right;
        this.builder = builder;
        this.line = line;

        leftPoint = new PixelPoint(left, line.yAtX(left));
        rightPoint = new PixelPoint(right, line.yAtX(right));
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getContourBox //
    //---------------//
    @Implement(LineInfo.class)
    public PixelRectangle getContourBox ()
    {
        Rectangle rect = new Rectangle(leftPoint);
        rect.add(rightPoint);

        // Trick to avoid empty area, and allow use of standard intersection
        if (rect.height < 1) {
            rect.height = 1;
        }

        return new PixelRectangle(rect);
    }

    //-------------//
    // getEndPoint //
    //-------------//
    public PixelPoint getEndPoint (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return leftPoint;
        } else {
            return rightPoint;
        }
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the id of this line
     *
     * @return the line id (debugging info)
     */
    public int getId ()
    {
        return id;
    }

    //--------------//
    // getLeftPoint //
    //--------------//
    public PixelPoint getLeftPoint ()
    {
        return leftPoint;
    }

    //---------------//
    // getRightPoint //
    //---------------//
    public PixelPoint getRightPoint ()
    {
        return rightPoint;
    }

    //-------------//
    // getSections //
    //-------------//
    @Implement(LineInfo.class)
    public Collection<GlyphSection> getSections ()
    {
        List<GlyphSection> members = new ArrayList<GlyphSection>();

        // Browse Sticks
        for (Stick stick : builder.getSticks()) {
            // Browse member sections
            for (GlyphSection section : stick.getMembers()) {
                members.add(section);
            }
        }

        return members;
    }

    //---------//
    // cleanup //
    //---------//
    @Implement(LineInfo.class)
    public void cleanup ()
    {
        builder.cleanup();
    }

    //--------//
    // render //
    //--------//
    @Implement(LineInfo.class)
    public void render (Graphics2D g)
    {
        // Paint the computed line
        if (line != null) {
            g.drawLine(left, line.yAtX(left), right, line.yAtX(right));
        }
    }

    //----------//
    // toString //
    //----------//
    /**
     * A readable information string
     *
     * @return a short description
     */
    @Override
    public String toString ()
    {
        return "LineInfo" + id + " left=" + left + " right=" + right +
               ((line != null) ? line.toString() : "");
    }

    //-----//
    // yAtX //
    //-----//
    @Implement(LineInfo.class)
    public int yAt (int x)
    {
        if (line == null) {
            throw new RuntimeException("No line defined");
        }

        return line.yAtX(x);
    }

    //-----//
    // yAtX //
    //-----//
    @Implement(LineInfo.class)
    public double yAt (double x)
    {
        if (line == null) {
            throw new RuntimeException("No line defined");
        }

        return line.yAtX(x);
    }
}
