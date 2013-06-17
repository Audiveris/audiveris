//----------------------------------------------------------------------------//
//                                                                            //
//                           T a r g e t S t a f f                            //
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
 * Class {@code TargetStaff} is an immutable perfect destination
 * object for a staff.
 *
 * @author Hervé Bitteur
 */
public class TargetStaff
{
    //~ Instance fields --------------------------------------------------------

    /** Initial raw information */
    public final StaffInfo info;

    /** Id for debug */
    public final int id;

    /** Ordinate of top in containing page */
    public final double top;

    /** Sequence of staff lines */
    public final List<TargetLine> lines = new ArrayList<>();

    /** Containing system */
    public final TargetSystem system;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // TargetStaff //
    //-------------//
    /**
     * Creates a new TargetStaff object.
     *
     * @param info initial raw information
     * @param top  Ordinate of top in containing page
     */
    public TargetStaff (StaffInfo info,
                        double top,
                        TargetSystem system)
    {
        this.info = info;
        this.top = top;
        this.system = system;

        id = info.getId();
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Staff");
        sb.append("#")
                .append(id);
        sb.append(" top:")
                .append(top);
        sb.append("}");

        return sb.toString();
    }
}
