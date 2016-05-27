//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         R u n C l a s s                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.Book;

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

    /**
     * The processing to be done.
     */
    public abstract void process ();
}
