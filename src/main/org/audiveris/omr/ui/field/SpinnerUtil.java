//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S p i n n e r U t i l                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;

/**
 * Class {@code SpinnerUtil} gathers a few utilities for JSpinner entities
 *
 * @author Hervé Bitteur
 */
public abstract class SpinnerUtil
{

    //----------------//
    // fixIntegerList //
    //----------------//
    /**
     * Workaround for a swing bug : when the user enters an illegal value, the
     * text is forced to the last value.
     *
     * @param spinner the spinner to update
     */
    public static void fixIntegerList (final JSpinner spinner)
    {
        JSpinner.DefaultEditor editor;
        editor = (JSpinner.DefaultEditor) spinner.getEditor();

        final JFormattedTextField ftf = editor.getTextField();
        ftf.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enterAction");
        ftf.getActionMap().put(
                "enterAction",
                new AbstractAction()
        {
            @Override
            public void actionPerformed (ActionEvent e)
            {
                try {
                    spinner.setValue(Integer.parseInt(ftf.getText()));
                } catch (Exception ex) {
                    // Reset to last value
                    ftf.setText(ftf.getValue().toString());
                }
            }
        });
    }

    //-------------//
    // setEditable //
    //-------------//
    /**
     * Make the spinner text field editable, or not
     *
     * @param spinner the spinner to update
     * @param bool    true if editable, false otherwise
     */
    public static void setEditable (JSpinner spinner,
                                    boolean bool)
    {
        JSpinner.DefaultEditor editor;
        editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setEditable(bool);
    }

    //---------//
    // setList //
    //---------//
    /**
     * Assign the List model (for a list-based spinner)
     *
     * @param spinner the spinner to update
     * @param values  the model list values
     */
    public static void setList (JSpinner spinner,
                                List<?> values)
    {
        SpinnerModel model = spinner.getModel();

        if (model instanceof SpinnerListModel) {
            ((SpinnerListModel) model).setList(values);
        } else {
            throw new IllegalArgumentException("Spinner model is not a SpinnerListModel");
        }
    }

    //-------------------//
    // setRightAlignment //
    //-------------------//
    /**
     * Align the spinner display to the right
     *
     * @param spinner the spinner to update
     */
    public static void setRightAlignment (JSpinner spinner)
    {
        JSpinner.DefaultEditor editor;
        editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
    }
}
