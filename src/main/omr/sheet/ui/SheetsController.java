//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S h e e t s C o n t r o l l e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.action.ActionManager;
import omr.action.Actions;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.lag.Lag;

import omr.selection.SelectionService;
import omr.selection.SheetEvent;

import omr.sheet.Book;
import omr.sheet.Sheet;

import omr.step.Step;

import omr.ui.Colors;
import omr.ui.util.SeparableMenu;

import omr.util.OmrExecutors;

import org.bushe.swing.event.EventSubscriber;

import org.jdesktop.application.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code SheetsController} is the UI Controller in charge of user interactions
 * with the sheets.
 * <p>
 * Multiple sheets are handled by means of a tabbed pane. For each tab, and thus for each sheet, we
 * have a separate {@link SheetAssembly}. All methods that access these shared entities (sheetsPane,
 * assemblies) are synchronized.
 * <p>
 * The sheetsPane plays with the foreground color of its tabs to indicate current sheet status:
 * <ul>
 * <li><span style="color:gray"><b>LIGHT_GRAY</b></span> as default color
 * (for a sheet just created and still empty).</li>
 * <li><span style="color:orange"><b>ORANGE</b></span> for a sheet on which early
 * steps, typically LOAD+BINARY, are being processed.</li>
 * <li><span style=""><b>BLACK</b></span> for a sheet ready.</li>
 * <li><span style="color:red"><b>RED</b></span> for a sheet where early steps
 * failed (e.g. for lack of memory on very large books).
 * Selecting the sheet tab again will re-launch those steps.</li>
 * </ul>
 * <p>
 * This class encapsulates an event service, which publishes the sheet currently selected by a user
 * interface. See {@link #subscribe}, {@link #unsubscribe} and {@link #getSelectedSheet}.
 * <p>
 * This class is meant to be a Singleton.
 *
 * @author Hervé Bitteur
 */
public class SheetsController
        implements ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SheetsController.class);

    /** Events that can be published on sheet service. */
    private static final Class<?>[] eventsWritten = new Class<?>[]{SheetEvent.class};

    /** The single instance of this class. */
    private static volatile SheetsController INSTANCE;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Ordered sequence of sheet assemblies. */
    private final ArrayList<SheetAssembly> assemblies;

    /** The concrete tabbed pane, one tab per sheet. */
    private final JTabbedPane sheetsPane;

    /** The global event service which publishes the currently selected sheet. */
    private final SelectionService sheetService = new SelectionService(
            getClass().getSimpleName(),
            eventsWritten);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create the SheetsController singleton.
     */
    private SheetsController ()
    {
        sheetsPane = new JTabbedPane();
        assemblies = new ArrayList<SheetAssembly>();

        // Listener on sheet tab operations
        sheetsPane.addChangeListener(this);
        sheetsPane.setForeground(Color.LIGHT_GRAY);

        // Key binding
        bindKeys();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // addAssembly //
    //-------------//
    /**
     * Add the provided sheet assembly.
     *
     * @param assembly the sheet assembly to add to tabbed pane
     */
    public synchronized void addAssembly (SheetAssembly assembly)
    {
        Sheet sheet = assembly.getSheet();
        logger.debug("addAssembly for {}", sheet);

        // Initial zoom ratio
        assembly.setZoomRatio(constants.initialZoomRatio.getValue());

        // Make sure the assembly is part of the tabbed pane
        int sheetIndex = sheetsPane.indexOfComponent(assembly.getComponent());

        if (sheetIndex == -1) {
            // Insert in tabbed pane
            assemblies.add(assembly);
            sheetsPane.addTab(
                    defineTitleFor(sheet),
                    null,
                    assembly.getComponent(),
                    sheet.getBook().getInputPath().toString());
        }
    }

    //----------------//
    // callAboutSheet //
    //----------------//
    /**
     * Call the attention about the provided sheet, by publishing it on
     * the proper event service.
     *
     * @param sheet the provided sheet, which may be null
     */
    public void callAboutSheet (Sheet sheet)
    {
        sheetService.publish(new SheetEvent(this, null, null, sheet));
    }

    //-------------------//
    // dumpAllAssemblies //
    //-------------------//
    @Action
    public synchronized void dumpAllAssemblies ()
    {
        for (SheetAssembly assembly : assemblies) {
            logger.info("Assembly of {} {}", assembly.getSheet(), assembly);
        }
    }

    //--------------------------//
    // dumpCurrentSheetServices //
    //--------------------------//
    /**
     * Debug action to dump the current status of all event services
     * related to the selected sheet if any.
     */
    public void dumpCurrentSheetServices ()
    {
        Sheet sheet = getSelectedSheet();
        logger.info("Selection services of {}", sheet);

        if (sheet == null) {
            return;
        }

        sheet.getLocationService().dumpSubscribers();

        for (Lag lag : sheet.getLagManager().getAllLags()) {
            if (lag != null) {
                lag.getSectionService().dumpSubscribers();
                lag.getRunService().dumpSubscribers();
            }
        }

        if (sheet.getGlyphNest() != null) {
            sheet.getGlyphNest().getGlyphService().dumpSubscribers();
        }
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Give access to the real pane (to insert in proper UI hierarchy).
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return sheetsPane;
    }

    //----------------//
    // getCurrentBook //
    //----------------//
    /**
     * Convenient method to get the current book instance.
     *
     * @return the current book instance, or null
     */
    public static Book getCurrentBook ()
    {
        Sheet sheet = getCurrentSheet();

        if (sheet != null) {
            return sheet.getBook();
        }

        return null;
    }

    //-----------------//
    // getCurrentSheet //
    //-----------------//
    /**
     * Convenient static method to directly report the currently selected sheet, if any.
     *
     * @return the selected sheet, or null
     */
    public static Sheet getCurrentSheet ()
    {
        return getInstance().getSelectedSheet();
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class.
     *
     * @return the single instance
     */
    public static SheetsController getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new SheetsController();
        }

        return INSTANCE;
    }

    //------------------//
    // getSelectedSheet //
    //------------------//
    /**
     * Convenient method to directly access the currently selected sheet, if any.
     *
     * @return the selected sheet, which may be null (if no sheet is selected)
     */
    public Sheet getSelectedSheet ()
    {
        SheetEvent sheetEvent = (SheetEvent) sheetService.getLastEvent(SheetEvent.class);

        return (sheetEvent != null) ? sheetEvent.getData() : null;
    }

    //---------//
    // markTab //
    //---------//
    /**
     * Set the sheet tab using provided foreground color
     *
     * @param sheet sheet at hand
     * @param color color for sheet tab
     */
    public void markTab (Sheet sheet,
                         Color color)
    {
        logger.debug("mark {} in {}", sheet, color);

        int sheetIndex = sheetsPane.indexOfComponent(sheet.getAssembly().getComponent());

        if (sheetIndex != -1) {
            sheetsPane.setForegroundAt(sheetIndex, color);
        }
    }

    //----------------//
    // removeAssembly //
    //----------------//
    /**
     * Remove the specified view from the tabbed pane.
     *
     * @param sheet the sheet to close
     */
    public synchronized void removeAssembly (Sheet sheet)
    {
        SheetAssembly assembly = sheet.getAssembly();
        int sheetIndex = sheetsPane.indexOfComponent(assembly.getComponent());

        if (sheetIndex != -1) {
            logger.debug("Removing assembly {}", sheet);

            // Let others know (if this closing sheet was the current one)
            if (sheet == getSelectedSheet()) {
                callAboutSheet(null);
            }

            // Remove from assemblies
            assemblies.remove(sheetIndex);

            // Remove from tabs
            sheetsPane.remove(sheetIndex);

            Book book = sheet.getBook();

            // Make sure the first sheet of a multipage score is OK
            // We need to modify the tab label for the book (new) first tab
            if (!book.getSheets().isEmpty()) {
                Sheet firstSheet = book.getSheets().get(0);
                int firstIndex = sheetsPane.indexOfComponent(
                        firstSheet.getAssembly().getComponent());

                if (firstIndex != -1) {
                    sheetsPane.setTitleAt(firstIndex, defineTitleFor(firstSheet));
                }
            }
        }
    }

    //--------------//
    // showAssembly //
    //--------------//
    /**
     * Display the assembly that relates to the specified sheet.
     *
     * @param sheet the sheet to be viewed (sheet cannot be null).
     */
    public synchronized void showAssembly (Sheet sheet)
    {
        logger.debug("showAssembly for {}", sheet);

        SheetAssembly assembly = sheet.getAssembly();

        // Make sure the assembly is part of the tabbed pane
        int sheetIndex = sheetsPane.indexOfComponent(assembly.getComponent());

        if (sheetIndex != -1) {
            sheetsPane.setSelectedIndex(sheetIndex);
        } else {
            logger.warn("No tab found for {}", sheet);
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * This method is called whenever the sheet selection is modified,
     * whether programmatically (by means of {@link #showAssembly})
     * or by user action (manual selection of the sheet tab).
     * <p>
     * Set the state (enabled or disabled) of all menu items that depend on status of current sheet.
     */
    @Override
    public synchronized void stateChanged (ChangeEvent e)
    {
        if (e.getSource() == sheetsPane) {
            final int sheetIndex = sheetsPane.getSelectedIndex();

            // User has selected a new sheet tab?
            if (sheetIndex != -1) {
                // Connect the new sheet tab
                sheetTabSelected(sheetIndex);
            }
        }
    }

    //-----------//
    // subscribe //
    //-----------//
    /**
     * Subscribe to the sheet event service (for the SheetEvent class).
     *
     * @param subscriber The subscriber to accept the events when published.
     */
    public void subscribe (EventSubscriber<SheetEvent> subscriber)
    {
        sheetService.subscribeStrongly(SheetEvent.class, subscriber);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return getClass().getSimpleName();
    }

    //-------------//
    // unsubscribe //
    //-------------//
    /**
     * Un-subscribe to the sheet event service (for the SheetEvent class).
     *
     * @param subscriber the entity to un-subscribe
     */
    public void unsubscribe (EventSubscriber<SheetEvent> subscriber)
    {
        sheetService.unsubscribe(SheetEvent.class, subscriber);
    }

    //----------//
    // bindKeys //
    //----------//
    /**
     * Bind a kew keyboard events to actions among assemblies.
     */
    private void bindKeys ()
    {
        final InputMap inputMap = sheetsPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap actionMap = sheetsPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), "PageUpAction");
        actionMap.put("PageUpAction", new PageUpAction());

        inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "PageDownAction");
        actionMap.put("PageDownAction", new PageDownAction());

        inputMap.put(KeyStroke.getKeyStroke("control HOME"), "CtrlHomeAction");
        actionMap.put("CtrlHomeAction", new CtrlHomeAction());

        inputMap.put(KeyStroke.getKeyStroke("control END"), "CtrlEndAction");
        actionMap.put("CtrlEndAction", new CtrlEndAction());
    }

    //------------------//
    // checkSheetStatus //
    //------------------//
    /**
     * Check whether the selected sheet is "visible" and, if not so, launch the proper
     * early steps on the sheet.
     *
     * @param sheet      the sheet at hand
     * @param sheetIndex the corresponding tab index in sheetsPane
     */
    private void checkSheetStatus (final Sheet sheet,
                                   final int sheetIndex)
    {
        logger.debug("checkSheetStatus for {}", sheet);

        Step currentStep = sheet.getCurrentStep();

        if (currentStep == null) {
            final Step earlyStep = Step.valueOf(constants.earlyStep.getValue());

            if ((earlyStep != null) && !sheet.isDone(earlyStep)) {
                // Process sheet asynchronously
                Callable<Void> task = new Callable<Void>()
                {
                    @Override
                    public Void call ()
                            throws Exception
                    {
                        boolean ok = sheet.doStep(earlyStep, null);
                        markTab(sheet, ok ? Color.BLACK : Color.RED);

                        return null;
                    }

                    @Override
                    public String toString ()
                    {
                        return earlyStep + " for " + sheet;
                    }
                };

                logger.debug("launching {}", task);
                sheetsPane.setForegroundAt(sheetIndex, Colors.SHEET_BUSY);

                if (SwingUtilities.isEventDispatchThread()) {
                    OmrExecutors.getCachedLowExecutor().submit(task);
                } else {
                    try {
                        task.call();
                    } catch (Exception ex) {
                        logger.warn("Error in synchronous call to " + task, ex);
                    }
                }
            }
        }
    }

    //----------------//
    // defineTitleFor //
    //----------------//
    /**
     * Generate proper tab title for the provided sheet.
     *
     * @param sheet the provided sheet instance
     * @return the title to use for the related tab
     */
    private String defineTitleFor (Sheet sheet)
    {
        Book book = sheet.getBook();
        int index = sheet.getIndex();

        if (book.isMultiSheet()) {
            if (sheet.getIndex() == 1) {
                return book.getRadix() + "#" + index;
            } else {
                return "#" + index;
            }
        } else {
            if (index != 1) {
                return book.getRadix() + "#" + index;
            } else {
                return book.getRadix();
            }
        }
    }

    //----------------//
    // injectBookMenu //
    //----------------//
    /**
     * If this sheet belongs to a multi-sheet book, make sure proper book sub-menu is
     * inserted at end of sheet menu.
     *
     * @param sheet the sheet at hand
     */
    private void injectBookMenu (final Sheet sheet)
    {
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    @Override
                    public void run ()
                    {
                        final ActionManager mgr = ActionManager.getInstance();
                        final JMenu topMenu = mgr.getMenu(Actions.Domain.SHEET.name());
                        final String subMenuName = Actions.Domain.BOOK.name();
                        final JMenu subMenu = mgr.getMenu(subMenuName);

                        final int count = topMenu.getMenuComponentCount();
                        final String lastName = topMenu.getMenuComponent(count - 1).getName();

                        if (sheet.getBook().isMultiSheet()) {
                            // Make sure the last item in top menu is the sub-menu
                            if ((lastName == null) || !lastName.equals(subMenuName)) {
                                topMenu.addSeparator();
                                topMenu.add(subMenu);
                            }
                        } else {
                            // Remove the sub-menu if any is found at end of the top-menu
                            if ((lastName != null) && lastName.equals(subMenuName)) {
                                topMenu.remove(subMenu);
                                SeparableMenu.trimSeparator(topMenu);
                            }
                        }
                    }
                });
    }

    //------------------//
    // sheetTabSelected //
    //------------------//
    /**
     * Run when a sheetTab has been selected in the sheetsPane.
     *
     * @param sheetIndex the index of the tab
     */
    private void sheetTabSelected (int sheetIndex)
    {
        // Remember the new selected sheet
        SheetAssembly assembly = assemblies.get(sheetIndex);
        Sheet sheet = assembly.getSheet();

        if (!sheet.getBook().isClosing()) {
            // Inject book submenu only if the containing book is a multi-sheet book
            injectBookMenu(sheet);

            // Check whether we should run early steps on the sheet
            checkSheetStatus(sheet, sheetIndex);

            // Tell everyone about the new selected sheet
            callAboutSheet(sheet);

            // Tell the selected assembly that it now has the focus...
            assembly.assemblySelected();
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.String earlyStep = new Constant.String(
                "BINARY",
                "Early step triggered when an empty sheet tab is selected ");

        private final Constant.Ratio initialZoomRatio = new Constant.Ratio(
                0.5,
                "Initial zoom ratio for displayed sheet pictures");
    }

    //---------------//
    // CtrlEndAction //
    //---------------//
    private class CtrlEndAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            int count = sheetsPane.getComponentCount();

            if (count > 0) {
                sheetsPane.setSelectedIndex(count - 1);
            }
        }
    }

    //----------------//
    // CtrlHomeAction //
    //----------------//
    private class CtrlHomeAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            int count = sheetsPane.getComponentCount();

            if (count > 0) {
                sheetsPane.setSelectedIndex(0);
            }
        }
    }

    //----------------//
    // PageDownAction //
    //----------------//
    private class PageDownAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            int index = sheetsPane.getSelectedIndex();
            int count = sheetsPane.getComponentCount();

            if (index < (count - 1)) {
                sheetsPane.setSelectedIndex(index + 1);
            }
        }
    }

    //--------------//
    // PageUpAction //
    //--------------//
    private class PageUpAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            int index = sheetsPane.getSelectedIndex();

            if (index > 0) {
                sheetsPane.setSelectedIndex(index - 1);
            }
        }
    }
}
