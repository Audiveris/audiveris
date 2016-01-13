//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 B e a m S t e m R e l a t i o n                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Scale;

import omr.sig.inter.Inter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BeamStemRelation} implements the geographic link between a beam
 * (or beam hook) and a stem.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "beam-stem")
public class BeamStemRelation
        extends AbstractStemConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            BeamStemRelation.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Which portion of beam is used?. */
    @XmlAttribute(name = "beam-portion")
    private BeamPortion beamPortion;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BeamStemRelation} object.
     */
    public BeamStemRelation ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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

    //----------------//
    // getBeamPortion //
    //----------------//
    /**
     * @return the beamPortion
     */
    public BeamPortion getBeamPortion ()
    {
        return beamPortion;
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

        return (extensionPoint.getY() < midStem) ? StemPortion.STEM_TOP : StemPortion.STEM_BOTTOM;
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
        return constants.beamSupportCoeff.getValue();
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return constants.stemSupportCoeff.getValue();
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

        private final Constant.Ratio beamSupportCoeff = new Constant.Ratio(
                4,
                "Value for source (beam) coeff in support formula");

        private final Constant.Ratio stemSupportCoeff = new Constant.Ratio(
                2,
                "Value for target (stem) coeff in support formula");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                0.8,
                "Maximum vertical gap between stem & beam");

        private final Scale.Fraction xInGapMax = new Scale.Fraction(
                0.5,
                "Maximum horizontal overlap between stem & beam");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
                0.1,
                "Maximum horizontal gap between stem & beam");
    }
}
