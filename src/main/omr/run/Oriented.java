//----------------------------------------------------------------------------//
//                                                                            //
//                              O r i e n t e d                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

/**
 * Interface <code>Oriented</code> flags an entity as having some orientation.
 *
 * @author Hervé Bitteur
 */
public interface Oriented
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report the orientation constant
     * @return HORIZONTAL or VERTICAL
     */
    Orientation getOrientation ();
}
