//----------------------------------------------------------------------------//
//                                                                            //
//                              S t e p p i n g                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

/**
 * Class {@code Stepping} handles the scheduling of step(s) on a score
 * or a sheet, with notification to the user interface when running in
 * interactive mode.
 *
 * @author Hervé Bitteur
 */
public class Stepping
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Stepping.class);

    /** Related progress monitor when used in interactive mode. */
    private static volatile StepMonitor monitor;

    //~ Constructors -----------------------------------------------------------
    /**
     * Not meant to be instantiated.
     */
    private Stepping ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //
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

    //----------------//
    // doOneSheetStep //
    //----------------//
    /**
     * At sheet level, do just one specified step, synchronously, with
     * display of related UI and recording of the step into the script.
     *
     * @param step  the step to perform
     * @param sheet the sheet to be processed
     * @throws StepException
     */
    private static void doOneSheetStep (final Step step,
                                        Sheet sheet,
                                        Collection<SystemInfo> systems)
            throws StepException
    {
        long startTime = System.currentTimeMillis();
        logger.debug("{}{} starting", sheet.getLogPrefix(), step);

        // Standard processing on an existing sheet
        step.doStep(systems, sheet);

        final long stopTime = System.currentTimeMillis();
        final long duration = stopTime - startTime;
        logger.debug("{}{} completed in {} ms",
                sheet.getLogPrefix(), step, duration);

        // Record this in sheet->score bench
        sheet.getBench().recordStep(step, duration);
    }

    //------------------------//
    // getLatestMandatoryStep //
    //------------------------//
    /**
     * Report the latest mandatory step done so far with the related sheet.
     *
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
     * Report the latest step done so far with the related sheet.
     *
     * @return the latest step done, or null
     */
    public static Step getLatestStep (Sheet sheet)
    {
        for (ListIterator<Step> it = Steps.values().listIterator(Steps.values().
                size());
                it.hasPrevious();) {
            Step step = it.previous();

            if (step.isDone(sheet)) {
                return step;
            }
        }

        return null;
    }

    //------------//
    // notifyStep //
    //------------//
    /**
     * Notify the UI part that the provided step has started or stopped
     * in the provided sheet.
     *
     * @param sheet the sheet concerned
     * @param step  the step notified
     */
    static void notifyStep (final Sheet sheet,
                            final Step step)
    {
        if (monitor != null) {
            final boolean finished = sheet.getCurrentStep() == null;
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    // Update sheet view for this step?
                    if (finished) {
                        step.displayUI(sheet);
                        sheet.getAssembly().selectViewTab(step);
                    }

                    // Call attention to this sheet (only if displayed), 
                    // so that score-dependent actions can get enabled.
                    SheetsController ctrl = SheetsController.
                            getInstance();
                    Sheet currentSheet = ctrl.getSelectedSheet();

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
     * When running interactively, move slightly the progress bar
     * animation.
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
     *
     * @param desiredSteps the desired steps
     * @param pages        specific set of pages, if any
     * @param score        the processed score (and its sheets)
     */
    public static void processScore (Set<Step> desiredSteps,
                                     SortedSet<Integer> pages,
                                     Score score)
    {
        logger.debug("processScore {} on {}", desiredSteps, score);

        // Sanity checks
        if (score == null) {
            throw new IllegalArgumentException("Score is null");
        }

        // Determine the precise ordered collection of steps to perform
        SortedSet<Step> orderedSteps = new TreeSet<>(comparator);
        orderedSteps.addAll(desiredSteps);

        try {
            // Determine starting step and stopping step
            final Step loadStep = Steps.valueOf(Steps.LOAD);
            final Step start;
            final Step stop;

            if (score.getPages().isEmpty()) {
                // Create score pages if not yet done
                score.createPages(pages);
                start = first;
                stop = orderedSteps.isEmpty() ? first : orderedSteps.last();
            } else {
                // Use a score sheet to retrieve the latest mandatory step
                Sheet sheet = score.getFirstPage().getSheet();
                Step latest = getLatestMandatoryStep(sheet);
                Step firstDesired = orderedSteps.first();
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

            if (!desiredSteps.contains(loadStep)) {
                orderedSteps.remove(loadStep);
            }

            // Schedule the steps on each sheet
            scheduleScoreStepSet(orderedSteps, score);

            // Record the step tasks to script
            for (Step step : desiredSteps) {
                score.getScript().addTask(new StepTask(step));
            }
        } catch (ProcessingCancellationException pce) {
            throw pce;
        } catch (Exception ex) {
            logger.warn("Error in performing " + orderedSteps, ex);
        }
    }

    //-----------------//
    // ensureScoreStep //
    //-----------------//
    /**
     * Make sure the provided step has been reached on the score at hand
     *
     * @param step  the step to check
     * @param score the score to process, if so needed
     */
    public static void ensureScoreStep (Step step,
                                        Score score)
    {
        if (!score.getFirstPage().getSheet().isDone(step)) {
            processScore(Collections.singleton(step), null, score);
        }
    }

    //----------------//
    // reprocessSheet //
    //----------------//
    /**
     * For just a given sheet, update the steps already done, starting
     * from the provided step.
     * This method will try to minimize the systems to rebuild in each step, by
     * processing only the provided "impacted" systems.
     *
     * @param step            the step to restart from
     * @param impactedSystems the ordered set of systems to rebuild, or null
     *                        if all systems must be rebuilt
     * @param imposed         flag to indicate that update is imposed
     */
    public static void reprocessSheet (Step step,
                                       Sheet sheet,
                                       Collection<SystemInfo> impactedSystems,
                                       boolean imposed)
    {
        reprocessSheet(step, sheet, impactedSystems, imposed, true);
    }

    //----------------//
    // reprocessSheet //
    //----------------//
    /**
     * For just a given sheet, update the steps already done, starting
     * from the provided step.
     * This method will try to minimize the systems to rebuild in each step, by
     * processing only the provided "impacted" systems.
     *
     * @param step            the step to restart from
     * @param impactedSystems the ordered set of systems to rebuild, or null
     *                        if all systems must be rebuilt
     * @param imposed         flag to indicate that update is imposed
     * @param merge           true if step SCORE (merge of pages) is allowed
     */
    public static void reprocessSheet (Step step,
                                       Sheet sheet,
                                       Collection<SystemInfo> impactedSystems,
                                       boolean imposed,
                                       boolean merge)
    {
        logger.debug("reprocessSheet {} on {}", step, sheet);

        // Sanity checks
        if (SwingUtilities.isEventDispatchThread()) {
            logger.error("Method reprocessSheet should not run on EDT!");
        }

        if (step == null) {
            return;
        }

        // Check whether the update must really be done
        if (!imposed && !ScoreActions.getInstance().isRebuildAllowed()) {
            return;
        }

        // A null set of systems means all of them
        if (impactedSystems == null) {
            impactedSystems = sheet.getSystems();
        }

        logger.debug("{}Rebuild launched from {} on {}",
                sheet.getLogPrefix(),
                step,
                SystemInfo.toString(impactedSystems));

        // Rebuild from specified step, if needed
        Step latest = getLatestMandatoryStep(sheet);

        // Avoid SCORE step?
        Step scoreStep = Steps.valueOf(Steps.SCORE);
        if (!merge && latest == scoreStep) {
            latest = Steps.previous(latest);
        }

        if ((latest == null) || (compare(latest, step) >= 0)) {
            // The range of steps to re-perform
            SortedSet<Step> stepRange = range(step, latest);

            notifyStart();

            try {
                doSheetStepSet(stepRange, sheet, impactedSystems);
            } catch (ProcessingCancellationException pce) {
                throw pce;
            } catch (Exception ex) {
                logger.warn("Error in re-processing from " + step, ex);
            } finally {
                notifyStop();
            }
        }
    }

    //----------------//
    // doOneScoreStep //
    //----------------//
    /**
     * At score level, do just one specified step, synchronously, with
     * display of related UI and recording of the step into the script.
     *
     * @param step  the step to perform
     * @param score the score to be processed
     * @throws StepException
     */
    private static void doOneScoreStep (final Step step,
                                        final Score score)
            throws StepException
    {
        long startTime = System.currentTimeMillis();
        logger.debug("{} Starting", step);

        // Standard processing (using first sheet)
        Sheet sheet = score.getFirstPage().getSheet();
        step.doStep(null, sheet);

        final long stopTime = System.currentTimeMillis();
        final long duration = stopTime - startTime;
        logger.debug("{} completed in {} ms", step, duration);

        // Record this in score bench
        score.getBench().recordStep(step, duration);
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
     * @param score   the score to be processed
     * @throws StepException
     */
    private static void doScoreStepSet (final SortedSet<Step> stepSet,
                                        final Score score)
    {
        if (score.isMultiPage()) {
            if (OmrExecutors.defaultParallelism.getTarget() == true) {
                // Process all sheets in parallel
                List<Callable<Void>> tasks = new ArrayList<>();

                for (TreeNode pn : new ArrayList<>(score.getPages())) {
                    final Page page = (Page) pn;

                    tasks.add(
                            new Callable<Void>()
                    {
                        @Override
                        public Void call ()
                                throws StepException
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
                    List<Future<Void>> futures = OmrExecutors.
                            getCachedLowExecutor().invokeAll(tasks);
                } catch (InterruptedException ex) {
                    logger.warn("Error in parallel doScoreStepSet", ex);
                }
            } else {
                // Process one sheet after the other
                for (TreeNode pn : new ArrayList<>(score.getPages())) {
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
     * At sheet level, perform a set of steps, with online progress monitor.
     * If any step in the step set throws {@link StepException} the processing
     * is stopped.
     *
     * @param stepSet the set of steps
     * @param sheet   the sheet to be processed
     * @params systems the impacted systems (null for all of them)
     * @throws StepException if processing must stop
     */
    private static void doSheetStepSet (SortedSet<Step> stepSet,
                                        Sheet sheet,
                                        Collection<SystemInfo> systems)
    {
        try {
            for (Step step : stepSet) {
                notifyMsg(sheet.getLogPrefix() + step);
                doOneSheetStep(step, sheet, systems);
            }
        } catch (StepException se) {
            logger.info("{}Processing stopped. {}",
                    sheet.getLogPrefix(), se.getMessage());
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
     * Organize the scheduling of steps at score level among the sheets,
     * since some steps have specific requirements
     *
     * @param orderedSet the sequence of steps
     * @param score      the score to process
     */
    private static void scheduleScoreStepSet (SortedSet<Step> orderedSet,
                                              Score score)
    {
        // Make a copy, so that we can modify the step set locally
        SortedSet<Step> stepSet = new TreeSet<>(orderedSet);

        if (stepSet.isEmpty()) {
            return;
        }

        logger.info("{}scheduling {}", score.getLogPrefix(), stepSet);

        long startTime = System.currentTimeMillis();
        notifyStart();

        try {
            // SCALE step, if present, is always the first step
            // We perform this step on all sheets, to allow early filtering
            Step scaleStep = Steps.valueOf(Steps.SCALE);

            if (stepSet.contains(scaleStep)) {
                SortedSet<Step> single = new TreeSet<>(comparator);
                single.add(scaleStep);
                stepSet.remove(scaleStep);

                doScoreStepSet(single, score);

                if (!score.isMultiPage()
                    && (score.getFirstPage().getSheet().getScale() == null)) {
                    throw new StepException("No scale available");
                }
            }

            // Perform the remaining steps at sheet level, if any
            SortedSet<Step> sheetSet = new TreeSet<>(comparator);

            for (Step step : stepSet) {
                if (!step.isScoreLevel()) {
                    sheetSet.add(step);
                }
            }

            stepSet.removeAll(sheetSet);
            doScoreStepSet(sheetSet, score);

            // Finally, perform steps that must be done at score level
            // SCORE step if present, must be done first, and in case of failure
            // must prevent the following score-level steps to run.
            Step scoreStep = Steps.valueOf(Steps.SCORE);

            if (stepSet.contains(scoreStep)) {
                stepSet.remove(scoreStep);
                doOneScoreStep(scoreStep, score);
            }

            // Perform the other score-level steps, if any
            for (Step step : stepSet) {
                try {
                    doOneScoreStep(step, score);
                } catch (StepException ignored) {
                }
            }
        } catch (StepException se) {
            logger.info("Processing stopped. {}", se.getMessage());
        } finally {
            notifyStop();
        }

        long stopTime = System.currentTimeMillis();
        logger.debug("End of step set in {} ms.", (stopTime - startTime));
    }
}
