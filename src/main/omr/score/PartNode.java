//----------------------------------------------------------------------------//
//                                                                            //
//                              P a r t N o d e                               //
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
import omr.score.visitor.ScoreVisitor;

import omr.sheet.PixelPoint;
import omr.sheet.Scale;

import omr.util.TreeNode;

import java.awt.*;
import java.util.Collection;

/**
 * Class <code>PartNode</code> is an abstract class that is subclassed for any
 * ScoreNode that is contained in a system part. So this class encapsulates a
 * direct link to the enclosing part.
 *
 * <p>A link to a related staff is provided as a potential tag only, since all
 * PartNode instances (Slur for example) are not related to a specific staff,
 * whereas a Wedge is.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class PartNode
    extends ScoreNode
{
    //~ Instance fields --------------------------------------------------------

    /** Containing part */
    private SystemPart part;

    /** Related staff, if relevant */
    private Staff staff;

    /** Location of the center of this entity WRT system top-left corner */
    private SystemPoint center;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // PartNode //
    //----------//
    /**
     * Create a PartNode
     *
     * @param container the (direct) container of the node
     */
    public PartNode (ScoreNode container)
    {
        super(container);

        // Set the part link
        for (TreeNode c = this; c != null; c = c.getParent()) {
            if (c instanceof SystemPart) {
                part = (SystemPart) c;

                break;
            }
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // setCenter //
    //-----------//
    /**
     * Remember the center of this part node
     *
     * @param center the system-based center of the part node
     */
    public void setCenter (SystemPoint center)
    {
        this.center = center;
    }

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Report the center of this entity, wrt to the part top-left corner.
     *
     * @return the center, in units, wrt system top-left
     */
    public SystemPoint getCenter ()
    {
        if (center == null) {
            computeCenter();
        }

        return center;
    }

    //------------------//
    // getContextString //
    //------------------//
    @Override
    public String getContextString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getSystem().getContextString());

        sb.append("P")
          .append(part.getId());

        return sb.toString();
    }

    //------------------//
    // getDisplayOrigin //
    //------------------//
    /**
     * Report the origin for the containing system, in the horizontal score
     * display, since coordinates use SystemPoint.
     *
     * @return the (system) display origin
     */
    public ScorePoint getDisplayOrigin ()
    {
        return getSystem()
                   .getDisplayOrigin();
    }

    //---------//
    // getPart //
    //---------//
    /**
     * Report the containing part
     *
     * @return the containing part entity
     */
    public SystemPart getPart ()
    {
        return part;
    }

    //----------//
    // setStaff //
    //----------//
    /**
     * Assign the related staff
     *
     * @param staff the related staff
     */
    public void setStaff (Staff staff)
    {
        this.staff = staff;
    }

    //----------//
    // getStaff //
    //----------//
    /**
     * Report the related staff if any
     *
     * @return the related staff, or null
     */
    public Staff getStaff ()
    {
        return staff;
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * Report the containing system
     *
     * @return the containing system
     */
    public System getSystem ()
    {
        return getPart()
                   .getSystem();
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
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
    public SystemPoint computeGlyphCenter (Glyph glyph)
    {
        return computeRectangleCenter(glyph.getContourBox());
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
    public SystemPoint computeGlyphsCenter (Collection<?extends Glyph> glyphs)
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

        return computeRectangleCenter(rect);
    }

    //---------------//
    // computeCenter //
    //---------------//
    /**
     * Compute the center of this entity, wrt to the part top-left corner.
     * Unless overridden, this method raises an exception.
     */
    protected void computeCenter ()
    {
        throw new RuntimeException(
            "computeCenter() not implemented in " + getClass().getName());
    }

    //------------------------//
    // computeRectangleCenter //
    //------------------------//
    private SystemPoint computeRectangleCenter (Rectangle rect)
    {
        PixelPoint pixPt = new PixelPoint(
            rect.x + (rect.width / 2),
            rect.y + (rect.height / 2));

        return getSystem()
                   .toSystemPoint(pixPt);
    }
}
