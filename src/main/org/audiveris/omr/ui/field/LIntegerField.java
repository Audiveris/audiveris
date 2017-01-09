//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   L I n t e g e r F i e l d                                    //
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
package org.audiveris.omr.ui.field;

/**
 * Class {@code LIntegerField} is an {@link LTextField}, whose field is meant to handle
 * an integer value.
 *
 * @author Hervé Bitteur
 */
public class LIntegerField
        extends LTextField
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a (constant) integer labeled field
     *
     * @param label string to be used as label text
     * @param tip   related tool tip text
     */
    public LIntegerField (String label,
                          String tip)
    {
        super(true, label, tip);
    }

    /**
     * Create an integer labeled field
     *
     * @param editable tells whether the field is editable
     * @param label    string to be used as label text
     * @param tip      related tool tip text
     */
    public LIntegerField (boolean editable,
                          String label,
                          String tip)
    {
        super(editable, label, tip);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getValue //
    //----------//
    /**
     * Extract the current integer value form the text field
     *
     * @return current integer value (a blank field is assumed to mean 0)
     * @throws NumberFormatException if the field syntax is incorrect
     */
    public int getValue ()
    {
        String str = getField().getText().trim();

        if (str.length() == 0) {
            return 0;
        } else {
            return Integer.parseInt(str);
        }
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Modify the current field
     *
     * @param val the integer value to be used
     */
    public void setValue (int val)
    {
        getField().setText(Integer.toString(val));
    }
}
