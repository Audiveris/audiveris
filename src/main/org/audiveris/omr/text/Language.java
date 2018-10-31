//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L a n g u a g e                                         //
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

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.util.UriUtil;
import org.audiveris.omr.util.param.ConstantBasedParam;
import org.audiveris.omr.util.param.Param;
import org.audiveris.omr.util.param.StringParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Class {@code Language} handles the collection of language codes with their related
 * full name, as well as the default language specification.
 * <p>
 * A language specification specifies a list of languages.
 * It is a string formatted as LAN[+LAN]
 * <p>
 * Note: Supported languages are implemented as a (sorted) map, since a compiled enum would not
 * provide the ability to add new items at run time.
 *
 * @author Hervé Bitteur
 */
public class Language
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Language.class);

    /** Languages file name. */
    private static final String LANG_FILE_NAME = "ISO639-3.xml";

    /** Separator in a specification. */
    public static final String SEP_CHAR = "+";

    /** Default language specification (such as deu+eng+fra). */
    public static final Param<String> ocrDefaultLanguages = new ConstantBasedParam<String, Constant.String>(
            constants.defaultSpecification);

    /** Language used when specification is empty. */
    private static final String NO_SPEC = "eng";

    /** Collection of supported languages, lazily created. */
    private static volatile SupportedLanguages supportedLanguages;

    //~ Constructors -------------------------------------------------------------------------------
    /** Not meant to be instantiated. */
    private Language ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------------//
    // getSupportedLanguages //
    //-----------------------//
    private static SupportedLanguages getSupportedLanguages ()
    {
        if (supportedLanguages == null) {
            supportedLanguages = new SupportedLanguages();
        }

        return supportedLanguages;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // ListModel //
    //-----------//
    /**
     * A JList model to support manual handling of language codes).
     */
    public static class ListModel
            extends AbstractListModel<String>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public String getElementAt (int index)
        {
            return getSupportedLanguages().getElementAt(index);
        }

        @Override
        public int getSize ()
        {
            return getSupportedLanguages().getSize();
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
            return getSupportedLanguages().indicesOf(spec);
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
            return getSupportedLanguages().specOf(list);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.String defaultSpecification = new Constant.String(
                "deu+eng+fra",
                "OCR language(s)");
    }

    //---------------------//
    // OcrDefaultLanguages //
    //---------------------//
    private static class OcrDefaultLanguages
            extends StringParam
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.String constant = constants.defaultSpecification;

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String getSourceValue ()
        {
            return constant.getSourceValue();
        }

        @Override
        public String getSpecific ()
        {
            if (constant.isSourceValue()) {
                return null;
            } else {
                return constant.getValue();
            }
        }

        @Override
        public String getValue ()
        {
            return constant.getValue();
        }

        @Override
        public boolean isSpecific ()
        {
            return !constant.isSourceValue();
        }

        @Override
        public boolean setSpecific (String specific)
        {
            if (!getValue().equals(specific)) {
                if (specific == null) {
                    constant.resetToSource();
                    logger.info(
                            "Default OCR language specification is reset to \"{}\"",
                            constant.getSourceValue());
                } else {
                    constant.setStringValue(specific);
                    logger.info("Default OCR language specification is now \"{}\"", specific);
                }

                return true;
            }

            return false;
        }
    }

    //--------------------//
    // SupportedLanguages //
    //--------------------//
    /**
     * Handles the collection of supported languages.
     */
    private static class SupportedLanguages
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Map of language code -> language full name. */
        private final SortedMap<String, String> codes = new TreeMap<String, String>();

        /** Convenient sequence of codes, parallel to sorted map. */
        private final List<String> codesList;

        //~ Constructors ---------------------------------------------------------------------------
        public SupportedLanguages ()
        {
            // Build the map of all possible codes
            Properties langNames = new Properties();
            URI uri = UriUtil.toURI(WellKnowns.RES_URI, LANG_FILE_NAME);

            try {
                InputStream input = null;

                try {
                    input = uri.toURL().openStream();
                    langNames.loadFromXML(input);

                    for (String code : langNames.stringPropertyNames()) {
                        codes.put(code, langNames.getProperty(code, code));
                    }
                } finally {
                    if (input != null) {
                        input.close();
                    }
                }
            } catch (Throwable ex) {
                logger.error("Error loading " + uri, ex);
            }

            // Now, keep only the supported codes
            // TODO: Protect against no OCR!
            Set<String> supported = OcrUtil.getOcr().getLanguages();
            codes.keySet().retainAll(supported);

            // Create parallel list of codes
            codesList = new ArrayList<String>(codes.keySet());
        }

        //~ Methods --------------------------------------------------------------------------------
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

        /**
         * Report a string built as: "code (full name)".
         *
         * @param code provided code, such as "fra"
         * @return the related string, such as "fra (French)"
         */
        public String getElementAt (int index)
        {
            final String code = codesList.get(index);
            final String fullName = nameOf(code);

            if (fullName != null) {
                return code + " (" + fullName + ")";
            } else {
                return code;
            }
        }

        public int getSize ()
        {
            return codesList.size();
        }

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

        public String specOf (Collection<String> list)
        {
            StringBuilder sb = new StringBuilder();

            for (String item : list) {
                if (sb.length() > 0) {
                    sb.append(SEP_CHAR);
                }

                sb.append(codeOf(item));
            }

            if (sb.length() == 0) {
                return null;
            } else {
                return sb.toString();
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
         * Report the full language name mapped to a language code.
         *
         * @param code the language code, such as "eng"
         * @return the corresponding language full name, such as "English",
         *         or null if unknown
         */
        private String nameOf (String code)
        {
            return codes.get(code);
        }
    }
}
