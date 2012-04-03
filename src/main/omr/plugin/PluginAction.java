//----------------------------------------------------------------------------//
//                                                                            //
//                          P l u g i n A c t i o n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.plugin;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ui.ScoreController;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Class {@code PluginAction} implements the concrete user action
 * related to a registered plugin.
 *
 * @author Hervé Bitteur
 */
class PluginAction
    extends AbstractAction
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(PluginAction.class);

    //~ Instance fields --------------------------------------------------------

    /** The related plugin */
    private final Plugin plugin;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // PluginAction //
    //--------------//
    /**
     * Creates a new PluginAction object.
     * @param plugin the underlying scripting plugin
     */
    public PluginAction (Plugin plugin)
    {
        super(plugin.getTitle());
        this.plugin = plugin;
        putValue(SHORT_DESCRIPTION, plugin.getDescription());
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // actionPerformed //
    //-----------------//
    @Override
    public void actionPerformed (ActionEvent e)
    {
        final Score score = ScoreController.getCurrentScore();

        if (score != null) {
            plugin.perform(score);
        }
    }
}
