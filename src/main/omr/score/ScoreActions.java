//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e A c t i o n s                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import static omr.plugin.Dependency.*;
import omr.plugin.Plugin;
import static omr.plugin.PluginType.*;

import omr.sheet.SheetManager;

import omr.step.Step;

import omr.ui.util.SwingWorker;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;

/**
 * Class <code>ScoreActions</code> gathers actions related to scores
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreActions
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    ///    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreActions.class);

    //~ Inner Classes ----------------------------------------------------------

    //--------------//
    // BrowseAction //
    //--------------//
    /**
     * Class <code>BrowseAction</code> launches the tree display of the current
     * score.
     */
    @Plugin(type = SCORE_EDIT, dependency = SCORE_AVAILABLE, onToolbar = true)
    public static class BrowseAction
        extends AbstractAction
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            ScoreController.getCurrentScore()
                           .viewScore();
        }
    }

    //------------//
    // DumpAction //
    //------------//
    /**
     * Class <code>DumpAction</code> dumps the internals of a score to system
     * output
     *
     */
    @Plugin(type = SCORE_EDIT, dependency = SCORE_AVAILABLE, onToolbar = true)
    public static class DumpAction
        extends AbstractAction
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            ScoreController.getCurrentScore()
                           .dump();
        }
    }

    //---------------//
    // RebuildAction //
    //---------------//
    /**
     * Class <code>RebuildAction</code> re-translates all sheet glyphs to score
     * entities.
     */
    @Plugin(type = SCORE_EDIT, dependency = SCORE_AVAILABLE, onToolbar = true)
    public static class RebuildAction
        extends AbstractAction
    {
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            final SwingWorker worker = new SwingWorker() {
                public Object construct ()
                {
                    try {
                        SheetManager.getSelectedSheet()
                                    .getSheetSteps()
                                    .doit(Step.SCORE);
                    } catch (Exception ex) {
                        logger.warning("Could not refresh score", ex);
                    }

                    return null;
                }
            };

            worker.start();
        }
    }

    //-------------//
    // StoreAction //
    //-------------//
    /**
     * Class <code>StoreAction</code> handles the saving of the currently
     * selected score, using MusicXML format.
     */
    @Plugin(type = SCORE_EXPORT, dependency = SCORE_AVAILABLE, onToolbar = true)
    public static class StoreAction
        extends AbstractAction
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            final SwingWorker worker = new SwingWorker() {
                public Object construct ()
                {
                    Score score = ScoreController.getCurrentScore();

                    try {
                        score.export();
                    } catch (Exception ex) {
                        logger.warning("Could not store " + score, ex);
                    }

                    return null;
                }
            };

            worker.start();
        }
    }
}
