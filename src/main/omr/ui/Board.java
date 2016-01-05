//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           B o a r d                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.ui.field.LCheckBox;
import omr.ui.util.Panel;

import omr.util.ClassUtil;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Class {@code Board} defines the common properties of any user board such as
 * PixelBoard, SectionBoard, and the like.
 * <p>
 * Each board has a standard header composed of a title, a horizontal separator and optionally a
 * dump button. The board body is handled by the subclass.</p>
 * <p>
 * Any board can be (de)selected in its containing {@link BoardsPane}. This can be done
 * programmatically using {@link #setSelected(boolean)} and manually (via a right-click in the
 * BoardsPane).</p>
 * <p>
 * Only selected boards can be seen in the BoardsPane display. A selected board can be made
 * currently (in)visible programmatically using {@link #setVisible(boolean)}.
 * Typically, {@link omr.check.CheckBoard}'s are visible only when they carry
 * glyph information.</p>
 * <p>
 * By default, any board can have a related SelectionService, used for subscribe (input) and publish
 * (output). When {@link #connect} is called, the board instance is subscribed to its
 * SelectionService for a specific collection of event classes. Similarly, {@link #disconnect}
 * un-subscribes the Board instance from the same event classes.</p>
 * <p>
 * This {@code Board} class is still an abstract class, since the onEvent() method must be
 * provided by every subclass.</p>
 *
 * @author Hervé Bitteur
 */
public abstract class Board
        implements EventSubscriber<UserEvent>, Comparable<Board>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Board.class);

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

    public static final Desc EVAL = new Desc("Eval", 700);

    public static final Desc SHAPE = new Desc("Shape", 800);

    public static final Desc CHECK = new Desc("Check", 900);

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

    /** The event service this board interacts with. */
    private final SelectionService selectionService;

    /** The collection of event classes to be observed. */
    private final Class<?>[] eventsRead;

    /** The preferred position in BoardsPane sequence. */
    private final int position;

    /** Board is selected?. */
    private boolean selected;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a board from a pre-defined descriptor (name + position).
     *
     * @param desc             the board descriptor
     * @param selectionService the related selection service for input & output
     * @param eventsRead       the collection of event classes to observe
     * @param withCount        true for a count field
     * @param withVip          true for a VIP label & field
     * @param withDump         true for a dump button
     * @param selected         true to make the board initially selected
     */
    public Board (Desc desc,
                  SelectionService selectionService,
                  Class<?>[] eventsRead,
                  boolean withCount,
                  boolean withVip,
                  boolean withDump,
                  boolean selected)
    {
        this(
                desc.name,
                desc.position,
                selectionService,
                eventsRead,
                withCount,
                withVip,
                withDump,
                selected);
    }

    /**
     * Create a board, with (dynamic) name and position.
     *
     * @param name             a name assigned to the board
     * @param position         the preferred position within BoardsPane display
     * @param selectionService the related selection service for input & output
     * @param eventsRead       the collection of event classes to observe
     * @param withCount        true for a count field
     * @param withVip          true for a VIP label & field
     * @param withDump         true for a dump button
     * @param selected         true to make the board selected
     */
    public Board (String name,
                  int position,
                  SelectionService selectionService,
                  Class[] eventsRead,
                  boolean withCount,
                  boolean withVip,
                  boolean withDump,
                  boolean selected)
    {
        this.name = name;
        this.position = position;
        this.selectionService = selectionService;
        this.eventsRead = eventsRead;
        this.selected = selected;

        // Layout header and body parts
        header = new Header(name, withCount, withVip, withDump);
        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
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
                ((JTextField) comp).setText("");
            }
        }
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Allow to sort boards according to their preferred display position.
     *
     * @param that the other board to compare to
     * @return comparison result
     */
    @Override
    public int compareTo (Board that)
    {
        return Integer.signum(this.position - that.position);
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
     * @param newBool true for selected, false for deselected
     */
    public void setSelected (boolean newBool)
    {
        // No modification?
        if (selected == newBool) {
            return;
        }

        if (newBool) {
            connect();
        } else {
            disconnect();
        }

        selected = newBool;

        if (parent != null) {
            parent.update();
        }
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
        return header.vip;
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        component.setNoInsets();
        body.setNoInsets();

        CellConstraints cst = new CellConstraints();
        FormLayout layout = new FormLayout(
                "pref",
                "pref," + Panel.getFieldInterline() + ",pref");
        PanelBuilder builder = new PanelBuilder(layout, component);

        builder.add(header, cst.xy(1, 1));
        builder.add(body, cst.xy(1, 3));
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
        //~ Instance fields ------------------------------------------------------------------------

        /** Default name for this board. */
        public final String name;

        /** Preferred position within its containing BoardsPane. */
        public final int position;

        //~ Constructors ---------------------------------------------------------------------------
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
     * The board header is a horizontal line with the board title,
     * and perhaps a dump button.
     */
    private static class Header
            extends Panel
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The board title. */
        private final String title;

        /** Output: Count of entities, if any. */
        private final JLabel count;

        /** Input / Output : VIP flag, if any. */
        private final LCheckBox vip;

        /** Dump button, if any. */
        private final JButton dump;

        //~ Constructors ---------------------------------------------------------------------------
        public Header (String title,
                       boolean withCount,
                       boolean withVip,
                       boolean withDump)
        {
            this.title = title;

            count = withCount ? new JLabel("") : null;
            vip = withVip ? new LCheckBox("Vip", "Is this entity flagged as VIP?") : null;
            dump = withDump ? new JButton("Dump") : null;

            setNoInsets();
            defineLayout();
        }

        //~ Methods --------------------------------------------------------------------------------
        private void defineLayout ()
        {
            CellConstraints cst = new CellConstraints();
            StringBuilder sb = new StringBuilder();
            // title & separator
            sb.append("107dlu");
            // count label
            sb.append(",").append(Panel.getFieldInterval()).append(",15dlu");
            // vip label+box
            sb.append(",").append(Panel.getFieldInterval()).append(",12dlu,").append(
                    Panel.getLabelInterval()).append(",10dlu");
            // dump button
            sb.append(",").append(Panel.getFieldInterval()).append(",35dlu");

            FormLayout layout = new FormLayout(sb.toString(), "pref");
            PanelBuilder builder = new PanelBuilder(layout, this);

            int sepEnd = 9;

            if (dump != null) {
                sepEnd = 7;
                builder.add(dump, cst.xyw(9, 1, 1));
            }

            if (vip != null) {
                sepEnd = 3;
                builder.add(vip.getLabel(), cst.xy(5, 1));
                builder.add(vip.getField(), cst.xy(7, 1));
            }

            if (count != null) {
                sepEnd = 1;
                builder.add(count, cst.xy(3, 1, "right, center"));
            }

            builder.addSeparator(title, cst.xyw(1, 1, sepEnd));
        }
    }
}
