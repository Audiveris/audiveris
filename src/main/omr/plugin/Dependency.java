//----------------------------------------------------------------------------//
//                                                                            //
//                            D e p e n d e n c y                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.plugin;

/**
 * Class <code>Dependency</code> defines predicates for enabling a command
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public enum Dependency {
    /**
     * No dependency, the action enabling is not handled
     */
    NONE,
    /**
     * Action is enabled only when a Sheet is selected
     */
    SHEET_AVAILABLE,
    /**
     * Action is enabled only when Score information is available
     * (which also requires that a Sheet is selected)
     */
    SCORE_AVAILABLE;
}
