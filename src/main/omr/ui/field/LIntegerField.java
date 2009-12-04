//----------------------------------------------------------------------------//
//                                                                            //
//                         L I n t e g e r F i e l d                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;


/**
 * Class <code>LIntegerField</code> is an {@link LTextField}, whose field is
 * meant to handle an integer value.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
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
     * @param tip related tool tip text
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
     * @param label string to be used as label text
     * @param tip related tool tip text
     */
    public LIntegerField (boolean editable,
                          String  label,
                          String  tip)
    {
        super(editable, label, tip);
    }

    //~ Methods ----------------------------------------------------------------

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

    //----------//
    // getValue //
    //----------//
    /**
     * Extract the current integer value form the text field (TODO: better
     * handling of exceptions)
     *
     * @return current integer value
     */
    public int getValue ()
    {
        int val = 0;

        try {
            val = Integer.parseInt(getField().getText());
        } catch (NumberFormatException ex) {
        }

        return val;
    }
}
