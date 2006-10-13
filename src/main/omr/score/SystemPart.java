//----------------------------------------------------------------------------//
//                                                                            //
//                            S y s t e m P a r t                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import java.util.List;

/**
 * Class <code>SystemPart</code> handles the various parts found in one system,
 * since the layout of parts may bary from system to system
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SystemPart
{
    //~ Instance fields --------------------------------------------------------

    /** Staves that belong to this system */
    private final List<Staff> staves;

    //~ Constructors -----------------------------------------------------------

    /** Creates a new instance of SystemPart */
    public SystemPart (List<Staff> staves)
    {
        this.staves = staves;
    }

    //~ Methods ----------------------------------------------------------------

    public List<Staff> getStaves ()
    {
        return staves;
    }
}
