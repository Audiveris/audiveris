//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L a n g u a g e                                         //
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

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.util.UriUtil;
import org.audiveris.omr.util.param.ConstantBasedParam;
import org.audiveris.omr.util.param.Param;
import static org.audiveris.omr.util.param.Param.GLOBAL_SCOPE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.swing.AbstractListModel;

/**
 * Class <code>Language</code> handles the dictionary of pre-defined language codes,
 * and the specification of codes to be used by an OCR run among the supported languages.
 * <p>
 * A language specification specifies a list of language codes like: "DEU+ENG+ITA".
 * <p>
 * It is a string formatted as LAN[+LAN]*
 *
 * @author Hervé Bitteur
 */
public abstract class Language
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Language.class);

    /** Separator in a specification. */
    public static final String SEP_CHAR = "+";

    /** Default specification (such as deu+eng+fra). */
    public static final Param<String> ocrDefaultLanguages = new ConstantBasedParam<>(
            constants.defaultSpecification,
            GLOBAL_SCOPE);

    /** Codes used when specification is empty. */
    public static final String NO_SPEC = "eng";

    /** Dictionary of all the defined languages (code / full name). */
    public static DefinedLanguages DEFINED_LANGUAGES = new DefinedLanguages();

    /** Dictionary file name. */
    private static final String LANG_FILE_NAME = "ISO639-3.xml";

    /** Collection of supported languages, lazily created. */
    private static volatile SupportedLanguages supportedLanguages;

    //~ Constructors -------------------------------------------------------------------------------

    /** Functional class, not meant to be instantiated. */
    @SuppressWarnings("unused")
    private Language ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------//
    // codeOf //
    //--------//
    /**
     * Report the code out of a list item.
     *
     * @param item the list item, such as "eng (English)"
     * @return the code, such as "eng"
     */
    public static String codeOf (String item)
    {
        final String[] tokens = item.split(" ");

        return tokens[0];
    }

    //---------//
    // codesOf //
    //---------//
    /**
     * Convert a language specification (DEU+FRA+ITA) to a sequence of codes [DEU, FRA, ITA].
     *
     * @param spec the language specification to parse
     * @return the sequence of codes.
     */
    public static List<String> codesOf (String spec)
    {
        final String[] tokens = spec.split("\\" + SEP_CHAR);

        return Arrays.asList(tokens);
    }

    //-----------------------//
    // getSupportedLanguages //
    //-----------------------//
    /**
     * Lazy retrieval of the supported languages.
     *
     * @return the collection of supported languages
     */
    public static SupportedLanguages getSupportedLanguages ()
    {
        if (supportedLanguages == null) {
            supportedLanguages = new SupportedLanguages();
        }

        return supportedLanguages;
    }

    //--------//
    // specOf //
    //--------//
    /**
     * Convert a collection of codes to a specification string.
     *
     * @param collection the collection of codes
     * @return the resulting specification
     */
    public static String specOf (Collection<String> collection)
    {
        return collection.stream().map(s -> codeOf(s)).collect(Collectors.joining(SEP_CHAR));
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.String defaultSpecification = new Constant.String(
                "eng",
                "OCR language(s)");
    }

    //-----------//
    // ListModel //
    //-----------//
    /**
     * A JList model to support the manual selection among the supported languages.
     */
    public static class ListModel
            extends AbstractListModel<String>
    {
        /**
         * Report a string built as: "code (full name)".
         *
         * @param index index in codesList
         * @return the related string, such as "fra (French)"
         */
        @Override
        public String getElementAt (int index)
        {
            final String code = getSupportedLanguages().getCode(index);
            final String fullName = DEFINED_LANGUAGES.fullNameOf(code);

            return code + ((fullName != null) ? " (" + fullName + ")" : "");
        }

        /**
         * Report the number of supported languages.
         *
         * @return count of supported codes
         */
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
    }

    //------------------//
    // DefinedLanguages //
    //------------------//
    /**
     * Handles the collection of all possible languages as defined by the ISO639-3 norm.
     */
    public static class DefinedLanguages
    {
        /** Map of language code -> language full name. */
        protected final SortedMap<String, String> codes = new TreeMap<>();

        public DefinedLanguages ()
        {
            // Build the map of all possible codes
            Properties langNames = new Properties();
            URI uri = UriUtil.toURI(WellKnowns.RES_URI, LANG_FILE_NAME);

            try (InputStream input = uri.toURL().openStream()) {
                langNames.loadFromXML(input);

                for (String code : langNames.stringPropertyNames()) {
                    codes.put(code, langNames.getProperty(code, code));
                }
            } catch (IOException ex) {
                logger.error("Error loading " + uri, ex);
            }
        }

        /**
         * Report the number of defined codes.
         *
         * @return the codes number
         */
        public int getSize ()
        {
            return codes.size();
        }

        /**
         * Report the full language name mapped to a language code.
         *
         * @param code the language code, such as "eng"
         * @return the corresponding language full name, such as "English", or null if unknown
         */
        public String fullNameOf (String code)
        {
            return codes.get(code);
        }
    }

    //--------------------//
    // SupportedLanguages //
    //--------------------//
    /**
     * Handles the collection of OCR languages available locally.
     */
    public static class SupportedLanguages
    {
        /** The sorted sequence of codes. */
        private final List<String> codesList;

        private SupportedLanguages ()
        {
            final SortedSet<String> supported = OcrUtil.getOcr().getSupportedLanguages();
            codesList = new ArrayList<>(supported);
        }

        /**
         * Add a language code to the collection.
         *
         * @param code the code to add
         */
        public void addCode (String code)
        {
            codesList.add(code);
            Collections.sort(codesList);
        }

        /**
         * Report the code at specified index.
         *
         * @param index the specified index
         * @return the code
         */
        public String getCode (int index)
        {
            return codesList.get(index);
        }

        /**
         * Report whether the collection contains the provided language code.
         *
         * @param code the language code to check
         * @return true if so
         */
        public boolean contains (String code)
        {
            return codesList.contains(code);
        }

        /**
         * Report the current size of the collection.
         *
         * @return the codes count
         */
        public int getSize ()
        {
            return codesList.size();
        }

        /**
         * Report the indices in codes collection that correspond to the provided spec.
         *
         * @param spec the codes specification
         * @return the corresponding indices
         */
        public int[] indicesOf (String spec)
        {
            if (spec.trim().isEmpty()) {
                return new int[0];
            } else {
                final List<String> list = codesOf(spec);
                final int[] ints = new int[list.size()];

                for (int i = 0; i < ints.length; i++) {
                    final String code = list.get(i);
                    ints[i] = codesList.indexOf(code);
                }

                return ints;
            }
        }
    }
}
