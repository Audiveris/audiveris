//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   A c t i o n M a n a g e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.ui.action;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.util.SeparableMenu;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.UriUtil;

import org.jdesktop.application.ApplicationAction;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.xml.bind.JAXBException;

/**
 * Class {@code ActionManager} handles the instantiation and dressing of actions,
 * their organization in the menus and the tool bar, and their enabling.
 *
 * @author Hervé Bitteur
 */
public class ActionManager
{

    private static final Logger logger = LoggerFactory.getLogger(ActionManager.class);

    /** Class loader. */
    private static final ClassLoader classLoader = ActionManager.class.getClassLoader();

    /** The map of all menus, so that we can directly provide some. */
    private final Map<String, JMenu> menuMap = new HashMap<>();

    /** The tool bar that hosts some actions. */
    private final JToolBar toolBar = new JToolBar();

    /** The menu bar for all actions. */
    private final JMenuBar menuBar = new JMenuBar();

    /**
     * Meant to be instantiated at most once.
     */
    private ActionManager ()
    {
    }

    //-------------------//
    // getActionInstance //
    //-------------------//
    /**
     * Retrieve an action knowing its methodName.
     *
     * @param instance   the instance of the hosting class
     * @param methodName the method name
     * @return the action found, or null if none
     */
    public ApplicationAction getActionInstance (Object instance,
                                                String methodName)
    {
        ActionMap actionMap = OmrGui.getApplication().getContext().getActionMap(instance);

        return (ApplicationAction) actionMap.get(methodName);
    }

    //---------//
    // getMenu //
    //---------//
    /**
     * Report the menu built for a given key.
     *
     * @param key the given menu key
     * @return the related menu
     */
    public JMenu getMenu (String key)
    {
        return menuMap.get(key);
    }

