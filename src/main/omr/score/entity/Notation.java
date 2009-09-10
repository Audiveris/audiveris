//----------------------------------------------------------------------------//
//                                                                            //
//                              N o t a t i o n                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.score.visitor.Visitable;

/**
 * Interface <code>Notation</code> is used to flag a measure element used
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
 *  </pre>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface Notation
    extends Visitable
{
}
