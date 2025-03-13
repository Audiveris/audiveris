//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    L D o u b l e F i e l d                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.ui.field;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

/**
 * Class <code>LDoubleField</code> is an {@link LTextField}, whose field is meant to handle
 * a double value.
 *
 * @author Hervé Bitteur
 */
public class LDoubleField
        extends LTextField
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Default format for display in the field : {@value} */
    public static final String DEFAULT_FORMAT = "%.5f";

    /** To cope with number formats that depend on current locale. */
    public static NumberFormat numberFormat = NumberFormat.getInstance(Locale.getDefault());

    //~ Instance fields ----------------------------------------------------------------------------

    /** Specific display format, if any. */
    private final String format;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a double labeled field with proper characteristics.
     *
     * @param editable tells whether the field must be editable
     * @param label    string for the label text
     * @param tip      related tool tip text
     * @param format   specific display format, if any
     */
    public LDoubleField (boolean editable,
                         String label,
                         String tip,
                         String format)
    {
        super(editable, label, tip);
        this.format = format;
        setFilter();
    }

    /**
     * Create a double labeled field with proper characteristics.
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

    /**
     * Create an (initially) editable double labeled field with proper characteristics.
     *
     * @param label string for the label text
     * @param tip   related tool tip text
     */
    public LDoubleField (String label,
                         String tip)
    {
        this(label, tip, null);
    }

    /**
     * Create an (initially) editable double labeled field with proper characteristics.
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

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // getValue //
    //----------//
    /**
     * Extract the double value from the field (a blank field is assumed to mean 0.0).
     *
     * @return the value as double
     */
    public double getValue ()
    {
        final String str = getField().getText().trim();

        if (str.length() == 0) {
            return 0.0;
        } else {
            return Double.parseDouble(str);
        }
    }

    //-----------//
    // setFilter //
    //-----------//
    /**
     * Adds the filter to the input field's document.
     */
    private void setFilter ()
    {
        final AbstractDocument doc = (AbstractDocument) getField().getDocument();
        doc.setDocumentFilter(new DoubleFilter());
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
        getField().setText(String.format((format != null) ? format : DEFAULT_FORMAT, val));
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
        getField().setText(String.format(format, val));
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    /**
     * Intercepts input in the LDoubleField and disallows input that would
     * result in an invalid double.
     */
    private class DoubleFilter
            extends DocumentFilter
    {
        @Override
        public void insertString (FilterBypass fb,
                                  int offset,
                                  String string,
                                  AttributeSet attr)
            throws BadLocationException
        {
            final Document doc = fb.getDocument();
            final StringBuilder sb = new StringBuilder(doc.getText(0, doc.getLength()));
            sb.insert(offset, string);

            if (test(sb.toString())) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace (FilterBypass fb,
                             int offset,
                             int length,
                             String text,
                             AttributeSet attrs)
            throws BadLocationException
        {
            final Document doc = fb.getDocument();
            final StringBuilder sb = new StringBuilder(doc.getText(0, doc.getLength()));
            sb.replace(offset, offset + length, text);

            if (test(sb.toString())) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        private boolean test (String text)
        {
            try {
                numberFormat.parse(text);
                return true;
            } catch (ParseException e) {
                return false;
            }
        }
    }
}
