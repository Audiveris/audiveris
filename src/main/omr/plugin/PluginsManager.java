//----------------------------------------------------------------------------//
//                                                                            //
//                        P l u g i n s M a n a g e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.plugin;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ui.ScoreController;
import omr.score.ui.ScoreDependent;

import omr.step.Step;
import omr.step.Stepping;
import omr.step.Steps;

import omr.ui.util.SeparableMenu;

import omr.util.FileUtil;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code PluginsManager} handles the collection of application
 * registered plugins.
 * Each registered plugin is represented by a menu item.
 * One of these plugins can be set as the default editor plugin and directly
 * launched by the dedicated toolbar button.
 *
 * @author Hervé Bitteur
 */
public class PluginsManager
    extends ScoreDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(PluginsManager.class);

    /** Singleton */
    private static PluginsManager INSTANCE;

    /** Filter for plugin script files */
    private static final FileFilter pluginFilter = new FileFilter() {
        @Override
        public boolean accept (File pathname)
        {
            // Check for proper extension
            String ext = FileUtil.getExtension(pathname);

            if (ext.equalsIgnoreCase(".js")) {
                return true;
            } else {
                return false;
            }
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** The sorted collection of registered plugins: ID -> Plugin */
    private final Map<String, Plugin> map = new TreeMap<String, Plugin>();

    /** The default plugin */
    private Plugin defaultPlugin = null;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // PluginsManager //
    //----------------//
    /**
     * Generates the menu to be inserted in the plugin menu hierarchy,
     * based on the script files discovered in the plugin folder.
     * @param menu the hosting menu, or null
     */
    private PluginsManager ()
    {
        // Browse the plugin folder for relevant scripts
        File pluginDir = WellKnowns.PLUGINS_FOLDER;

        for (File file : pluginDir.listFiles(pluginFilter)) {
            Plugin plugin = new Plugin(file);
            map.put(plugin.getId(), plugin);
        }

        // Default plugin, if any is defined
        for (Plugin plugin : map.values()) {
            if (plugin.getId()
                      .equalsIgnoreCase(constants.defaultPlugin.getValue())) {
                defaultPlugin = plugin;

                break;
            }
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the class singleton.
     * @return the unique instance of this class
     */
    public static synchronized PluginsManager getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new PluginsManager();
        }

        return INSTANCE;
    }

    //---------//
    // getMenu //
    //---------//
    /**
     * Report the concrete UI menu of all plugins
     * @param menu a preallocated menu instance, or null
     * @return the populated menu entity
     */
    public JMenu getMenu (JMenu menu)
    {
        if (menu == null) {
            menu = new SeparableMenu();
        }

        for (Plugin plugin : map.values()) {
            menu.add(new JMenuItem(new PluginAction(plugin)));
        }

        return menu;
    }

    //--------------//
    // invokeEditor //
    //--------------//
    /**
     * Action to invoke the default score editor
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SCORE_AVAILABLE)
    public Task invokeDefaultPlugin (ActionEvent e)
    {
        if (defaultPlugin == null) {
            logger.warning("No default plugin defined");

            return null;
        }

        // Current score export file
        final Score score = ScoreController.getCurrentScore();

        if (score == null) {
            return null;
        } else {
            return defaultPlugin.getTask(score);
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

        Constant.String defaultPlugin = new Constant.String(
            "musescore",
            "Name of default plugin");
    }
}
