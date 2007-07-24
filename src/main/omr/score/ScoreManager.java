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

import omr.score.visitor.ScoreExporter;

import omr.script.ExportTask;

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
     * Report the single instance of this class,
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
     * Export a score using the partwise structure of MusicXML, using the
     * default file for the provided score
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
                Main.getOutputFolder(),
                score.getRadix() + SCORE_EXTENSION);

            // Ask user confirmation, if Gui available
            if (Main.getGui() != null) {
                // Let the user select a score output file
                FileFilter filter = new FileFilter(
                        "XML files",
                        new String[] { SCORE_EXTENSION });
                xmlFile = UIUtilities.fileChooser(true, 
                	null, xmlFile.getPath(), filter);
            }
        }

        if (xmlFile != null) {
        	// Remember (even across runs) the selected directory
        	Main.setOutputFolder(xmlFile.getParent());
        	
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
            new ScoreExporter(score, xmlFile);
        }
    }
}
