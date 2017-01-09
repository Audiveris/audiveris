//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              I n t e g e r L i s t S p i n n e r                               //
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

import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;

/**
 * Class {@code IntegerListSpinner} is a spinner whose model is a list of integers.
 *
 * @author Hervé Bitteur
 */
public class IntegerListSpinner
        extends JSpinner
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new IntegerListSpinner object.
     */
    public IntegerListSpinner ()
    {
        setModel(new SpinnerListModel());

        // Right alignment
        JSpinner.DefaultEditor editor;
        editor = (JSpinner.DefaultEditor) getEditor();
        editor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
    }
}
