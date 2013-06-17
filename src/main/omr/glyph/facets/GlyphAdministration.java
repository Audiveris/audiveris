//----------------------------------------------------------------------------//
//                                                                            //
//                   G l y p h A d m i n i s t r a t i o n                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.Nest;

import omr.util.Vip;

/**
 * Interface {@code GlyphAdministration} defines the administration
 * facet of a glyph, handling the glyph id and its related containing
 * nest.
 *
 * @author Hervé Bitteur
 */
interface GlyphAdministration
        extends GlyphFacet, Vip
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report whether this entity has been "processed".
     *
     * @return the processed flag value
     */
    public boolean isProcessed ();

    /**
     * Set a flag to be used at caller's will.
     *
     * @param processed the processed to set
     */
    public void setProcessed (boolean processed);

    /**
     * Report the unique glyph id within its containing nest
     *
     * @return the glyph id
     */
    int getId ();

    /**
     * Report the containing nest
     *
     * @return the containing nest
     */
    Nest getNest ();

    /**
     * Report a short glyph reference
     *
     * @return glyph reference
     */
    String idString ();

    /**
     * Test whether the glyph is transient (not yet inserted into the nest)
     *
     * @return true if transient
     */
    boolean isTransient ();

    /**
     * Report whether this glyph is virtual (rather than real)
     *
     * @return true if virtual
     */
    boolean isVirtual ();

    /**
     * Assign a unique ID to the glyph
     *
     * @param id the unique id
     */
    void setId (int id);

    /**
     * The setter for glyph nest.
     * To be used with care, it freezes the glyph geometry, forbidding any
     * geometric modification (such as {@link Glyph#addSection} that would
     * impact its signature.
     *
     * @param nest the containing nest
     */
    void setNest (Nest nest);
}
