//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t e p p i n g                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.ui.ScoreActions;

import omr.script.BookStepTask;

import omr.sheet.Book;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.SheetsController;

import omr.util.OmrExecutors;
import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

/**
 * Class {@code Stepping} handles the scheduling of step(s) on a book or a sheet.
 * <p>
 * When running in interactive mode, progress is notified to the user interface.
 * <p>
 * <img src="doc-files/Stepping.png" />
 *
 * @author Hervé Bitteur
 */
public abstract class Stepping
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Stepping.class);

    /** Related progress monitor when used in interactive mode. */
    private static volatile StepMonitor monitor;

    /** First value of Step. */
    private static final Step FIRST_STEP = Step.values()[0];

    /** Last value of Step. */
    private static final Step LAST_STEP = Step.values()[Step.values().length - 1];

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Not meant to be instantiated.
     */
    private Stepping ()
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

    //-----------------//
    // ensureSheetStep //
    //-----------------//
    /**
     * Make sure the provided step has been reached on the sheet at hand
     *
     * @param step  the step to check
     * @param sheet the sheet to process, if so needed
     */
    public static void ensureSheetStep (Step step,
                                        Sheet sheet)
    {
        if (!sheet.isDone(step)) {
            processSheet(Collections.singleton(step), sheet);
        }
    }

    //---------------//
    // getLatestStep //
    //---------------//
    /**
     * Report the latest step done so far on the provided sheet.
     *
     * @param sheet the sheet concerned
     * @return the latest step done, or null
     */
    public static Step getLatestStep (Sheet sheet)
    {
        Step latest = null;

        for (Step step : Step.values()) {
            if (sheet.isDone(step)) {
                latest = step;
            }
        }

        return latest;
    }

    //-------------//
    // processBook //
    //-------------//
    /**
     * At book level, perform the desired steps (and all needed intermediate steps).
     * <p>
     * This method is used from the CLI, from a script or from the Step menu (via the BookStepTask),
     * and from the drag&drop handler.
     *
     * @param desiredSteps the desired steps
     * @param sheetIndices specific set of sheet indices, if any
     * @param book         the processed book (and its sheets)
     */
    public static void processBook (Set<Step> desiredSteps,
                                    SortedSet<Integer> sheetIndices,
                                    Book book)
    {
        logger.debug("processBook {} on {}", desiredSteps, book);

        // Sanity checks
        if (book == null) {
            throw new IllegalArgumentException("Book is null");
        }

        // Determine the precise ordered collection of steps to perform
        TreeSet<Step> orderedSteps = new TreeSet(desiredSteps);

        try {
            if (book.getSheets().isEmpty()) {
                // Create book sheets if not yet done
                // This will usually trigger the early step on first sheet in synchronous mode
                book.createSheets(sheetIndices);
            }

            if (desiredSteps.isEmpty()) {
                return; // No explicit step to perform
            }

            // Find the first step across all book sheets
            Step first = LAST_STEP;

            for (Sheet sheet : book.getSheets()) {
                Step latest = getLatestStep(sheet);

                if (latest == null) {
                    // This sheet has not been processed at all
                    first = null;

                    break;
                }

                if (latest.compareTo(first) < 0) {
                    first = latest;
                }
            }

            // Determine starting step and stopping step
            final Step start = (first == null) ? FIRST_STEP : next(first);
            final Step stop = (first.compareTo(orderedSteps.last()) >= 0) ? first
                    : orderedSteps.last();

            // Add all intermediate steps
            for (Step step : EnumSet.range(start, stop)) {
                orderedSteps.add(step);
            }

            // Launch the steps on each sheet
            doBookStepSet(orderedSteps, book);

            // Record the step tasks to script
            for (Step step : desiredSteps) {
                book.getScript().addTask(new BookStepTask(step));
            }
        } catch (ProcessingCancellationException pce) {
            throw pce;
        } catch (Exception ex) {
            logger.warn("Error in performing " + orderedSteps, ex);
        }
    }

    //--------------//
    // processSheet //
    //--------------//
    /**
     * At sheet level, perform the desired steps (with all needed intermediate steps).
     * <p>
     * This method is used from the CLI, from a script or from the Step menu (via the
     * SheetStepTask), and from the drag&drop handler. ???
     *
     * @param desiredSteps the desired steps
     * @param sheet        the processed sheet
     * @return true if successful, false if exception was thrown
     */
    public static boolean processSheet (Set<Step> desiredSteps,
                                        Sheet sheet)
    {
        logger.debug("processSheet {} on {}", desiredSteps, sheet);

        // Sanity checks
        if (sheet == null) {
            throw new IllegalArgumentException("Sheet is null");
        }

        // Determine the precise ordered collection of steps to perform
        TreeSet<Step> orderedSteps = new TreeSet(desiredSteps);

        notifyStart();

        try {
            // Determine starting step and stopping step
            final Step start;
            final Step stop;

            // Retrieve the latest step done
            Step latest = getLatestStep(sheet);
            Step firstDesired = orderedSteps.first();
            start = (latest == null) ? FIRST_STEP
                    : ((latest == firstDesired) ? firstDesired : next(latest));
            stop = ((latest != null) && (latest.compareTo(orderedSteps.last()) >= 0)) ? latest
                    : orderedSteps.last();

            // Add all intermediate steps
            for (Step step : EnumSet.range(start, stop)) {
                orderedSteps.add(step);
            }

            // Remove the LOAD step (unless it is explicitly desired)
            // LOAD step may appear only in reprocessSheet()
            if (!desiredSteps.contains(Step.LOAD)) {
                orderedSteps.remove(Step.LOAD);
            }

            // Schedule the steps on the sheet
            doSheetStepSet(orderedSteps, sheet, null);

            //            // Record the step tasks to script
            //            for (Step step : desiredSteps) {
            //                sheet.getScript().addTask(new BookStepTask(step));
            //            }
            return true;
        } catch (ProcessingCancellationException pce) {
            throw pce;
        } catch (Exception ex) {
            logger.warn(sheet.getLogPrefix() + " Error in performing " + orderedSteps, ex);

            return false;
        } finally {
            notifyStop();
        }
    }

    //----------------//
    // reprocessSheet //
    //----------------//
    /**
     * For just a given sheet, update the steps already done, starting from the provided
     * step.
     * This method will try to minimize the systems to rebuild in each step, by processing only the
     * provided "impacted" systems.
     *
     * @param step            the step to restart from
     * @param sheet           the sheet to process
     * @param impactedSystems the ordered set of systems to rebuild, or null for all systems
     * @param imposed         flag to indicate that update is imposed
     */
    @Deprecated
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
     * For just a given sheet, update the steps already done, starting from the provided
     * step.
     * This method will try to minimize the systems to rebuild in each step, by processing only the
     * provided "impacted" systems.
     *
     * @param step            the step to restart from
     * @param sheet           the sheet to process
     * @param impactedSystems the ordered set of systems to rebuild, or null for all systems
     * @param imposed         flag to indicate that update is imposed
     * @param merge           true if step SCORE (merge of pages) is allowed
     */
    @Deprecated
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

        logger.debug(
                "{}Rebuild launched from {} on {}",
                sheet.getLogPrefix(),
                step,
                SystemInfo.toString(impactedSystems));

        // Rebuild from specified step, if needed
        Step latest = getLatestStep(sheet);

        if ((latest == null) || (latest.compareTo(step) >= 0)) {
            // The range of steps to re-perform
            EnumSet<Step> stepRange = EnumSet.range(step, latest);

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

    //------------//
    // notifyStep //
    //------------//
    /**
     * Notify the UI part that the provided step has started or stopped in the provided
     * sheet.
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
                                sheet.getAssembly().selectViewTab(step.getTab());
                            }

                            // Call attention to this sheet (only if displayed),
                            // so that score-dependent actions can get enabled.
                            SheetsController ctrl = SheetsController.getInstance();
                            Sheet currentSheet = ctrl.getSelectedSheet();

                            if (currentSheet == sheet) {
                                ctrl.callAboutSheet(sheet);
                            }
                        }
                    });
        }
    }

    //---------------//
    // doBookStepSet //
    //---------------//
    /**
     * At book level, perform a set of steps, with online display of a progress monitor.
     * <p>
     * We can perform all the sheets in parallel or in sequence, depending on the current value of
     * {@link OmrExecutors#defaultParallelism}.
     * TODO: We could allow parallelism within a sheet and not between sheets, to save memory.
     *
     * @param stepSet the set of steps
     * @param book    the book to be processed
     * @throws StepException
     */
    private static void doBookStepSet (final Set<Step> stepSet,
                                       final Book book)
    {
        if (stepSet.isEmpty()) {
            return;
        }

        logger.info("{}Book processing {}", book.getLogPrefix(), stepSet);

        long startTime = System.currentTimeMillis();
        notifyStart();

        try {
            if (book.isMultiSheet()) {
                if (OmrExecutors.defaultParallelism.getTarget() == true) {
                    // Process all sheets in parallel
                    List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

                    for (final Sheet sheet : new ArrayList<Sheet>(book.getSheets())) {
                        tasks.add(
                                new Callable<Void>()
                                {
                                    @Override
                                    public Void call ()
                                    throws StepException
                                    {
                                        doSheetStepSet(stepSet, sheet, null);

                                        return null;
                                    }
                                });
                    }

                    try {
                        List<Future<Void>> futures = OmrExecutors.getCachedLowExecutor()
                                .invokeAll(tasks);
                    } catch (InterruptedException ex) {
                        logger.warn("Error in parallel doScoreStepSet", ex);
                    }
                } else {
                    // Process one sheet after the other
                    for (Sheet sheet : new ArrayList<Sheet>(book.getSheets())) {
                        doSheetStepSet(stepSet, sheet, null);
                    }
                }
            } else {
                // Process the single sheet
                doSheetStepSet(stepSet, book.getFirstSheet(), null);
            }
        } finally {
            notifyStop();
        }

        long stopTime = System.currentTimeMillis();
        logger.debug("End of step set in {} ms.", (stopTime - startTime));
    }

    //----------------//
    // doOneSheetStep //
    //----------------//
    /**
     * At sheet level, do just one specified step, synchronously, with display of
     * related UI and recording of the step into the script.
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
        logger.debug("{}{} completed in {} ms", sheet.getLogPrefix(), step, duration);

        // Record this in sheet->score bench
        sheet.getBench().recordStep(step, duration);
    }

    //----------------//
    // doSheetStepSet //
    //----------------//
    /**
     * At sheet level, perform a set of steps, with online progress monitor.
     * If any step in the step set throws {@link StepException} the processing is stopped.
     *
     * @param stepSet the set of steps
     * @param sheet   the sheet to be processed
     * @params systems the impacted systems (null for all of them)
     * @throws StepException if processing must stop
     */
    private static void doSheetStepSet (Set<Step> stepSet,
                                        Sheet sheet,
                                        Collection<SystemInfo> systems)
    {
        SortedSet<Step> sheetStepSet = new TreeSet(stepSet);

        for (Iterator<Step> it = sheetStepSet.iterator(); it.hasNext();) {
            Step step = it.next();

            if (sheet.isDone(step)) {
                it.remove();
            }
        }

        logger.debug("Sheet#{} scheduling {}", sheet.getIndex(), sheetStepSet);

        StopWatch watch = new StopWatch("doSheetStepSet");

        try {
            for (Step step : sheetStepSet) {
                watch.start(step.name());
                notifyMsg(sheet.getLogPrefix() + step);
                doOneSheetStep(step, sheet, systems);
            }

            if (Main.getGui() != null) {
                // Update sheet tab color
                SheetsController.getInstance().markTab(sheet, Color.BLACK);
            }
        } catch (StepException se) {
            logger.info("{}Processing stopped. {}", sheet.getLogPrefix(), se.getMessage());
        }

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //------//
    // next //
    //------//
    /**
     * Report the step immediately after the provided one.
     *
     * @param step the provided step
     * @return the next step if any, otherwise null
     */
    private static Step next (Step step)
    {
        if (step == LAST_STEP) {
            return null;
        }

        return Step.values()[step.ordinal() + 1];
    }

    //-----------//
    // notifyMsg //
    //-----------//
    /**
     * Notify a simple message, which may be not related to any step.
     *
     * @param msg the message to display on the UI window, or to write in the log if there is no UI.
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
     * When running interactively, start the progress bar animation.
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
     * When running interactively, stop the progress bar animation.
     */
    private static void notifyStop ()
    {
        // Reset the progress bar?
        if (monitor != null) {
            notifyMsg("");
            monitor.displayAnimation(false);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");
    }
}
