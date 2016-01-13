//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            C h o r d D y n a m i c s R e l a t i o n                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ChordDynamicsRelation} represents a support relation between a chord
 * and a dynamics element.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "chord-dynamics")
public class ChordDynamicsRelation
        extends AbstractSupport
{
}
