//----------------------------------------------------------------------------//
//                                                                            //
//                               O c r L i n e                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text;

import omr.glyph.text.tesseract.LineDesc;

/**
 * Class <code>OcrLine</code> defines an non-mutable structure to report useful
 * info on one OCR-decoded line
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class OcrLine
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific value to indicate an invalid font size value */
    public static final int INVALID_FONT_SIZE = -1;

    //~ Instance fields --------------------------------------------------------

    /** Detected font size, defined in points (use -1 if not available) */
    public final int fontSize;

    /** Detected line content */
    public final String value;

    /** Detailed chars information */
    public final LineDesc lineDesc;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new OcrLine object.
     *
     * @param fontSize the detected font size, or -1 if not known
     * @param value the string ascii value
     * @param lineDesc the detailed line description
     */
    public OcrLine (int      fontSize,
                    String   value,
                    LineDesc lineDesc)
    {
        this.fontSize = fontSize;
        this.value = value;
        this.lineDesc = lineDesc;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // isFontSizeValid //
    //-----------------//
    /**
     * Report whether the font size field contains a valid value
     * @return true if font size is valid
     */
    public boolean isFontSizeValid ()
    {
        return fontSize != INVALID_FONT_SIZE;
    }
}
