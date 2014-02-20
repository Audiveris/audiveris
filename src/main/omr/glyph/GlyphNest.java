//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        G l y p h N e s t                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
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
import java.util.List;
import java.util.Set;

/**
 * Interface {@code GlyphNest} handles a collection of {@link Glyph}
 * instances, with the ability to retrieve a Glyph based on its Id or
 * its location, and the ability to give birth to new glyph instances.
 * <p>
 * A nest has no orientation, nor any of its glyph instances since a glyph is
 * a collection of sections that can be differently oriented.</p>
 * <p>
 * A glyph is made of member sections and always keeps a collection of its
 * member sections. Sections are made of runs of pixels and sections do not
 * overlap in a given layer.
 * <p>
 * A nest hosts a SelectionService that deals with glyph selection
 * (Events related to Glyph, GlyphId, GlyphSet, GlyphPile, GlyphLayer).
 * <p>
 * A GlyphNest uses sheet scale, but has no notion of system.
 * Hence, assignment of a glyph to a system must be handled separately.
 * <p>
 * Several notions must be considered when building glyph instances:<dl>
 *
 * <dt><b>Layer</b></dt>
 * <dd>A layer must always be specified for any glyph creation.
 * A layer roughly defines an ensemble of non-overlapping sections.
 * See {@link GlyphLayer} enumeration.</dd>
 *
 * <dt><b>ID</b></dt>
 * <dd>A glyph instance may be <i>declared</i> at sheet glyph nest, which
 * then maps a sequential ID (unique within the sheet) to the glyph instance.
 * This allows to name the glyph in debugging output and manually retrieve the
 * glyph via its ID at user interface level.
 * A glyph that is not (yet) handled by a nest is called <b>transient</b>.
 * <br/>There is yet no method to simply "declare" a glyph to a nest, only
 * {@link #registerGlyph}.</dd>
 *
 * <dt><b>Signature</b></dt>
 * <dd>A glyph instance may be <b>registered</b> at sheet glyph nest.
 * A signature is then computed based on glyph physical characteristics and
 * registered for a specific layer in glyph nest.
 * This allows to handle <b>original</b> instances and avoid duplicates that
 * would represent the same set of sections.
 * TODO: Is this still useful? This is not certain..
 * <br/>Use {@link #registerGlyph}</dd>
 * </dl>
 *
 * @author Hervé Bitteur
 */
public interface GlyphNest
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Events that can be published on a nest service. */
    static final Class<?>[] eventsWritten = new Class<?>[]{
        GlyphEvent.class, GlyphIdEvent.class,
        GlyphSetEvent.class, GlyphPileEvent.class,
        GlyphLayerEvent.class
    };

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Build one glyph in provided layer from a collection of sections.
     * The returned glyph will be transient (not known by nest) unless the
     * 'register' parameter is set.
     *
     * @param sections the provided members of the future glyph
     * @param layer    the containing layer
     * @param register true for registering the glyph
     * @param linking  should we link sections back to glyph?
     * @return the glyph built
     */
    Glyph buildGlyph (Collection<Section> sections,
                      GlyphLayer layer,
                      boolean register,
                      Glyph.Linking linking);

    /**
     * Build one glyph from a collection of glyph parts.
     * The returned glyph will be transient (not known by nest) unless the
     * 'register' parameter is set.
     * <p>
     * Nota: All parts must belong to the same layer.
     *
     * @param parts    the provided glyph parts
     * @param register true for registering the glyph
     * @param linking  should we link sections back to glyph?
     * @return the glyph compound
     */
    Glyph buildGlyph (Collection<? extends Glyph> parts,
                      boolean register,
                      Glyph.Linking linking);

    /**
     * Look up for <b>all</b> glyph instances in the provided
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
     * Export the whole unmodifiable collection of glyph instances
     * of all layers in the nest.
     *
     * @return the collection of glyph instances whatever their layer
     */
    Collection<Glyph> getAllGlyphs ();

    /**
     * Export the whole unmodifiable collection of all glyph instances
     * ever inserted in the nest.
     *
     * @return the collection of glyph instances including deleted ones
     */
    Collection<Glyph> getAllGlyphsEver ();

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
     * Export the unmodifiable collection of glyph instances of
     * the nest for the provided layer.
     *
     * @param layer the containing glyph layer
     * @return the collection of glyph instances for specified layer
     */
    Collection<Glyph> getGlyphs (GlyphLayer layer);

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
     * Report the glyph layer currently selected, if any.
     *
     * @return the current glyph layer, or null
     */
    GlyphLayer getSelectedGlyphLayer ();

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
     * Look up for <b>all</b> glyph instances from provided layer and
     * that are intersected by the provided rectangle.
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
     * Register a (transient) glyph in the graph with its physical
     * signature, or re-register a known glyph with its updated
     * signature.
     * <p>
     * Nota: Make sure to use the returned glyph value (the original) instead of
     * the initial one passed as parameter
     *
     * @param glyph the glyph to add to the nest
     * @return the original glyph (already existing or brand new)
     */
    Glyph registerGlyph (Glyph glyph);

    /**
     * Remove the provided glyph
     *
     * @param glyph the glyph to remove
     */
    void removeGlyph (Glyph glyph);

    /**
     * Browse through the provided sections and return a list of
     * glyph instances, one for each set of connected sections.
     * <p>
     * Nota: The method modifies the 'processed' property of each section.
     *
     * @param sections the sections to browse
     * @param layer    the target layer
     * @param register true for registering every glyph created
     * @param linking  should we link sections back to glyph?
     * @return the list of glyph instances created
     */
    List<Glyph> retrieveGlyphs (Collection<Section> sections,
                                GlyphLayer layer,
                                boolean register,
                                Glyph.Linking linking);

    /**
     * Inject dependency on location service, and trigger subscriptions
     *
     * @param locationService the location service
     */
    void setServices (SelectionService locationService);
}
