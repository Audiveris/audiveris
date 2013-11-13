//----------------------------------------------------------------------------//
//                                                                            //
//                        H e a d S t e m R e l a t i o n                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Scale;

import omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code HeadStemRelation} represents the relation support
 * between a head and a stem.
 *
 * @author Hervé Bitteur
 */
public class HeadStemRelation
        extends AbstractConnection
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            HeadStemRelation.class);

    //~ Instance fields --------------------------------------------------------
    /** Which side of head is used?. */
    private HorizontalSide headSide;

    /** Which part of stem is used?. */
    private StemPortion stemPortion;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new HeadStemRelation object.
     */
    public HeadStemRelation ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    /**
     * @return the headSide
     */
    public HorizontalSide getHeadSide ()
    {
        return headSide;
    }

    @Override
    public String getName ()
    {
        return "Head-Stem";
    }

    /**
     * @return the stem Portion
     */
    public StemPortion getStemPortion ()
    {
        return stemPortion;
    }

    //------------------//
    // getXInGapMaximum //
    //------------------//
    public static Scale.Fraction getXInGapMaximum ()
    {
        return constants.xInGapMax;
    }

    //-------------------//
    // getXOutGapMaximum //
    //-------------------//
    public static Scale.Fraction getXOutGapMaximum ()
    {
        return constants.xOutGapMax;
    }

    //----------------//
    // getYGapMaximum //
    //----------------//
    public static Scale.Fraction getYGapMaximum ()
    {
        return constants.yGapMax;
    }

    /**
     * @param headSide the headSide to set
     */
    public void setHeadSide (HorizontalSide headSide)
    {
        this.headSide = headSide;
    }

    /**
     * @param stemPortion the stem portion to set
     */
    public void setStemPortion (StemPortion stemPortion)
    {
        this.stemPortion = stemPortion;
    }

    //-----------------//
    // getSupportCoeff //
    //-----------------//
    @Override
    protected double getSupportCoeff ()
    {
        return constants.supportCoeff.getValue();
    }

    //--------------//
    // getXInGapMax //
    //--------------//
    @Override
    protected Scale.Fraction getXInGapMax ()
    {
        return getXInGapMaximum();
    }

    //---------------//
    // getXOutGapMax //
    //---------------//
    @Override
    protected Scale.Fraction getXOutGapMax ()
    {
        return getXOutGapMaximum();
    }

    //------------//
    // getYGapMax //
    //------------//
    @Override
    protected Scale.Fraction getYGapMax ()
    {
        return getYGapMaximum();
    }

    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" ")
                .append(headSide);

        sb.append(",")
                .append(stemPortion);

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Ratio supportCoeff = new Constant.Ratio(
                5,
                "Value for coeff in support formula");

        final Scale.Fraction yGapMax = new Scale.Fraction(
                0.8,
                "Maximum vertical gap between stem & head");

        final Scale.Fraction xInGapMax = new Scale.Fraction(
                0.25,
                "Maximum horizontal overlap between stem & head");

        final Scale.Fraction xOutGapMax = new Scale.Fraction(
                0.2,
                "Maximum horizontal gap between stem & head");

    }
}
