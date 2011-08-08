//----------------------------------------------------------------------------//
//                                                                            //
//                               B a r I n f o                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.glyph.facets.Stick;

import omr.util.HorizontalSide;

import java.util.*;

/**
 * Class {@code BarInfo} records the physical information about a bar line,
 * used especially as a vertical limit for a staff or system
 *
 * @author Hervé Bitteur
 */
public class BarInfo
{
    //~ Instance fields --------------------------------------------------------

    /** Composing sticks, ordered by their relative position (abscissa) */
    private List<Stick> sticks;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // BarInfo //
    //---------//
    /**
     * Creates a new BarInfo object.
     *
     * @param sticks one or several bars, from left to right
     */
    public BarInfo (Stick... sticks)
    {
        this(Arrays.asList(sticks));
    }

    //---------//
    // BarInfo //
    //---------//
    /**
     * Creates a new BarInfo object.
     *
     * @param sticks one or several bars, from left to right
     */
    public BarInfo (Collection<?extends Stick> sticks)
    {
        setSticks(sticks);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getStick //
    //----------//
    public Stick getStick (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return sticks.get(0);
        } else {
            return sticks.get(sticks.size() - 1);
        }
    }

    //-----------//
    // setSticks //
    //-----------//
    public final void setSticks (Collection<?extends Stick> sticks)
    {
        this.sticks = new ArrayList(sticks); // Copy
    }

    //--------------------//
    // getSticksAncestors //
    //--------------------//
    public List<Stick> getSticksAncestors ()
    {
        List<Stick> list = new ArrayList<Stick>(sticks.size());

        for (Stick stick : sticks) {
            list.add((Stick) stick.getAncestor());
        }

        return list;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{BarInfo");

        for (Stick stick : sticks) {
            sb.append(" #")
              .append(stick.getAncestor().getId());
        }

        sb.append("}");

        return sb.toString();
    }
}
