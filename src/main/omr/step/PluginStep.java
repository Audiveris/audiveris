//----------------------------------------------------------------------------//
//                                                                            //
//                            P l u g i n S t e p                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2012. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.Main;

import omr.log.Logger;

import omr.plugin.Plugin;

import omr.score.Score;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code PluginStep} launches the default plugin.
 *
 * @author Hervé Bitteur
 */
public class PluginStep
    extends AbstractStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(PluginStep.class);

    //~ Instance fields --------------------------------------------------------

    /** The defined default plugin */
    private final Plugin plugin;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // PluginStep //
    //------------//
    /**
     * Creates a new PluginStep object.
     */
    public PluginStep (Plugin plugin)
    {
        super(
            Steps.PLUGIN,
            Level.SCORE_LEVEL,
            Mandatory.OPTIONAL,
            DATA_TAB,
            "Launch the default plugin");

        this.plugin = plugin;
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> systems,
                      Sheet                  sheet)
        throws StepException
    {
        Score score = sheet.getScore();

        // Interactive or Batch?
        if (Main.getGui() != null) {
            plugin.getTask(score)
            .execute();
        } else {
            plugin.runPlugin(score);
        }
    }

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Augment the description with the plugin title
     * @return a named description
     */
    @Override
    public String getDescription ()
    {
        return super.getDescription() + " (" + plugin.getTitle() + ")";
    }
}
