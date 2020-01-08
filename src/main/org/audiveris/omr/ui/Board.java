//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           B o a r d                                            //
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

import org.audiveris.omr.ui.field.LCheckBox;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.ClassUtil;

import org.bushe.swing.event.EventSubscriber;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;

/**
 * Class {@code Board} defines the common properties of any user board such as
 * PixelBoard, SectionBoard, and the like.
 * <p>
 * Each board has a standard header composed of a title, a horizontal separator and optionally a
 * dump button. The board body is handled by the subclass.
 * </p>
 * <p>
 * Any board can be (de)selected in its containing {@link BoardsPane}. This can be done
 * programmatically using {@link #setSelected(boolean)} and manually (via a right-click in the
 * BoardsPane).
 * </p>
 * <p>
 * Only selected boards can be seen in the BoardsPane display. A selected board can be made
 * currently (in)visible programmatically using {@link #setVisible(boolean)}.
 * Typically, {@link org.audiveris.omr.check.CheckBoard}'s are visible only when they carry
 * glyph information.
 * </p>
 * <p>
 * By default, any board can have a related SelectionService, used for subscribe (input) and publish
 * (output). When {@link #connect} is called, the board instance is subscribed to its
 * SelectionService for a specific collection of event classes. Similarly, {@link #disconnect}
 * un-subscribes the Board instance from the same event classes.
 * </p>
 * <p>
 * This {@code Board} class is still an abstract class, since the onEvent() method must be
 * provided by every subclass.
 * </p>
 *
 * @author Hervé Bitteur
 */
