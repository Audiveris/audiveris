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

import omr.check.CheckBoard;
import omr.glyph.Glyph;
import omr.glyph.GlyphLagView;
import omr.glyph.ui.GlyphBoard;
import omr.lag.LagView;
import omr.lag.Run;
import omr.lag.Section;
import omr.stick.StickView;
import omr.ui.Panel;
import omr.util.Logger;

import static omr.ui.Board.Tag.*;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.util.EnumMap;
import javax.swing.*;

/**
 * Class <code>BoardsPane</code> defines a comprehensive user board, where
 * data related to current point, run, section and glyph can be displayed
 * in dedicated boards, as well as a general-purpose Filter board and a
 * custom board.
 *
 * <p>One BoardsPane is dedicated to one view of one sheet.
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
    private JPanel component;

    // Unique (application-wide) name for this pane.
    private String name;

    //~ Constructors ------------------------------------------------------

    /**
     * Create a BoardsPane, with selected boards
     *
     * @param view the related view
     * @param boards a varying number of boards
     */
    public BoardsPane (RubberZoomedPanel view,
                       Board...          boards)
    {
        // View
        if (view == null) {
            logger.severe("BoardsPane needs a non-null view");
        }

        component = new JPanel();

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
                pixelBoard.setPixelFocus(view);
                view.addObserver(pixelBoard);
                pixelBoard.update((Point) null);
                break;

            case SECTION :
                SectionBoard sectionBoard = (SectionBoard) board;
                LagView lagView = (LagView) view;
                sectionBoard.setSectionFocus(lagView);
                lagView.addObserver(sectionBoard);
                sectionBoard.update((Section) null);
                sectionBoard.update((Run) null);
                break;

            case GLYPH :
                GlyphBoard glyphBoard = (GlyphBoard) board;
                if (view instanceof GlyphLagView) {
                    GlyphLagView glyphView = (GlyphLagView) view;
                    glyphBoard.setGlyphFocus(glyphView);
                    glyphView.addObserver(glyphBoard);
                }
                glyphBoard.update((Glyph) null);
                break;

            case CHECK :
                // CheckBoard checkBoard = (CheckBoard) board;
                // if (view instanceof StickView) {
                //     StickView stickView = (StickView) view;
                //     stickView.setCheckMonitor(checkBoard);
                // }
                break;

            case CUSTOM :
                // CustomBoard customBoard = board;
                break;
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
}
