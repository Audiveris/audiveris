//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S u p p o r t                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import omr.sig.GradeImpacts;

/**
 * Interface {@code Support} is a relation between interpretation instances that support
 * one another.
 * <p>
 * Typical example is a mutual support between a stem and a note head, or between a stem and a beam.
 *
 * @author Hervé Bitteur
 */
public interface Support
        extends Relation
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report details about the final relation grade
     *
     * @return the grade details
     */
    GradeImpacts getImpacts ();

    /**
     * Report the support ratio for source inter
     *
     * @return support ratio for source (value is always >= 1)
     */
    double getSourceRatio ();

    /**
     * Report the support ratio for target inter
     *
     * @return support ratio for target (value is always >= 1)
     */
    double getTargetRatio ();

    /**
     * Assign details about the relation grade
     *
     * @param impacts the grade impacts
     */
    void setImpacts (GradeImpacts impacts);
}
