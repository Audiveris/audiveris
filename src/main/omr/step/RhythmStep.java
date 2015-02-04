//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       R h y t h m S t e p                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.score.entity.Page;

import omr.sheet.MeasureStack;
import omr.sheet.Sheet;
import omr.sheet.StackBuilder;
import omr.sheet.SystemInfo;
import omr.sheet.SystemVoiceFixer;

import omr.sig.inter.Inter;

import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Class {@code RhythmStep} is a comprehensive step that handles the timing of every
 * relevant item within a page.
 * <ul>
 * <li>Key sig changes.</li>
 * <li>Tuplets for head & rest chords.</li>
 * <li>Augmentation dots.</li>
 * <li>Flags.</li>
 * <li>Chords organized by time slots & voices.</li>
 * <li>Inference and possible adjustment of time signatures.</li>
 * <li>Measures assignment & numbering.</li>
 * </ul>
 * To do so, processing is done system per system <b>sequentially</b> because of impact of potential
 * key-sig changes on the following systems. Consistently, within a system, processing is done
 * measure stack after measure stack.
 * <p>
 * TODO: Time sig can be inferred from stacks actual content, this is a chicken & egg problem.
 * Perhaps two passes will be needed?
 *
 * @author Hervé Bitteur
 */
public class RhythmStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RhythmStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TimingStep} object.
     */
    public RhythmStep ()
    {
        super(
                Steps.RHYTHM,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Handle rhythm within measures");
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    protected void doit (Collection<SystemInfo> systems,
                         Sheet sheet)
            throws StepException
    {
        // Populate each measure stack with inters relevant for rhythm handling
        // (chords, flags, tuplets, augDots)
        //
        // TODO: add time-sig instances, since they impact expected durations.
        // This should be handled at column-level in a measure stack, reuse TimeBuilder!
        for (Page page : sheet.getPages()) {
            for (SystemInfo system : page.getSystems()) {
                // Select relevant rhythm inters at system level
                List<Inter> systemInters = getSystemInters(system);

                // Process stack after stack
                for (MeasureStack stack : system.getMeasureStacks()) {
                    logger.info("\n--- Processing {} -------------------------------------", stack);
                    new StackBuilder(stack).process(systemInters);
                }

                // Refine voices ids (and thus colors) across all measures of the system
                new SystemVoiceFixer(system).refine();
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
    }

    //-----------------//
    // getSystemInters //
    //-----------------//
    private List<Inter> getSystemInters (SystemInfo system)
    {
        // Filter system inter's by relevant class
        return system.getSig().inters(
                new Predicate<Inter>()
                {
                    @Override
                    public boolean check (Inter inter)
                    {
                        for (Class classe : StackBuilder.timingClasses) {
                            if (classe.isInstance(inter)) {
                                return true;
                            }
                        }

                        return false;
                    }
                });
    }
}
