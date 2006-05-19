//-----------------------------------------------------------------------//
//                                                                       //
//                           S t a f f N o d e                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import omr.ui.icon.SymbolIcon;
import omr.ui.view.Zoom;
import omr.util.Logger;

import static omr.score.ScoreConstants.*;

import java.awt.*;
import omr.util.TreeNode;

/**
 * Class <code>StaffNode</code> is an abstract class that is subclassed for
 * any MusicNode whose location is known with respect to its containing
 * staff. So this class encapsulates a direct link to the enclosing staff.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class StaffNode
    extends MusicNode
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(MusicNode.class);

    //~ Instance variables ------------------------------------------------

    /**
     * Containing staff
     */
    protected Staff staff;

    //~ Constructors ------------------------------------------------------

    //-----------//
    // StaffNode //
    //-----------//
    /**
     * Create a StaffNode
     *
     * @param container the (direct) container of the node
     * @param staff     the enclosing staff, which is never the direct
     *                  container by the way
     */
    public StaffNode (MusicNode container,
                      Staff staff)
    {
        super(container);
        this.staff = staff;
    }

    //~ Methods -----------------------------------------------------------

    //-----------//
    // getOrigin //
    //-----------//
    /**
     * The display origin which is relevant for this node (this is the
     * staff origin)
     *
     * @return the display origin
     */
    public ScorePoint getOrigin ()
    {
        return staff.getOrigin();
    }

    //----------//
    // setStaff //
    //----------//
    /**
     * Set the containing staff
     *
     * @param staff the staff entity
     */
    public void setStaff (Staff staff)
    {
        this.staff = staff;
        setChildrenStaff(staff);
    }

    //------------------//
    // setChildrenStaff //
    //------------------//
    /**
     * Pattern to launch computation recursively on all children of this
     * node
     */
    public void setChildrenStaff (Staff staff)
    {
        for (TreeNode node : children) {
            if (node instanceof StaffNode) {
                StaffNode child = (StaffNode) node;
                child.setStaff(staff);
            }
        }
    }
    
    //------------//
    // stepToUnit //
    //------------//
    /**
     * Compute the ordinate Y (counted in units and measured from staff
     * origin) that corresponds to a given step line
     *
     * @param stepLine the pitch position (-4 for top line, +4 for bottom line)
     * @return the ordinate in pixels, counted from staff origin (upper
     * line), so top line is 0px and bottom line is 64px (with an inter
     * line of 16).
     */
    public int stepToUnit (int stepLine)
    {
        return (stepLine + 4) * INTER_LINE /2;
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol using its pitch position for ordinate in the
     * containing staff
     *
     * @param g graphical context
     * @param zoom display zoom
     * @param comp containing component
     * @param icon the symbol icon to paint
     * @param center staff-based coordinates of bounding center in units
     *               (only abscissa is actually used)
     * @param stepLine staff-based ordinate in step lines
     */
    public void paintSymbol (Graphics   g,
                             Zoom       zoom,
                             Component  comp,
                             SymbolIcon icon,
                             StaffPoint center,
                             int        stepLine)
    {
        if (icon == null) {
            logger.warning("No icon to paint");
        } else if (center == null) {
            logger.warning("Need bounding center for " + icon.getName());
        } else if (icon.getRefPoint() == null) {
            logger.warning("Need ref point for " + icon.getName());
        } else {
            ScorePoint origin = getOrigin();
//             logger.info("stepLine=" + stepLine + " stepToUnit=" + stepToUnit(stepLine)
//             + " refPoint=" + icon.getRefPoint());
            int dy = stepToUnit(stepLine);
            icon.paintIcon
                (comp,
                 g,
                 zoom.scaled(origin.x + center.x) - icon.getActualWidth()/2,
                 zoom.scaled(origin.y + dy)       - icon.getRefPoint().y);
        }
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol icon using the coordinates in units of its bounding
     * center within the containing staff
     *
     * @param g graphical context
     * @param zoom display zoom
     * @param comp containing component
     * @param icon the symbol icon to paint
     * @param center staff-based bounding center in units
     */
    public void paintSymbol (Graphics   g,
                             Zoom       zoom,
                             Component  comp,
                             SymbolIcon icon,
                             StaffPoint center)
    {
        if (icon == null) {
            logger.warning("No icon to paint");
        } else if (center == null) {
            logger.warning("Need area center for " + icon.getName());
        } else {
            ScorePoint origin = getOrigin();
            icon.paintIcon
                (comp,
                 g,
                 zoom.scaled(origin.x + center.x) - icon.getActualWidth()/2,
                 zoom.scaled(origin.y + center.y) - icon.getIconHeight()/2);
        }
    }
}
