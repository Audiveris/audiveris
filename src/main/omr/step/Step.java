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

import omr.util.Logger;
import omr.util.Memory;
import omr.util.OmrExecutors;

import java.io.File;

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
     * Retrieve the vertical bar lines, and so the systems and measures
     */
    BARS("Detect vertical Bar lines"), 
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

    //---------------//
    // performSerial //
    //---------------//
    /**
     * Trigger the execution of this step serially
     *
     * @param sheet the sheet on which analysis is performed
     * @param param a potential parameter (depending on the processing)
     */
    public void performSerial (Sheet  sheet,
                               Object param)
    {
        try {
            if (monitor != null) {
                monitor.perform(this, sheet, param);
            } else {
                doStep(sheet, param);
            }
        } catch (Exception ex) {
            logger.warning("Error in processing " + this, ex);
        }
    }

    //-----------------//
    // performParallel //
    //-----------------//
    /**
     * Trigger the execution of this step in parallel
     *
     * @param sheet the sheet on which analysis is performed
     * @param param a potential parameter (depending on the processing)
     */
    public void performParallel (final Sheet  sheet,
                                 final Object param)
    {
        OmrExecutors.getLowExecutor()
                    .execute(
            new Runnable() {
                    public void run ()
                    {
                        performSerial(sheet, param);
                    }
                });
    }

    //--------//
    // doStep //
    //--------//
    /**
     * Do this step
     *
     * @param sheet the sheet to be processed
     * @param param the potential step parameter
     *
     * @return the (created or modified) sheet
     * @throws StepException
     */
    Sheet doStep (Sheet  sheet,
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

        // Record the step action into sheet script
        // (except for LOAD step, which is done by default)
        if (this != LOAD) {
            sheet.getScript()
                 .addTask(new StepTask(this));
        }

        // Check for loading of a sheet
        if (this != LOAD) {
            // Standard processing on an existing sheet
            sheet.getSheetSteps()
                 .getResult(this);
        }

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
