//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R h y t h m s S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.score.Page;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.AbstractStep;
import omr.step.StepException;

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
