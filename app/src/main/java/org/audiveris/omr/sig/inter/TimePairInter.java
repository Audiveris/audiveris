//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T i m e P a i r I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.NumDenSymbol;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.BOTTOM;
import static org.audiveris.omr.util.VerticalSide.TOP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>TimePairInter</code> is a time signature composed of two halves
 * (num and den).
 * <p>
 * It is an Inter ensemble composed of 2 {@link TimeNumberInter} instances, one for the top,
 * and one for the bottom.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "time-pair")
@XmlAccessorType(XmlAccessType.NONE)
public class TimePairInter
        extends AbstractTimeInter
        implements InterEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TimePairInter.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private TimePairInter ()
    {
        super((Glyph) null, null, 0.0);
    }

    /**
     * Creates a new <code>TimePairInter</code> object.
     *
     * @param timeRational num/den literal value
     * @param grade        quality grade
     */
    public TimePairInter (TimeRational timeRational,
                          Double grade)
    {
        super(null, null, timeRational, grade);
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

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        setAbnormal(getMembers().size() != 2);

        return isAbnormal();
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

        return new Rectangle(bounds);
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

        return switch (members.size()) {
            case 2 -> (TimeNumberInter) members.get((side == TOP) ? 0 : 1);

            case 1 -> {
                if ((staff != null) && !staff.isTablature()) {
                    final TimeNumberInter tni = (TimeNumberInter) members.get(0);
                    final double pp = staff.pitchPositionOf(tni.getCenter());

                    if (((side == TOP) && (pp < 0)) || ((side == BOTTOM) && (pp > 0))) {
                        yield tni;
                    }
                }
                yield null;
            }

            case 0 -> null;
            default -> null;
        };
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

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "TIME_SIG:" + getTimeRational();
    }

    //----------------//
    // getShapeSymbol //
    //----------------//
    @Override
    public ShapeSymbol getShapeSymbol (MusicFamily family)
    {
        final TimeNumberInter num = getNum();
        final TimeNumberInter den = getDen();

        if (num == null || den == null) {
            return null;
        }

        return new NumDenSymbol(null, family, num.getValue(), den.getValue());
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
                        ((AbstractNumberInter) members.get(0)).getValue(),
                        ((AbstractNumberInter) members.get(1)).getValue());
            }
        }

        return timeRational;
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        bounds = null;
        timeRational = null;

        checkAbnormal();
        setGrade(isAbnormal() ? 0 : EnsembleHelper.computeMeanContextualGrade(this));
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
        TimePairInter inter = new TimePairInter(getTimeRational(), getGrade());
        inter.setStaff(targetStaff);

        return inter;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------//
    // createAdded //
    //-------------//
    /**
     * Create and add a <code>TimePairInter</code> object from its two halves.
     *
     * @param num numerator: non-null, registered in sig
     * @param den denominator: non-null, registered in sig
     * @return the created instance, already added to sig
     */
    public static TimePairInter createAdded (TimeNumberInter num,
                                             TimeNumberInter den)
    {
        final TimePairInter pair = new TimePairInter(null, null);
        final SIGraph sig = num.getSig();
        sig.addVertex(pair);
        pair.addMember(num);
        pair.addMember(den);

        // Safer
        pair.getBounds();
        pair.setStaff(num.getStaff());

        if (pair.isVip()) {
            logger.info("VIP created {} from num:{} den:{}", pair, num, den);
        }

        return pair;
    }
}
