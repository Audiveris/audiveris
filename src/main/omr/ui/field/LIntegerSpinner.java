//----------------------------------------------------------------------------//
//                                                                            //
//                       L I n t e g e r S p i n n e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

/**
 * Class {@code LSpinner} is a logical combination of a JLabel and a
 * JSpinner, a "Labelled Spinner", where the label describes
 * the dynamic content of the spinner.
 *
 * @author Hervé Bitteur
 */
public class LIntegerSpinner
        extends LSpinner
{
    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // LIntegerSpinner //
    //-----------------//
    /**
     * Create an editable labelled spinner with provided
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

    //~ Methods ----------------------------------------------------------------
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
