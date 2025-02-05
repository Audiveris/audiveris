//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              C o m b o B o x T i p R e n d e r e r                             //
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
package org.audiveris.omr.ui.util;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * Class <code>ComboBoxTipRenderer</code> is a renderer for JComboBox with support for tool tip
 * for every value in combo box.
 *
 * @author Hervé Bitteur
 */
public class ComboBoxTipRenderer
        extends BasicComboBoxRenderer
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final String[] tooltips;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a tool-tip renderer for the combo box.
     *
     * @param tooltips tips for each combo value
     */
    public ComboBoxTipRenderer (String[] tooltips)
    {
        this.tooltips = tooltips;
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public Component getListCellRendererComponent (JList list,
                                                   Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean cellHasFocus)
    {
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());

            if (-1 < index) {
                list.setToolTipText(tooltips[index]); // Specific value tooltip
            }
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        setFont(list.getFont());

        if (value instanceof Icon icon) {
            setIcon(icon);
        } else {
            setText((value == null) ? "" : value.toString());
        }

        return this;
    }
}
