//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B o a r d s P a n e                                       //
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
package org.audiveris.omr.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Dimension;

import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.SeparablePopupMenu;
import static org.audiveris.omr.ui.util.UIPredicates.*;
import org.audiveris.omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

/**
 * Class {@code BoardsPane} defines a view on a set of user {@link
 * Board} instances, where data related to current point, run, section,
 * glyph, etc can be displayed in dedicated boards.
 * <p>
 * There is one BoardsPane instance for each view of the same sheet,
 * each with its own collection of board instances.
 *
 * @author Hervé Bitteur
 */
public class BoardsPane
{

    private static final Logger logger = LoggerFactory.getLogger(BoardsPane.class);

    /** The concrete UI component */
    private final Panel component;

    /** Sequence of current boards, kept ordered by board preferred position */
    private final List<Board> boards = new ArrayList<>();

    /** Unique (application-wide) name for this pane. */
    private String name;

    /** Mouse listener */
    private final MouseAdapter mouseAdapter = new MyMouseAdapter();

    /**
     * Create a BoardsPane, with initial boards.
     *
     * @param boards the initial collection of boards
     */
    public BoardsPane (List<Board> boards)
    {
        this.boards.clear();

        for (Board board : boards) {
            this.boards.add(board);
            board.setParent(this);
        }

        Collections.sort(this.boards, Board.byPosition);

        component = new Panel();
        component.setName("boardsPane");
        component.setBorder(null);
        final int inset = UIUtil.adjustedSize(6);
        component.setInsets(inset, inset, inset, inset); // TLBR
        component.setMinimumSize(new Dimension(Board.MIN_BOARD_WIDTH + 2 * inset, 1));
        component.addMouseListener(mouseAdapter);

        defineLayout();
    }

    //------------//
    // BoardsPane //
    //------------//
    /**
     * Create a BoardsPane, with initial boards.
     *
     * @param boards the initial collection of boards
     */
    public BoardsPane (Board... boards)
    {
        this(Arrays.asList(boards));
    }

    //----------//
    // addBoard //
    //----------//
    /**
     * Dynamically add a board into BoardsPane structure.
     *
     * @param board the board instance to add
     */
    public void addBoard (Board board)
    {
        // Replace any board with same name, if any
        Board oldBoard = getBoard(board.getName());

        if (oldBoard != null) {
            boards.remove(oldBoard);
            oldBoard.disconnect();
        }

        boards.add(board);
        board.setParent(this);
        Collections.sort(this.boards, Board.byPosition);
        update();

        if (board.isSelected()) {
            board.connect();
        }
    }

    //---------//
    // connect //
    //---------//
    /**
     * Invoked when the boardsPane has been selected, in order to
     * connect all the contained boards to their inputs.
     */
    public void connect ()
    {
        ///logger.warn("Connect " + this);
        for (Board board : boards) {
            if (board.isSelected()) {
                board.connect();
            }
        }
    }

    //------------//
    // disconnect //
    //------------//
    /**
     * Invoked when the boardsPane has been deselected.
     */
    public void disconnect ()
    {
        ///logger.info("-BoardPane " + name + " disconnect");
        for (Board board : boards) {
            if (board.isSelected()) {
                board.disconnect();
            }
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

    //---------//
    // getName //
    //---------//
    /**
     * Report the unique name for this boards pane.
     *
     * @return the declared name
     */
    public String getName ()
    {
        return name;
    }

    //---------//
    // setName //
    //---------//
    /**
     * Assign the unique name for this boards pane.
     *
     * @param name the assigned name
     */
    public void setName (String name)
    {
        this.name = name;
        component.setName(name);
    }

    //-------------//
    // removeBoard //
    //-------------//
    /**
     * Dynamically remove a board from BoardsPane structure.
     *
     * @param board the board instance to remove
     */
    public void removeBoard (Board board)
    {
        if (board.isSelected()) {
            board.disconnect();
        }

        boards.remove(board);
        update();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        sb.append(" ").append(name).append(" [");

        boolean first = true;

        for (Board board : boards) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }

            sb.append(board.getName());

            if (!board.isSelected()) {
                sb.append(":HIDDEN");
            }
        }

        sb.append("]}");

        return sb.toString();
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        // Prepare layout elements
        final String panelInterline = Panel.getPanelInterline();
        StringBuilder sbr = null;

        for (Board board : boards) {
            if (board.isSelected()) {
                if (sbr != null) {
                    sbr.append(',').append(panelInterline).append(',');
                } else {
                    sbr = new StringBuilder();
                }

                sbr.append("pref");
            }
        }

        if (sbr == null) {
            return; // No selected boards
        }

        FormLayout layout = new FormLayout("fill:pref:grow", sbr.toString());
        PanelBuilder builder = new PanelBuilder(layout, component);
        CellConstraints cst = new CellConstraints();

        // Now add the desired components, using provided order
        int r = 1;

        for (Board board : boards) {
            if (board.isSelected()) {
                builder.add(board.getComponent(), cst.xy(1, r));
                r += 2;
            }
        }
    }

    //----------//
    // getBoard //
    //----------//
    private Board getBoard (String title)
    {
        for (Board b : boards) {
            if (b.getName().equals(title)) {
                return b;
            }
        }

        return null;
    }

    //--------//
    // update //
    //--------//
    /**
     * Modify the BoardsPane component composition.
     */
    void update ()
    {
        component.removeAll();
        defineLayout();

        component.invalidate();
        component.revalidate();
        component.repaint();
    }

    //----------------//
    // MyMouseAdapter //
    //----------------//
    /**
     * Sub-classed to offer mouse interaction.
     */
    private class MyMouseAdapter
            extends MouseAdapter
            implements ItemListener
    {

        //------------------//
        // itemStateChanged //
        //------------------//
        /**
         * Triggered from popup menu.
         *
         * @param e menu item event
         */
        @Override
        public void itemStateChanged (ItemEvent e)
        {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getItem();
            Board board = getBoard(item.getText());
            board.setSelected(item.getState());
        }

        //--------------//
        // mousePressed //
        //--------------//
        /**
         * Triggered when mouse is pressed.
         *
         * @param e mouse event
         */
        @Override
        public void mousePressed (MouseEvent e)
        {
            if (isContextWanted(e)) {
                JPopupMenu popup = new SeparablePopupMenu();

                // A title for this menu
                JMenuItem head = new JMenuItem("Boards for selection:");
                head.setHorizontalAlignment(SwingConstants.CENTER);
                head.setEnabled(false);
                popup.add(head);
                popup.addSeparator();

                for (Board board : boards) {
                    JMenuItem item = new JCheckBoxMenuItem(board.getName(), board.isSelected());
                    item.addItemListener(this);
                    item.setToolTipText(
                            board.isSelected() ? "Deselect this board?" : "Select this board?");
                    popup.add(item);
                }

                popup.show(component, e.getX(), e.getY());
            }
        }
    }
}
