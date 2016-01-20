//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S t a f f B a r l i n e                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.Shape;

import omr.math.PointUtil;

import omr.sheet.PartBarline.Style;
import static omr.sheet.PartBarline.Style.*;

import omr.sig.SIGraph;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.CodaInter;
import omr.sig.inter.FermataInter;
import omr.sig.inter.Inter;
import omr.sig.inter.SegnoInter;
import omr.sig.relation.CodaBarRelation;
import omr.sig.relation.FermataBarRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.RepeatDotBarRelation;
import omr.sig.relation.SegnoBarRelation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Class {@code StaffBarline} represents a logical barline for one staff only.
 * <p>
 * A {@link PartBarline} is a logical barline for one part, that is made of one
 * {@code StaffBarline} for each staff in the part.
 * <p>
 * Such logical barline is composed of a horizontal sequence of one or several {@link BarlineInter}
 * instances, called 'bars' in this class.
 * <p>
 * Barline-related entities such as repeat dot(s), ending(s), fermata(s), segno or coda are not
 * contained by this class, but implemented as separate inters, linked to a bar by proper relation.
 * <p>
 * Reference abscissa (quoting Michael Good): the best approximation that's application-independent
 * would probably be to use the center of the barline. In back-to-back barlines that would be the
 * center of the thick barline. For double barlines like light-light or light-heavy this would be
 * the center of the rightmost barline.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "staff-barline")
