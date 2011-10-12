//----------------------------------------------------------------------------//
//                                                                            //
//                              P a r t N o d e                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;

import omr.score.common.PixelPoint;
import omr.score.visitor.ScoreVisitor;

import omr.util.TreeNode;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;

/**
 * Class <code>PartNode</code> is an abstract class that is subclassed for any
 * SystemNode that is contained in a system part. So this class encapsulates a
 * direct link to the enclosing part.
 *
 * <p>A link to a related staff is provided as a potential tag only, since all
 * PartNode instances (Slur for example) are not related to a specific staff,
 * whereas a Wedge is.
 *
 * <p>Similarly, we handle a sorted set of underlying glyphs which is useful for
 * most of the subclasses.
 *
 * @author Hervé Bitteur
 */
public abstract class PartNode
    extends SystemNode
{
    //~ Instance fields --------------------------------------------------------

    /** The glyph(s) that compose this element, sorted by abscissa */
    protected final SortedSet<Glyph> glyphs = Glyphs.sortedSet();

    /** Related staff, if relevant */
    private Staff staff;

    /** Reference point */
    private PixelPoint referencePoint;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // PartNode //
    //----------//
    /**
     * Create a PartNode
     *
     * @param container the (direct) container of the node
     */
    public PartNode (SystemNode container)
    {
        super(container);
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // getContextString //
    //------------------//
    @Override
    public String getContextString ()
    {
        StringBuilder sb = new StringBuilder(super.getContextString());
        sb.append("P")
          .append(getPart().getId());

        return sb.toString();
    }

    //-----------//
    // getGlyphs //
    //-----------//
    /**
     * Report the collection of physical glyphs that compose this entity
     *
     * @return the collection of glyphs, which may be empty
     */
    public Collection<Glyph> getGlyphs ()
    {
        return Collections.unmodifiableSortedSet(glyphs);
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
        for (TreeNode c = this; c != null; c = c.getParent()) {
            if (c instanceof SystemPart) {
                return (SystemPart) c;
            }
        }

        return null;
    }

    //-------------------//
    // setReferencePoint //
    //-------------------//
    public void setReferencePoint (PixelPoint referencePoint)
    {
        this.referencePoint = referencePoint;
    }

    //-------------------//
    // getReferencePoint //
    //-------------------//
    /**
     * Report the point of reference for this element, which is generally the
     * element box center, but may be different. For example, the reference
     * point of a DirectionStatement is located on the left side on the base
     * line.
     * @return the point of reference for this element
     */
    public PixelPoint getReferencePoint ()
    {
        if (referencePoint == null) {
            computeReferencePoint();
        }

        return referencePoint;
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

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addGlyph //
    //----------//
    /**
     * Insert a glyph into the collection of underlying glyphs
     * @param glyph the glyph to insert
     */
    public void addGlyph (Glyph glyph)
    {
        glyphs.add(glyph);

        // Invalidate
        reset();
    }

    //------------//
    // computeBox //
    //------------//
    /**
     * Compute the bounding box  of this entity, wrt to the system top-left corner.
     * Unless overridden, this method works on the glyphs collection
     */
    @Override
    protected void computeBox ()
    {
        setBox(computeGlyphsBox(getGlyphs()));
    }

    //------------------//
    // computeReference //
    //------------------//
    /**
     * By default, define the reference point as the center.
     */
    protected void computeReferencePoint ()
    {
        setReferencePoint(getCenter());
    }

    //-------//
    // reset //
    //-------//
    @Override
    protected void reset ()
    {
        super.reset();

        referencePoint = null;
    }
}
