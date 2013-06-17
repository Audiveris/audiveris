//----------------------------------------------------------------------------//
//                                                                            //
//                               B a r I n f o                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.glyph.facets.Glyph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code BarInfo} records the physical information about a bar
 * line, used especially as a vertical limit for a staff or system.
 *
 * @author Hervé Bitteur
 */
public class BarInfo
{
    //~ Instance fields --------------------------------------------------------

    /** Composing sticks, ordered by their relative abscissa. */
    private List<Glyph> sticks;

    //~ Constructors -----------------------------------------------------------
    //---------//
    // BarInfo //
    //---------//
    /**
     * Creates a new BarInfo object.
     *
     * @param sticks one or several physical bars, from left to right
     */
    public BarInfo (Glyph... sticks)
    {
        this(Arrays.asList(sticks));
    }

    //---------//
    // BarInfo //
    //---------//
    /**
     * Creates a new BarInfo object.
     *
     * @param sticks one or several physical bars, from left to right
     */
    public BarInfo (Collection<? extends Glyph> sticks)
    {
        setSticks(sticks);
    }

    //~ Methods ----------------------------------------------------------------
    //--------------------//
    // getSticksAncestors //
    //--------------------//
    public List<Glyph> getSticksAncestors ()
    {
        List<Glyph> list = new ArrayList<>(sticks.size());

        for (Glyph stick : sticks) {
            list.add(stick.getAncestor());
        }

        return list;
    }

    //-----------//
    // setSticks //
    //-----------//
    public final void setSticks (Collection<? extends Glyph> sticks)
    {
        this.sticks = new ArrayList<>(sticks); // Copy
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{BarInfo");

        for (Glyph stick : sticks) {
            sb.append(" #")
                    .append(stick.getAncestor().getId());
        }

        sb.append("}");

        return sb.toString();
    }
}
