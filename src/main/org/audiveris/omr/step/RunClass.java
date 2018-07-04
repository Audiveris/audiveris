//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         R u n C l a s s                                        //
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
package org.audiveris.omr.step;

import org.audiveris.omr.sheet.Book;

import java.util.SortedSet;

/**
 * Class {@code RunClass} is the abstract basis for specific processing.
 *
 * @author Hervé Bitteur
 */
public abstract class RunClass
{
    //~ Instance fields ----------------------------------------------------------------------------

    protected Book book;

    protected SortedSet<Integer> sheetIds;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RunClass} object.
     *
     * @param book     the book to process
     * @param sheetIds specific sheet IDs if any
     */
    public RunClass (Book book,
                     SortedSet<Integer> sheetIds)
    {
        this.book = book;
        this.sheetIds = sheetIds;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * The processing to be done.
     */
    public abstract void process ();
}
