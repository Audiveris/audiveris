//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     D o u b l e V a l u e                                      //
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
package org.audiveris.omr.util;

import javax.xml.bind.annotation.XmlValue;

/**
 * Class {@code DoubleValue} is a "poor man" version of java.lang.Double, when we need
 * a non-final class (whereas Double is declared as final)
 *
 * @author Hervé Bitteur
 */
public class DoubleValue
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The underlying double value */
    @XmlValue
    protected final double value;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new DoubleValue object.
     *
     * @param value a double value
     */
    public DoubleValue (double value)
    {
        this.value = value;
    }

    /**
     * Creates a new DoubleValue object.
     *
     * @param value a Double value (note the initial capital D)
     */
    public DoubleValue (Double value)
    {
        this.value = value.doubleValue();
    }

    /**
     * Creates a new DoubleValue object.
     *
     * @param str the string representation of the value
     */
    public DoubleValue (String str)
    {
        this(Double.valueOf(str));
    }

    /**
     * Creates a new DoubleValue object, initialized at zero. Meant for JAXB
     */
    private DoubleValue ()
    {
        this(0d);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // doubleValue //
    //-------------//
    /**
     * Returns the {@code double} value of this object.
     *
     * @return the {@code double} value represented by this object
     */
    public double doubleValue ()
    {
        return value;
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof DoubleValue) {
            return ((DoubleValue) obj).value == value;
        } else {
            return false;
        }
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 5;
        hash = (83 * hash)
               + (int) (Double.doubleToLongBits(this.value)
                        ^ (Double.doubleToLongBits(this.value) >>> 32));

        return hash;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return Double.toString(value);
    }
}
