//-----------------------------------------------------------------------//
//                                                                       //
//                           S t i c k V i e w                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.stick;

import omr.check.Checkable;
import omr.check.CheckMonitor;
import omr.glyph.GlyphDirectory;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphLagView;
import omr.glyph.GlyphSection;

import java.util.*;

/**
 * Class <code>StickView</code> is a specific {@link omr.lag.LagView}
 * dedicated to the display and processing of selected stick.
 *
 * A {@link CheckMonitor} interface can be connected to this view, to
 * display information about the checked stick.
 *
 * @param <C> the {@link Checkable} type used for CheckMonitor
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class StickView <C extends Checkable>
    extends GlyphLagView
{
    // Connected stick monitor if any
    protected CheckMonitor<C> checkMonitor;

    //~ Constructors -----------------------------------------------------

    //-----------//
    // StickView //
    //-----------//
    /**
     * Create a StickView as a LagView, with lag and potential specific
     * collection of sections
     *
     * @param lag the related lag
     * @param specific the specific sections if any, otherwise null
     * @param directory how to retrieve glyphs by id
     */
    public StickView (GlyphLag                 lag,
                      Collection<GlyphSection> specific,
                      GlyphDirectory           directory)
    {
        super(lag, specific, directory);
    }

    //~ Methods ----------------------------------------------------------

    //------------------//
    // setCheckMonitor //
    //------------------//
    /**
     * Connect a CheckMonitor to this view, in order to display related
     * stick information
     *
     * @param checkMonitor the monitor to connect
     */
    public void setCheckMonitor (CheckMonitor<C> checkMonitor)
    {
        this.checkMonitor = checkMonitor;
    }
}
