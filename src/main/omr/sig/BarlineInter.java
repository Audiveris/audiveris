//----------------------------------------------------------------------------//
//                                                                            //
//                           B a r l i n e I n t e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.math.AreaUtil;
import omr.math.Line;

/**
 * Class {@code BarmedianInter} represents an interpretation of bar median
 * (thin or thick vertical segment).
 *
 * @author Hervé Bitteur
 */
public class BarlineInter
    extends AbstractInter
{
    //~ Instance fields --------------------------------------------------------

    /** True if this bar median defines a part. */
    private boolean partDefining;

    /** Median line, perhaps not fully straight. */
    private final Line median;

    /** Line width. */
    private final double width;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BarmedianInter object.
     *
     * @param glyph   the underlying glyph
     * @param shape   the assigned shape
     * @param impacts the assignment details
     * @param median  the median line
     * @param width   the bar line width
     */
    public BarlineInter (Glyph        glyph,
                         Shape        shape,
                         GradeImpacts impacts,
                         Line         median,
                         double       width)
    {
        super(glyph, shape, impacts.getGrade());
        setImpacts(impacts);
        this.median = median;
        this.width = width;

        setArea(AreaUtil.verticalRibbon(median, width));

        // Define precise bounds based on this path
        setBounds(getArea().getBounds());
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        if (partDefining) {
            sb.append(" part");
        }

        return sb.toString();
    }

    //-----------//
    // getMedian //
    //-----------//
    /**
     * @return the median
     */
    public Line getMedian ()
    {
        return median;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * @return the width
     */
    public double getWidth ()
    {
        return width;
    }

    //----------------//
    // isPartDefining //
    //----------------//
    /**
     * @return the partDefining
     */
    public boolean isPartDefining ()
    {
        return partDefining;
    }

    //-----------------//
    // setPartDefining //
    //-----------------//
    /**
     * @param partDefining the partDefining to set
     */
    public void setPartDefining (boolean partDefining)
    {
        this.partDefining = partDefining;
    }
}
