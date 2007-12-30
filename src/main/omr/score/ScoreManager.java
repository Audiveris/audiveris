//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e M a n a g e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.midi.MidiAgent;
import omr.score.ui.ScoreActions;
import omr.score.visitor.ScoreExporter;

import omr.script.ExportTask;
import omr.script.MidiWriteTask;

import omr.ui.util.FileFilter;
import omr.ui.util.UIUtilities;

import omr.util.Logger;

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
    private static ScoreManager INSTANCE;

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
        export(score, null);
    }

    //--------//
    // export //
    //--------//
    /**
     * Export a score using the partwise structure of MusicXML to the provided
     * file
     *
     * @param score the score to export
     * @param xmlFile the xml file to write, or null
     */
    public void export (Score score,
                        File  xmlFile)
    {
        // Where do we write the score xml file?
        if (xmlFile == null) {
            xmlFile = new File(
                constants.defaultScoreDirectory.getValue(),
                score.getRadix() + SCORE_EXTENSION);

            // Ask user confirmation, if Gui available
            if (Main.getGui() != null) {
                // Let the user select a score output file
                FileFilter filter = new FileFilter(
                    "XML files",
                    new String[] { SCORE_EXTENSION });
                xmlFile = UIUtilities.fileChooser(true, null, xmlFile, filter);
            }
        }

        if (xmlFile != null) {
            // Remember (even across runs) the selected directory
            constants.defaultScoreDirectory.setValue(xmlFile.getParent());

            // Make sure the folder exists
            File folder = new File(xmlFile.getParent());

            if (folder.mkdirs()) {
                logger.info("Creating folder " + folder);
            }

            // Record this task in the sheet script
            score.getSheet()
                 .getScript()
                 .addTask(new ExportTask(xmlFile.getPath()));

            // Actually export the score material
            try {
                new ScoreExporter(score).export(xmlFile);
                logger.info("Score exported to " + xmlFile);
            } catch (Exception ex) {
                logger.warning("Error storing score to " + xmlFile, ex);
            }
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

        // Make sure the folder exists
        File folder = new File(midiFile.getParent());

        if (folder.mkdirs()) {
            logger.info("Creating folder " + folder);
        }

        // Record this task in the sheet script
        score.getSheet()
             .getScript()
             .addTask(new MidiWriteTask(midiFile.getPath()));

        // Actually write the Midi file
        try {
            MidiAgent agent = MidiAgent.getInstance();

            if (agent.getStatus() == MidiAgent.Status.STOPPED) {
                agent.setScore(score);
            }

            if (agent.getScore() == score) {
                agent.write(midiFile);
                logger.info("Midi written to " + midiFile);
            } else {
                logger.warning("Midi Agent is busy with another score");
            }
        } catch (Exception ex) {
            logger.warning("Cannot write Midi to " + midiFile, ex);
            throw ex;
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
    }
}
