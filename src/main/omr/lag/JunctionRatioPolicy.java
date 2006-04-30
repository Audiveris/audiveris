//-----------------------------------------------------------------------//
//                                                                       //
//                 J u n c t i o n R a t i o P o l i c y                 //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.lag;

/**
 * Class <code>JunctionRatioPolicy</code> defined a junction policy based
 * on the ratio between the length of the candidate run and the mean length
 * of the section runs so far.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class JunctionRatioPolicy
    extends JunctionPolicy
{
    //~ Instance variables ------------------------------------------------

    /**
     * Maximum value acceptable for height ratio, for a ratio criteria
     */
    private final double maxHeightRatio;

    /**
     * Minimum value acceptable for height ratio, for a ratio criteria
     */
    private final double minHeightRatio;

    //~ Constructors ------------------------------------------------------

    //---------------------//
    // JunctionRatioPolicy //
    //---------------------//
    /**
     * Creates a policy based on ratio of run length versus mean length of
     * section runs
     *
     * @param maxHeightRatio maximum difference ratio to continue the
     *                       current section
     */
    public JunctionRatioPolicy (double maxHeightRatio)
    {
        this.maxHeightRatio = maxHeightRatio;
        this.minHeightRatio = 1f / maxHeightRatio;
    }

    //~ Methods -----------------------------------------------------------

    //---------------//
    // consistentRun //
    //---------------//
    public boolean consistentRun (Run run,
                                  Section section)
    {
        // Check is based on ratio of lengths
        final float ratio = (float) run.getLength() / (float) section
                .getMeanRunLength();

        return (ratio <= maxHeightRatio) && (ratio >= minHeightRatio);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Return a descriptive string of this junction policy
     *
     * @return a descriptive string
     */
    @Override
    public String toString ()
    {
        return "{JunctionRatioPolicy" + " maxHeightRatio=" + maxHeightRatio
               + " minHeightRatio=" + minHeightRatio + "}";
    }
}
