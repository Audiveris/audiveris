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

import org.slf4j.Logger; import org.slf4j.LoggerFactory;

import omr.util.Param;
import omr.util.UriUtil;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.AbstractListModel;

/**
 * Class {@code Language} handles the collection of language codes with
 * their related full name, as well as the default language
 * specification.
 *
 * <p>A language specification specifies a list of languages.
 * It is a string formatted as LAN[+LAN]*
 *
 * <p>Note: languages are implemented as a (sorted) map, since a compiled enum
 * would not provide the ability to add new items at run time.
 *
 * @author Hervé Bitteur
 */
public class Language
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Language.class);

    /** Languages file name. */
    private static final String LANG_FILE_NAME = "ISO639-3.xml";

    /** Separator in a specification. */
    public static final String SEP_CHAR = "+";

    /** Default language specification (such as ENG+DEU+ITA). */
    public static final Param<String> defaultSpecification = new Default();

    /** Language used when specification is empty. */
    private static final String NO_SPEC = "eng";

    /** Model usable in a JList, lazily created. */
    private static LanguageModel model;

    //~ Constructors -----------------------------------------------------------
    //
    //----------//
    // Language //
    //----------//
    /** Not meant to be instantiated */
    private Language ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //
    //----------//
    // getModel //
    //----------//
    public static LanguageModel getModel ()
    {
        if (model == null) {
            model = new LanguageModel();
        }
        
        return model;
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.String defaultSpecification = new Constant.String(
                NO_SPEC,
                "List of 3-letter codes, separated by '+'");

    }

    //---------//
    // Default //
    //---------//
    private static class Default
            extends Param<String>
    {

        @Override
        public String getSpecific ()
        {
            return constants.defaultSpecification.getValue();
        }

        @Override
        public boolean setSpecific (String specific)
        {
            if (!getSpecific().equals(specific)) {
                constants.defaultSpecification.setValue(specific);
                logger.info("Default language specification is now ''{}''",
                        specific);

                return true;
            }

            return false;
        }
    }

    //---------------//
    // LanguageModel //
    //---------------//
    /**
     * A swing JList model to support manual handling of language codes.
     */
    public static class LanguageModel
            extends AbstractListModel<String>
    {

        /** Map of language code -> language full name. */
        private final SortedMap<String, String> codes = new TreeMap<>();

        /** Convenient sequence of codes, parallel to sorted map. */
        private final List<String> codesList;

        public LanguageModel ()
        {
            // Build the map of all possible codes
            Properties langNames = new Properties();
            URI uri = UriUtil.toURI(WellKnowns.RES_URI,LANG_FILE_NAME);

            try (InputStream input = uri.toURL().openStream()) {
                langNames.loadFromXML(input);
                for (String code : langNames.stringPropertyNames()) {
                    codes.put(code, langNames.getProperty(code, code));
                }
            } catch (Throwable ex) {
                logger.error("Error loading " + uri, ex);
            }

            // Now, keep only the supported codes
            // TODO: Protect against no OCR!
            Set<String> supported = TextBuilder.getOcr().getLanguages();
            codes.keySet().retainAll(supported);

            // Create parallel list of codes
            codesList = new ArrayList<>(codes.keySet());
        }

        @Override
        public int getSize ()
        {
            return codesList.size();
        }

        @Override
        public String getElementAt (int index)
        {
            return itemOf(codesList.get(index));
        }

        /**
         * Report the array of indices for the spec codes.
         *
         * @param spec the provided spec
         * @return the array of indices in the model.
         *         If the spec is empty, an empty int array is returned
         */
        public int[] indicesOf (String spec)
        {
            if (spec.trim().isEmpty()) {
                return new int[0];
            } else {
                List<String> list = codesOf(spec);
                int[] ints = new int[list.size()];

                for (int i = 0; i < ints.length; i++) {
                    String code = list.get(i);
                    ints[i] = codesList.indexOf(code);
                }

                return ints;
            }
        }

        /**
         * Report the full language name mapped to a language code.
         *
         * @param code the language code, such as "eng"
         * @return the corresponding language full name, such as "English",
         *         or null if unknown
         */
        public String nameOf (String code)
        {
            return codes.get(code);
        }

        /**
         * Report a list item built as: "code (full name)".
         *
         * @param code provided code, such as "fra"
         * @return the related list item, such as "fra (French)"
         */
        public String itemOf (String code)
        {
            String fullName = nameOf(code);

            if (fullName != null) {
                return code + " (" + fullName + ")";
            } else {
                return code;
            }
        }

        /**
         * Convert a language specification (DEU+FRA+ITA) to a sequence
         * of codes [DEU, FRA, ITA].
         *
         * @param spec the language specification to parse
         * @return the sequence of codes.
         */
        private List<String> codesOf (String spec)
        {
            final String[] tokens = spec.split("\\" + SEP_CHAR);
            return Arrays.asList(tokens);
        }

        /**
         * Build the spec string out of the provided sequence of codes.
         *
         * @param list the provided codes
         * @return the resulting specification string.
         *         The "eng" string is returned if the provided list is empty.
         */
        public String specOf (Collection<String> list)
        {
            StringBuilder sb = new StringBuilder();

            for (String item : list) {
                if (sb.length() > 0) {
                    sb.append(Language.SEP_CHAR);
                }
                sb.append(model.codeOf(item));
            }

            if (sb.length() > 0) {
                return sb.toString();
            } else {
                return NO_SPEC;
            }
        }

        /**
         * Report the code out of a list item.
         *
         * @param item the list item, such as "eng (English)"
         * @return the code, such as "eng"
         */
        public String codeOf (String item)
        {
            return item.substring(0, 3);
        }
    }
}
