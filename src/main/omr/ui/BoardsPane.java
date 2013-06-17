//----------------------------------------------------------------------------//
//                                                                            //
//                            B o a r d s P a n e                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.ui.util.Panel;
import omr.ui.util.SeparablePopupMenu;
import static omr.ui.util.UIPredicates.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
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
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

/**
 * Class {@code BoardsPane} defines a view on a set of user {@link
 * Board} instances, where data related to current point, run, section,
 * glyph, etc can be displayed in dedicated boards.
 *
 * <p>There is one BoardsPane instance for each view of the same sheet,
 * each with its own collection of board instances.
 *
 * @author Hervé Bitteur
 */
public class BoardsPane
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(BoardsPane.class);

    //~ Instance fields --------------------------------------------------------
    /** The concrete UI component */
    private final Panel component;

    /** Sequence of current boards, kept ordered by board preferred position */
    private final List<Board> boards = new ArrayList<>();

    /** Unique (application-wide) name for this pane. */
    private String name;

    /** Mouse listener */
    private MouseAdapter mouseAdapter = new MyMouseAdapter();

    //~ Constructors -----------------------------------------------------------
    //------------//
    // BoardsPane //
    //------------//
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

        Collections.sort(this.boards);

        component = new Panel();
        component.setNoInsets();
        component.add(defineLayout());
        component.addMouseListener(mouseAdapter);
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

    //~ Methods ----------------------------------------------------------------
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
        // Avoid duplicates
        if (getBoard(board.getName()) != null) {
            return;
        }

        boards.add(board);
        board.setParent(this);
        Collections.sort(this.boards);
        update();
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
            } else {
                board.disconnect();
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
            board.disconnect();
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
        boards.remove(board);
        update();
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        sb.append(" ")
                .append(name)
                .append(" [");

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

    //--------//
    // update //
    //--------//
    /**
     * Modify the BoardsPane component composition.
     */
    void update ()
    {
        connect();

        int count = component.getComponentCount();
        Component comp = component.getComponent(count - 1);
        component.remove(comp);
        component.add(defineLayout());
        component.revalidate();
        component.repaint();
    }

    //--------------//
    // defineLayout //
    //--------------//
    private JPanel defineLayout ()
    {
        // Prepare layout elements
        final String panelInterline = Panel.getPanelInterline();
        StringBuilder sbr = new StringBuilder();
        boolean first = true;

        for (Board board : boards) {
            if (board.isSelected()) {
                if (first) {
                    first = false;
                } else {
                    sbr.append(", ")
                            .append(panelInterline)
                            .append(", ");
                }

                sbr.append("pref");
            }
        }

        FormLayout layout = new FormLayout("pref", sbr.toString());

        Panel panel = new Panel();
        panel.setNoInsets();

        PanelBuilder builder = new PanelBuilder(layout, panel);
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        // Now add the desired components, using provided order
        int r = 1;

        for (Board board : boards) {
            if (board.isSelected()) {
                builder.add(board.getComponent(), cst.xy(1, r));
                r += 2;
            }
        }

        JPanel boardsPanel = builder.getPanel();
        boardsPanel.setBorder(null);

        return boardsPanel;
    }

    //----------//
    // getBoard //
    //----------//
    private Board getBoard (String title)
    {
        for (Board b : boards) {
            if (b.getName()
                    .equals(title)) {
                return b;
            }
        }

        return null;
    }

    //~ Inner Classes ----------------------------------------------------------
    //----------------//
    // MyMouseAdapter //
    //----------------//
    /**
     * Subclassed to offer mouse interaction.
     */
    private class MyMouseAdapter
            extends MouseAdapter
            implements ItemListener
    {
        //~ Methods ------------------------------------------------------------

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
                    JMenuItem item = new JCheckBoxMenuItem(
                            board.getName(),
                            board.isSelected());
                    item.addItemListener(this);
                    item.setToolTipText(
                            board.isSelected() ? "Deselect this board?"
                            : "Select this board?");
                    popup.add(item);
                }

                popup.show(component, e.getX(), e.getY());
            }
        }
    }
}
