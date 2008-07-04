//----------------------------------------------------------------------------//
//                                                                            //
//                            S h e e t S t e p s                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.step;

import omr.Main;

import omr.glyph.Glyph;
import omr.glyph.GlyphInspector;
import omr.glyph.Shape;
import omr.glyph.SlurGlyph;

import omr.score.ui.ScoreActions;
import omr.score.visitor.ScoreChecker;

import omr.sheet.HorizontalsBuilder;
import omr.sheet.ImageFormatException;
import omr.sheet.LinesBuilder;
import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SkewBuilder;
import omr.sheet.SystemInfo;
import static omr.step.Step.*;

import omr.util.Logger;
import omr.util.OmrExecutors;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.*;

/**
 * Class <code>SheetSteps</code> handles the actual progress of steps for a
 * given sheet instance.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SheetSteps
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetSteps.class);

    //~ Instance fields --------------------------------------------------------

    /** The related sheet instance */
    private final Sheet sheet;

    /** The tasks that relate to each step */
    private final Map<Step, SheetTask> tasks = new LinkedHashMap<Step, SheetTask>();

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
        tasks.put(BARS, new BarsTask(sheet, BARS));
        tasks.put(SYMBOLS, new SymbolsTask(sheet, SYMBOLS));
        tasks.put(VERTICALS, new VerticalsTask(sheet, VERTICALS));
        tasks.put(LEAVES, new LeavesTask(sheet, LEAVES));
        tasks.put(CLEANUP, new CleanupTask(sheet, CLEANUP));
        tasks.put(SCORE, new ScoreTask(sheet, SCORE));
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // isDone //
    //--------//
    /**
     * Convenient method to check whether a given step has been done (or simply
     * started)
     * @param step the provided step
     * @return true if step has been done / started
     */
    public boolean isDone (Step step)
    {
        return getTask(step)
                   .isDone();
    }

    //-----------//
    // getResult //
    //-----------//
    /**
     * Convenient method to make sure the result of a given step is
     * available
     * @param step the provided step
     * @exception StepException if processing goes wrong
     */
    public void getResult (Step step)
        throws StepException
    {
        getTask(step)
            .getResult();
    }

    //-----------------//
    // getSystemResult //
    //-----------------//
    /**
     * Convenient method to make sure the result of a given step on a given
     * system is available
     * @param step the provided step
     * @param system the provided system
     * @exception StepException if processing goes wrong
     */
    public void getSystemResult (Step       step,
                                 SystemInfo system)
        throws StepException
    {
        SystemTask systemTask = (SystemTask) getTask(step);
        systemTask.getResult(system);
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

    //----------//
    // doSystem //
    //----------//
    /**
     * Convenient method to launch the processing of a given system in a
     * given step
     * @param step the provided step
     * @param system the provided system
     * @exception StepException if processing goes wrong
     */
    public void doSystem (Step       step,
                          SystemInfo system)
        throws StepException
    {
        SheetTask task = getTask(step);

        if (task instanceof SystemTask) {
            SystemTask systemTask = (SystemTask) task;
            systemTask.doSystem(system);
        } else {
            logger.severe("Illegal system processing from step " + step);
        }
    }

    //------//
    // doit //
    //------//
    /**
     * Convenient method to launch the processing of a given step
     * @param step the provided step
     * @exception StepException if processing goes wrong
     */
    public void doit (Step step)
        throws StepException
    {
        getTask(step)
            .doit();
    }

    //-----------------//
    // updateLastSteps //
    //-----------------//
    /**
     * Following the modification of a collection of glyphs, this method
     * launches the re-processing of the steps following VERTICALS on the
     * systems impacted by the modifications
     *
     * @param glyphs the modified glyphs
     * @param shapes the previous shapes of these glyphs
     * @param imposed true if update must occur, else depends on current mode
     */
    public void updateLastSteps (Collection<Glyph> glyphs,
                                 Collection<Shape> shapes,
                                 boolean           imposed)
    {
        // Check whether the update must really be done
        if (!imposed && !ScoreActions.getInstance()
                                     .isRebuildAllowed()) {
            return;
        }

        // Determine impacted systems, from the collection of modified glyphs
        final SortedSet<SystemInfo> impactedSystems = (glyphs != null)
                                                      ? sheet.getImpactedSystems(
            glyphs,
            shapes) : new TreeSet<SystemInfo>(sheet.getSystems());

        if (logger.isFineEnabled()) {
            logger.fine(
                "Score rebuild launched on" +
                SystemInfo.toString(impactedSystems));
        }

        // Prepare the reprocessing of the impacted systems
        Collection<Callable<Void>> work = new ArrayList<Callable<Void>>();

        for (SystemInfo info : impactedSystems) {
            final SystemInfo system = info;
            work.add(
                new Callable<Void>() {
                        public Void call ()
                            throws Exception
                        {
                            system.getScoreSystem()
                                  .cleanupNode();

                            try {
                                if (isDone(LEAVES)) {
                                    doSystem(LEAVES, system);
                                }

                                if (isDone(CLEANUP)) {
                                    doSystem(CLEANUP, system);
                                }

                                if (isDone(SCORE)) {
                                    doSystem(SCORE, system);
                                }
                            } catch (StepException ex) {
                                ex.printStackTrace();
                            }

                            return null;
                        }
                    });
        }

        try {
            OmrExecutors.getLowExecutor()
                        .invokeAll(work);

            // Final cross-system translation tasks
            if (isDone(SCORE)) {
                Runnable finalWork = new Runnable() {
                    public void run ()
                    {
                        SystemInfo firstSystem = (impactedSystems.size() > 0)
                                                 ? impactedSystems.first() : null;
                        sheet.getScoreBuilder()
                             .buildFinal(firstSystem);
                    }
                };

                OmrExecutors.getLowExecutor()
                            .submit(finalWork)
                            .get();
            }

            // Always refresh views if any
            if (Main.getGui() != null) {
                SwingUtilities.invokeLater(
                    new Runnable() {
                            public void run ()
                            {
                                if (isDone(VERTICALS)) {
                                    getTask(VERTICALS)
                                        .displayUI();
                                }

                                if (isDone(SYMBOLS)) {
                                    getTask(SYMBOLS)
                                        .displayUI();
                                }

                                // Kludge, to put the Glyphs tab on top of all others.
                                sheet.getAssembly()
                                     .selectTab("Glyphs");
                            }
                        });
            }
        } catch (Exception ex) {
            logger.warning("Last steps failed", ex);
        }
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

    //----------//
    // BarsTask //
    //----------//
    /**
     * Step to retrieve barlines, and thus systems and measures
     */
    private static class BarsTask
        extends SheetTask
    {
        //~ Constructors -------------------------------------------------------

        BarsTask (Sheet sheet,
                  Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void doit ()
            throws StepException
        {
            sheet.getBarsBuilder()
                 .buildInfo();
            done();

            // Force score view creation if UI is present
            if (Main.getGui() != null) {
                Main.getGui().scoreController.setScoreView(sheet.getScore());
            }
        }
    }

    //-------------//
    // CleanupTask //
    //-------------//
    /**
     * Step to clean up undue constructions, such as wrong stems..
     */
    private class CleanupTask
        extends SystemTask
    {
        //~ Constructors -------------------------------------------------------

        CleanupTask (Sheet sheet,
                     Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void displayUI ()
        {
            if (logger.isFineEnabled()) {
                logger.fine("CLEANUP displayUI");
            }

            getTask(SYMBOLS)
                .displayUI();
            getTask(VERTICALS)
                .displayUI();
        }

        @Override
        public void doSystem (SystemInfo system)
            throws StepException
        {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "CLEANUP doSystem #" + system.getScoreSystem().getId());
            }

            getSystemResult(LEAVES, system);
            sheet.getGlyphInspector()
                 .verifyStems(system);
            sheet.getGlyphsBuilder()
                 .removeSystemInactives(system);
            SlurGlyph.verifySlurs(system);
            sheet.getGlyphsBuilder()
                 .extractNewSystemGlyphs(system);
            sheet.getGlyphInspector()
                 .processGlyphs(system, GlyphInspector.getCleanupMaxDoubt());
            system.retrieveTextGlyphs();
            done(system);
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
        public void doit ()
            throws StepException
        {
            sheet.setHorizontalsBuilder(new HorizontalsBuilder(sheet));
            sheet.setHorizontals(sheet.getHorizontalsBuilder().buildInfo());
            done();
        }
    }

    //------------//
    // LeavesTask //
    //------------//
    /**
     * Step to extract newly segmented leaves, since sections belonging to stems
     * are properly assigned.
     */
    private class LeavesTask
        extends SystemTask
    {
        //~ Constructors -------------------------------------------------------

        LeavesTask (Sheet sheet,
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
            if (logger.isFineEnabled()) {
                logger.fine(
                    "LEAVES doSystem #" + system.getScoreSystem().getId());
            }

            // Trigger previous processing for this system, if needed
            getSystemResult(VERTICALS, system);
            // Processing for this step
            sheet.getGlyphInspector()
                 .processGlyphs(system, GlyphInspector.getLeafMaxDoubt());
            done(system);
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
        public void doit ()
            throws StepException
        {
            sheet.setLinesBuilder(new LinesBuilder(sheet));
            sheet.setStaves(sheet.getLinesBuilder().getStaves());
            done();
        }
    }

    //----------//
    // LoadTask //
    //----------//
    /**
     * Step to (re)load sheet picture. A brand new sheet is created with the
     * provided image file as parameter.
     */
    private static class LoadTask
        extends SheetTask
    {
        //~ Constructors -------------------------------------------------------

        LoadTask (Sheet sheet,
                  Step  step)
        {
            super(sheet, step);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void doit ()
            throws StepException
        {
            File imageFile = sheet.getImageFile();

            try {
                Picture picture = new Picture(imageFile);
                done();
                sheet.setPicture(picture);
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find file " + imageFile);
                throw new StepException(ex);
            } catch (IOException ex) {
                logger.warning("Input error on file " + imageFile);
                throw new StepException(ex);
            } catch (ImageFormatException ex) {
                logger.warning("Unsupported image format in file " + imageFile);
                logger.warning(ex.getMessage());

                if (Main.getGui() != null) {
                    Main.getGui()
                        .displayWarning(
                        "<B>" + ex.getMessage() + "</B><BR>" +
                        "Please use grey scale with 256 values");
                }

                throw new StepException(ex);
            } catch (Exception ex) {
                logger.warning("Exception", ex);
                throw new StepException(ex);
            }
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
        public void doit ()
            throws StepException
        {
            Scale scale = new Scale(sheet);
            sheet.setScale(scale);
            done();
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
            if (logger.isFineEnabled()) {
                logger.fine("SCORE displayUI");
            }

            // Make sure symbols & verticals are displayed
            getTask(SYMBOLS)
                .displayUI();
            getTask(VERTICALS)
                .displayUI();

            // Kludge, to put the Glyphs tab on top of all others.
            sheet.getAssembly()
                 .selectTab("Glyphs");
        }

        @Override
        public void doFinal ()
            throws StepException
        {
            if (logger.isFineEnabled()) {
                logger.fine("SCORE final");
            }

            // Final cross-system translation tasks
            sheet.getScoreBuilder()
                 .buildFinal(null);
        }

        @Override
        public void doSystem (SystemInfo system)
            throws StepException
        {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "SCORE doSystem #" + system.getScoreSystem().getId());
            }

            omr.score.entity.System scoreSystem = system.getScoreSystem();
            getSystemResult(CLEANUP, system);
            sheet.getScoreBuilder()
                 .buildSystem(scoreSystem);
            scoreSystem.acceptChildren(new ScoreChecker());
            done(system);
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
        public void doit ()
            throws StepException
        {
            sheet.setSkewBuilder(new SkewBuilder(sheet));
            sheet.setSkew(sheet.getSkewBuilder().buildInfo());
            done();
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
            if (logger.isFineEnabled()) {
                logger.fine("SYMBOLS displayUI");
            }

            sheet.getSymbolsEditor()
                 .refresh();
        }

        @Override
        public void doSystem (SystemInfo system)
        {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "SYMBOLS doSystem #" + system.getScoreSystem().getId());
            }

            sheet.getGlyphInspector()
                 .processGlyphs(system, GlyphInspector.getSymbolMaxDoubt());
            done(system);
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
    private class VerticalsTask
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
            if (logger.isFineEnabled()) {
                logger.fine("VERTICALS displayUI");
            }

            // Create verticals display
            sheet.getVerticalsBuilder()
                 .refresh();
        }

        @Override
        public void doSystem (SystemInfo system)
            throws StepException
        {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "VERTICALS doSystem #" + system.getScoreSystem().getId());
            }

            getSystemResult(SYMBOLS, system);
            sheet.getVerticalsBuilder()
                 .retrieveSystemVerticals(system);
            done(system);
        }
    }
}
