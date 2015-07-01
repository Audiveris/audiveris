//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B a s i c N e s t                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphComposition.Linking;
import omr.glyph.ui.ViewParameters;

import omr.lag.Section;
import omr.lag.Sections;

import omr.selection.GlyphEvent;
import omr.selection.GlyphIdEvent;
import omr.selection.GlyphLayerEvent;
import omr.selection.GlyphPileEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.NestEvent;
import omr.selection.SelectionHint;
import static omr.selection.SelectionHint.*;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.util.Dumping;
import omr.util.IntUtil;

import org.bushe.swing.event.EventSubscriber;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class {@code BasicNest} implements a {@link GlyphNest}.
 *
 * @author Hervé Bitteur
 */
public class BasicNest
        implements GlyphNest, EventSubscriber<UserEvent>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            BasicNest.class);

    /** Events read on location service. */
    public static final Class<?>[] locEventsRead = new Class<?>[]{LocationEvent.class};

    /** Events read on nest (glyph) service. */
    public static final Class<?>[] glyphEventsRead = new Class<?>[]{
        GlyphIdEvent.class, GlyphEvent.class,
        GlyphSetEvent.class, GlyphLayerEvent.class
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** (Debug) a unique name for this nest. */
    private final String name;

    /** Sheet scale. */
    private Scale scale;

    /** Elaborated constants for this nest. */
    private final Parameters params;

    /** Location service (read & write). */
    private SelectionService locationService;

    /** Hosted glyph service. (Glyph, GlyphId, GlyphSet, GlyphPile) */
    protected final SelectionService glyphService;

    /** Global id to uniquely identify a glyph. */
    private final AtomicInteger globalGlyphId = new AtomicInteger(0);

    /**
     * Collection of all glyph instances ever inserted in this GlyphNest,
     * indexed by glyph id.
     */
    private final ConcurrentHashMap<Integer, Glyph> allGlyphsEver = new ConcurrentHashMap<Integer, Glyph>();

    /** Partitioning of glyph instances into layers. */
    private final Map<GlyphLayer, LayerNest> byLayer = new EnumMap<GlyphLayer, LayerNest>(
            GlyphLayer.class);

    /** Predefined layer for historical virtual glyph instances. */
    private final LayerNest dropNest;

    /** Current layer used for glyph lookup. */
    private GlyphLayer currentLayer = GlyphLayer.DEFAULT;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a glyph nest.
     *
     * @param name  the distinguished name for this instance
     * @param sheet the related sheet
     */
    public BasicNest (String name,
                      Sheet sheet)
    {
        this.name = name;

        if (sheet != null) {
            scale = sheet.getScale();
        }

        // Allocate the various layers
        for (GlyphLayer layer : GlyphLayer.values()) {
            byLayer.put(layer, new LayerNest(layer));
        }

        dropNest = byLayer.get(GlyphLayer.DROP);

        params = new Parameters();
        glyphService = new SelectionService(name, GlyphNest.eventsWritten);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // buildGlyph //
    //------------//
    @Override
    public Glyph buildGlyph (Collection<Section> sections,
                             GlyphLayer layer,
                             boolean register,
                             Glyph.Linking linking)
    {
        Glyph glyph = new BasicGlyph(scale.getInterline(), layer);

        for (Section section : sections) {
            glyph.addSection(section, linking);
        }

        if (register) {
            return registerGlyph(glyph);
        } else {
            return glyph;
        }
    }

    //------------//
    // buildGlyph //
    //------------//
    @Override
    public Glyph buildGlyph (Collection<? extends Glyph> parts,
                             boolean register)
    {
        // Gather all the sections involved
        Collection<Section> sections = new HashSet<Section>();

        for (Glyph part : parts) {
            sections.addAll(part.getMembers());
        }

        return buildGlyph(sections, currentLayer, register, Linking.NO_LINK);
    }

    //-----------------//
    // containedGlyphs //
    //-----------------//
    @Override
    public Set<Glyph> containedGlyphs (Rectangle rect,
                                       GlyphLayer layer)
    {
        return Glyphs.containedGlyphs(byLayer.get(layer).getGlyphs(), rect);
    }

    //-------------//
    // setServices //
    //-------------//
    @Override
    public void cutServices (SelectionService locationService)
    {
        for (Class<?> eventClass : locEventsRead) {
            locationService.unsubscribe(eventClass, this);
        }

        for (Class<?> eventClass : glyphEventsRead) {
            glyphService.unsubscribe(eventClass, this);
        }
    }

    //--------------//
    // getAllGlyphs //
    //--------------//
    @Override
    public Collection<Glyph> getAllGlyphs ()
    {
        return Collections.unmodifiableCollection(allGlyphsEver.values());
    }

    //------------------//
    // getAllGlyphsEver //
    //------------------//
    @Override
    public Collection<Glyph> getAllGlyphsEver ()
    {
        return Collections.unmodifiableCollection(allGlyphsEver.values());
    }

    //----------//
    // getGlyph //
    //----------//
    @Override
    public Glyph getGlyph (Integer id)
    {
        return allGlyphsEver.get(id);
    }

    //-----------------//
    // getGlyphService //
    //-----------------//
    @Override
    public SelectionService getGlyphService ()
    {
        return glyphService;
    }

    //-----------//
    // getGlyphs //
    //-----------//
    @Override
    public Collection<Glyph> getGlyphs (GlyphLayer layer)
    {
        return byLayer.get(layer).getGlyphs();
    }

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return name;
    }

    //-------------//
    // getOriginal //
    //-------------//
    @Override
    public Glyph getOriginal (Glyph glyph)
    {
        return getOriginal(glyph.getSignature(), glyph.getLayer());
    }

    //-------------//
    // getOriginal //
    //-------------//
    @Override
    public Glyph getOriginal (GlyphSignature signature,
                              GlyphLayer layer)
    {
        return byLayer.get(layer).getOriginal(signature);
    }

    //------------------//
    // getSelectedGlyph //
    //------------------//
    @Override
    public Glyph getSelectedGlyph ()
    {
        return (Glyph) getGlyphService().getSelection(GlyphEvent.class);
    }

    //-----------------------//
    // getSelectedGlyphLayer //
    //-----------------------//
    @Override
    public GlyphLayer getSelectedGlyphLayer ()
    {
        return (GlyphLayer) getGlyphService().getSelection(GlyphLayerEvent.class);
    }

    //----------------------//
    // getSelectedGlyphPile //
    //----------------------//
    @SuppressWarnings("unchecked")
    @Override
    public Set<Glyph> getSelectedGlyphPile ()
    {
        return (Set<Glyph>) getGlyphService().getSelection(GlyphPileEvent.class);
    }

    //---------------------//
    // getSelectedGlyphSet //
    //---------------------//
    @SuppressWarnings("unchecked")
    @Override
    public Set<Glyph> getSelectedGlyphSet ()
    {
        return (Set<Glyph>) getGlyphService().getSelection(GlyphSetEvent.class);
    }

    //-------------------//
    // intersectedGlyphs //
    //-------------------//
    @Override
    public Set<Glyph> intersectedGlyphs (Rectangle rect,
                                         GlyphLayer layer)
    {
        return Glyphs.intersectedGlyphs(byLayer.get(layer).getGlyphs(), rect);
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip (Glyph glyph)
    {
        return params.vipGlyphs.contains(glyph.getId());
    }

    //--------------------//
    // lookupVirtualGlyph //
    //--------------------//
    @Override
    public Glyph lookupVirtualGlyph (Point point)
    {
        for (Glyph virtual : dropNest.getGlyphs()) {
            if (virtual.getBounds().contains(point)) {
                return virtual;
            }
        }

        return null;
    }

    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            if (event instanceof LocationEvent) {
                // Location => enclosed Glyph(s) or 1 virtual glyph
                handleEvent((LocationEvent) event);
            } else if (event instanceof GlyphEvent) {
                // Glyph => glyph contour & GlyphSet update
                handleEvent((GlyphEvent) event);
            } else if (event instanceof GlyphSetEvent) {
                // GlyphSet => Compound glyph
                handleEvent((GlyphSetEvent) event);
            } else if (event instanceof GlyphIdEvent) {
                // Glyph Id => Glyph
                handleEvent((GlyphIdEvent) event);
            } else if (event instanceof GlyphLayerEvent) {
                // Glyph Layer
                handleEvent((GlyphLayerEvent) event);
            }
        } catch (Throwable ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //---------------//
    // registerGlyph //
    //---------------//
    @Override
    public Glyph registerGlyph (Glyph glyph)
    {
        return byLayer.get(glyph.getLayer()).registerGlyph(glyph);
    }

    //-------------//
    // removeGlyph //
    //-------------//
    @Override
    public void removeGlyph (Glyph glyph)
    {
        byLayer.get(glyph.getLayer()).removeGlyph(glyph);
    }

    //----------------//
    // retrieveGlyphs //
    //----------------//
    @Override
    public List<Glyph> retrieveGlyphs (Collection<Section> sections,
                                       GlyphLayer layer,
                                       boolean register)
    {
        List<Glyph> glyphs = new ArrayList<Glyph>();

        // Reset section processed flag
        for (Section section : sections) {
            section.setProcessed(false);
        }

        // Browse all sections provided
        for (Section section : sections) {
            // Not already visited?
            if (!section.isProcessed()) {
                // Let's build a new glyph around this starting section
                Glyph glyph = new BasicGlyph(scale.getInterline(), layer);
                considerConnection(glyph, section, sections);

                // Insert this newly built glyph into nest?
                if (register) {
                    glyphs.add(registerGlyph(glyph));
                } else {
                    glyphs.add(glyph);
                }
            }
        }

        return glyphs;
    }

    //------------------------------------//
    // retrieveGlyphsFromIsolatedSections //
    //------------------------------------//
    /**
     * Since sections links are not assumed to be (fully) set, we use an external
     * graph of inter-sections relations.
     *
     * @param sections collection of (isolated) sections
     * @param layer    target layer
     * @param register true for registering in nest
     * @return the list of created glyph instances
     */
    @Override
    public List<Glyph> retrieveGlyphsFromIsolatedSections (Collection<Section> sections,
                                                           GlyphLayer layer,
                                                           boolean register)
    {
        // Build a temporary graph of all sections with "touching" relations
        List<Section> list = new ArrayList<Section>(sections);

        ///Collections.sort(list, Section.byAbscissa);
        SimpleGraph<Section, SectionLink> graph = new SimpleGraph<Section, SectionLink>(
                SectionLink.class);

        // Populate graph with all sections as vertices
        for (Section section : list) {
            graph.addVertex(section);
        }

        // Populate graph with relations
        for (int i = 0; i < list.size(); i++) {
            Section one = list.get(i);

            for (Section two : list.subList(i + 1, list.size())) {
                if (one.touches(two)) {
                    graph.addEdge(one, two, new SectionLink());
                }
            }
        }

        // Retrieve all the clusters of sections (sets of touching sections)
        ConnectivityInspector inspector = new ConnectivityInspector(graph);
        List<Set<Section>> sets = inspector.connectedSets();
        logger.debug("sets: {}", sets.size());

        List<Glyph> glyphs = new ArrayList<Glyph>();

        for (Set<Section> set : sets) {
            glyphs.add(buildGlyph(set, layer, register, Linking.NO_LINK));
        }

        return glyphs;
    }

    //-------------//
    // setServices //
    //-------------//
    @Override
    public void setServices (SelectionService locationService)
    {
        this.locationService = locationService;

        for (Class<?> eventClass : locEventsRead) {
            locationService.subscribeStrongly(eventClass, this);
        }

        for (Class<?> eventClass : glyphEventsRead) {
            glyphService.subscribeStrongly(eventClass, this);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        sb.append(name);

        if (!allGlyphsEver.isEmpty()) {
            sb.append(" ").append(allGlyphsEver.size());

            // Active/All glyphs per layer
            for (GlyphLayer layer : GlyphLayer.values()) {
                LayerNest layerNest = byLayer.get(layer);

                if (!layerNest.originals.isEmpty()) {
                    sb.append(
                            String.format(
                                    " %s:%d/%d",
                                    layer,
                                    getGlyphs(layer).size(),
                                    layerNest.originals.size()));
                }
            }
        } else {
            sb.append(" noglyphs");
        }

        sb.append("}");

        return sb.toString();
    }

    //---------//
    // publish //
    //---------//
    /**
     * Publish on glyph service
     *
     * @param event the event to publish
     */
    protected void publish (NestEvent event)
    {
        glyphService.publish(event);
    }

    //---------//
    // publish //
    //---------//
    /**
     * Publish on location service
     *
     * @param event the event to publish
     */
    protected void publish (LocationEvent event)
    {
        locationService.publish(event);
    }

    //------------------//
    // subscribersCount //
    //------------------//
    /**
     * Convenient method to retrieve the number of subscribers on the glyph
     * service for a specific class
     *
     * @param classe the specific class
     * @return the number of subscribers interested in the specific class
     */
    protected int subscribersCount (Class<? extends NestEvent> classe)
    {
        return glyphService.subscribersCount(classe);
    }

    //--------------------//
    // considerConnection //
    //--------------------//
    /**
     * Consider all sections transitively connected to the provided section in order to
     * populate the provided glyph.
     *
     * @param glyph    the provided glyph
     * @param section  the section to consider
     * @param sections the collection of sections allowed to be used
     */
    private void considerConnection (Glyph glyph,
                                     Section section,
                                     Collection<Section> sections)
    {
        // Check whether this section is suitable to expand the glyph
        if (!section.isProcessed()) {
            section.setProcessed(true);

            glyph.addSection(section, Glyph.Linking.NO_LINK);

            // Add recursively all linked sections
            // Incoming ones
            for (Section source : section.getSources()) {
                if (sections.contains(source)) {
                    considerConnection(glyph, source, sections);
                }
            }

            // Outgoing ones
            for (Section target : section.getTargets()) {
                if (sections.contains(target)) {
                    considerConnection(glyph, target, sections);
                }
            }

            // Sections from other orientation
            for (Section other : section.getOppositeSections()) {
                if (sections.contains(other)) {
                    considerConnection(glyph, other, sections);
                }
            }
        }
    }

    //------------//
    // generateId //
    //------------//
    private int generateId ()
    {
        return globalGlyphId.incrementAndGet();
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in sheet location => [active] glyph(s)
     *
     * @param locationEvent
     */
    private void handleEvent (LocationEvent locationEvent)
    {
        SelectionHint hint = locationEvent.hint;
        MouseMovement movement = locationEvent.movement;
        Rectangle rect = locationEvent.getData();

        if (!hint.isLocation() && !hint.isContext()) {
            return;
        }

        if (rect == null) {
            return;
        }

        if ((rect.width > 0) && (rect.height > 0)) {
            // This is a non-degenerated rectangle
            // Look for set of enclosed active glyphs in the current layer
            Set<Glyph> glyphsFound = containedGlyphs(rect, currentLayer);

            // Publish first Glyph of the set
            Glyph glyph = glyphsFound.isEmpty() ? null : glyphsFound.iterator().next();
            publish(new GlyphEvent(this, hint, movement, glyph));

            // Publish GlyphSet
            publish(new GlyphSetEvent(this, hint, movement, glyphsFound));
        } else {
            // This is just a point
            // Look for containing glyph in current layer
            Glyph glyph = Glyphs.containingGlyph(
                    byLayer.get(currentLayer).getGlyphs(),
                    rect.getLocation());

            // Publish glyph found (perhaps null)
            publish(new GlyphEvent(this, hint, movement, glyph));

            // Retrieve and publish the pile of glyphs at this location
            Set<Glyph> pile = new LinkedHashSet<Glyph>();

            for (LayerNest layerNest : byLayer.values()) {
                pile.addAll(Glyphs.containingGlyphs(layerNest.getGlyphs(), rect.getLocation()));
            }

            publish(new GlyphPileEvent(this, hint, movement, pile));
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Glyph => glyph contour & GlyphSet update
     *
     * @param glyphEvent
     */
    private void handleEvent (GlyphEvent glyphEvent)
    {
        SelectionHint hint = glyphEvent.hint;
        MouseMovement movement = glyphEvent.movement;
        Glyph glyph = glyphEvent.getData();

        if ((hint == GLYPH_INIT) || (hint == GLYPH_MODIFIED)) {
            // Display glyph contour
            if (glyph != null) {
                Rectangle box = glyph.getBounds();
                publish(new LocationEvent(this, hint, movement, box));
            }
        }

        // In glyph-selection mode, for non-transient glyphs
        // (and only if we have interested subscribers)
        if ((hint != GLYPH_TRANSIENT)
            && !ViewParameters.getInstance().isSectionMode()
            && (subscribersCount(GlyphSetEvent.class) > 0)) {
            // Update glyph set
            Set<Glyph> glyphs = getSelectedGlyphSet();

            if (glyphs == null) {
                glyphs = new LinkedHashSet<Glyph>();
            }

            if (hint == LOCATION_ADD) {
                // Adding to (or Removing from) the set of glyphs
                if (glyph != null) {
                    if (glyphs.contains(glyph)) {
                        glyphs.remove(glyph);
                    } else {
                        glyphs.add(glyph);
                    }
                }
            } else if (hint == CONTEXT_ADD) {
                // Don't modify the set
            } else {
                // Overwriting the set of glyphs
                if (glyph != null) {
                    // Make a one-glyph set
                    glyphs = Glyphs.sortedSet(glyph);
                } else {
                    // Make an empty set
                    glyphs = Glyphs.sortedSet();
                }
            }

            publish(new GlyphSetEvent(this, hint, movement, glyphs));
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in GlyphSet => Compound
     *
     * @param glyphSetEvent
     */
    private void handleEvent (GlyphSetEvent glyphSetEvent)
    {
        if (ViewParameters.getInstance().isSectionMode()) {
            // Section mode
            return;
        }

        // Glyph mode
        MouseMovement movement = glyphSetEvent.movement;
        Set<Glyph> glyphs = glyphSetEvent.getData();

        if ((glyphs != null) && (glyphs.size() > 1)) {
            Glyph compound = buildGlyph(glyphs, false);
            publish(new GlyphEvent(this, SelectionHint.GLYPH_TRANSIENT, movement, compound));
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Glyph ID => glyph
     *
     * @param glyphIdEvent
     */
    private void handleEvent (GlyphIdEvent glyphIdEvent)
    {
        SelectionHint hint = glyphIdEvent.hint;
        MouseMovement movement = glyphIdEvent.movement;
        int id = glyphIdEvent.getData();

        // Report Glyph entity (which may be null)
        publish(new GlyphEvent(this, hint, movement, getGlyph(id)));
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Glyph Layer [=> glyph]
     *
     * @param glyphLayerEvent
     */
    private void handleEvent (GlyphLayerEvent glyphLayerEvent)
    {
        // Update current layer for future lookup
        currentLayer = glyphLayerEvent.getData();

        // Should we publish a new glyph, according to new layer?
        // Forge a new event (to avoid the RELEASING mouvement) & publish it
        LocationEvent locEvent = (LocationEvent) locationService.getLastEvent(LocationEvent.class);

        if (locEvent != null) {
            publish(
                    new LocationEvent(this, locEvent.hint, MouseMovement.PRESSING, locEvent.getData()));
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.String vipGlyphs = new Constant.String(
                "",
                "(Debug) Comma-separated values of VIP glyphs IDs");
    }

    //-----------//
    // LayerNest //
    //-----------//
    /**
     * Handles the glyph instances for a given layer.
     */
    private class LayerNest
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The corresponding glyph layer. */
        private final GlyphLayer glyphLayer;

        /**
         * Smart glyph map, based on a physical glyph signature, and
         * thus usable across several glyph extractions, to ensure
         * glyph is unique.
         */
        private final ConcurrentHashMap<GlyphSignature, Glyph> originals = new ConcurrentHashMap<GlyphSignature, Glyph>();

        //~ Constructors ---------------------------------------------------------------------------
        public LayerNest (GlyphLayer glyphLayer)
        {
            this.glyphLayer = glyphLayer;
        }

        //~ Methods --------------------------------------------------------------------------------
        //-----------//
        // getGlyphs //
        //-----------//
        public Collection<Glyph> getGlyphs ()
        {
            return Collections.unmodifiableCollection(originals.values());
        }

        //-------------//
        // getOriginal //
        //-------------//
        public Glyph getOriginal (Glyph glyph)
        {
            return getOriginal(glyph.getSignature());
        }

        //-------------//
        // getOriginal //
        //-------------//
        public Glyph getOriginal (GlyphSignature signature)
        {
            // Find an old glyph registered with this signature
            Glyph oldGlyph = originals.get(signature);

            if (oldGlyph == null) {
                return null;
            }

            // Check the old signature is still valid
            if (oldGlyph.getSignature().compareTo(signature) == 0) {
                return oldGlyph;
            } else {
                logger.debug("Obsolete signature for {}", oldGlyph);

                return null;
            }
        }

        //---------------//
        // registerGlyph //
        //---------------//
        public Glyph registerGlyph (Glyph glyph)
        {
            // First check this physical glyph does not already exist
            Glyph original = getOriginal(glyph);

            if (original != null) {
                if (original != glyph) {
                    // Reuse the existing glyph
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                                "new avatar of #{}{}{}",
                                original.getId(),
                                Sections.toString(" members", glyph.getMembers()),
                                Sections.toString(" original", original.getMembers()));
                    }

                    glyph = original;
                    glyph.setPartOf(null);
                }
            } else {
                GlyphSignature newSig = glyph.getSignature();

                if (glyph.isTransient()) {
                    // Register with a brand new Id
                    final int id = generateId();
                    glyph.setId(id);
                    glyph.setNest(BasicNest.this);
                    allGlyphsEver.put(id, glyph);

                    if (isVip(glyph)) {
                        glyph.setVip();
                    }
                } else {
                    // This is a re-registration
                    GlyphSignature oldSig = glyph.getRegisteredSignature();

                    if ((oldSig != null) && !newSig.equals(oldSig)) {
                        Glyph oldGlyph = originals.remove(oldSig);

                        if (oldGlyph != null) {
                            logger.debug("Updating registration of glyph#{}", glyph.getId());
                        }
                    }
                }

                originals.put(newSig, glyph);
                glyph.setRegisteredSignature(newSig);

                logger.debug(
                        "Registered {} as original {}",
                        glyph.idString(),
                        glyph.getSignature());
            }

            return glyph;
        }

        //-------------//
        // removeGlyph //
        //-------------//
        public synchronized void removeGlyph (Glyph glyph)
        {
            originals.remove(glyph.getSignature(), glyph);

            // Should we keep track of every glyph ever added?
            ///??? allGlyphs.remove(glyph.getId(), glyph);
        }
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all constants related to nest
     */
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final List<Integer> vipGlyphs; // List of IDs for VIP glyphs

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters ()
        {
            vipGlyphs = IntUtil.parseInts(constants.vipGlyphs.getValue());

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }

            if (!vipGlyphs.isEmpty()) {
                logger.info("VIP glyphs: {}", vipGlyphs);
            }
        }
    }

    //-------------//
    // SectionLink //
    //-------------//
    /**
     * Represents a "touching" relationship between two sections.
     */
    private static class SectionLink
    {
    }
}
