//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R h y t h m s S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.score.Page;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.step.AbstractStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code RhythmsStep} is a comprehensive step that handles the timing of every
 * relevant item within a page.
 *
 * @author Hervé Bitteur
 */
public class RhythmsStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RhythmsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TimingStep} object.
     */
    public RhythmsStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void doit (Sheet sheet)
            throws StepException
    {
        // Detect cross-system exclusions
        ///new CrossDetector(sheet).process();

        // Process each page of the sheet
        for (Page page : sheet.getPages()) {
            new PageRhythm(page).process();

            // Complete each measure with its needed data
            for (SystemInfo system : page.getSystems()) {
                new MeasureFiller(system).process();
            }
        }
    }
}
//
//        for (Page page : sheet.getPages()) {
//            // Organize chords into time slots & voices
//            for (SystemInfo system : page.getSystems()) {
//                new SlotsBuilder(system).buildSlots();
//            }
//
//            //            //            // 1/ Look carefully for time signatures
//            //            //            page.accept(new TimeSignatureRetriever());
//            //            //
//            //            //            // 2/ Adapt time sigs to intrinsic measure & chord durations
//            //            //            page.accept(new TimeSignatureFixer());
//            //            //
//            /**
//             * Strategy.
//             * The purpose is to perform as much processing at page level, rather than at score
//             * level (which needs to handle the data from the sequence of all score pages).
//             * <p>
//             * We are optimistic, and try to infer time sig for the page at hand, even if the
//             * page begins with no time signature. If some adjustments are needed at score level,
//             * then relevant page data will be later updated accordingly.
//             */
//            // - Retrieve the actual duration of every measure
//            new DurationRetriever().process(page);
//
//            //            // - Check all voices timing, assign forward items if needed.
//            //            // - Detect special measures and assign proper measure ids
//            //            new MeasureFixer().process(page);
//        }
