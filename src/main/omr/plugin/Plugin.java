//  Plugin
//
//  Copyright (C) Brenton Partridge 2007.
//  All rights reserved.
//  This software is released under the GNU General Public License.
//  Contact herve.bitteur@laposte.net to report bugs & suggestions.
//----------------------------------------------------------------------------//
package omr.plugin;

import java.lang.annotation.*;

/**
 * Designates that a class is a valid Audiveris plugin.
 * The class must extend javax.swing.Action, have a 
 * no-argument constructor, and
 * specify within the Plugin annotation what type it is.
 * It will be represented in the UI as a toolbar button and
 * a menu item.
 * 
 * @author Brenton Partridge
 * @version $Id$
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Plugin 
{ 
	PluginType type() default PluginType.DEFAULT; 
}