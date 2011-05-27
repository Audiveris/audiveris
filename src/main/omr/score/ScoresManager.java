//----------------------------------------------------------------------------//
//                                                                            //
//                         S c o r e s M a n a g e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.midi.MidiAbstractions;
import omr.score.midi.MidiAgent;
import omr.score.ui.ScoreActions;
import omr.score.ui.SheetPdfOutput;

import omr.script.ScriptActions;

import omr.util.NameSet;

import org.jdesktop.application.Application.ExitListener;

import java.io.*;
import java.util.*;

/**
 * Class {@code ScoresManager} is a singleton which provides administrative
 * features for score instances.
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
    private static final Logger logger = Logger.getLogger(ScoresManager.class);

    /** The extension used for score output files: {@value} */
    public static final String SCORE_EXTENSION = ".xml";

    /** The extension used for score bench files: {@value} */
    public static final String BENCH_EXTENSION = ".bench.properties";

    /** The single instance of this class */
    private static volatile ScoresManager INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** Instances of Score */
    private List<Score> instances = new ArrayList<Score>();

    /** Image file history  (filled only when images are successfully loaded) */
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
            Main.getGui()
                .addExitListener(
                new ExitListener() {
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

                        public void willExit (EventObject e)
                        {
                            // Close all sheets, to record their bench data
                            closeAllScores();
                        }
                    });
        }
    }

    //~ Methods ----------------------------------------------------------------

    //----------------------//
    // getDefaultExportFile //
    //----------------------//
    /**
     * Report the file to which the score would be written by default
     * @param score the score to export
     * @return the default file
     */
    public File getDefaultExportFile (Score score)
    {
        return (score.getExportFile() != null) ? score.getExportFile()
               : new File(
            constants.defaultScoreDirectory.getValue(),
            score.getRadix() + SCORE_EXTENSION);
    }

    //--------------------------//
    // setDefaultImageDirectory //
    //--------------------------//
    /**
     * Remember the directory where images should be found
     * @param directory the latest image directory
     */
    public void setDefaultImageDirectory (String directory)
    {
        constants.defaultImageDirectory.setValue(directory);
    }

    //--------------------------//
    // getDefaultImageDirectory //
    //--------------------------//
    /**
     * Report the directory where images should be found
     * @return the latest image directory
     */
    public String getDefaultImageDirectory ()
    {
        return constants.defaultImageDirectory.getValue();
    }

    //--------------------//
    // getDefaultMidiFile //
    //--------------------//
    /**
     * Report the file to which the MIDI data would be written by default
     * @param score the score to export
     * @return the default file
     */
    public File getDefaultMidiFile (Score score)
    {
        return (score.getMidiFile() != null) ? score.getMidiFile()
               : new File(
            constants.defaultMidiDirectory.getValue(),
            score.getRadix() + MidiAbstractions.MIDI_EXTENSION);
    }

    //------------------------//
    // getDefaultSheetPdfFile //
    //------------------------//
    /**
     * Report the file to which the sheet PDF data would be written by default
     * @param score the score to export
     * @return the default file
     */
    public File getDefaultSheetPdfFile (Score score)
    {
        return (score.getSheetPdfFile() != null) ? score.getSheetPdfFile()
               : new File(
            constants.defaultSheetPdfDirectory.getValue(),
            score.getRadix() + ".sheet.pdf");
    }

    //------------//
    // getHistory //
    //------------//
    /**
     * Get access to the list of previously handled images
     *
     * @return the history set of image files
     */
    public NameSet getHistory ()
    {
        if (history == null) {
            history = new NameSet(
                "Images History",
                constants.imagesHistory,
                10);
        }

        return history;
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class
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

    //---------------------//
    // getDefaultBenchFile //
    //---------------------//
    /**
     * Report the file to which the bench data would be written by default
     * @param score the score to export
     * @return the default file
     */
    public File getDefaultBenchFile (Score score)
    {
        return new File(
            constants.defaultBenchDirectory.getValue(),
            score.getRadix() + BENCH_EXTENSION);
    }

    //---------------------------//
    // getDefaultDewarpDirectory //
    //---------------------------//
    /**
     * Report the directory to which dewarped images would be saved by default
     * @return the default file
     */
    public File getDefaultDewarpDirectory ()
    {
        return new File(constants.defaultDewarpDirectory.getValue());
    }

    //--------------//
    // isMultiScore //
    //--------------//
    /**
     * Report whether we are handling more than one score
     * @return true if more than one score
     */
    public static boolean isMultiScore ()
    {
        return getInstance().instances.size() > 1;
    }

    //-------------//
    // addInstance //
    //-------------//
    /**
     * Insert this new score in the set of score instances
     *
     * @param score the score to insert
     */
    public synchronized void addInstance (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("addInstance " + score);
        }

        // Remove duplicate if any
        for (Iterator<Score> it = instances.iterator(); it.hasNext();) {
            Score  s = it.next();
            String path = s.getImagePath();

            if (path.equals(score.getImagePath())) {
                if (logger.isFineEnabled()) {
                    logger.fine("Removing duplicate " + s);
                }

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
     * Export a score using the partwise structure of MusicXML to the default
     * file for the provided score
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
     * Export a score using the partwise structure of MusicXML to the provided
     * file
     *
     * @param score the score to export
     * @param exportFile the xml file to write, or null
     * @param injectSignature should we inject our signature?
     */
    public void export (Score   score,
                        File    exportFile,
                        Boolean injectSignature)
    {
        exportFile = getActualFile(exportFile, getDefaultExportFile(score));

        // Actually export the score material
        try {
            ScoreExporter exporter = new ScoreExporter(score);

            if (injectSignature != null) {
                exporter.export(exportFile, injectSignature);
            } else {
                exporter.export(
                    exportFile,
                    constants.defaultInjectSignature.getValue());
            }

            logger.info("Score exported to " + exportFile);

            // Remember (even across runs) the selected directory
            constants.defaultScoreDirectory.setValue(exportFile.getParent());

            // Remember the file in the score itself
            score.setExportFile(exportFile);
        } catch (Exception ex) {
            logger.warning("Error storing score to " + exportFile, ex);
        }
    }

    //-----------//
    // midiClose //
    //-----------//
    /**
     * Cut any relationship between the provided score and the Midi interface
     * (MidiAgent & MidiReceiver) if any
     *
     * @param score the score being closed
     */
    public void midiClose (Score score)
    {
        try {
            MidiAgent agent = MidiAgent.getInstance();

            if (agent.getScore() == score) {
                agent.setScore(null);
            }
        } catch (Exception ex) {
            logger.warning("Error closing Midi interface ", ex);
        }
    }

    //-----------//
    // midiWrite //
    //-----------//
    /**
     * Write the Midi sequence of the score into the provided midi file.
     *
     * @param score the provided score
     * @param midiFile the Midi file to write
     * @throws Exception if the writing goes wrong
     */
    public void midiWrite (Score score,
                           File  midiFile)
        throws Exception
    {
        if (!ScoreActions.checkParameters(score)) {
            return;
        }

        midiFile = getActualFile(midiFile, getDefaultMidiFile(score));

        // Actually write the Midi file
        try {
            MidiAgent agent = MidiAgent.getInstance();

            if (agent.getScore() != score) {
                agent.setScore(score);
            }

            agent.write(midiFile);
            score.setMidiFile(midiFile);
            logger.info("Midi written to " + midiFile);

            // Remember (even across runs) the selected directory
            constants.defaultMidiDirectory.setValue(midiFile.getParent());
        } catch (Exception ex) {
            logger.warning("Cannot write Midi to " + midiFile, ex);
            throw ex;
        }
    }

    //----------------//
    // removeInstance //
    //----------------//
    /**
     * Remove the provided score from the collection of instances
     * @param score the score to remove
     */
    public synchronized void removeInstance (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("removeInstance " + score);
        }

        instances.remove(score);
    }

    //------------//
    // storeBench //
    //------------//
    /**
     * Store the sheet bench
     * @param bench the bench to write to disk
     * @param file the written file, or null
     * @param complete true if we need to complete the bench data
     */
    public void storeBench (ScoreBench bench,
                            File       file,
                            boolean    complete)
    {
        // Check if we do save bench data
        if (!Main.hasBenchFlag() && !constants.saveBenchToDisk.getValue()) {
            return;
        }

        file = getActualFile(file, getDefaultBenchFile(bench.getScore()));

        // Actually store the score bench
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
            bench.store(fos, complete);

            if (complete) {
                logger.info("Complete score bench stored as " + file);
            }

            // Remember (even across runs) the selected directory
            constants.defaultBenchDirectory.setValue(file.getParent());
        } catch (Exception ex) {
            logger.warning("Error storing score bench to " + file, ex);
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
     * @param pdfFile the PDF file to write
     */
    public void writePhysicalPdf (Score score,
                                  File  pdfFile)
    {
        pdfFile = getActualFile(pdfFile, getDefaultSheetPdfFile(score));

        // Actually write the PDF file
        try {
            new SheetPdfOutput(score, pdfFile).write();
            score.setSheetPdfFile(pdfFile);
            logger.info("Score printed to " + pdfFile);

            // Remember (even across runs) the selected directory
            constants.defaultSheetPdfDirectory.setValue(pdfFile.getParent());
        } catch (Exception ex) {
            logger.warning("Cannot write PDF to " + pdfFile, ex);
        }
    }

    //---------------//
    // getActualFile //
    //---------------//
    /**
     * Report the actual file to be used as target, using the provided target
     * file if any otherwise the score default, and making sure the file parent
     * folder really exists
     * @param targetFile the provided target candidate, or null
     * @param defaultFile the default target
     * @return the file to use
     */
    private File getActualFile (File targetFile,
                                File defaultFile)
    {
        if (targetFile == null) {
            targetFile = defaultFile;
        }

        // Make sure the folder exists
        File folder = new File(targetFile.getParent());

        if (folder.mkdirs()) {
            logger.info("Creating folder " + folder);
        }

        return targetFile;
    }

    //----------------//
    // closeAllScores //
    //----------------//
    /**
     * Close all score instances
     */
    private void closeAllScores ()
    {
        int count = 0;

        // NB: Use a COPY of instances, to avoid concurrent modification
        for (Score score : new ArrayList<Score>(instances)) {
            score.close();
            count++;
        }

        if (logger.isFineEnabled()) {
            logger.fine(count + " score(s) closed");
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

        /** Default directory for saved scores */
        Constant.String defaultScoreDirectory = new Constant.String(
            System.getProperty("user.home"),
            "Default directory for saved scores");

        /** Should we save bench data to disk */
        Constant.Boolean saveBenchToDisk = new Constant.Boolean(
            false,
            "Should we save bench data to disk");

        /** Default directory for saved benches */
        Constant.String defaultBenchDirectory = new Constant.String(
            System.getProperty("user.home"),
            "Default directory for saved benches");

        /** Default directory for writing Midi files */
        Constant.String defaultMidiDirectory = new Constant.String(
            System.getProperty("user.home"),
            "Default directory for writing Midi files");

        /** Default directory for writing sheet PDF files */
        Constant.String defaultSheetPdfDirectory = new Constant.String(
            System.getProperty("user.home"),
            "Default directory for writing sheet PDF files");

        /** Should we export our signature? */
        Constant.Boolean defaultInjectSignature = new Constant.Boolean(
            true,
            "Should we inject our signature in the exported scores?");

        /** Backing constant for image history */
        Constant.String imagesHistory = new Constant.String(
            "",
            "History of loaded images");

        /** Default directory for selection of image files */
        Constant.String defaultImageDirectory = new Constant.String(
            System.getProperty("user.home"),
            "Default directory for selection of image files");

        /** Default directory for saved dewarped images */
        Constant.String defaultDewarpDirectory = new Constant.String(
            System.getProperty("user.home"),
            "Default directory for saved dewarped images");
    }
}
