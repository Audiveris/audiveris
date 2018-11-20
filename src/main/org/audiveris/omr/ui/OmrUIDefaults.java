//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   O m r U I D e f a u l t s                                    //
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
package org.audiveris.omr.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.swing.KeyStroke;
import javax.swing.UIDefaults;

/**
 * Class {@code OmrUIDefaults} handles all the user interface defaults for
 * the OMR application
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 */
public class OmrUIDefaults
        extends UIDefaults
{

    //------------//
    // getKeyCode //
    //------------//
    /**
     * Report the numeric key code for a given description string
     *
     * @param key the key description
     * @return the key code
     */
    public Integer getKeyCode (String key)
    {
        KeyStroke ks = getKeyStroke(key);

        return (ks != null) ? ks.getKeyCode() : null;
    }

    //--------------//
    // getKeyStroke //
    //--------------//
    /**
     * Report the keyboard action described by a given string
     *
     * @param key the key description
     * @return the keyboard action
     */
    public KeyStroke getKeyStroke (String key)
    {
        return KeyStroke.getKeyStroke(getString(key));
    }

    /**
     * Load UI strings from a Properties object.
     *
     * @param properties properties
     */
    public void loadFrom (Properties properties)
    {
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            this.put(e.getKey(), e.getValue());
        }
    }

    /**
     * Load UI strings from a properties file (.properties).
     *
     * @param file properties file path without locale or country information
     *             or .properties extension
     * @throws FileNotFoundException if the specified file could not be found
     * @throws IOException           if the specified file could be read
     */
    public void loadFrom (File file)
            throws FileNotFoundException,
                   IOException
    {
        String path = file.getPath();
        StringBuilder b = new StringBuilder(path);
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();

        if ((language != null) && (language.length() > 0)) {
            b.append('_').append(language);
        }

        b.append(".properties");
        file = new File(b.toString());

        if (!file.exists()) {
            file = new File(path + ".properties");
        }

        Properties p = new Properties();

        try (InputStream in = new FileInputStream(file)) {
            p.load(in);
            loadFrom(p);
        }
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class in application.
     *
     * @return the instance
     */
    public static OmrUIDefaults getInstance ()
    {
        return LazySingleton.INSTANCE;
    }

    //---------------//
    // LazySingleton //
    //---------------//
    private static class LazySingleton
    {

        static final OmrUIDefaults INSTANCE = new OmrUIDefaults();
    }

}
