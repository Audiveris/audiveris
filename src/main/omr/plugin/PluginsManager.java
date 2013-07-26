//----------------------------------------------------------------------------//
//                                                                            //
//                        P l u g i n s M a n a g e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.plugin;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.Score;
import omr.score.ui.ScoreController;
import omr.score.ui.ScoreDependent;

import omr.sheet.ui.SheetsController;

import omr.ui.util.SeparableMenu;

import omr.util.FileUtil;
import omr.util.Param;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import omr.step.PluginStep;
import omr.step.Steps;

/**
 * Class {@code PluginsManager} handles the collection of application
 * registered plugins.
 * Each registered plugin is represented by a menu item.
 * One of these plugins can be set as the default editor plugin and directly
 * launched by the dedicated toolbar button.
 *
 * <p>Any file, with the ".js" extension, found in the
 * <code>plugins</code>
 * folder will lead to the creation of a corresponding Plugin instance.</p>
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
    private static final Logger logger = LoggerFactory.getLogger(PluginsManager.class);

    /** Singleton. */
    private static PluginsManager INSTANCE;

    /** Filter for plugin script files. */
    private static final FileFilter pluginFilter = new FileFilter()
    {
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

    /** Default plugin id. */
    public static final Param<String> defaultPluginId = new Default();

    //~ Instance fields --------------------------------------------------------
    //
    /** The concrete UI menu. */
    private JMenu menu;

    /** The sorted collection of registered plugins: ID -> Plugin. */
    private final Map<String, Plugin> map = new TreeMap<>();

    /** The default plugin. */
    private Plugin defaultPlugin;

    //~ Constructors -----------------------------------------------------------
    //
    //----------------//
    // PluginsManager //
    //----------------//
    /**
     * Generates the menu to be inserted in the plugin menu hierarchy,
     * based on the script files discovered in the plugin folder.
     *
     * @param menu the hosting menu, or null
     */
    private PluginsManager ()
    {
        // Browse the plugin folder for relevant scripts
        File pluginDir = WellKnowns.PLUGINS_FOLDER;

        if (pluginDir.exists() && pluginDir.isDirectory()) {
            for (File file : pluginDir.listFiles(pluginFilter)) {
                try {
                    Plugin plugin = new Plugin(file);
                    map.put(plugin.getId(), plugin);
                } catch (Exception ex) {
                    logger.warn("Could not process plugin file {} [{}]",
                            file, ex);
                }
            }

            // Default plugin, if any is defined
            setDefaultPlugin(constants.defaultPlugin.getValue().trim());
        }
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // getPlugins //
    //------------//
    /**
     * Report the collection of plugins ids
     *
     * @return the various plugins ids
     */
    public Collection<String> getPluginIds ()
    {
        return map.keySet();
    }

    //------------------//
    // getDefaultPlugin //
    //------------------//
    /**
     * Return the default plugin if any.
     *
     * @return the default plugin, or null if none is defined
     */
    public Plugin getDefaultPlugin ()
    {
        return defaultPlugin;
    }

    //------------------//
    // setDefaultPlugin //
    //------------------//
    /**
     * Assign the default plugin.
     */
    public final void setDefaultPlugin (String pluginId)
    {
        Plugin plugin = findDefaultPlugin(pluginId);

        if (!pluginId.isEmpty() && (plugin == null)) {
            logger.warn("Could not find default plugin {}", pluginId);
        } else {
            setDefaultPlugin(plugin);
        }
    }

    //------------------//
    // setDefaultPlugin //
    //------------------//
    /**
     * Assign the default plugin.
     */
    public final void setDefaultPlugin (Plugin defaultPlugin)
    {
        Plugin oldDefaultPlugin = this.defaultPlugin;
        this.defaultPlugin = defaultPlugin;

        if (oldDefaultPlugin != null) {
            PluginStep pluginStep = (PluginStep) Steps.valueOf(Steps.PLUGIN);
            pluginStep.setPlugin(defaultPlugin);
        }
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the class singleton.
     *
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
     *
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

        // Listener to modify attributes on-the-fly
        menu.addMenuListener(new MyMenuListener());

        this.menu = menu;

        return menu;
    }

    //--------------//
    // invokeEditor //
    //--------------//
    /**
     * Action to invoke the default score editor
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SCORE_AVAILABLE)
    public Task<Void, Void> invokeDefaultPlugin (ActionEvent e)
    {
        if (defaultPlugin == null) {
            logger.warn("No default plugin defined");

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

    //-------------------//
    // findDefaultPlugin //
    //-------------------//
    private Plugin findDefaultPlugin (String pluginId)
    {
        for (Plugin plugin : map.values()) {
            if (plugin.getId().equalsIgnoreCase(pluginId)) {
                return plugin;
            }
        }

        return null;
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
                "",
                "Name of default plugin");

    }

//----------------//
// MyMenuListener //
//----------------//
    /**
     * Class {@code MyMenuListener} is triggered when menu is entered.
     * This is meant to enable menu items only when a sheet is selected.
     */
    private class MyMenuListener
            implements MenuListener
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public void menuCanceled (MenuEvent e)
        {
        }

        @Override
        public void menuDeselected (MenuEvent e)
        {
        }

        @Override
        public void menuSelected (MenuEvent e)
        {
            boolean enabled = SheetsController.getCurrentSheet() != null;

            for (int i = 0; i < menu.getItemCount(); i++) {
                JMenuItem menuItem = menu.getItem(i);

                // Beware of separators (for which returned menuItem is null)
                if (menuItem != null) {
                    menuItem.setEnabled(enabled);
                }
            }
        }
    }

    //---------//
    // Default //
    //---------//
    private static class Default
            extends Param<String>
    {

        @Override
        public String getSpecific ()
        {
            return constants.defaultPlugin.getValue();
        }

        @Override
        public boolean setSpecific (String specific)
        {
            if (!getSpecific().equals(specific)) {
                constants.defaultPlugin.setValue(specific);
                getInstance().setDefaultPlugin(specific);
                logger.info("Default plugin is now ''{}''", specific);

                return true;
            }

            return false;
        }
    }
}
