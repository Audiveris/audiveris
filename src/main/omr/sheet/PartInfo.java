//----------------------------------------------------------------------------//
//                                                                            //
//                              P a r t I n f o                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>PartInfo</code> is the physical gathering of StaffInfo instances
 * in a part
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class PartInfo
{
    //~ Instance fields --------------------------------------------------------

    /** Staves in this part */
    private List<StaffInfo> staves = new ArrayList<StaffInfo>();

    //~ Constructors -----------------------------------------------------------

    //----------//
    // PartInfo //
    //----------//
    /** Creates a new instance of PartInfo */
    public PartInfo ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getStaves //
    //-----------//
    public List<StaffInfo> getStaves ()
    {
        return staves;
    }

    //----------//
    // addStaff //
    //----------//
    public void addStaff (StaffInfo staffInfo)
    {
        staves.add(staffInfo);
    }
}
