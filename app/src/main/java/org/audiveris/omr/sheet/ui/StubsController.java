//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S t u b s C o n t r o l l e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.ViewParameters;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.selection.StubEvent;
import org.audiveris.omr.ui.util.WaitingTask;
import org.audiveris.omr.util.OmrExecutors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bushe.swing.event.EventSubscriber;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class <code>StubsController</code> is the UI Controller in charge of user interactions
 * with the sheets stubs.
 * <p>
 * Multiple stubs are handled by means of a tabbed pane. For each tab, and thus for each stub, we
 * have a separate {@link SheetAssembly}.
 * NOTA: All methods that access the tabbed pane must be called only from Swing EDT.
 * <p>
 * The stubsPane plays with the foreground color of its tabs to indicate current sheet stub status:
 * <ul>
 * <li><span style="color:gray"><b>LIGHT_GRAY</b></span> as default color
 * (for a stub just created and still empty).</li>
 * <li><span style="color:orange"><b>ORANGE</b></span> for a stub on which early steps,
 * typically LOAD+BINARY, are being processed.</li>
 * <li><span style=""><b>BLACK</b></span> for a sheet ready.</li>
 * <li><span style="color:red"><b>RED</b></span> for a sheet stub where early steps
 * failed (e.g. for lack of memory on very large books).
 * Selecting the tab again will re-launch those steps.</li>
 * <li><span style="color:pink"><b>PINK</b></span> for a sheet stub flagged as invalid.</li>
 * </ul>
 * <p>
 * This class encapsulates an event service, which publishes the stub currently selected by a user
 * interface. See {@link #subscribe}, {@link #unsubscribe} and {@link #getSelectedStub}.
 * <p>
 * This class is meant to be a Singleton.
 *
 * @author Hervé Bitteur
 */
public class StubsController
        implements ChangeListener, PropertyChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StubsController.class);

    /** Events that can be published on sheet service. */
    private static final Class<?>[] eventsWritten = new Class<?>[] { StubEvent.class };

    //~ Instance fields ----------------------------------------------------------------------------

    /** The concrete tabbed pane, one tab per sheet stub. */
    private final JTabbedPane stubsPane;

    /** Reverse map: component -> Stub. */
    private final Map<JComponent, SheetStub> stubsMap;

    /** The global event service which publishes the currently selected sheet stub. */
    private final SelectionService stubService = new SelectionService("stubService", eventsWritten);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create the <code>StubsController</code> singleton.
     */
    private StubsController ()
    {
        stubsMap = new HashMap<>();

        stubsPane = new JTabbedPane();
        stubsPane.setBorder(null);
        stubsPane.setForeground(Colors.SHEET_NOT_LOADED);

        // Listener on sheet tab operations
        stubsPane.addChangeListener(this);

        // Listener on invalid sheets display
        ViewParameters.getInstance().addPropertyChangeListener(
                ViewParameters.INVALID_SHEET_DISPLAY,
                this);

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
     * @param index    desired index, null for end
     */
    public void addAssembly (SheetAssembly assembly,
                             Integer index)
    {
        SheetStub stub = assembly.getStub();
        logger.debug("addAssembly for {}", stub);

        // Initial zoom ratio
        assembly.setZoomRatio(constants.initialZoomRatio.getValue());

        // Insert in reverse map and in tabbed pane
        stubsMap.put(assembly.getComponent(), stub);
        insertAssembly(stub, (index != null) ? index : stubsPane.getTabCount());
    }

    //----------------//
    // adjustStubTabs //
    //----------------//
    /**
     * Adjust color and title for tab of each displayed stub in the provided book.
     *
     * @param book the provided book
     */
    public void adjustStubTabs (Book book)
    {
        SheetStub firstDisplayed = null;

        for (SheetStub stub : book.getStubs()) {
            int tabIndex = stubsPane.indexOfComponent(stub.getAssembly().getComponent());

            if (tabIndex != -1) {
                if (firstDisplayed == null) {
                    firstDisplayed = stub;
                }

                if (!stub.isValid()) {
                    stubsPane.setForegroundAt(tabIndex, Colors.SHEET_INVALID);
                }

                stubsPane.setTitleAt(tabIndex, defineTitleFor(stub, firstDisplayed));
            }
        }
    }

    //----------//
    // bindKeys //
    //----------//
    /**
     * Bind a few keyboard events to actions among assemblies.
     */
    private void bindKeys ()
    {
        final InputMap inputMap = stubsPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap actionMap = stubsPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), "PageUpAction");
        actionMap.put("PageUpAction", new PageUpAction());

        inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "PageDownAction");
        actionMap.put("PageDownAction", new PageDownAction());

        inputMap.put(KeyStroke.getKeyStroke("control HOME"), "CtrlHomeAction");
        actionMap.put("CtrlHomeAction", new CtrlHomeAction());

        inputMap.put(KeyStroke.getKeyStroke("control END"), "CtrlEndAction");
        actionMap.put("CtrlEndAction", new CtrlEndAction());
    }

    //---------------//
    // callAboutStub //
    //---------------//
    /**
     * Call the attention about the provided sheet stub, by publishing it on
     * the proper event service.
     *
     * @param stub the provided sheet stub, which may be null
     */
    public void callAboutStub (SheetStub stub)
    {
        stubService.publish(new StubEvent(this, null, null, stub));
    }

    //-----------------//
    // checkStubStatus //
    //-----------------//
    /**
     * Check whether the selected sheet is visible and, if not so, launch the proper
     * early steps on the sheet.
     * <p>
     * Method is called on non-EDT task.
     *
     * @param stub  the sheet at hand
     * @param early if true, perform early steps on stub
     */
    private void checkStubStatus (final SheetStub stub,
                                  final boolean early)
    {
        logger.debug("stateChanged/checkStubStatus on {}", stub);

        // If stub lock is not free, then there is some processing going on on this stub.
        // Hence, give up and let this processing go.
        // Otherwise, launch the performing of the early steps.
        if (stub.getLock().tryLock()) {
            try {
                LogUtil.start(stub);
                logger.debug("checkStubStatus got lock on {}", stub);

                if (early) {
                    final OmrStep earlyStep = getEarlyStep();

                    if (earlyStep != null) {
                        logger.debug("EarlyStep. reachStep {} on {}", earlyStep, stub);
                        stub.reachStep(earlyStep, false);
                    }
                }

                final Sheet sheet;

                if (!stub.hasSheet()) {
                    // Stub just loaded from book file, load & display the related sheet
                    logger.debug("get & display sheet for {}", stub);
                    sheet = loadSheet(stub);
                    markTab(stub, Colors.SHEET_OK);
                } else {
                    logger.debug("{} already has sheet", stub);
                    sheet = stub.getSheet();
                }

                sheet.displayMainTabs();
            } finally {
                logger.debug("checkStubStatus releasing lock on {}", stub);
                stub.getLock().unlock();
                LogUtil.stopStub();
            }
        } else {
            logger.debug("{} currently busy, checkStubStatus giving up.", stub);
        }
    }

    //----------------//
    // defineTitleFor //
    //----------------//
    /**
     * Generate proper tab title for the provided sheet.
     *
     * @param stub           the provided sheet stub instance
     * @param firstDisplayed the first displayed stub
     * @return the title to use for the related tab
     */
    private String defineTitleFor (SheetStub stub,
                                   SheetStub firstDisplayed)
    {
        final Book book = stub.getBook();
        final int number = stub.getNumber();

        if (book.isMultiSheet()) {
            if (stub == firstDisplayed) {
                return book.getRadix() + "#" + number;
            } else {
                return "#" + number;
            }
        } else if (number != 1) {
            return book.getRadix() + "#" + number;
        } else {
            return book.getRadix();
        }
    }

    //----------------//
    // deleteAssembly //
    //----------------//
    /**
     * Remove the assembly for the provided stub.
     *
     * @param stub the provided stub
     */
    public void deleteAssembly (SheetStub stub)
    {
        removeAssembly(stub); // Removed from stubsPane
        stubsMap.remove(stub.getAssembly().getComponent()); // Removed from stubsMap
    }

    //---------//
    // display //
    //---------//
    /**
     * Display/redisplay the provided stub.
     *
     * @param stub  the provided stub
     * @param early if true, perform the early steps on stub
     */
    public void display (final SheetStub stub,
                         final boolean early)
    {
        // Since we are on Swing EDT, use asynchronous processing
        OmrExecutors.getCachedLowExecutor().submit( () -> {
            try {
                LogUtil.start(stub);

                // Check whether we should run early steps on the sheet
                checkStubStatus(stub, early);

                SwingUtilities.invokeAndWait( () -> {
                    // Race condition: let's check we are working on the selected stub
                    SheetStub selected = getSelectedStub();

                    if (stub == selected) {
                        // Tell the selected assembly that it now has the focus
                        // (to display stub related boards and error pane)
                        stub.getAssembly().assemblySelected();

                        // Stub status
                        BookActions.getInstance().updateProperties(stub.getSheet());

                        // GUI: Perform sheets upgrade?
                        final Book book = stub.getBook();

                        if (!book.promptedForUpgrade() && !book.getStubsToUpgrade().isEmpty()) {
                            promptForUpgrades(stub, book.getStubsToUpgrade());
                        }
                    } else {
                        logger.debug("Too late for {}", stub);
                    }
                });

                return null;
            } finally {
                LogUtil.stopStub();
            }
        });
    }

    //--------------//
    // displayStubs //
    //--------------//
    /**
     * Display the stubs of the provided book, perhaps focusing on a certain stub,
     * and using current validity policy as defined by ViewParameters.
     *
     * @param book  the book to display
     * @param focus the stub number to focus upon, if any
     */
    public void displayStubs (Book book,
                              Integer focus)
    {
        displayStubs(
                book,
                focus,
                ViewParameters.getInstance().isInvalidSheetDisplay() ? null
                        : SheetStub.VALIDITY_CHECK);
    }

    //--------------//
    // displayStubs //
    //--------------//
    /**
     * Display the stubs of a book, focusing on a stub number if provided,
     * and respecting the provided validity check if any.
     *
     * @param book          the book to display
     * @param focus         the stub number to focus upon, if any
     * @param validityCheck the check, if any, to apply on each stub
     */
    public void displayStubs (Book book,
                              Integer focus,
                              Predicate<SheetStub> validityCheck)
    {
        // Make sure we are on EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait( () -> displayStubs(book, focus, validityCheck));
            } catch (InterruptedException | InvocationTargetException ex) {
                logger.warn("invokeAndWait error", ex);
            }

            return;
        }

        final List<SheetStub> stubs = book.getStubs();

        // First, look for target location in stubsPane: tabPivot
        int tabPivot = getFirstDisplayedStubIndex(book);

        // Corresponding index in book stubs (of either tabPivot of focusStub)
        Integer stubPivot = null;

        // Potential stub focus
        final SheetStub focusStub;

        if (tabPivot == -1) {
            // Nothing displayed for this book yet, hence append all stubs at the end
            // We begin by the focus stub if any, to avoid unnecessary sheet loading
            tabPivot = stubsPane.getTabCount();
            focusStub = ((focus != null) && ((validityCheck == null) || validityCheck.test(
                    book.getStub(focus)))) ? book.getStub(focus) : null;
        } else {
            focusStub = null;
            stubPivot = getStubAt(tabPivot).getNumber() - 1;
        }

        if (focusStub != null) {
            insertAssembly(focusStub, tabPivot);
            stubPivot = focusStub.getNumber() - 1;
        }

        // Position each stub tab WRT pivot
        for (int stubIndex = 0; stubIndex < stubs.size(); stubIndex++) {
            final SheetStub stub = stubs.get(stubIndex);
            final JComponent component = stub.getAssembly().getComponent();
            final int tabIndex = stubsPane.indexOfComponent(component);

            if ((validityCheck == null) || validityCheck.test(stub)) {
                if (tabIndex != -1) {
                    tabPivot = tabIndex; // Already displayed
                } else if ((stubPivot == null) || (stubIndex < stubPivot)) {
                    insertAssembly(stub, tabPivot++); // Display just before pivot
                } else {
                    insertAssembly(stub, ++tabPivot); // Display just after pivot
                }
            } else {
                // Remove unwanted displayed stub
                if (tabIndex != -1) {
                    deleteAssembly(stub);
                    tabPivot--;
                }
            }
        }

        // Adjust color & title for each stub
        adjustStubTabs(book);
    }

    //--------------------------//
    // dumpCurrentSheetServices //
    //--------------------------//
    /**
     * Debug action to dump the current status of all event services related to
     * the selected sheet if any.
     */
    public void dumpCurrentSheetServices ()
    {
        SheetStub stub = getSelectedStub();
        logger.info("Selection services of {}", stub);

        if (stub == null) {
            return;
        }

        if (stub.hasSheet()) {
            final Sheet sheet = stub.getSheet();

            if (sheet.getLocationService() != null) {
                sheet.getLocationService().dumpSubscribers();
            } else {
                logger.info("No locationService");
            }

            for (Lag lag : sheet.getLagManager().getAllLags()) {
                if (lag != null) {
                    if (lag.getEntityService() != null) {
                        lag.getEntityService().dumpSubscribers();
                    }

                    if (lag.getRunService() != null) {
                        lag.getRunService().dumpSubscribers();
                    }
                }
            }

            if (sheet.getFilamentIndex().getEntityService() != null) {
                sheet.getFilamentIndex().getEntityService().dumpSubscribers();
            } else {
                logger.info("No filamentService");
            }

            if (sheet.getGlyphIndex().getEntityService() != null) {
                sheet.getGlyphIndex().getEntityService().dumpSubscribers();
            } else {
                logger.info("No glyphService");
            }

            if (sheet.getInterIndex() != null) {
                if (sheet.getInterIndex().getEntityService() != null) {
                    sheet.getInterIndex().getEntityService().dumpSubscribers();
                } else {
                    logger.info("No interService");
                }
            }
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
    public JTabbedPane getComponent ()
    {
        return stubsPane;
    }

    //----------------------------//
    // getFirstDisplayedStubIndex //
    //----------------------------//
    /**
     * Report the index of the first displayed stub (valid or not) from the given book.
     *
     * @param book the provided book
     * @return the index in stubsPane of first displayed stub or -1 if none
     */
    private int getFirstDisplayedStubIndex (Book book)
    {
        for (SheetStub stub : book.getStubs()) {
            final JComponent component = stub.getAssembly().getComponent();
            final int tabIndex = stubsPane.indexOfComponent(component);

            if (tabIndex != -1) {
                return tabIndex;
            }
        }

        return -1;
    }

    //----------//
    // getIndex //
    //----------//
    /**
     * Report the index in stubsPane of the provided stub.
     *
     * @param stub the stub of interest
     * @return the stub index in stubsPane
     */
    public Integer getIndex (SheetStub stub)
    {
        return stubsPane.indexOfComponent(stub.getAssembly().getComponent());
    }

    //--------------//
    // getLastIndex //
    //--------------//
    /**
     * Report the index of last tab.
     *
     * @return the index of last tab
     */
    public int getLastIndex ()
    {
        return stubsPane.getTabCount() - 1;
    }

    //-----------------//
    // getSelectedStub //
    //-----------------//
    /**
     * Convenient method to directly access the currently selected sheet stub, if any.
     *
     * @return the selected sheet stub, which may be null (if none is selected)
     */
    public SheetStub getSelectedStub ()
    {
        StubEvent stubEvent = (StubEvent) stubService.getLastEvent(StubEvent.class);

        return (stubEvent != null) ? stubEvent.getData() : null;
    }

    //-----------//
    // getStubAt //
    //-----------//
    /**
     * Report the stub displayed at provided index
     *
     * @param tabIndex the provided index (assumed to be a valid number) in stubsPane
     * @return the corresponding stub
     */
    private SheetStub getStubAt (int tabIndex)
    {
        JComponent component = (JComponent) stubsPane.getComponentAt(tabIndex);

        return stubsMap.get(component);
    }

    //----------------//
    // insertAssembly //
    //----------------//
    /**
     * Insert the assembly of provided stub at the desired index in stubsPane
     *
     * @param stub  the assembly to insertAssembly
     * @param index target index in stubsPane
     */
    private void insertAssembly (SheetStub stub,
                                 int index)
    {
        final JComponent component = stub.getAssembly().getComponent();
        stubsMap.put(component, stub);
        stubsPane.insertTab(
                defineTitleFor(stub, null),
                null,
                component,
                stub.getSheetInput().toString(),
                index);
    }

    //-------------//
    // isDisplayed //
    //-------------//
    /**
     * Report whether the provided book is currently displayed.
     *
     * @param book the provided book to check
     * @return true if so
     */
    public boolean isDisplayed (Book book)
    {
        for (SheetStub stub : stubsMap.values()) {
            if (stub.getBook() == book) {
                return true;
            }
        }

        return false;
    }

    //-----------//
    // loadSheet //
    //-----------//
    /**
     * Load the sheet while displaying a waiting dialog if loading takes time.
     *
     * @param stub the sheet stub
     * @return the loaded sheet
     */
    private Sheet loadSheet (SheetStub stub)
    {
        try {
            final LoadingSheetTask task = new LoadingSheetTask(stub);
            task.execute();
            final Sheet sheet = task.get();
            task.finished();
            return sheet;
        } catch (InterruptedException | ExecutionException ex) {
            logger.warn("Error loading sheet {}", ex.toString(), ex);
            return null;
        }
    }

    //---------//
    // markTab //
    //---------//
    /**
     * Set the stub tab using provided foreground color
     *
     * @param stub  sheet at hand
     * @param color color for sheet tab
     */
    public void markTab (final SheetStub stub,
                         final Color color)
    {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait( () -> markTab(stub, color));
            } catch (InterruptedException | InvocationTargetException ex) {
                logger.warn("invokeAndWait error", ex);
            }
        } else {
            logger.debug("mark {} with {}", stub, color);

            int tabIndex = stubsPane.indexOfComponent(stub.getAssembly().getComponent());

            if (tabIndex != -1) {
                stubsPane.setForegroundAt(tabIndex, color);
            }
        }
    }

    //-------------------//
    // promptForUpgrades //
    //-------------------//
    private void promptForUpgrades (final SheetStub stub,
                                    final Set<SheetStub> stubsToUpgrade)
    {
        final Book book = stub.getBook();

        // Dialog title
        final int size = stubsToUpgrade.size();
        final boolean plural = size > 1;
        final String title = "Upgrade needed for " + size + " sheet" + (plural ? "s" : "") + " in "
                + book.getRadix() + " book";

        // Dialog message
        final String question = "Should we upgrade and store the whole book in background?";

        SwingUtilities.invokeLater( () -> {
            if (OMR.gui.displayConfirmation(question, title)) {
                new SwingWorker<Void, Void>()
                {
                    @Override
                    protected Void doInBackground ()
                        throws Exception
                    {
                        try {
                            LogUtil.start(book);

                            book.upgradeStubs();
                            return null;
                        } finally {
                            LogUtil.stopBook();
                        }
                    }

                    @Override
                    protected void done ()
                    {
                        logger.info("Upgrade completed for book " + book.getRadix());
                        BookActions.getInstance().setBookUpgradable(false);
                    }
                }.execute();
            }

            book.setPromptedForUpgrade();
        });
    }

    //----------------//
    // propertyChange //
    //----------------//
    /**
     * Called when INVALID_SHEET_DISPLAY property changes
     *
     * @param evt not used
     */
    @Override
    public void propertyChange (PropertyChangeEvent evt)
    {
        if (ViewParameters.getInstance().isInvalidSheetDisplay()) {
            // Display all sheets for all books, including invalid ones
            for (Book book : OMR.engine.getAllBooks()) {
                displayStubs(book, null, null);
            }
        } else {
            // Hide invalid sheets for all books
            for (Book book : OMR.engine.getAllBooks()) {
                for (SheetStub stub : book.getStubs()) {
                    if (!stub.isValid()) {
                        removeAssembly(stub);
                    }
                }
            }
        }
    }

    //---------//
    // refresh //
    //---------//
    /**
     * Convenient method to refresh current stub properties.
     */
    public void refresh ()
    {
        logger.trace("StubsController refresh()");
        callAboutStub(getSelectedStub());
    }

    //----------------//
    // removeAssembly //
    //----------------//
    /**
     * Remove the specified view from the tabbed pane (but the view is not deleted yet).
     *
     * @param stub the sheet stub to close
     */
    public void removeAssembly (SheetStub stub)
    {
        SheetAssembly assembly = stub.getAssembly();
        int tabIndex = stubsPane.indexOfComponent(assembly.getComponent());

        if (tabIndex != -1) {
            logger.debug("Removing assembly {}", stub);

            // Remove from tabs (keep map)
            stubsPane.remove(tabIndex);

            // Make sure the first sheet of a multipage score is OK
            // We need to modify the tab label for the book (new) first tab
            Book book = stub.getBook();
            updateFirstStubTitle(book);
        }

        // Empty sheets cache?
        if (stubsPane.getTabCount() == 0) {
            callAboutStub(null); // No more current sheet!
        }
    }

    //----------------//
    // selectAssembly //
    //----------------//
    /**
     * Select the assembly that relates to the specified stub.
     *
     * @param stub the stub to be displayed, perhaps null
     */
    public void selectAssembly (SheetStub stub)
    {
        logger.debug("selectAssembly for {}", stub);

        if (stub != null) {
            if (stub == getSelectedStub()) {
                logger.debug("{} already selected", stub);

                return;
            } else {
                logger.debug("current selection: {}", getSelectedStub());
            }

            // Make sure the assembly is part of the tabbed pane
            final int tabIndex = stubsPane.indexOfComponent(stub.getAssembly().getComponent());

            if (tabIndex != -1) {
                stubsPane.setSelectedIndex(tabIndex);
            } else {
                logger.warn("No tab found for {}", stub);
            }
        }
    }

    //-----------------//
    // selectOtherBook //
    //-----------------//
    /**
     * Select suitable stub of another book than the provided one (which is about to be
     * closed).
     * This is meant to avoid the automatic selection of a not-yet loaded sheet.
     *
     * @param currentBook the book about to close
     */
    public void selectOtherBook (final Book currentBook)
    {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait( () -> selectOtherBook(currentBook));
            } catch (InterruptedException | InvocationTargetException ex) {
                logger.warn("invokeAndWait error", ex);
            }
        } else {
            SheetStub currentStub = getCurrentStub();

            if (currentStub == null) {
                return;
            }

            int currentIndex = stubsPane.getSelectedIndex();

            // Look for a suitable stub on right
            for (int index = currentIndex + 1; index < stubsPane.getTabCount(); index++) {
                JComponent component = (JComponent) stubsPane.getComponentAt(index);
                SheetStub stub = stubsMap.get(component);

                if ((stub.getBook() != currentBook) && stub.hasSheet()) {
                    stubsPane.setSelectedIndex(index);

                    return;
                }
            }

            // Not found on right, so look for a suitable stub on left
            for (int index = currentIndex - 1; index >= 0; index--) {
                JComponent component = (JComponent) stubsPane.getComponentAt(index);
                SheetStub stub = stubsMap.get(component);

                if ((stub.getBook() != currentBook) && stub.hasSheet()) {
                    stubsPane.setSelectedIndex(index);

                    return;
                }
            }
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * This method is called whenever the sheet selection is modified,
     * whether programmatically (by means of {@link #selectAssembly} or {@link
     * SheetStub#reset()} or by user action (manual selection of the sheet tab).
     * <p>
     * This method is run on EDT.
     */
    @Override
    public void stateChanged (ChangeEvent e)
    {
        // Did user select a new stub?
        JComponent component = (JComponent) stubsPane.getSelectedComponent();

        if (component == null) {
            return;
        }

        final SheetStub stub = stubsMap.get(component);
        logger.debug("stateChanged {}", stub);

        if (stub == getSelectedStub()) {
            return;
        }

        if (!stub.getBook().isClosing()) {
            logger.debug("stateChanged for non-selected {}", stub);

            // This is the new current stub
            callAboutStub(stub);

            display(stub, true);
        }
    }

    //-----------//
    // subscribe //
    //-----------//
    /**
     * Subscribe to the sheet event service (for the StubEvent class).
     *
     * @param subscriber The subscriber to accept the events when published.
     */
    public void subscribe (EventSubscriber<StubEvent> subscriber)
    {
        stubService.subscribeStrongly(StubEvent.class, subscriber);
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
     * Un-subscribe to the sheet event service (for the StubEvent class).
     *
     * @param subscriber the entity to un-subscribe
     */
    public void unsubscribe (EventSubscriber<StubEvent> subscriber)
    {
        stubService.unsubscribe(StubEvent.class, subscriber);
    }

    //----------------------//
    // updateFirstStubTitle //
    //----------------------//
    /**
     * Update the title of first displayed stub tab, according to book radix.
     *
     * @param book the book at hand
     */
    public void updateFirstStubTitle (Book book)
    {
        final int tabIndex = getFirstDisplayedStubIndex(book);

        if (tabIndex != -1) {
            SheetStub firstDisplayed = getStubAt(tabIndex);
            stubsPane.setTitleAt(tabIndex, defineTitleFor(firstDisplayed, firstDisplayed));
            stubsPane.invalidate();
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //----------------//
    // getCurrentBook //
    //----------------//
    /**
     * Convenient method to get the current book instance, if any.
     *
     * @return the current book instance, or null
     */
    public static Book getCurrentBook ()
    {
        SheetStub stub = getCurrentStub();

        if (stub == null) {
            return null;
        }

        return stub.getBook();
    }

    //----------------//
    // getCurrentStub //
    //----------------//
    /**
     * Convenient method to get the currently selected sheet stub, if any.
     *
     * @return the selected stub, or null
     */
    public static SheetStub getCurrentStub ()
    {
        return getInstance().getSelectedStub();
    }

    //--------------//
    // getEarlyStep //
    //--------------//
    /**
     * Report the step run by default on every new stub displayed.
     *
     * @return the default target step
     */
    public static OmrStep getEarlyStep ()
    {
        return constants.earlyStep.getValue();
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class in application.
     *
     * @return the instance
     */
    public static StubsController getInstance ()
    {
        return LazySingleton.INSTANCE;
    }

    //--------------//
    // invokeSelect //
    //--------------//
    /**
     * Delegate to EDT the selection of provided stub.
     *
     * @param stub the stub to select
     */
    public static void invokeSelect (final SheetStub stub)
    {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait( () -> invokeSelect(stub));
            } catch (InterruptedException | InvocationTargetException ex) {
                logger.warn("invokeAndWait error", ex);
            }
        } else {
            StubsController.getInstance().selectAssembly(stub);
        }
    }

    //--------------//
    // setEarlyStep //
    //--------------//
    /**
     * Set the step run by default on every new stub displayed.
     *
     * @param step the default target step
     */
    public static void setEarlyStep (OmrStep step)
    {
        if (step != getEarlyStep()) {
            constants.earlyStep.setValue(step);
            logger.info("Early step is now: {}", step);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Enum<OmrStep> earlyStep = new Constant.Enum<>(
                OmrStep.class,
                OmrStep.BINARY,
                "Early step triggered when an empty stub tab is selected ");

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
        @Override
        public void actionPerformed (ActionEvent e)
        {
            int count = stubsPane.getComponentCount();

            if (count > 0) {
                stubsPane.setSelectedIndex(count - 1);
            }
        }
    }

    //----------------//
    // CtrlHomeAction //
    //----------------//
    private class CtrlHomeAction
            extends AbstractAction
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            if (stubsPane.getComponentCount() > 0) {
                stubsPane.setSelectedIndex(0);
            }
        }
    }

    //---------------//
    // LazySingleton //
    //---------------//
    private static class LazySingleton
    {
        static final StubsController INSTANCE = new StubsController();

        private LazySingleton ()
        {
        }
    }

    //------------------//
    // LoadingSheetTask //
    //------------------//
    private class LoadingSheetTask
            extends WaitingTask<Sheet, Void>
    {
        final SheetStub stub;

        LoadingSheetTask (SheetStub stub)
        {
            super(OmrGui.getApplication(), "Loading sheet " + stub.getId() + " ...");
            this.stub = stub;
        }

        @Override
        protected Sheet doInBackground ()
            throws Exception
        {
            return stub.getSheet();
        }

        @Override
        public void finished ()
        {
            super.finished(); // Safer
        }
    }

    //----------------//
    // PageDownAction //
    //----------------//
    private class PageDownAction
            extends AbstractAction
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            int tabIndex = stubsPane.getSelectedIndex();

            if (tabIndex < (stubsPane.getComponentCount() - 1)) {
                stubsPane.setSelectedIndex(tabIndex + 1);
            }
        }
    }

    //--------------//
    // PageUpAction //
    //--------------//
    private class PageUpAction
            extends AbstractAction
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            int tabIndex = stubsPane.getSelectedIndex();

            if (tabIndex > 0) {
                stubsPane.setSelectedIndex(tabIndex - 1);
            }
        }
    }
}
