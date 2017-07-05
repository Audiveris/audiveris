//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A l i a s P a t t e r n s                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.util.UriUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code AliasPatterns} allows to apply patterns to file names to assign them an
 * alias.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "alias-patterns")
public class AliasPatterns
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AliasPatterns.class);

    private static final String ALIAS_PATTERNS_FILENAME = "alias-patterns.xml";

    //~ Instance fields ----------------------------------------------------------------------------
    private final List<Pattern> patterns = loadAliasPatterns();

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getAlias //
    //----------//
    /**
     * Try to retrieve an alias for the provided name, by applying registered patterns.
     *
     * @param name the full name provided
     * @return the first alias found, or null if none
     */
    public String getAlias (String name)
    {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(name);

            if (matcher.find()) {
                String alias = matcher.group(1);

                if ((alias != null) && !alias.isEmpty() && !alias.equals(name)) {
                    return alias;
                }
            }
        }

        return null;
    }

    //------------------//
    // useAliasPatterns //
    //------------------//
    public static boolean useAliasPatterns ()
    {
        return constants.useAliasPatterns.isSet();
    }

    //-------------------//
    // loadAliasPatterns //
    //-------------------//
    /**
     * Try to load alias patterns.
     * <p>
     * User 'config' is first searched for patterns file, followed by program 'res'.
     *
     * @return the list of patterns found, perhaps empty
     */
    private List<Pattern> loadAliasPatterns ()
    {
        final List<Pattern> patternList = new ArrayList<Pattern>();

        if (useAliasPatterns()) {
            URI[] uris = new URI[]{
                WellKnowns.CONFIG_FOLDER.resolve(ALIAS_PATTERNS_FILENAME).toUri()
                .normalize(),
                UriUtil.toURI(WellKnowns.RES_URI, ALIAS_PATTERNS_FILENAME)
            };

            for (int i = 0; i < uris.length; i++) {
                URI uri = uris[i];

                try {
                    URL url = uri.toURL();

                    // Retrieve the raw strings
                    JAXBContext jaxbContext = JAXBContext.newInstance(Strings.class);
                    InputStream input = url.openStream();
                    Unmarshaller um = jaxbContext.createUnmarshaller();
                    Strings strings = (Strings) um.unmarshal(input);
                    input.close();

                    List<String> stringList = strings.list;

                    // Compile strings into patterns
                    if (!stringList.isEmpty()) {
                        logger.info("Alias patterns: {}", stringList);

                        for (String raw : stringList) {
                            patternList.add(Pattern.compile(raw));
                        }
                    }
                } catch (IOException ex) {
                    if (i != 0) {
                        // First item (user) is optional
                        logger.error("Mandatory file not found {}", uri, ex);
                    }
                } catch (JAXBException ex) {
                    logger.warn("Error unmarshalling " + uri, ex);
                }
            }
        }

        return patternList;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean useAliasPatterns = new Constant.Boolean(
                true,
                "Should we apply alias patterns on input names?");
    }

    //---------//
    // Strings //
    //---------//
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "alias-patterns")
    private static class Strings
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** List of patterns on input names. */
        @XmlElement(name = "pattern")
        private List<String> list = new ArrayList<String>();

        //~ Constructors ---------------------------------------------------------------------------
        /** No-arg constructor meant for JAXB. */
        private Strings ()
        {
        }
    }
}
