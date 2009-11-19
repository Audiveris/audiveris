//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e M a n a g e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
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

import omr.ui.util.OmrFileFilter;
import omr.ui.util.UIUtilities;

import java.io.*;

/**
 * Class <code>ScoreManager</code> handles a collection of score instances.
 *
 * @author Herv&eacute; Bitteur and Brenton Partridge
 * @version $Id$
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
        export(score, score.getExportFile());
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
     */
    public void export (Score score,
                        File  exportFile)
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
            exporter.export(exportFile);
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

                if (agent.getStatus() == MidiAgent.Status.STOPPED) {
                    agent.setScore(score);
                }

                if (agent.getScore() == score) {
                    agent.write(midiFile);
                    logger.info("Midi written to " + midiFile);

                    // Remember (even across runs) the selected directory
                    constants.defaultMidiDirectory.setValue(
                        midiFile.getParent());
                } else {
                    logger.warning("Midi Agent is busy with another score");
                }
            } catch (Exception ex) {
                logger.warning("Cannot write Midi to " + midiFile, ex);
                throw ex;
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
        Constant.String defaultMidiDirectory = new Constant.String(
            "",
            "Default directory for writing Midi files");
    }
}
