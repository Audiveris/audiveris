//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t e p M o n i t o r i n g                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step.ui;

import omr.sheet.ui.StubsController;

import omr.step.Step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import omr.sheet.SheetStub;

/**
 * Class {@code StepMonitoring} handles the step progress notification to user, when
 * running in interactive mode.
 *
 * @author Hervé Bitteur
 */
public abstract class StepMonitoring
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StepMonitoring.class);

    /** Related progress monitor when used in interactive mode. */
    private static volatile StepMonitor monitor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Not meant to be instantiated.
     */
    private StepMonitoring ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // animate //
    //---------//
    /**
     * When running interactively, animate the progress bar.
     */
    public static void animate ()
    {
        if (monitor != null) {
            monitor.animate();
        }
    }

    //---------------//
    // createMonitor //
    //---------------//
    /**
     * Allows to couple the steps with a UI.
     *
     * @return the monitor to deal with steps
     */
    public static StepMonitor createMonitor ()
    {
        return monitor = new StepMonitor();
    }

    //-----------//
    // notifyMsg //
    //-----------//
    /**
     * Notify a simple message, which may be not related to any step.
     *
     * @param msg the message to display on the UI window, or to write in the log if there is no UI.
     */
    public static void notifyMsg (String msg)
    {
        if (monitor != null) {
            monitor.notifyMsg(msg);
        } else {
            logger.info(msg);
        }
    }

    //-------------//
    // notifyStart //
    //-------------//
    /**
     * When running interactively, start the progress bar animation.
     */
    public static void notifyStart ()
    {
        // "Activate" the progress bar
        if (monitor != null) {
            monitor.displayAnimation(true);
        }
    }

    //------------//
    // notifyStep //
    //------------//
    /**
     * Notify the UI part that the provided step has started or stopped in the provided
     * sheet.
     *
     * @param stub the sheet stub concerned
     * @param step the step notified
     */
    public static void notifyStep (final SheetStub stub,
                                   final Step step)
    {
        if (monitor != null) {
            final boolean finished = stub.getCurrentStep() == null;
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run ()
                {
                    // Update sheet view for this step?
                    if (finished) {
                        step.displayUI(stub.getSheet());
                        stub.getAssembly().selectViewTab(step.getSheetTab());
                    }

                            // Call attention to this sheet (only if displayed),
                    // so that score-dependent actions can get enabled.
                    StubsController ctrl = StubsController.getInstance();
                    SheetStub currentStub = ctrl.getSelectedStub();

                    if (currentStub == stub) {
                        ctrl.callAboutStub(currentStub);
                    }
                }
            });
        }
    }

    //------------//
    // notifyStop //
    //------------//
    /**
     * When running interactively, stop the progress bar animation.
     */
    public static void notifyStop ()
    {
        // Reset the progress bar?
        if (monitor != null) {
            notifyMsg("");
            monitor.displayAnimation(false);
        }
    }
//
//    //----------------//
//    // reprocessSheet //
//    //----------------//
//    /**
//     * For just a given sheet, update the steps already done, starting from the provided
//     * step.
//     * This method will try to minimize the systems to rebuild in each step, by processing only the
//     * provided "impacted" systems.
//     *
//     * @param step            the step to restart from
//     * @param sheet           the sheet to process
//     * @param impactedSystems the ordered set of systems to rebuild, or null for all systems
//     * @param imposed         flag to indicate that update is imposed
//     */
//    @Deprecated
//    public static void reprocessSheet (Step step,
//                                       Sheet sheet,
//                                       Collection<SystemInfo> impactedSystems,
//                                       boolean imposed)
//    {
//        reprocessSheet(step, sheet, impactedSystems, imposed, true);
//    }
//
//    //----------------//
//    // reprocessSheet //
//    //----------------//
//    /**
//     * For just a given sheet, update the steps already done, starting from the provided
//     * step.
//     * This method will try to minimize the systems to rebuild in each step, by processing only the
//     * provided "impacted" systems.
//     *
//     * @param step            the step to restart from
//     * @param sheet           the sheet to process
//     * @param impactedSystems the ordered set of systems to rebuild, or null for all systems
//     * @param imposed         flag to indicate that update is imposed
//     * @param merge           true if step SCORE (merge of pages) is allowed
//     */
//    @Deprecated
//    public static void reprocessSheet (Step step,
//                                       Sheet sheet,
//                                       Collection<SystemInfo> impactedSystems,
//                                       boolean imposed,
//                                       boolean merge)
//    {
//        logger.debug("reprocessSheet {} on {}", step, sheet);
//
//        // Sanity checks
//        if (SwingUtilities.isEventDispatchThread()) {
//            logger.error("Method reprocessSheet should not run on EDT!");
//        }
//
//        if (step == null) {
//            return;
//        }
//
//        // Check whether the update must really be done
//        if (!imposed && !ScoreActions.getInstance().isRebuildAllowed()) {
//            return;
//        }
//
//        // A null set of systems means all of them
//        if (impactedSystems == null) {
//            impactedSystems = sheet.getSystems();
//        }
//
//        logger.debug(
//                "{}Rebuild launched from {} on {}",
//                sheet.getLogPrefix(),
//                step,
//                SystemInfo.toString(impactedSystems));
//
//        // Rebuild from specified step, if needed
//        Step latest = sheet.getLatestStep();
//
//        if ((latest == null) || (latest.compareTo(step) >= 0)) {
//            // The range of steps to re-perform
//            EnumSet<Step> stepRange = EnumSet.range(step, latest);
//
//            notifyStart();
//
//            try {
//                sheet.doStep(stepRange, impactedSystems);
//            } catch (ProcessingCancellationException pce) {
//                throw pce;
//            } catch (Exception ex) {
//                logger.warn("Error in re-processing from " + step, ex);
//            } finally {
//                notifyStop();
//            }
//        }
//    }
}
