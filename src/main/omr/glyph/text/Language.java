//----------------------------------------------------------------------------//
//                                                                            //
//                              L a n g u a g e                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import java.io.*;
import java.util.*;

/**
 * Class <code>Language</code> handles the collection of language codes with
 * their related full name, as well as the default language.
 *
 * <p>Note: This is implemented as a (sorted) map, since a compiled enum would
 * not provide the ability to add new items dynamically.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Language
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Language.class);

    /** Map of language code -> language full name */
    private static SortedMap<String, String> knowns = new TreeMap<String, String>();

    static {
        try {
            // Retrieve correspondences between codes and names
            Properties      langNames = new Properties();
            FileInputStream fis = new FileInputStream(
                new File(Main.getConfigFolder(), "ISO639-3.xml"));
            langNames.loadFromXML(fis);
            fis.close();

            for (String code : langNames.stringPropertyNames()) {
                knowns.put(code, langNames.getProperty(code, code));
            }
        } catch (IOException ex) {
            logger.severe("Missing config/ISO639-3.xml file", ex);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // setDefaultLanguage //
    //--------------------//
    /**
     * Assign the new global default language code
     * @param code global default language code
     */
    public static void setDefaultLanguage (String code)
    {
        logger.info("Default language is now '" + code + "'");
        constants.defaultLanguageCode.setValue(code);
    }

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
        return knowns.get(code);
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
