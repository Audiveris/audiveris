//----------------------------------------------------------------------------//
//                                                                            //
//                             D i r e c t i o n                              //
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
 * Interface {@code Direction} is the basis for all variants of
 * direction indications.
 * This should apply to:
 * <pre>
 * rehearsal                    nyi
 * segno                        standard
 * words                        nyi
 * coda                         standard
 * wedge                        standard
 * dynamics                     standard (a dynamic can also be a notation)
 * dashes                       nyi
 * bracket                      nyi
 * pedal                        standard
 * metronome                    nyi
 * octave-shift                 nyi
 * harp-pedals                  nyi
 * damp                         nyi
 * damp-all                     nyi
 * eyeglasses                   nyi
 * scordatura                   nyi
 * image                        nyi
 * accordion-registration       nyi
 * other-direction              nyi
 * </pre>
 *
 * <p>For some directions (such as wedge, dashes, pedal), we may have two
 * "events": the starting event and the stopping event. Both will trigger the
 * creation of a Direction instance, the difference being made by the "start"
 * boolean.
 *
 * @author Hervé Bitteur
 */
public interface Direction
        extends Visitable
{
}
