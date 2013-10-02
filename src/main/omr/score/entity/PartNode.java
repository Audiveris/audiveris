//----------------------------------------------------------------------------//
//                                                                            //
//                              P a r t N o d e                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;

import omr.score.visitor.ScoreVisitor;

import omr.util.TreeNode;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

/**
 * Class {@code PartNode} is an abstract class that is subclassed for
 * any SystemNode that is contained in a system part.
 * So this class encapsulates a direct link to the enclosing part.
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
    private Point referencePoint;

    //~ Constructors -----------------------------------------------------------
    //----------//
    // PartNode //
    //----------//
    /**
     * Create a PartNode.
     *
     * @param container the (direct) container of the node
     */
    public PartNode (SystemNode container)
    {
        super(container);
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
    // addGlyph //
    //----------//
    /**
     * Insert a glyph into the collection of underlying glyphs.
     *
     * @param glyph the glyph to insert
     */
    public void addGlyph (Glyph glyph)
    {
        glyphs.add(glyph);

        // Invalidate
        reset();
    }

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
     * Report the collection of physical glyphs that compose this entity.
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
     * Report the containing part.
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
    // getReferencePoint //
    //-------------------//
    /**
     * Report the point of reference for this element, which is
     * generally the element box center, but may be different.
     * For example, the reference point of a DirectionStatement is located on
     * the left side on the base line.
     *
     * @return the point of reference for this element
     */
    public Point getReferencePoint ()
    {
        if (referencePoint == null) {
            computeReferencePoint();
        }

        return referencePoint;
    }

    //----------//
    // getStaff //
    //----------//
    /**
     * Report the related staff if any.
     *
     * @return the related staff, or null
     */
    public Staff getStaff ()
    {
        return staff;
    }

    //---------------------//
    // getTranslationLinks //
    //---------------------//
    /**
     * Report the translation link(s) between the provided glyph and
     * this translating entity.
     *
     * @param glyph an originating glyph
     * @return the collection of links, perhaps empty but not null.
     */
    public List<Line2D> getTranslationLinks (Glyph glyph)
    {
        Point from = glyph.getLocation();
        Point to = getReferencePoint();
        Line2D line = new Line2D.Double(from, to);

        return Arrays.asList(line);
    }

    //-------------------//
    // setReferencePoint //
    //-------------------//
    public void setReferencePoint (Point referencePoint)
    {
        this.referencePoint = referencePoint;
    }

    //----------//
    // setStaff //
    //----------//
    /**
     * Assign the related staff.
     *
     * @param staff the related staff
     */
    public void setStaff (Staff staff)
    {
        this.staff = staff;
    }

    //------------//
    // computeBox //
    //------------//
    /**
     * Compute the bounding box of this entity.
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
