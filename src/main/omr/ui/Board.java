//----------------------------------------------------------------------------//
//                                                                            //
//                                 B o a r d                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.log.Logger;

import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.ui.util.Panel;

import omr.util.ClassUtil;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.bushe.swing.event.EventSubscriber;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Class <code>Board</code> defines the common properties of any user board
 * such as PixelBoard, SectionBoard, and the like.
 *
 * <p>Each board has a standard header composed of a "groom" button to expand
 * and collapse the board, a title, and a horizontal separator. The board body
 * is handled by the subclass.
 *
 * <p>By default, any board can have a related SelectionService, used for
 * subscribe (input) and publish (output). When {@link #connect} is called, the
 * board instance is subscribed to its SelectionService for a specific
 * collection of event classes. Similarly, {@link #disconnect} unsubscribes the
 * Board instance from the same event classes.
 *
 * <p>This is still an abstract class, since the onEvent() method must be
 * provided by every subclass.
 *
 * @author Hervé Bitteur
 */
public abstract class Board
    implements EventSubscriber<UserEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Board.class);

    /** Color for groom buttom */
    private static final Color groomColor = new Color(240, 240, 240);

    //~ Instance fields --------------------------------------------------------

    /** The swing component of the Board instance */
    protected final Panel component = new Panel();

    /** The body part of the component */
    protected final Panel body = new Panel();

    /** The event service this board interacts with */
    protected final SelectionService selectionService;

    /** The collection of event classes to be observed */
    protected final Collection<Class<?extends UserEvent>> eventList;

    /** The Board instance name */
    protected String name;

    /** The groom for expand/collapse actions */
    private final Groom groom;

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Board //
    //-------//
    /**
     * Create a board
     *
     * @param name a name assigned to the board, for debug reason
     * @param title the string to appear as the board title
     * @param selectionService the related selection service (for input & output)
     * @param eventList the collection of event classes to observe
     * @param expanded true to pre-expand the board, false to pre-collapse
     */
    public Board (String                                name,
                  String                                title,
                  SelectionService                      selectionService,
                  Collection<Class<?extends UserEvent>> eventList,
                  boolean                               expanded)
    {
        this.name = name;
        this.selectionService = selectionService;
        this.eventList = eventList;

        groom = new Groom(expanded);

        // Layout header and body parts
        defineBoardLayout(title);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // emptyFields //
    //-------------//
    /**
     * Empty all the text fields of a given JComponent
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

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component
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
     * Report a distinct name for this board instance
     *
     * @return an instance name
     */
    public String getName ()
    {
        return name;
    }

    //---------//
    // connect //
    //---------//
    /**
     * Invoked when the board has been made visible, to connect to input
     * selections.
     */
    public void connect ()
    {
        if ((eventList != null) && groom.expanded) {
            ///logger.warning("connect " + this + " on " + selectionService);
            for (Class eventClass : eventList) {
                selectionService.subscribeStrongly(eventClass, this);

                // Refresh with latest data for this event class
                UserEvent event = (UserEvent) selectionService.getLastEvent(
                    eventClass);

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
     * Invoked when the board has been made invisible, to disconnect from input
     * selections.
     */
    public void disconnect ()
    {
        if ((eventList != null) && groom.expanded) {
            ///logger.warning("disconnect " + this + " from " + selectionService);
            for (Class eventClass : eventList) {
                selectionService.unsubscribe(eventClass, this);
            }
        }
    }

    //--------//
    // expand //
    //--------//
    /**
     * Programmatically expand this board
     */
    public void expand ()
    {
        if (!groom.expanded) {
            groom.actionPerformed(null);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return ClassUtil.nameOf(this);
    }

    //---------//
    // getBody //
    //---------//
    protected JPanel getBody ()
    {
        return body;
    }

    //-------------------//
    // defineBoardLayout //
    //-------------------//
    private void defineBoardLayout (String title)
    {
        component.setNoInsets();
        body.setNoInsets();

        CellConstraints cst = new CellConstraints();
        FormLayout      layout = new FormLayout(
            "pref",
            "pref," + Panel.getFieldInterline() + ",pref");
        PanelBuilder    builder = new PanelBuilder(layout, component);

        builder.add(new Header(title), cst.xy(1, 1));
        builder.add(body, cst.xy(1, 3));
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------//
    // Groom //
    //-------//
    /**
     * The groom is in charge of expanding / collapsing the board body panel
     *
     * TODO: For the time being, we can't (de)connect at close/open events,
     * because some boards (indirectly) take their input from other boards
     * (example: glyph needs run or section)
     */
    private class Groom
        extends AbstractAction
    {
        //~ Instance fields ----------------------------------------------------

        /** Is the body panel expanded (or collapsed)? */
        private boolean expanded;

        //~ Constructors -------------------------------------------------------

        public Groom (boolean expanded)
        {
            this.expanded = expanded;

            // Initialize the board
            display();
        }

        //~ Methods ------------------------------------------------------------

        public final void actionPerformed (ActionEvent e)
        {
            expanded = !expanded;
            display();
            handleConnection();
        }

        private void display ()
        {
            if (expanded) {
                putValue(Action.NAME, "X");
                putValue(Action.SHORT_DESCRIPTION, "Collapse");
            } else {
                putValue(Action.NAME, "+");
                putValue(Action.SHORT_DESCRIPTION, "Expand");
            }

            body.setVisible(expanded);
            component.invalidate();
        }

        private void handleConnection ()
        {
            if (expanded) {
                // Connect the input
                connect();
            } else {
                // Disconnect the input
                // Trick: disconnect() method is a no-op if not expanded, so...
                expanded = true;
                disconnect();
                expanded = false;
            }
        }
    }

    //--------//
    // Header //
    //--------//
    /**
     * The board header is a horizontal line with the groom and the board title
     */
    private class Header
        extends Panel
    {
        //~ Instance fields ----------------------------------------------------

        /** The board title */
        private final String title;

        //~ Constructors -------------------------------------------------------

        public Header (String title)
        {
            this.title = title;
            setNoInsets();
            defineLayout();
        }

        //~ Methods ------------------------------------------------------------

        private void defineLayout ()
        {
            /** Groom of the board */
            JButton button = new JButton(groom);
            button.setBorderPainted(false);
            button.setBackground(groomColor);

            CellConstraints cst = new CellConstraints();
            FormLayout      layout = new FormLayout(
                "175dlu," + Panel.getFieldInterval() + ",pref",
                "pref");
            PanelBuilder    builder = new PanelBuilder(layout, this);

            builder.addSeparator(title, cst.xy(1, 1));
            builder.add(button, cst.xy(3, 1));
        }
    }
}
