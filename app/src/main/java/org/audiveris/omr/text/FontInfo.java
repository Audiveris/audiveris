//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F o n t I n f o                                         //
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
package org.audiveris.omr.text;

import static org.audiveris.omr.ui.symbol.TextFont.TEXT_FONT_NAME;
import static org.audiveris.omr.util.RegexUtil.group;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Immutable font information (generally for a word).
 *
 * @author Hervé Bitteur
 */
public class FontInfo
        extends FontAttributes
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(FontInfo.class);

    /** Optional separator in memo between attributes (if any) and point size. */
    private static final char SEPARATOR = '-';

    // Regular expression items to decode a FontInfo mnemonic

    private static final String SIZE = "size";

    private static final String sizePat = group(SIZE, "\\d+");

    private static final Pattern mnemoPattern = Pattern.compile(
            attrsPat + SEPARATOR + "?" + sizePat);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Font size, specified in printer points (1/72 inch). */
    public final int pointSize;

    /** Font name. */
    public final String fontName;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>FontInfo</code> object.
     *
     * @param isBold       True if the font is bold
     * @param isItalic     True if the font is italic
     * @param isUnderlined True if the font is underlined
     * @param isMonospaced True if the font has a fixed width
     * @param isSerif      True for fonts with serifs
     * @param isSmallcaps  True for small caps fonts
     * @param pointSize    font size in points
     * @param fontName     font name
     */
    public FontInfo (boolean isBold,
                     boolean isItalic,
                     boolean isUnderlined,
                     boolean isMonospaced,
                     boolean isSerif,
                     boolean isSmallcaps,
                     int pointSize,
                     String fontName)
    {
        super(isBold, isItalic, isUnderlined, isMonospaced, isSerif, isSmallcaps);
        this.pointSize = pointSize;
        this.fontName = fontName;
    }

    /**
     * Creates a new <code>FontInfo</code> object, with only bold and italic possible attributes.
     *
     * @param isBold    True if the font is bold
     * @param isItalic  True if the font is italic
     * @param pointSize font size in points
     * @param fontName  font name
     */
    public FontInfo (boolean isBold,
                     boolean isItalic,
                     int pointSize,
                     String fontName)
    {
        this(isBold, isItalic, false, false, false, false, pointSize, fontName);
    }

    /**
     * Create a new <code>FontInfo</code> from another one and a specific point size.
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
                org.isMonospaced,
                org.isSerif,
                org.isSmallcaps,
                pointSize,
                org.fontName);
    }

    public FontInfo (FontAttributes attrs,
                     int pointSize,
                     String fontName)
    {
        this(
                attrs.isBold,
                attrs.isItalic,
                attrs.isUnderlined,
                attrs.isMonospaced,
                attrs.isSerif,
                attrs.isSmallcaps,
                pointSize,
                fontName);
    }

    /**
     * Create a new <code>FontInfo</code> from another one and a specific font name.
     *
     * @param org      the original font info
     * @param fontName the specific font name
     */
    public FontInfo (FontInfo org,
                     String fontName)
    {
        this(
                org.isBold,
                org.isItalic,
                org.isUnderlined,
                org.isMonospaced,
                org.isSerif,
                org.isSmallcaps,
                org.pointSize,
                fontName);
    }

    /**
     * Creates a new <code>FontInfo</code> object, with no attribute set,
     * just point size and font name.
     *
     * @param pointSize font size in points
     * @param fontName  font name
     */
    public FontInfo (int pointSize,
                     String fontName)
    {
        this(false, false, false, false, false, false, pointSize, fontName);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // getAttributes //
    //---------------//
    public FontAttributes getAttributes ()
    {
        return new FontAttributes(
                isBold,
                isItalic,
                isUnderlined,
                isMonospaced,
                isSerif,
                isSmallcaps);
    }

    //-------------------//
    // getAttributesSpec //
    //-------------------//
    /**
     * Report the font attributes specification, name and size excepted.
     *
     * @return the font attributes specification
     */
    public String getAttributesSpec ()
    {
        return getAttributes().getSpec();
    }

    //----------//
    // getMnemo //
    //----------//
    /**
     * Report a very short description of font characteristics.
     * <p>
     * Format examples: "I67", "BS-45", "53".
     * <p>
     * The optional separator (-) can be present only if there is at least one attribute.
     * This is to avoid "-53" which would look like a negative value.
     *
     * @return a short description
     */
    public String getMnemo ()
    {
        final StringBuilder sb = new StringBuilder(getAttributesSpec());

        if (sb.length() > 0) {
            sb.append(SEPARATOR);
        }

        sb.append(pointSize);

        return sb.toString();
    }

    //----------//
    // getStyle //
    //----------//
    /**
     * Report an integer style value, based on the relevant attributes.
     *
     * @return the style int value
     */
    public int getStyle ()
    {
        return (isBold ? Font.BOLD : 0) | (isItalic ? Font.ITALIC : 0);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return new StringBuilder(getClass().getSimpleName()).append("{") //
                .append(fontName) //
                .append(' ').append(getMnemo()) //
                .append("}").toString();
    }

    //~ Static Methods -----------------------------------------------------------------------------

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
        return new FontInfo(false, false, false, false, false, false, fontSize, TEXT_FONT_NAME);
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
        final Matcher matcher = mnemoPattern.matcher(str);

        if (matcher.matches()) {
            final String attrs = matcher.group(ATTRS);
            final String sizeStr = matcher.group(SIZE);
            final int size = Integer.decode(sizeStr);

            if (attrs != null) {
                return new FontInfo(FontAttributes.decode(attrs), size, null);
            } else {
                return new FontInfo(size, null);
            }

        } else {
            throw new IllegalArgumentException("Invalid font mnemo " + str);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-------------//
    // JaxbAdapter //
    //-------------//
    /**
     * JAXB adapter for FontInfo.
     */
    public static class JaxbAdapter
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
