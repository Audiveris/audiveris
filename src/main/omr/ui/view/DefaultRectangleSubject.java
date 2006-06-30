//-----------------------------------------------------------------------//
//                                                                       //
//             D e f a u l t R e c t a n g l e S u b j e c t             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui.view;

import omr.util.DefaultSubject;
import omr.util.Subject;

import java.awt.Rectangle;

/**
 * Class <code>DefaultRectangleSubject</code> is an implementation of the
 * specific {@link Subject} meant for {@link RectangleObserver} observers
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class DefaultRectangleSubject
    extends DefaultSubject<RectangleSubject, RectangleObserver, Rectangle>
{
}
