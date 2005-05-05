//-----------------------------------------------------------------------//
//                                                                       //
//                           S t i c k V i e w                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.stick;

import omr.glyph.Glyph;
import omr.glyph.GlyphDirectory;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphLagView;
import omr.glyph.GlyphSection;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Collection;

/**
 * Class <code>StickView</code> is a specific {@link omr.lag.LagView}
 * dedicated to the display and processing of selected stick.
 *
 * A {@link FilterMonitor} interface can be connected to this view, to display
 * information about the filtered stick.
 */
public class StickView
    extends GlyphLagView
{
    // Connected stick monitor if any
    protected FilterMonitor filterMonitor;

    //~ Constructors -----------------------------------------------------

    //-----------//
    // StickView //
    //-----------//
    /**
     * Create a StickView as a LagView, with lag and potential specific collection
     * of sections
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
    // setFilterMonitor //
    //------------------//
    /**
     * Connect a FilterMonitor to this view, in order to display related stick
     * information
     *
     * @param filterMonitor the monitor to connect
     */
    public void setFilterMonitor (FilterMonitor filterMonitor)
    {
        this.filterMonitor = filterMonitor;
    }

    //--------------//
    // pointUpdated //
    //--------------//
    @Override
    public void pointUpdated (MouseEvent e,
                              Point pt)
    {
        // Erase the previous stick info
        if (filterMonitor != null) {
            filterMonitor.tellHtml(null);
        }

        super.pointUpdated(e, pt);
    }

    //---------------//
    // pointSelected //
    //---------------//
    @Override
    public void pointSelected (MouseEvent e,
                               Point pt)
    {
        // First, provide info related to designated point
        pointUpdated(e, pt);

        // Then, look for a stick selection
        final StickSection section = (StickSection) lookupSection(pt);

        if (section != null) {
            Glyph glyph =  section.getGlyph();
            if (glyph != null) {
                glyphSelected(glyph, pt);
            }
        }
    }
}
