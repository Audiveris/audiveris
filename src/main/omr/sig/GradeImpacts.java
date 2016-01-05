//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G r a d e I m p a c t s                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code GradeImpacts} defines data that impact a resulting grade value.
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(AbstractImpacts.Adapter.class)
public interface GradeImpacts
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report a string about impacts details
     *
     * @return string of details
     */
    String getDump ();

    /**
     * Retrieve a global grade value from detailed impacts.
     *
     * @return the computed grade in range 0 .. 1
     */
    double getGrade ();

    /**
     * Report the value of the grade impact corresponding to index
     *
     * @param index the index of desired impact
     * @return the impact value
     */
    double getImpact (int index);

    /**
     * Report the number of individual grade impacts.
     *
     * @return the count of impacts
     */
    int getImpactCount ();

    /**
     * Report the reduction ratio to be applied on intrinsic grade
     *
     * @return the reduction ratio to be applied
     */
    double getIntrinsicRatio ();

    /**
     * Report the name of the grade impact corresponding to index
     *
     * @param index the index of desired impact
     * @return the impact name
     */
    String getName (int index);

    /**
     * Report the weight of the grade impact corresponding to index
     *
     * @param index the index of desired impact
     * @return the impact weight
     */
    double getWeight (int index);
}
