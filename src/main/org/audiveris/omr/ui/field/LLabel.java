//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           L L a b e l                                          //
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
package org.audiveris.omr.ui.field;

import javax.swing.JLabel;

/**
 * Class <code>LLabel</code> is a labeled label.
 *
 * @author Hervé Bitteur
 */
public class LLabel
        extends LField<JLabel>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>LLabel</code> object.
     *
     * @param horizontalAlignment horizontal alignment
     */
    public LLabel (int horizontalAlignment)
    {
        this(null, null, horizontalAlignment);
    }

    /**
     * Creates a new <code>LLabel</code> object, with default center alignment
     *
     * @param label the string to be used as label text
     * @param tip   the related tool tip text
     */
    public LLabel (String label,
                   String tip)
    {
        this(label, tip, JLabel.CENTER);
    }

    /**
     * Creates a new <code>LLabel</code> object.
     *
     * @param label               the string to be used as label text
     * @param tip                 the related tool tip text
     * @param horizontalAlignment horizontal alignment
     */
    public LLabel (String label,
                   String tip,
                   int horizontalAlignment)
    {
        super(label, tip, new JLabel());
        getField().setHorizontalAlignment(horizontalAlignment);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // getText //
    //---------//
    /**
     * Report the current content of the field
     *
     * @return the field content
     */
    public String getText ()
    {
        return getField().getText();
    }

    //---------//
    // setText //
    //---------//
    /**
     * Modify the content of the field
     *
     * @param text new field content to set
     */
    public void setText (String text)
    {
        getField().setText(text);
    }
}
