//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T i m e P a i r I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.score.TimeRational;

import omr.sheet.Staff;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;

/**
 * Class {@code TimePairInter} is a time signature composed of two halves.
 *
 * @author Hervé Bitteur
 */
public class TimePairInter
        extends TimeInter
        implements InterEnsemble
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Numerator. */
    private final TimeNumberInter num;

    /** Denominator. */
    private final TimeNumberInter den;

    private final List<? extends Inter> members;

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
                           Rectangle box,
                           TimeRational timeRational,
                           double grade)
    {
        super(null, box, timeRational, grade);
        this.num = num;
        this.den = den;
        members = Arrays.asList(num, den);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * Create a {@code TimePairInter} object from its 2 numbers.
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

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    /**
     * @return the den
     */
    public TimeNumberInter getDen ()
    {
        return den;
    }

    //------------//
    // getMembers //
    //------------//
    @Override
    public List<? extends Inter> getMembers ()
    {
        return members;
    }

    /**
     * @return the num
     */
    public TimeNumberInter getNum ()
    {
        return num;
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
        Rectangle rect = num.getSymbolBounds(interline);
        rect.add(den.getSymbolBounds(interline));

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
