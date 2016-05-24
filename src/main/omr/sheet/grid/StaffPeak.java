//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t a f f P e a k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.glyph.dynamic.Filament;

import omr.sheet.Skew;
import omr.sheet.Staff;
import static omr.sheet.grid.StaffPeak.Attribute.*;

import omr.sig.GradeImpacts;
import omr.sig.inter.Inter;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.LEFT;
import omr.util.VerticalSide;
import static omr.util.VerticalSide.TOP;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.EnumSet;

/**
 * Class {@code StaffPeak} represents a peak in staff projection onto x-axis.
 * <p>
 * Such peak may indicate a brace portion, a barline portion, a bracket portion or just garbage.
 *
 * @author Hervé Bitteur
 */
public class StaffPeak
        implements Comparable<StaffPeak>
{
    //~ Enumerations -------------------------------------------------------------------------------

    /**
     * All attributes flags that can be assigned to a StaffPeak instance.
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
        /** This peak is the thick one of a C-Clef */
        CCLEF_ONE,
        /** This peak is the thin one of a C-Clef */
        CCLEF_TWO,
        /** This peak is part of the tail of a C-Clef */
        CCLEF_TAIL,
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

    /** De-skewed center. */
    protected Point2D dsk;

    /** Underlying filament. */
    protected Filament filament;

    /** Top serif filament, if any. */
    private Filament topSerif;

    /** Bottom serif filament, if any. */
    private Filament bottomSerif;

    /** Corresponding inter, if any. */
    protected Inter inter;

    /** Attributes currently set. */
    protected final EnumSet<Attribute> attrs = EnumSet.noneOf(Attribute.class);

    /** Evaluation, if any. */
    private final GradeImpacts impacts;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StaffPeak} object.
     *
     * @param staff   containing staff
     * @param top     top ordinate
     * @param bottom  bottom ordinate
     * @param start   starting abscissa
     * @param stop    stopping abscissa
     * @param impacts evaluation details or null
     */
    public StaffPeak (Staff staff,
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
    public int compareTo (StaffPeak that)
    {
        if (this.getStaff() != that.getStaff()) {
            return Staff.byId.compare(this.getStaff(), that.getStaff());
        }

        return Integer.compare(this.start, that.start);
    }

    //-----------------------//
    // computeDeskewedCenter //
    //-----------------------//
    public void computeDeskewedCenter (Skew skew)
    {
        Point2D mid = new Point2D.Double((start + stop) / 2.0, (top + bottom) / 2.0);

        dsk = skew.deskewed(mid);
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

    //----------------//
    // getBottomSerif //
    //----------------//
    /**
     * @return the bottomSerif
     */
    public Filament getBottomSerif ()
    {
        return bottomSerif;
    }

    //-----------//
    // getBounds //
    //-----------//
    public Rectangle getBounds ()
    {
        return new Rectangle(start, top, getWidth(), bottom - top + 1);
    }

    //---------------------//
    // getDeskewedAbscissa //
    //---------------------//
    public double getDeskewedAbscissa ()
    {
        return dsk.getX();
    }

    //-------------------//
    // getDeskewedCenter //
    //-------------------//
    public Point2D getDeskewedCenter ()
    {
        return dsk;
    }

    //-------------//
    // getFilament //
    //-------------//
    public Filament getFilament ()
    {
        return filament;
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

    //-------------//
    // getTopSerif //
    //-------------//
    /**
     * @return the topSerif
     */
    public Filament getTopSerif ()
    {
        return topSerif;
    }

    //----------//
    // getWidth //
    //----------//
    public int getWidth ()
    {
        return stop - start + 1;
    }

    //---------//
    // isBrace //
    //---------//
    /**
     * Report whether this peak is a portion of a brace.
     *
     * @return true if brace, brace top, bottom or middle
     */
    public boolean isBrace ()
    {
        return isSet(BRACE) || isSet(BRACE_TOP) || isSet(BRACE_MIDDLE) || isSet(BRACE_BOTTOM);
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

    //-------//
    // isVip //
    //-------//
    public boolean isVip ()
    {
        return (filament != null) && filament.isVip();
    }

    //--------//
    // render //
    //--------//
    public void render (Graphics2D g)
    {
        g.fillRect(start, top, stop - start + 1, bottom - top + 1);
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

    //------------//
    // isStaffEnd //
    //------------//
    public boolean isStaffEnd (HorizontalSide side)
    {
        return isSet((side == LEFT) ? STAFF_LEFT_END : STAFF_RIGHT_END);
    }

    //---------------//
    // setBracketEnd //
    //---------------//
    /**
     * Set peak as bracket end on provided side.
     *
     * @param side  provided end side
     * @param serif the serif filament
     */
    public void setBracketEnd (VerticalSide side,
                               Filament serif)
    {
        if (side == TOP) {
            set(BRACKET_TOP);
            topSerif = serif;
        } else {
            set(BRACKET_BOTTOM);
            bottomSerif = serif;
        }
    }

    //-------------//
    // setFilament //
    //-------------//
    public void setFilament (Filament filament)
    {
        this.filament = filament;
    }

    //----------//
    // setInter //
    //----------//
    /**
     * Set the related Inter.
     *
     * @param inter the inter to set (instance of BraceInter or AbstractVerticalInter)
     */
    public void setInter (Inter inter)
    {
        this.inter = inter;
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
        StringBuilder sb = new StringBuilder("Peak");
        sb.append("{");

        sb.append(start);
        sb.append("-");
        sb.append(stop);

        if (filament != null) {
            sb.append(" F#").append(filament.getId());
        }

        sb.append(" T#").append(staff.getId());

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
}
