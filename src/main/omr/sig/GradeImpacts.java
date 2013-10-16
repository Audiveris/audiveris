//----------------------------------------------------------------------------//
//                                                                            //
//                           G r a d e I m p a c t s                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

/**
 * Interface {@code GradeImpacts} defines data that impact a resulting
 * grade value.
 *
 * @author Hervé Bitteur
 */
public interface GradeImpacts
{
    //~ Methods ----------------------------------------------------------------

    double computeGrade ();
}
