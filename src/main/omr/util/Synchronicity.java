//----------------------------------------------------------------------------//
//                                                                            //
//                         S y n c h r o n i c i t y                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;


/**
 * Used to indicate whether a processing must be done synchronously or
 * asynchronously with respect to its caller.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public enum Synchronicity {
    /** Asynchronous execution */
    ASYNC,
    /** Synchronous execution */
    SYNC;
}
