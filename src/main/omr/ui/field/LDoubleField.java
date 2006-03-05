//-----------------------------------------------------------------------//
//                                                                       //
//                        L D o u b l e F i e l d                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui.field;

import java.util.Scanner;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Class <code>LDoubleField</code> is an {@link LField}, whose field is
 * meant to handle a double value.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LDoubleField
    extends LField
{
    //~ Constructors ------------------------------------------------------

    //--------------//
    // LDoubleField //
    //--------------//
    /**
     * Create an (initially) editable double labelled field with proper
     * characteristics
     *
     * @param label string for the label text
     * @param tip related tool tip text
     */
    public LDoubleField(String label,
                        String tip)
    {
        this(true, label, tip);
    }

    //--------------//
    // LDoubleField //
    //--------------//
    /**
     * Create a double labelled field with proper characteristics
     *
     * @param editable tells whether the field must be editable
     * @param label string for the label text
     * @param tip related tool tip text
     */
    public LDoubleField(boolean editable,
                        String  label,
                        String  tip)
    {
        super(editable, label, tip);
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // getValue //
    //----------//
    /**
     * Extract the double value from the field (more precisely, the first
     * value found in the text of the field ...)
     *
     * @return the value as double
     */
    public double getValue()
    {
        return new Scanner(getText()).nextDouble();
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Set the field value with a double, using predefined format
     *
     * @param val the new value
     */
    public void setValue (double val)
    {
        field.setText(String.format("%.5f", val));
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Set the field value with a double
     *
     * @param val the provided double value
     * @param format the specific format to be used
     */
    public void setValue (double val,
                          String format)
    {
        field.setText(String.format(format, val));
    }
}
