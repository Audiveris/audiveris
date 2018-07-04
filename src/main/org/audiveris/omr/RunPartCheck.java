//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     R u n P a r t C h e c k                                    //
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
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.step.RunClass;
import org.audiveris.omr.step.Step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * Class {@code RunPartCheck}
 *
 * @author Hervé Bitteur
 */
public class RunPartCheck
        extends RunClass
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RunPartCheck.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RunPartCheck} object.
     *
     * @param book     book to process
     * @param sheetIds sheet IDS if any
     */
    public RunPartCheck (Book book,
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
                logger.info("RunPartCheck. process {}", stub);

                if (stub.isDone(Step.GRID)) {
                    Sheet sheet = stub.getSheet();

                    for (SystemInfo system : sheet.getSystems()) {
                        List<Staff> staves = new ArrayList<Staff>(system.getStaves());

                        for (Part part : system.getParts()) {
                            staves.removeAll(part.getStaves());
                        }

                        if (!staves.isEmpty()) {
                            logger.warn("Unassigned staves: {}", staves);
                        }
                    }
                }
            }
        }
    }
}
