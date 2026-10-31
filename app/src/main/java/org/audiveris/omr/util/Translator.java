//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T r a n s l a t o r                                      //
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

import java.util.ResourceBundle;

/**
 * Class <code>Translator</code> handles the translation and formatting of localized messages
 * for a provided class.
 *
 * @author Hervé Bitteur
 */
public class Translator
{
    //~ Instance fields ----------------------------------------------------------------------------

    protected final ResourceBundle resources; // The underlying bundle of resources

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a <code>Translator</code> instance.
     *
     * @param classe the related Java class
     */
    public Translator (Class<?> classe)
    {
        resources = ResourceUtil.getBundle(classe);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // getBundle //
    //-----------//
    /**
     * Report the ResourceBundle used by this Translator.
     *
     * @return the underlying bundle
     */
    public ResourceBundle getBundle ()
    {
        return resources;
    }

    //-------//
    // trans //
    //-------//
    /**
     * Translate and format.
     *
     * @param key       key to the pattern string
     * @param arguments elements
     * @return the formatted string
     */
    public String trans (String key,
                         Object... arguments)
    {
        return ResourceUtil.format(trans(key), arguments);
    }

    //-------//
    // trans //
    //-------//
    /**
     * Translate.
     *
     * @param key item id
     * @return the translated string
     */
    public String trans (String key)
    {
        return resources.getString(key);
    }
}
