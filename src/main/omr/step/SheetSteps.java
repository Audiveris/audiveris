//----------------------------------------------------------------------------//
//                                                                            //
//                            S h e e t S t e p s                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphInspector;

import omr.log.Logger;

import omr.score.ScoreChecker;
import omr.score.ScoreCleaner;
import omr.score.ScoreFixer;
import omr.score.ScoreManager;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.midi.MidiActions;
import omr.score.ui.ScoreActions;

import omr.selection.GlyphEvent;
import omr.selection.SelectionService;

import omr.sheet.HorizontalsBuilder;
import omr.sheet.LinesBuilder;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SheetsManager;
import omr.sheet.SkewBuilder;
import omr.sheet.SystemInfo;
import omr.sheet.picture.ImageFormatException;
import omr.sheet.picture.Picture;
import static omr.step.Step.*;

import omr.util.WrappedBoolean;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.swing.*;

/**
 * Class <code>SheetSteps</code> handles the actual progress of steps for a
 * given sheet instance.
 *
 * @author Hervé Bitteur
 */
public class SheetSteps
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetSteps.class);

    //~ Instance fields --------------------------------------------------------

    /** The related sheet instance */
    private final Sheet sheet;

    /** The tasks that relate to each step */
    private final EnumMap<Step, SheetTask> tasks = new EnumMap<Step, SheetTask>(
        Step.class);

    /** Step currently being processed */
    private Step currentStep;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SheetSteps //
    //------------//
    /**
     * Create all the task definitions for the given sheet instance
     * @param sheet the given sheet instance
     */
    public SheetSteps (Sheet sheet)
    {
        this.sheet = sheet;

        // Register all tasks
        tasks.put(LOAD, new LoadTask(sheet, LOAD));
        tasks.put(SCALE, new ScaleTask(sheet, SCALE));
        tasks.put(SKEW, new SkewTask(sheet, SKEW));
        tasks.put(LINES, new LinesTask(sheet, LINES));
        tasks.put(HORIZONTALS, new HorizontalsTask(sheet, HORIZONTALS));
        tasks.put(SYSTEMS, new SystemsTask(sheet, SYSTEMS));
        tasks.put(MEASURES, new MeasuresTask(sheet, MEASURES));
        tasks.put(SYMBOLS, new SymbolsTask(sheet, SYMBOLS));
        tasks.put(VERTICALS, new VerticalsTask(sheet, VERTICALS));
        tasks.put(PATTERNS, new PatternsTask(sheet, PATTERNS));
        tasks.put(SCORE, new ScoreTask(sheet, SCORE));
        tasks.put(PRINT, new PrintTask(sheet, PRINT));
        tasks.put(PLAY, new PlayTask(sheet, PLAY));
        tasks.put(MIDI, new MidiWriteTask(sheet, MIDI));
        tasks.put(EXPORT, new ExportTask(sheet, EXPORT));
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getCurrentStep //
    //----------------//
    /**
     * Retrieve the step being processed "as we speak"
     * @return the current step
     */
    public Step getCurrentStep ()
    {
        return currentStep;
    }

    //--------//
    // isDone //
    //--------//
    /**
     * Convenient method to check whether a given step has been done
     * @param step the provided step
     * @return true if step has been done
     */
    public boolean isDone (Step step)
    {
        return getTask(step)
                   .isDone();
    }

    //------------------------//
    // getLatestMandatoryStep //
    //------------------------//
    /**
     * Report the latest mandatory step done so far with the related sheet
     * @return the latest mandatory step done, or null
     */
    public Step getLatestMandatoryStep ()
    {
        Step last = null;

        for (Step step : Step.values()) {
            if (step.isMandatory && isDone(step)) {
                last = step;
            } else {
                break;
            }
        }

        return last;
    }

    //---------------//
    // getLatestStep //
    //---------------//
    /**
     * Report the latest step done so far with the related sheet
     * @return the latest step done, or null
     */
    public Step getLatestStep ()
    {
        Step last = null;

        for (Step step : Step.values()) {
            if (isDone(step)) {
                last = step;
            } else {
                break;
            }
        }

        return last;
    }

    //-----------//
    // isStarted //
    //-----------//
    /**
     * Convenient method to check whether a given step has started
     * @param step the provided step
     * @return true if step has been started
     */
    public boolean isStarted (Step step)
    {
        return getTask(step)
                   .isStarted();
    }

    //-----------//
    // displayUI //
    //-----------//
    /**
     * Launch the UI aspect, if any, of the provided step
     * @param step the provided step
     */
    public void displayUI (Step step)
    {
        getTask(step)
            .displayUI();
    }

    //--------//
    // doStep //
    //--------//
    /**
     * Convenient method to run a given step
     * @param step the given step
     * @param systems systems to process (null means all systems)
     * @exception StepException if processing goes wrong
     */
    public void doStep (Step                   step,
                        Collection<SystemInfo> systems)
        throws StepException
    {
        currentStep = step;

        getTask(step)
            .doStep(systems);
    }

    //-------------//
    // rebuildFrom //
    //-------------//
    /**
     * Update the steps already done, starting from the provided step.
     * This method will try to minimize the systems to rebuild in each step, by
     * processing only the provided "impacted" systems.
     *
     * <p>There is a mutual exclusion with {@link Step#performUntil} method.
     *
     * @param step the step to restart from
     * @param impactedSystems the ordered set of systems to rebuild, or null
     * if all systems must be rebuilt
     * @param imposed flag to indicate that update is imposed
     */
    public synchronized void rebuildFrom (Step                   step,
                                          Collection<SystemInfo> impactedSystems,
                                          boolean                imposed)
    {
        if (SwingUtilities.isEventDispatchThread()) {
            logger.severe("Method rebuildFrom should not run on EDT!");
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
            impactedSystems = new TreeSet<SystemInfo>(sheet.getSystems());
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                "Rebuild launched from " + step + " on" +
                SystemInfo.toString(impactedSystems));
        }

        // Rebuild from specified step, if needed
        if (shouldRebuildFrom(step)) {
            Step          latest = sheet.getSheetSteps()
                                        .getLatestMandatoryStep();

            // The range of steps to re-perform
            EnumSet<Step> stepRange = EnumSet.range(step, latest);

            try {
                Step.doStepRange(stepRange, sheet, impactedSystems);
            } catch (Exception ex) {
                logger.warning("Error in re-processing from " + this, ex);
            }
        }
    }

    //-------------------//
    // shouldRebuildFrom //
    //-------------------//
    /**
     * Check whether some steps need to be rebuilt, starting from step 'from'
     * @param from the step to rebuild from
     * @return true if some rebuilding must take place
     */
    public boolean shouldRebuildFrom (Step from)
    {
        Step latest = sheet.getSheetSteps()
                           .getLatestMandatoryStep();

        return (latest == null) || (latest.compareTo(from) >= 0);
    }

    //---------//
    // getTask //
    //---------//
    /**
     * Give access to the step task
     *
     * @param step the provided step
     * @return the actual task (SheetTask or SystemTask)
     */
    private SheetTask getTask (Step step)
    {
        return tasks.get(step);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        private final Constant.Integer MaxPatternsIterations = new Constant.Integer(
            "count",
            1,
            "Maximum number of iterations for PATTERNS task");
        private final Constant.Integer maxScoreIterations = new Constant.Integer(
            "count",
            2,
            "Maximum number of iterations for SCORE task");
    }

    //------------//
    // ExportTask //
    //------------//
    /**
     * Step to export the score to the MusicXML file. We use the default
     * directory for scores.
     */
    private static class ExportTask
        extends SheetTask
    {
        //~ Constructors -------------------------------------------------------

        ExportTask (Sheet sheet,
                    Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void doit (Collection<SystemInfo> unused)
            throws StepException
        {
            ScoreManager.getInstance()
                        .export(sheet.getScore(), null, null);
        }
    }

    //-----------------//
    // HorizontalsTask //
    //-----------------//
    /**
     * Step to retrieve all horizontal dashes
     */
    private static class HorizontalsTask
        extends SheetTask
    {
        //~ Constructors -------------------------------------------------------

        HorizontalsTask (Sheet sheet,
                         Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void doit (Collection<SystemInfo> unused)
            throws StepException
        {
            sheet.setHorizontalsBuilder(new HorizontalsBuilder(sheet));
            sheet.setHorizontals(sheet.getHorizontalsBuilder().buildInfo());
        }
    }

    //-----------//
    // LinesTask //
    //-----------//
    /**
     * Step to retrieve all staff lines, and remove them from the picture
     */
    private static class LinesTask
        extends SheetTask
    {
        //~ Constructors -------------------------------------------------------

        LinesTask (Sheet sheet,
                   Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void doit (Collection<SystemInfo> unused)
            throws StepException
        {
            //logger.info("*** Using StavesFuzzyBuilder ***");
            sheet.setStavesBuilder(new LinesBuilder(sheet));
            ///sheet.setStavesBuilder(new StavesFuzzyBuilder(sheet));
            sheet.getStavesBuilder()
                 .buildInfo();
            sheet.setStaves(sheet.getStavesBuilder().getStaves());
        }
    }

    //----------//
    // LoadTask //
    //----------//
    /**
     * Step to (re)load sheet picture.
     */
    private static class LoadTask
        extends SheetTask
    {
        //~ Instance fields ----------------------------------------------------

        private Picture picture;

        //~ Constructors -------------------------------------------------------

        LoadTask (Sheet sheet,
                  Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void doit (Collection<SystemInfo> unused)
            throws StepException
        {
            if (sheet.getImageFile() != null) {
                File imageFile = sheet.getImageFile();

                try {
                    picture = new Picture(imageFile);
                    picture.setMaxForeground(sheet.getMaxForeground());
                    sheet.setPicture(picture);

                    sheet.getBench()
                         .recordImageDimension(
                        picture.getWidth(),
                        picture.getHeight());
                } catch (FileNotFoundException ex) {
                    logger.warning("Cannot find file " + imageFile);
                    throw new StepException(ex);
                } catch (IOException ex) {
                    logger.warning("Input error on file " + imageFile);
                    throw new StepException(ex);
                } catch (ImageFormatException ex) {
                    String msg = "Unsupported image format in file " +
                                 imageFile + "\n" + ex.getMessage() +
                                 "\nPlease use gray scale with 256 values";

                    if (Main.getGui() != null) {
                        Main.getGui()
                            .displayWarning(msg);
                    } else {
                        logger.warning(msg);
                    }

                    throw new StepException(ex);
                } catch (Exception ex) {
                    logger.warning("Exception", ex);
                    throw new StepException(ex);
                }
            } else if (sheet.getImageUrl() != null) {
                URL imageUrl = sheet.getImageUrl();

                try {
                    picture = new Picture(imageUrl);
                    picture.setMaxForeground(sheet.getMaxForeground());
                    sheet.setPicture(picture);

                    sheet.getBench()
                         .recordImageDimension(
                        picture.getWidth(),
                        picture.getHeight());
                } catch (IOException ex) {
                    logger.warning("Input error on URL " + imageUrl);
                    throw new StepException(ex);
                } catch (ImageFormatException ex) {
                    String msg = "Unsupported image format in URL " + imageUrl +
                                 "\n" + ex.getMessage() +
                                 "\nPlease use gray scale with 256 values";

                    if (Main.getGui() != null) {
                        Main.getGui()
                            .displayWarning(msg);
                    } else {
                        logger.warning(msg);
                    }

                    throw new StepException(ex);
                } catch (Exception ex) {
                    logger.warning("Exception", ex);
                    throw new StepException(ex);
                }
            }
        }

        @Override
        public void done ()
        {
            // Remember (even across runs) the parent directory
            if (sheet.getImageFile() != null) {
                SheetsManager.getInstance()
                             .setDefaultSheetDirectory(
                    sheet.getImageFile().getParent());
            }

            // Insert in sheet history
            SheetsManager.getInstance()
                         .getHistory()
                         .add(sheet.getPath());

            super.done();
        }
    }

    //--------------//
    // MeasuresTask //
    //--------------//
    /**
     * Step to retrieve measures
     */
    private static class MeasuresTask
        extends SystemTask
    {
        //~ Constructors -------------------------------------------------------

        MeasuresTask (Sheet sheet,
                      Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void displayUI ()
        {
            Main.getGui().scoreController.setScoreEditor(sheet.getScore());
        }

        @Override
        public void doSystem (SystemInfo system)
            throws StepException
        {
            if (Main.getGui() != null) {
                system.getSheet()
                      .getErrorsEditor()
                      .clearSystem(step, system.getId());
            }

            system.buildMeasures(); // For Measures
        }

        @Override
        protected void doEpilog (Collection<SystemInfo> systems)
            throws StepException
        {
            if (logger.isFineEnabled()) {
                logger.fine(step + " doEpilog");
            }

            // Update score internal data
            sheet.getScore()
                 .accept(new ScoreFixer());
            sheet.getScore()
                 .dumpMeasureCounts(null);
        }
    }

    //---------------//
    // MidiWriteTask //
    //---------------//
    /**
     * Step to write the output MIDI file
     */
    private static class MidiWriteTask
        extends SheetTask
    {
        //~ Constructors -------------------------------------------------------

        MidiWriteTask (Sheet sheet,
                       Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void doit (Collection<SystemInfo> unused)
            throws StepException
        {
            try {
                ScoreManager.getInstance()
                            .midiWrite(sheet.getScore(), null);
            } catch (Exception ex) {
                logger.warning("Midi write failed", ex);
            }
        }
    }

    //----------//
    // PlayTask //
    //----------//
    /**
     * Step to play the whole score, using a MIDI sequencer
     */
    private static class PlayTask
        extends SheetTask
    {
        //~ Constructors -------------------------------------------------------

        PlayTask (Sheet sheet,
                  Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void doit (Collection<SystemInfo> unused)
            throws StepException
        {
            new MidiActions.PlayTask(sheet.getScore(), null).execute();
        }
    }

    //-----------//
    // PrintTask //
    //-----------//
    /**
     * Step to print the whole score, to a PDF output
     */
    private static class PrintTask
        extends SheetTask
    {
        //~ Constructors -------------------------------------------------------

        PrintTask (Sheet sheet,
                   Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void doit (Collection<SystemInfo> unused)
            throws StepException
        {
            ScoreManager.getInstance()
                        .pdfWrite(sheet.getScore(), null);
        }
    }

    //-----------//
    // ScaleTask //
    //-----------//
    /**
     * Step to determine the main scale of the sheet. The scale is the mean
     * distance, in pixels, between two consecutive staff lines. This is based
     * on the population of vertical runs, since most frequent foreground runs
     * come from staff lines, and most frequent background runs come from inter
     * staff lines.
     */
    private static class ScaleTask
        extends SheetTask
    {
        //~ Constructors -------------------------------------------------------

        ScaleTask (Sheet sheet,
                   Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void doit (Collection<SystemInfo> unused)
            throws StepException
        {
            Scale scale = new Scale(sheet);
            sheet.setScale(scale);
        }
    }

    //-------------//
    // SystemsTask //
    //-------------//
    /**
     * Step to retrieve bar sticks, and thus systems
     */
    private static class SystemsTask
        extends SheetTask
    {
        //~ Constructors -------------------------------------------------------

        SystemsTask (Sheet sheet,
                     Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void displayUI ()
        {
            Main.getGui().scoreController.setScoreEditor(sheet.getScore());
        }

        @Override
        public void doit (Collection<SystemInfo> systems)
            throws StepException
        {
            sheet.createSystemsBuilder();
            sheet.getSystemsBuilder()
                 .buildSystems();
        }
    }

    //--------------//
    // PatternsTask //
    //--------------//
    /**
     * Step to run processing of specific sheet glyph patterns arounds clefs,
     * sharps, naturals, stems, slurs, etc
     */
    private class PatternsTask
        extends SystemTask
    {
        //~ Instance fields ----------------------------------------------------

        private boolean firstTime = true;

        //~ Constructors -------------------------------------------------------

        PatternsTask (Sheet sheet,
                      Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void displayUI ()
        {
            getTask(SYMBOLS)
                .displayUI();
            getTask(VERTICALS)
                .displayUI();
        }

        @Override
        public void doSystem (SystemInfo system)
            throws StepException
        {
            system.getSentences()
                  .clear();

            for (int iter = 1;
                 iter <= constants.MaxPatternsIterations.getValue(); iter++) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "System#" + system.getId() + " patterns iter #" + iter);
                }

                if (Main.getGui() != null) {
                    system.getSheet()
                          .getErrorsEditor()
                          .clearSystem(step, system.getId());
                }

                if (!system.runPatterns()) {
                    return; // No more progress made
                }
            }
        }

        @Override
        protected void doEpilog (Collection<SystemInfo> systems)
            throws StepException
        {
            // For the very first time, we reperform the VERTICALS step
            // The other times will be triggered by glyph deassignments which
            // reperform from VERTICALS.
            // TODO: This is wrong, VERTICALS is not triggered by programmatic
            // deassignments...
            if (firstTime) {
                firstTime = false;

                // Reperform verticals once
                // The range of steps to re-perform
                EnumSet<Step> stepRange = EnumSet.range(Step.VERTICALS, step);

                try {
                    Step.doStepRange(stepRange, sheet, systems);
                } catch (Exception ex) {
                    logger.warning("Error in re-processing from " + this, ex);
                }
            }
        }
    }

    //-----------//
    // ScoreTask //
    //-----------//
    /**
     * Step to translate recognized glyphs into score items
     */
    private class ScoreTask
        extends SystemTask
    {
        //~ Constructors -------------------------------------------------------

        ScoreTask (Sheet sheet,
                   Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void displayUI ()
        {
            // Symbols may have been updated (beam hooks for example)
            getTask(SYMBOLS)
                .displayUI();
        }

        @Override
        public void doEpilog (Collection<SystemInfo> systems)
            throws StepException
        {
            // Final cross-system translation tasks
            if (systems == null) {
                systems = sheet.getSystems();
            }

            if (!systems.isEmpty()) {
                systems.iterator()
                       .next()
                       .translateFinal();
            }
        }

        @Override
        public void doSystem (final SystemInfo system)
            throws StepException
        {
            final int            iterMax = constants.maxScoreIterations.getValue();
            final ScoreSystem    scoreSystem = system.getScoreSystem();
            final WrappedBoolean modified = new WrappedBoolean(true);

            // Purge system of non-active glyphs
            system.removeInactiveGlyphs();

            for (int iter = 1; modified.isSet() && (iter <= iterMax); iter++) {
                modified.set(false);

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "System#" + system.getId() + " translation iter #" +
                        iter);
                }

                // Clear errors for this system only (and this step)
                if (Main.getGui() != null) {
                    system.getSheet()
                          .getErrorsEditor()
                          .clearSystem(step, system.getId());
                }

                // Cleanup the system, staves, measures, barlines, ...
                // and clear glyph (& sentence) translations
                scoreSystem.accept(new ScoreCleaner());

                // Real translation
                system.translateSystem();

                /** Final checks at system level */
                scoreSystem.acceptChildren(new ScoreChecker(modified));
            }

            // Additional measure checking
            try {
                Measure.checkPartialMeasures(scoreSystem);
            } catch (Exception ex) {
                logger.warning(
                    "Error checking partial measures in " + system,
                    ex);
            }
        }
    }

    //----------//
    // SkewTask //
    //----------//
    /**
     * Step to determine the general slope of the sheet, still based on
     * pseudo-horizontal (staff) lines. If the absolute value of the computed
     * slope is above a maximum threshold, then the image as a whole is
     * "deskewed", since this significantly eases the subsequent processing.
     * From this step on, we'll play only with the deskewed image.
     */
    private static class SkewTask
        extends SheetTask
    {
        //~ Constructors -------------------------------------------------------

        SkewTask (Sheet sheet,
                  Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void doit (Collection<SystemInfo> unused)
            throws StepException
        {
            sheet.setSkewBuilder(new SkewBuilder(sheet));
            sheet.setSkew(sheet.getSkewBuilder().buildInfo());
            sheet.getBench()
                 .recordSkew(sheet.getSkew().angle());

            // If rotated, rescale the sheet
            if (sheet.getPicture()
                     .isRotated()) {
                sheet.getSheetSteps()
                     .getTask(Step.SCALE)
                     .doit(unused);
            }
        }
    }

    //-------------//
    // SymbolsTask //
    //-------------//
    /**
     * Step to process all glyphs, built with connected sections from the
     * current collection of non-recognized sections.
     */
    private static class SymbolsTask
        extends SystemTask
    {
        //~ Constructors -------------------------------------------------------

        SymbolsTask (Sheet sheet,
                     Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void displayUI ()
        {
            sheet.getSymbolsEditor()
                 .refresh();

            // Update glyph board if needed (to see OCR'ed data)
            SelectionService service = sheet.getVerticalLag()
                                            .getSelectionService();
            GlyphEvent       glyphEvent = (GlyphEvent) service.getLastEvent(
                GlyphEvent.class);

            if (glyphEvent != null) {
                service.publish(glyphEvent);
            }
        }

        @Override
        public void doSystem (SystemInfo system)
            throws StepException
        {
            if (Main.getGui() != null) {
                system.getSheet()
                      .getErrorsEditor()
                      .clearSystem(step, system.getId());
            }

            system.inspectGlyphs(GlyphInspector.getSymbolMaxDoubt());
        }

        @Override
        protected void doEpilog (Collection<SystemInfo> systems)
            throws StepException
        {
            if (Main.getGui() != null) {
                sheet.createSymbolsControllerAndEditor();
            }
        }
    }

    //---------------//
    // VerticalsTask //
    //---------------//
    /**
     * Step to extract vertical stick as Stems (or vertical Endings), and
     * recognize newly segmented leaves, since sections belonging to stems are
     * properly assigned.
     */
    private static class VerticalsTask
        extends SystemTask
    {
        //~ Constructors -------------------------------------------------------

        VerticalsTask (Sheet sheet,
                       Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void displayUI ()
        {
            // Create verticals display
            sheet.getVerticalsController()
                 .refresh();
        }

        @Override
        public void doSystem (SystemInfo system)
            throws StepException
        {
            if (Main.getGui() != null) {
                system.getSheet()
                      .getErrorsEditor()
                      .clearSystem(step, system.getId());
            }

            system.retrieveVerticals();
        }

        @Override
        protected void doEpilog (Collection<SystemInfo> systems)
            throws StepException
        {
            sheet.createVerticalsController();
        }
    }
}
