//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 L I n t e g e r S p i n n e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.ui.field;

import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

/**
 * Class {@code LSpinner} is a logical combination of a JLabel and a JSpinner,
 * a "Labeled Spinner", where the label describes the dynamic content of the spinner.
 *
 * @author Hervé Bitteur
 */
public class LIntegerSpinner
        extends LSpinner
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create an editable labeled spinner with provided
     * characteristics.
     *
     * @param label the string to be used as label text
     * @param tip   the related tool tip text
     */
    public LIntegerSpinner (String label,
                            String tip)
    {
        super(label, tip);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------------//
    // addChangeListener //
    //-------------------//
    /**
     * Add a change listener to the spinner
     *
     * @param listener
     */
    @Override
    public void addChangeListener (ChangeListener listener)
    {
        spinner.addChangeListener(listener);
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the current content of the spinner
     *
     * @return the spinner content
     */
    public Integer getValue ()
    {
        return (Integer) spinner.getValue();
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Modify the content of the spinner
     *
     * @param value
     */
    public void setValue (Integer value)
    {
        spinner.setValue(value);
    }

    //----------//
    // setModel //
    //----------//
    /**
     * Set the data model for the spinner
     *
     * @param model the new data model
     */
    void setModel (SpinnerNumberModel model)
    {
        spinner.setModel(model);
    }
}
