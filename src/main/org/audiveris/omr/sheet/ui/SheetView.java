//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S h e e t V i e w                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.SheetAssembly.ScrollValues;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.ui.view.RubberPanel;
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

/**
 * Class <code>SheetView</code> gathers a specific view (Binary, Data, ..) on a sheet
 * with its related column of boards.
 * <p>
 * It uses a JSplitPane:
 * <ul>
 * <li>The left part is the view image, in its vertical/horizontal scroll pane.
 * <li>The right part is the boards pane for the view, in its vertical scroll pane.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class SheetView
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetView.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The containing sheet assembly. */
    private final SheetAssembly assembly;

    /** The related stub. */
    @Navigable(false)
    private final SheetStub stub;

    /** Title used for the tab (Binary, Data, NoStaff, ...). */
    private final String title;

    /** Hosts the sheet view. */
    private final ScrollView scrollView;

    /** Related boards pane. */
    private final BoardsPane boardsPane;

    /** Scroll pane to contain boardsPane. */
    private final BoardsScrollPane scrollPane = new BoardsScrollPane();

    /** Split Component. */
    private final JSplitPane splitPane;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     *
     * @param assembly   the sheet assembly
     * @param title      title for the view
     * @param boardsPane related boards pane
     * @param scrollView scroll view to host the sheet view
     */
    public SheetView (SheetAssembly assembly,
                      String title,
                      BoardsPane boardsPane,
                      ScrollView scrollView)
    {
        this.assembly = assembly;
        this.title = title;
        this.boardsPane = boardsPane;
        this.scrollView = scrollView;

        stub = assembly.getStub();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.99d); // Give bulk space to left part

        if (scrollView != null) {
            splitPane.setLeftComponent(scrollView.getComponent());
        }

        if (boardsPane != null) {
            scrollPane.setBoards(boardsPane.getComponent());
            splitPane.setRightComponent(scrollPane);
            boardsPane.setSplitContainer(splitPane);
        }

        // Re-use common divider location, if already set
        if (assembly.getCommonDividerLocation() != null) {
            splitPane.setDividerLocation(assembly.getCommonDividerLocation());
        }

        // Called back when split container is resized
        splitPane.addPropertyChangeListener(
                JSplitPane.DIVIDER_LOCATION_PROPERTY,
                (PropertyChangeEvent pce) -> {
            assembly.setCommonDividerLocation(splitPane.getDividerLocation());
        });

        // Make the new view reuse the common zoom and rubber instances
        RubberPanel rubberPanel = scrollView.getView();
        rubberPanel.setZoom(assembly.getZoom());
        rubberPanel.setRubber(assembly.getRubber());

        // Set the model size?
        if (assembly.getModelSize() == null) {
            if (stub.hasSheet()) {
                Sheet sheet = stub.getSheet();

                if (sheet.getPicture() != null) {
                    assembly.setModelSize(new Dimension(sheet.getWidth(), sheet.getHeight()));
                }
            }
        }

        if (assembly.getModelSize() != null) {
            rubberPanel.setModelSize(assembly.getModelSize());
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // close //
    //-------//
    /**
     * Close this viewTab instance.
     */
    public void close ()
    {
        final RubberPanel rubberPanel = scrollView.getView();
        rubberPanel.unsetZoom(assembly.getZoom());
        rubberPanel.unsetRubber(assembly.getRubber());
        logger.debug("Closed tab: {}", this);
    }

    //------------//
    // deselected //
    //------------//
    /**
     * Run when this tab gets deselected.
     */
    public void deselected ()
    {
        logger.debug("SheetAssembly: {} viewTab. deselected for {}", stub.getId(), this);

        assembly.setScrollValues(new SheetAssembly.ScrollValues(scrollView));

        // Disconnection of events
        RubberPanel rubberPanel = scrollView.getView();
        rubberPanel.unsubscribe();

        // Disconnection of related boards, if any
        if ((boardsPane != null) && assembly.getComponent().isVisible()) {
            boardsPane.disconnect();
        }
    }

    //--------------------//
    // disconnectKeyboard //
    //--------------------//
    public void disconnectKeyboard ()
    {
        if (boardsPane != null) {
            for (Component topComp : boardsPane.getComponent().getComponents()) {
                for (Component comp : ((Container) topComp).getComponents()) {
                    if (comp instanceof JComponent) {
                        ((JComponent) comp).resetKeyboardActions();
                    }
                }
            }
        }
    }

    //---------------//
    // displayBoards //
    //---------------//
    public void displayBoards ()
    {
        if (boardsPane != null) {
            if (splitPane.getRightComponent() != boardsPane.getComponent()) {
                splitPane.setRightComponent(boardsPane.getComponent());
            }

            // (Re)connect the boards to their selection inputs if needed
            boardsPane.connect();

            // Resize boards
            boardsPane.resize();
        }
    }

    //---------------//
    // getBoardsPane //
    //---------------//
    /**
     * Report the boards pane.
     *
     * @return the boardsPane
     */
    public BoardsPane getBoardsPane ()
    {
        return boardsPane;
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the Swing component (JSplitPane).
     *
     * @return the splitPane
     */
    public JSplitPane getComponent ()
    {
        return splitPane;
    }

    //----------//
    // getTitle //
    //----------//
    /**
     * Report the view title (Binary, Data, ...)
     *
     * @return view title
     */
    public String getTitle ()
    {
        return title;
    }

    //----------//
    // selected //
    //----------//
    /**
     * Run when this tab gets selected.
     */
    public void selected ()
    {
        logger.debug(
                "SheetAssembly: {} viewTab.   selected for {} dim:{}",
                stub.getId(),
                this,
                scrollView.getView().getPreferredSize());

        // Link rubber with proper view
        RubberPanel rubberPanel = scrollView.getView();
        assembly.getRubber().connectComponent(rubberPanel);
        assembly.getRubber().setMouseMonitor(rubberPanel);

        // Make connections to events
        rubberPanel.subscribe();

        // Restore display of proper context
        if (assembly.getComponent().isShowing()) {
            displayBoards();
        }

        // Apply the same scroll bar positions
        final ScrollValues scrollValues = assembly.getScrollValues();

        if (scrollValues != null) {
            scrollValues.applyTo(scrollView.getComponent());
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append(title);
        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // getScrollView //
    //---------------//
    /**
     * Report the image scroll view.
     *
     * @return the scrollView
     */
    public ScrollView getScrollView ()
    {
        return scrollView;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------------//
    // BoardsScrollPane //
    //------------------//
    /**
     * Just a scrollPane to host the pane of user boards, trying to offer enough room for
     * the boards.
     */
    private static class BoardsScrollPane
            extends JScrollPane
    {

        public BoardsScrollPane ()
        {
            this.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);

            final int inset = UIUtil.adjustedSize(6);
            setMinimumSize(new Dimension(Board.MIN_BOARD_WIDTH + (2 * inset), 1));
        }

        public void setBoards (JComponent boards)
        {
            setViewportView(boards);
            revalidate();
        }
    }
}
