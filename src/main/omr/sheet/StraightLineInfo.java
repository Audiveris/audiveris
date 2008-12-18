//----------------------------------------------------------------------------//
//                                                                            //
//                      S t r a i g h t L i n e I n f o                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.glyph.GlyphSection;

import omr.math.Line;

import omr.stick.Stick;

import omr.ui.view.Zoom;

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
     * @param right computed  abscissa of the right line end
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
    public int getLeft ()
    {
        return left;
    }

    //----------//
    // getRight //
    //----------//
    public int getRight ()
    {
        return right;
    }

    //---------//
    // cleanup //
    //---------//
    public void cleanup ()
    {
        builder.cleanup();
    }

    //--------//
    // render //
    //--------//
    public void render (Graphics g,
                        Zoom     z,
                        int      left,
                        int      right)
    {
        // Paint the computed line
        if (line != null) {
            g.drawLine(
                z.scaled(left + 0.5),
                z.scaled(line.yAt(left + 0.5) + 0.5),
                z.scaled(right + 0.5),
                z.scaled(line.yAt(right + 0.5) + 0.5));
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
    public double yAt (double x)
    {
        if (line == null) {
            throw new RuntimeException("No line defined");
        }

        return line.yAt(x);
    }

    //-------------//
    // getSections //
    //-------------//
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
}
