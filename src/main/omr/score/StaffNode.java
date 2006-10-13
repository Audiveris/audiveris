//----------------------------------------------------------------------------//
//                                                                            //
//                             S t a f f N o d e                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Glyph;
import static omr.score.ScoreConstants.*;
import omr.score.visitor.Visitor;

import omr.sheet.Scale;

import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.*;
import java.util.Collection;

/**
 * Class <code>StaffNode</code> is an abstract class that is subclassed for any
 * MusicNode whose location is known with respect to its containing staff. So
 * this class encapsulates a direct link to the enclosing staff.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class StaffNode
    extends MusicNode
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(MusicNode.class);

    //~ Instance fields --------------------------------------------------------

    /** Containing staff */
    protected Staff staff;

    /** Location of the center of this entity WRT staff top-left corner */
    protected StaffPoint center;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // StaffNode //
    //-----------//
    /**
     * Create a StaffNode
     *
     * @param container the (direct) container of the node
     * @param staff the enclosing staff, which is never the direct container by
     *                  the way
     */
    public StaffNode (MusicNode container,
                      Staff     staff)
    {
        super(container);
        this.staff = staff;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Report the center of this entity, wrt to the staff top-left corner.
     *
     * @return the center, in units, wrt to staff top-left
     */
    public StaffPoint getCenter ()
    {
        if (center == null) {
            computeCenter();
        }

        return center;
    }

    //------------------//
    // setChildrenStaff //
    //------------------//
    /**
     * Pattern to launch computation recursively on all children of this node
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

    //-----------//
    // getOrigin //
    //-----------//
    /**
     * The display origin which is relevant for this node (this is the staff
     * origin)
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

    //----------//
    // getStaff //
    //----------//
    /**
     * Report the containing staff
     *
     * @return the containint staff entity
     */
    public Staff getStaff ()
    {
        return staff;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (Visitor visitor)
    {
        return visitor.visit(this);
    }

    //--------------------//
    // computeGlyphCenter //
    //--------------------//
    /**
     * Compute the bounding center of a glyph
     *
     * @param glyph the glyph
     *
     * @return the glyph center
     */
    public StaffPoint computeGlyphCenter (Glyph glyph)
    {
        // We compute the bounding center of all glyphs
        Rectangle  rect = new Rectangle(glyph.getContourBox());
        Scale      scale = staff.getInfo()
                                .getScale();
        StaffPoint p = new StaffPoint(
            scale.pixelsToUnits(rect.x + (rect.width / 2)) -
            staff.getTopLeft().x,
            scale.pixelsToUnits(rect.y + (rect.height / 2)) -
            staff.getTopLeft().y);

        return p;
    }

    //---------------------//
    // computeGlyphsCenter //
    //---------------------//
    /**
     * Compute the bounding center of a collection of glyphs
     *
     * @param glyphs the collection of glyph components
     *
     * @return the area center
     */
    public StaffPoint computeGlyphsCenter (Collection<?extends Glyph> glyphs)
    {
        // We compute the bounding center of all glyphs
        Rectangle rect = null;

        for (Glyph glyph : glyphs) {
            if (rect == null) {
                rect = new Rectangle(glyph.getContourBox());
            } else {
                rect = rect.union(glyph.getContourBox());
            }
        }

        Scale      scale = staff.getInfo()
                                .getScale();
        StaffPoint p = new StaffPoint(
            scale.pixelsToUnits(rect.x + (rect.width / 2)) -
            staff.getTopLeft().x,
            scale.pixelsToUnits(rect.y + (rect.height / 2)) -
            staff.getTopLeft().y);

        return p;
    }

    //----------//
    // fixStaff //
    //----------//
    /**
     * Fix the staff reference, by walking up the container hierarchy
     */
    public void fixStaff ()
    {
        for (TreeNode c = getContainer(); c != null; c = c.getContainer()) {
            if (c instanceof Staff) {
                setStaff((Staff) c);

                break;
            }
        }
    }

    //---------------//
    // computeCenter //
    //---------------//
    /**
     * Compute the center of this entity, wrt to the staff top-left corner.
     * Unless overridden, this method raises an exception.
     */
    protected void computeCenter ()
    {
        throw new RuntimeException(
            "computeCenter() not implemented in " + getClass().getName());
    }
}
