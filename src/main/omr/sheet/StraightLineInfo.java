//----------------------------------------------------------------------------//
//                                                                            //
//                      S t r a i g h t L i n e I n f o                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.glyph.GlyphSection;

import omr.math.Line;

import omr.stick.Stick;

import omr.util.Implement;

import java.awt.Graphics;
import java.util.*;

/**
 * Class <code>StraightLineInfo</code> implements the LineInfo interface with
 * a simple straight line, that should be sufficient for printed music.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
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
    }

    //~ Methods ----------------------------------------------------------------

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

    //---------//
    // getLeft //
    //---------//
    @Implement(LineInfo.class)
    public int getLeft ()
    {
        return left;
    }

    //----------//
    // getRight //
    //----------//
    @Implement(LineInfo.class)
    public int getRight ()
    {
        return right;
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
    public void render (Graphics g,
                        int      left,
                        int      right)
    {
        // Paint the computed line
        if (line != null) {
            g.drawLine(left, line.yAt(left), right, line.yAt(right));
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
    // yAt //
    //-----//
    @Implement(LineInfo.class)
    public int yAt (int x)
    {
        if (line == null) {
            throw new RuntimeException("No line defined");
        }

        return line.yAt(x);
    }

    //-----//
    // yAt //
    //-----//
    @Implement(LineInfo.class)
    public double yAt (double x)
    {
        if (line == null) {
            throw new RuntimeException("No line defined");
        }

        return line.yAt(x);
    }
}
