//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           B o a r d                                            //
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
package org.audiveris.omr.ui;

import org.audiveris.omr.ui.field.LCheckBox;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.ClassUtil;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bushe.swing.event.EventSubscriber;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Component;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;

/**
 * Class <code>Board</code> defines the common properties of any user board such as
 * PixelBoard, SectionBoard, and the like.
 * <p>
 * Each board has a standard header composed of a title, a horizontal separator and optionally a
 * dump button. The board body is handled by the subclass.
 * <p>
 * Any board can be (de)selected in its containing {@link BoardsPane}. This can be done
 * programmatically using {@link #setSelected(boolean)} and manually (via a right-click in the
 * BoardsPane).
 * <p>
 * Only selected boards are displayed in the BoardsPane display. A selected board can be made
 * currently (in)visible programmatically using {@link #setVisible(boolean)}.
 * Typically, {@link org.audiveris.omr.check.CheckBoard}'s are visible only when they carry
 * glyph information.
 * <p>
 * By default, any board can have a related SelectionService, used for subscribe (input) and publish
 * (output). When {@link #connect} is called, the board instance is subscribed to its
 * SelectionService for a specific collection of event classes. Similarly, {@link #disconnect}
 * unsubscribes the Board instance from the same event classes.
 * <p>
 * This <code>Board</code> class is still an abstract class, since the onEvent() method must be
 * provided by every subclass.
 *
 * @author Hervé Bitteur
 */
public abstract class Board
        implements EventSubscriber<UserEvent>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Board.class);

    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(Board.class);

    /** Minimum width available for a board. */
    public static final int MIN_BOARD_WIDTH = UIUtil.adjustedSize(350);

    // Predefined boards names with preferred display positions
    public static final Desc PIXEL = new Desc("Pixel", 100);

    public static final Desc BINARIZATION = new Desc("Binarization filter", 125);

    public static final Desc BINARY = new Desc("Binary pixel", 150);

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
    public static final Comparator<Board> byPosition = (Board b1,
                                                        Board b2) -> Integer.compare(
                                                                b1.position,
                                                                b2.position);

    //~ Instance fields ----------------------------------------------------------------------------

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

    /**
     * The event service this board interacts with.
     * It can serve different event classes, like a list of entities or an entity ID.
     */
    private final SelectionService selectionService;

    /** The collection of event classes to be observed. */
    private final Class<?>[] eventsRead;

    /** The preferred position in BoardsPane sequence. */
    private final int position;

    /** The split container that will contain the boards pane. */
    private JSplitPane splitContainer;

    /** Board is selected? (it appears in boards pane). */
    private boolean selected;

    /** Cached music font family, if any. To trigger board symbols update only when needed. */
    protected MusicFamily cachedMusicFamily;

    /** Cached text font family, if any. To trigger board symbols update only when needed. */
    protected TextFamily cachedTextFamily;

    //~ Constructors -------------------------------------------------------------------------------

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

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // connect //
    //---------//
    /**
     * Connect to input selections.
     */
    public void connect ()
    {
        logger.debug("connect {}", this);

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

        // Update action if any
        update();
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        component.setName(name + " board");
        component.setBorder(new TitledBorder(name));
        component.setInsets(
                UIUtil.adjustedSize(12),
                UIUtil.adjustedSize(10),
                UIUtil.adjustedSize(5),
                UIUtil.adjustedSize(5)); // TLBR sides

        body.setNoInsets();

        final StringBuilder rowsSpec = new StringBuilder();

        if (header != null) {
            rowsSpec.append("pref,").append(Panel.getFieldInterline()).append(',');
        }

        rowsSpec.append("pref");

        final FormLayout layout = new FormLayout("fill:pref:grow", rowsSpec.toString());

        FormBuilder builder = FormBuilder.create().layout(layout).panel(component);

        if (header != null) {
            builder.addRaw(header).xy(1, 1);
        }

        builder.addRaw(body).xy(1, (header != null) ? 3 : 1);
    }

    //------------//
    // disconnect //
    //------------//
    /**
     * Disconnect from input selections.
     */
    public void disconnect ()
    {
        logger.debug("disconnect {}", this);

        if (eventsRead != null) {
            for (Class<?> eventClass : eventsRead) {
                selectionService.unsubscribe(eventClass, this);
            }
        }
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

    //-------------------//
    // getSplitContainer //
    //-------------------//
    /**
     * Report the JSplitPane that will contain the boards pane.
     *
     * @return the related split container
     */
    public JSplitPane getSplitContainer ()
    {
        return splitContainer;
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

    //-------------------//
    // setSplitContainer //
    //-------------------//
    /**
     * Set the JSplitPane that will contain the boards pane.
     *
     * @param sp the related split container
     */
    public void setSplitContainer (JSplitPane sp)
    {
        splitContainer = sp;
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

    //--------//
    // update //
    //--------//
    /**
     * Trigger an update of the board.
     */
    public void update ()
    {
        // Void by default
    }

    //~ Static Methods -----------------------------------------------------------------------------

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

    //~ Inner Classes ------------------------------------------------------------------------------

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
            vip = withVip ? new LCheckBox(
                    resources.getString("vip.text"),
                    resources.getString("vip.toolTipText")) : null;
            dump = withDump ? new JButton("Dump") : null;

            defineLayout();
        }

        private void defineLayout ()
        {
            final StringBuilder sb = new StringBuilder();
            // count label
            sb.append("15dlu:grow");
            // vip label+field
            sb.append(",").append(Panel.getFieldInterval()).append(",12dlu,").append(
                    Panel.getLabelInterval()).append(",10dlu");
            // dump button
            sb.append(",").append(Panel.getFieldInterval()).append(",35dlu");

            final FormLayout layout = new FormLayout(sb.toString(), "pref");
            final FormBuilder builder = FormBuilder.create().layout(layout).panel(this);

            if (dump != null) {
                builder.addRaw(dump).xyw(7, 1, 1);
            }

            if (vip != null) {
                builder.addRaw(vip.getLabel()).xy(3, 1);
                builder.addRaw(vip.getField()).xy(5, 1);
            }

            if (count != null) {
                builder.addRaw(count).xy(1, 1, "right, center");
            }
        }
    }
}
