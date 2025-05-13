//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   F o n t A t t r i b u t e s                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2024. All rights reserved.
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
package org.audiveris.omr.text;

import static org.audiveris.omr.util.RegexUtil.group;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class <code>FontAttributes</code> gathers immutable font attributes for a word,
 * provided generally by OCR or manual assignment.
 *
 * @author Hervé Bitteur
 */
public class FontAttributes
{
    //~ Static fields/initializers -----------------------------------------------------------------

    protected static final String ATTRS = "attrs";

    protected static final String attrsPat = group(ATTRS, "[BIUMSC]*");

    protected static final Pattern attrsPattern = Pattern.compile(attrsPat);

    //~ Instance fields ----------------------------------------------------------------------------

    /** True if bold. */
    public final boolean isBold;

    /** True if italic. */
    public final boolean isItalic;

    /** True if underlined. */
    public final boolean isUnderlined;

    /** True if monospaced. */
    public final boolean isMonospaced;

    /** True if serif. */
    public final boolean isSerif;

    /** True if small capitals. */
    public final boolean isSmallcaps;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>FontAttributes</code> object.
     *
     * @param isBold       True if the font is bold
     * @param isItalic     True if the font is italic
     * @param isUnderlined True if the font is underlined
     * @param isMonospaced True if the font has a fixed width
     * @param isSerif      True for fonts with serifs
     * @param isSmallcaps  True for small caps fonts
     */
    public FontAttributes (boolean isBold,
                           boolean isItalic,
                           boolean isUnderlined,
                           boolean isMonospaced,
                           boolean isSerif,
                           boolean isSmallcaps)
    {
        this.isBold = isBold;
        this.isItalic = isItalic;
        this.isUnderlined = isUnderlined;
        this.isMonospaced = isMonospaced;
        this.isSerif = isSerif;
        this.isSmallcaps = isSmallcaps;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // getSpec //
    //---------//
    /**
     * Report a string representing the specification of this attributes instance.
     *
     * @return the specification string
     * @see #decode(java.lang.String)
     */
    public String getSpec ()
    {
        final StringBuilder sb = new StringBuilder();

        if (isBold) {
            sb.append('B');
        }

        if (isItalic) {
            sb.append('I');
        }

        if (isUnderlined) {
            sb.append('U');
        }

        if (isMonospaced) {
            sb.append('M');
        }

        if (isSerif) {
            sb.append('S');
        }

        if (isSmallcaps) {
            sb.append('C');
        }

        return sb.toString();
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------//
    // decode //
    //--------//
    /**
     * Decode a FontAttributes instance out of the provided specification string.
     *
     * @param str the input specification
     * @return decoded attributes
     * @see #getSpec()
     */
    public static FontAttributes decode (String str)
    {
        final Matcher matcher = attrsPattern.matcher(str);

        if (matcher.matches()) {
            final String attrs = matcher.group(ATTRS);

            if (attrs != null) {
                return new FontAttributes(
                        attrs.indexOf('B') != -1,
                        attrs.indexOf('I') != -1,
                        attrs.indexOf('U') != -1,
                        attrs.indexOf('M') != -1,
                        attrs.indexOf('S') != -1,
                        attrs.indexOf('C') != -1);
            } else {
                return null;
            }

        } else {
            throw new IllegalArgumentException("Invalid font attributes \"" + str + "\"");
        }
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof FontAttributes that) {
            return this.isBold == that.isBold //
                    && this.isItalic == that.isItalic //
                    && this.isUnderlined == that.isUnderlined //
                    && this.isMonospaced == that.isMonospaced //
                    && this.isSerif == that.isSerif //
                    && this.isSmallcaps == that.isSmallcaps;
        }

        return false;
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = 67 * hash + (this.isBold ? 1 : 0);
        hash = 67 * hash + (this.isItalic ? 1 : 0);
        hash = 67 * hash + (this.isUnderlined ? 1 : 0);
        hash = 67 * hash + (this.isMonospaced ? 1 : 0);
        hash = 67 * hash + (this.isSerif ? 1 : 0);
        hash = 67 * hash + (this.isSmallcaps ? 1 : 0);
        return hash;
    }
}
