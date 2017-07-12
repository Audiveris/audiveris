//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    W r a p p e d D o u b l e                                   //
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
package org.audiveris.omr.util;

/**
 * Class {@code WrappedDouble} is a wrapper around a Double, with the ability to set
 * an initial value.
 *
 * @author Hervé Bitteur
 */
public class WrappedDouble
        extends Wrapper<Double>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new WrappedDouble object.
     *
     * @param value the initial value
     */
    public WrappedDouble (Double value)
    {
        super(value);
    }
}
