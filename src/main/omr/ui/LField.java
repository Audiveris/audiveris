//-----------------------------------------------------------------------//
//                                                                       //
//                              L F i e l d                              //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Class <code>LField</code> is a logical combination of a JLabel and an
 * {@link SField}, a "Labelled Field", where the label describes the
 * dynamic content of the field.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class LField
{
    //~ Instance variables ------------------------------------------------

    protected JLabel label;
    protected SField field;

    //~ Constructors ------------------------------------------------------

    //--------//
    // LField //
    //--------//
    /**
     * Create an editable labelled field with provided
     * characteristics. Note that the field can later be explicitly set as
     * editable
     *
     * @param label the string to be used as label text
     * @param tip the related tool tip text
     */
    public LField (String label,
                   String tip)
    {
        this(true, label, tip);
    }

    //--------//
    // LField //
    //--------//
    /**
     * Create a labelled field with initial characteristics
     *
     * @param editable tells whether the field is editable
     * @param label the string to be used as label text
     * @param tip the related tool tip text
     */
    public LField (boolean editable,
                   String  label,
                   String  tip)
    {
        this.label = new JLabel(label, SwingConstants.RIGHT);

        if (tip != null) {
            this.label.setToolTipText(tip);
        }

        field = new SField(editable, tip);
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // getLabel //
    //----------//
    /**
     * Report the label part
     *
     * @return the label
     */
    public JLabel getLabel()
    {
        return label;
    }

    //----------//
    // getField //
    //----------//
    /**
     * Report the field part
     *
     * @return the field
     */
    public SField getField()
    {
        return field;
    }

    //---------//
    // setText //
    //---------//
    /**
     * Modify the content of the field
     *
     * @param text
     */
    public void setText (String text)
    {
        field.setText(text);
    }

    //---------//
    // getText //
    //---------//
    /**
     * Report the current content of the field
     *
     * @return the field content
     */
    public String getText()
    {
        return field.getText();
    }
}
