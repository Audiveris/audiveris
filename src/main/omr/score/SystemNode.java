//----------------------------------------------------------------------------//
//                                                                            //
//                            S y s t e m N o d e                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Glyph;
import static omr.score.ScoreConstants.*;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.PixelPoint;

import omr.util.TreeNode;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class <code>SystemNode</code> is an abstract class that is subclassed
 * for any ScoreNode that is contained in a system, beginning by the system
 * itself. So this class encapsulates a direct link to the enclosing
 * system.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class SystemNode
    extends ScoreNode
{
    //~ Instance fields --------------------------------------------------------

    /** Containing system */
    private System system;

    /** Location of the center of this entity WRT system top-left corner */
    private SystemPoint center;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SystemNode //
    //------------//
    /**
     * Create a SystemNode
     *
     * @param container the (direct) container of the node
     */
    public SystemNode (ScoreNode container)
    {
        super(container);

        // Set the system link
        for (TreeNode c = this; c != null; c = c.getParent()) {
            if (c instanceof System) {
                system = (System) c;

                break;
            }
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addError //
    //----------//
    public void addError (String text)
    {
        addError(null, text);
    }

    //----------//
    // addError //
    //----------//
    public void addError (Glyph  glyph,
                          String text)
    {
        getScore()
            .getSheet()
            .addError(this, glyph, text);
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

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Report the center of this entity, wrt to the system top-left corner.
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
        return system;
    }

    //-----------//
    // setCenter //
    //-----------//
    /**
     * Remember the center of this system node
     *
     * @param center the system-based center of the system node
     */
    public void setCenter (SystemPoint center)
    {
        this.center = center;
    }

    //---------------//
    // computeCenter //
    //---------------//
    /**
     * Compute the center of this entity, wrt to the system top-left corner.
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
