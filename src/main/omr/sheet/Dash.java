//----------------------------------------------------------------------------//
//                                                                            //
//                                  D a s h                                   //
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
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;

import omr.math.Line;

import omr.ui.util.UIUtilities;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>Dash</code> is used to handle a horizontal segment, which can
 * represent a ledger, a legato sign or the horizontal part of an alternate
 * ending.
 *
 * <p>The role of a Dash, as compared to a plain {@link omr.glyph.facets.Stick}
 * is to handle the horizontal segment (its Line and contour box), even if the
 * underlying stick has been discarded. Doing so saves us the need to serialize
 * the whole horizontal GlyphLag.
 *
 * @author Hervé Bitteur
 */
public abstract class Dash
{
    //~ Instance fields --------------------------------------------------------

    /** Approximating line */
    private Line line;

    /** Contour box */
    private Rectangle box;

    /** The underlying stick if any */
    private final Stick stick;

    /** The patching sections */
    private Set<GlyphSection> patches = new HashSet<GlyphSection>();

    //~ Constructors -----------------------------------------------------------

    //------//
    // Dash //
    //------//
    /**
     * Creates a new Dash object.
     *
     * @param stick the underlying stick
     */
    public Dash (Stick stick)
    {
        this.stick = stick;
        line = stick.getLine();
        box = stick.getContourBox();
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getContourBox //
    //---------------//
    /**
     * Report the contour box, horizontally oriented, and so directly usable for
     * display of intersection test.
     *
     * @return the contour box
     */
    public Rectangle getContourBox ()
    {
        return box;
    }

    //-----------//
    // getDashOf //
    //-----------//
    /**
     * Report whether this dash is based on the provided glyph
     * @param glyph the provided glyph
     * @return true if they are related
     */
    public boolean isDashOf (Glyph glyph)
    {
        return this.stick == glyph;
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

    //------------//
    // setPatches //
    //------------//
    /**
     * Remember the set of patches
     * @param patches the patches to remember
     */
    public void setPatches (Collection<GlyphSection> patches)
    {
        if (this.getPatches() != patches) {
            this.getPatches()
                .clear();
            this.getPatches()
                .addAll(patches);
        }
    }

    //------------//
    // getPatches //
    //------------//
    /**
     * Report the set of patches
     * @return the patches
     */
    public Set<GlyphSection> getPatches ()
    {
        return patches;
    }

    //----------//
    // getStick //
    //----------//
    /**
     * Report the underlying stick, or null if not found
     *
     * @return the underlying stick, null otherwise
     */
    public Stick getStick ()
    {
        return stick;
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
            Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1f);

            line = getLine();

            Point start = new Point(
                box.x,
                (int) Math.rint(line.yAt((double) box.x)));
            Point stop = new Point(
                box.x + box.width,
                (int) Math.rint(line.yAt((double) box.x + box.width + 1)));

            g.drawLine(start.x, start.y, stop.x, stop.y);
            ((Graphics2D) g).setStroke(oldStroke);
        }
    }

    //---------------//
    // renderContour //
    //---------------//
    /**
     * Render the contour box of the dash, using the current foreground color
     *
     * @param g the graphic context
     * @return true if something has been rendered
     */
    public boolean renderContour (Graphics g)
    {
        // Check the clipping
        if (box.intersects(g.getClipBounds())) {
            Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1f);
            g.drawRect(box.x, box.y, box.width, box.height);
            ((Graphics2D) g).setStroke(oldStroke);

            return true;
        } else {
            return false;
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
        sb.append(" stick#")
          .append(stick.getId());
        sb.append("}");

        return sb.toString();
    }
}
