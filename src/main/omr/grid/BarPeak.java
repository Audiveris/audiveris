//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          B a r P e a k                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.glyph.facets.Glyph;

import omr.sig.AbstractVerticalInter;
import omr.sig.GradeImpacts;

import omr.util.VerticalSide;

import java.awt.Rectangle;

/**
 * Class {@code BarPeak} records a peak in staff projection, likely to indicate a bar
 * line crossing the staff.
 *
 * @author Hervé Bitteur
 */
public class BarPeak
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Containing staff. */
    private final StaffInfo staff;

    /** Line ordinate at top. */
    private final int top;

    /** Line ordinate at bottom. */
    private final int bottom;

    /** Precise left abscissa. */
    private final int start;

    /** Precise right abscissa. */
    private final int stop;

    /** Cumulated height. */
    private final int value;

    /** Cumulated chunk pixels. */
    private final int chunk;

    /** Evaluation. */
    private final GradeImpacts impacts;

    /** Thin or thick peak. */
    private Boolean isThin;

    /** Underlying stick. */
    private Glyph glyph;

    /** Does the related glyph ends as a bracket above the staff?. */
    private boolean isBracketAbove;

    /** Does the related glyph ends as a bracket below the staff?. */
    private boolean isBracketBelow;

    /** Does this peak correspond to a bracket middle?. */
    private boolean isBracketMiddle;

    /** Does the related glyph get above the staff?. */
    private boolean isAbove;

    /** Does the related glyph get below the staff?. */
    private boolean isBelow;

    /** Corresponding bar or bracket inter, if any. */
    private AbstractVerticalInter inter;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BarPeak object.
     *
     * @param staff   containing staff
     * @param top     top ordinate
     * @param bottom  bottom ordinate
     * @param start   starting abscissa
     * @param stop    stopping abscissa
     * @param value   count of cumulated pixels
     * @param chunk   count in excess of standard staff lines
     * @param impacts evaluation details
     */
    public BarPeak (StaffInfo staff,
                    int top,
                    int bottom,
                    int start,
                    int stop,
                    int value,
                    int chunk,
                    GradeImpacts impacts)
    {
        this.staff = staff;
        this.top = top;
        this.bottom = bottom;
        this.start = start;
        this.stop = stop;
        this.value = value;
        this.chunk = chunk;
        this.impacts = impacts;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * @return the bottom
     */
    public int getBottom ()
    {
        return bottom;
    }

    //-----------//
    // getBounds //
    //-----------//
    public Rectangle getBounds ()
    {
        return new Rectangle(start, top, getWidth(), bottom - top + 1);
    }

    //----------//
    // getGlyph //
    //----------//
    /**
     * @return the glyph
     */
    public Glyph getGlyph ()
    {
        return glyph;
    }

    //------------//
    // getImpacts //
    //------------//
    /**
     * @return the impacts
     */
    public GradeImpacts getImpacts ()
    {
        return impacts;
    }

    //----------//
    // getInter //
    //----------//
    /**
     * @return the inter
     */
    public AbstractVerticalInter getInter ()
    {
        return inter;
    }

    //-------------//
    // getOrdinate //
    //-------------//
    public int getOrdinate (VerticalSide side)
    {
        if (side == VerticalSide.TOP) {
            return top;
        } else {
            return bottom;
        }
    }

    //----------//
    // getStaff //
    //----------//
    /**
     * @return the staff
     */
    public StaffInfo getStaff ()
    {
        return staff;
    }

    /**
     * @return the start
     */
    public int getStart ()
    {
        return start;
    }

    /**
     * @return the stop
     */
    public int getStop ()
    {
        return stop;
    }

    /**
     * @return the top
     */
    public int getTop ()
    {
        return top;
    }

    //----------//
    // getWidth //
    //----------//
    public int getWidth ()
    {
        return stop - start + 1;
    }

    /**
     * @return the isAbove
     */
    public boolean isAbove ()
    {
        return isAbove;
    }

    /**
     * @return the isBelow
     */
    public boolean isBelow ()
    {
        return isBelow;
    }

    //----------//
    // isBeyond //
    //----------//
    public boolean isBeyond (VerticalSide side)
    {
        if (side == VerticalSide.TOP) {
            return isAbove;
        } else {
            return isBelow;
        }
    }

    //-----------//
    // isBracket //
    //-----------//
    public boolean isBracket ()
    {
        return isBracketAbove || isBracketMiddle || isBracketBelow;
    }

    /**
     * @return the isBracketAbove
     */
    public boolean isBracketAbove ()
    {
        return isBracketAbove;
    }

    /**
     * @return the isBracketBelow
     */
    public boolean isBracketBelow ()
    {
        return isBracketBelow;
    }

    /**
     * @return the isBracketMiddle
     */
    public boolean isBracketMiddle ()
    {
        return isBracketMiddle;
    }

    //--------//
    // isThin //
    //--------//
    public Boolean isThin ()
    {
        return isThin;
    }

    /**
     *
     */
    public void setAbove ()
    {
        this.isAbove = true;
    }

    /**
     *
     */
    public void setBelow ()
    {
        this.isBelow = true;
    }

    /**
     *
     */
    public void setBracketAbove ()
    {
        this.isBracketAbove = true;
    }

    /**
     *
     */
    public void setBracketBelow ()
    {
        this.isBracketBelow = true;
    }

    /**
     *
     */
    public void setBracketMiddle ()
    {
        this.isBracketMiddle = true;
    }

    //----------//
    // setGlyph //
    //----------//
    /**
     * @param glyph the glyph to set
     */
    public void setGlyph (Glyph glyph)
    {
        this.glyph = glyph;
    }

    /**
     * @param inter the inter to set
     */
    public void setInter (AbstractVerticalInter inter)
    {
        this.inter = inter;
    }

    //---------//
    // setThin //
    //---------//
    public void setThin (boolean bool)
    {
        isThin = bool;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("BarPeak{");

        if (isThin != null) {
            sb.append(isThin ? "THIN" : "THICK");
        }

        sb.append("(");
        sb.append(start);
        sb.append("-");
        sb.append(stop);
        sb.append(")");

        if (glyph != null) {
            sb.append(" glyph#").append(glyph.getId());
        }

        if (isBracketAbove) {
            sb.append(" bracketTop");
        }

        if (isAbove) {
            sb.append(" above");
        }

        if (isBracketMiddle) {
            sb.append(" bracketMiddle");
        }

        if (isBracketBelow) {
            sb.append(" bracketBottom");
        }

        if (isBelow) {
            sb.append(" below");
        }

        sb.append("}");

        return sb.toString();
    }
}
