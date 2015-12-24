//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t a f f P e a k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.glyph.dynamic.Filament;

import omr.sheet.Staff;

import static omr.sheet.grid.StaffPeak.Attribute.*;

import omr.sig.GradeImpacts;
import omr.sig.inter.AbstractVerticalInter;
import omr.sig.inter.BraceInter;
import omr.sig.inter.Inter;

import omr.util.HorizontalSide;

import static omr.util.HorizontalSide.LEFT;

import omr.util.VerticalSide;

import static omr.util.VerticalSide.TOP;

import java.awt.Rectangle;
import java.util.EnumSet;

/**
 * Class {@code StaffPeak} represents a peak in staff projection onto x-axis.
 * <p>
 * Such peak can represent a brace peak or a bar peak (barline or bracket).
 *
 * @author Hervé Bitteur
 */
public abstract class StaffPeak
        implements Comparable<StaffPeak>
{
    //~ Enumerations -------------------------------------------------------------------------------

    /**
     * All attributes flags that can be assigned to a Bar instance.
     */
    public static enum Attribute
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** This is a thin peak */
        THIN,
        /** This is a thick peak */
        THICK,
        /** This peak defines staff left end */
        STAFF_LEFT_END,
        /** This peak defines staff right end */
        STAFF_RIGHT_END,
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
        /** This peak is the thick one of a C-Clef */
        CCLEF_ONE,
        /** This peak is the thin one of a C-Clef */
        CCLEF_TWO,
        /** This peak is part of the tail of a C-Clef */
        CCLEF_TAIL,
        /** This peak is aligned (if not connected) with a peak in staff above or below */
        ALIGNED,
        /** This peak is in a multi-staff system, but not aligned */
        UNALIGNED,
        /** This peak is a portion of a brace */
        BRACE,
        /** This peak is a top portion of a brace */
        BRACE_TOP,
        /** This peak is a middle portion of a brace */
        BRACE_MIDDLE,
        /** This peak is a bottom portion of a brace */
        BRACE_BOTTOM;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Containing staff. */
    protected final Staff staff;

    /** Line ordinate at top. */
    protected final int top;

    /** Line ordinate at bottom. */
    protected final int bottom;

    /** Precise left abscissa. */
    protected final int start;

    /** Precise right abscissa. */
    protected final int stop;

    /** Underlying filament. */
    protected Filament filament;

    /** Corresponding inter, if any. */
    protected Inter inter;

    /** Attributes currently set. */
    protected final EnumSet<Attribute> attrs = EnumSet.noneOf(Attribute.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StaffPeak} object.
     *
     * @param staff  containing staff
     * @param top    top ordinate
     * @param bottom bottom ordinate
     * @param start  starting abscissa
     * @param stop   stopping abscissa
     */
    public StaffPeak (Staff staff,
                      int top,
                      int bottom,
                      int start,
                      int stop)
    {
        this.staff = staff;
        this.top = top;
        this.bottom = bottom;
        this.start = start;
        this.stop = stop;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // compareTo //
    //-----------//
    @Override
    public int compareTo (StaffPeak that)
    {
        // Peaks are implicitly sorted by abscissa
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

    //-------------//
    // getFilament //
    //-------------//
    public Filament getFilament ()
    {
        return filament;
    }

    //----------//
    // getInter //
    //----------//
    /**
     * @return the inter
     */
    public Inter getInter ()
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
    public Staff getStaff ()
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

    //------------//
    // isBraceEnd //
    //------------//
    /**
     * Report whether this peak is the end portion of a brace on desired side.
     *
     * @param side desired side
     * @return true if brace end on desired side
     */
    public boolean isBraceEnd (VerticalSide side)
    {
        return isSet((side == TOP) ? BRACE_TOP : BRACE_BOTTOM);
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

    //------------//
    // isStaffEnd //
    //------------//
    public boolean isStaffEnd (HorizontalSide side)
    {
        return isSet((side == LEFT) ? STAFF_LEFT_END : STAFF_RIGHT_END);
    }

    //-----//
    // set //
    //-----//
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

    //---------------//
    // setBracketEnd //
    //---------------//
    /**
     * Set peak as bracket end on provided side.
     *
     * @param side provided end side
     */
    public void setBracketEnd (VerticalSide side)
    {
        set((side == TOP) ? BRACKET_TOP : BRACKET_BOTTOM);
    }

    //-------------//
    // setFilament //
    //-------------//
    public void setFilament (Filament filament)
    {
        this.filament = filament;
    }

    //-------------//
    // setStaffEnd //
    //-------------//
    /**
     * Set peak as staff end on the provided horizontal side.
     *
     * @param side provided horizontal side
     */
    public void setStaffEnd (HorizontalSide side)
    {
        set((side == LEFT) ? STAFF_LEFT_END : STAFF_RIGHT_END);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        sb.append("(");
        sb.append(start);
        sb.append("-");
        sb.append(stop);
        sb.append(")");

        if (filament != null) {
            sb.append(" glyph#").append(filament.getId());
        }

        sb.append(internals());
        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // internals //
    //-----------//
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder();

        if (!attrs.isEmpty()) {
            sb.append(" ").append(attrs);
        }

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----//
    // Bar //
    //-----//
    /**
     * Class {@code Bar} records a peak in staff projection, likely to indicate a bar
     * line or a bracket crossing the staff.
     */
    public static class Bar
            extends StaffPeak
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Evaluation. */
        private final GradeImpacts impacts;

        //~ Constructors ---------------------------------------------------------------------------
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
        public Bar (Staff staff,
                    int top,
                    int bottom,
                    int start,
                    int stop,
                    GradeImpacts impacts)
        {
            super(staff, top, bottom, start, stop);
            this.impacts = impacts;
        }

        //~ Methods --------------------------------------------------------------------------------
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
        @Override
        public AbstractVerticalInter getInter ()
        {
            return (AbstractVerticalInter) inter;
        }

        /**
         * @param inter the inter to set
         */
        public void setInter (AbstractVerticalInter inter)
        {
            this.inter = inter;
        }
    }

    //-------//
    // Brace //
    //-------//
    /**
     * Class {@code Brace} is a peak meant for brace portion.
     */
    public static class Brace
            extends StaffPeak
    {
        //~ Constructors ---------------------------------------------------------------------------

        /**
         * Creates a new {@code BracePeak} object.
         *
         * @param staff  containing staff
         * @param top    top ordinate
         * @param bottom bottom ordinate
         * @param start  starting abscissa
         * @param stop   stopping abscissa
         */
        public Brace (Staff staff,
                      int top,
                      int bottom,
                      int start,
                      int stop)
        {
            super(staff, top, bottom, start, stop);
        }

        //~ Methods --------------------------------------------------------------------------------
        //----------//
        // getInter //
        //----------//
        /**
         * @return the inter
         */
        @Override
        public BraceInter getInter ()
        {
            return (BraceInter) inter;
        }

        //----------//
        // setInter //
        //----------//
        /**
         * @param inter the inter to set
         */
        public void setInter (BraceInter inter)
        {
            this.inter = inter;
        }
    }
}
