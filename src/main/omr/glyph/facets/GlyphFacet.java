//----------------------------------------------------------------------------//
//                                                                            //
//                            G l y p h F a c e t                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
package omr.glyph.facets;


/**
 * Interface {@code GlyphFacet} is the root of any Glyph facet, gathering
 * just utility methods to be provided by each facet.
 *
 * @author Hervé Bitteur
 */
interface GlyphFacet
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Print out facet internal data
     */
    void dump ();

    /**
     * Reset relevant cached data
     */
    void invalidateCache ();
}
