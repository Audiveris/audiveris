//----------------------------------------------------------------------------//
//                                                                            //
//                              L S p i n n e r                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>LSpinner</code> is a logical combination of a JLabel and a
 * JSpinner, a "Labelled Spinner", where the label describes
 * the dynamic content of the spinner.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LSpinner
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The related label
     */
    protected JLabel label;

    /**
     * The underlying spinner
     */
    protected JSpinner spinner;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // LSpinner //
    //----------//
    /**
     * Create an editable labelled spinner with provided
     * characteristics.
     *
     * @param label the string to be used as label text
     * @param tip the related tool tip text
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

    //~ Methods ----------------------------------------------------------------

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

    //-------------------//
    // addChangeListener //
    //-------------------//
    /**
     * Add a change listener to the spinner
     *
     * @param listener
     */
    public void addChangeListener (ChangeListener listener)
    {
        spinner.addChangeListener(listener);
    }
}
