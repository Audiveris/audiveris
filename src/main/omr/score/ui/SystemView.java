//----------------------------------------------------------------------------//
//                                                                            //
//                            S y s t e m V i e w                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.ui;

import omr.log.Logger;

import omr.score.common.PagePoint;
import omr.score.common.ScorePoint;
import omr.score.common.ScoreRectangle;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.entity.ScoreSystem;
import static omr.score.ui.ScoreConstants.*;

/**
 * Class <code>SystemView</code> encapsulates information for a specific
 * display of a score system, and especially the display origin in the ScoreView
 * of the topLeft corner of the related system.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SystemView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SystemView.class);

    //~ Instance fields --------------------------------------------------------

    /** The related system */
    private final ScoreSystem system;

    /** Global system layout */
    private final ScoreOrientation orientation;

    /** Actual display origin in the score view */
    private final ScorePoint displayOrigin;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SystemView //
    //------------//
    /**
     * Create a view on a given system
     *
     * @param system the system to display
     * @param orientation chosen lsystem layout
     * @param displayOrigin the display origin of the system in the score view
     */
    public SystemView (ScoreSystem      system,
                       ScoreOrientation orientation,
                       ScorePoint       displayOrigin)
    {
        this.system = system;
        this.orientation = orientation;
        this.displayOrigin = displayOrigin;
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // getDisplayOrigin //
    //------------------//
    /**
     * Report the origin for this system, in the ScoreView display
     *
     * @return the display origin (TODO: a copy instead?)
     */
    public ScorePoint getDisplayOrigin ()
    {
        return displayOrigin;
    }

    //----------------//
    // getOrientation //
    //----------------//
    /**
     * Report hte system layout orientation
     * @return the system orientation
     */
    public ScoreOrientation getOrientation ()
    {
        return orientation;
    }

    //------------------//
    // getRightPosition //
    //------------------//
    /**
     * Return the actual display position of the right side.
     *
     * @return the display abscissa of the right system edge
     */
    public int getRightPosition ()
    {
        return (displayOrigin.x + system.getDimension().width) - 1;
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * Report the related system
     * @return the system
     */
    public ScoreSystem getSystem ()
    {
        return system;
    }

    //--------//
    // locate //
    //--------//
    /**
     * Return the position of given ScorePoint, relative to the system.
     *
     * @param scrPt the ScorePoint in the score display
     *
     * @return -1 for left, 0 for middle, +1 for right
     */
    public int locate (ScorePoint scrPt)
    {
        if (orientation == ScoreOrientation.HORIZONTAL) {
            if (scrPt.x < displayOrigin.x) {
                return -1;
            }

            if (scrPt.x > getRightPosition()) {
                return +1;
            }

            return 0;
        } else {
            if (scrPt.y < displayOrigin.y) {
                return -1;
            }

            if (scrPt.y > (displayOrigin.y + system.getDimension().height +
                          STAFF_HEIGHT)) {
                return +1;
            }

            return 0;
        }
    }

    //-------------//
    // toPagePoint //
    //-------------//
    /**
     * Compute the point in the sheet that corresponds to a given point in the
     * score display
     *
     * @param scrPt the point in the score display
     * @return the corresponding page point
     * @see #toScorePoint
     */
    public PagePoint toPagePoint (ScorePoint scrPt)
    {
        PagePoint topLeft = system.getTopLeft();

        return new PagePoint(
            topLeft.x + (scrPt.x - displayOrigin.x),
            topLeft.y + (scrPt.y - displayOrigin.y));
    }

    //--------------//
    // toScorePoint //
    //--------------//
    /**
     * Compute the score display point that corresponds to a given page point
     *
     * @param pagPt the point in the sheet
     * @return the score point
     * @see #toPagePoint
     */
    public ScorePoint toScorePoint (PagePoint pagPt)
    {
        PagePoint topLeft = system.getTopLeft();

        return new ScorePoint(
            displayOrigin.x + (pagPt.x - topLeft.x),
            displayOrigin.y + (pagPt.y - topLeft.y));
    }

    //--------------//
    // toScorePoint //
    //--------------//
    /**
     * Compute the score display point that corresponds to a given system point
     *
     * @param sysPt the point in the system
     * @return the score point
     */
    public ScorePoint toScorePoint (SystemPoint sysPt)
    {
        return toScorePoint(system.toPagePoint(sysPt));
    }

    //------------------//
    // toScoreRectangle //
    //------------------//
    /**
     * Compute the score display rectangle that corresponds to a given system
     * rectangle
     * @param sysRect the rectangle in the system
     * @return the score rectangle
     */
    public ScoreRectangle toScoreRectangle (SystemRectangle sysRect)
    {
        ScorePoint org = toScorePoint(new SystemPoint(sysRect.x, sysRect.y));

        return new ScoreRectangle(org.x, org.y, sysRect.width, sysRect.height);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{SystemView#")
          .append(system.getId());

        if (displayOrigin != null) {
            sb.append(" displayOrigin=[")
              .append(displayOrigin.x)
              .append(",")
              .append(displayOrigin.y)
              .append("]");
        }

        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // toSystemPoint //
    //---------------//
    /**
     * Compute the system-based point that corresponds to a given score point
     *
     * @param scrPt the point in the score display
     * @return the system relative point
     */
    public SystemPoint toSystemPoint (ScorePoint scrPt)
    {
        return system.toSystemPoint(toPagePoint(scrPt));
    }
}
