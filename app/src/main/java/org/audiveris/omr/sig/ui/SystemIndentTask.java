//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S y s t e m I n d e n t T a s k                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.sheet.SystemInfo;

/**
 * Class <code>SystemIndentTask</code> toggles the indentation flag on a system,
 * which controls whether this system starts a new movement.
 *
 * @author Hervé Bitteur
 */
public class SystemIndentTask
        extends UITask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The system whose indentation is toggled. */
    private final SystemInfo system;

    /** The indentation value before the toggle. */
    private final boolean previousIndented;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>SystemIndentTask</code> object.
     *
     * @param system the system to toggle
     */
    public SystemIndentTask (SystemInfo system)
    {
        super(system.getSig(), "toggle-indent");

        this.system = system;
        this.previousIndented = system.isIndented();
    }

    //~ Methods ------------------------------------------------------------------------------------

    public SystemInfo getSystem ()
    {
        return system;
    }

    @Override
    public void performDo ()
    {
        system.setIndented(!previousIndented);
        sheet.getSystemManager().rebuildPages();
    }

    @Override
    public void performUndo ()
    {
        system.setIndented(previousIndented);
        sheet.getSystemManager().rebuildPages();
    }
}
