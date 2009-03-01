//----------------------------------------------------------------------------//
//                                                                            //
//                         A c t i o n M a n a g e r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.action;

import omr.Main;

import omr.log.Logger;

import omr.selection.MouseMovement;
import omr.selection.SheetEvent;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetsController;

import omr.ui.util.SeparableMenu;
import omr.ui.util.SeparableToolBar;
import omr.ui.util.UIUtilities;

import omr.util.Implement;

import org.bushe.swing.event.EventSubscriber;

import org.jdesktop.application.ApplicationAction;
import org.jdesktop.application.ResourceMap;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import javax.swing.*;
import javax.xml.bind.JAXBException;

/**
 * Class <code>ActionManager</code> handles the instantiation and dressing of
 * actions, their organization in the menus and the tool bar, and their
 * enabling.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ActionManager
    implements EventSubscriber<SheetEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ActionManager.class);

    /** Class loader */
    private static final ClassLoader classLoader = ActionManager.class.getClassLoader();

    /** Singleton */
    private static ActionManager INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** The map of all menus, so that we can directly provide some */
    private final Map<String, JMenu> menuMap = new HashMap<String, JMenu>();

    /** Collection of actions enabled only when a sheet is selected */
    private final Collection<Action> sheetDependentActions = new ArrayList<Action>();

    /** Collection of actions enabled only when current score is available */
    private final Collection<Action> scoreDependentActions = new ArrayList<Action>();

    /** The tool bar that hosts some actions */
    private final JToolBar toolBar = new SeparableToolBar();

    /** The menu bar for all actions */
    private final JMenuBar menuBar = new JMenuBar();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // ActionManager //
    //---------------//
    /**
     * Meant to be instantiated at most once
     */
    private ActionManager ()
    {
        // Stay informed on sheet selection, in order to enable sheet-dependent
        // actions accordingly
        SheetsController.getInstance()
                          .subscribe(this);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the action manager
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
     * Retrieve an action knowing its methodName
     *
     * @param instance the instance of the hosting class
     * @param methodName the method name
     * @return the action found, or null if none
     */
    public ApplicationAction getActionInstance (Object instance,
                                                String methodName)
    {
        ActionMap actionMap = Main.getInstance()
                                  .getContext()
                                  .getActionMap(instance);

        return (ApplicationAction) actionMap.get(methodName);
    }

    //---------//
    // getMenu //
    //---------//
    /**
     * Report the menu built for a given key
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
     * Report the bar containing all generated pull-down menus
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
     * Report a describing name
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
     * Report the tool bar containing all generated buttons
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
     * Insert a predefined menu, either partly or fully built
     *
     * @param key the menu unique name
     * @param menu the menu to inject
     */
    public void injectMenu (String key,
                            JMenu  menu)
    {
        menuMap.put(key, menu);
    }

    //--------------------//
    // loadAllDescriptors //
    //--------------------//
    /**
     * Load all descriptors as found in system and user configuration files,
     * either in local config subdirectory or in the resource hierarchy.
     */
    public void loadAllDescriptors ()
    {
        // Load classes first for system actions, then for user actions
        // We consider local folder first, then archive resources if needed
        for (String name : new String[] { "system-actions.xml", "user-actions.xml" }) {
            // Choose the proper input stream
            InputStream input = null;

            // Look for a local file
            File file = new File(Main.getConfigFolder(), name);

            if (file.exists()) {
                try {
                    input = new FileInputStream(file);
                } catch (FileNotFoundException ex) {
                    logger.warning("Cannot find " + file, ex);
                }
            }

            if (input == null) {
                // Then look for a resource
                input = getClass()
                            .getResourceAsStream("config/" + name);
            }

            if (input != null) {
                try {
                    Actions.loadActionsFrom(input);
                } catch (JAXBException ex) {
                    logger.warning("Error loading actions from " + name, ex);
                } finally {
                    try {
                        input.close();
                    } catch (IOException ignored) {
                    }
                }
            } else {
                logger.warning("No file and no resource found for " + name);
            }
        }
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Notification of sheet selection, to update frame title
     *
     * @param sheetEvent the event about sheet selection
     */
    @Implement(EventSubscriber.class)
    public void onEvent (SheetEvent sheetEvent)
    {
        try {
            // Ignore RELEASING
            if (sheetEvent.movement == MouseMovement.RELEASING) {
                return;
            }

            final Sheet sheet = sheetEvent.getData();
            enableSheetActions(sheet != null);
            enableScoreActions((sheet != null) && (sheet.getScore() != null));
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------------------//
    // registerAllActions //
    //--------------------//
    /**
     * Register all actions as listed in the descriptor files, and organize them
     * according to the various domains defined. There is one pull-down menu
     * generated for each domain found.
     */
    public void registerAllActions ()
    {
        for (String domain : Actions.getDomainNames()) {
            // Create dedicated menu for this range, if not already existing
            JMenu menu = menuMap.get(domain);

            if (menu == null) {
                if (logger.isFineEnabled()) {
                    logger.fine("Creating menu:" + domain);
                }

                menu = new SeparableMenu(domain);
                menuMap.put(domain, menu);
            } else {
                logger.fine("Augmenting menu:" + domain);
            }

            // Proper menu decoration
            ResourceMap resource = Main.getInstance()
                                       .getContext()
                                       .getResourceMap(Actions.class);
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

                SeparableMenu.purgeSeparator(menu); // No orphan at end
                menuBar.add(menu);
            }
        }
    }

    //--------------------//
    // enableScoreActions //
    //--------------------//
    private void enableScoreActions (boolean bool)
    {
        for (Action action : scoreDependentActions) {
            action.setEnabled(bool);
        }
    }

    //--------------------//
    // enableSheetActions //
    //--------------------//
    private void enableSheetActions (boolean bool)
    {
        for (Action action : sheetDependentActions) {
            action.setEnabled(bool);
        }
    }

    //----------------//
    // registerAction //
    //----------------//
    /**
     * Allocate and dress an instance of the provided class, then register the
     * action in the UI structure (menus and buttons) according to the action
     * descriptor parameters.
     *
     * @param action the provided action class
     * @return the registered and decorated instance of the action class
     */
    @SuppressWarnings("unchecked")
    private ApplicationAction registerAction (ActionDescriptor desc)
    {
        ApplicationAction action = null;

        try {
            // Retrieve proper class instance
            Class  classe = classLoader.loadClass(desc.className);
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
                ///logger.warning("instantiating instance of " + classe);
                instance = classe.newInstance();
            }

            // Retrieve the action instance
            action = getActionInstance(instance, desc.methodName);

            if (action != null) {
                // Insertion on Tool Bar?
                if ((desc.onToolbar != null) && desc.onToolbar) {
                    JButton button = toolBar.add(action);
                    button.setBorder(UIUtilities.getToolBorder());
                }
            } else {
                logger.severe(
                    "Unknown action " + desc.methodName + " in class " +
                    desc.className);
            }
        } catch (Throwable ex) {
            logger.warning("Error while registering action " + desc, ex);
        } finally {
            return action;
        }
    }

    //-----------------------//
    // registerDomainActions //
    //-----------------------//
    @SuppressWarnings("unchecked")
    private void registerDomainActions (String domain,
                                        JMenu  menu)
    {
        // Create all type sections for this menu
        for (String section : Actions.getSectionNames()) {
            if (logger.isFineEnabled()) {
                logger.fine("Starting section: " + section);
            }

            menu.addSeparator();

            for (ActionDescriptor desc : Actions.getAllDescriptors()) {
                if (desc.domain.equalsIgnoreCase(domain) &&
                    desc.section.equalsIgnoreCase(section)) {
                    if (logger.isFineEnabled()) {
                        logger.fine("Registering " + desc);
                    }

                    try {
                        Class<?extends JMenuItem> itemClass;

                        if (desc.itemClassName != null) {
                            itemClass = (Class<?extends JMenuItem>) classLoader.loadClass(
                                desc.itemClassName);
                        } else {
                            itemClass = JMenuItem.class;
                        }

                        JMenuItem item = itemClass.newInstance();
                        item.setText(desc.methodName);

                        ApplicationAction action = registerAction(desc);
                        action.setSelected(action.isSelected());

                        if (action != null) {
                            item.setAction(action);
                            menu.add(item);
                        } else {
                            logger.warning("Could not register " + desc);
                        }
                    } catch (InstantiationException ex) {
                        logger.warning(
                            "Cannot instantiate " + desc.itemClassName,
                            ex);
                    } catch (IllegalAccessException ex) {
                        logger.warning(
                            "Cannot access " + desc.itemClassName,
                            ex);
                    } catch (ClassNotFoundException ex) {
                        logger.warning(
                            "Cannot find class " + desc.itemClassName,
                            ex);
                    }
                }
            }
        }
    }
}
