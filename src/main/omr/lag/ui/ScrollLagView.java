//----------------------------------------------------------------------------//
//                                                                            //
//                         S c r o l l L a g V i e w                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag.ui;

import omr.log.Logger;

import omr.ui.view.ScrollView;

/**
 * Class <code>ScrollLagView</code> is a customized {@link ScrollView} dedicated
 * to the display of a {@link omr.lag.ui.LagView}, with monitoring of run and
 * section informations
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScrollLagView
    extends ScrollView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScrollLagView.class);

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // ScrollLagView //
    //---------------//
    /**
     * Create a scroll view on a lag.
     *
     * @param view the contained {@link omr.lag.ui.LagView}
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
