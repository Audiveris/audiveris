//----------------------------------------------------------------------------//
//                                                                            //
//                                  S t e p                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.log.Logger;

import omr.script.StepTask;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.SheetsController;

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
    LOAD(true, "Picture", "Load the sheet picture"),

    /**
     * Determine the general scale of the sheet, based on the mean distance
     * between staff lines
     */
    SCALE(true, LOAD.label, "Compute the global Skew, and rotate if needed"), 

    /**
     * Determine the average skew of the picture, and deskews it if needed
     */
    SKEW(true, "Skew", "Detect & remove all Staff Lines"), 

    /**
     * Retrieve the staff lines, erases their pixels and creates crossing
     * objects when needed
     */
    LINES(true, "Lines", "Retrieve horizontal Dashes"), 

    /**
     * Retrieve the horizontal dashes (ledgers, endings)
     */
    HORIZONTALS(true, "Horizontals", "Detect horizontal dashes"), 

    /**
     * Retrieve the vertical bar lines, and so the systems
     */
    SYSTEMS(true, "Systems", "Detect vertical Bar sticks and thus systems"), 

    /**
     * Retrieve the measures from the bar line glyphs
     */
    MEASURES(true, SYSTEMS.label, "Translate Bar glyphs to Measures"), 

    /**
     * Recognize isolated symbols glyphs and aggregates unknown symbols into
     * compound glyphs
     */
    SYMBOLS(true, "Glyphs", "Recognize Symbols & Compounds"), 

    /**
     * Retrieve the vertical items such as stems
     */
    VERTICALS(true, "Verticals", "Extract verticals"), 

    /**
     * Process specific patterns at sheet glyph level
     * (true,clefs, sharps, naturals, stems, slurs, ...)
     */
    PATTERNS(true, SYMBOLS.label, "Specific sheet glyph patterns"), 

    /**
     * Translate glyphs into score entities
     */
    SCORE(true, SYMBOLS.label, "Translate glyphs to score items"), 

    /**
     * Play the whole score
     */
    PLAY(false, SYMBOLS.label, "Play the whole score"), 

    /**
     * Write the output MIDI file
     */
    MIDI(false, SYMBOLS.label, "Write the output MIDI file"), 

    /**
     * Export the score into the MusicXML file
     */
    EXPORT(false, SYMBOLS.label, "Export the score into the MusicXML file");
    //
    //--------------------------------------------------------------------------
    //
    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Step.class);

    /** Related UI when used in interactive mode */
    private static volatile StepMonitor monitor;

    /** Is the step mandatory? */
    public final boolean isMandatory;

    /** Related short label */
    public final String label;

    /** Description of the step */
    public final String description;

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
     * @param label The title of the related (or most relevant) view tab
     * @param description A step description for the end user
     */
    private Step (boolean isMandatory,
                  String  label,
                  String  description)
    {
        this.isMandatory = isMandatory;
        this.label = label;
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
     * Trigger the execution of all mandatory steps until this one.
     * Processing is done synchronously, so if asynchronicity is desired, it
     * must be handled by the caller.
     *
     * <p>There is a mutual exclusion with {@link SheetSteps#rebuildAfter}
     *
     * @param sheet the sheet on which analysis is performed
     */
    public void performUntil (Sheet sheet)
    {
        if (sheet == null) {
            throw new IllegalArgumentException(
                "Cannot run performUntil() on a null sheet");
        }

        try {
            synchronized (sheet.getSheetSteps()) {
                // Determine the starting step
                Step latest = sheet.getSheetSteps()
                                   .getLatestStep();
                Step from = (latest == null) ? first : latest.next();

                if (from.compareTo(this) <= 0) {
                    // The precise collection of steps to perform
                    EnumSet<Step> steps = EnumSet.noneOf(Step.class);

                    for (Step step : EnumSet.range(from, this)) {
                        if (step.isMandatory) {
                            steps.add(step);
                        }
                    }

                    // Last step is always included
                    steps.add(this);

                    doStepRange(steps, sheet, null);
                } else {
                    if (monitor != null) {
                        // Update sheet (& score) dependent entities
                        SheetsController.getInstance()
                                        .setSelectedSheet(sheet);
                    }
                }
            }
        } catch (Exception ex) {
            logger.warning("Error in processing " + this, ex);
        }

        // Record the step task to script
        sheet.getScript()
             .addTask(new StepTask(this));
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
     * @param systems systems to process (null means all systems)
     */
    public static void doStepRange (EnumSet<Step>          stepRange,
                                    Sheet                  sheet,
                                    Collection<SystemInfo> systems)
    {
        long startTime = 0;

        if (logger.isFineEnabled()) {
            startTime = System.currentTimeMillis();

            StringBuilder sb = new StringBuilder("Performing ");
            sb.append(stepRange);

            if (sheet != null) {
                sb.append(" sheet=")
                  .append(sheet.getRadix());
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

            // Systems are rebuilt from scratch in SYSTEMS step, so ...
            if (stepRange.contains(Step.SYSTEMS)) {
                systems = null;
            }

            // The actual processing
            for (Step step : stepRange) {
                notifyMsg(step.name());
                step.doOneStep(sheet, systems);

                if (monitor != null) {
                    monitor.animate();
                    // Update sheet (& score) dependent entities
                    SheetsController.getInstance()
                                    .setSelectedSheet(sheet);
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
                long stopTime = System.currentTimeMillis();
                logger.fine(
                    "End of " + stepRange + " in " + (stopTime - startTime) +
                    " ms.");
            }
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

    //-----------//
    // doOneStep //
    //-----------//
    /**
     * Do this step, synchronously.
     *
     * @param sheet the sheet to be processed
     * @param systems systems to process (null means all systems)
     * @throws StepException
     */
    private void doOneStep (Sheet                  sheet,
                            Collection<SystemInfo> systems)
        throws StepException
    {
        long startTime = 0;

        if (logger.isFineEnabled()) {
            logger.fine(this + " Starting");
            startTime = System.currentTimeMillis();
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
                this + " completed in " + (stopTime - startTime) + " ms");
        }
    }

    //----------//
    // Constant //
    //----------//
    /**
     * Class <code>Constant</code> is a subclass of
     * {@link omr.constant.Constant}, meant to store a {@link Step} value.
     */
    public static class Constant
        extends omr.constant.Constant
    {
        /**
         * Normal constructor
         *
         * @param unit         the enclosing unit
         * @param name         the constant name
         * @param defaultValue the default Step value
         * @param description  the semantic of the constant
         */
        public Constant (java.lang.String unit,
                         java.lang.String name,
                         Step             defaultValue,
                         java.lang.String description)
        {
            super(null, defaultValue.toString(), description);
            setUnitAndName(unit, name);
        }

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the default Step value
         * @param description  the semantic of the constant
         */
        public Constant (Step             defaultValue,
                         java.lang.String description)
        {
            super(null, defaultValue.toString(), description);
        }

        /**
         * Set a new value to the constant
         *
         * @param val the new Step value
         */
        public void setValue (Step val)
        {
            setTuple(val.toString(), val);
        }

        @Override
        public void setValue (java.lang.String string)
        {
            setValue(decode(string));
        }

        /**
         * Retrieve the current constant value
         *
         * @return the current Step value
         */
        public Step getValue ()
        {
            return (Step) getCachedValue();
        }

        @Override
        protected Step decode (java.lang.String str)
        {
            return Step.valueOf(str);
        }
    }
}
