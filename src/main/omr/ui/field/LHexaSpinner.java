//----------------------------------------------------------------------------//
//                                                                            //
//                          L H e x a S p i n n e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright ® Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;

/**
 * Class {@code LHexaSpinner} is an LIntegerSpinner with values
 * displayed in hexadecimal.
 *
 * @author Hervé Bitteur
 */
public class LHexaSpinner
        extends LIntegerSpinner
{
    //~ Constructors -----------------------------------------------------------

    //--------------//
    // LHexaSpinner //
    //--------------//
    /**
     * Create an editable labelled hexa spinner with provided
     * characteristics.
     *
     * @param label the string to be used as label text
     * @param tip   the related tool tip text
     */
    public LHexaSpinner (String label,
                         String tip)
    {
        super(label, tip);
        spinner.setEditor(new HexaEditor(spinner));
    }

    //~ Inner Classes ----------------------------------------------------------
    //------------//
    // HexaEditor //
    //------------//
    private static class HexaEditor
            extends JSpinner.NumberEditor
    {
        //~ Constructors -------------------------------------------------------

        HexaEditor (JSpinner spinner)
        {
            super(spinner);

            JFormattedTextField ftf = getTextField();
            ftf.setEditable(true);
            ftf.setFormatterFactory(new HexaFormatterFactory());
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void stateChanged (ChangeEvent e)
        {
            JSpinner spinner = (JSpinner) (e.getSource());
            getTextField()
                    .setValue(spinner.getValue());
        }
    }

    //---------------//
    // HexaFormatter //
    //---------------//
    private static class HexaFormatter
            extends DefaultFormatter
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public Object stringToValue (String string)
                throws ParseException
        {
            try {
                JFormattedTextField ftf = getFormattedTextField();
                Object value = ftf.getValue();

                if (value instanceof Integer) {
                    return Integer.valueOf(string, 16);
                } else if (value instanceof Long) {
                    return Long.valueOf(string, 16);
                } else {
                    throw new IllegalArgumentException(
                            "Illegal Number class for HexaFormatter "
                            + value.getClass());
                }
            } catch (NumberFormatException ex) {
                throw new ParseException(string, 0);
            }
        }

        @Override
        public String valueToString (Object value)
                throws ParseException
        {
            if (value == null) {
                return "";
            }

            return Long.toHexString(((Number) value).longValue());
        }
    }

    //----------------------//
    // HexaFormatterFactory //
    //----------------------//
    private static class HexaFormatterFactory
            extends DefaultFormatterFactory
    {
        //~ Constructors -------------------------------------------------------

        public HexaFormatterFactory ()
        {
            super(new HexaFormatter());
        }
    }
}
