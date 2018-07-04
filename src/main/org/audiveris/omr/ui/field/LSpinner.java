//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L S p i n n e r                                         //
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

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

/**
 * Class {@code LSpinner} is a logical combination of a JLabel and a JSpinner,
 * a "Labeled Spinner", where the label describes the dynamic content of the spinner.
 *
 * @author Hervé Bitteur
 */
public class LSpinner
{
    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * The related label
     */
    protected JLabel label;

    /**
     * The underlying spinner
     */
    protected JSpinner spinner;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an editable labeled spinner with provided
     * characteristics.
     *
     * @param label the string to be used as label text
     * @param tip   the related tool tip text
     */
    public LSpinner (String label,
                     String tip)
    {
        this.label = new JLabel(label, SwingConstants.RIGHT);
        spinner = new JSpinner();

        if (tip != null) {
            this.label.setToolTipText(tip);
            spinner.setToolTipText(tip);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------------//
    // addChangeListener //
    //-------------------//
    /**
     * Add a change listener to the spinner
     *
     * @param listener the change listener to add
     */
    public void addChangeListener (ChangeListener listener)
    {
        spinner.addChangeListener(listener);
    }

    //----------//
    // getLabel //
    //----------//
    /**
     * Report the label part
     *
     * @return the label
     */
    public JLabel getLabel ()
    {
        return label;
    }

    //------------//
    // getSpinner //
    //------------//
    /**
     * Report the spinner part
     *
     * @return the spinner
     */
    public JSpinner getSpinner ()
    {
        return spinner;
    }

    //----------//
    // setModel //
    //----------//
    /**
     * Set the data model for the spinner
     *
     * @param model the new data model
     */
    public void setModel (SpinnerModel model)
    {
        spinner.setModel(model);
    }
}
