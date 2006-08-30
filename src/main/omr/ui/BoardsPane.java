//-----------------------------------------------------------------------//
//                                                                       //
//                          B o a r d s P a n e                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.selection.Selection;
import omr.selection.SelectionTag;
import omr.sheet.Sheet;
import omr.ui.util.Panel;
import omr.ui.view.RubberZoomedPanel;
import omr.util.Logger;

import static omr.ui.Board.Tag.*;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

/**
 * Class <code>BoardsPane</code> defines a comprehensive user board, where
 * data related to current point, run, section and glyph can be displayed
 * in dedicated boards, as well as a general-purpose Filter board and a
 * custom board.
 * 
 * <p>There is now one single BoardsPane for all views of the same sheet,
 * while the visibility of some of its boards may vary with the view at hand.
 * 
 * 
 * 
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class BoardsPane
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(BoardsPane.class);

    //~ Instance variables ------------------------------------------------

    // The concrete UI component
    private Panel component;

    // Unique (application-wide) name for this pane.
    private String name;

    // Collection of boards
    private Board[] boards;

    //~ Constructors ------------------------------------------------------

    /**
     * Create a BoardsPane, with selected boards
     *
     * @param sheet the related sheet
     * @param view the related view
     * @param boards a varying number of boards
     */
    public BoardsPane (Sheet             sheet,
                       RubberZoomedPanel view,
                       Board...          boards)
    {
        // View
        if (view == null) {
            logger.severe("BoardsPane needs a non-null view");
        }

        this.boards = boards;

        component = new Panel();
        component.setNoInsets();

        // Prepare layout elements
        final String panelInterline = Panel.getPanelInterline();
        StringBuffer sbr = new StringBuffer();
        for (int n = 0; n <= boards.length; n++) {
            if (n != 0) {
                sbr.append(", ").append(panelInterline).append(", ");
            }
            sbr.append("pref");
        }

        FormLayout layout = new FormLayout
            ("pref",
             sbr.toString());

        Panel panel = new Panel();
        panel.setNoInsets();

        PanelBuilder builder = new PanelBuilder(layout, panel);
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        // Now add the desired components, using provided order
        int r = 1;
        for (Board board : boards) {
            builder.add(board.getComponent(), cst.xy(1, r));
            switch(board.getTag()) {
            case PIXEL :
                PixelBoard pixelBoard = (PixelBoard) board;
                // inputs
                List<Selection> inputs = new ArrayList<Selection>();
                inputs.add(sheet.getSelection(SelectionTag.PIXEL));
                inputs.add(sheet.getSelection(SelectionTag.LEVEL));
                pixelBoard.setInputSelectionList(inputs);
                // output
                pixelBoard.setOutputSelection
                        (sheet.getSelection(SelectionTag.PIXEL));
                break;

            case RUN :
            case SECTION :
                // Connections done by constructor, since they depend on Lag
                break;

            case GLYPH :
                break;

            case CHECK :
                break;

            case CUSTOM :
                break;

            default :
                logger.severe ("Unexpected Board Tag : " + board.getTag());
            }
            r += 2;
        }

        component.add(builder.getPanel());
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component
     *
     * @return the concrete component
     */
    public JComponent getComponent()
    {
        return component;
    }

    //---------//
    // setName //
    //---------//
    /**
     * Assign the unique name for this boards pane
     *
     * @param name the assigned name
     */
    public void setName (String name)
    {
        this.name = name;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the unique name for this boards pane
     *
     * @return the declared name
     */
    public String getName()
    {
        return name;
    }

    //----------//
    // toString //
    //----------//
    @Override
        public String toString()
    {
        return "{BoardsPane " + name + "}";
    }

    //-------//
    // shown //
    //-------//
    /**
     * Invoked when the boardsPane has been selected
     */
    public void shown()
    {
        ///logger.info("+BoardPane " + name + " Shown");
        for (Board board : boards) {
            board.boardShown();
        }
    }

    //--------//
    // hidden //
    //--------//
    /**
     * Invoked when the boardsPane has been made deselected
     */
    public void hidden()
    {
        ///logger.info("-BoardPane " + name + " Hidden");
        for (Board board : boards) {
            board.boardHidden();
        }
    }
}
