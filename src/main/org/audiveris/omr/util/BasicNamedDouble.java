//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 B a s i c N a m e d D o u b l e                                //
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
 * Class {@code BasicNamedDouble} is a simple implementation of NamedDouble.
 *
 * @author Hervé Bitteur
 */
public class BasicNamedDouble
        implements NamedDouble
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final String name;

    private final String quantityUnit;

    private double value;

    private final String description;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BasicNamedDouble} object.
     *
     * @param name         a name for this entity
     * @param quantityUnit unit used by value
     * @param value        initial value
     * @param description  semantic
     */
    public BasicNamedDouble (String name,
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
    @Override
    public String getDescription ()
    {
        return description;
    }

    @Override
    public String getName ()
    {
        return name;
    }

    @Override
    public String getQuantityUnit ()
    {
        return quantityUnit;
    }

    @Override
    public String getShortTypeName ()
    {
        return name;
    }

    @Override
    public double getValue ()
    {
        return value;
    }

    @Override
    public void setValue (double value)
    {
        this.value = value;
    }
}
