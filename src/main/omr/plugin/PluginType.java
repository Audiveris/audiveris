//  PluginType
//
//  Copyright (C) Brenton Partridge 2007.
//  All rights reserved.
//  This software is released under the GNU General Public License.
//  Contact herve.bitteur@laposte.net to report bugs & suggestions.
//----------------------------------------------------------------------------//
package omr.plugin;

/**
 * Used in the Plugin annotation to designate in
 * which section of the GUI the plugin should be shown.
 * 
 * @author Brenton Partridge
 * @version $Id$
 */
public enum PluginType
{
	DEFAULT,
	SCORE_EXPORT,
	SCORE_EDIT,
	SCORE_IMPORT,
	SHEET_IMPORT,
	SHEET_EXPORT
}
