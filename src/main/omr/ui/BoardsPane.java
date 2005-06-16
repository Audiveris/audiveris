//-----------------------------------------------------------------------//
//                                                                       //
//                          B o a r d s P a n e                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphLagView;
import omr.glyph.ui.GlyphBoard;
import omr.lag.LagView;
import omr.lag.Run;
import omr.lag.Section;
import omr.stick.StickView;

import java.awt.*;
import java.util.EnumMap;
import javax.swing.*;

/**
 * Class <code>BoardsPane</code> defines a comprehensive user board, where
 * data related to current point, run, section and glyph is displayed in
 * dedicated boards, as well as a general-purpose Filter board.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class BoardsPane
    extends JPanel
{
    //~ Instance variables ------------------------------------------------

    /** The board for pixel information */
    public  final PixelBoard   pixelBoard;

    /** The board for section and run information */
    public  final SectionBoard sectionBoard;

    /** The board for glyph information */
    public  final GlyphBoard   glyphBoard;

    /** The board for stick processing information */
    public  final FilterBoard  filterBoard;

    /** The board for custom processing */
    public  final Board        customBoard;

    // Map to the various boards
    private final EnumMap<Board.Tag, Board> map;

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
        // Populate the map of boards
        map = new EnumMap<Board.Tag,Board>(Board.Tag.class);
        for (Board board : boards) {
            map.put(board.getTag(), board);
        }

        // Now add the desired components, using always the same order and
        // layout
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;

        //c.weightx = 1.0; //0.5;
        c.gridx   = 0;
        c.gridy   = 0;

        // Pixel
        pixelBoard = (PixelBoard) map.get(Board.Tag.PIXEL);
        if (pixelBoard != null) {
            add(pixelBoard, c);
            pixelBoard.setPixelFocus(view);
            view.addObserver(pixelBoard);
            pixelBoard.update((Point) null);
        }

        c.gridy = GridBagConstraints.RELATIVE;

        // Section
        sectionBoard = (SectionBoard) map.get(Board.Tag.SECTION);
        if (sectionBoard != null) {
            add(sectionBoard, c);
            if (view instanceof LagView) {
                LagView lagView = (LagView) view;
                sectionBoard.setSectionFocus(lagView);
                lagView.addObserver(sectionBoard);
            }

            sectionBoard.update((Section) null);
            sectionBoard.update((Run) null);
        }

        // Glyph
        glyphBoard = (GlyphBoard) map.get(Board.Tag.GLYPH);
        if (glyphBoard != null) {
            add(glyphBoard, c);
            if (view instanceof GlyphLagView) {
                GlyphLagView glyphView = (GlyphLagView) view;
                glyphBoard.setGlyphFocus(glyphView);
                glyphView.addObserver(glyphBoard);
            }

            glyphBoard.update((Glyph) null);
        }

        // Check board
        filterBoard = (FilterBoard) map.get(Board.Tag.FILTER);
        if (filterBoard != null) {
            add(filterBoard, c);
            if (view instanceof StickView) {
                StickView stickView = (StickView) view;
                stickView.setFilterMonitor(filterBoard);
            }

            filterBoard.tellHtml(null);
        }

        // Custom board
        customBoard = map.get(Board.Tag.CUSTOM);
        if (customBoard != null) {
            add(customBoard, c);
        }
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // getBoard //
    //----------//
    /**
     * Allows to return a specific board, knowing its tag
     *
     * @param tag the tag of the desired board
     * @return the desired board, or null if not found
     */
    public Board getBoard (Board.Tag tag)
    {
        return map.get(tag);
    }
}
