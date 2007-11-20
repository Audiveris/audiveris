//----------------------------------------------------------------------------//
//                                                                            //
//                         A c t i o n M a n a g e r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.ui;

import omr.plugin.*;
import omr.plugin.PluginType.Range;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionObserver;
import omr.selection.SelectionTag;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.ui.util.SeparableMenu;
import omr.ui.util.SeparableToolBar;
import omr.ui.util.UIUtilities;

import omr.util.Implement;
import omr.util.Logger;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.swing.*;

/**
 * Class <code>ActionManager</code> handles the instantiation and dressing of
 * actions, their organization in the menus and the tool bar, and their
 * enabling.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ActionManager
    implements SelectionObserver
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ActionManager.class);

    /** Singleton */
    private static ActionManager INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** The map of all menus, so that we can directly provide some */
    private final Map<String, JMenu> menuMap = new HashMap<String, JMenu>();

    /** The map of all handled actions */
    private final Map<String, Action> actionMap = new HashMap<String, Action>();

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
        SheetManager.getSelection()
                    .addObserver(this);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // getActionInstance //
    //-------------------//
    public Action getActionInstance (String actionName)
    {
        return actionMap.get(actionName);
    }

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
    @Implement(SelectionObserver.class)
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
    // registerAllActions //
    //--------------------//
    /**
     * Register all actions as listed in the plugins files, and organize them
     * according to the various ranges defined in the PluginType. There is one
     * pull-down menu generated for each range defined in PluginType.
     */
    public void registerAllActions ()
    {
        for (Range range : PluginType.getRanges()) {
            // Create dedicated menu for this range, if not already existing
            JMenu menu = menuMap.get(range.getName());

            if (menu == null) {
                if (logger.isFineEnabled()) {
                    logger.fine("Creating menu:" + range.getName());
                }

                menu = new SeparableMenu(range.getName());
                menuMap.put(range.getName(), menu);
            } else {
                logger.fine("Augmenting menu:" + range.getName());
            }

            // Dress up the menu itself
            UIDressing.dressUp(
                menu,
                PluginType.class.getName() + "." + range.getName());

            // Register all actions in the given range (menu)
            registerRangeActions(range, menu);

            toolBar.addSeparator();

            // Smart insertion of the menu into the menu bar
            if (menu.getItemCount() > 0) {
                if (range == PluginType.HelpTypes) {
                    menuBar.add(Box.createHorizontalStrut(50));
                }

                SeparableMenu.purgeSeparator(menu); // No orphan at end
                menuBar.add(menu);
            }
        }
    }

    //--------//
    // update //
    //--------//
    /**
     * Notification of sheet selection, to update frame title
     *
     * @param selection the selection object (SHEET)
     * @param hint processing hint (not used)
     */
    @Implement(SelectionObserver.class)
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        if (selection.getTag() == SelectionTag.SHEET) {
            Sheet sheet = (Sheet) selection.getEntity();
            enableSheetActions(sheet != null);
            enableScoreActions((sheet != null) && (sheet.getScore() != null));
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
     * action in the UI structure (menus and buttons) according to the class
     * plugin annotations.
     *
     * @param action the provided action class
     * @return the registered and dressed instance of the action class
     */
    private Action registerAction (Action action)
    {
        // Use information from plugin annotation
        Plugin plugin = action.getClass()
                              .getAnnotation(Plugin.class);

        if (plugin != null) {
            final String actionName = action.getClass()
                                            .getName();
            // Remember the action instance
            actionMap.put(actionName, action);

            // Dress the action according to local language
            UIDressing.dressUp(action, actionName);

            if (logger.isFineEnabled()) {
                logger.fine(
                    "type=" + plugin.type() + " dependency=" +
                    plugin.dependency() + " onToolbar=" + plugin.onToolbar());
            }

            // Dependency?
            switch (plugin.dependency()) {
            case SHEET_AVAILABLE :
                sheetDependentActions.add(action);
                action.setEnabled(false);

                break;

            case SCORE_AVAILABLE :
                scoreDependentActions.add(action);
                action.setEnabled(false);

                break;

            case NONE :
            }

            // Insertion on Tool Bar?
            if (plugin.onToolbar() &&
                (action.getValue(Action.SMALL_ICON) != null)) {
                JButton button = toolBar.add(action);
                button.setBorder(UIUtilities.getToolBorder());
            }
        } else {
            logger.severe("Attempt to build a non-plugin action");
        }

        return action;
    }

    //----------------------//
    // registerRangeActions //
    //----------------------//
    private void registerRangeActions (Range range,
                                       JMenu menu)
    {
        // Create all type sections for this menu
        for (PluginType type : range.getTypes()) {
            if (logger.isFineEnabled()) {
                logger.fine("Starting section: " + type);
            }

            menu.addSeparator();

            // Create all actions of the type section
            for (Class<?extends Action> actionClass : Plugins.getActions(type)) {
                if (logger.isFineEnabled()) {
                    logger.fine("Creating action: " + actionClass);
                }

                Annotation ann = actionClass.getAnnotation(Plugin.class);

                if (ann != null) {
                    try {
                        Class<?extends JMenuItem> itemClass = ((Plugin) ann).item();
                        JMenuItem                 item = itemClass.newInstance();
                        item.setAction(
                            registerAction(actionClass.newInstance()));
                        menu.add(item);
                    } catch (Exception ex) {
                        logger.warning("Cannot instantiate " + actionClass, ex);
                    }
                } else {
                    logger.warning(
                        "Class " + actionClass.getClass() +
                        " without plugin annotation");
                }
            }
        }
    }
}
