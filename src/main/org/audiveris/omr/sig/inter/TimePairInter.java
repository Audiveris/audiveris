//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T i m e P a i r I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.VerticalSide;

import static org.audiveris.omr.util.VerticalSide.*;

import java.awt.Rectangle;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code TimePairInter} is a time signature composed of two halves (num & den).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "time-pair")
@XmlAccessorType(XmlAccessType.NONE)
public class TimePairInter
        extends AbstractTimeInter
        implements InterEnsemble
{
    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------
    //
    /**
     * Signature halves: numerator then denominator.
     * This is deprecated, replaced by the use of containment relation.
     */
    @XmlElement(name = "number")
    @Deprecated
    private List<TimeNumberInter> oldMembers;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * (Private) constructor.
     *
     * @param timeRational
     * @param grade
     */
    private TimePairInter (TimeRational timeRational,
                           double grade)
    {
        super(null, null, timeRational, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private TimePairInter ()
    {
        super(null, null, 0);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //--------//
    // create //
    //--------//
    /**
     * Create a {@code TimePairInter} object from its two halves.
     *
     * @param num numerator: non-null, registered in sig
     * @param den denominator: non-null, registered in sig
     * @return the created instance
     */
    public static TimePairInter create (TimeNumberInter num,
                                        TimeNumberInter den)
    {
        double grade = 0.5 * (num.getGrade() + den.getGrade());
        TimePairInter pair = new TimePairInter(null, grade);
        SIGraph sig = num.getSig();
        sig.addVertex(pair);
        pair.addMember(num);
        pair.addMember(den);

        return pair;
    }

    //-----------//
    // addMember //
    //-----------//
    @Override
    public void addMember (Inter member)
    {
        if (!(member instanceof TimeNumberInter)) {
            throw new IllegalArgumentException("Only TimeNumberInter can be added to TimePair");
        }

        if (getMembers().size() >= 2) {
            throw new IllegalStateException("TimePairInter is already full");
        }

        EnsembleHelper.addMember(this, member);
    }

    @Override
    public void memberAdded (Inter member)
    {
        reset();
    }

    @Override
    public void memberRemoved (Inter member)
    {
        reset();
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            bounds = Entities.getBounds(getMembers());
        }

        return bounds;
    }

    //--------//
    // getDen //
    //--------//
    /**
     * Convenient method to report denominator member.
     *
     * @return the denominator
     */
    public TimeNumberInter getDen ()
    {
        return getMember(BOTTOM);
    }

    //-----------//
    // getMember //
    //-----------//
    /**
     * Convenient method to report the pair member on desired side.
     *
     * @param side TOP or BOTTOM
     * @return the numerator or denominator, or null
     */
    public TimeNumberInter getMember (VerticalSide side)
    {
        final List<Inter> members = getMembers();

        switch (members.size()) {
        case 2:
            return (TimeNumberInter) members.get((side == TOP) ? 0 : 1);

        case 1:

            if (staff != null) {
                final TimeNumberInter tni = (TimeNumberInter) members.get(0);
                final double pp = staff.pitchPositionOf(tni.getCenter());

                if (((side == TOP) && (pp < 0)) || ((side == BOTTOM) && (pp > 0))) {
                    return tni;
                }
            }

        default:
        case 0:
            return null;
        }
    }

    //------------//
    // getMembers //
    //------------//
    /**
     * Report the sequence of pair members: numerator then denominator.
     *
     * @return sequence of pair half members
     */
    @Override
    public List<Inter> getMembers ()
    {
        return EnsembleHelper.getMembers(this, Inters.byCenterOrdinate);
    }

    //--------//
    // getNum //
    //--------//
    /**
     * Convenient method to report the numerator member.
     *
     * @return the numerator
     */
    public TimeNumberInter getNum ()
    {
        return getMember(TOP);
    }

    //-----------------//
    // getSymbolBounds //
    //-----------------//
    /**
     * {@inheritDoc}.
     * <p>
     * This implementation uses two rectangles, one for numerator and one for denominator.
     *
     * @param interline scaling factor
     * @return the symbol bounds
     */
    @Override
    public Rectangle getSymbolBounds (int interline)
    {
        Rectangle rect = getNum().getSymbolBounds(interline);
        rect.add(getDen().getSymbolBounds(interline));

        return rect;
    }

    //-----------------//
    // getTimeRational //
    //-----------------//
    /**
     * Report the timeRational value, lazily computed.
     *
     * @return the timeRational
     */
    @Override
    public TimeRational getTimeRational ()
    {
        if (timeRational == null) {
            final List<Inter> members = getMembers();

            if (members.size() == 2) {
                timeRational = new TimeRational(
                        ((TimeNumberInter) members.get(0)).getValue(),
                        ((TimeNumberInter) members.get(1)).getValue());
            }
        }

        return timeRational;
    }

    //----------------//
    // linkOldMembers //
    //----------------//
    @Override
    public void linkOldMembers ()
    {
        EnsembleHelper.linkOldMembers(this, oldMembers);
        oldMembers = null;
    }

    //--------------//
    // removeMember //
    //--------------//
    @Override
    public void removeMember (Inter member)
    {
        if (!(member instanceof TimeNumberInter)) {
            throw new IllegalArgumentException("Only TimeNumberInter can be removed from TimePair");
        }

        EnsembleHelper.removeMember(this, member);
    }

    //-----------//
    // replicate //
    //-----------//
    @Override
    public TimePairInter replicate (Staff targetStaff)
    {
        TimePairInter inter = new TimePairInter(getTimeRational(), 0);
        inter.setStaff(targetStaff);

        return inter;
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "TIME_SIG_" + getTimeRational();
    }

    //-------//
    // reset //
    //-------//
    private void reset ()
    {
        bounds = null;
        timeRational = null;
    }
}
