//----------------------------------------------------------------------------//
//                                                                            //
//                            P l u g i n T y p e                             //
//                                                                            //
//  Copyright (C) Brenton Partridge 2007. All rights reserved.                //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.plugin;


/**
 * Used in the Plugin annotation to designate in which section of the GUI the
 * plugin should be shown.
 *
 * @author Brenton Partridge
 * @version $Id$
 */
public enum PluginType {
    /** No specific section ??? */
    DEFAULT,
    /** Score input */
    SCORE_IMPORT, 
    /** Score modification */
    SCORE_EDIT, 
    /** Score output */
    SCORE_EXPORT, 
    /** Sheet input */
    SHEET_IMPORT, 
    /** Sheet output */
    SHEET_EXPORT;
}
