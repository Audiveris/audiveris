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
import omr.ui.Mark;
import omr.ui.SheetPane;
import omr.util.Logger;
import omr.ui.Zoom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Iterator;

/**
 * Class <code>ScoreView</code> encapsulates the horizontal display of all
 * the score systems which may encompass several pages & systems. It knows
 * about the display zoom ratio of the score.
 *
 * <B>NOTA</B> : For the time being, this score display has been removed
 * from the Audiveris application. The file is still here, because of
 * dependencies on logical constants. This should be cleaned up someday of
 * course.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreView
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(ScoreView.class);

    // Symbolic constants

    /**
     * Height in pixels of the score display : {@value}
     */
    public static final int SCORE_AREA_HEIGHT = 500;

    /**
     * Horizontal offset in pixels of the score origin : {@value}
     */
    public static final int SCORE_INIT_X = 200;

    /**
     * Vertical offset in pixels of the score origin : {@value}
     */
    public static final int SCORE_INIT_Y = 200;

    /**
     * Horizontal gutter in pixels between two systems : {@value}
     */
    public static final int INTER_SYSTEM = 100;

    /**
     * Vertical distance in pixels between two lines of a staff : {@value}
     */
    public static final int INTER_LINE = 16;

    /**
     * Horizontal gutter in pixels between two pages : {@value}
     */
    public static final int INTER_PAGE = 200;

    /**
     * Number of lines in a staff : {@value}
     */
    public static final int LINE_NB = 5;

    /**
     * Height in pixels of one staff : {@value}
     */
    public static final int STAFF_HEIGHT = (LINE_NB - 1) * INTER_LINE;

    /**
     * Used to code fractions with an integer value, with a resolution of
     * 1/{@value}
     */
    public static final int BASE = 1024;

    // Display zoom
    private static final Zoom zoom = new Zoom(0.5d);

    // Default view position
    private static final Point defaultPosition
            = new Point(0,
                        zoom.scaled(SCORE_INIT_Y / 2));

    //~ Instance variables ------------------------------------------------

    // The concrete component
    private JPanel component;

    // The displayed panel
    private final Panel panel = new Panel();

    // The related score
    private final Score score;

    // To mark a location
    private final Mark mark = new Mark(Color.orange);

    // Scrolled pane
    private final JScrollPane scrollPane;

    // Related Information display
    private final JLabel info = new JLabel();

    // Meant for Point computation, w/o continuous allocations
    //
    // Point in score display in units  (not zoomed)
    private Point scrPt = new Point();

    // Point in virtual sheet display in units  (not zoomed, not skewed)
    private PagePoint pagPt = new PagePoint();

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
        component = new JPanel();

        // Cross referencing between score and its view
        this.score = score;
        score.setView(this);

        // Default mark location
        //////////////mark.setLocation (new Point (SCORE_INIT_X, SCORE_INIT_Y));
        // Global organization
        component.setLayout(new BorderLayout());

        // Scroll pane to contain the component
        scrollPane = new JScrollPane(panel);
        component.add(scrollPane, BorderLayout.CENTER);

        // Information display
        component.add(info, BorderLayout.SOUTH);

        // The panel in the scroll pane
        ////setViewportView (panel);
        // Compute origin of all memners
        computePositions();

        // Pre-position the scroll
        scrollPane.getViewport().setViewPosition(defaultPosition);
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

    //----------//
    // setFocus //
    //----------//

    /**
     * Move the score display, so that focus is on the specified page point.
     *
     * @param pagPt the point in the score
     */
    public void setFocus (PagePoint pagPt)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("setFocus pagPt=" + pagPt);
        }

        if (pagPt != null) {
            tellUnits(pagPt);

            // Which system ?
            final System system = score.yLocateSystem(pagPt.y);

            if (system != null) {
                system.sheetToScore(pagPt, scrPt);
                mark.setLocation(scrPt);
                panel.repaint();
                panel.scrollRectToVisible(mark.getRectangle(zoom));
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
        if (mark.isActive()) {
            scrPt = mark.getLocation();

            // The enclosing system
            System system = score.xLocateSystem(scrPt.x);

            if (system != null) {
                system.scoreToSheet(scrPt, pagPt); // Point in the sheet

                return pagPt;
            }
        }

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

    //---------//
    // getZoom //
    //---------//

    /**
     * Report the zoom used to display scores
     *
     * @return the zoom entity (actually, its ratio is the constant 1/2)
     */
    public static Zoom getZoom ()
    {
        return zoom;
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
        Main.getJui().scorePane.close(component);
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
        panel.setPreferredSize(new Dimension(zoom.scaled((SCORE_INIT_X
                                                          + totalWidth
                                                          + INTER_SYSTEM)),
                                             SCORE_AREA_HEIGHT));
        scrollPane.setViewportView(panel);
    }

    //------------------//
    // showRelatedSheet //
    //------------------//

    /**
     * Make an attempt to trigger the display (on the other side) of the
     * sheet related to this score.
     */
    public void showRelatedSheet ()
    {
//         if (logger.isDebugEnabled()) {
//             logger.debug("showRelatedSheet for " + score);
//         }

//         SheetPane sheetPane = Main.getJui().sheetPane;
//         Sheet sheet = score.getSheet();

//         if (sheet != null) {
//             sheetPane.showSheetView(sheet.getView());
//         } else {
//             sheetPane.showSheetView(null);
//         }
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
    // tellUnits //
    //-----------//
    private void tellUnits (PagePoint pagPt)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("tellUnits pagPt=" + pagPt);
        }

        info.setText("ScoreView. Units X:" + pagPt.x + " Y:" + pagPt.y);
    }

    //---------------//
    // useMouseFocus //
    //---------------//
    private void useMouseFocus (Point pt)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("useMouseFocus pt=" + pt);
        }

        // The designated mouse point, transformed into score units
        scrPt.x = zoom.unscaled(pt.x);
        scrPt.y = zoom.unscaled(pt.y);

        // Update local mark, where the mouse is in the score display
        mark.setLocation(scrPt);
        panel.repaint();

        // The enclosing system
        final System system = score.xLocateSystem(scrPt.x);

        if (system != null) {
            system.scoreToSheet(scrPt, pagPt); // Point in the sheet
            tellUnits(pagPt); // Update score panel

//             // Focus on that point in the original sheet
//             int viewIndex = Main.getJui().sheetPane.setScoreSheetView(score,
//                                                                       pagPt);

//             // Make it visible
//             if (viewIndex != SheetPane.DIFFERED_INDEX) {
//                 Main.getJui().sheetPane.showSheetView(viewIndex);
//             }
        }
    }

    //~ Classes -----------------------------------------------------------

    //-------//
    // Panel //
    //-------//
    private class Panel
        extends JPanel
    {
        //~ Constructors --------------------------------------------------

        //-------//
        // Panel //
        //-------//
        Panel ()
        {
            // Initialize drawing colors, border, opacity.
            setBackground(new Color(255, 255, 220)); // (Color.white);
            setForeground(Color.black);

            // Track pressed mouse events
            addMouseListener(new MouseAdapter()
            {
                public void mousePressed (MouseEvent e)
                {
                    useMouseFocus(e.getPoint());

                    // TBD HB add case of object selection (see DisplayPane)
                }
            });

            // Track dragged mouse events
            addMouseMotionListener(new MouseMotionAdapter()
            {
                public void mouseDragged (MouseEvent e)
                {
                    useMouseFocus(e.getPoint());
                }
            });
        }

        //~ Methods -------------------------------------------------------

        //----------------//
        // paintComponent //
        //----------------//
        @Override
        public void paintComponent (Graphics g)
        {
            if (logger.isDebugEnabled()) {
                logger.debug("paintComponent for " + score);
            }

            Graphics2D g2 = (Graphics2D) g;

            // Paint background
            super.paintComponent(g2);

            // All nodes information
            score.paintChildren(g2);

            // The location mark if any
            if (mark.isActive()) {
                mark.render(g, zoom);
            }
        }
    }
}
