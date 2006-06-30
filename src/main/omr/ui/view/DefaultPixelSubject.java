//--------------------------------------------------------------------------//
//                                                                          //
//                  D e f a u l t P i x e l S u b j e c t                   //
//                                                                          //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.             //
//  This software is released under the terms of the GNU General Public     //
//  License. Please contact the author at herve.bitteur@laposte.net         //
//  to report bugs & suggestions.                                           //
//--------------------------------------------------------------------------//

package omr.ui.view;

import omr.util.DefaultSubject;
import omr.util.Subject;
import omr.sheet.PixelPoint;

/**
 * Class <code>DefaultPixelSubject</code> is an implementation of the
 * specific {@link Subject} meant for {@link PixelObserver} observers
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class DefaultPixelSubject
    extends DefaultSubject<PixelSubject, PixelObserver, PixelPoint>
{
}
