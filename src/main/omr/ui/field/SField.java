//----------------------------------------------------------------------------//
//                                                                            //
//                                S F i e l d                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import javax.swing.JTextField;

/**
 * Class <code>SField</code> is a simple JTextField with predefined
 * attributes
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SField
    extends JTextField
{
    //~ Static fields/initializers ---------------------------------------------

    /** Defaut number of characters in the text field : {@value}*/
    private static final int FIELD_WIDTH = 6;

    //~ Constructors -----------------------------------------------------------

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
        super(FIELD_WIDTH);

        setHorizontalAlignment(JTextField.CENTER);

        if (tip != null) {
            setToolTipText(tip);
        }

        setEditable(editable);
    }
}
