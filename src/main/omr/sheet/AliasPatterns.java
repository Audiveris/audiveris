//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A l i a s P a t t e r n s                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
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
        if (patterns == null) {
            return null;
        }

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
    private List<Pattern> loadAliasPatterns ()
    {
        if (useAliasPatterns()) {
            try {
                // Retrieve the raw strings
                URL url = WellKnowns.CONFIG_FOLDER.resolve("alias-patterns.xml").toUri()
                        .normalize().toURL();
                JAXBContext jaxbContext = JAXBContext.newInstance(Strings.class);
                Unmarshaller um = jaxbContext.createUnmarshaller();
                InputStream input = url.openStream();
                Strings strings = (Strings) um.unmarshal(input);
                List<String> stringList = strings.list;

                // Compile strings into patterns
                if (!stringList.isEmpty()) {
                    logger.info("Alias patterns: {}", stringList);

                    List<Pattern> patternList = new ArrayList<Pattern>();

                    for (String raw : stringList) {
                        patternList.add(Pattern.compile(raw));
                    }

                    return patternList;
                }
            } catch (Exception ex) {
                logger.warn("Error loading alias patterns " + ex, ex);
            }
        }

        return null;
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
