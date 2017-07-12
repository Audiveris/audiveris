//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    L D o u b l e F i e l d                                     //
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
package org.audiveris.omr.ui.field;

import java.util.Scanner;

/**
 * Class {@code LDoubleField} is an {@link LTextField}, whose field is meant to handle
 * a double value.
 *
 * @author Hervé Bitteur
 */
public class LDoubleField
        extends LTextField
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Default format for display in the field : {@value} */
    public static final String DEFAULT_FORMAT = "%.5f";

    //~ Instance fields ----------------------------------------------------------------------------
    /** Specific display format, if any */
    private final String format;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an (initially) editable double labeled field with proper
     * characteristics
     *
     * @param label  string for the label text
     * @param tip    related tool tip text
     * @param format specific display format
     */
    public LDoubleField (String label,
                         String tip,
                         String format)
    {
        this(true, label, tip, format);
    }

    //--------------//
    // LDoubleField //
    //--------------//
    /**
     * Create an (initially) editable double labeled field with proper
     * characteristics
     *
     * @param label string for the label text
     * @param tip   related tool tip text
     */
    public LDoubleField (String label,
                         String tip)
    {
        this(label, tip, null);
    }

    //--------------//
    // LDoubleField //
    //--------------//
    /**
     * Create a double labeled field with proper characteristics
     *
     * @param editable tells whether the field must be editable
     * @param label    string for the label text
     * @param tip      related tool tip text
     */
    public LDoubleField (boolean editable,
                         String label,
                         String tip)
    {
        this(editable, label, tip, null);
    }

    //--------------//
    // LDoubleField //
    //--------------//
    /**
     * Create a double labeled field with proper characteristics
     *
     * @param editable tells whether the field must be editable
     * @param label    string for the label text
     * @param tip      related tool tip text
     * @param format   specific display format
     */
    public LDoubleField (boolean editable,
                         String label,
                         String tip,
                         String format)
    {
        super(editable, label, tip);
        this.format = format;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getValue //
    //----------//
    /**
     * Extract the double value from the field (more precisely, the first
     * value found in the text of the field ...)
     *
     * @return the value as double
     */
    public double getValue ()
    {
        return new Scanner(getText()).nextDouble();
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Set the field value with a double, using the assigned format
     *
     * @param val the new value
     */
    public void setValue (double val)
    {
        getField().setText(String.format((format != null) ? format : DEFAULT_FORMAT, val));
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Set the field value with a double
     *
     * @param val    the provided double value
     * @param format the specific format to be used
     */
    public void setValue (double val,
                          String format)
    {
        getField().setText(String.format(format, val));
    }
}
