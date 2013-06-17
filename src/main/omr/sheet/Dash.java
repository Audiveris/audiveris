//----------------------------------------------------------------------------//
//                                                                            //
//                                  D a s h                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.math.Line;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Comparator;

/**
 * Class {@code Dash} is used to handle a horizontal segment, which can
 * represent a ledger, a legato sign or the horizontal part of an
 * alternate ending.
 *
 * <p>The role of a Dash, as compared to a plain {@link omr.glyph.facets.Glyph}
 * is to handle the horizontal segment (its Line and contour box), even if the
 * underlying stick has been discarded. Doing so saves us the need to serialize
 * the whole horizontal GlyphLag.
 *
 * @author Hervé Bitteur
 */
public abstract class Dash
{
    //~ Static fields/initializers ---------------------------------------------

    /** A comparator based on abscissa of underlying glyph */
    public static final Comparator<Dash> byAbscissa = new Comparator<Dash>()
    {
        @Override
        public int compare (Dash o1,
                            Dash o2)
        {
            return Glyph.byAbscissa.compare(o1.stick, o2.stick);
        }
    };

    //~ Instance fields --------------------------------------------------------
    /** Related staff */
    private StaffInfo staff;

    /** Approximating line */
    private Line line;

    /** Contour box */
    private Rectangle box;

    /** The underlying stick if any */
    private final Glyph stick;

    //~ Constructors -----------------------------------------------------------
    //------//
    // Dash //
    //------//
    /**
     * Creates a new Dash object.
     *
     * @param stick the underlying stick
     * @param staff the nearby staff
     */
    public Dash (Glyph stick,
                 StaffInfo staff)
    {
        this.stick = stick;
        this.staff = staff;

        line = stick.getLine();
        box = stick.getBounds();
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getBounds //
    //-----------//
    /**
     * Report the contour box, horizontally oriented, and so directly usable for
     * display of intersection test.
     *
     * @return the contour box
     */
    public Rectangle getBounds ()
    {
        return box;
    }

    //---------//
    // getLine //
    //---------//
    /**
     * Report the approximating line
     *
     * @return the dash line
     */
    public Line getLine ()
    {
        if (line == null) {
            if (stick != null) {
                line = stick.getLine();
            }
        }

        return line;
    }

    //----------//
    // getStaff //
    //----------//
    /**
     * Report the staff nearby this entity
     *
     * @return the related staff
     */
    public StaffInfo getStaff ()
    {
        return staff;
    }

    //----------//
    // getStick //
    //----------//
    /**
     * Report the underlying stick, or null if not found
     *
     * @return the underlying stick, null otherwise
     */
    public Glyph getStick ()
    {
        return stick;
    }

    //-----------//
    // getDashOf //
    //-----------//
    /**
     * Report whether this dash is based on the provided glyph
     *
     * @param glyph the provided glyph
     * @return true if they are related
     */
    public boolean isDashOf (Glyph glyph)
    {
        return this.stick == glyph;
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the dash.
     *
     * @param g the graphics context
     */
    public void render (Graphics g)
    {
        if (box.intersects(g.getClipBounds())) {
            line = getLine();

            Point start = new Point(
                    box.x,
                    (int) Math.rint(line.yAtX((double) box.x)));
            Point stop = new Point(
                    box.x + box.width,
                    (int) Math.rint(line.yAtX((double) box.x + box.width + 1)));

            g.drawLine(start.x, start.y, stop.x, stop.y);
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
        sb.append(internalsString());

        sb.append("}");

        return sb.toString();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" staff#")
                .append(staff.getId());
        sb.append(" ")
                .append(stick.idString());

        return sb.toString();
    }
}
