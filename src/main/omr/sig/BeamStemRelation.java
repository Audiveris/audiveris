//----------------------------------------------------------------------------//
//                                                                            //
//                       B e a m S t e m R e l a t i o n                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;

/**
 * Class {@code BeamStemRelation} implements the geographic link
 * between a beam (or beam hook) and a stem.
 *
 * @author Hervé Bitteur
 */
public class BeamStemRelation
        extends AbstractConnection
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            BeamStemRelation.class);

    //~ Instance fields --------------------------------------------------------
    /** Which portion of beam is used?. */
    private BeamPortion beamPortion;

    /** Which part of stem is used?. */
    private StemPortion stemPortion;

    /** Precise point where the stem crosses the beam. */
    private Point2D crossPoint;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new BeamStemRelation object.
     */
    public BeamStemRelation ()
    {
    }

    //~ Methods ----------------------------------------------------------------
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
     * @return the beamPortion
     */
    public BeamPortion getBeamPortion ()
    {
        return beamPortion;
    }

    /**
     * @return the crossPoint
     */
    public Point2D getCrossPoint ()
    {
        return crossPoint;
    }

    @Override
    public GradeImpacts getImpacts ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName ()
    {
        return "Beam-Stem";
    }

    /**
     * @return the stem Portion
     */
    public StemPortion getStemPortion ()
    {
        return stemPortion;
    }

    /**
     * @param beamPortion the beamPortion to set
     */
    public void setBeamPortion (BeamPortion beamPortion)
    {
        this.beamPortion = beamPortion;
    }

    /**
     * @param crossPoint the crossPoint to set
     */
    public void setCrossPoint (Point2D crossPoint)
    {
        this.crossPoint = crossPoint;
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

    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" ")
                .append(beamPortion);

        if (crossPoint != null) {
            sb.append(
                    String.format(
                            "[x:%.0f,y:%.0f]",
                            crossPoint.getX(),
                            crossPoint.getY()));
        }

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

        final Constant.Ratio minGrade = new Constant.Ratio(
                0.1,
                "Minimum interpretation grade");

        final Constant.Ratio supportCoeff = new Constant.Ratio(
                5,
                "Value for coeff in support formula");

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
