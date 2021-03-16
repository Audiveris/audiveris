//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S u p p o r t I m p a c t s                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
 * Class {@code SupportImpacts} handles impacts for a supporting relation.
 *
 * @author Hervé Bitteur
 */
public class SupportImpacts
        extends GradeImpacts
{

    /**
     * Creates a new RelationImpacts object.
     *
     * @param names   array of names
     * @param weights array of weights
     */
    public SupportImpacts (String[] names,
                           double[] weights)
    {
        super(names, weights);
    }

    //-------------------//
    // getIntrinsicRatio //
    //-------------------//
    /**
     * A relation is not supposed to have a contextual grade, so there is no point to
     * leave room for it.
     *
     * @return 1
     */
    @Override
    public double getIntrinsicRatio ()
    {
        return 1;
    }
}
