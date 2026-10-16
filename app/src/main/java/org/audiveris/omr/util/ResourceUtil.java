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

    //~ Instance fields ----------------------------------------------------------------------------

    //~ Constructors -------------------------------------------------------------------------------

    //~ Methods ------------------------------------------------------------------------------------

    //~ Static Methods -----------------------------------------------------------------------------

    public static ResourceBundle getBundle (Class<?> classe)
    {
        final String packageName = classe.getPackageName();
        final String name = classe.getSimpleName();

        final ResourceBundle bundle = ResourceBundle.getBundle(
                packageName + "." + RESOURCES + "/" + name);

        return bundle;
    }

    /*
     * For a Java Desktop Application (like a Swing or JavaFX app), your biggest risk is
     * distribution. Your users will run your app on many different operating systems (Windows,
     * macOS, Linux), each with different default system encodings.
     * Because of this, the absolute safest approach is to use the Unicode escape sequence (\u00e9)
     * inside your .properties files, unless you explicitly force the resource bundle to load as
     * UTF-8 in your Java code.
     * Here is exactly how to handle both approaches in NetBeans for a desktop app:
     *
     * Approach 1: The "Bulletproof" Unicode Way (\u00e9)
     *
     * If you manually input \u00e9 into your NetBeans properties file, Java will load the character
     * flawlessly on any operating system, with any Java version (including Java 8), without any
     * extra code changes.
     *
     * properties
     * # src/main/resources/messages.properties
     * window.title=Mi Caf\u00e9
     * button.label=Confirm\u00e9
     * Use code with caution.
     *
     * In your desktop UI code, you just load it normally:
     *
     * java
     * ResourceBundle bundle = ResourceBundle.getBundle("messages");
     * String title = bundle.getString("window.title"); // Correctly displays "Mi Café"
     * Use code with caution.
     *
     * Approach 2: The Modern UTF-8 Way (Raw é)
     *
     * If you hate typing \u00e9 and want to type raw é directly in NetBeans, you must explicitly
     * tell your desktop app to read the bundle using UTF-8. Otherwise, a Windows user running your
     * app might see garbled text because Windows defaults to a different local encoding (like
     * Windows-1252).
     *
     * Step 1: Ensure NetBeans saves it as UTF-8
     *
     * 1. Right-click your desktop project -> Properties.
     * 2. Under Sources, verify Encoding is set to UTF-8.
     *
     *
     * Step 2: Update your Java code to force UTF-8 loading
     *
     * Use a custom ResourceBundle.Control when loading the bundle so Java ignores the old Latin-1
     * default:
     * java
     * import java.io.InputStream;
     * import java.io.InputStreamReader;
     * import java.net.URL;
     * import java.net.URLConnection;
     * import java.nio.charset.StandardCharsets;
     * import java.util.Locale;
     * import java.util.PropertyResourceBundle;
     * import java.util.ResourceBundle;
     *
     * public class LanguageManager {
     * public static ResourceBundle getUTF8Bundle(String baseName) {
     * return ResourceBundle.getBundle(baseName, ResourceBundle.Control.getControl(
     * ResourceBundle.Control.FORMAT_PROPERTIES
     * ) {
     *
     * @Override
     * public ResourceBundle newBundle(String baseName, Locale locale, String format,
     * ClassLoader loader, boolean reload) throws Exception {
     * String bundleName = toBundleName(baseName, locale);
     * String resourceName = toResourceName(bundleName, "properties");
     * URL url = loader.getResource(resourceName);
     * if (url != null) {
     * URLConnection connection = url.openConnection();
     * if (reload) { connection.setUseCaches(false); }
     * try (InputStream stream = connection.getInputStream();
     * InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
     * return new PropertyResourceBundle(reader);
     * }
     * }
     * return super.newBundle(baseName, locale, format, loader, reload);
     * }
     * });
     * }
     * }
     * Use code with caution.
     * How you use it in your UI:
     * java
     * // Now you can safely type raw "é" or é in NetBeans, and it will render perfectly on all user
     * PCs!
     * ResourceBundle bundle = LanguageManager.getUTF8Bundle("messages");
     * myJFrame.setTitle(bundle.getString("window.title"));
     * Use code with caution.
     *
     * Is your application built as a plain Ant project (the older NetBeans default) or a
     * Maven/Gradle project? Knowing this helps ensure that your final .jar compilation step
     * packages these files correctly.
     *
     *
     *
     * If your project is a Gradle Project
     *
     * Open your build.gradle file in the project root and add the following snippet to ensure the
     * Java compiler and resource processor force UTF-8:
     * groovy
     * tasks.withType(JavaCompile) {
     * options.encoding = 'UTF-8'
     * }
     *
     * tasks.withType(ProcessResources) {
     * filteringCharset = 'UTF-8'
     * }
     * Use code with caution.
     *
     * ⚠️ The Golden Caveat for Desktop Apps
     *
     * Even if you update Maven or Gradle to use UTF-8 so you can type raw é signs cleanly inside
     * the NetBeans editor, Approach 2 from the previous step is still mandatory for your
     * application's Java code.
     *
     * Because it is a desktop app, unless you load your resource files using a custom
     * ResourceBundle.Control that explicitly forces StandardCharsets.UTF_8, Java's ResourceBundle
     * engine will ignore the NetBeans settings and attempt to read the file using the local user's
     * operating system default (which is usually Windows-1252 on Western Windows machines),
     * resulting in scrambled text at runtime.
     *
     *
     */

    // Found in https://github.com/apache/netbeans/issues/4351
    // -------------------------------------------------------
    /*
     * As a side note: UTF-8 is the default for properties files loaded via PropertyResourceBundle,
     * while properties files loaded via Properties#load(InputStream) are ISO 8859-1. Due to
     * backwards compatibility, the latter is unlikely to ever change.
     * (Applications who want a
     * different encoding can use Properties#load(Reader) with an appropriate InputStreamReader.)
     */

    //~ Inner Classes ------------------------------------------------------------------------------

}
