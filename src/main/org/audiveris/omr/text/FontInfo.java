//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F o n t I n f o                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Non-mutable font attributes (generally for a word).
 *
 * @author Hervé Bitteur
 */
public class FontInfo
{

    private static final Logger logger = LoggerFactory.getLogger(FontInfo.class);

    /** A default FontInfo instance when we absolutely need one. */
    public static final FontInfo DEFAULT = createDefault(36);

    /** Separator in memo between attributes (if any) and point size. */
    private static final char SEPARATOR = '-';

    /** True if bold. */
    public final boolean isBold;

    /** True if italic. */
    public final boolean isItalic;

    /** True if underlined. */
    public final boolean isUnderlined;

    /** True if monospace. */
    public final boolean isMonospace;

    /** True if serif. */
    public final boolean isSerif;

    /** True if small capitals. */
    public final boolean isSmallcaps;

    /** Font size, specified in printer points (1/72 inch). */
    public final int pointsize;

    /** Font name. */
    public final String fontName;

    /**
     * Creates a new FontInfo object.
     *
     * @param isBold       True if the font is bold
     * @param isItalic     True if the font is italic
     * @param isUnderlined True if the font is underlined
     * @param isMonospace  True if the font has a fixed width
     * @param isSerif      True for fonts with serifs
     * @param isSmallcaps  True for small caps fonts
     * @param pointsize    font size in points
     * @param fontName     font name
     */
    public FontInfo (boolean isBold,
                     boolean isItalic,
                     boolean isUnderlined,
                     boolean isMonospace,
                     boolean isSerif,
                     boolean isSmallcaps,
                     int pointsize,
                     String fontName)
    {
        this.isBold = isBold;
        this.isItalic = isItalic;
        this.isUnderlined = isUnderlined;
        this.isMonospace = isMonospace;
        this.isSerif = isSerif;
        this.isSmallcaps = isSmallcaps;
        this.pointsize = pointsize;
        this.fontName = fontName;
    }

    /**
     * Create a new {@code FontInfo} from another one and a specific point size.
     *
     * @param org       the original font info
     * @param pointSize the specific point size
     */
    public FontInfo (FontInfo org,
                     int pointSize)
    {
        this(
                org.isBold,
                org.isItalic,
                org.isUnderlined,
                org.isMonospace,
                org.isSerif,
                org.isSmallcaps,
                pointSize,
                org.fontName);
    }

    //----------//
    // getMnemo //
    //----------//
    /**
     * Report a very short description of font characteristics.
     * <p>
     * Format is "BS-45" or "53".
     * Separator (-) is present only if there is at least one attribute.
     * This is to avoid "-53" which would look like a negative value.
     *
     * @return a short description
     */
    public String getMnemo ()
    {
        StringBuilder sb = new StringBuilder();

        if (isBold) {
            sb.append('B');
        }

        if (isItalic) {
            sb.append('I');
        }

        if (isUnderlined) {
            sb.append('U');
        }

        if (isMonospace) {
            sb.append('M');
        }

        if (isSerif) {
            sb.append('S');
        }

        if (isSmallcaps) {
            sb.append('C');
        }

        if (sb.length() > 0) {
            sb.append(SEPARATOR);
        }

        sb.append(pointsize);

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append(fontName);
        sb.append(' ').append(getMnemo());
        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // createDefault //
    //---------------//
    /**
     * Create a default font using provided fontSize.
     *
     * @param fontSize the size to use
     * @return the FontInfo instance
     */
    public static FontInfo createDefault (int fontSize)
    {
        return new FontInfo(false, false, false, false, true, false, fontSize, "Serif");
    }

    //--------//
    // decode //
    //--------//
    /**
     * Decode a FontInfo out of the provided string.
     *
     * @param str input string
     * @return decoded FontInfo
     */
    public static FontInfo decode (String str)
    {
        final int sep = str.indexOf(SEPARATOR);
        final String sizeStr = (sep != -1) ? str.substring(sep + 1) : str;
        final int size = Integer.decode(sizeStr);

        return new FontInfo(
                str.indexOf('B') != -1,
                str.indexOf('I') != -1,
                str.indexOf('U') != -1,
                str.indexOf('M') != -1,
                str.indexOf('S') != -1,
                str.indexOf('C') != -1,
                size,
                "generic");
    }

    //---------//
    // Adapter //
    //---------//
    /**
     * JAXB adapter for FontInfo.
     */
    public static class Adapter
            extends XmlAdapter<String, FontInfo>
    {

        @Override
        public String marshal (FontInfo info)
                throws Exception
        {
            if (info == null) {
                logger.warn("Null FontInfo");

                return null;
            }

            return info.getMnemo();
        }

        @Override
        public FontInfo unmarshal (String mnemo)
                throws Exception
        {
            if (mnemo == null) {
                logger.warn("Null mnemo");

                return null;
            }

            return FontInfo.decode(mnemo);
        }
    }
}
