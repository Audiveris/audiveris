//----------------------------------------------------------------------------//
//                                                                            //
//                                  S t e p                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr;

import omr.StepMonitor;

import omr.sheet.Sheet;

import omr.util.Logger;
import omr.util.Memory;

import java.io.File;
import java.lang.reflect.Field;

/**
 * Class <code>Step</code> describes the ordered sequence of processing steps
 * that are defined on a sheet. The comprehensive ordered list of step names is
 * defined in {@link omr.sheet.Sheet} class.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Step
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Step.class);

    /** Related UI when used in interactive mode */
    private static volatile StepMonitor monitor;

    /** The most popular step, so it's easier to get it directly */
    private static Step LOAD;

    //~ Instance fields --------------------------------------------------------

    /** Reflection field to the corresponding sheet InstanceStep */
    private final Field field;

    /** Readable description */
    private final String description;

    //~ Constructors -----------------------------------------------------------

    /**
     * Create a step, related to a sheet InstanceStep
     *
     * @param field       the reflection field of the related InstanceStep
     * @param description the verbose description of the related InstanceStep
     */
    public Step (Field  field,
                 String description)
    {
        this.field = field;
        this.description = description;
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // createMonitor //
    //---------------//
    /**
     * Allows to couple the steps with a UI.
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
     * Return a description for this step, usable for a tip for example.
     *
     * @return a description string
     */
    public String getDescription ()
    {
        return description;
    }

    //----------//
    // getField //
    //----------//
    /**
     * Return the reflection field of the related Sheet.InstanceStep
     *
     * @return the related field
     */
    public Field getField ()
    {
        return field;
    }

    //-------------//
    // getLoadStep //
    //-------------//
    /**
     * Return the most popular step accessed by name : LOAD
     *
     * @return the LOAD step
     */
    public static Step getLoadStep ()
    {
        if (LOAD == null) {
            LOAD = getStep("LOAD");
        }

        return LOAD;
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
        for (Step step : Sheet.getSteps()) {
            if (id.equalsIgnoreCase(step.toString())) {
                return step;
            }
        }

        logger.severe("Cannot find Step for id " + id);

        return null;
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
     * @throws ProcessingException Raised when processing has failed
     */
    public void doPerform (Sheet  sheet,
                           Object param)
        throws ProcessingException
    {
        Step current = null;

        try {
            // Force execution of specified step
            current = this;
            doStep(sheet, param);
        } catch (ProcessingException ex) {
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
        } catch (ProcessingException ex) {
            // User has already been informed of error details, so do nothing
        }
    }

    //----------//
    // toString //
    //----------//
    /**
     * Usual method to return a readable string
     *
     * @return a short string
     */
    @Override
    public String toString ()
    {
        return field.getName();
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
     * @throws ProcessingException
     */
    private Sheet doStep (Sheet  sheet,
                          Object param)
        throws ProcessingException
    {
        long startTime = 0;

        if (logger.isFineEnabled()) {
            logger.fine("Starting " + this);
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
        if (this != getLoadStep()) {
            // Standard processing on an existing sheet
            sheet.getInstanceStep(this)
                 .undo();
            sheet.getInstanceStep(this)
                 .getResult();
        }

        // Update user interface ?
        if (monitor != null) {
            sheet.getInstanceStep(this)
                 .displayUI();
        }

        if (logger.isFineEnabled()) {
            final long stopTime = System.currentTimeMillis();
            logger.fine(
                "Completed " + this + " in " + (stopTime - startTime) +
                " ms with " + Memory.getValue() + " bytes");
        }

        return sheet;
    }
}
