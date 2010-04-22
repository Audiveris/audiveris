//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e M a n a g e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.sheet.SheetBench;
import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.midi.MidiAbstractions;
import omr.score.midi.MidiAgent;
import omr.score.ui.ScoreActions;

import omr.ui.util.OmrFileFilter;
import omr.ui.util.UIUtilities;

import java.io.*;

/**
 * Class <code>ScoreManager</code> handles a collection of score instances.
 *
 * @author Hervé Bitteur and Brenton Partridge
 */
public class ScoreManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreManager.class);

    /** The extension used for score output files: {@value} */
    public static final String SCORE_EXTENSION = ".xml";

    /** The extension used for score bench files: {@value} */
    public static final String BENCH_EXTENSION = ".bench.properties";

    /** The single instance of this class */
    private static volatile ScoreManager INSTANCE;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScoreManager //
    //--------------//
    /**
     * Creates a Score Manager.
     */
    private ScoreManager ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class
     *
     * @return the single instance
     */
    public static ScoreManager getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new ScoreManager();
        }

        return INSTANCE;
    }

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
        if (exportFile == null) {
            exportFile = this.getDefaultExportFile(score);
        }

        // Make sure the folder exists
        File folder = new File(exportFile.getParent());

        if (folder.mkdirs()) {
            logger.info("Creating folder " + folder);
        }

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

        // Where do we write the midi file?
        if (midiFile == null) {
            midiFile = new File(
                constants.defaultMidiDirectory.getValue(),
                score.getRadix() + MidiAbstractions.MIDI_EXTENSION);

            // Ask user confirmation, if Gui available
            if (Main.getGui() != null) {
                // Let the user select a score output file
                OmrFileFilter filter = new OmrFileFilter(
                    "Midi files",
                    new String[] { MidiAbstractions.MIDI_EXTENSION });
                midiFile = UIUtilities.fileChooser(
                    true,
                    null,
                    midiFile,
                    filter);
            }
        }

        if (midiFile != null) {
            // Make sure the folder exists
            File folder = new File(midiFile.getParent());

            if (folder.mkdirs()) {
                logger.info("Creating folder " + folder);
            }

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
    }

    //------------//
    // storeBench //
    //------------//
    /**
     * Store the score bench
     * @param score the score whose bench is to be stored
     * @param file the written file, or null
     */
    public void storeBench (Score score,
                            File  file)
    {
        if (file == null) {
            file = new File(
                constants.defaultBenchDirectory.getValue(),
                score.getRadix() + BENCH_EXTENSION);
        }

        // Make sure the folder exists
        File folder = new File(file.getParent());

        if (folder.mkdirs()) {
            logger.info("Creating folder " + folder);
        }

        // Actually store the score bench
        FileOutputStream fos = null;

        try {
            SheetBench bench = score.getSheet()
                                    .getBench();
            fos = new FileOutputStream(file);
            bench.store(fos);

            logger.info("Score bench stored as " + file);

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
            "",
            "Default directory for saved scores");

        /** Default directory for saved benches */
        Constant.String defaultBenchDirectory = new Constant.String(
            "",
            "Default directory for saved benches");

        /** Default directory for writing Midi files */
        Constant.String defaultMidiDirectory = new Constant.String(
            "",
            "Default directory for writing Midi files");

        /** Should we export our signature? */
        Constant.Boolean defaultInjectSignature = new Constant.Boolean(
            true,
            "Should we export our signature?");
    }
}