    //------------//
    // getMenuBar //
    //------------//
    /**
     * Report the bar containing all generated pull-down menus.
     *
     * @return the menu bar
     */
    public JMenuBar getMenuBar ()
    {
        return menuBar;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report a describing name.
     *
     * @return a describing name
     */
    public String getName ()
    {
        return "ActionManager";
    }

    //------------//
    // getToolBar //
    //------------//
    /**
     * Report the tool bar containing all generated buttons.
     *
     * @return the tool bar
     */
    public JToolBar getToolBar ()
    {
        return toolBar;
    }

    //------------//
    // injectMenu //
    //------------//
    /**
     * Insert a predefined menu, either partly or fully built.
     *
     * @param key  the menu unique name
     * @param menu the menu to inject
     */
    public void injectMenu (String key,
                            JMenu menu)
    {
        menuMap.put(key, menu);
    }

    //--------------------//
    // loadAllDescriptors //
    //--------------------//
    /**
     * Load all descriptors as found in system and user configuration files.
     */
    public void loadAllDescriptors ()
    {
        // Load classes first for system actions, then for user actions if any
        URI[] uris = new URI[]{
            UriUtil.toURI(WellKnowns.RES_URI, "system-actions.xml"),
            WellKnowns.CONFIG_FOLDER.resolve("user-actions.xml").toUri().normalize()};

        for (int i = 0; i < uris.length; i++) {
            URI uri = uris[i];

            try {
                URL url = uri.toURL();
                try (InputStream input = url.openStream()) {
                    Actions.loadActionDescriptors(input);
                }
            } catch (IOException ex) {
                // Item does not exist
                if (i == 0) {
                    // Only the first item (system) is mandatory
                    logger.error("Mandatory file not found {}", uri);
                }
            } catch (JAXBException ex) {
                logger.warn("Error loading actions from " + uri, ex);
            }
        }
    }

    //--------------------//
    // registerAllActions //
    //--------------------//
    /**
     * Register all actions as listed in the descriptor files, and organize them
     * according to the various domains defined.
     * There is one pull-down menu generated for each domain found.
     */
    public void registerAllActions ()
    {
        // Insert an initial separator, to let user easily grab the toolBar
        toolBar.addSeparator();

        DomainLoop:
        for (String domain : Actions.getDomainNames()) {
            // Create dedicated menu for this range, if not already existing
            JMenu menu = menuMap.get(domain);

            if (menu == null) {
                logger.debug("Creating menu:{}", domain);
                menu = new SeparableMenu(domain);
                menuMap.put(domain, menu);
            } else {
                logger.debug("Augmenting menu:{}", domain);
            }

            // Proper menu decoration
            ResourceMap resource = OmrGui.getApplication().getContext().getResourceMap(
                    Actions.class);
            menu.setText(domain); // As default
            menu.setName(domain);

            // Register all actions in the given domain
            registerDomainActions(domain, menu);
            resource.injectComponents(menu); // Localized

            SeparableMenu.trimSeparator(menu); // No separator at end of menu

            // Smart insertion of the menu into the menu bar, and separators into the toolBar
            if (menu.getItemCount() > 0) {
                final int toolCount = toolBar.getComponentCount();

                if (toolCount > 0) {
                    Component comp = toolBar.getComponent(toolCount - 1);

                    if (!(comp instanceof JToolBar.Separator)) {
                        toolBar.addSeparator();
                    }
                }

                menuBar.add(menu);
            }
        }
    }

    //----------------//
    // registerAction //
    //----------------//
    /**
     * Allocate and dress an instance of the provided class, then register the action in
     * the UI structure (menus and buttons) according to the action descriptor parameters.
     *
     * @param action the provided action class
     * @return the registered and decorated instance of the action class
     */
    @SuppressWarnings("unchecked")
    private ApplicationAction registerAction (ActionDescriptor desc)
    {
        ///logger.info("registerAction. " + desc);
        ApplicationAction action = null;

        try {
            // Retrieve proper class instance
            Class<?> classe = classLoader.loadClass(desc.className);
            Object instance = null;

            // Reuse existing instance through a 'getInstance()' method if any
            try {
                Method getInstance = classe.getDeclaredMethod("getInstance", (Class[]) null);

                if (Modifier.isStatic(getInstance.getModifiers())) {
                    instance = getInstance.invoke(null);
                }
            } catch (NoSuchMethodException ignored) {
            }

            if (instance == null) {
                // Fall back to allocate a new class instance
                instance = classe.newInstance();
            }

            // Retrieve the action instance
            action = getActionInstance(instance, desc.methodName);

            if (action != null) {
                // Insertion of a button on Tool Bar?
                if (desc.buttonClassName != null) {
                    Class buttonClass = classLoader.loadClass(desc.buttonClassName);
                    AbstractButton button = (AbstractButton) buttonClass.newInstance();
                    button.setAction(action);
                    toolBar.add(button);
                    button.setBorder(UIUtil.getToolBorder());
                    button.setText("");
                }
            } else {
                logger.error("Unknown action {} in class {}", desc.methodName, desc.className);
            }
        } catch (ClassNotFoundException |
                 IllegalAccessException |
                 IllegalArgumentException |
                 InstantiationException |
                 SecurityException |
                 InvocationTargetException ex) {
            logger.warn("Error while registering " + desc, ex);
        }

        return action;
    }

    //-----------------------//
    // registerDomainActions //
    //-----------------------//
    @SuppressWarnings("unchecked")
    private void registerDomainActions (String domain,
                                        JMenu menu)
    {
        // Create all type sections for this menu
        for (int section : Actions.getSections()) {
            logger.debug("Starting section: {}", section);

            // Use a separator between sections
            menu.addSeparator();

            for (ActionDescriptor desc : Actions.getAllDescriptors()) {
                if (desc.domain.equalsIgnoreCase(domain) && (desc.section == section)) {
                    // Skip advanced topics, unless explicitly set
                    if ((desc.topic != null) && !desc.topic.isSet()) {
                        continue;
                    }

                    logger.debug("Registering {}", desc);

                    try {
                        // Allocate menu item of proper class
                        final Class<? extends JMenuItem> itemClass;

                        if (desc.itemClassName != null) {
                            itemClass = (Class<? extends JMenuItem>) classLoader.loadClass(
                                    desc.itemClassName);
                        } else if (desc.menuName != null) {
                            itemClass = JMenu.class;
                        } else {
                            itemClass = JMenuItem.class;
                        }

                        JMenuItem item = itemClass.newInstance();

                        // Inject menu item information
                        if (desc.methodName != null) {
                            item.setText(desc.methodName); // As default

                            ApplicationAction action = registerAction(desc);

                            if (action != null) {
                                action.setSelected(action.isSelected());
                                item.setAction(action);
                                menu.add(item);
                            } else {
                                logger.warn("Could not register {}", desc);
                            }
                        } else if (desc.menuName != null) {
                            item.setText(desc.menuName); // As default
                            item.setName(desc.menuName);
                            menu.add(item);
                        }
                    } catch (ClassNotFoundException |
                             IllegalAccessException |
                             InstantiationException ex) {
                        logger.warn("Error with " + desc.itemClassName, ex);
                    }
                }
            }
        }
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class in application.
     *
     * @return the instance
     */
    public static ActionManager getInstance ()
    {
        return LazySingleton.INSTANCE;
    }

    //---------------//
    // LazySingleton //
    //---------------//
    private static class LazySingleton
    {

        static final ActionManager INSTANCE = new ActionManager();
    }
}
