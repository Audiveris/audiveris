//-----------------------------------------------------------------------//
//                                                                       //
//                           S c o r e V i e w                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import omr.Main;
import omr.sheet.Sheet;
import omr.util.Logger;
import omr.ui.view.Zoom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Iterator;
import omr.ui.SheetAssembly;
import omr.ui.view.Rubber;
import omr.ui.view.RubberZoomedPanel;
import omr.ui.view.ScrollView;

/**
 * Class <code>ScoreView</code> encapsulates the horizontal display of all
 * the score systems which may encompass several pages & systems. It knows
 * about the display zoom ratio of the score.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreView
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(ScoreView.class);

    // Symbolic constants

    /** Height in pixels of a stave display : {@value} */
    public static final int STAVE_AREA_HEIGHT = 100;

    /** Height in pixels above/under a stave display : {@value} */
    public static final int STAVE_MARGIN_HEIGHT = 40;

    /** Horizontal offset in pixels of the score origin : {@value} */
    public static final int SCORE_INIT_X = 200;

    /** Vertical offset in pixels of the score origin : {@value} */
    public static final int SCORE_INIT_Y = 150;

    /** Horizontal gutter in pixels between two systems : {@value} */
    public static final int INTER_SYSTEM = 100;

    /** Vertical distance in pixels between two lines of a staff :
        {@value} */
    public static final int INTER_LINE = 16;

    /** Horizontal gutter in pixels between two pages : {@value} */
    public static final int INTER_PAGE = 200;

    /** Number of lines in a staff : {@value} */
    public static final int LINE_NB = 5;

    /** Height in pixels of one staff : {@value} */
    public static final int STAFF_HEIGHT = (LINE_NB - 1) * INTER_LINE;

    /** Used to code fractions with an integer value, with a resolution of
        1/{@value} */
    public static final int BASE = 1024;

    //~ Instance variables ------------------------------------------------

    // The related score
    private final Score score;

    // Display zoom
    private final Zoom zoom = new Zoom(0.5d);

    // Mouse rubber
    private final Rubber rubber = new Rubber(zoom);

    // Default view position
    private final Point defaultPosition
            = new Point(0, zoom.scaled(SCORE_INIT_Y / 2));

    // The displayed panel
    private final MyPanel panel = new MyPanel(zoom, rubber);

    // The scroll pane
    private final ScrollView pane = new ScrollView(panel);

    // Meant for Point computation, w/o continuous allocations
    //
    // Point in score display in units  (not zoomed)
    private Point scrPt = new Point();

    // Point in virtual sheet display in units  (not zoomed, not skewed)
    private PagePoint pagPt = new PagePoint();

    // To avoid circular updating
    private volatile transient boolean localSelfUpdating;

    //~ Constructors ------------------------------------------------------

    //-----------//
    // ScoreView //
    //-----------//
    /**
     * Create the view dedicated to the display of the provided score.
     *
     * @param score the score to display
     */
    public ScoreView (Score score)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("new ScoreView on " + score);
        }

        // Cross referencing between score and its view
        this.score = score;
        score.setView(this);

        // Compute origin of all members
        computePositions();
    }

    //~ Methods -----------------------------------------------------------

    //---------//
    // getPane //
    //---------//
    public ScrollView getPane()
    {
        return pane;
    }

    //----------//
    // setFocus //
    //----------//
    /**
     * Move the score display, so that focus is on the specified page
     * point.
     *
     * @param pagPt the point in the score
     */
    public void setFocus (PagePoint pagPt)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("setFocus pagPt=" + pagPt);
        }

         if (pagPt != null) {
             tellPoint(pagPt);

             // Which system ?
             final System system = score.yLocateSystem(pagPt.y);

             if (system != null) {
                if (!localSelfUpdating) {
                   system.sheetToScore(pagPt, scrPt);
                   localSelfUpdating = true;
                   panel.setFocusPoint(scrPt);
                   localSelfUpdating = false;
                }
             }
         }
    }

    //----------//
    // getFocus //
    //----------//
    /**
     * Report the focus point within this score view, if any
     *
     * @return the current focus point, or null
     */
    public PagePoint getFocus ()
    {
//         if (mark.isActive()) {
//             scrPt = mark.getLocation();

//             // The enclosing system
//             System system = score.xLocateSystem(scrPt.x);

//             if (system != null) {
//                 system.scoreToSheet(scrPt, pagPt); // Point in the sheet

//                 return pagPt;
//             }
//         }

        return null;
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Report the score this view is dedicated to
     *
     * @return the related score
     */
    public Score getScore ()
    {
        return score;
    }

    //-------//
    // close //
    //-------//
    /**
     * Close the score display, by removing it from the enclosing tabbed
     * pane.
     */
    public void close ()
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Closing scoreView");
        }

        Sheet sheet = score.getSheet();
        if (sheet != null) {
            SheetAssembly assembly = sheet.getAssembly();
            if (assembly != null) {
                assembly.closeScoreView();
            }
        }
    }

    //------------------//
    // computePositions //
    //------------------//
    /**
     * Run computations on the tree of score, systems, etc, so that all
     * display data, such as origins and widths are available for display
     * use.
     */
    public void computePositions ()
    {
        if (logger.isDebugEnabled()) {
            score.dump();
            logger.debug("computePositions");
        }

        score.computeChildren();

        // Determine the X value of the whole set of systems
        int totalWidth = 0;
        for (Iterator it = score.getChildren().iterator(); it.hasNext();) {
            System system = (System) it.next();
            totalWidth += (system.getWidth() + INTER_SYSTEM);
        }

        // Total size of this panel (for proper scrolling)
        int w = zoom.scaled(SCORE_INIT_X + totalWidth + INTER_SYSTEM);
        int h = 2 * STAVE_MARGIN_HEIGHT +
            score.getMaxStaveNumber() * STAVE_AREA_HEIGHT;
        panel.setModelSize
            (new Dimension(SCORE_INIT_X + totalWidth + INTER_SYSTEM, 2*h));
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return a string based on the score XML file name
     */
    @Override
    public String toString ()
    {
        return "{ScoreView " + score.getRadix() + "}";
    }

    //-----------//
    // tellPoint //
    //-----------//
    private void tellPoint (PagePoint pagPt)
    {
        ////info.setText("PagePoint X:" + pagPt.x + " Y:" + pagPt.y);
    }

    //~ Classes -----------------------------------------------------------

    //-------//
    // Panel //
    //-------//
    private class MyPanel
        extends RubberZoomedPanel
    {
        //~ Constructors --------------------------------------------------

        //---------//
        // MyPanel //
        //---------//
        MyPanel (Zoom zoom,
                Rubber rubber)
        {
            super(zoom, rubber);
            rubber.setMouseMonitor(this);

            // Initialize drawing colors, border, opacity.
            setBackground(new Color(255, 255, 220)); // (Color.white);
            setForeground(Color.black);
        }

        //~ Methods -------------------------------------------------------

        //--------//
        // render //
        //--------//
        @Override
            public void render (Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g;

            // All nodes information
            score.paintChildren(g2, zoom, this);
        }

        //---------------//
        // setFocusPoint //
        //---------------//
        @Override
            public void setFocusPoint (Point scrPt)
        {
            // Display the point in score view
            super.setFocusPoint(scrPt);

            // The enclosing system
            final System system = score.xLocateSystem(scrPt.x);

            if (system != null) {
                system.scoreToSheet(scrPt, pagPt); // Point in the sheet
                tellPoint(pagPt); // Update score panel
                ///logger.info("scrPt=" + scrPt + " pagPt=" + pagPt);

                // Focus on that point in the related sheet
                Sheet sheet = score.getSheet();
                if (sheet != null) {
                    SheetAssembly assembly = sheet.getAssembly();
                    if (assembly != null) {
                        assembly.setSheetFocusPoint(pagPt);
                    }
                }
            }
        }

        //-------------------//
        // setFocusRectangle //
        //-------------------//
        public void setFocusRectangle (Rectangle rect)
        {
            super.setFocusRectangle(rect);

            if (rect != null) {
                setFocusPoint(new Point(rect.x + rect.width,
                                        rect.y + rect.height));
            }
        }
    }
}
