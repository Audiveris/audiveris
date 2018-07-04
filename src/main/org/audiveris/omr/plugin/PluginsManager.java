//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P l u g i n s M a n a g e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.plugin;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.ui.util.AbstractMenuListener;
import org.audiveris.omr.ui.util.SeparableMenu;
import org.audiveris.omr.util.param.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code PluginsManager} handles the collection of registered plugins.
 * <p>
 * Each registered plugin is represented by a menu item.
 * A plugin can be manually selected as default and directly launched by a dedicated toolbar button.
 * <p>
 * The <code>config</code> folder is lookup for a potential plugins file.
 *
 * @author Hervé Bitteur
 */
public class PluginsManager
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(PluginsManager.class);

    /** File name for plugins definitions: {@value}. */
    private static final String PLUGINS_FILE_NAME = "plugins.xml";

    /** Singleton. */
    private static PluginsManager INSTANCE;

    /** Persistent default plugin id. */
    public static final Param<String> defaultPluginId = new Default();

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** The concrete UI menu. */
    private JMenu menu;

    /** The list of registered plugins. */
    private final List<Plugin> plugins;

    /** The current default plugin. */
    private Plugin defaultPlugin;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Generates the menu to be inserted in the plugin menu hierarchy,
     * based on the plugins file discovered in Audiveris user config folder.
     *
     * @param menu the hosting menu, or null
     */
    private PluginsManager ()
    {
        // Load all defined plugins
        plugins = loadPlugins();

        // Default plugin, if any is defined
        if (!constants.defaultPlugin.getValue().trim().isEmpty()) {
            setDefaultPlugin(constants.defaultPlugin.getValue().trim());
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // setDefaultPlugin //
    //------------------//
    /**
     * Assign the default plugin via its id.
     *
     * @param pluginId id of new default plugin
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
     *
     * @param defaultPlugin the new default plugin
     */
    public final void setDefaultPlugin (Plugin defaultPlugin)
    {
        this.defaultPlugin = defaultPlugin;
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

        for (Plugin plugin : plugins) {
            menu.add(new JMenuItem(new PluginAction(plugin)));
        }

        // Listener to modify attributes on-the-fly
        menu.addMenuListener(new MyMenuListener());

        this.menu = menu;

        return menu;
    }

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
        List<String> ids = new ArrayList<String>();

        for (Plugin plugin : plugins) {
            ids.add(plugin.getId());
        }

        return ids;
    }

    //-------------------//
    // findDefaultPlugin //
    //-------------------//
    private Plugin findDefaultPlugin (String pluginId)
    {
        for (Plugin plugin : plugins) {
            if (plugin.getId().equalsIgnoreCase(pluginId)) {
                return plugin;
            }
        }

        return null;
    }

    //-------------//
    // loadPlugins //
    //-------------//
    private List<Plugin> loadPlugins ()
    {
        final Path folder = WellKnowns.CONFIG_FOLDER;
        final Path pluginsPath = folder.resolve(PLUGINS_FILE_NAME);

        if (Files.exists(pluginsPath)) {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(PluginsHolder.class);
                Unmarshaller um = jaxbContext.createUnmarshaller();
                PluginsHolder pluginsHolder = (PluginsHolder) um.unmarshal(pluginsPath.toFile());

                for (Plugin plugin : pluginsHolder.list) {
                    plugin.check();
                }

                logger.info("Loaded plugins from {}", pluginsPath);

                return pluginsHolder.list; // Normal exit
            } catch (Throwable ex) {
                logger.warn("Error loading {}", pluginsPath, ex);
            }
        } else {
            logger.info("No {} file found", pluginsPath);
        }

        return Collections.EMPTY_LIST;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Constant.String defaultPlugin = new Constant.String("", "Name of default plugin");
    }

    //---------//
    // Default //
    //---------//
    private static class Default
            extends Param<String>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public String getSpecific ()
        {
            if (isSpecific()) {
                return getValue();
            } else {
                return null;
            }
        }

        @Override
        public String getValue ()
        {
            return constants.defaultPlugin.getValue();
        }

        @Override
        public boolean isSpecific ()
        {
            return !constants.defaultPlugin.isSourceValue();
        }

        @Override
        public boolean setSpecific (String specific)
        {
            if (!getValue().equals(specific)) {
                constants.defaultPlugin.setStringValue(specific);
                getInstance().setDefaultPlugin(specific);
                logger.info("Default plugin is now: {}", specific);

                return true;
            }

            return false;
        }
    }

    //----------------//
    // MyMenuListener //
    //----------------//
    /**
     * Class {@code MyMenuListener} is triggered when menu is entered.
     * <p>
     * This is meant to enable menu items only when a sheet is selected,
     * and to indicate the default plugin if any.
     */
    private class MyMenuListener
            extends AbstractMenuListener
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void menuSelected (MenuEvent e)
        {
            boolean enabled = StubsController.getCurrentStub() != null;

            for (int i = 0; i < menu.getItemCount(); i++) {
                JMenuItem item = menu.getItem(i);

                // Beware of separators (for which returned menuItem is null)
                if (item != null) {
                    item.setEnabled(enabled);

                    // Indicate which plugin is the default (if any)
                    Action action = item.getAction();

                    if (action instanceof PluginAction) {
                        Plugin plugin = ((PluginAction) action).getPlugin();
                        item.setText(
                                plugin.getId() + ((plugin == defaultPlugin) ? " (default)" : ""));
                    }
                }
            }
        }
    }

    //---------------//
    // PluginsHolder //
    //---------------//
    /**
     * Class {@code PluginsHolder} is used to unmarshal the plugins root element.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "plugins")
    private static class PluginsHolder
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** List of plugins. */
        @XmlElementRef
        private List<Plugin> list = new ArrayList<Plugin>();

        //~ Constructors ---------------------------------------------------------------------------
        /** No-arg constructor meant for JAXB. */
        private PluginsHolder ()
        {
        }
    }
}
