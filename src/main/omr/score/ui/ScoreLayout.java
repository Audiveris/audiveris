//----------------------------------------------------------------------------//
//                                                                            //
//                           S c o r e L a y o u t                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.log.Logger;

import omr.score.Score;
import omr.score.common.PixelPoint;
import omr.score.common.ScorePoint;
import omr.score.common.ScoreRectangle;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.entity.ScorePart;
import omr.score.entity.ScoreSystem;
import static omr.score.ui.ScoreConstants.*;

import omr.util.TreeNode;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>ScoreLayout</code> handles the layout of all the systems of
 * a score (which may encompass several pages & systems) for a specific
 * orientation (horizontal or vertical).
 * <p>For a given score, there is one instance per orientation.
 *
 * @author Hervé Bitteur
 */
public class ScoreLayout
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreLayout.class);

    //~ Instance fields --------------------------------------------------------

    /** The related score */
    private final Score score;

    /**
     * The related systems layout, horizontal (side by side) or vertical
     * (one above the other).
     */
    private final ScoreOrientation orientation;

    /** Sequence of system views, ordered by system id */
    private volatile List<SystemView> systemViews;

    /** Global layout dimension */
    private Dimension scoreDimension;

    /** The most recent system pointed at */
    private WeakReference<ScoreSystem> recentSystem = null;

    /** To trigger recomputation */
    private boolean dirty = true;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // ScoreLayout //
    //-------------//
    /**
     * Create the layout of the provided score.
     *
     * @param score the score to organize
     */
    public ScoreLayout (Score            score,
                        ScoreOrientation orientation)
    {
        if (logger.isFineEnabled()) {
            logger.fine("new ScoreLayout on " + score);
        }

        this.score = score;
        this.orientation = orientation;

        dirty = true;
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // getScoreDimension //
    //-------------------//
    public Dimension getScoreDimension ()
    {
        if (dirty) {
            computeLayout();
        }

        return scoreDimension;
    }

    //---------------//
    // getSystemView //
    //---------------//
    /**
     * Report the specific system view for the system in this specific score
     * view
     * @param system the provided system
     * @return the specific system view
     */
    public SystemView getSystemView (ScoreSystem system)
    {
        return getSystemView(system.getId());
    }

    //---------------//
    // getSystemView //
    //---------------//
    public SystemView getSystemView (int id)
    {
        if (systemViews == null) {
            return null;
        } else {
            return systemViews.get(id - 1);
        }
    }

    //---------------//
    // computeLayout //
    //---------------//
    /**
     * Run computations on the collection of systems so that they are displayed
     * in a nice manner, using  horizontal or vertical layout
     */
    public void computeLayout ()
    {
        // Get a fresh set of system views
        createSystemViews();

        // Browse all system views and compute the union of all rectangles
        ScoreRectangle scoreContour = null;

        for (TreeNode node : score.getSystems()) {
            ScoreSystem    system = (ScoreSystem) node;
            SystemView     systemView = getSystemView(system);
            ScoreRectangle absSystemContour = systemView.toScoreRectangle(
                system.getDisplayContour());

            if (scoreContour == null) {
                scoreContour = absSystemContour;
            } else {
                Rectangle r = scoreContour.union(absSystemContour);
                scoreContour = new ScoreRectangle(r.x, r.y, r.width, r.height);
            }
        }

        scoreDimension = new Dimension(scoreContour.width, scoreContour.height);

        dirty = false;
    }

    //-------//
    // reset //
    //-------//
    /**
     * Discard the views on systems, to disable concurrent system painting
     * while the systems are being redefined
     */
    public void reset ()
    {
        dirty = true;
        systemViews = null;
        scoreDimension = null;
        recentSystem = null;
    }

    //-------------------//
    // scoreLocateSystem //
    //-------------------//
    /**
     * Retrieve the system 'scrPt' is pointing to, according to the current
     * layout.
     *
     * @param scrPt the point in the SCORE horizontal display
     * @return the nearest system
     */
    public ScoreSystem scoreLocateSystem (ScorePoint scrPt)
    {
        if (dirty) {
            computeLayout();
        }

        ScoreSystem recentSystem = getRecentSystem();

        if (recentSystem != null) {
            // Check first with most recent system (loosely)
            SystemView systemView = getSystemView(recentSystem);

            switch (systemView.locate(scrPt)) {
            case -1 :

                // Check w/ previous system
                ScoreSystem prevSystem = (ScoreSystem) recentSystem.getPreviousSibling();

                if (prevSystem == null) {
                    // Very first system
                    return recentSystem;
                } else {
                    if (getSystemView(prevSystem)
                            .locate(scrPt) > 0) {
                        return recentSystem;
                    }
                }

                break;

            case 0 :
                return recentSystem;

            case +1 :

                // Check w/ next system
                ScoreSystem nextSystem = (ScoreSystem) recentSystem.getNextSibling();

                if (nextSystem == null) {
                    // Very last system
                    return recentSystem;
                } else {
                    if (getSystemView(nextSystem)
                            .locate(scrPt) < 0) {
                        return recentSystem;
                    }
                }

                break;
            }
        }

        // Recent system is not OK, Browse though all the score systems
        ScoreSystem system = null;

        for (TreeNode node : score.getSystems()) {
            system = (ScoreSystem) node;

            SystemView systemView = getSystemView(system);

            // How do we locate the point wrt the system  ?
            switch (systemView.locate(scrPt)) {
            case -1 : // Point is before system (but after previous), give up.
            case 0 : // Point is within system.
                return setRecentSystem(system);

            case +1 : // Point is after the system, go on.
                break;
            }
        }

        // Return the last system in the score
        return setRecentSystem(system);
    }

    //--------------//
    // toPixelPoint //
    //--------------//
    /**
     * Report the PixelPoint that corresponds to a given ScorePoint,
     * according to the current layout of the score view
     * @param scorePoint the given score point
     * @return the corresponding pixel point
     */
    public PixelPoint toPixelPoint (ScorePoint scorePoint)
    {
        if (scorePoint == null) {
            return null;
        }

        if (dirty) {
            computeLayout();
        }

        ScoreSystem system = scoreLocateSystem(scorePoint);
        SystemView  systemView = getSystemView(system);
        SystemPoint sysPt = systemView.toSystemPoint(scorePoint);

        return system.toPixelPoint(sysPt);
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
        return "{ScoreLayout " + orientation + " " + score.getRadix() + "}";
    }

    //-----------------//
    // setRecentSystem //
    //-----------------//
    private ScoreSystem setRecentSystem (ScoreSystem system)
    {
        recentSystem = new WeakReference<ScoreSystem>(system);

        return system;
    }

    //-----------------//
    // getRecentSystem //
    //-----------------//
    private ScoreSystem getRecentSystem ()
    {
        return (recentSystem == null) ? null : recentSystem.get();
    }

    //-------------------//
    // createSystemViews //
    //-------------------//
    /**
     * Set the display parameters of each system
     */
    private void createSystemViews ()
    {
        final int        highestTop = score.getHighestSystemTop();
        List<SystemView> views = new ArrayList<SystemView>();
        SystemView       prevSystemView = null;

        for (TreeNode node : score.getSystems()) {
            ScoreSystem     system = (ScoreSystem) node;
            SystemRectangle contour = system.getDisplayContour();
            ScorePoint      origin = new ScorePoint();

            if (orientation == ScoreOrientation.HORIZONTAL) {
                if (prevSystemView == null) {
                    // Very first system in the score
                    origin.x = -contour.x;
                } else {
                    // Not the first system
                    origin.x = (prevSystemView.getDisplayOrigin().x +
                               prevSystemView.getSystem().getDimension().width) +
                               INTER_SYSTEM_WIDTH;
                }

                ScorePart scorePart = system.getFirstPart()
                                            .getScorePart();
                origin.y = -highestTop + system.getDummyOffset() +
                           ((scorePart != null)
                            ? scorePart.getDisplayOrdinate() : 0);
            } else {
                if (prevSystemView == null) {
                    // Very first system in the score
                    origin.y = -contour.y;
                } else {
                    // Not the first system
                    origin.y = (prevSystemView.getDisplayOrigin().y +
                               prevSystemView.getSystem()
                                             .getDimension().height +
                               STAFF_HEIGHT) + INTER_SYSTEM_HEIGHT;
                }

                origin.x = -contour.x;
            }

            // Create an immutable view for this system
            recentSystem = null;

            SystemView systemView = new SystemView(system, orientation, origin);
            views.add(systemView);
            prevSystemView = systemView;

            if (logger.isFineEnabled()) {
                logger.fine(system + " origin:" + origin);
            }
        }

        // Write the new collection of SystemView instances
        // This will allow the painting of systems from now on
        systemViews = views;
    }
}
