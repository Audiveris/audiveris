//-----------------------------------------------------------------------//
//                                                                       //
//                               B o a r d                               //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionObserver;
import omr.ui.util.Panel;
import omr.util.Logger;

import java.awt.*;
import javax.swing.*;

/**
 * Class <code>Board</code> defines the common properties of any user board
 * such as PixelBoard, SectionBoard, and the like.
 *
 * This is still an abstract class, since the update() method must be
 * provided by every subclass.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class Board
    implements SelectionObserver
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(Board.class);

    /** To indicate that value is invalid */
    public static final int NO_VALUE = 0;

    //~ Instance variables ------------------------------------------------

    // Concrete UI panel
    protected final Panel component;

    // Board Tag
    protected Tag tag;

    // Board instance name
    protected String name;

    // Input selection
    protected Selection inputSelection;

    // Output selection (if any)
    protected Selection outputSelection;

    //~ Constructors ------------------------------------------------------

    //-------//
    // Board //
    //-------//
    /**
     * Create a board
     *
     * @param tag the tag to wrap the board
     */
    public Board (Tag tag,
            String name)
    {
        this.tag = tag;
        this.name = name;

        component = new Panel();
        component.setNoInsets();
    }

    //~ Methods -----------------------------------------------------------

    //-------------------//
    // setInputSelection //
    //-------------------//
    /**
     * Inject the selection object where input is to be read from
     *
     * @param inputSelection the proper input selection object
     */
    public void setInputSelection (Selection inputSelection)
    {
        if (this.inputSelection != null) {
            this.inputSelection.deleteObserver(this);
        }

        this.inputSelection = inputSelection;
    }

    //-------------------//
    // setOutputSelection //
    //-------------------//
    /**
     * Inject the selection object where output is to be written to
     *
     * @param outputSelection the proper output selection object
     */
    public void setOutputSelection (Selection outputSelection)
    {
        this.outputSelection = outputSelection;
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component
     *
     * @return the concrete component
     */
    public JPanel getComponent()
    {
        return component;
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

    //---------//
    // getName //
    //---------//
    public String getName()
    {
        return name;
    }

    //-------------//
    // emptyFields //
    //-------------//
    /**
     * Empty all the text fields of a given JComponent
     *
     * @param component the component to "blank".
     */
    public static void emptyFields(JComponent component)
    {
        for (Component comp : component.getComponents()) {
            if (comp instanceof JTextField){
                ((JTextField) comp).setText("");
            }
        }
    }

    //------------//
    // boardShown //
    //------------//
    /**
     * Invoked when the board has been made visible.
     */
    public void boardShown()
    {
        ///logger.info("+Board " + tag + " Shown");
        if (inputSelection != null) {
            inputSelection.addObserver(this);
        }
    }

    //-------------//
    // boardHidden //
    //-------------//
    /**
     * Invoked when the board has been made invisible.
     */
    public void boardHidden()
    {
        ///logger.info("-Board " + tag + " Hidden");
        if (inputSelection != null) {
            inputSelection.deleteObserver(this);
        }
    }

    //--------//
    // update //
    //--------//
    /**
     * Just a placeholder
     *
     * @param selection the Selection which emits this notification
     * @param hint potential notification hint
     */
    public void update (Selection selection,
                        SelectionHint hint)
    {
        logger.info("Board default update. selection=" + selection);
    }

    //~ Classes -----------------------------------------------------------

    //-----//
    // Tag //
    //-----//
    /**
     * Enum <code>Tag</code> is used to refer to the various user boards.
     */
    public static enum Tag
    {
            /** Board for pixel info (coordinates, pixel grey level) */
            PIXEL   ("Pixel"),

            /** Board for run info */
            RUN     ("Run"),

            /** Board for section info */
            SECTION ("Section"),

            /** Board for glyph info */
            GLYPH   ("Glyph"),

            /** Board for check results */
            CHECK   ("Check"),

            /** Custom board */
            CUSTOM  ("Custom");

        //~ Instance variables --------------------------------------------

        private String label;

        //~ Constructors --------------------------------------------------

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
            public String toString()
        {
            return label;
        }
    }
}
