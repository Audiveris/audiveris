//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e V i e w                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import static omr.score.ScoreConstants.*;
import omr.score.visitor.ScorePainter;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionTag;

import omr.sheet.Sheet;

import omr.ui.SheetAssembly;
import omr.ui.util.Panel;
import omr.ui.view.Rubber;
import omr.ui.view.RubberZoomedPanel;
import omr.ui.view.ScrollView;
import omr.ui.view.Zoom;

import omr.util.Logger;

import java.awt.*;
import java.util.*;

import javax.swing.*;

/**
 * Class <code>ScoreView</code> encapsulates the horizontal display of all the
 * score systems which may encompass several pages & systems. It knows about the
 * display zoom ratio of the score.
 *
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>SCORE Location
 * </ul>
 * </dl>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreView.class);

    //~ Instance fields --------------------------------------------------------

    /** The info zone */
    private final JLabel info = new JLabel("Score");

    /** The displayed panel */
    private final MyPanel panel;

    /** The scroll pane + info zone */
    private final Panel compound = new Panel();

    /** The related score */
    private final Score score;

    /** The scroll pane */
    private final ScrollView pane;

    /** Display zoom */
    private final Zoom zoom = new Zoom(0.5d);

    /** Mouse rubber */
    private final Rubber rubber = new Rubber(zoom);

    //~ Constructors -----------------------------------------------------------

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

        panel = new MyPanel(zoom, rubber);
        pane = new ScrollView(panel);

        // Explicitly register the display to Score Selection
        Sheet sheet = score.getSheet();
        panel.setLocationSelection(sheet.getSelection(SelectionTag.SCORE));
        sheet.getSelection(SelectionTag.SCORE)
             .addObserver(panel);

        info.setHorizontalAlignment(SwingConstants.LEFT);
        compound.setLayout(new BorderLayout());
        compound.add(pane.getComponent(), BorderLayout.CENTER);
        compound.add(info, BorderLayout.SOUTH);

        // Cross referencing between score and its view
        this.score = score;
        score.setView(this);

        // Compute origin of all members
        computeModelSize();

        // Bridge companion between Score and Sheet locations both ways
        new ScoreSheetBridge(score);

        // Force selection update
        Selection pixelSelection = sheet.getSelection(SelectionTag.PIXEL);
        pixelSelection.reNotifyObservers(null);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the encapsulated UI component
     *
     * @return the UI component
     */
    public Component getComponent ()
    {
        return compound;
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

    //---------------//
    // getScrollPane //
    //---------------//
    /**
     * Report the underlying ScrollView
     *
     * @return the Scroll Pane
     */
    public ScrollView getScrollPane ()
    {
        return pane;
    }

    //-------//
    // close //
    //-------//
    /**
     * Close the score display, by removing it from the enclosing tabbed pane.
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
    // computeModelSize //
    //------------------//
    /**
     * Run computations on the tree of score, systems, etc, so that all display
     * data, such as origins and widths are available for display use.
     */
    public void computeModelSize ()
    {
        // Should be a visitor !!! TBD

        // Determine the X value of the whole set of systems
        int totalWidth = 0;

        for (Iterator it = score.getChildren()
                                .iterator(); it.hasNext();) {
            System system = (System) it.next();
            totalWidth += (system.getDimension().width + INTER_SYSTEM);
        }

        // Total size of this panel (for proper scrolling)
        int h = (2 * STAFF_MARGIN_HEIGHT) +
                (score.getMaxStaffNumber() * STAFF_AREA_HEIGHT);
        panel.setModelSize(
            new Dimension(SCORE_INIT_X + totalWidth + INTER_SYSTEM, 2 * h));
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
    private void tellPoint (ScorePoint  scrPt,
                            PagePoint   pagPt,
                            SystemPoint sysPt)
    {
        StringBuilder sb = new StringBuilder();

        //        if (scrPt != null) {
        //            sb.append("ScorePoint")
        //              .append(" X:")
        //              .append(scrPt.x)
        //              .append(" Y:")
        //              .append(scrPt.y)
        //              .append(" ");
        //        }
        if (pagPt != null) {
            sb.append("PagePoint")
              .append(" X:")
              .append(pagPt.x)
              .append(" Y:")
              .append(pagPt.y)
              .append(" ");
        }

        if (sysPt != null) {
            sb.append("SystemPoint")
              .append(" X:")
              .append(sysPt.x)
              .append(" Y:")
              .append(sysPt.y)
              .append(" ");
        }

        info.setText(sb.toString());
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------//
    // Panel //
    //-------//
    private class MyPanel
        extends RubberZoomedPanel
    {
        //---------//
        // MyPanel //
        //---------//
        MyPanel (Zoom   zoom,
                 Rubber rubber)
        {
            super(zoom, rubber);
            setName("ScoreView-MyPanel");
            rubber.setMouseMonitor(this);

            // Initialize drawing colors, border, opacity.
            setBackground(new Color(255, 255, 220));
            setForeground(Color.black);
        }

        //--------//
        // render //
        //--------//
        @Override
        public void render (Graphics g)
        {
            score.accept(new ScorePainter(g, zoom));
        }

        //--------//
        // update //
        //--------//
        /**
         * Call-back (part of Observer interface) triggered when location has
         * been modified
         *
         * @param selection the notified Selection
         * @param hint potential notification hint
         */
        public void update (Selection     selection,
                            SelectionHint hint)
        {
            if (logger.isFineEnabled()) {
                logger.fine("ScoreView: Score selection updated " + selection);
            }

            // Show location in display (default dehavior)
            super.update(selection, hint);

            // Display location coordinates
            if (selection == locationSelection) {
                Rectangle rect = (Rectangle) selection.getEntity();

                if (rect != null) {
                    ScorePoint  scrPt = new ScorePoint(rect.x, rect.y);
                    System      system = score.scoreLocateSystem(scrPt);
                    PagePoint   pagPt = system.toPagePoint(scrPt);
                    SystemPoint sysPt = system.toSystemPoint(pagPt);
                    tellPoint(scrPt, pagPt, sysPt);
                } else {
                    tellPoint(null, null, null);
                }
            }
        }
    }
}
