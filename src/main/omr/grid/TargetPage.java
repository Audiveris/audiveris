//----------------------------------------------------------------------------//
//                                                                            //
//                            T a r g e t P a g e                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code TargetPage} is an immutable perfect destination object
 * for a page.
 *
 * @author Hervé Bitteur
 */
public class TargetPage
{
    //~ Instance fields --------------------------------------------------------

    /** Page width */
    public final double width;

    /** Page height */
    public final double height;

    /** Sequence of systems */
    public final List<TargetSystem> systems = new ArrayList<>();

    //~ Constructors -----------------------------------------------------------
    //------------//
    // TargetPage //
    //------------//
    /**
     * Creates a new TargetPage object.
     *
     * @param width  page width
     * @param height page height
     */
    public TargetPage (double width,
                       double height)
    {
        this.width = width;
        this.height = height;
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Page");

        sb.append(" width:")
                .append(width);
        sb.append(" height:")
                .append(height);

        sb.append("}");

        return sb.toString();
    }
}