public abstract class Board
        implements EventSubscriber<UserEvent>
{

    private static final Logger logger = LoggerFactory.getLogger(Board.class);

    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(Board.class);

    /** Minimum width available for a board. */
    public static final int MIN_BOARD_WIDTH = UIUtil.adjustedSize(350);

    // Predefined boards names with preferred display positions
    public static final Desc PIXEL = new Desc("Pixel", 100);

    public static final Desc BINARIZATION = new Desc("Binarization", 150);

    public static final Desc RUN = new Desc("Run", 200);

    public static final Desc SECTION = new Desc("Section", 250);

    public static final Desc FILAMENT = new Desc("Filament", 300);

    public static final Desc SAMPLE = new Desc("Sample", 400);

    public static final Desc GLYPH = new Desc("Glyph", 500);

    public static final Desc INTER = new Desc("Inter", 550);

    public static final Desc TEMPLATE = new Desc("Template", 575);

    public static final Desc FOCUS = new Desc("Focus", 600);

    public static final Desc SHAPE = new Desc("Shape", 700);

    public static final Desc EVAL = new Desc("Eval", 800);

    public static final Desc CHECK = new Desc("Check", 900);

    /** To sort boards by their position. */
    public static final Comparator<Board> byPosition = new Comparator<Board>()
    {
        @Override
        public int compare (Board b1,
                            Board b2)
        {
            return Integer.compare(b1.position, b2.position);
        }
    };

    /** The board instance name. */
    private final String name;

    /** The hosting BoardsPane, if any. */
    private BoardsPane parent;

    /** The board header. */
    private final Header header;

    /** The body part of the component. */
    private final Panel body = new Panel();

    /** The swing component of the board instance. */
    private final Panel component = new Panel();

    /** The event service this board interacts with. */
    private final SelectionService selectionService;

    /** The collection of event classes to be observed. */
    private final Class<?>[] eventsRead;

    /** The preferred position in BoardsPane sequence. */
    private final int position;

    /** Board is selected?. */
    private boolean selected;

    /**
     * Create a board from a pre-defined descriptor (name + position).
     *
     * @param desc             the board descriptor
     * @param selectionService the related selection service for input and output
     * @param eventsRead       the collection of event classes to observe
     * @param selected         true to pre-select the board
     * @param useCount         true for a count field
     * @param useVip           true for a VIP label and field
     * @param useDump          true for a dump button
     */
    public Board (Desc desc,
                  SelectionService selectionService,
                  Class<?>[] eventsRead,
                  boolean selected,
                  boolean useCount,
                  boolean useVip,
                  boolean useDump)
    {
        this(
                desc.name,
                desc.position,
                selectionService,
                eventsRead,
                selected,
                useCount,
                useVip,
                useDump);
    }

    /**
     * Create a board, with (dynamic) name and position.
     *
     * @param name             a name assigned to the board
     * @param position         the preferred position within BoardsPane display
     * @param selectionService the related selection service for input and output
     * @param eventsRead       the collection of event classes to observe
     * @param selected         true to pre-select the board
     * @param useCount         true for a count field
     * @param useVip           true for a VIP label and field
     * @param useDump          true for a dump button
     */
    public Board (String name,
                  int position,
                  SelectionService selectionService,
                  Class[] eventsRead,
                  boolean selected,
                  boolean useCount,
                  boolean useVip,
                  boolean useDump)
    {
        this.name = name;
        this.position = position;
        this.selectionService = selectionService;
        this.eventsRead = eventsRead;
        this.selected = selected;

        // Layout header and body parts
        header = (useCount || useVip || useDump) ? new Header(useCount, useVip, useDump) : null;

        if (header != null) {
            header.setInsets(0, 0, UIUtil.adjustedSize(3), 0); // TLBR
        }

        defineLayout();
    }

    //---------//
    // connect //
    //---------//
    /**
     * Connect to input selections.
     */
    public void connect ()
    {
        if (eventsRead != null) {
            for (Class<?> eventClass : eventsRead) {
                selectionService.subscribeStrongly(eventClass, this);

                // Refresh with latest data for this event class
                UserEvent event = (UserEvent) selectionService.getLastEvent(eventClass);

                if (event != null) {
                    event.movement = null;
                    onEvent(event);
                }
            }
        }
    }

    //------------//
    // disconnect //
    //------------//
    /**
     * Disconnect from input selections.
     */
    public void disconnect ()
    {
        if (eventsRead != null) {
            for (Class<?> eventClass : eventsRead) {
                selectionService.unsubscribe(eventClass, this);
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
    public JPanel getComponent ()
    {
        return component;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the name for this board instance.
     *
     * @return an instance name
     */
    public String getName ()
    {
        return name;
    }

    //------------//
    // isSelected //
    //------------//
    /**
     * Report whether this board is currently selected.
     *
     * @return true if selected
     */
    public boolean isSelected ()
    {
        return selected;
    }

    //-------------//
    // setSelected //
    //-------------//
    /**
     * Select or not this board in its containing BoardsPane.
     *
     * @param selected true for selected, false for de-selected
     */
    public void setSelected (boolean selected)
    {
        // No modification?
        if (selected == this.selected) {
            return;
        }

        if (selected) {
            connect();
        } else {
            disconnect();
        }

        this.selected = selected;

        if (parent != null) {
            parent.update();
        }
    }

    //-------------//
    // resizeBoard //
    //-------------//
    /**
     * Resize board component, to adapt to its new composition.
     */
    public void resizeBoard ()
    {
        component.invalidate();
        component.validate();
        component.repaint();
    }

    //-----------//
    // setParent //
    //-----------//
    public void setParent (BoardsPane parent)
    {
        this.parent = parent;
    }

    //------------//
    // setVisible //
    //------------//
    /**
     * Make this board visible or not.
     *
     * @param bool true for visible
     */
    public void setVisible (boolean bool)
    {
        component.setVisible(bool);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{" + ClassUtil.nameOf(this) + " " + name + "}";
    }

    //---------//
    // getBody //
    //---------//
    /**
     * Report the body part of the board.
     *
     * @return the body
     */
    protected JPanel getBody ()
    {
        return body;
    }

    //---------------//
    // getCountField //
    //---------------//
    protected JLabel getCountField ()
    {
        if (header == null) {
            return null;
        }

        return header.count;
    }

    //---------------//
    // getDumpButton //
    //---------------//
    /**
     * Report the Dump button of the board, if any.
     *
     * @return the dump button, or null
     */
    protected JButton getDumpButton ()
    {
        if (header == null) {
            return null;
        }

        return header.dump;
    }

    //---------------------//
    // getSelectionService //
    //---------------------//
    /**
     * Report the selection service this board is linked to.
     *
     * @return the selectionService
     */
    protected SelectionService getSelectionService ()
    {
        return selectionService;
    }

    //-----------//
    // getVipBox //
    //-----------//
    /**
     * Get access to the VIP box, if any.
     *
     * @return the vip label+field
     */
    protected LCheckBox getVipBox ()
    {
        if (header == null) {
            return null;
        }

        return header.vip;
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        component.setName(name + " board");
        component.setBorder(new TitledBorder(name));
        component.setInsets(UIUtil.adjustedSize(12),
                            UIUtil.adjustedSize(10),
                            UIUtil.adjustedSize(5),
                            UIUtil.adjustedSize(5)); // TLBR sides

        body.setNoInsets();

        CellConstraints cst = new CellConstraints();

        StringBuilder rowsSpec = new StringBuilder();

        if (header != null) {
            rowsSpec.append("pref,").append(Panel.getFieldInterline()).append(',');
        }

        rowsSpec.append("pref");

        FormLayout layout = new FormLayout("fill:pref:grow", rowsSpec.toString());

        PanelBuilder builder = new PanelBuilder(layout, component);

        if (header != null) {
            builder.add(header, cst.xy(1, 1));
        }

        builder.add(body, cst.xy(1, (header != null) ? 3 : 1));
    }

    //-------------//
    // emptyFields //
    //-------------//
    /**
     * Convenient method to empty all the text fields of a given JComponent.
     *
     * @param component the component to "blank".
     */
    public static void emptyFields (JComponent component)
    {
        for (Component comp : component.getComponents()) {
            if (comp instanceof JTextField) {
                ((JTextComponent) comp).setText("");
            }
        }
    }

    //------//
    // Desc //
    //------//
    /**
     * A way to describe a board kind.
     */
    public static class Desc
    {

        /** Default name for this board. */
        public final String name;

        /** Preferred position within its containing BoardsPane. */
        public final int position;

        public Desc (String name,
                     int position)
        {
            this.name = name;
            this.position = position;
        }
    }

    //--------//
    // Header //
    //--------//
    /**
     * The board header provides a line of perhaps count, vip, dump button.
     */
    private static class Header
            extends Panel
    {

        /** Output: Count of entities, if any. */
        private final JLabel count;

        /** Input / Output : VIP flag, if any. */
        private final LCheckBox vip;

        /** Dump button, if any. */
        private final JButton dump;

        Header (boolean withCount,
                boolean withVip,
                boolean withDump)
        {
            count = withCount ? new JLabel("") : null;
            vip = withVip ? new LCheckBox(resources.getString("vip.text"),
                                          resources.getString("vip.toolTipText")) : null;
            dump = withDump ? new JButton("Dump") : null;

            defineLayout();
        }

        private void defineLayout ()
        {
            CellConstraints cst = new CellConstraints();
            StringBuilder sb = new StringBuilder();
            // count label
            sb.append("15dlu:grow");
            // vip label+field
            sb.append(",")
                    .append(Panel.getFieldInterval()).append(",12dlu,")
                    .append(Panel.getLabelInterval()).append(",10dlu");
            // dump button
            sb.append(",")
                    .append(Panel.getFieldInterval()).append(",35dlu");

            FormLayout layout = new FormLayout(sb.toString(), "pref");
            PanelBuilder builder = new PanelBuilder(layout, this);

            if (dump != null) {
                builder.add(dump, cst.xyw(7, 1, 1));
            }

            if (vip != null) {
                builder.add(vip.getLabel(), cst.xy(3, 1));
                builder.add(vip.getField(), cst.xy(5, 1));
            }

            if (count != null) {
                builder.add(count, cst.xy(1, 1, "right, center"));
            }
        }
    }
}
