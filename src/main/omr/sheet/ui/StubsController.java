//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S t u b s C o n t r o l l e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.OMR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.lag.Lag;

import omr.sheet.Book;
import omr.sheet.Sheet;
import omr.sheet.SheetStub;

import omr.step.Step;

import omr.ui.Colors;
import omr.ui.ViewParameters;
import omr.ui.selection.SelectionService;
import omr.ui.selection.StubEvent;

import omr.util.OmrExecutors;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code StubsController} is the UI Controller in charge of user interactions
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

    private static final Logger logger = LoggerFactory.getLogger(
            StubsController.class);

    /** Events that can be published on sheet service. */
    private static final Class<?>[] eventsWritten = new Class<?>[]{StubEvent.class};

    /** The single instance of this class. */
    private static volatile StubsController INSTANCE;

    //~ Instance fields ----------------------------------------------------------------------------
    /** The concrete tabbed pane, one tab per sheet stub. */
    private final JTabbedPane stubsPane;

    /** Reverse map: component -> Stub. */
    private final Map<JComponent, SheetStub> stubsMap;

    /** The global event service which publishes the currently selected sheet stub. */
    private final SelectionService stubService = new SelectionService("stubService", eventsWritten);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create the {@code StubsController} singleton.
     */
    private StubsController ()
    {
        stubsMap = new HashMap<JComponent, SheetStub>();

        stubsPane = new JTabbedPane();
        stubsPane.setForeground(Color.LIGHT_GRAY);

        // Listener on sheet tab operations
        stubsPane.addChangeListener(this);

        // Listener on invalid sheets display
        ViewParameters.getInstance()
                .addPropertyChangeListener(ViewParameters.INVALID_SHEET_DISPLAY, this);

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
    public void addAssembly (SheetAssembly assembly)
    {
        SheetStub stub = assembly.getStub();
        logger.debug("addAssembly for {}", stub);

        // Initial zoom ratio
        assembly.setZoomRatio(constants.initialZoomRatio.getValue());

        // Make sure the assembly is part of the tabbed pane
        int tabIndex = stubsPane.indexOfComponent(assembly.getComponent());

        if (tabIndex == -1) {
            // Insert in tabbed pane and in reverse map
            stubsMap.put(assembly.getComponent(), stub);
            insertAssembly(stub, stubsPane.getTabCount());
        }
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

    //----------------//
    // deleteAssembly //
    //----------------//
    public void deleteAssembly (SheetStub stub)
    {
        removeAssembly(stub); // Removed from stubsPane
        stubsMap.remove(stub.getAssembly().getComponent()); // Removed from stubsMap
    }

    //-----------------//
    // displayAllStubs //
    //-----------------//
    /**
     * Display all the stubs (valid or not) of the provided book
     *
     * @param book the provided book
     */
    public void displayAllStubs (Book book)
    {
        final List<SheetStub> stubs = book.getStubs();

        // Do we have a pivot?
        int tabPivot = getFirstDisplayedStubIndex(book);

        if (tabPivot == -1) {
            // Nothing displayed for this book yet, hence append all stubs at the end
            for (SheetStub stub : stubs) {
                addAssembly(stub.getAssembly());
            }
        } else {
            int stubPivot = getStubAt(tabPivot).getNumber() - 1;

            // Position WRT pivot
            for (int stubIndex = 0; stubIndex < stubs.size(); stubIndex++) {
                SheetStub stub = stubs.get(stubIndex);
                JComponent component = stub.getAssembly().getComponent();
                int tabIndex = stubsPane.indexOfComponent(component);

                if (tabIndex != -1) {
                    tabPivot = tabIndex; // Already displayed
                } else if (stubIndex < stubPivot) {
                    insertAssembly(stub, tabPivot); // Display just before pivot
                } else {
                    insertAssembly(stub, ++tabPivot); // Display just after pivot
                }

                stubPivot = stubIndex;
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
            sheet.getLocationService().dumpSubscribers();

            for (Lag lag : sheet.getLagManager().getAllLags()) {
                if (lag != null) {
                    lag.getEntityService().dumpSubscribers();
                    lag.getRunService().dumpSubscribers();
                }
            }

            if (sheet.getGlyphIndex() != null) {
                sheet.getGlyphIndex().getEntityService().dumpSubscribers();
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
    public JComponent getComponent ()
    {
        return stubsPane;
    }

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

        if (stub != null) {
            return stub.getBook();
        }

        return null;
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
    public static Step getEarlyStep ()
    {
        return constants.earlyStep.getValue();
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class.
     *
     * @return the single instance
     */
    public static StubsController getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new StubsController();
        }

        return INSTANCE;
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

    //---------//
    // markTab //
    //---------//
    /**
     * Set the stub tab using provided foreground color
     *
     * @param stub  sheet at hand
     * @param color color for sheet tab
     */
    public void markTab (SheetStub stub,
                         Color color)
    {
        logger.debug("mark {} in {}", stub, color);

        int tabIndex = stubsPane.indexOfComponent(stub.getAssembly().getComponent());

        if (tabIndex != -1) {
            stubsPane.setForegroundAt(tabIndex, color);
        }
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
        StubsController controller = StubsController.getInstance();

        if (ViewParameters.getInstance().isInvalidSheetDisplay()) {
            // Display all sheets for all books, including invalid ones
            for (Book book : OMR.getEngine().getAllBooks()) {
                controller.displayAllStubs(book);
            }
        } else {
            // Hide invalid sheets for all books
            for (Book book : OMR.getEngine().getAllBooks()) {
                for (SheetStub stub : book.getStubs()) {
                    if (!stub.isValid()) {
                        controller.removeAssembly(stub);
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
        logger.debug("StubsController refresh()");
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
     * @param stub the stub to be displayed (cannot be null).
     */
    public void selectAssembly (SheetStub stub)
    {
        logger.debug("showAssembly for {}", stub);

        // Make sure the assembly is part of the tabbed pane
        int tabIndex = stubsPane.indexOfComponent(stub.getAssembly().getComponent());

        if (tabIndex != -1) {
            stubsPane.setSelectedIndex(tabIndex);
        } else {
            logger.warn("No tab found for {}", stub);
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * This method is called whenever the sheet selection is modified,
     * whether programmatically (by means of {@link #selectAssembly} or {@link
     * SheetStub#reset()} or by user action (manual selection of the sheet tab).
     */
    @Override
    public void stateChanged (ChangeEvent e)
    {
        final int tabIndex = stubsPane.getSelectedIndex();
        logger.debug("stateChanged tabIndex: {}", tabIndex);

        // User has selected a new sheet tab?
        if (tabIndex != -1) {
            // Remember the new selected sheet stub
            JComponent component = (JComponent) stubsPane.getComponentAt(tabIndex);
            SheetStub stub = stubsMap.get(component);

            if (!stub.getBook().isClosing()) {
                // Check whether we should run early steps on the sheet
                checkStubStatus(stub, tabIndex);

                // Tell the selected assembly that it now has the focus
                stub.getAssembly().assemblySelected();

                // This is the new current stub
                callAboutStub(stub);
            }
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

    //-----------------//
    // checkStubStatus //
    //-----------------//
    /**
     * Check whether the selected sheet is visible and, if not so, launch the proper
     * early steps on the sheet.
     *
     * @param stub     the sheet at hand
     * @param tabIndex the corresponding tab index in stubsPane
     */
    private void checkStubStatus (final SheetStub stub,
                                  final int tabIndex)
    {
        logger.debug("checkStubStatus for {}", stub);

        Step currentStep = stub.getCurrentStep();

        if (currentStep == null) {
            final Step earlyStep = getEarlyStep();

            if ((earlyStep != null) && !stub.isDone(earlyStep)) {
                // Process sheet asynchronously
                Callable<Void> task = new Callable<Void>()
                {
                    @Override
                    public Void call ()
                            throws Exception
                    {
                        boolean ok = stub.ensureStep(earlyStep);
                        markTab(stub, ok ? Color.BLACK : Color.RED);

                        return null;
                    }

                    @Override
                    public String toString ()
                    {
                        return earlyStep + " for " + stub;
                    }
                };

                logger.debug("launching {}", task);
                stubsPane.setForegroundAt(tabIndex, Colors.SHEET_BUSY);

                // Since we are on Swing EDT, use asynchronous processing
                OmrExecutors.getCachedLowExecutor().submit(task);
            } else if (!stub.hasSheet()) {
                // Stub just loaded from project file, load & display the related sheet
                stub.getSheet().displayMainTabs();
            }
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
        stubsPane.insertTab(
                defineTitleFor(stub, null),
                null,
                stub.getAssembly().getComponent(),
                stub.getBook().getInputPath().toString(),
                index);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Step.Constant earlyStep = new Step.Constant(
                Step.BINARY,
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
        //~ Methods --------------------------------------------------------------------------------

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
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            if (stubsPane.getComponentCount() > 0) {
                stubsPane.setSelectedIndex(0);
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
        //~ Methods --------------------------------------------------------------------------------

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
