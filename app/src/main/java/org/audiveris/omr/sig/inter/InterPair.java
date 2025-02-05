//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I n t e r P a i r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sig.inter;

/**
 * Class <code>InterPair</code> is a mutable pair composed of source inter and target inter.
 * <p>
 * It can be used for method input/output.
 *
 * @author Hervé Bitteur
 */
public class InterPair
{
    //~ Instance fields ----------------------------------------------------------------------------

    public Inter source;

    public Inter target;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>InterPair</code> object.
     *
     * @param source source inter
     * @param target target inter
     */
    public InterPair (Inter source,
                      Inter target)
    {
        this.source = source;
        this.target = target;
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public String toString ()
    {
        return new StringBuilder("Pair{").append(source).append(',').append(target).append('}')
                .toString();
    }
}
