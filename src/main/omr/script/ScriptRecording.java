//----------------------------------------------------------------------------//
//                                                                            //
//                       S c r i p t R e c o r d i n g                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;


/**
 * Class <code>ScriptRecording</code> specifies whether an action must be
 * recorded in the current sheet script
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public enum ScriptRecording {
    /** Action must be recorded */
    RECORDING,
    /** Action must not be recorded */
    NO_RECORDING;
}
