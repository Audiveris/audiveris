//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L R a d i o B u t t o n                                 //
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
package org.audiveris.omr.ui.field;

import java.awt.event.ActionListener;

import javax.swing.JRadioButton;

/**
 * Class <code>LCheckBox</code> is a logical combination of a JLabel and a JRadioButton,
 * a "Labeled Radio", where the label describes the dynamic content of the check box.
 *
 * @author Hervé Bitteur
 */
public class LRadioButton
        extends LField<JRadioButton>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new LRadioButton object.
     *
     * @param label the string to be used as label text
     * @param tip   the related tool tip text
     */
    public LRadioButton (String label,
                      String tip)
    {
        super(label, tip, new JRadioButton());
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------------------//
    // addActionListener //
    //-------------------//
    /**
     * Add an action listener to the box.
     *
     * @param listener the action listener to add
     */
    public void addActionListener (ActionListener listener)
    {
        getField().addActionListener(listener);
    }
}
