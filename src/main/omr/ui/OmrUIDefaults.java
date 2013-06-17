//----------------------------------------------------------------------------//
//                                                                            //
//                         O m r U I D e f a u l t s                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

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
    //~ Static fields/initializers ---------------------------------------------

    private static volatile OmrUIDefaults INSTANCE;

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // getInstance //
    //-------------//
    public static OmrUIDefaults getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new OmrUIDefaults();
        }

        return INSTANCE;
    }

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
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void loadFrom (File file)
            throws FileNotFoundException, IOException
    {
        String path = file.getPath();
        StringBuilder b = new StringBuilder(path);
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();

        if ((language != null) && (language.length() > 0)) {
            b.append('_')
                    .append(language);
        }

        b.append(".properties");
        file = new File(b.toString());

        if (!file.exists()) {
            file = new File(path + ".properties");
        }

        Properties p = new Properties();
        InputStream in = null;

        try {
            in = new FileInputStream(file);
            p.load(in);
            loadFrom(p);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
