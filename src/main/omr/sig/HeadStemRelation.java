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

import omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code HeadStemRelation}
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

    //----------//
    // getRatio //
    //----------//
    @Override
    public Double getRatio ()
    {
        return 1.0 + (10.0 * grade);
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
