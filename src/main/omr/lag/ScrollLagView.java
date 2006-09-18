//----------------------------------------------------------------------------//
//                                                                            //
//                         S c r o l l L a g V i e w                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.lag;

import omr.ui.view.ScrollView;

import omr.util.Logger;

/**
 * Class <code>ScrollLagView</code> is a customized {@link ScrollView} dedicated
 * to the display of a {@link omr.lag.LagView}, with monitoring of run and
 * section informations
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScrollLagView
    extends ScrollView
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(ScrollLagView.class);

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // ScrollLagView //
    //---------------//
    /**
     * Create a scroll view on a lag.
     *
     * @param view the contained {@link omr.lag.LagView}
     */
    public ScrollLagView (LagView view)
    {
        setView(view);
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getView //
    //---------//
    /**
     * Report the encapsulated LagView
     *
     * @return the related LagView
     */
    @Override
    public LagView getView ()
    {
        return (LagView) view;
    }
}
