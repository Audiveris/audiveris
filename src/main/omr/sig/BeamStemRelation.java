//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 B e a m S t e m R e l a t i o n                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Line2D;

/**
 * Class {@code BeamStemRelation} implements the geographic link between a beam
 * (or beam hook) and a stem.
 *
 * @author Hervé Bitteur
 */
public class BeamStemRelation
        extends StemConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BeamStemRelation.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Which portion of beam is used?. */
    private BeamPortion beamPortion;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BeamStemRelation object.
     */
    public BeamStemRelation ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * @return the beamPortion
     */
    public BeamPortion getBeamPortion ()
    {
        return beamPortion;
    }

    @Override
    public String getName ()
    {
        return "Beam-Stem";
    }

    //----------------//
    // getStemPortion //
    //----------------//
    @Override
    public StemPortion getStemPortion (Inter source,
                                       Line2D stemLine,
                                       Scale scale)
    {
        double midStem = (stemLine.getY1() + stemLine.getY2()) / 2;

        return (anchorPoint.getY() < midStem) ? StemPortion.STEM_TOP : StemPortion.STEM_BOTTOM;
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
     * @param beamPortion the beamPortion to set
     */
    public void setBeamPortion (BeamPortion beamPortion)
    {
        this.beamPortion = beamPortion;
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    @Override
    protected double getSourceCoeff ()
    {
        return constants.sourceCoeff.getValue();
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return constants.targetCoeff.getValue();
    }

    //--------------//
    // getXInGapMax //
    //--------------//
    @Override
    protected Scale.Fraction getXInGapMax ()
    {
        throw new UnsupportedOperationException("Not supported.");
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

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" ").append(beamPortion);

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Ratio minGrade = new Constant.Ratio(0.1, "Minimum interpretation grade");

        final Constant.Ratio sourceCoeff = new Constant.Ratio(
                4, //5,
                "Value for source (beam) coeff in support formula");

        final Constant.Ratio targetCoeff = new Constant.Ratio(
                2, //5,
                "Value for target (stem) coeff in support formula");

        final Scale.Fraction yGapMax = new Scale.Fraction(
                1.0,
                "Maximum vertical gap between stem & beam");

        final Scale.Fraction xInGapMax = new Scale.Fraction(
                0.5,
                "Maximum horizontal overlap between stem & beam");

        final Scale.Fraction xOutGapMax = new Scale.Fraction(
                0.1,
                "Maximum horizontal gap between stem & beam");
    }
}
