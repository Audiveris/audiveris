//----------------------------------------------------------------------------//
//                                                                            //
//                          L D o u b l e F i e l d                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import java.util.Scanner;

/**
 * Class {@code LDoubleField} is an {@link LTextField}, whose field is
 * meant to handle a double value.
 *
 * @author Hervé Bitteur
 */
public class LDoubleField
        extends LTextField
{
    //~ Static fields/initializers ---------------------------------------------

    /** Default format for display in the field : {@value} */
    public static final String DEFAULT_FORMAT = "%.5f";

    //~ Instance fields --------------------------------------------------------
    /** Specific display format, if any */
    private final String format;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // LDoubleField //
    //--------------//
    /**
     * Create an (initially) editable double labelled field with proper
     * characteristics
     *
     * @param label  string for the label text
     * @param tip    related tool tip text
     * @param format specific display format
     */
    public LDoubleField (String label,
                         String tip,
                         String format)
    {
        this(true, label, tip, format);
    }

    //--------------//
    // LDoubleField //
    //--------------//
    /**
     * Create an (initially) editable double labelled field with proper
     * characteristics
     *
     * @param label string for the label text
     * @param tip   related tool tip text
     */
    public LDoubleField (String label,
                         String tip)
    {
        this(label, tip, null);
    }

    //--------------//
    // LDoubleField //
    //--------------//
    /**
     * Create a double labelled field with proper characteristics
     *
     * @param editable tells whether the field must be editable
     * @param label    string for the label text
     * @param tip      related tool tip text
     */
    public LDoubleField (boolean editable,
                         String label,
                         String tip)
    {
        this(editable, label, tip, null);
    }

    //--------------//
    // LDoubleField //
    //--------------//
    /**
     * Create a double labelled field with proper characteristics
     *
     * @param editable tells whether the field must be editable
     * @param label    string for the label text
     * @param tip      related tool tip text
     * @param format   specific display format
     */
    public LDoubleField (boolean editable,
                         String label,
                         String tip,
                         String format)
    {
        super(editable, label, tip);
        this.format = format;
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // getValue //
    //----------//
    /**
     * Extract the double value from the field (more precisely, the first
     * value found in the text of the field ...)
     *
     * @return the value as double
     */
    public double getValue ()
    {
        return new Scanner(getText()).nextDouble();
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Set the field value with a double, using the assigned format
     *
     * @param val the new value
     */
    public void setValue (double val)
    {
        getField()
                .setText(
                String.format((format != null) ? format : DEFAULT_FORMAT, val));
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Set the field value with a double
     *
     * @param val    the provided double value
     * @param format the specific format to be used
     */
    public void setValue (double val,
                          String format)
    {
        getField()
                .setText(String.format(format, val));
    }
}
