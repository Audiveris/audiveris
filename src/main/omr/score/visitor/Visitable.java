//----------------------------------------------------------------------------//
//                                                                            //
//                             V i s i t a b l e                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.visitor;


/**
 * Interface <code>Visitable</code> must be implemented by any node to be
 * visited in the Score hierarchy
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface Visitable
{
    //~ Methods ----------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    /**
     * General entry for any visiting
     *
     * @param visitor concrete visitor object to define the actual processing
     * @return true if children must be visited also, false otherwise.
     *              <b>Nota</b>: Unless there is a compelling reason, it's safer
     *              to return true to let the visitor work normally.
     */
    boolean accept (ScoreVisitor visitor);
}
