//----------------------------------------------------------------------------//
//                                                                            //
//                     U s e r E v e n t S u b s c r i b e r                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.selection.UserEvent;

import org.bushe.swing.event.EventSubscriber;

/**
 * Class {@code UserEventSubscriber}
 *
 * @author Hervé Bitteur
 */
public interface UserEventSubscriber
    extends EventSubscriber<UserEvent>
{
}
