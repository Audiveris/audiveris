//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         E n u m P a n e                                        //
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
package org.audiveris.omr.score.ui;

import org.audiveris.omr.ui.util.ComboBoxTipRenderer;

import org.jdesktop.application.ResourceMap;

import com.jgoodies.forms.builder.FormBuilder;

import javax.swing.JComboBox;

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
        enumCombo.setRenderer(new ComboBoxTipRenderer(tooltips)); // For support of value tooltips
        enumCombo.addActionListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public int defineLayout (FormBuilder builder,
                             int titleWidth,
                             int r)
    {
        super.defineLayout(builder, 1, r); // sel + title, no advance

        builder.addRaw(enumCombo).xyw(7, r, 3);

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
}
