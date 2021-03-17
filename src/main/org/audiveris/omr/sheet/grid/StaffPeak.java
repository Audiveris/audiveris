//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t a f f P e a k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.grid;

import org.audiveris.omr.glyph.dynamic.Filament;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.Staff;
import static org.audiveris.omr.sheet.grid.StaffPeak.Attribute.*;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.TOP;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.EnumSet;
import java.util.Objects;

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

    /** Corresponding inter, if any. */
    protected Inter inter;

    /** Attributes currently set. */
    protected final EnumSet<Attribute> attrs = EnumSet.noneOf(Attribute.class);

    /** Top serif filament, if any. */
    private Filament topSerif;

    /** Bottom serif filament, if any. */
    private Filament bottomSerif;

    /** Evaluation, if any. */
    private final GradeImpacts impacts;

    /** Containing column, if any. */
    private BarColumn column;

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
        if (this == that) {
            return 0;
        }

        // Total ordering, first by staff order, then by abscissa in staff
        if (this.staff != that.staff) {
            return Staff.byId.compare(this.staff, that.staff);
        }

        if (this.start != that.start) {
            return Integer.compare(this.start, that.start);
        }

        return Integer.compare(this.stop, that.stop);
    }

    //-----------------------//
    // computeDeskewedCenter //
    //-----------------------//
    /**
     * Compute the (de-skewed) peak center.
     *
     * @param skew global sheet skew
     */
    public void computeDeskewedCenter (Skew skew)
    {
        Point2D mid = new Point2D.Double((start + stop) / 2.0, (top + bottom) / 2.0);

        dsk = skew.deskewed(mid);
    }

    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (obj instanceof StaffPeak) {
            return compareTo((StaffPeak) obj) == 0;
        }

        return false;
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
    /**
     * Report the bounding box of the peak.
     *
     * @return bounds
     */
    public Rectangle getBounds ()
    {
        return new Rectangle(start, top, getWidth(), bottom - top + 1);
    }

    //-----------//
    // getColumn //
    //-----------//
    /**
     * @return the column
     */
    public BarColumn getColumn ()
    {
        return column;
    }

    //-----------//
    // setColumn //
    //-----------//
    /**
     * Assign the containing column.
     *
     * @param column the column to set
     */
    public void setColumn (BarColumn column)
    {
        this.column = column;
    }

    //---------------------//
    // getDeskewedAbscissa //
    //---------------------//
    /**
     * Report the abscissa of peak de-skewed center.
     *
     * @return (de-skewed) x
     */
    public double getDeskewedAbscissa ()
    {
        return dsk.getX();
    }

    //-------------------//
    // getDeskewedCenter //
    //-------------------//
    /**
     * Report the de-skewed peak center
     *
     * @return de-skewed center
     */
    public Point2D getDeskewedCenter ()
    {
        return dsk;
    }

    //-------------//
    // getFilament //
    //-------------//
    /**
     * Report the related filament, if any
     *
     * @return filament, perhaps null
     */
    public Filament getFilament ()
    {
        return filament;
    }

    //-------------//
    // setFilament //
    //-------------//
    /**
     * Assign a related filament.
     *
     * @param filament the related filament
     */
    public void setFilament (Filament filament)
    {
        this.filament = filament;
    }

    //------------//
    // getImpacts //
    //------------//
    /**
     * Report the peak evaluation, if any.
     *
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
     * Report the related inter, if any.
     *
     * @return the inter
     */
    public Inter getInter ()
    {
        return inter;
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
    // getOrdinate //
    //-------------//
    /**
     * Report peak ordinate on desired vertical side.
     *
     * @param side the desired vertical side
     * @return the end ordinate on given side
     */
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
     * Report the underlying staff.
     *
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
     * Report the starting abscissa
     *
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
     * Report the ending abscissa
     *
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
     * Return line ordinate at top of peak.
     *
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
     * Report the serif at top of peak, if any
     *
     * @return the topSerif
     */
    public Filament getTopSerif ()
    {
        return topSerif;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report peak width
     *
     * @return the width of peak
     */
    public int getWidth ()
    {
        return stop - start + 1;
    }

    @Override
    public int hashCode ()
    {
        int hash = 5;
        hash = (71 * hash) + Objects.hashCode(staff);
        hash = (71 * hash) + this.start;

        return hash;
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

    //------------//
    // isStaffEnd //
    //------------//
    /**
     * Tell whether this peak is a staff end on provided horizontal side.
     *
     * @param side desired horizontal side
     * @return true if so
     */
    public boolean isStaffEnd (HorizontalSide side)
    {
        return isSet((side == LEFT) ? STAFF_LEFT_END : STAFF_RIGHT_END);
    }

    //-------//
    // isVip //
    //-------//
    /**
     * Tell whether it's a VIP object
     *
     * @return true if so
     */
    public boolean isVip ()
    {
        return (filament != null) && filament.isVip();
    }

    //--------//
    // render //
    //--------//
    /**
     * Render this peak on the provided graphics
     *
     * @param g graphics context
     */
    public void render (Graphics2D g)
    {
        g.setColor(
                isBrace() ? Colors.STAFF_PEAK_BRACE
                        : (isBracket() ? Colors.STAFF_PEAK_BRACKET : Colors.STAFF_PEAK));
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

    //-------//
    // unset //
    //-------//
    /**
     * Un-set the provided attribute to this instance.
     *
     * @param attr provided attribute
     */
    public final void unset (Attribute attr)
    {
        attrs.remove(attr);
    }

    //-----------//
    // internals //
    //-----------//
    /**
     * Report a string description of class internals
     *
     * @return string description of internals
     */
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder();

        if (!attrs.isEmpty()) {
            sb.append(" ").append(attrs);
        }

        return sb.toString();
    }
}
