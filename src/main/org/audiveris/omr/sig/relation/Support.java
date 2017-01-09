//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S u p p o r t                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.sig.GradeImpacts;

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
