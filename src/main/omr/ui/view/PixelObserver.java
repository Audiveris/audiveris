//-----------------------------------------------------------------------//
//                                                                       //
//                       P i x e l O b s e r v e r                       //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui.view;

import omr.sheet.PixelPoint;
import omr.util.Observer;

/**
 * Interface <code>PixelObserver</code> defines an {@link Observer} for
 * pixel Point information.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public interface PixelObserver
    extends Observer<PixelPoint>
{
}
