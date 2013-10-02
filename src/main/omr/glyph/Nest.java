//----------------------------------------------------------------------------//
//                                                                            //
//                                  N e s t                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.selection.GlyphEvent;
import omr.selection.GlyphIdEvent;
import omr.selection.GlyphLayerEvent;
import omr.selection.GlyphPileEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.SelectionService;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Set;

/**
 * Interface {@code Nest} handles a collection of {@link Glyph} instances,
 * with the ability to retrieve a Glyph based on its Id or its location,
 * and the ability to give birth to new glyph instances.
 *
 * <p>A nest has no orientation, nor any of its glyph instances since a glyph is
 * a collection of sections that can be differently oriented.</p>
 *
 * <p>A glyph is made of member sections and always keeps a collection of its
 * member sections. Sections are made of runs of pixels and thus sections do not
 * overlap.
 *
 * <p>TODO: Following paragraphs are obsolete, due to the advent of notion of
 * layers.
 *
 * <p>Different glyph instances can have sections in common, and in that
 * case they overlap, however only one of these glyph instances is the current
 * "owner" of these common sections. It is known as being "active" while the
 * others are inactive.
 * </p>
 *
 * <p>A nest hosts a SelectionService that deals with glyph selection
 * (Events related to Glyph, GlyphId and GlyphSet).
 *
 * <p>Selecting a (foreground) pixel, thus selects its containing section, and
 * its active glyph if any.</p>
 *
 * @author Hervé Bitteur
 */
public interface Nest
{
    //~ Static fields/initializers ---------------------------------------------

    /** Events that can be published on a nest service. */
    static final Class<?>[] eventsWritten = new Class<?>[]{
        GlyphEvent.class,
        GlyphIdEvent.class,
        GlyphSetEvent.class,
        GlyphPileEvent.class,
        GlyphLayerEvent.class
    };

    //~ Methods ----------------------------------------------------------------
    /**
     * Report the glyph layer currently selected, if any.
     *
     * @return the current glyph layer, or null
     */
    public GlyphLayer getSelectedGlyphLayer ();

    /**
     * Register a glyph and make sure all its member sections point
     * back to it.
     *
     * @param glyph the glyph to add to the nest
     * @return the actual glyph (already existing or brand new)
     * @see #registerGlyph
     */
    Glyph addGlyph (Glyph glyph);

    /**
     * Look up for <b>all</b> active glyph instances in the provided
     * layer that are contained in a provided rectangle.
     *
     * @param rect  the coordinates rectangle
     * @param layer the containing glyph layer
     * @return the glyph instances found, which may be an empty list
     */
    Set<Glyph> containedGlyphs (Rectangle rect,
                                GlyphLayer layer);

    /**
     * Remove link and subscription to locationService
     *
     * @param locationService the location service
     */
    void cutServices (SelectionService locationService);

    /**
     * Print out major internal info about this glyph nest.
     *
     * @param title a specific title to be used for the dumpOf
     */
    String dumpOf (String title);

    /**
     * Export the unmodifiable collection of active glyph instances of
     * the nest for the binary layer.
     *
     * @return the collection of glyph instances for which at least a section is
     *         assigned
     */
    Collection<Glyph> getActiveGlyphs ();

    /**
     * Export the unmodifiable collection of active glyph instances of
     * the nest for the provided layer.
     *
     * @param layer the containing glyph layer
     * @return the collection of glyph instances for which at least a section is
     *         assigned
     */
    Collection<Glyph> getActiveGlyphs (GlyphLayer layer);

    /**
     * Export the whole unmodifiable collection of glyph instances of
     * the nest.
     *
     * @return the collection of glyph instances, both active and inactive
     */
    Collection<Glyph> getAllGlyphs ();

    /**
     * Retrieve a glyph via its Id among the collection of glyph
     * instances.
     *
     * @param id the glyph id to search for
     * @return the glyph found, or null otherwise
     */
    Glyph getGlyph (Integer id);

    /**
     * Report the nest selection service.
     *
     * @return the nest selection service (Glyph, GlyphSet, GlyphId)
     */
    SelectionService getGlyphService ();

    /**
     * Report a name for this nest instance.
     *
     * @return a (distinguished) name
     */
    String getName ();

    /**
     * Return the original glyph, if any, that the provided glyph
     * duplicates in its layer.
     *
     * @param glyph the provided glyph
     * @return the original for this glyph, if any, otherwise null
     */
    Glyph getOriginal (Glyph glyph);

    /**
     * Return the original glyph, if any, that corresponds to the
     * provided signature for its layer.
     *
     * @param signature the provided signature
     * @return the original glyph for this signature, if any, otherwise null
     */
    Glyph getOriginal (GlyphSignature signature);

    /**
     * Return the original glyph, if any, that corresponds to the
     * provided signature for the provided layer.
     *
     * @param signature the provided signature
     * @param layer     the containing glyph layer
     * @return the original glyph for this signature, if any, otherwise null
     */
    Glyph getOriginal (GlyphSignature signature,
                       GlyphLayer layer);

    /**
     * Report the glyph currently selected, if any
     *
     * @return the current glyph, or null
     */
    Glyph getSelectedGlyph ();

    /**
     * Report the glyph pile currently selected, if any
     *
     * @return the current glyph pile, or null
     */
    Set<Glyph> getSelectedGlyphPile ();

    /**
     * Report the glyph set currently selected, if any
     *
     * @return the current glyph set, or null
     */
    Set<Glyph> getSelectedGlyphSet ();

    /**
     * Look up for <b>all</b> active glyph instances intersected by a
     * provided rectangle.
     *
     * @param rect  the coordinates rectangle
     * @param layer the containing glyph layer
     * @return the glyph instances found, which may be an empty list
     */
    Set<Glyph> intersectedGlyphs (Rectangle rect,
                                  GlyphLayer layer);

    /**
     * Check whether the provided glyph is among the VIP ones
     *
     * @param glyph the glyph (ID) to check
     * @return true if this is a vip glyph
     */
    boolean isVip (Glyph glyph);

    /**
     * Look for a glyph whose box contains the designated point
     * for the drop layer.
     *
     * @param point the designated point
     * @return the virtual glyph found, or null
     */
    Glyph lookupVirtualGlyph (Point point);

    /**
     * Map a section to a glyph, making the glyph active
     *
     * @param section the section to map
     * @param glyph   the assigned glyph
     * @param layer   the containing glyph layer
     */
    void mapSection (Section section,
                     Glyph glyph,
                     GlyphLayer layer);

    /**
     * Simply register a glyph in the graph, making sure we do not
     * duplicate any existing glyph.
     * (a glyph being really defined by the set of its member sections)
     *
     * @param glyph the glyph to add to the nest
     * @return the actual glyph (already existing or brand new)
     * @see #addGlyph
     */
    Glyph registerGlyph (Glyph glyph);

    /**
     * Remove the provided virtual glyph
     *
     * @param glyph the virtual glyph to remove
     */
    void removeVirtualGlyph (VirtualGlyph glyph);

    /**
     * Inject dependency on location service, and trigger subscriptions
     *
     * @param locationService the location service
     */
    void setServices (SelectionService locationService);
}
