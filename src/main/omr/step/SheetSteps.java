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
import omr.glyph.Shape;

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
import omr.step.StepException;

import omr.util.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

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
    private final SortedMap<Step, SheetTask> tasks = new TreeMap<Step, SheetTask>();

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SheetSteps //
    //------------//
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
        SheetTask task = getTask(step);

        if (task instanceof SystemTask) {
            SystemTask systemTask = (SystemTask) task;
            systemTask.getResult(system);
        }
    }

    //--------//
    // isDone //
    //--------//
    /**
     * Convenient method to check whether a given step has been done /
     * started
     * @param step the provided step
     * @return true if step has been done / started
     */
    public boolean isDone (Step step)
    {
        return getTask(step)
                   .isDone();
    }

    //-----------------//
    // updateLastSteps //
    //-----------------//
    public void updateLastSteps (Collection<Glyph> glyphs,
                                 Collection<Shape> shapes)
    {
        // Determine impacted systems, from the collection of modified glyphs
        Collection<SystemInfo> impactedSystems = sheet.getImpactedSystems(
            glyphs,
            shapes);

        if (logger.isFineEnabled()) {
            logger.fine(impactedSystems.size() + " Impacted system(s)");
        }

        for (SystemInfo info : impactedSystems) {
            final SystemInfo system = info;

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
        }

        // Final cross-system translation tasks
        sheet.getScoreBuilder()
             .buildFinal();

        // Always refresh sheet views
        sheet.getSymbolsEditor()
             .refresh();

        if (isDone(VERTICALS)) {
            sheet.getVerticalsBuilder()
                 .refresh();
        }
    }

    //---------//
    // getTask //
    //---------//
    /**
     * Give access to the step task
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
    class BarsTask
        extends SheetTask
    {
        BarsTask (Sheet sheet,
                  Step  step)
        {
            super(sheet, step);
        }

        public void doit ()
            throws StepException
        {
            sheet.getBarsBuilder()
                 .buildInfo();
            done();

            // Force score view creation if UI is present
            if (Main.getGui() != null) {
                Main.getGui().scoreController.setScoreView(
                    SheetSteps.this.sheet.getScore());
            }
        }
    }

    //-------------//
    // CleanupTask //
    //-------------//
    /**
     * Step to clean up undue constructions, such as wrong stems..
     */
    class CleanupTask
        extends SystemTask
    {
        CleanupTask (Sheet sheet,
                     Step  step)
        {
            super(sheet, step);
        }

        public void displayUI ()
        {
            if (logger.isFineEnabled()) {
                logger.fine("CLEANUP displayUI");
            }

            sheet.getSymbolsEditor()
                 .refresh();
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
            sheet.getGlyphInspector()
                 .verifySlurs(system);
            sheet.getGlyphInspector()
                 .processGlyphs(
                system,
                sheet.getGlyphInspector().getCleanupMaxDoubt());
            done(system);
        }
    }

    //-----------------//
    // HorizontalsTask //
    //-----------------//
    /**
     * Step to retrieve all horizontal dashes
     */
    class HorizontalsTask
        extends SheetTask
    {
        HorizontalsTask (Sheet sheet,
                         Step  step)
        {
            super(sheet, step);
        }

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
    class LeavesTask
        extends SystemTask
    {
        LeavesTask (Sheet sheet,
                    Step  step)
        {
            super(sheet, step);
        }

        public void displayUI ()
        {
            sheet.getSymbolsEditor()
                 .refresh();
        }

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
                 .processGlyphs(
                system,
                sheet.getGlyphInspector().getLeafMaxDoubt());
            done(system);
        }
    }

    //-----------//
    // LinesTask //
    //-----------//
    /**
     * Step to retrieve all staff lines, and remove them from the picture
     */
    class LinesTask
        extends SheetTask
    {
        LinesTask (Sheet sheet,
                   Step  step)
        {
            super(sheet, step);
        }

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
     *
     * <p>The result of this step (a Picture) is <b>transient</b>, thus not
     * saved nor restored, since a picture is too costly. If picture is indeed
     * needed, then it is explicitly reloaded from the image file through the
     * <b>getPicture</b> method.
     */
    class LoadTask
        extends SheetTask
    {
        LoadTask (Sheet sheet,
                  Step  step)
        {
            super(sheet, step);
        }

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
    class ScaleTask
        extends SheetTask
    {
        ScaleTask (Sheet sheet,
                   Step  step)
        {
            super(sheet, step);
        }

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
    class ScoreTask
        extends SystemTask
    {
        ScoreTask (Sheet sheet,
                   Step  step)
        {
            super(sheet, step);
        }

        public void displayUI ()
        {
            // Make sure the verticals are displayed too
            getTask(VERTICALS)
                .displayUI();

            if (logger.isFineEnabled()) {
                logger.fine("SCORE displayUI");
            }

            sheet.getSymbolsEditor()
                 .refresh();
        }

        public void doFinal ()
            throws StepException
        {
            if (logger.isFineEnabled()) {
                logger.fine("SCORE final");
            }

            // Final cross-system translation tasks
            sheet.getScoreBuilder()
                 .buildFinal();
        }

        @Override
        public void doSystem (SystemInfo system)
            throws StepException
        {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "SCORE doSystem #" + system.getScoreSystem().getId());
            }

            omr.score.System scoreSystem = system.getScoreSystem();
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
     * The resulting (deskewed) image is stored on disk, and reloaded in place
     * of the original (skewed) image. From this step on, we'll play only with
     * the deskewed image.
     */
    class SkewTask
        extends SheetTask
    {
        SkewTask (Sheet sheet,
                  Step  step)
        {
            super(sheet, step);
        }

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
    class SymbolsTask
        extends SystemTask
    {
        SymbolsTask (Sheet sheet,
                     Step  step)
        {
            super(sheet, step);
        }

        public void displayUI ()
        {
            if (logger.isFineEnabled()) {
                logger.fine("SYMBOLS displayUI");
            }

            sheet.getSymbolsEditor()
                 .refresh();
        }

        public void doSystem (SystemInfo system)
        {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "SYMBOLS doSystem #" + system.getScoreSystem().getId());
            }

            sheet.getGlyphInspector()
                 .processGlyphs(
                system,
                sheet.getGlyphInspector().getSymbolMaxDoubt());
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
    class VerticalsTask
        extends SystemTask
    {
        VerticalsTask (Sheet sheet,
                       Step  step)
        {
            super(sheet, step);
        }

        public void displayUI ()
        {
            if (logger.isFineEnabled()) {
                logger.fine("VERTICALS displayUI");
            }

            // Create verticals display
            sheet.getVerticalsBuilder()
                 .refresh();
        }

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
