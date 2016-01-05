//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F o n t I n f o                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Non-mutable font attributes (generally for a word).
 */
public class FontInfo
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(FontInfo.class);

    /** A default FontInfo instance when we absolutely need one. */
    public static final FontInfo DEFAULT = createDefault(36);

    //~ Instance fields ----------------------------------------------------------------------------
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

    //~ Constructors -------------------------------------------------------------------------------
    //
    //----------//
    // FontInfo //
    //----------//
    /**
     * Creates a new FontInfo object.
     *
     * @param isBold
     * @param isItalic
     * @param isUnderlined
     * @param isMonospace
     * @param isSerif
     * @param isSmallcaps
     * @param pointsize
     * @param fontName
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

    //~ Methods ------------------------------------------------------------------------------------
    //
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
    public static FontInfo decode (String str)
    {
        int dash = str.indexOf('-');

        if (dash == -1) {
            throw new IllegalArgumentException("Illegal font mnemo: " + str);
        }

        int size = Integer.decode(str.substring(dash + 1));

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

    //----------//
    // getMnemo //
    //----------//
    /**
     * Report a very short description of font characteristics.
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

        sb.append('-').append(pointsize);

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

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    public static class Adapter
            extends XmlAdapter<String, FontInfo>
    {
        //~ Methods --------------------------------------------------------------------------------

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
