//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       L C o m b o B o x                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import javax.swing.JComboBox;

/**
 * Class {@code LComboBox} is a logical combination of a JLabel and a JComboBox,
 * a "Labeled Combo", where the label describes the dynamic content of the combo.
 *
 * @param <E> type of combo entity
 * @author Hervé Bitteur
 */
public class LComboBox<E>
        extends LField<JComboBox<E>>
{

    /**
     * Create an editable labeled combo with provided characteristics.
     *
     * @param items the items handled by the combo
     */
    public LComboBox (E[] items)
    {
        this(null, null, items);
    }

    /**
     * Create an editable labeled combo with provided
     * characteristics.
     *
     * @param label the string to be used as label text
     * @param tip   the related tool tip text
     * @param items the items handled by the combo
     */
    public LComboBox (String label,
                      String tip,
                      E[] items)
    {
        super(label, tip, new JComboBox<>(items));
    }

    //-------------------//
    // addActionListener //
    //-------------------//
    /**
     * Add an action listener to the combo.
     *
     * @param listener the action listener to add
     */
    public void addActionListener (ActionListener listener)
    {
        getField().addActionListener(listener);
    }

    //-----------------//
    // getSelectedItem //
    //-----------------//
    /**
     * Report the selected item.
     *
     * @return selected item
     */
    @SuppressWarnings("unchecked")
    public E getSelectedItem ()
    {
        return (E) getField().getSelectedItem();
    }

    //-----------------//
    // setSelectedItem //
    //-----------------//
    /**
     * Select the provided item.
     *
     * @param item provided item
     */
    public void setSelectedItem (E item)
    {
        getField().setSelectedItem(item);
    }
}
