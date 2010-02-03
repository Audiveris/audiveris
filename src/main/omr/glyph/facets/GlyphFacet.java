//----------------------------------------------------------------------------//
//                                                                            //
//                            G l y p h F a c e t                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
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

    //------//
    // dump //
    //------//
    /**
     * Print out facet internal data
     */
    void dump ();

    //-----------------//
    // invalidateCache //
    //-----------------//
    /**
     * Reset relevant cached data
     */
    void invalidateCache ();
}
