//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     R e l a t i o n P a i r                                    //
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

import org.audiveris.omr.sig.inter.Inter;

/**
 * Class {@code RelationPair} is a mutable pair composed of source inter and target inter.
 * <p>
 * It can be used for method input/output.
 *
 * @author Hervé Bitteur
 */
public class RelationPair
{

    public Inter source;

    public Inter target;

    public RelationPair (Inter source,
                         Inter target)
    {
        this.source = source;
        this.target = target;
    }

    @Override
    public String toString ()
    {
        return new StringBuilder("Pair{").append(source).append(',').append(target).append('}')
                .toString();
    }
}
