//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    L H e x a S p i n n e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;

/**
 * Class {@code LHexaSpinner} is an LIntegerSpinner with values displayed in hexadecimal.
 *
 * @author Hervé Bitteur
 */
public class LHexaSpinner
        extends LIntegerSpinner
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create an editable labeled hexa spinner with provided
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // HexaEditor //
    //------------//
    private static class HexaEditor
            extends JSpinner.NumberEditor
    {
        //~ Constructors ---------------------------------------------------------------------------

        HexaEditor (JSpinner spinner)
        {
            super(spinner);

            JFormattedTextField ftf = getTextField();
            ftf.setEditable(true);
            ftf.setFormatterFactory(new HexaFormatterFactory());
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void stateChanged (ChangeEvent e)
        {
            JSpinner spinner = (JSpinner) (e.getSource());
            getTextField().setValue(spinner.getValue());
        }
    }

    //---------------//
    // HexaFormatter //
    //---------------//
    private static class HexaFormatter
            extends DefaultFormatter
    {
        //~ Methods --------------------------------------------------------------------------------

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
                            "Illegal Number class for HexaFormatter " + value.getClass());
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
        //~ Constructors ---------------------------------------------------------------------------

        public HexaFormatterFactory ()
        {
            super(new HexaFormatter());
        }
    }
}
