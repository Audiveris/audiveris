//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             J u n c t i o n R a t i o P o l i c y                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.run.Run;

/**
 * Class {@code JunctionRatioPolicy} defines a junction policy based on the ratio
 * between the length of the candidate run and the mean length of the section runs so far.
 *
 * @author Hervé Bitteur
 */
public class JunctionRatioPolicy
        implements JunctionPolicy
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    public static final JunctionRatioPolicy DEFAULT = new JunctionRatioPolicy();

    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * Maximum value acceptable for length ratio.
     */
    private final double maxLengthRatio;

    /**
     * Minimum value acceptable for length ratio.
     */
    private final double minLengthRatio;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a policy based on default length ratio.
     */
    public JunctionRatioPolicy ()
    {
        this(constants.maxLengthRatio.getValue());
    }

    /**
     * Creates a policy based on ratio of run length versus mean length of section runs.
     *
     * @param maxLengthRatio maximum difference ratio to continue the
     *                       current section
     */
    public JunctionRatioPolicy (double maxLengthRatio)
    {
        this.maxLengthRatio = maxLengthRatio;
        this.minLengthRatio = 1f / maxLengthRatio;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // consistentRun //
    //---------------//
    /**
     * Check whether the Run is consistent with the provided Section, according to this
     * junction policy, based on run length and mean section run length.
     *
     * @param run     the Run candidate
     * @param section the potentially hosting Section
     * @return true if consistent, false otherwise
     */
    @Override
    public boolean consistentRun (Run run,
                                  Section section)
    {
        // Check is based on ratio of lengths
        final double ratio = (double) run.getLength() / section.getMeanRunLength();

        return (minLengthRatio <= ratio) && (ratio <= maxLengthRatio);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{JunctionRatioPolicy" + " maxLengthRatio=" + maxLengthRatio + " minLengthRatio="
               + minLengthRatio + "}";
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio maxLengthRatio = new Constant.Ratio(
                1.25,
                "Maximum ratio in length for a run to be combined with an existing section");
    }
}
