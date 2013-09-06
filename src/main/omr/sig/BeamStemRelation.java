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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;

/**
 * Class {@code BeamStemRelation}
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
    
    @Override
    public String getName()
    {
        return "Beam-Stem";
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

    @Override
    protected double getXWeight ()
    {
        return constants.xWeight.getValue();
    }

    @Override
    protected double getYWeight ()
    {
        return constants.yWeight.getValue();
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

        final Constant.Double xWeight = new Constant.Double(
                "weight",
                4,
                "Weight assigned to horizontal Gap");

        final Constant.Double yWeight = new Constant.Double(
                "weight",
                1,
                "Weight assigned to vertical Gap");

    }
}
