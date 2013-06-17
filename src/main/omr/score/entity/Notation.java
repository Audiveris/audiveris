//----------------------------------------------------------------------------//
//                                                                            //
//                              N o t a t i o n                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.score.visitor.Visitable;

/**
 * Interface {@code Notation} is used to flag a measure element used
 * as a (note related) notation.
 * This should apply to:
 * <pre>
 *  tied                specific
 *  slur                specific
 *  tuplet              nyi, to be done soon
 *  glissando           nyi
 *  slide               nyi
 *  ornaments           standard
 *  technical           nyi
 *  articulations       nyi
 *  dynamics            nyi
 *  fermata             nyi
 *  arpeggiate          standard
 *  non-arpeggiate      nyi
 *  accidental-mark     nyi
 *  other-notation      nyi
 * </pre>
 *
 * @author Hervé Bitteur
 */
public interface Notation
        extends Visitable
{
}