public class StaffBarline
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Abscissa-ordered sequence of physical barlines. */
    @XmlList
    @XmlValue
    @XmlIDREF
    private final ArrayList<BarlineInter> bars = new ArrayList<BarlineInter>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StaffBarline} object.
     */
    public StaffBarline ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // addBar //
    //--------//
    /**
     * Insert a physical barline inter as part of this logical barline.
     *
     * @param inter the barline inter to insert
     */
    public void addBar (BarlineInter inter)
    {
        if (!bars.contains(inter)) {
            bars.add(inter);
            Collections.sort(bars, Inter.byFullAbscissa);
        }
    }

    //---------//
    // getBars //
    //---------//
    public List<BarlineInter> getBars ()
    {
        return Collections.unmodifiableList(bars);
    }

    //-----------//
    // getCenter //
    //-----------//
    public Point getCenter ()
    {
        switch (getStyle()) {
        case REGULAR:
        case HEAVY:
        case HEAVY_LIGHT:
            return getLeftBar().getCenter();

        case LIGHT_LIGHT:
        case LIGHT_HEAVY:
        case HEAVY_HEAVY:
            return getRightBar().getCenter();

        case NONE:
        default:
            return null;
        }
    }

    //---------//
    // getCoda //
    //---------//
    /**
     * Convenient method to report the related coda, if any
     *
     * @return coda inter or null
     */
    public CodaInter getCoda ()
    {
        return (CodaInter) getRelated(CodaBarRelation.class);
    }

    //-------------//
    // getFermatas //
    //-------------//
    /**
     * Convenient method to report related fermata signs, if any
     *
     * @return set of (maximum two) fermata inters, perhaps empty but not null
     */
    public Set<FermataInter> getFermatas ()
    {
        Set<FermataInter> fermatas = null;

        for (BarlineInter bar : bars) {
            SIGraph sig = bar.getSig();

            for (Relation rel : sig.getRelations(bar, FermataBarRelation.class)) {
                if (fermatas == null) {
                    fermatas = new HashSet<FermataInter>();
                }

                fermatas.add((FermataInter) sig.getOppositeInter(bar, rel));
            }
        }

        return (fermatas != null) ? fermatas : Collections.EMPTY_SET;
    }

    //------------//
    // getLeftBar //
    //------------//
    public BarlineInter getLeftBar ()
    {
        if (bars.isEmpty()) {
            return null;
        }

        return bars.get(0);
    }

    //----------//
    // getLeftX //
    //----------//
    /**
     * Report the center abscissa of the left bar
     *
     * @return abscissa of the left side
     */
    public int getLeftX ()
    {
        final BarlineInter leftBar = getLeftBar();

        if (leftBar != null) {
            return leftBar.getCenter().x;
        }

        throw new IllegalStateException("No abscissa computable for " + this);
    }

    //------------//
    // getRelated //
    //------------//
    /**
     * Report the first barline-related entity found, if any
     *
     * @param relationClass the desired class for bar-entity relation
     * @return the first related entity found or null
     */
    public Inter getRelated (Class<?> relationClass)
    {
        for (BarlineInter bar : bars) {
            SIGraph sig = bar.getSig();

            for (Relation rel : sig.getRelations(bar, relationClass)) {
                return sig.getOppositeInter(bar, rel);
            }
        }

        return null;
    }

    //-------------//
    // getRightBar //
    //-------------//
    public BarlineInter getRightBar ()
    {
        if (bars.isEmpty()) {
            return null;
        }

        return bars.get(bars.size() - 1);
    }

    //-----------//
    // getRightX //
    //-----------//
    /**
     * Report the center abscissa of the right bar
     *
     * @return abscissa of the right side
     */
    public int getRightX ()
    {
        final BarlineInter rightBar = getRightBar();

        if (rightBar != null) {
            return rightBar.getCenter().x;
        }

        throw new IllegalStateException("No abscissa computable for " + this);
    }

    //----------//
    // getSegno //
    //----------//
    /**
     * Convenient method to report the related segno, if any
     *
     * @return segno inter or null
     */
    public SegnoInter getSegno ()
    {
        return (SegnoInter) getRelated(SegnoBarRelation.class);
    }

    //----------//
    // getStyle //
    //----------//
    public PartBarline.Style getStyle ()
    {
        switch (getBarCount()) {
        case 0:
            return NONE;

        case 1:
            return (getLeftBar().getShape() == Shape.THIN_BARLINE) ? REGULAR : HEAVY;

        case 2: {
            if (getLeftBar().getShape() == Shape.THIN_BARLINE) {
                return (getRightBar().getShape() == Shape.THIN_BARLINE) ? LIGHT_LIGHT : LIGHT_HEAVY;
            } else {
                return (getRightBar().getShape() == Shape.THIN_BARLINE) ? HEAVY_LIGHT : HEAVY_HEAVY;
            }
        }

        default:
            return null;
        }
    }

    //---------------//
    // hasDotsOnLeft //
    //---------------//
    public boolean hasDotsOnLeft ()
    {
        final Point center = getCenter();

        for (BarlineInter bar : bars) {
            SIGraph sig = bar.getSig();

            for (Relation rel : sig.getRelations(bar, RepeatDotBarRelation.class)) {
                Inter dot = sig.getOppositeInter(bar, rel);

                if (dot.getCenter().x < center.x) {
                    return true;
                }
            }
        }

        return false;
    }

    //----------------//
    // hasDotsOnRight //
    //----------------//
    public boolean hasDotsOnRight ()
    {
        final Point center = getCenter();

        for (BarlineInter bar : bars) {
            SIGraph sig = bar.getSig();

            for (Relation rel : sig.getRelations(bar, RepeatDotBarRelation.class)) {
                Inter dot = sig.getOppositeInter(bar, rel);

                if (dot.getCenter().x > center.x) {
                    return true;
                }
            }
        }

        return false;
    }

    //--------------//
    // isLeftRepeat //
    //--------------//
    public boolean isLeftRepeat ()
    {
        return (getStyle() == HEAVY_LIGHT) && hasDotsOnRight();
    }

    //---------------//
    // isRightRepeat //
    //---------------//
    public boolean isRightRepeat ()
    {
        return (getStyle() == LIGHT_HEAVY) && hasDotsOnLeft();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{StaffBarline");
        Style style = getStyle();
        sb.append(" ").append(style);
        sb.append(" ").append(PointUtil.toString(getCenter()));
        sb.append("}");

        return sb.toString();
    }

    //-------------//
    // getBarCount //
    //-------------//
    private int getBarCount ()
    {
        return bars.size();
    }
}
