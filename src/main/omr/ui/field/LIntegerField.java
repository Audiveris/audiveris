//----------------------------------------------------------------------------//
//                                                                            //
//                         L I n t e g e r F i e l d                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

/**
 * Class {@code LIntegerField} is an {@link LTextField}, whose field is
 * meant to handle an integer value.
 *
 * @author Hervé Bitteur
 */
public class LIntegerField
        extends LTextField
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // LIntegerField //
    //---------------//
    /**
     * Create a (constant) integer labelled field
     *
     * @param label string to be used as label text
     * @param tip   related tool tip text
     */
    public LIntegerField (String label,
                          String tip)
    {
        super(true, label, tip);
    }

    //---------------//
    // LIntegerField //
    //---------------//
    /**
     * Create an integer labelled field
     *
     * @param editable tells whether the field is editable
     * @param label    string to be used as label text
     * @param tip      related tool tip text
     */
    public LIntegerField (boolean editable,
                          String label,
                          String tip)
    {
        super(editable, label, tip);
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // getValue //
    //----------//
    /**
     * Extract the current integer value form the text field
     *
     * @return current integer value (a blank field is assumed to mean 0)
     * @throws NumberFormatException if the field syntax is incorrect
     */
    public int getValue ()
    {
        String str = getField()
                .getText()
                .trim();

        if (str.length() == 0) {
            return 0;
        } else {
            return Integer.parseInt(str);
        }
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Modify the current field
     *
     * @param val the integer value to be used
     */
    public void setValue (int val)
    {
        getField()
                .setText(Integer.toString(val));
    }
}
