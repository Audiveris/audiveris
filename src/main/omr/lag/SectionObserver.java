//-----------------------------------------------------------------------//
//                                                                       //
//                     S e c t i o n O b s e r v e r                     //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.lag;

import omr.util.Observer;

/**
 * Interface <code>SectionObserver</code> defines an {@link Observer} for
 * {@link Section} information. We have added an update() method to cater
 * for Run entities as sell.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public interface SectionObserver
    extends Observer<Section>
{
    //--------//
    // update //
    //--------//
    /**
     * Added entry for Run entity
     *
     * @param run the Run entity to be used
     */
    void update (Run run);
}
