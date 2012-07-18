//----------------------------------------------------------------------------//
//                                                                            //
//                              L a n g u a g e                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.text.tesseract.TesseractOCR;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class {@code Language} handles the collection of language codes with
 * their related full name, as well as the default language.
 *
 * <p>Note: This is implemented as a (sorted) map, since a compiled enum would
 * not provide the ability to add new items dynamically.
 *
 * @author Hervé Bitteur
 */
public class Language
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Language.class);

    /** Languages file name */
    private static final String LANG_FILE_NAME = "ISO639-3.xml";

    /** Map of language code -> language full name */
    private static final SortedMap<String, String> codes = new TreeMap<>();

    static {
        File inputFile = new File(WellKnowns.RES_FOLDER, LANG_FILE_NAME);
        Properties langNames = new Properties();
        
        try (InputStream input = new FileInputStream(inputFile)) {
            langNames.loadFromXML(input);
            for (String code : langNames.stringPropertyNames()) {
                codes.put(code, langNames.getProperty(code, code));
            }
        } catch (Throwable ex) {
            logger.severe("Error loading " + inputFile, ex);
        }
    }
    
    //~ Constructors -----------------------------------------------------------
    
    /** Not meant to be instantiated */
    private Language ()
    {
    }    

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // getDefaultLanguage //
    //--------------------//
    /**
     * Report the global default language code
     * @return the global default language code
     */
    public static String getDefaultLanguage ()
    {
        return constants.defaultLanguageCode.getValue();
    }

    //--------//
    // nameOf //
    //--------//
    /**
     * Report the language name mapped to a language code
     * @param code the language code
     * @return the language full name, or null if unknown
     */
    public static String nameOf (String code)
    {
        return codes.get(code);
    }

    //--------------------//
    // setDefaultLanguage //
    //--------------------//
    /**
     * Assign the new global default language code
     * @param code global default language code
     */
    public static void setDefaultLanguage (String code)
    {
        constants.defaultLanguageCode.setValue(code);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.String defaultLanguageCode = new Constant.String(
            "eng",
            "3-letter code for the default sheet language");
    }

}
