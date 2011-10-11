//----------------------------------------------------------------------------//
//                                                                            //
//                              S t e p p i n g                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.Score;
import omr.score.entity.Page;
import omr.score.ui.ScoreActions;

import omr.script.StepTask;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.SheetsController;
import static omr.step.Steps.*;

import omr.util.OmrExecutors;
import omr.util.TreeNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

/**
 * Class {@code Stepping} handles the scheduling of step(s) on a score or a
 * sheet, with notification to the user interface when running in interactive
 * mode.
 *
 * @author Hervé Bitteur
 */
public class Stepping
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Stepping.class);

    //--------------------------------------------------------------------------

    /** Related progress monitor when used in interactive mode */
    private static volatile StepMonitor monitor;

    //~ Methods ----------------------------------------------------------------

    //------------------------//
    // getLatestMandatoryStep //
    //------------------------//
    /**
     * Report the latest mandatory step done so far with the related sheet
     * @return the latest mandatory step done, or null
     */
    public static Step getLatestMandatoryStep (Sheet sheet)
    {
        Step latest = null;

        for (Step step : Steps.values()) {
            if (step.isMandatory() && step.isDone(sheet)) {
                latest = step;
            } else {
                break;
            }
        }

        return latest;
    }

    //---------------//
    // getLatestStep //
    //---------------//
    /**
     * Report the latest step done so far with the related sheet
     * @return the latest step done, or null
     */
    public static Step getLatestStep (Sheet sheet)
    {
        for (ListIterator<Step> it = Steps.values()
                                          .listIterator(Steps.values().size());
             it.hasPrevious();) {
            Step step = it.previous();

            if (step.isDone(sheet)) {
                return step;
            }
        }

        return null;
    }

    //---------------//
    // createMonitor //
    //---------------//
    /**
     * Allows to couple the steps with a UI.
     * @return the monitor to deal with steps
     */
    public static StepMonitor createMonitor ()
    {
        return monitor = new StepMonitor();
    }

    //----------------//
    // doOneSheetStep //
    //----------------//
    /**
     * At sheet level, do just one specified step, synchronously, with display
     * of related UI and recording of the step into the script
     *
     * @param step the step to perform
     * @param sheet the sheet to be processed
     * @throws StepException
     */
    public static void doOneSheetStep (final Step             step,
                                       Sheet                  sheet,
                                       Collection<SystemInfo> systems)
        throws StepException
    {
        long startTime = System.currentTimeMillis();

        if (logger.isFineEnabled()) {
            logger.fine(sheet.getLogPrefix() + step + " starting");
        }

        // Standard processing on an existing sheet
        step.doStep(systems, sheet);

        // Update user interface if any
        notifyFinalStep(sheet, step);

        final long stopTime = System.currentTimeMillis();
        final long duration = stopTime - startTime;

        if (logger.isFineEnabled()) {
            logger.fine(
                sheet.getLogPrefix() + step + " completed in " + duration +
                " ms");
        }

        // Record this in sheet->score bench
        sheet.getBench()
             .recordStep(step, duration);
    }

    //-----------------//
    // notifyFinalStep //
    //-----------------//
    /**
     * Notify the UI part that we have reached the provided step in the
     * provided sheet
     * @param sheet the sheet concerned
     * @param step the step just done
     */
    public static void notifyFinalStep (final Sheet sheet,
                                        final Step  step)
    {
        if (monitor != null) {
            SwingUtilities.invokeLater(
                new Runnable() {
                        public void run ()
                        {
                            // Update sheet view for this step
                            step.displayUI(sheet);
                            sheet.getAssembly()
                                 .selectViewTab(step);

                            // Call attention to this sheet (only if displayed), 
                            // so that score-dependent actions can get enabled.
                            SheetsController ctrl = SheetsController.getInstance();
                            Sheet            currentSheet = ctrl.getSelectedSheet();

                            if (currentSheet == sheet) {
                                ctrl.callAboutSheet(sheet);
                            }
                        }
                    });
        }
    }

    //----------------//
    // notifyProgress //
    //----------------//
    /**
     * When running interactively, move slightly the progress bar animation
     */
    public static void notifyProgress ()
    {
        if (monitor != null) {
            monitor.animate();
        }
    }

    //--------------//
    // processScore //
    //--------------//
    /**
     * At score level, perform the desired steps (as well as all needed
     * intermediate steps).
     * <p>This method is used from the CLI, from a script or from the Step menu
     * (via the StepTask), and from the drag&drop handler.
     * @param desiredSteps the desired steps
     * @param score the processed score (and its sheets)
     */
    public static void processScore (Set<Step> desiredSteps,
                                     Score     score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("processScore " + desiredSteps + " on " + score);
        }

        // Sanity checks
        if (score == null) {
            throw new IllegalArgumentException("Score is null");
        }

        if (desiredSteps.isEmpty()) {
            return;
        }

        // Determine the precise ordered collection of steps to perform
        SortedSet<Step> orderedSteps = new TreeSet<Step>(comparator);
        orderedSteps.addAll(desiredSteps);

        try {
            // Determine starting step and stopping step
            Step start = null;
            Step stop = null;

            if (score.getPages()
                     .isEmpty()) {
                // Create score pages if not yet done
                score.createPages();
                start = first;
                stop = orderedSteps.last();
            } else {
                // Use a score sheet to retrieve the latest mandatory step
                Sheet sheet = score.getFirstPage()
                                   .getSheet();
                Step  latest = getLatestMandatoryStep(sheet);
                Step  firstDesired = orderedSteps.first();
                start = (latest == null) ? first
                        : ((latest == firstDesired) ? firstDesired : next(
                    latest));
                stop = (Steps.compare(latest, orderedSteps.last()) >= 0)
                       ? latest : orderedSteps.last();
            }

            // Add all intermediate mandatory steps
            for (Step step : range(start, stop)) {
                if (step.isMandatory()) {
                    orderedSteps.add(step);
                }
            }

            // Remove the LOAD step (unless it is explicitly desired)
            // LOAD step may appear only in reprocessSheet()
            Step loadStep = Steps.valueOf(Steps.LOAD);

            if (!desiredSteps.contains(loadStep)) {
                orderedSteps.remove(loadStep);
            }

            // Schedule the steps on each sheet
            ///logger.info("orderedSteps: " + orderedSteps);
            scheduleScoreStepSet(orderedSteps, score);

            // Record the step tasks to script
            for (Step step : desiredSteps) {
                score.getScript()
                     .addTask(new StepTask(step));
            }
        } catch (ProcessingCancellationException pce) {
            throw pce;
        } catch (Exception ex) {
            logger.warning("Error in performing " + orderedSteps, ex);
        }
    }

    //----------------//
    // reprocessSheet //
    //----------------//
    /**
     * For just a given sheet, update the steps already done, starting from the
     * provided step.
     * This method will try to minimize the systems to rebuild in each step, by
     * processing only the provided "impacted" systems.
     *
     * @param step the step to restart from
     * @param impactedSystems the ordered set of systems to rebuild, or null
     * if all systems must be rebuilt
     * @param imposed flag to indicate that update is imposed
     */
    public static void reprocessSheet (Step                   step,
                                       Sheet                  sheet,
                                       Collection<SystemInfo> impactedSystems,
                                       boolean                imposed)
    {
        if (logger.isFineEnabled()) {
            logger.fine("reprocessSheet " + step + " on " + sheet);
        }

        // Sanity checks
        if (SwingUtilities.isEventDispatchThread()) {
            logger.severe("Method reprocessSheet should not run on EDT!");
        }

        if (step == null) {
            return;
        }

        // Check whether the update must really be done
        if (!imposed && !ScoreActions.getInstance()
                                     .isRebuildAllowed()) {
            return;
        }

        // A null set of systems means all of them
        if (impactedSystems == null) {
            impactedSystems = sheet.getSystems();
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                sheet.getLogPrefix() + "Rebuild launched from " + step + " on" +
                SystemInfo.toString(impactedSystems));
        }

        // Rebuild from specified step, if needed
        if (shouldReprocessSheet(step, sheet)) {
            Step            latest = getLatestMandatoryStep(sheet);

            // The range of steps to re-perform
            SortedSet<Step> stepRange = range(step, latest);

            notifyStart();

            try {
                doSheetStepSet(stepRange, sheet, impactedSystems);
            } catch (ProcessingCancellationException pce) {
                throw pce;
            } catch (Exception ex) {
                logger.warning("Error in re-processing from " + step, ex);
            } finally {
                notifyStop();
            }
        }
    }

    //----------------------//
    // shouldReprocessSheet //
    //----------------------//
    /**
     * Check whether some steps need to be reperformed, starting from step 'from'
     * @param from the step to rebuild from
     * @return true if some reprocessing must take place
     */
    public static boolean shouldReprocessSheet (Step  from,
                                                Sheet sheet)
    {
        Step latest = getLatestMandatoryStep(sheet);

        return (latest == null) || (compare(latest, from) >= 0);
    }

    //----------------//
    // doOneScoreStep //
    //----------------//
    /**
     * At score level, do just one specified step, synchronously, with display
     * of related UI and recording of the step into the script
     *
     * @param step the step to perform
     * @param score the score to be processed
     * @throws StepException
     */
    private static void doOneScoreStep (final Step step,
                                        Score      score)
        throws StepException
    {
        long startTime = System.currentTimeMillis();

        if (logger.isFineEnabled()) {
            logger.fine(step + " Starting");
        }

        // Standard processing (using first sheet)
        Sheet sheet = score.getFirstPage()
                           .getSheet();
        step.doStep(null, sheet);

        // Update user interface if any
        notifyFinalStep(sheet, step);

        final long stopTime = System.currentTimeMillis();
        final long duration = stopTime - startTime;

        if (logger.isFineEnabled()) {
            logger.fine(step + " completed in " + duration + " ms");
        }

        // Record this in score bench
        score.getBench()
             .recordStep(step, duration);
    }

    //----------------//
    // doScoreStepSet //
    //----------------//
    /**
     * At score level, perform a set of steps, with online display of a
     * progress monitor.
     *
     * <p>We can perform all the pages in parallel or in sequence, depending on
     * the value of constant 'pagesInParallel'.</p>
     *
     * @param stepSet the set of steps
     * @param score the score to be processed
     */
    private static void doScoreStepSet (final SortedSet<Step> stepSet,
                                        final Score           score)
    {
        if (score.isMultiPage()) {
            if (constants.pagesInParallel.getValue()) {
                // Process all sheets in parallel
                List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

                for (TreeNode pn : new ArrayList<TreeNode>(score.getPages())) {
                    final Page page = (Page) pn;

                    tasks.add(
                        new Callable<Void>() {
                                public Void call ()
                                    throws Exception
                                {
                                    doSheetStepSet(
                                        stepSet,
                                        page.getSheet(),
                                        null);

                                    return null;
                                }
                            });
                }

                try {
                    List<Future<Void>> futures = OmrExecutors.getCachedLowExecutor()
                                                             .invokeAll(tasks);
                } catch (InterruptedException ex) {
                    logger.warning("Error in parallel doScoreStepSet", ex);
                }
            } else {
                // Process one sheet after the other
                for (TreeNode pn : new ArrayList<TreeNode>(score.getPages())) {
                    Page page = (Page) pn;
                    doSheetStepSet(stepSet, page.getSheet(), null);
                }
            }
        } else {
            // Process the single sheet
            doSheetStepSet(stepSet, score.getFirstPage().getSheet(), null);
        }
    }

    //----------------//
    // doSheetStepSet //
    //----------------//
    /**
     * At sheet level, perform a set of steps, with online progress monitor
     * @param stepSet the set of steps
     * @param sheet the sheet to be processed
     * @params systems the impacted systems (null for all of them)
     */
    private static void doSheetStepSet (SortedSet<Step>        stepSet,
                                        Sheet                  sheet,
                                        Collection<SystemInfo> systems)
    {
        try {
            for (Step step : stepSet) {
                notifyMsg(sheet.getLogPrefix() + step);
                doOneSheetStep(step, sheet, systems);
            }
        } catch (StepException ex) {
            logger.info(sheet.getLogPrefix() + "Processing stopped");
        }
    }

    //-----------//
    // notifyMsg //
    //-----------//
    /**
     * Notify a simple message, which may be not related to any step.
     *
     * @param msg the message to display on the UI window, or to write in the
     *            log if there is no UI.
     */
    private static void notifyMsg (String msg)
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
     * When running interactively, start the progress bar animation
     */
    private static void notifyStart ()
    {
        // "Activate" the progress bar
        if (monitor != null) {
            monitor.displayAnimation(true);
        }
    }

    //------------//
    // notifyStop //
    //------------//
    /**
     * When running interactively, stop the progress bar animation
     */
    private static void notifyStop ()
    {
        // Reset the progress bar?
        if (monitor != null) {
            notifyMsg("");
            monitor.displayAnimation(false);
        }
    }

    //----------------------//
    // scheduleScoreStepSet //
    //----------------------//
    /**
     * Organize the scheduling of steps at score level among the sheets, since
     * some steps have certain requirements
     *
     * @param orderedSet the sequence of steps
     * @param score the score to be processed
     */
    private static void scheduleScoreStepSet (SortedSet<Step> orderedSet,
                                              Score           score)
    {
        // Make a copy, so that we can modify the step set locally
        SortedSet<Step> stepSet = new TreeSet<Step>(orderedSet);

        if (stepSet.isEmpty()) {
            return;
        }

        logger.info(score.getRadix() + " Scheduling " + stepSet);

        long startTime = System.currentTimeMillis();
        notifyStart();

        try {
            // SCALE step, if present, is always the first step
            // We performed this step on all sheets, to allow early filtering
            Step scaleStep = Steps.valueOf(Steps.SCALE);

            if (stepSet.contains(scaleStep)) {
                SortedSet<Step> single = new TreeSet<Step>(comparator);
                single.add(scaleStep);
                stepSet.remove(scaleStep);
                doScoreStepSet(single, score);
            }

            // Perform the remaining steps at sheet level
            SortedSet<Step> sheetSet = new TreeSet<Step>(comparator);

            for (Step step : stepSet) {
                if (!step.isScoreLevel()) {
                    sheetSet.add(step);
                }
            }

            stepSet.removeAll(sheetSet);
            doScoreStepSet(sheetSet, score);

            // Finally, perform steps at score level if any
            for (Step step : stepSet) {
                try {
                    doOneScoreStep(step, score);
                } catch (Exception ex) {
                    logger.warning("Error on step " + step, ex);
                }
            }
        } finally {
            notifyStop();
        }

        if (logger.isFineEnabled()) {
            long stopTime = System.currentTimeMillis();
            logger.fine(
                "End of step set in " + (stopTime - startTime) + " ms.");
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Should we apply steps in parallel (vs in sequence) on score pages */
        Constant.Boolean pagesInParallel = new Constant.Boolean(
            false,
            "Should we process score pages in parallel?");
    }
}
