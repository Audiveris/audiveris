//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P a r t B a r l i n e                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.FermataInter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code PartBarline} represents a logical barline for a part, that is made of
 * one {@code StaffBarlineInter} for each staff in the part.
 * <p>
 * In the case of "back to back" repeat configuration, we use two instances of this class, one
 * for the backward repeat and one for the forward repeat.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "part-barline")
public class PartBarline
{

    private static final Logger logger = LoggerFactory.getLogger(PartBarline.class);

    /** <b>OLD</b> underlying {@link StaffBarlineInter} instances. */
    @Deprecated
    @XmlElement(name = "staff-barline")
    private List<OldStaffBarline> oldStaffBarlines;

    /** Underlying {@link StaffBarlineInter} instances, one per staff in the part. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "staff-barlines")
    private final List<StaffBarlineInter> staffBarlines = new ArrayList<>();

    /**
     * Creates a new {@code PartBarline} object.
     */
    public PartBarline ()
    {
    }

    //-----------------//
    // addStaffBarline //
    //-----------------//
    /**
     * Include a staff barline into this PartBarline.
     *
     * @param staffBarline the StaffBarlineInter to include
     */
    public void addStaffBarline (StaffBarlineInter staffBarline)
    {
        Objects.requireNonNull(staffBarline, "Trying to add a null StaffBarline");

        staffBarlines.add(staffBarline);
    }

    //----------//
    // contains //
    //----------//
    /**
     * Report whether this PartBarline contains the provided StaffBarline.
     *
     * @param staffBarline the StaffBarlineInter to check
     * @return true if contained
     */
    public boolean contains (StaffBarlineInter staffBarline)
    {
        return staffBarlines.contains(staffBarline);
    }

    //-----------//
    // getEnding //
    //-----------//
    /**
     * Report related ending, if any, with bar on desired side of ending.
     *
     * @param side horizontal side of barline WRT ending
     * @return the ending found or null
     */
    public EndingInter getEnding (HorizontalSide side)
    {
        final StaffBarlineInter sb = staffBarlines.get(0);

        return sb.getEnding(side);
    }

    //-------------//
    // getFermatas //
    //-------------//
    /**
     * Convenient method to report related fermata signs, if any
     *
     * @return list of (several?) fermata inters, top down, perhaps empty but not null
     */
    public List<FermataInter> getFermatas ()
    {
        Set<FermataInter> fermatas = new LinkedHashSet<>();

        for (StaffBarlineInter sb : staffBarlines) {
            fermatas.addAll(sb.getFermatas());
        }

        if (!fermatas.isEmpty()) {
            List<FermataInter> list = new ArrayList<>(fermatas);
            Collections.sort(list, Inters.byCenterOrdinate);

            return list;
        }

        return Collections.emptyList();
    }

    //----------//
    // getLeftX //
    //----------//
    /**
     * Report the center abscissa of the left bar
     *
     * @param part  the containing part
     * @param staff the staff for precise ordinate
     * @return abscissa of the left side
     */
    public int getLeftX (Part part,
                         Staff staff)
    {
        StaffBarlineInter bar = getStaffBarline(part, staff);

        if (bar != null) {
            return bar.getLeftX();
        } else {
            throw new IllegalStateException("Part Barline with no proper StaffBarline");
        }
    }

    //-----------//
    // getRightX //
    //-----------//
    /**
     * Report the center abscissa of the right bar
     *
     * @param part  the containing part
     * @param staff the staff for precise ordinate
     * @return abscissa of the right side
     */
    public int getRightX (Part part,
                          Staff staff)
    {
        StaffBarlineInter bar = getStaffBarline(part, staff);

        if (bar != null) {
            return bar.getRightX();
        } else {
            throw new IllegalStateException("Part Barline with no proper StaffBarline");
        }
    }

    //-----------------//
    // getStaffBarline //
    //-----------------//
    /**
     * Report the StaffBarline at crossing of part with provided staff.
     *
     * @param part  containing part
     * @param staff desired staff
     * @return the StaffBarline found or null
     */
    public StaffBarlineInter getStaffBarline (Part part,
                                              Staff staff)
    {
        int index = part.getStaves().indexOf(staff);

        if ((index != -1) && (index < staffBarlines.size())) {
            return staffBarlines.get(index);
        }

        return null;
    }

    //------------------//
    // getStaffBarlines //
    //------------------//
    /**
     * Report the vertical sequence of StaffBarlineInter instances.
     *
     * @return list of StaffBarline instances
     */
    public List<StaffBarlineInter> getStaffBarlines ()
    {
        return Collections.unmodifiableList(staffBarlines);
    }

    //----------//
    // getStyle //
    //----------//
    /**
     * Report the (part) barline style.
     *
     * @return barline style, or null if no StaffBarline is contained.
     */
    public Style getStyle ()
    {
        if (staffBarlines.isEmpty()) {
            return null;
        }

        return staffBarlines.get(0).getStyle();
    }

    //--------------//
    // isLeftRepeat //
    //--------------//
    /**
     * Report whether this is a left repeat barline.
     *
     * @return true if so
     */
    public boolean isLeftRepeat ()
    {
        for (StaffBarlineInter sb : staffBarlines) {
            if (sb.isLeftRepeat()) {
                return true;
            }
        }

        return false;
    }

    //---------------//
    // isRightRepeat //
    //---------------//
    /**
     * Report whether this is a right repeat barline.
     *
     * @return true if so
     */
    public boolean isRightRepeat ()
    {
        for (StaffBarlineInter sb : staffBarlines) {
            if (sb.isRightRepeat()) {
                return true;
            }
        }

        return false;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder("{PartBarline");

        if (!staffBarlines.isEmpty()) {
            StaffBarlineInter first = staffBarlines.get(0);
            StaffBarlineInter last = staffBarlines.get(staffBarlines.size() - 1);

            Style style = first.getStyle();
            sb.append(' ').append(style);

            Rectangle box = new Rectangle(first.getCenter());
            box.add(last.getCenter());
            sb.append(' ').append(PointUtil.toString(GeoUtil.centerOf(box)));
        }

        sb.append('}');

        return sb.toString();
    }

    //-----------------//
    // upgradeOldStuff //
    //-----------------//
    /**
     * Temporary method to upgrade from oldStaffBarlines to staffBarlines.
     *
     * @return true if really upgraded
     */
    @Deprecated
    public boolean upgradeOldStuff ()
    {
        if (oldStaffBarlines != null) {
            if (!oldStaffBarlines.isEmpty()) {
                for (OldStaffBarline oldSb : oldStaffBarlines) {
                    staffBarlines.add(new StaffBarlineInter(oldSb.bars));
                }
            }

            oldStaffBarlines = null;

            return true;
        }

        return false;
    }

    /**
     * Barline style.
     * <p>
     * Identical to (or subset of) MusicXML BarStyle, to avoid strict dependency on MusicXML with
     * the addition of LIGHT_HEAVY_LIGHT to handle back-to-back configuration.
     */
    public static enum Style
    {
        REGULAR,
        DOTTED,
        DASHED,
        HEAVY,
        LIGHT_LIGHT,
        LIGHT_HEAVY,
        HEAVY_LIGHT,
        HEAVY_HEAVY,
        TICK,
        SHORT,
        NONE,

        /** LIGHT_HEAVY_LIGHT is not part of MusicXML. */
        LIGHT_HEAVY_LIGHT;
    }
}
