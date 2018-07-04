//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          L F i e l d                                           //
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

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Class {@code LField} is a kind of "Labeled Field", a logical composition of a label
 * and a component, which are handled as a whole.
 *
 * @param <C> precise subtype of JComponent field
 * <br>
 * <img src="doc-files/Fields.png" alt="Labeled Field Component UML">
 *
 * @author Hervé Bitteur
 */
public class LField<C extends JComponent>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The label. */
    private final JLabel label;

    /** The field. */
    private final C field;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new LField object.
     *
     * @param label the string to be used as label text
     * @param tip   the related tool tip text
     * @param field the field instance
     */
    public LField (String label,
                   String tip,
                   C field)
    {
        this.label = new JLabel(label, SwingConstants.RIGHT);
        this.field = field;

        if (tip != null) {
            this.label.setToolTipText(tip);
            this.field.setToolTipText(tip);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getField //
    //----------//
    /**
     * Getter for the field.
     *
     * @return the field
     */
    public C getField ()
    {
        return field;
    }

    //----------//
    // getLabel //
    //----------//
    /**
     * Getter for the label.
     *
     * @return the label
     */
    public JLabel getLabel ()
    {
        return label;
    }

    //------------//
    // setEnabled //
    //------------//
    /**
     * Enable or disable the whole label + field structure.
     *
     * @param bool true for enable, false for disable
     */
    public void setEnabled (boolean bool)
    {
        label.setEnabled(bool);
        field.setEnabled(bool);
    }

    //------------//
    // setVisible //
    //------------//
    /**
     * Make the whole label + field structure visible or not.
     *
     * @param bool true for visible, false for non visible
     */
    public void setVisible (boolean bool)
    {
        label.setVisible(bool);
        field.setVisible(bool);
    }
}
