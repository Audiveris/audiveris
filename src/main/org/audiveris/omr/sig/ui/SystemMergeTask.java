//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S y s t e m M e r g e T a s k                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.sheet.SystemInfo;

import java.util.List;

/**
 * Class <code>SystemMergeTask</code> implements the merge of two systems.
 *
 * @author Hervé Bitteur
 */
public class SystemMergeTask
        extends UITask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Top system. */
    private final SystemInfo system;

    /** Bottom system. */
    private final SystemInfo systemBelow;

    /** PageRef removed, if any. */
    private PageRef pageRef;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>SystemMergeTask</code> object.
     *
     * @param system DOCUMENT ME!
     */
    public SystemMergeTask (SystemInfo system)
    {
        super(system.getSig(), "merge-system");

        this.system = system;

        List<SystemInfo> systems = sheet.getSystems();
        systemBelow = systems.get(1 + systems.indexOf(system));
    }

    //~ Methods ------------------------------------------------------------------------------------
    public SystemInfo getSystem ()
    {
        return system;
    }

    @Override
    public void performDo ()
    {
        pageRef = system.mergeWithBelow();
    }

    @Override
    public void performUndo ()
    {
        system.unmergeWith(systemBelow, pageRef);
    }
}
