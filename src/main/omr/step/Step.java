//----------------------------------------------------------------------------//
//                                                                            //
//                                  S t e p                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.step;

import omr.script.StepTask;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;
import omr.sheet.SystemInfo;

import omr.util.Logger;
import omr.util.Memory;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;

import javax.swing.SwingUtilities;

/**
 * Enum <code>Step</code> lists the various sheet processing steps in
 * chronological order.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public enum Step {
    /**
     * Load the image for the sheet, from a provided image file
     */
    LOAD("Load the sheet picture"),

    /**
     * Determine the general scale of the sheet, based on the mean distance
     * between staff lines
     */
    SCALE("Compute the global Skew, and rotate if needed"), 

    /**
     * Determine the average skew of the picture, and deskews it if needed
     */
    SKEW("Detect & remove all Staff Lines"), 

    /**
     * Retrieve the staff lines, erases their pixels and creates crossing
     * objects when needed
     */
    LINES("Retrieve horizontal Dashes"), 

    /**
     * Retrieve the horizontal dashes (ledgers, endings)
     */
    HORIZONTALS("Detect horizontal dashes"), 

    /**
     * Retrieve the vertical bar lines, and so the systems
     */
    SYSTEMS("Detect vertical Bar sticks and thus systems"), 

    /**
     * Retrieve the measures from the bar line glyphs
     */
    MEASURES("Translate Bar glyphs to Measures"), 

    /**
     * Recognize isolated symbols glyphs and aggregates unknown symbols into
     * compound glyphs
     */
    SYMBOLS("Recognize Symbols & Compounds"), 
    /**
     * Retrieve the vertical items such as stems
     */
    VERTICALS("Extract verticals"), 
    /**
     * Process leaves, which are glyphs attached to stems and aggregates unknown
     * leaves into compound glyphs
     */
    LEAVES("Recognize Leaves & Compounds"), 
    /**
     * Cleanup stems and slurs
     */
    CLEANUP("Cleanup stems and slurs"), 

    /**
     * Translate glyphs into score entities
     */
    SCORE("Translate glyphs to score items");
    //
    //--------------------------------------------------------------------------
    //
    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Step.class);

    /** Related UI when used in interactive mode */
    private static volatile StepMonitor monitor;

    /** Description of the step */
    private final String description;

    /** First step */
    public static final Step first = Step.values()[0];

    /** Last step */
    public static final Step last = Step.values()[Step.values().length - 1];

    //--------------------------------------------------------------------------

    //------//
    // Step //
    //------//
    /**
     * This enumeration is not meant to be instantiated outside of this class
     */
    private Step (String description)
    {
        this.description = description;
    }

    //--------------------------------------------------------------------------

    //---------------//
    // createMonitor //
    //---------------//
    /**
     * Allows to couple the steps with a UI.
     * @return the monitor to deal with steps
     */
    public static StepMonitor createMonitor ()
    {
        monitor = new StepMonitor();

        return monitor;
    }

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report the user-friendly description of this step
     *
     * @return the step description
     */
    public String getDescription ()
    {
        return description;
    }

    //------------//
    // getMonitor //
    //------------//
    /**
     * Give access to a related UI monitor
     * @return the related step monitor, or null
     */
    public static StepMonitor getMonitor ()
    {
        return monitor;
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
    public static void notifyMsg (String msg)
    {
        if (monitor != null) {
            monitor.notifyMsg(msg);
        } else {
            logger.info(msg);
        }
    }

    //--------------//
    // performUntil //
    //--------------//
    /**
     * Trigger the execution of all needed steps until this one.
     * Processing is done synchronously, so if asynchronicity is desired, it
     * must be handled by the caller.
     *
     * @param sheet the sheet on which analysis is performed
     * @param param a potential parameter (depending on the processing)
     */
    public void performUntil (Sheet  sheet,
                              Object param)
    {
        // Determine the starting step
        Step          from = (sheet == null) ? first
                             : getFollowingStep(
            sheet.getSheetSteps().getLatestStep());

        // The range of steps to perform
        EnumSet<Step> stepRange = EnumSet.range(from, this);

        try {
            sheet = doStepRange(stepRange, sheet, param, null);
        } catch (Exception ex) {
            logger.warning("Error in processing " + this, ex);
        }

        // Record to script?
        if (sheet != null) {
            sheet.getScript()
                 .addTask(new StepTask(this));
        }
    }

    //-----------//
    // reperform //
    //-----------//
    /**
     * Re-perform all tasks already done, starting from this step, in order to
     * update needed data
     * @param sheet the related sheet, which cannot be null
     * @param systems only the systems to reperform (null means all systems)
     */
    public void reperform (Sheet                  sheet,
                           Collection<SystemInfo> systems)
    {
        if (sheet == null) {
            throw new IllegalArgumentException(
                "Reperform step on a null sheet");
        }

        // The range of steps to re-perform
        EnumSet<Step> stepRange = EnumSet.range(
            this,
            sheet.getSheetSteps().getLatestStep());

        try {
            doStepRange(stepRange, sheet, null, systems);
        } catch (Exception ex) {
            logger.warning("Error in re-processing " + this, ex);
        }
    }

    //-------------//
    // doStepRange //
    //-------------//
    /**
     * Perform a range of steps, with an online display of a progress
     * monitor.
     *
     * @param stepRange the range of steps
     * @param sheet the sheet being analyzed
     * @param param an optional parameter
     * @param systems systems to process (null means all systems)
     */
    private Sheet doStepRange (EnumSet<Step>          stepRange,
                               Sheet                  sheet,
                               Object                 param,
                               Collection<SystemInfo> systems)
    {
        if (logger.isFineEnabled()) {
            StringBuilder sb = new StringBuilder("Performing ");
            sb.append(stepRange);

            if (sheet != null) {
                sb.append(" sheet=")
                  .append(sheet.getRadix());
            }

            if (param != null) {
                sb.append(" param=")
                  .append(param);
            }

            if (systems != null) {
                sb.append(SystemInfo.toString(systems));
            }

            sb.append(" ...");
            logger.fine(sb.toString());
        }

        try {
            // "Activate" the progress bar?
            if (monitor != null) {
                monitor.animate(true);
            }

            // The actual processing
            for (Step step : stepRange) {
                notifyMsg(step.name());
                sheet = step.doOneStep(sheet, param, systems);

                if (monitor != null) {
                    monitor.animate();
                    // Update sheet (& score) dependent entities
                    SheetManager.setSelectedSheet(sheet);
                }
            }
        } catch (Exception ex) {
            logger.warning("Processing aborted", ex);
        } finally {
            // Reset the progress bar?
            if (monitor != null) {
                notifyMsg("");
                monitor.animate(false);
            }

            if (logger.isFineEnabled()) {
                logger.fine("End of " + stepRange + ".");
            }

            return sheet;
        }
    }

    //------//
    // next //
    //------//
    /**
     * Report the step right after this one
     * @return the following step, or null if none
     */
    public Step next ()
    {
        if (this != last) {
            return Step.values()[ordinal() + 1];
        } else {
            return null;
        }
    }

    //------------------//
    // getFollowingStep //
    //------------------//
    private static Step getFollowingStep (Step of)
    {
        if (of == null) {
            return first;
        } else {
            return of.next();
        }
    }

    //-----------//
    // doOneStep //
    //-----------//
    /**
     * Do this step, synchronously.
     *
     * @param sheet the sheet to be processed
     * @param param the potential step parameter
     * @param systems systems to process (null means all systems)
     *
     * @return the (created or modified) sheet
     * @throws StepException
     */
    Sheet doOneStep (Sheet                  sheet,
                     Object                 param,
                     Collection<SystemInfo> systems)
        throws StepException
    {
        long startTime = 0;

        if (logger.isFineEnabled()) {
            logger.fine(this + " Starting");
            startTime = System.currentTimeMillis();
        }

        // Do we have the sheet already ?
        if (sheet == null) {
            // Load sheet using the provided parameter
            sheet = new Sheet((File) param, /* force => */
                              false);
        }

        // Standard processing on an existing sheet
        sheet.getSheetSteps()
             .doStep(this, systems);

        // Update user interface ?
        if (monitor != null) {
            final Sheet finalSheet = sheet;
            SwingUtilities.invokeLater(
                new Runnable() {
                        public void run ()
                        {
                            finalSheet.getSheetSteps()
                                      .displayUI(Step.this);
                        }
                    });
        }

        if (logger.isFineEnabled()) {
            final long stopTime = System.currentTimeMillis();
            logger.fine(
                this + " completed in " + (stopTime - startTime) + " ms with " +
                Memory.getValue() + " bytes");
        }

        return sheet;
    }
}
