//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      N a m e d D o u b l e                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.audiveris.omr.constant.Constant;

/**
 * Class <code>NamedDouble</code> is a documented Double.
 *
 * @author Hervé Bitteur
 */
public class NamedDouble
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final String name;

    private final String quantityUnit;

    private double value;

    private final String description;

    /**
     * Creates a new <code>BasicNamedDouble</code> object based on a {@link Constant.Double}.
     *
     * @param cst the provided Constant.DOuble instance
     */
    public NamedDouble (Constant.Double cst)
    {
        this(cst.getName(), cst.getQuantityUnit(), cst.getValue(), cst.getDescription());
    }

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>BasicNamedDouble</code> object.
     *
     * @param name         a name for this entity
     * @param quantityUnit unit used by value
     * @param value        initial value
     * @param description  semantic
     */
    public NamedDouble (String name,
                        String quantityUnit,
                        double value,
                        String description)
    {
        this.name = name;
        this.quantityUnit = quantityUnit;
        this.value = value;
        this.description = description;
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Get the description sentence recorded with the NamedDouble
     *
     * @return the description sentence as a string
     */
    public String getDescription ()
    {
        return description;
    }

    /**
     * Report the name of the NamedDouble
     *
     * @return the NamedDouble name
     */
    public String getName ()
    {
        return name;
    }

    /**
     * Report the unit, if any, used as base of quantity measure
     *
     * @return the quantity unit, if any
     */
    public String getQuantityUnit ()
    {
        return quantityUnit;
    }

    /**
     * Report the current value
     *
     * @return current value
     */
    public double getValue ()
    {
        return value;
    }

    /**
     * Assign a new value
     *
     * @param value value to be assigned
     */
    public void setValue (double value)
    {
        this.value = value;
    }

    @Override
    public String toString ()
    {
        return name + "/" + value;
    }
}
