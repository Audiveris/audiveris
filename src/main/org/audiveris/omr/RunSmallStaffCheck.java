//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               R u n S m a l l S t a f f C h e c k                              //
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
package org.audiveris.omr;

import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.step.RunClass;
import org.audiveris.omr.step.Step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;

/**
 * Class {@code RunSmallStaffCheck}
 *
 * @author Hervé Bitteur
 */
public class RunSmallStaffCheck
        extends RunClass
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RunSmallStaffCheck.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RunSmallStaffCheck} object.
     *
     * @param book     book to process
     * @param sheetIds sheet IDS if any
     */
    public RunSmallStaffCheck (Book book,
                               SortedSet<Integer> sheetIds)
    {
        super(book, sheetIds);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void process ()
    {
        for (SheetStub stub : book.getValidStubs()) {
            if ((sheetIds == null) || sheetIds.contains(stub.getNumber())) {
                if (stub.isDone(Step.GRID)) {
                    Sheet sheet = stub.getSheet();

                    for (Staff staff : sheet.getStaffManager().getStaves()) {
                        if (staff.isSmall()) {
                            logger.info("{} small staff#{}", sheet.getId(), staff.getId());
                        }
                    }
                }
            }
        }
    }
}
