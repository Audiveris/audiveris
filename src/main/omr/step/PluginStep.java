//----------------------------------------------------------------------------//
//                                                                            //
//                            P l u g i n S t e p                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.Main;

import omr.plugin.Plugin;

import omr.score.Score;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(
            PluginStep.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** The current default plugin. */
    private Plugin plugin;

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
                      Sheet sheet)
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
     *
     * @return a named description
     */
    @Override
    public String getDescription ()
    {
        return super.getDescription() + " (" + plugin.getTitle() + ")";
    }

    //-----------//
    // setPlugin //
    //-----------//
    /**
     * Set the default plugin.
     *
     * @param plugin the (new) default plugin
     */
    public void setPlugin (Plugin plugin)
    {
        Plugin oldPlugin = this.plugin;
        this.plugin = plugin;

        if (oldPlugin != null) {
            // Update step tooltip with this new plugin
            Main.getGui()
                    .getStepMenu()
                    .updateMenu();
        }
    }
}
