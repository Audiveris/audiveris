//----------------------------------------------------------------------------//
//                                                                            //
//                              F o n t I n f o                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

/**
 * Non-mutable font attributes (generally for a word).
 */
public class FontInfo
{

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

    //----------//
    // FontInfo //
    //----------//
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(fontName).append(' ');

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
        sb.append("}");
        return sb.toString();
    }
}
