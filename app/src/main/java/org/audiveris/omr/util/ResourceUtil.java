//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     R e s o u r c e U t i l                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.util;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Class <code>ResourceUtil</code> provides utilities dealing with ResourceBundle.
 *
 * @author Hervé Bitteur
 */
public abstract class ResourceUtil
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Name of the sub-folder dedicated to resources. */
    public static final String RESOURCES = "resources";

    /** Specific value to indicate lack of resource, and avoid recurring searches. */
    public static final String NO_RESOURCE = "NO_RESOURCE";

    //~ Constructors -------------------------------------------------------------------------------

    private ResourceUtil ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-----------//
    // getBundle //
    //-----------//
    /**
     * Loads the resource bundle related to a Java class, by pointing to a sibling "resources"
     * sub-folder.
     *
     * @param classe the Java class
     * @return the loaded bundle
     */
    public static ResourceBundle getBundle (Class<?> classe)
    {
        final String packageName = classe.getPackageName();
        final String name = classe.getSimpleName();

        return ResourceBundle.getBundle(packageName + "." + RESOURCES + "." + name);
    }

    //--------//
    // format //
    //--------//
    /**
     * Format a string pattern with the provided arguments.
     * This is similar to MessageFormat, except that single quotes are preserved.
     *
     * @param pattern   the formatting pattern
     * @param arguments the provided arguments
     * @return the formatted string
     */
    public static String format (String pattern,
                                 Object... arguments)
    {
        final String protectedPattern = pattern.replace("'", "''");
        return MessageFormat.format(protectedPattern, arguments);
    }
}
