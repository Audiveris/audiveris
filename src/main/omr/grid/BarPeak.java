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
import static omr.grid.BarPeak.Attribute.*;

import omr.sig.AbstractVerticalInter;
import omr.sig.GradeImpacts;

import omr.util.VerticalSide;
import static omr.util.VerticalSide.*;

import java.awt.Rectangle;
import java.util.EnumSet;

/**
 * Class {@code BarPeak} records a peak in staff projection, likely to indicate a bar
 * line crossing the staff.
 *
 * @author Hervé Bitteur
 */
public class BarPeak
        implements Comparable<BarPeak>
{
    //~ Enumerations -------------------------------------------------------------------------------

    /**
     * All attributes flags that can be assigned to a BarPeak instance.
     */
    public static enum Attribute
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** This is a thin peak */
        THIN,
        /** This is a thick peak */
        THICK,
        /** This peak defines staff end abscissa on left or right side */
        STAFF_END,
        /** This peak is a top portion of a bracket */
        BRACKET_TOP,
        /** This peak is a middle portion of a bracket */
        BRACKET_MIDDLE,
        /** This peak is a bottom portion of a bracket */
        BRACKET_BOTTOM,
        /** This peak goes beyond staff top line */
        BEYOND_TOP,
        /** This peak goes beyond staff bottom line */
        BEYOND_BOTTOM,
        /** This peak is a portion of a brace */
        BRACE,
        /** This peak is the thick one of a C-Clef */
        CCLEF_ONE,
        /** This peak is the thin one of a C-Clef */
        CCLEF_TWO,
        /** This peak is part of the tail of a C-Clef */
        CCLEF_TAIL,
        /** This peak is aligned (if not connected) with a peak in staff above or below */
        ALIGNED,
        /** This peak is not aligned in a multi-staff system */
        UNALIGNED;
    }

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

    /** Evaluation. */
    private final GradeImpacts impacts;

    /** Underlying stick. */
    private Glyph glyph;

    /** Corresponding bar or bracket inter, if any. */
    private AbstractVerticalInter inter;

    /** Attributes currently set. */
    private final EnumSet<Attribute> attrs = EnumSet.noneOf(Attribute.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BarPeak object.
     *
     * @param staff   containing staff
     * @param top     top ordinate
     * @param bottom  bottom ordinate
     * @param start   starting abscissa
     * @param stop    stopping abscissa
     * @param impacts evaluation details
     */
    public BarPeak (StaffInfo staff,
                    int top,
                    int bottom,
                    int start,
                    int stop,
                    GradeImpacts impacts)
    {
        this.staff = staff;
        this.top = top;
        this.bottom = bottom;
        this.start = start;
        this.stop = stop;
        this.impacts = impacts;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // compareTo //
    //-----------//
    @Override
    public int compareTo (BarPeak that)
    {
        return Integer.compare(start, that.start);
    }

    //-----------//
    // getBottom //
    //-----------//
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

    //----------//
    // getStart //
    //----------//
    /**
     * @return the start
     */
    public int getStart ()
    {
        return start;
    }

    //---------//
    // getStop //
    //---------//
    /**
     * @return the stop
     */
    public int getStop ()
    {
        return stop;
    }

    //--------//
    // getTop //
    //--------//
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

    //----------//
    // isBeyond //
    //----------//
    public boolean isBeyond (VerticalSide side)
    {
        return isSet((side == TOP) ? BEYOND_TOP : BEYOND_BOTTOM);
    }

    //----------//
    // isBeyond //
    //----------//
    public boolean isBeyond ()
    {
        return isSet(BEYOND_TOP) || isSet(BEYOND_BOTTOM);
    }

    //-----------//
    // isBracket //
    //-----------//
    /**
     * Report whether this peak is a portion of a bracket.
     *
     * @return true if bracket top, bottom or middle
     */
    public boolean isBracket ()
    {
        return isSet(BRACKET_TOP) || isSet(BRACKET_MIDDLE) || isSet(BRACKET_BOTTOM);
    }

    //--------------//
    // isBracketEnd //
    //--------------//
    /**
     * Report whether this peak is the end portion of a bracket on desired side.
     *
     * @param side desired side
     * @return true if bracket end on desired side
     */
    public boolean isBracketEnd (VerticalSide side)
    {
        return isSet((side == TOP) ? BRACKET_TOP : BRACKET_BOTTOM);
    }

    //-------//
    // isSet //
    //-------//
    /**
     * Check whether the provided attribute is set to this instance.
     *
     * @param attr provided attribute
     * @return true if set
     */
    public final boolean isSet (Attribute attr)
    {
        return attrs.contains(attr);
    }

    //-------//
    // isSet //
    //-------//
    /**
     * Set the provided attribute to this instance.
     *
     * @param attr provided attribute
     */
    public final void set (Attribute attr)
    {
        attrs.add(attr);
    }

    //-----------//
    // setBeyond //
    //-----------//
    /**
     * Set peak as beyond staff height on provided side.
     *
     * @param side provided vertical side
     */
    public void setBeyond (VerticalSide side)
    {
        set((side == TOP) ? BEYOND_TOP : BEYOND_BOTTOM);
    }

    /**
     * Set peak as bracket end on provided side.
     *
     * @param side provided end side
     */
    public void setBracketEnd (VerticalSide side)
    {
        set((side == TOP) ? BRACKET_TOP : BRACKET_BOTTOM);
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("BarPeak{");

        sb.append("(");
        sb.append(start);
        sb.append("-");
        sb.append(stop);
        sb.append(")");

        if (glyph != null) {
            sb.append(" glyph#").append(glyph.getId());
        }

        if (!attrs.isEmpty()) {
            sb.append(" ").append(attrs);
        }

        sb.append("}");

        return sb.toString();
    }
}
