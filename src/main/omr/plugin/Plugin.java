//----------------------------------------------------------------------------//
//                                                                            //
//                                P l u g i n                                 //
//                                                                            //
//  Copyright (C) Brenton Partridge 2007. All rights reserved.                //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.plugin;

import java.lang.annotation.*;

/**
 * Annotation <code>Plugin</code> designates that a class is a valid Audiveris
 * plugin.
 *
 * <p>The class must extend javax.swing.Action, have a no-argument constructor,
 * and specify within the Plugin annotation what type it is.
 *
 * <p>It will be represented in the UI as a toolbar button and a menu item.
 *
 * @author Brenton Partridge
 * @version $Id$
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Plugin {PluginType type() default PluginType.DEFAULT;
}
