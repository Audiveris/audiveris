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
import omr.ui.SheetAssembly;
import omr.ui.view.Rubber;
import omr.ui.view.RubberZoomedPanel;
import omr.ui.view.ScrollView;
import omr.ui.view.Zoom;
import omr.ui.util.Panel;
import omr.util.Logger;

import static omr.score.ScoreConstants.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Iterator;
import javax.swing.*;

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

    //~ Instance variables ------------------------------------------------

    // The related score
    private final Score score;

    // Display zoom
    private final Zoom zoom = new Zoom(0.5d);

    // Mouse rubber
    private final Rubber rubber = new Rubber(zoom);

    // The displayed panel
    private final MyPanel panel = new MyPanel(zoom, rubber);

    // The scroll pane + info zone
    private final Panel compound = new Panel();

    // The scroll pane
    private final ScrollView pane = new ScrollView(panel);

    // The info zone
    private final JLabel info = new JLabel("Score");

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
        if (logger.isFineEnabled()) {
            logger.fine("new ScoreView on " + score);
        }

        info.setHorizontalAlignment(SwingConstants.LEFT);
        compound.setLayout(new BorderLayout());
        compound.add(pane.getComponent(), BorderLayout.CENTER);
        compound.add(info, BorderLayout.SOUTH);

        // Cross referencing between score and its view
        this.score = score;
        score.setView(this);

        // Compute origin of all members
        computePositions();
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    public Component getComponent()
    {
        return compound;
    }

    //---------------//
    // getScrollPane //
    //---------------//
    public ScrollView getScrollPane()
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
        if (logger.isFineEnabled()) {
            logger.fine("setFocus pagPt=" + pagPt);
        }

         if (pagPt != null) {
             ScorePoint scrPt = null;
             // Which system ?
             final System system = score.pageLocateSystem(pagPt);
             if (system != null && !localSelfUpdating) {
                 scrPt = system.sheetToScore(pagPt, null);
                 localSelfUpdating = true;
                 panel.setFocusPoint(scrPt);
                 localSelfUpdating = false;
             }
             tellPoint(scrPt, pagPt);
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
//             System system = score.scoreLocateSystem(scrPt.x);

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
        if (logger.isFineEnabled()) {
            logger.fine("Closing scoreView");
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
        if (logger.isFineEnabled()) {
            score.dump();
            logger.fine("computePositions");
        }

        score.computeChildren();

        // Determine the X value of the whole set of systems
        int totalWidth = 0;
        for (Iterator it = score.getChildren().iterator(); it.hasNext();) {
            System system = (System) it.next();
            totalWidth += (system.getDimension().width + INTER_SYSTEM);
        }

        // Total size of this panel (for proper scrolling)
        int h = 2 * STAFF_MARGIN_HEIGHT +
            score.getMaxStaffNumber() * STAFF_AREA_HEIGHT;
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
    private void tellPoint(ScorePoint scrPt)
    {
        tellPoint(scrPt, null);
    }

    //-----------//
    // tellPoint //
    //-----------//
    private void tellPoint(PagePoint  pagPt)
    {
        tellPoint(null, pagPt);
    }

    //-----------//
    // tellPoint //
    //-----------//
    private void tellPoint(ScorePoint scrPt,
                           PagePoint  pagPt)
    {
        StringBuilder sb = new StringBuilder();
        if (scrPt != null) {
            sb.append("ScorePoint")
                .append(" X:").append(scrPt.x)
                .append(" Y:").append(scrPt.y)
                .append(" ");
        }
        if (pagPt != null) {
            sb.append("PagePoint")
                .append(" X:").append(pagPt.x)
                .append(" Y:").append(pagPt.y)
                .append(" ");
        }
        info.setText(sb.toString());
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
            setBackground(new Color(255, 255, 220));
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
            public void setFocusPoint (Point pt)
        {
            // We forge a ScorePoint from the display point
            ScorePoint scrPt = new ScorePoint(pt.x, pt.y);
            
            // Display the point in score view
            super.setFocusPoint(scrPt);

            // The enclosing system
            System system = score.scoreLocateSystem(scrPt);

            if (system != null) {
                PagePoint pagPt = system.scoreToSheet(scrPt, null);

                // Update score panel
                tellPoint(scrPt, pagPt); 

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
        @Override
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
