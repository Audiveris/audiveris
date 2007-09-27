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

import javax.swing.JMenuItem;

/**
 * Annotation <code>Plugin</code> designates that a class is a valid Audiveris
 * plugin.
 *
 * <p>The class must implement javax.swing.Action and have a no-argument 
 * constructor to be instantiatabled. 
 *
 * <p>It will be represented in the UI as a menu item, and possibly as a tool 
 * bar button.
 * 
 * <p>The <code>Plugin</code> annotation allows to specify a UI section type,
 * a dependency to enable or disable the action, a specific class for the 
 * hosting menu item, and whether a tool bar button must be generated.
 *
 * @author Brenton Partridge
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Plugin {
    /**
     * Which UI section should host this action
     */
    PluginType type() default PluginType.DEFAULT;
    /**
     * How the action enabling should be handled
     */
    Dependency dependency() default Dependency.NONE;
    /**
     * Which precise kind of menu item should be generated for this action
     */
    Class<?extends JMenuItem> item() default JMenuItem.class;
    /**
     * Should a toolbar button be generated for this action
     */
    boolean onToolbar() default false;
}
