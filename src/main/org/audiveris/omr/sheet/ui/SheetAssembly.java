//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S h e e t A s s e m b l y                                    //
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.OMR;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.util.ClosableTabbedPane;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.ui.view.LogSlider;
import org.audiveris.omr.ui.view.Rubber;
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.ui.view.Zoom;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code SheetAssembly} is a UI assembly dedicated to the display of various
 * views around the same sheet.
 * All views share the same zoom and the same position within their containing {@link JScrollPane}.
 * <p>
 * It gathers:
 * <ul>
 * <li><b>slider</b>: a {@link Zoom} with its dedicated graphical {@link LogSlider}</li>
 * <li>a mouse adapter {@link Rubber}</li>
 * <li><b>viewsPane</b>: a tabbed pane of all {@link SheetView}'s of this sheet.
 * A SheetView combines a sheet view on left and the related boardsPane on right.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class SheetAssembly
        implements ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetAssembly.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Link with sheet stub. */
    @Navigable(false)
    private final SheetStub stub;

    /** The concrete UI component. */
    private final Panel component = new Panel();

    /** To manually control the zoom ratio. */
    private final LogSlider slider = new LogSlider(2, 5, LogSlider.VERTICAL, -3, 5, 0);

    /** Zoom, with default ratio set to 1. */
    private final Zoom zoom = new Zoom(slider, 1);

    /** Mouse adapter. */
    private final Rubber rubber = new Rubber(zoom);

    /** Previously selected tab. */
    private SheetView previousView = null;

    /** Shared scroll values across all SheetView's (for JScrollPane instances). */
    private ScrollValues scrollValues;

    /** Sheet size. */
    private Dimension modelSize;

    /** Same split divider location for all SheetView's of the sheet. */
    private Integer commonDividerLocation;

    /** List of views (parallel to viewsPane). */
    private final List<SheetView> views = new ArrayList<>();

    /** Closable tabbed container for all SheetView's of the sheet. */
    private final ViewsPane viewsPane = new ViewsPane();

    /** Temporary information: the view being removed, if any. */
    private SheetView viewBeingRemoved;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new {@code SheetAssembly} instance dedicated to one sheet stub.
     *
     * @param stub the related sheet stub
     */
    public SheetAssembly (SheetStub stub)
    {
        logger.debug("creating SheetAssembly on {}", stub);

        this.stub = stub;

        // GUI stuff
        slider.setToolTipText("Adjust Zoom Ratio");

        // To be notified of view selection (manually or programmatically)
        viewsPane.addChangeListener(this);

        logger.debug("SheetAssembly created.");

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addBoard //
    //----------//
    /**
     * Add a board into the BoardsPane corresponding to the provided view tab title.
     *
     * @param tab   the tab of the targeted view
     * @param board the board to add dynamically
     */
    public void addBoard (final SheetTab tab,
                          final Board board)
    {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> addBoard(tab, board));
            } catch (InterruptedException |
                     InvocationTargetException ex) {
                logger.warn("invokeAndWait error", ex);
            }
        } else {
            final SheetView view = getView(tab.label);

            if (view != null) {
                view.getBoardsPane().addBoard(board);
            }
        }
    }

    //------------//
    // addViewTab //
    //------------//
    /**
     * Add a new tab, that contains a new view on the sheet.
     *
     * @param tab        the label to use for the tab
     * @param sv         the view on the sheet
     * @param boardsPane the board pane associated to the tab
     */
    public void addViewTab (SheetTab tab,
                            ScrollView sv,
                            BoardsPane boardsPane)
    {
        addViewTab(tab.label, sv, boardsPane);
    }

    //------------//
    // addViewTab //
    //------------//
    /**
     * Add a new tab, that contains a new view on the sheet.
     *
     * @param label      the label to use for the tab
     * @param scrollView the view on the sheet
     * @param boardsPane the board pane associated to the tab
     */
    public void addViewTab (final String label,
                            final ScrollView scrollView,
                            final BoardsPane boardsPane)
    {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> addViewTab(label, scrollView, boardsPane));
            } catch (InterruptedException |
                     InvocationTargetException ex) {
                logger.warn("invokeAndWait error", ex);
            }
        } else {
            JScrollPane scrollPane = scrollView.getComponent();
            UIUtil.suppressBorders(scrollPane);

            if (boardsPane != null) {
                boardsPane.setName(label + " boards");
            }

            logger.debug("addViewTab begin {} boardsPane={}", label, boardsPane);

            // Remove any existing SheetView with the same label
            final SheetView oldView = getView(label);
            if (oldView != null) {
                viewsPane.remove(oldView.getComponent());
                views.remove(oldView);
            }

            // Register the new SheetView
            final SheetView view = new SheetView(this, label, boardsPane, scrollView);
            views.add(view);
            viewsPane.addTab(label, view.getComponent());
            viewsPane.setSelectedComponent(view.getComponent());

            if (boardsPane != null) {
                boardsPane.resize();
            }

            logger.debug("addViewTab end {} boardsPane={}", label, boardsPane);
        }
    }

    //------------------//
    // assemblySelected //
    //------------------//
    /**
     * Method called when this sheet assembly is selected, since we can have several
     * sheets displayed, each with its sheet assembly.
     * This is called from {@link org.audiveris.omr.sheet.ui.StubsController} when the tab
     * of another sheet is selected.
     */
    public void assemblySelected ()
    {
        logger.debug("{} assemblySelected", stub.getId());

        // Display the related boards
        displayBoards();

        if (stub.hasSheet() && stub.getLatestStep().compareTo(Step.HEADS) >= 0) {
            // Update repetitiveInput checkbox
            BookActions.getInstance().updateRepetitiveInput(stub.getSheet());
        }
    }

    //-------//
    // close //
    //-------//
    /**
     * Close the assembly, by removing it from the containing sheet tabbed pane.
     */
    public void close ()
    {
        for (SheetView view : views) {
            // Disconnect all keyboard bindings from PixelBoard's (this fixes Swing memory leak)
            view.disconnectKeyboard();

            // Remove the related boards
            view.deselected();
        }
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component.
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return component;
    }

    //--------------------//
    // getDividerLocation //
    //--------------------//
    /**
     * @return the dividerLocation
     */
    public Integer getCommonDividerLocation ()
    {
        return commonDividerLocation;
    }

    //--------------------//
    // setDividerLocation //
    //--------------------//
    /**
     * @param dividerLocation the dividerLocation to set
     */
    public void setCommonDividerLocation (Integer dividerLocation)
    {
        this.commonDividerLocation = dividerLocation;

        if (dividerLocation != null) {
            for (SheetView view : views) {
                view.getComponent().setDividerLocation(dividerLocation);
            }
        }
    }

    //--------------//
    // getModelSize //
    //--------------//
    /**
     * @return the modelSize
     */
    public Dimension getModelSize ()
    {
        return modelSize;
    }

    //--------------//
    // setModelSize //
    //--------------//
    /**
     * @param modelSize the modelSize to set
     */
    public void setModelSize (Dimension modelSize)
    {
        this.modelSize = modelSize;
    }

    //-----------//
    // getRubber //
    //-----------//
    /**
     * @return the rubber
     */
    public Rubber getRubber ()
    {
        return rubber;
    }

    //-----------------//
    // getScrollValues //
    //-----------------//
    /**
     * @return the scrollValues
     */
    public ScrollValues getScrollValues ()
    {
        return scrollValues;
    }

    //-----------------//
    // setScrollValues //
    //-----------------//
    /**
     * @param scrollValues the scrollValues to set
     */
    public void setScrollValues (ScrollValues scrollValues)
    {
        this.scrollValues = scrollValues;
    }

    //---------------------//
    // getSelectedViewName //
    //---------------------//
    /**
     * Report name of selected view.
     *
     * @return name of selected view, or null
     */
    public String getSelectedViewName ()
    {
        final SheetView view = getCurrentView();

        return (view != null) ? view.getTitle() : null;
    }

    //-----------------------//
    // getSelectedScrollView //
    //-----------------------//
    /**
     * Report the ScrollView currently selected.
     *
     * @return the ScrollView component of current view
     */
    public ScrollView getSelectedScrollView ()
    {
        final SheetView view = getCurrentView();

        return (view != null) ? view.getScrollView() : null;
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the sheet this assembly is related to.
     *
     * @return the related sheet
     */
    public Sheet getSheet ()
    {
        return stub.getSheet();
    }

    //---------//
    // getStub //
    //---------//
    /**
     * Report the underlying sheet stub.
     *
     * @return the related sheet stub
     */
    public SheetStub getStub ()
    {
        return stub;
    }

    //---------//
    // getView //
    //---------//
    /**
     * Find the SheetView that corresponds to the provided tab title.
     *
     * @param title the tab title.
     * @return the viewTab found, or null
     */
    public SheetView getView (String title)
    {
        for (SheetView view : views) {
            if (view.getTitle().equals(title)) {
                return view;
            }
        }

        return null;
    }

    //---------//
    // getZoom //
    //---------//
    /**
     * @return the zoom
     */
    public Zoom getZoom ()
    {
        return zoom;
    }

    //-------------//
    // lockViewTab //
    //-------------//
    /**
     * Make the provided tab non closable.
     *
     * @param tab the tab to lock
     */
    public void lockViewTab (SheetTab tab)
    {
        for (int i = 0, count = viewsPane.getTabCount(); i < count; i++) {
            if (viewsPane.getTitleAt(i).equals(tab.label)) {
                viewsPane.removeClosingButton(i);

                return;
            }
        }
    }

    //-----------//
    // removeTab //
    //-----------//
    /**
     * Remove the provided tab.
     *
     * @param tab the tab to remove
     */
    public void removeTab (SheetTab tab)
    {
        for (int i = 0, count = viewsPane.getTabCount(); i < count; i++) {
            if (viewsPane.getTitleAt(i).equals(tab.label)) {
                viewBeingRemoved = views.get(i); // To disable notifications

                viewsPane.removeTabAt(i);
                views.remove(i);

                viewBeingRemoved = null; // To re-enable notifications

                return;
            }
        }
    }

    //-----------//
    // renameTab //
    //-----------//
    /**
     * Change the name of a tab.
     *
     * @param oldName old tab name
     * @param newName new tab name
     */
    public void renameTab (String oldName,
                           String newName)
    {
        for (int i = 0, count = viewsPane.getTabCount(); i < count; i++) {
            if (viewsPane.getTitleAt(i).equals(oldName)) {
                viewsPane.setTitleAt(i, newName);

                return;
            }
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reset the assembly to its initial empty state.
     */
    public void reset ()
    {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> reset());
            } catch (InterruptedException |
                     InvocationTargetException ex) {
                logger.warn("invokeAndWait error", ex);
            }
        } else {
            ScrollView scrollView = getSelectedScrollView();

            if (scrollView != null) {
                scrollValues = new ScrollValues(scrollView);
                logger.debug("Storing {}", scrollValues);
            }

            close();

            for (SheetView view : views) {
                view.close();
            }

            viewsPane.removeAll();
            views.clear();
            previousView = null;
        }
    }

    //---------------//
    // selectViewTab //
    //---------------//
    /**
     * Force a tab selection programmatically.
     *
     * @param tab the tab to be selected
     */
    public void selectViewTab (SheetTab tab)
    {
        for (int i = 0, count = viewsPane.getTabCount(); i < count; i++) {
            if (viewsPane.getTitleAt(i).equals(tab.label)) {
                viewsPane.setSelectedIndex(i);
                viewsPane.repaint();

                logger.debug("Selected view tab {}", tab);

                return;
            }
        }
    }

    //--------------//
    // setZoomRatio //
    //--------------//
    /**
     * Modify the ratio of the global zoom for all views of the sheet.
     *
     * @param ratio the new display ratio
     */
    public void setZoomRatio (double ratio)
    {
        zoom.setRatio(ratio);
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * This method is called whenever another view tab is selected in the SheetAssembly
     * or when a tab is removed.
     * <ul>
     * <li>Selection: notify previous view if any of deselection, notify current view of selection.
     * <li>Removal: no notification to be done.
     * </ul>
     *
     * @param e the originating change event (not used)
     */
    @Override
    public void stateChanged (ChangeEvent e)
    {
        final SheetView view = getCurrentView();
        logger.debug("{} SheetAssembly stateChanged previous:{} current:{} removed:{}",
                     stub, previousView, view, viewBeingRemoved);

        if (view == viewBeingRemoved) {
            return;
        }

        if (view != previousView) {
            if (previousView != null) {
                previousView.deselected();
            }

            if (view != null) {
                view.selected();
            }
        }

        previousView = view;
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout of this assembly.
     */
    private void defineLayout ()
    {
        component.setLayout(new BorderLayout());
        component.setNoInsets();
        component.add(slider, BorderLayout.WEST);
        component.add(viewsPane, BorderLayout.CENTER);
        component.setName("SheetAssemblyPanel");

        // Avoid slider to react on (and consume) page up/down keys or arrow keys
        InputMap inputMap = slider.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), "none");
        inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "none");
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "none");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "none");
        inputMap.put(KeyStroke.getKeyStroke("UP"), "none");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "none");
    }

    //---------------//
    // displayBoards //
    //---------------//
    /**
     * Make the boards pane visible (for this sheet & view).
     */
    private void displayBoards ()
    {
        // Make sure the view tab is ready
        final SheetView view = getCurrentView();

        if (view != null) {
            view.displayBoards();
        }
    }

    //----------------//
    // getCurrentView //
    //----------------//
    /**
     * Report the SheetView currently selected, if any.
     *
     * @return the current SheetView, or null
     */
    public SheetView getCurrentView ()
    {
        final int index = viewsPane.getSelectedIndex();

        return (index < 0) ? null : views.get(index);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------------//
    // ScrollValues //
    //--------------//
    /**
     * To preserve and replicate scroll bar values.
     */
    public static class ScrollValues
    {

        final DefaultBoundedRangeModel hori; // Model for horizontal scrollbar

        final DefaultBoundedRangeModel vert; // Model for vertical scrollbar

        public ScrollValues (ScrollView scrollView)
        {
            hori = copy(scrollView.getComponent().getHorizontalScrollBar().getModel());
            vert = copy(scrollView.getComponent().getVerticalScrollBar().getModel());
        }

        public void applyTo (JScrollPane scrollPane)
        {
            apply(hori, scrollPane.getHorizontalScrollBar().getModel());
            apply(vert, scrollPane.getVerticalScrollBar().getModel());
        }

        @Override
        public String toString ()
        {
            return "ScrollValues{hori:" + hori + ", vert:" + vert + "}";
        }

        private void apply (BoundedRangeModel src,
                            BoundedRangeModel tgt)
        {
            tgt.setRangeProperties(
                    src.getValue(),
                    src.getExtent(),
                    src.getMinimum(),
                    src.getMaximum(),
                    false);
        }

        private DefaultBoundedRangeModel copy (BoundedRangeModel m)
        {
            return new DefaultBoundedRangeModel(
                    m.getValue(),
                    m.getExtent(),
                    m.getMinimum(),
                    m.getMaximum());
        }
    }

    //-----------//
    // ViewsPane //
    //-----------//
    /**
     * Closable tabbed pane, with user confirmation on tab closing.
     */
    private class ViewsPane
            extends ClosableTabbedPane
    {

        @Override
        public boolean tabAboutToClose (int tabIndex)
        {
            final boolean ok = OMR.gui.displayConfirmation(
                    getTitleAt(tabIndex) + " tab is about to close." + "\nDo you confirm?");

            if (ok) {
                views.remove(tabIndex); // Keep views list parallel to viewsPane
            }

            return ok;
        }
    }
}
