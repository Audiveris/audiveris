//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         E n u m P a n e                                        //
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
package org.audiveris.omr.score.ui;

import org.jdesktop.application.ResourceMap;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * Class <code>EnumPane</code> is a data pane to select an enum value.
 *
 * @param <T> specific category
 * @param <E> the enum to handle
 * @author Hervé Bitteur
 */
public class EnumPane<T, E extends Enum<E>>
        extends XactPane<E>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** ComboBox for enum. */
    private final JComboBox<E> enumCombo;

    /** Tooltips for enum values. */
    private final String[] tooltips;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates an <code>EnumPane</code> object.
     * <p>
     * Model and Parent to be assigned later.
     *
     * @param tag       unique in scope
     * @param values    value array of the enum
     * @param resources UI resources
     */
    public EnumPane (T tag,
                     E[] values,
                     ResourceMap resources)
    {
        super(resources.getString(tag + "Pane.title"));

        title.setToolTipText(resources.getString(tag + "Pane.toolTipText"));

        // Retrieve the tooltip for each combo value
        tooltips = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            tooltips[i] = resources.getString(tag + "Pane.combo." + values[i] + ".toolTipText");
        }

        enumCombo = new JComboBox<>(values);
        enumCombo.setToolTipText(resources.getString(tag + "Pane.combo.toolTipText"));
        enumCombo.setRenderer(new MyComboBoxRenderer()); // For support of value tooltips
        enumCombo.addActionListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public int defineLayout (PanelBuilder builder,
                             CellConstraints cst,
                             int titleWidth,
                             int r)
    {
        super.defineLayout(builder, cst, 1, r); // sel + title, no advance

        builder.add(enumCombo, cst.xyw(7, r, 3));

        return r + 2;
    }

    @Override
    protected void display (E value)
    {
        enumCombo.setSelectedItem(value);
    }

    @Override
    protected E read ()
    {
        return enumCombo.getItemAt(enumCombo.getSelectedIndex());
    }

    @Override
    public void setEnabled (boolean bool)
    {
        super.setEnabled(bool);
        enumCombo.setEnabled(bool);
    }

    @Override
    public void setVisible (boolean bool)
    {
        super.setVisible(bool);
        enumCombo.setVisible(bool);
    }

    class MyComboBoxRenderer
            extends BasicComboBoxRenderer
    {
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
}
