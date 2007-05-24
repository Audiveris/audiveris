//----------------------------------------------------------------------------//
//                                                                            //
//                                 B o a r d                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.ui;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionObserver;

import omr.ui.util.Panel;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.*;
import java.util.List;

import javax.swing.*;
import omr.selection.SelectionManager;

/**
 * Class <code>Board</code> defines the common properties of any user board such
 * as PixelBoard, SectionBoard, and the like.
 *
 * <p>By default, any board can have multiple inputSelection and an
 * outputSelection objects. When {@link #boardShown} is called, the board
 * instance is added as an observer to its various inputSelection
 * objects. Similarly, {@link #boardHidden} deletes the observer from the same
 * inputSelection objects.
 *
 * <p>This is still an abstract class, since the update() method must be
 * provided by every subclass.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class Board
    implements SelectionObserver
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Board.class);

    //~ Enumerations -----------------------------------------------------------

    //-----//
    // Tag //
    //-----//
    /**
     * Enum <code>Tag</code> is used to refer to the various user boards.
     */
    public enum Tag {
        /** Board for check results */
        CHECK("Check"),
        /** Custom board */
        CUSTOM("Custom"), 
        /** Board for glyph info */
        GLYPH("Glyph"), 
        /** Board for pixel info (coordinates, pixel grey level) */
        PIXEL("Pixel"), 
        /** Board for run info */
        RUN("Run"), 
        /** Board for section info */
        SECTION("Section");
        // For description only
        private String label;

        //-----//
        // Tag //
        //-----//
        /**
         * Create a tag enum item, with its provided description
         *
         * @param label the tag description
         */
        Tag (String label)
        {
            this.label = label;
        }

        //----------//
        // toString //
        //----------//
        /**
         * Report the tag description
         *
         * @return the tag description
         */
        @Override
        public String toString ()
        {
            return label;
        }
    }

    //~ Instance fields --------------------------------------------------------

    /**
     * The swing component of the Board instance
     */
    protected final Panel component;

    /**
     * The collection of (input) selection entities to be observed
     */
    protected List<Selection> inputSelectionList;

    /**
     * The Output selection (if any)
     */
    protected Selection outputSelection;

    /**
     * The Board instance name
     */
    protected String name;

    /**
     * The Board Tag
     */
    protected Tag tag;

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Board //
    //-------//
    /**
     * Create a board
     *
     * @param tag the tag to wrap the board
     */
    public Board (Tag    tag,
                  String name)
    {
        this.tag = tag;
        this.name = name;

        component = new Panel();
        component.setNoInsets();
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

    //------------//
    // disconnect //
    //------------//
    /**
     * Invoked when the board has been made invisible, to disconnect from input
     * selections.
     */
    public void disconnect ()
    {
        ///logger.info("-Board " + tag + " Hidden");
        if (inputSelectionList != null) {
            for (Selection input : inputSelectionList) {
                input.deleteObserver(this);
            }
        }
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
        ///logger.info("+Board " + tag + " Shown");
        if (inputSelectionList != null) {
            for (Selection input : inputSelectionList) {
                input.addObserver(this);
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
    @Implement(SelectionObserver.class)
    public String getName ()
    {
        return name;
    }

    //--------//
    // getTag //
    //--------//
    /**
     * Report the tag of the board
     *
     * @return the board tag
     */
    public Tag getTag ()
    {
        return tag;
    }

    //-----------------------//
    // setInputSelectionList //
    //-----------------------//
    /**
     * Inject the selection objects where input are to be read from
     *
     * @param inputSelectionList the proper list of input selection objects
     */
    public void setInputSelectionList (List<Selection> inputSelectionList)
    {
        if (this.inputSelectionList != null) {
            for (Selection input : this.inputSelectionList) {
                input.deleteObserver(this);
            }
        }

        this.inputSelectionList = inputSelectionList;
    }

    //--------------------//
    // setOutputSelection //
    //--------------------//
    /**
     * Inject the selection object where output is to be written to
     *
     * @param outputSelection the proper output selection object
     */
    public void setOutputSelection (Selection outputSelection)
    {
        this.outputSelection = outputSelection;
    }

    //--------//
    // update //
    //--------//
    /**
     * This implementation is just a placeholder
     *
     * @param selection the Selection object which emits this notification
     * @param hint a potential notification hint
     */
    @Implement(SelectionObserver.class)
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        logger.info("Board default update. selection=" + selection);
    }
}
