//----------------------------------------------------------------------------//
//                                                                            //
//                         A c t i o n M a n a g e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.action;

import omr.WellKnowns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import omr.ui.MainGui;
import omr.ui.util.SeparableMenu;
import omr.ui.util.UIUtil;

import omr.util.UriUtil;

import org.jdesktop.application.ApplicationAction;
import org.jdesktop.application.ResourceMap;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.xml.bind.JAXBException;

/**
 * Class {@code ActionManager} handles the instantiation and dressing
 * of actions, their organization in the menus and the tool bar, and
 * their enabling.
 *
 * @author Hervé Bitteur
 */
public class ActionManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(ActionManager.class);

    /** Class loader */
    private static final ClassLoader classLoader = ActionManager.class.
            getClassLoader();

    /** Singleton */
    private static volatile ActionManager INSTANCE;

    //~ Instance fields --------------------------------------------------------
    //
    /** The map of all menus, so that we can directly provide some. */
    private final Map<String, JMenu> menuMap = new HashMap<>();

    /** Collection of actions enabled only when a sheet is selected. */
    private final Collection<Action> sheetDependentActions = new ArrayList<>();

    /** Collection of actions enabled only when current score is available. */
    private final Collection<Action> scoreDependentActions = new ArrayList<>();

    /** The tool bar that hosts some actions. */
    private final JToolBar toolBar = new JToolBar();

    /** The menu bar for all actions. */
    private final JMenuBar menuBar = new JMenuBar();

    //~ Constructors -----------------------------------------------------------
    //
    //---------------//
    // ActionManager //
    //---------------//
    /**
     * Meant to be instantiated at most once.
     */
    private ActionManager ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single action manager instance.
     *
     * @return the unique instance of this class
     */
    public static ActionManager getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new ActionManager();
        }

        return INSTANCE;
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
        ActionMap actionMap = MainGui.getInstance().getContext().getActionMap(
                instance);

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
     * Load all descriptors as found in system and user configuration
     * files.
     */
    public void loadAllDescriptors ()
    {
        // Load classes first for system actions, then for user actions
        URI[] uris = new URI[]{
            UriUtil.toURI(WellKnowns.RES_URI, "system-actions.xml"),
            new File(WellKnowns.CONFIG_FOLDER, "user-actions.xml").toURI().normalize()};

        for (int i = 0; i < uris.length; i++) {
            URI uri = uris[i];
            try {
                URL url = uri.toURL();
                InputStream input = url.openStream();
                Actions.loadActionsFrom(input);
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
     * Register all actions as listed in the descriptor files, and
     * organize them according to the various domains defined.
     * There is one pull-down menu generated for each domain found.
     */
    public void registerAllActions ()
    {
        // Insert an initial separator, to let user easily grab the toolBar
        toolBar.addSeparator();

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
            ResourceMap resource = MainGui.getInstance().getContext().
                    getResourceMap(Actions.class);
            menu.setText(domain); // As default
            menu.setName(domain);

            // Register all actions in the given domain
            registerDomainActions(domain, menu);
            resource.injectComponents(menu);

            toolBar.addSeparator();

            // Smart insertion of the menu into the menu bar
            if (menu.getItemCount() > 0) {
                if (domain.equalsIgnoreCase("help")) {
                    menuBar.add(Box.createHorizontalStrut(50));
                }

                SeparableMenu.trimSeparator(menu); // No separator at end
                menuBar.add(menu);
            }
        }
    }

    //----------------//
    // registerAction //
    //----------------//
    /**
     * Allocate and dress an instance of the provided class, then
     * register the action in the UI structure (menus and buttons)
     * according to the action descriptor parameters.
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
                Method getInstance = classe.getDeclaredMethod(
                        "getInstance",
                        (Class[]) null);

                if (Modifier.isStatic(getInstance.getModifiers())) {
                    instance = getInstance.invoke(null);
                }
            } catch (NoSuchMethodException ignored) {
            }

            if (instance == null) {
                // Fall back to allocate a new class instance
                ///logger.warn("instantiating instance of " + classe);
                instance = classe.newInstance();
            }

            // Retrieve the action instance
            action = getActionInstance(instance, desc.methodName);

            if (action != null) {
                // Insertion of a button on Tool Bar?
                if (desc.buttonClassName != null) {
                    Class<? extends AbstractButton> buttonClass =
                            (Class<? extends AbstractButton>) classLoader.
                            loadClass(desc.buttonClassName);
                    AbstractButton button = buttonClass.newInstance();
                    button.setAction(action);
                    toolBar.add(button);
                    button.setBorder(UIUtil.getToolBorder());
                    button.setText("");
                }
            } else {
                logger.error("Unknown action {} in class {}",
                        desc.methodName, desc.className);
            }
        } catch (ClassNotFoundException | SecurityException |
                IllegalAccessException | IllegalArgumentException |
                InvocationTargetException | InstantiationException ex) {
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
                if (desc.domain.equalsIgnoreCase(domain)
                    && (desc.section == section)) {
                    logger.debug("Registering {}", desc);

                    try {
                        Class<? extends JMenuItem> itemClass;

                        if (desc.itemClassName != null) {
                            itemClass = (Class<? extends JMenuItem>) classLoader.
                                    loadClass(
                                    desc.itemClassName);
                        } else {
                            itemClass = JMenuItem.class;
                        }

                        JMenuItem item = itemClass.newInstance();
                        item.setText(desc.methodName);

                        ApplicationAction action = registerAction(desc);

                        if (action != null) {
                            action.setSelected(action.isSelected());
                            item.setAction(action);
                            menu.add(item);
                        } else {
                            logger.warn("Could not register {}", desc);
                        }
                    } catch (ClassNotFoundException | InstantiationException |
                            IllegalAccessException ex) {
                        logger.warn("Error with " + desc.itemClassName, ex);
                    }
                }
            }
        }
    }
}
