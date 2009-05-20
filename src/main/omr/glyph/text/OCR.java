//----------------------------------------------------------------------------//
//                                                                            //
//                                   O C R                                    //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface <code>OCR</code> encapsulates the interaction with an OCR engine.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface OCR
{
    //~ Methods ----------------------------------------------------------------

    //-----------------------//
    // getSupportedLanguages //
    //-----------------------//
    /**
     * Report the collection of supported languages, as a map from language
     * 3-letter code to language name
     * @return the map code -> name
     */
    Map<String, String> getSupportedLanguages ();

    //-----------//
    // recognize //
    //-----------//
    /**
     * Launch the recognition of the provided image, whose language is specified
     *
     * @param image the provided textual image
     * @param languageCode the code of the (dominant?) language of the text,
     * or null if this language is unknown
     * @return a list of OcrLine instances
     * @throws IOException
     * @throws InterruptedException
     */
    List<OcrLine> recognize (BufferedImage image,
                             String        languageCode)
        throws IOException, InterruptedException;
}
