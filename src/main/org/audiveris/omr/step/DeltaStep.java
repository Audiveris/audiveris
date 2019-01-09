//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        D e l t a S t e p                                       //
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
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetDiff;
import org.audiveris.omr.sheet.SheetStub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code DeltaStep} computes the delta value as a kind of recognition level on a
 * whole sheet.
 *
 * @author Hervé Bitteur
 */
@Deprecated
public class DeltaStep
        extends AbstractStep
{

    private static final Logger logger = LoggerFactory.getLogger(DeltaStep.class);

    /**
     * Creates a new DeltaStep object.
     */
    public DeltaStep ()
    {
        ///super(Steps.DELTA, DATA_TAB, "Compute page delta");
    }

    //------//
    // doit //
    //------//
    @Override
    public void doit (Sheet sheet)
            throws StepException
    {
        // Run it for ALL sheets of the book and compute a mean ratio other all sheets
        final Book book = sheet.getStub().getBook();
        int count = 0;
        double globalRatio = 0;

        for (SheetStub stub : book.getStubs()) {
            SheetDiff sheetDelta = new SheetDiff(stub.getSheet());
            double ratio = sheetDelta.computeDiff();
            globalRatio += ratio;
            count++;
        }

        if (count > 0) {
            globalRatio /= count;
            logger.info("Global score delta: {}%", String.format("%4.1f", 100 * globalRatio));
        }
    }
}
