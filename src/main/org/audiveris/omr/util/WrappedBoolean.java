//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  W r a p p e d B o o l e a n                                   //
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
package org.audiveris.omr.util;

/**
 * Class {@code WrappedBoolean} is a specific wrapper around a boolean,
 * meant to carry an output boolean as method parameter.
 *
 * @author Hervé Bitteur
 */
public class WrappedBoolean
        extends Wrapper<Boolean>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new WrappedBoolean object.
     *
     * @param value the initial boolean value
     */
    public WrappedBoolean (boolean value)
    {
        super(value);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----//
    // set //
    //-----//
    /**
     * Assign the boolean value
     *
     * @param value the assigned value
     */
    public final void set (boolean value)
    {
        this.value = value;
    }

    //-------//
    // isSet //
    //-------//
    /**
     * Report the current boolean value
     *
     * @return the current value
     */
    public boolean isSet ()
    {
        return value;
    }
}
