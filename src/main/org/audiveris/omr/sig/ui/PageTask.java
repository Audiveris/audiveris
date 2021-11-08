//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P a g e T a s k                                        //
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

import org.audiveris.omr.score.Page;

/**
 * Class <code>PageTask</code> implements the on demand re-processing of a page.
 *
 * @author Hervé Bitteur
 */
public class PageTask
        extends UITask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Impacted page. */
    private final Page page;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>PageTask</code> object.
     *
     * @param page the impacted page
     */
    public PageTask (Page page)
    {
        super(page, "reprocess-page");
        this.page = page;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public Page getPage ()
    {
        return page;
    }

    @Override
    public void performDo ()
    {
        // Void
    }

    @Override
    public void performUndo ()
    {
        // Void
    }
}
