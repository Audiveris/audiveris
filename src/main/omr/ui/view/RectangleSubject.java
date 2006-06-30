//-----------------------------------------------------------------------//
//                                                                       //
//                    R e c t a n g l e S u b j e c t                    //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui.view;

import omr.util.Subject;

import java.awt.Rectangle;

/**
 * Interface <code>RectangleSubject</code> is a specific {@link Subject} meant
 * for {@link RectangleObserver} observers
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public interface RectangleSubject
    extends Subject<RectangleSubject, RectangleObserver, Rectangle>
{
}
