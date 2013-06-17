//----------------------------------------------------------------------------//
//                                                                            //
//                         S c o r e s M a n a g e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.Main;
import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.ui.SheetPdfOutput;

import omr.script.ScriptActions;

import omr.util.NameSet;

import org.jdesktop.application.Application.ExitListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code ScoresManager} is a singleton which provides
 * administrative features for score instances.
 * <p>It handles the collection of all loaded score instances.</p>
 * <p>It handles the history of scores previously loaded.</p>
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 */
public class ScoresManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(ScoresManager.class);

    /** The extension used for score output files: {@value} */
    public static final String SCORE_EXTENSION = ".xml";

    /** The extension used for score bench files: {@value} */
    public static final String BENCH_EXTENSION = ".bench.properties";

    /** The single instance of this class */
    private static volatile ScoresManager INSTANCE;

    //~ Instance fields --------------------------------------------------------
    /** Instances of Score */
    private List<Score> instances = new ArrayList<>();

    /** Image file history (filled only when images are successfully loaded) */
    private NameSet history;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // ScoresManager //
    //---------------//
    /**
     * Private constructor
     */
    private ScoresManager ()
    {
        if (Main.getGui() != null) {
            Main.getGui().addExitListener(
                    new ExitListener()
            {
                @Override
                public boolean canExit (EventObject e)
                {
                    // Are all scripts stored (or explicitly ignored)?
                    for (Score score : instances) {
                        if (!ScriptActions.checkStored(
                                score.getScript())) {
                            return false;
                        }
                    }

                    return true;
                }

                @Override
                public void willExit (EventObject e)
                {
                    // Close all sheets, to record their bench data
                    closeAllScores();
                }
            });
        }
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // addInstance //
    //-------------//
    /**
     * Insert this new score in the set of score instances.
     *
     * @param score the score to insert
     */
    public synchronized void addInstance (Score score)
    {
        logger.debug("addInstance {}", score);

        // Remove duplicate if any
        for (Iterator<Score> it = instances.iterator(); it.hasNext();) {
            Score s = it.next();
            String path = s.getImagePath();

            if (path.equals(score.getImagePath())) {
                logger.debug("Removing duplicate {}", s);
                it.remove();
                s.close();

                break;
            }
        }

        // Insert new score instance
        instances.add(score);
    }

    //--------//
    // export //
    //--------//
    /**
     * Export a score using the partwise structure of MusicXML to the
     * default file for the provided score.
     *
     * @param score the score to export
     */
    public void export (Score score)
    {
        export(score, score.getExportFile(), null);
    }

    //--------//
    // export //
    //--------//
    /**
     * Export a score using the partwise structure of MusicXML to the
     * provided file.
     *
     * @param score           the score to export
     * @param file            the xml file to write, or null
     * @param injectSignature should we inject our signature?
     */
    public void export (Score score,
                        File file,
                        Boolean injectSignature)
    {
        if (Main.getExportPath() != null) {
            File path = new File(Main.getExportPath());

            if (path.isDirectory()) {
                file = getActualFile(file, getDefaultExportFile(path, score));
            } else {
                file = getActualFile(file, path);
            }
        } else {
            file = getActualFile(file, getDefaultExportFile(null, score));
        }

        // Actually export the score material
        try {
            ScoreExporter exporter = new ScoreExporter(score);

            if (injectSignature != null) {
                exporter.export(file, injectSignature);
            } else {
                exporter.export(
                        file,
                        constants.defaultInjectSignature.getValue());
            }

            logger.info("Score exported to {}", file);

            // Remember (even across runs) the selected directory
            constants.defaultExportDirectory.setValue(file.getParent());

            // Remember the file in the score itself
            score.setExportFile(file);
        } catch (Exception ex) {
            logger.warn("Error storing score to " + file, ex);
        }
    }

    //---------------------//
    // getDefaultBenchFile //
    //---------------------//
    /**
     * Report the file to which the bench data would be written by default.
     *
     * @param folder the target folder if any
     * @param score  the score to export
     * @return the default file
     */
    public File getDefaultBenchFile (File folder,
                                     Score score)
    {
        String child = score.getRadix() + BENCH_EXTENSION;

        if (folder != null) {
            return new File(folder, child);
        } else {
            return new File(constants.defaultBenchDirectory.getValue(), child);
        }
    }

    //---------------------------//
    // getDefaultDewarpDirectory //
    //---------------------------//
    /**
     * Report the directory to which dewarped images would be saved by default.
     *
     * @return the default file
     */
    public File getDefaultDewarpDirectory ()
    {
        return new File(constants.defaultDewarpDirectory.getValue());
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class.
     *
     * @return the single instance
     */
    public static ScoresManager getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new ScoresManager();
        }

        return INSTANCE;
    }

    //--------------//
    // isMultiScore //
    //--------------//
    /**
     * Report whether we are handling more than one score.
     *
     * @return true if more than one score
     */
    public static boolean isMultiScore ()
    {
        return getInstance().instances.size() > 1;
    }

    //----------------------//
    // getDefaultExportFile //
    //----------------------//
    /**
     * Report the file to which the score would be written by default.
     *
     * @param folder the target folder if any
     * @param score  the score to export
     * @return the default file
     */
    public File getDefaultExportFile (File folder,
                                      Score score)
    {
        if (score.getExportFile() != null) {
            return score.getExportFile();
        }

        String child = score.getRadix() + SCORE_EXTENSION;

        if (folder != null) {
            return new File(folder, child);
        } else {
            return new File(constants.defaultExportDirectory.getValue(), child);
        }
    }

    //--------------------------//
    // getDefaultInputDirectory //
    //--------------------------//
    /**
     * Report the directory where images should be found.
     *
     * @return the latest image directory
     */
    public String getDefaultInputDirectory ()
    {
        return constants.defaultInputDirectory.getValue();
    }

    //---------------------//
    // getDefaultPrintFile //
    //---------------------//
    /**
     * Report the file to which the sheet PDF data would be written by default.
     *
     * @param folder the target folder if any
     * @param score  the score to export
     * @return the default file
     */
    public File getDefaultPrintFile (File folder,
                                     Score score)
    {
        if (score.getPrintFile() != null) {
            return score.getPrintFile();
        }

        String child = score.getRadix() + ".sheet.pdf";

        if (folder != null) {
            return new File(folder, child);
        } else {
            return new File(constants.defaultPrintDirectory.getValue(), child);
        }
    }

    //------------//
    // getHistory //
    //------------//
    /**
     * Get access to the list of previously handled images.
     *
     * @return the history set of image files
     */
    public NameSet getHistory ()
    {
        if (history == null) {
            history = new NameSet(
                    "Images History",
                    constants.imagesHistory,
                    constants.historySize.getValue());
        }

        return history;
    }

    //    //-----------//
    //    // midiClose //
    //    //-----------//
    //    /**
    //     * Cut any relationship between the provided score and the Midi
    //     * interface (MidiAgent & MidiReceiver) if any.
    //     * @param score the score being closed
    //     */
    //    public void midiClose (Score score)
    //    {
    //        try {
    //            if (MidiAgentFactory.hasAgent()) {
    //                MidiAgent agent = MidiAgentFactory.getAgent();
    //
    //                if (agent.getScore() == score) {
    //                    agent.setScore(null);
    //                }
    //            }
    //        } catch (Exception ex) {
    //            logger.warn("Error closing Midi interface ", ex);
    //        }
    //    }
    //
    //    //-----------//
    //    // midiWrite //
    //    //-----------//
    //    /**
    //     * Write the Midi sequence of the score into the provided midi file.
    //     * @param score the provided score
    //     * @param file the Midi file to write
    //     * @throws Exception if the writing goes wrong
    //     */
    //    public void midiWrite (Score score,
    //                           File  file)
    //        throws Exception
    //    {
    //        if (!ScoreActions.checkParameters(score)) {
    //            return;
    //        }
    //
    //        if (Main.getMidiPath() != null) {
    //            File path = new File(Main.getMidiPath());
    //
    //            if (path.isDirectory()) {
    //                file = getActualFile(file, getDefaultMidiFile(path, score));
    //            } else {
    //                file = getActualFile(file, path);
    //            }
    //        } else {
    //            file = getActualFile(file, getDefaultMidiFile(null, score));
    //        }
    //
    //        // Actually write the Midi file
    //        try {
    //            MidiAgent agent = MidiAgentFactory.getAgent();
    //
    //            if (agent.getScore() != score) {
    //                agent.setScore(score);
    //            }
    //
    //            agent.write(file);
    //            score.setMidiFile(file);
    //            logger.info("Midi written to " + file);
    //
    //            // Remember (even across runs) the selected directory
    //            constants.defaultMidiDirectory.setValue(file.getParent());
    //        } catch (Exception ex) {
    //            logger.warn("Cannot write Midi to " + file, ex);
    //            throw ex;
    //        }
    //    }
    //----------------//
    // removeInstance //
    //----------------//
    /**
     * Remove the provided score from the collection of instances.
     *
     * @param score the score to remove
     */
    public synchronized void removeInstance (Score score)
    {
        logger.debug("removeInstance {}", score);
        instances.remove(score);
    }

    //--------------------------//
    // setDefaultInputDirectory //
    //--------------------------//
    /**
     * Remember the directory where images should be found.
     *
     * @param directory the latest image directory
     */
    public void setDefaultInputDirectory (String directory)
    {
        constants.defaultInputDirectory.setValue(directory);
    }

    //------------//
    // storeBench //
    //------------//
    /**
     * Store the sheet bench.
     *
     * @param bench    the bench to write to disk
     * @param file     the written file, or null
     * @param complete true if we need to complete the bench data
     */
    public void storeBench (ScoreBench bench,
                            File file,
                            boolean complete)
    {
        // Check if we do save bench data
        if ((Main.getBenchPath() == null)
            && !constants.saveBenchToDisk.getValue()) {
            return;
        }

        if (Main.getBenchPath() != null) {
            File path = new File(Main.getBenchPath());

            if (path.isDirectory()) {
                file = getActualFile(
                        file,
                        getDefaultBenchFile(path, bench.getScore()));
            } else {
                file = getActualFile(file, path);
            }
        } else {
            file = getActualFile(
                    file,
                    getDefaultBenchFile(null, bench.getScore()));
        }

        // Actually store the score bench
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
            bench.store(fos, complete);

            if (complete) {
                logger.info("Complete score bench stored as {}", file);
            }

            // Remember (even across runs) the selected directory
            constants.defaultBenchDirectory.setValue(file.getParent());
        } catch (Exception ex) {
            logger.warn("Error storing score bench to " + file, ex);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    //------------------//
    // writePhysicalPdf //
    //------------------//
    /**
     * Print the score physical appearance into the provided PDF file.
     *
     * @param score the provided score
     * @param file  the PDF file to write
     */
    public void writePhysicalPdf (Score score,
                                  File file)
    {
        if (Main.getPrintPath() != null) {
            File path = new File(Main.getPrintPath());

            if (path.isDirectory()) {
                file = getActualFile(file, getDefaultPrintFile(path, score));
            } else {
                file = getActualFile(file, path);
            }
        } else {
            file = getActualFile(file, getDefaultPrintFile(null, score));
        }

        // Actually write the PDF file
        try {
            new SheetPdfOutput(score, file).write();
            score.setPrintFile(file);
            logger.info("Score printed to {}", file);

            // Remember (even across runs) the selected directory
            constants.defaultPrintDirectory.setValue(file.getParent());
        } catch (Exception ex) {
            logger.warn("Cannot write PDF to " + file, ex);
        }
    }

    //----------------//
    // closeAllScores //
    //----------------//
    /**
     * Close all score instances.
     */
    private void closeAllScores ()
    {
        int count = 0;

        // NB: Use a COPY of instances, to avoid concurrent modification
        for (Score score : new ArrayList<>(instances)) {
            score.close();
            count++;
        }

        logger.debug("{} score(s) closed", count);
    }

    //---------------//
    // getActualFile //
    //---------------//
    /**
     * Report the actual file to be used as target, using the provided
     * target file if any, otherwise the score default, and making sure
     * the file parent folder really exists.
     *
     * @param targetFile  the provided target candidate, or null
     * @param defaultFile the default target
     * @return the file to use
     */
    private File getActualFile (File targetFile,
                                File defaultFile)
    {
        try {
            if (targetFile == null) {
                targetFile = defaultFile;
            }

            File canon = new File(targetFile.getCanonicalPath());

            // Make sure the folder exists
            File folder = new File(canon.getParent());

            if (folder.mkdirs()) {
                logger.info("Creating folder {}", folder);
            }

            return canon;
        } catch (IOException ex) {
            logger.warn("Cannot getCanonicalPath for " + targetFile, ex);

            return null;
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.String defaultExportDirectory = new Constant.String(
                WellKnowns.DEFAULT_SCORES_FOLDER.toString(),
                "Default directory for saved scores");

        Constant.Boolean saveBenchToDisk = new Constant.Boolean(
                false,
                "Should we save bench data to disk");

        Constant.String defaultBenchDirectory = new Constant.String(
                WellKnowns.DEFAULT_BENCHES_FOLDER.toString(),
                "Default directory for saved benches");

        Constant.String defaultPrintDirectory = new Constant.String(
                WellKnowns.DEFAULT_PRINT_FOLDER.toString(),
                "Default directory for printing sheet files");

        Constant.Boolean defaultInjectSignature = new Constant.Boolean(
                true,
                "Should we inject our signature in the exported scores?");

        Constant.String imagesHistory = new Constant.String(
                "",
                "History of loaded images");

        Constant.Integer historySize = new Constant.Integer(
                "count",
                10,
                "Maximum number of files names kept in history");

        Constant.String defaultInputDirectory = new Constant.String(
                WellKnowns.EXAMPLES_FOLDER.toString(),
                "Default directory for selection of image files");

        Constant.String defaultDewarpDirectory = new Constant.String(
                WellKnowns.TEMP_FOLDER.toString(),
                "Default directory for saved dewarped images");

    }
}
