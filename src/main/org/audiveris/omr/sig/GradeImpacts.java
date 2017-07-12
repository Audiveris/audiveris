//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G r a d e I m p a c t s                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sig;

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
