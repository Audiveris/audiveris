//-----------------------------------------------------------------------//
//                                                                       //
//                              S F i e l d                              //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import javax.swing.JTextField;

/**
 * Class <code>SField</code> is a simple JTextField with predefined
 * attributes
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SField
    extends JTextField
{
    //~ Constructors ------------------------------------------------------

    //--------//
    // SField //
    //--------//
    /**
     * Create an editable text field, with provided tool tip
     *
     * @param tip the related tool tip text
     */
    public SField (String tip)
    {
        this(true, tip);
    }

    //--------//
    // SField //
    //--------//
    /**
     * Create a text field
     *
     * @param editable tells whether the field must be editable
     * @param tip the related tool tip text
     */
    public SField (boolean editable,
                   String  tip)
    {
        super(6);

        setHorizontalAlignment(JTextField.CENTER);

        if (tip != null) {
            setToolTipText(tip);
        }

        setEditable(editable);
    }
}
