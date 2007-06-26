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

import omr.sheet.Sheet;

import omr.step.StepException;

import omr.util.Logger;
import omr.util.Memory;

import java.io.File;
public enum Step {
    LOAD("Load the sheet picture"),
    SCALE("Compute the global Skew, and rotate if needed"),
    SKEW("Detect & remove all Staff Lines"),
    LINES("Retrieve horizontal Dashes"),
    HORIZONTALS("Detect vertical Bar lines"),
    BARS("Detect vertical Bar lines"),
    SYMBOLS("Recognize Symbols & Compounds"),
    VERTICALS("Extract verticals"),
    LEAVES("Recognize Leaves & Compounds"),
    CLEANUP("Cleanup stems and slurs"),
    SCORE("Translate glyphs to score items");

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Step.class);

    /** Related UI when used in interactive mode */
    private static volatile StepMonitor monitor;

    /** Description of the step */
    private final String description;

    //------//
    // Step //
    //------//
    Step (String description)
    {
        this.description = description;
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

    //---------//
    // getStep //
    //---------//
    /**
     * Retrieve a step, knowing its name
     *
     * @param id the name of the step (case is not relevant)
     *
     * @return the step found, or null otherwise
     */
    public static Step getStep (String id)
    {
        Step step = Step.valueOf(id.toUpperCase());

        if (step != null) {
            return step;
        } else {
            logger.severe("Cannot find Step for id " + id);

            return null;
        }
    }

    //-----------//
    // doPerform //
    //-----------//
    /**
     * Meant to actually perform the series of step(s), with or without UI.
     *
     * @param sheet the sheet on which analysis is performed, sheet may be null
     *              (case of loading a brand new sheet)
     * @param param a potential parameter for the step. This is actually used
     *              only when loading a new sheet.
     *
     * @throws StepException Raised when processing has failed
     */
    public void doPerform (Sheet  sheet,
                           Object param)
        throws StepException
    {
        Step current = null;

        try {
            // Force execution of specified step
            current = this;
            doStep(sheet, param);
        } catch (StepException ex) {
            // User has already been informed, so just stop
        } catch (Exception ex) {
            logger.warning("Exception in performing step " + current, ex);
        }
    }

    //------------//
    // getMonitor //
    //------------//
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

    //---------//
    // perform //
    //---------//
    /**
     * Trigger the execution of all needed steps up to this one.
     *
     * <p> This is delegated to the UI if there is such interface, which will
     * ultimately call doPerform(). If there is no UI, doPerform() is called
     * directly.
     *
     * @param sheet the sheet on which analysis is performed
     * @param param a potential parameters (depending on the processing)
     */
    public void perform (Sheet  sheet,
                         Object param)
    {
        try {
            if (monitor != null) {
                monitor.perform(this, sheet, param);
            } else {
                doPerform(sheet, param);
            }
        } catch (StepException ex) {
            // User has already been informed of error details, so do nothing
        }
    }

    //--------//
    // doStep //
    //--------//
    /**
     * Do just one step (probably within a larger series)
     *
     * @param sheet the sheet to be processed
     * @param param the potential step parameter
     *
     * @return the (created or modified) sheet
     * @throws StepException
     */
    private Sheet doStep (Sheet  sheet,
                          Object param)
        throws StepException
    {
        long startTime = 0;

        if (logger.isFineEnabled()) {
            logger.fine(this + " Starting");
            startTime = System.currentTimeMillis();
        }

        notifyMsg(toString() + ((param != null) ? (" " + param) : ""));

        // Do we have the sheet already ?
        if (sheet == null) {
            // Load sheet using the provided parameter
            sheet = new Sheet((File) param, /* force => */
                              false);
        }

        // Check for loading of a sheet
        if (this != LOAD) {
            // Standard processing on an existing sheet
            //            sheet.getInstanceStep(this)
            //                 .undo();
            sheet.getSheetSteps()
                 .getResult(this);
        }

        // Update user interface ?
        if (monitor != null) {
            sheet.getSheetSteps()
                 .displayUI(this);
        }

        if (logger.isFineEnabled()) {
            final long stopTime = System.currentTimeMillis();
            logger.fine(
                this + "Completed in " + (stopTime - startTime) + " ms with " +
                Memory.getValue() + " bytes");
        }

        return sheet;
    }
}
