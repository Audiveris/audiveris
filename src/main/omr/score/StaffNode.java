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
}
