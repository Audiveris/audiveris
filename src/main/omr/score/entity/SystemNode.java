//----------------------------------------------------------------------------//
//                                                                            //
//                            S y s t e m N o d e                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Glyph;

import omr.score.common.PixelRectangle;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.visitor.ScoreVisitor;

import omr.util.Navigable;
import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>SystemNode</code> is an abstract class that is subclassed
 * for any ScoreNode that is contained in a system, beginning by the system
 * itself. So this class encapsulates a direct link to the enclosing
 * system.
 *
 * @author Hervé Bitteur
 */
public abstract class SystemNode
    extends ScoreNode
{
    //~ Instance fields --------------------------------------------------------

    /** Containing system */
    @Navigable(false)
    private final ScoreSystem system;

    /** Bounding box of this entity, WRT system top-left corner */
    private SystemRectangle box;

    /** Location of the center of this entity, WRT system top-left corner */
    protected SystemPoint center;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SystemNode //
    //------------//
    /**
     * Create a SystemNode
     * @param container the (direct) container of the node
     */
    public SystemNode (ScoreNode container)
    {
        super(container);

        // Set the system link
        for (TreeNode c = this; c != null; c = c.getParent()) {
            if (c instanceof ScoreSystem) {
                system = (ScoreSystem) c;

                return;
            }
        }

        ///throw new RuntimeException("Creating a SystemNode with no ScoreSystem");
        system = null;
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // getBox //
    //--------//
    /**
     * Report a copy of the bounding box of the entity, WRT system top-left
     * corner
     * @return the box
     */
    public SystemRectangle getBox ()
    {
        if (box == null) {
            computeBox();
        }

        if (box != null) {
            return (SystemRectangle) box.clone();
        } else {
            return null;
        }
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * Report a copy of the center of this entity, WRT system top-left corner.
     * @return the center, in units, wrt system top-left
     */
    public SystemPoint getCenter ()
    {
        if (center == null) {
            computeCenter();
        }

        if (center != null) {
            return (SystemPoint) center.clone();
        } else {
            return null;
        }
    }

    //------------------//
    // getContextString //
    //------------------//
    @Override
    public String getContextString ()
    {
        StringBuilder sb = new StringBuilder(super.getContextString());
        sb.append("S")
          .append(system.getId());

        return sb.toString();
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * Report the containing system
     * @return the containing system
     */
    public ScoreSystem getSystem ()
    {
        return system;
    }

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
    /**
     * Register a system-based error in the ErrorsWindow
     * @param text the error message
     */
    public void addError (String text)
    {
        addError(null, text);
    }

    //----------//
    // addError //
    //----------//
    /**
     * Register a system-based error in the ErrorsWindow, with the related glyph
     * @param glyph the related glyph
     * @param text the error message
     */
    public void addError (Glyph  glyph,
                          String text)
    {
        if ((getScore() != null) && (getScore()
                                         .getSheet() != null)) {
            getScore()
                .getSheet()
                .addError(this, glyph, text);
        }
    }

    //--------------------//
    // computeGlyphCenter //
    //--------------------//
    /**
     * Compute the bounding center of a glyph
     * @param glyph the glyph
     * @return the glyph center
     */
    public SystemPoint computeGlyphCenter (Glyph glyph)
    {
        return computeGlyphsCenter(Collections.singleton(glyph));
    }

    //--------//
    // setBox //
    //--------//
    /**
     * Assign the bounding box
     * @param box the bounding box
     */
    protected void setBox (SystemRectangle box)
    {
        this.box = box;
    }

    //-----------//
    // setCenter //
    //-----------//
    /**
     * Remember the center of this system node
     * @param center the system-based center of the system node
     */
    protected void setCenter (SystemPoint center)
    {
        this.center = center;
    }

    //------------//
    // computeBox //
    //------------//
    /**
     * Compute the bounding box of this entity, wrt to the system top-left corner.
     * Unless overridden, this method raises an exception.
     */
    protected void computeBox ()
    {
        throw new RuntimeException(
            "computeBox() not implemented in " + getClass().getName());
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
        SystemRectangle theBox = getBox();

        if (theBox != null) {
            setCenter(theBox.getCenter());
        } else {
            setCenter(null);
        }
    }

    //------------------//
    // computeGlyphsBox //
    //------------------//
    /**
     * Compute the bounding box of a collection of glyphs
     * @param glyphs the collection of glyph components
     * @return the bounding box
     */
    protected SystemRectangle computeGlyphsBox (Collection<?extends Glyph> glyphs)
    {
        if ((glyphs == null) || (getSystem() == null)) {
            return null;
        }

        PixelRectangle pixRect = null;

        for (Glyph glyph : glyphs) {
            if (pixRect == null) {
                pixRect = glyph.getContourBox();
            } else {
                pixRect = pixRect.union(glyph.getContourBox());
            }
        }

        return (pixRect == null) ? null : getSystem()
                                              .toSystemRectangle(pixRect);
    }

    //---------------------//
    // computeGlyphsCenter //
    //---------------------//
    /**
     * Compute the bounding center of a collection of glyphs
     * @param glyphs the collection of glyph components
     * @return the area center
     */
    protected SystemPoint computeGlyphsCenter (Collection<?extends Glyph> glyphs)
    {
        SystemRectangle glyphsBox = computeGlyphsBox(glyphs);

        return new SystemPoint(
            glyphsBox.x + (glyphsBox.width / 2),
            glyphsBox.y + (glyphsBox.height / 2));
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data
     */
    protected void reset ()
    {
        box = null;
        center = null;
    }
}
