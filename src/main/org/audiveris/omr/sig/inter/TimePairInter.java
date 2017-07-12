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
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.VerticalSide;

import java.awt.Rectangle;
import java.util.ArrayList;
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
    /** Signature halves: numerator then denominator. */
    @XmlElement(name = "number")
    private final List<TimeNumberInter> members = new ArrayList<TimeNumberInter>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * (Private) constructor.
     *
     * @param num
     * @param den
     * @param timeRational
     * @param grade
     */
    private TimePairInter (TimeNumberInter num,
                           TimeNumberInter den,
                           Rectangle bounds,
                           TimeRational timeRational,
                           double grade)
    {
        super(null, bounds, timeRational, grade);
        members.add(num);
        members.add(den);
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
     * @param num numerator
     * @param den denominator
     * @return the created instance
     */
    public static TimePairInter create (TimeNumberInter num,
                                        TimeNumberInter den)
    {
        Rectangle box = num.getBounds();
        box.add(den.getBounds());

        TimeRational timeRational = new TimeRational(num.getValue(), den.getValue());
        double grade = 0.5 * (num.getGrade() + den.getGrade());
        TimePairInter pair = new TimePairInter(num, den, box, timeRational, grade);

        ///pair.setContextualGrade(0.5 * (num.getContextualGrade() + den.getContextualGrade()));
        return pair;
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
        return members.get(1);
    }

    //-----------//
    // getMember //
    //-----------//
    /**
     * Convenient method to report the pair member on desired side.
     *
     * @param side TOP or BOTTOM
     * @return the numerator
     */
    public TimeNumberInter getMember (VerticalSide side)
    {
        return members.get((side == VerticalSide.TOP) ? 0 : 1);
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
    public List<? extends Inter> getMembers ()
    {
        return members;
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
        return members.get(0);
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

    //-----------//
    // replicate //
    //-----------//
    @Override
    public TimePairInter replicate (Staff targetStaff)
    {
        TimePairInter inter = new TimePairInter(null, null, null, timeRational, 0);
        inter.setStaff(targetStaff);

        return inter;
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "TIME_SIG_" + timeRational;
    }
}
