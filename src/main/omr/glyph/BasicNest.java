//----------------------------------------------------------------------------//
//                                                                            //
//                             B a s i c N e s t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;
import omr.glyph.ui.ViewParameters;

import omr.lag.BasicRoi;
import omr.lag.Roi;
import omr.lag.Section;
import omr.lag.Sections;

import omr.math.Histogram;

import omr.run.Orientation;

import omr.selection.GlyphEvent;
import omr.selection.GlyphIdEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.NestEvent;
import omr.selection.SelectionHint;
import static omr.selection.SelectionHint.*;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.VipUtil;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class {@code BasicNest} implements a {@link Nest}.
 *
 * @author Hervé Bitteur
 */
public class BasicNest
        implements Nest,
                   EventSubscriber<UserEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(BasicNest.class);

    /** Events read on location service */
    public static final Class<?>[] locEventsRead = new Class<?>[]{
        LocationEvent.class};

    /** Events read on nest (glyph) service */
    public static final Class<?>[] glyEventsRead = new Class<?>[]{
        GlyphIdEvent.class,
        GlyphEvent.class,
        GlyphSetEvent.class
    };

    //~ Instance fields --------------------------------------------------------
    /** (Debug) a unique name for this nest. */
    private final String name;

    /** Related sheet. */
    private final Sheet sheet;

    /** Elaborated constants for this nest. */
    private final Parameters params;

    /**
     * Smart glyph map, based on a physical glyph signature, and thus
     * usable across several glyph extractions, to ensure glyph unicity
     * whatever the sequential ID it is assigned.
     */
    private final ConcurrentHashMap<GlyphSignature, Glyph> originals = new ConcurrentHashMap<>();

    /**
     * Collection of all glyphs ever inserted in this Nest, indexed by
     * glyph id. No non-virtual glyph is ever removed from this map.
     */
    private final ConcurrentHashMap<Integer, Glyph> allGlyphs = new ConcurrentHashMap<>();

    /**
     * Current map of section -> glyphs.
     * This defines the glyphs that are currently active, since there is at
     * least one section pointing to them (the sections collection is
     * immutable).
     * Nota: The glyph reference within the section is kept in sync
     */
    private final ConcurrentHashMap<Section, Glyph> activeMap = new ConcurrentHashMap<>();

    /**
     * Collection of active glyphs.
     * This is derived from the activeMap, to give direct access to all the
     * active glyphs, and is kept in sync with activeMap.
     * It also contains the virtual glyphs since these are always active.
     */
    private Set<Glyph> activeGlyphs;

    /** Collection of virtual glyphs. (with no underlying sections) */
    private Set<Glyph> virtualGlyphs = new HashSet<>();

    /** Global id to uniquely identify a glyph. */
    private final AtomicInteger globalGlyphId = new AtomicInteger(0);

    /** Location service (read & write). */
    private SelectionService locationService;

    /** Hosted glyph service. (Glyph, GlyphId and GlyphSet) */
    protected final SelectionService glyphService;

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // BasicNest //
    //-----------//
    /**
     * Create a glyph nest.
     *
     * @param name the distinguished name for this instance
     */
    public BasicNest (String name,
                      Sheet sheet)
    {
        this.name = name;
        this.sheet = sheet;

        params = new Parameters();
        glyphService = new SelectionService(name, Nest.eventsWritten);
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // addGlyph //
    //----------//
    @Override
    public Glyph addGlyph (Glyph glyph)
    {
        glyph = registerGlyph(glyph);

        // Make absolutely all its sections point back to it
        glyph.linkAllSections();

        if (glyph.isVip()) {
            logger.info("{} added", glyph.idString());
        }

        return glyph;
    }

    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf (String title)
    {
        StringBuilder sb = new StringBuilder();

        if (title != null) {
            sb.append(String.format("%s%n", title));
        }

        // Dump of active glyphs
        sb.append(String.format("Active glyphs (%s) :%n",
                getActiveGlyphs().size()));

        for (Glyph glyph : getActiveGlyphs()) {
            sb.append(String.format("%s%n", glyph));
        }

        // Dump of inactive glyphs
        Collection<Glyph> inactives = new ArrayList<>(getAllGlyphs());
        inactives.removeAll(getActiveGlyphs());
        sb.append(String.format("%nInactive glyphs (%s) :%n", inactives.size()));

        for (Glyph glyph : inactives) {
            sb.append(String.format("%s%n", glyph));
        }

        return sb.toString();
    }

    //-----------------//
    // getActiveGlyphs //
    //-----------------//
    @Override
    public synchronized Collection<Glyph> getActiveGlyphs ()
    {
        if (activeGlyphs == null) {
            activeGlyphs = Glyphs.sortedSet(activeMap.values());
            activeGlyphs.addAll(virtualGlyphs);
        }

        return Collections.unmodifiableCollection(activeGlyphs);
    }

    //--------------//
    // getAllGlyphs //
    //--------------//
    @Override
    public Collection<Glyph> getAllGlyphs ()
    {
        return Collections.unmodifiableCollection(allGlyphs.values());
    }

    //----------//
    // getGlyph //
    //----------//
    @Override
    public Glyph getGlyph (Integer id)
    {
        return allGlyphs.get(id);
    }

    //-----------------//
    // getGlyphService //
    //-----------------//
    @Override
    public SelectionService getGlyphService ()
    {
        return glyphService;
    }

    //--------------//
    // getHistogram //
    //--------------//
    @Override
    public Histogram<Integer> getHistogram (Orientation orientation,
                                            Collection<Glyph> glyphs)
    {
        Histogram<Integer> histo = new Histogram<>();

        if (!glyphs.isEmpty()) {
            Rectangle box = Glyphs.getBounds(glyphs);
            Roi roi = new BasicRoi(box);
            histo = roi.getSectionHistogram(
                    orientation,
                    Glyphs.sectionsOf(glyphs));
        }

        return histo;
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
        return getOriginal(glyph.getSignature());
    }

    //-------------//
    // getOriginal //
    //-------------//
    @Override
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

    //------------------//
    // getSelectedGlyph //
    //------------------//
    @Override
    public Glyph getSelectedGlyph ()
    {
        return (Glyph) getGlyphService().getSelection(GlyphEvent.class);
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

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip (Glyph glyph)
    {
        return params.vipGlyphs.contains(glyph.getId());
    }

    //--------------//
    // lookupGlyphs //
    //--------------//
    @Override
    public Set<Glyph> lookupGlyphs (Rectangle rect)
    {
        return Glyphs.lookupGlyphs(getActiveGlyphs(), rect);
    }

    //-------------------------//
    // lookupIntersectedGlyphs //
    //-------------------------//
    @Override
    public Set<Glyph> lookupIntersectedGlyphs (Rectangle rect)
    {
        return Glyphs.lookupIntersectedGlyphs(getActiveGlyphs(), rect);
    }

    //--------------------//
    // lookupVirtualGlyph //
    //--------------------//
    @Override
    public Glyph lookupVirtualGlyph (Point point)
    {
        for (Glyph virtual : virtualGlyphs) {
            if (virtual.getBounds().contains(point)) {
                return virtual;
            }
        }

        return null;
    }

    //------------//
    // mapSection //
    //------------//
    /**
     * Map a section to a glyph, making the glyph active
     *
     * @param section the section to map
     * @param glyph   the assigned glyph
     */
    @Override
    public synchronized void mapSection (Section section,
                                         Glyph glyph)
    {
        if (glyph != null) {
            activeMap.put(section, glyph);
        } else {
            activeMap.remove(section);
        }

        // Invalidate the collection of active glyphs
        activeGlyphs = null;
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
        // First check this physical glyph does not already exist
        Glyph original = getOriginal(glyph);

        if (original != null) {
            if (original != glyph) {
                // Reuse the existing glyph
                if (logger.isDebugEnabled()) {
                    logger.debug("new avatar of #{}{}{}",
                            original.getId(),
                            Sections.
                            toString(" members", glyph.getMembers()),
                            Sections.toString(" original", original.
                            getMembers()));
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
                glyph.setNest(this);
                allGlyphs.put(id, glyph);

                if (isVip(glyph)) {
                    glyph.setVip();
                }
            } else {
                // This is a re-registration
                GlyphSignature oldSig = glyph.getRegisteredSignature();

                if ((oldSig != null) && !newSig.equals(oldSig)) {
                    Glyph oldGlyph = originals.remove(oldSig);

                    if (oldGlyph != null) {
                        logger.debug("Updating registration of {} oldGlyph:{}",
                                glyph.idString(), oldGlyph.getId());
                    }
                }
            }

            originals.put(newSig, glyph);
            glyph.setRegisteredSignature(newSig);

            logger.debug("Registered {} as original {}",
                    glyph.idString(), glyph.getSignature());
        }

        // Special for virtual glyphs
        if (glyph.isVirtual()) {
            virtualGlyphs.add(glyph);
        }

        return glyph;
    }

    //--------------------//
    // removeVirtualGlyph //
    //--------------------//
    @Override
    public synchronized void removeVirtualGlyph (VirtualGlyph glyph)
    {
        originals.remove(glyph.getSignature(), glyph);
        allGlyphs.remove(glyph.getId(), glyph);
        virtualGlyphs.remove(glyph);
        activeGlyphs = null;
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

        for (Class<?> eventClass : glyEventsRead) {
            glyphService.subscribeStrongly(eventClass, this);
        }
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

        for (Class<?> eventClass : glyEventsRead) {
            glyphService.unsubscribe(eventClass, this);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Nest");

        sb.append(" ").append(name);

        // Active/All glyphs
        if (!allGlyphs.isEmpty()) {
            sb.append(" glyphs=").append(getActiveGlyphs().size()).append("/").
                    append(allGlyphs.size());
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
     * @param classe the specific classe
     * @return the number of subscribers interested in the specific class
     */
    protected int subscribersCount (Class<? extends NestEvent> classe)
    {
        return glyphService.subscribersCount(classe);
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
            // Look for set of enclosed active glyphs
            Set<Glyph> glyphsFound = lookupGlyphs(rect);

            // Publish Glyph
            Glyph glyph = glyphsFound.isEmpty() ? null
                    : glyphsFound.iterator().next();
            publish(new GlyphEvent(this, hint, movement, glyph));

            // Publish GlyphSet
            publish(new GlyphSetEvent(this, hint, movement, glyphsFound));
        } else {
            // This is just a point
            Glyph glyph = lookupVirtualGlyph(
                    new Point(rect.getLocation()));

            // Publish virtual Glyph, if any
            if (glyph != null) {
                publish(new GlyphEvent(this, hint, movement, glyph));
            } else {
                // No virtual glyph found, a standard glyph is found by:
                // Pt -> (h/v)run -> (h/v)section -> glyph
                // So there is nothing to do here, except nullifying glyph
                publish(new GlyphEvent(this, hint, movement, null));

                // And let proper lag publish non-null glyph later
                // Since BasicNest is first subscriber on location (berk!)
            }
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
                glyphs = new LinkedHashSet<>();
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
        Glyph compound = null;

        if ((glyphs != null) && (glyphs.size() > 1)) {
            try {
                SystemInfo system = sheet.getSystemOf(glyphs);

                if (system != null) {
                    compound = system.buildTransientCompound(glyphs);
                    publish(
                            new GlyphEvent(
                            this,
                            SelectionHint.GLYPH_TRANSIENT,
                            movement,
                            compound));
                }
            } catch (IllegalArgumentException ex) {
                // All glyphs do not belong to the same system
                // No compound is allowed and displayed
                logger.warn("Selecting glyphs from different systems");
            }
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

        //TODO: Check the need for this:
        //        // Nullify Run  entity
        //        publish(new RunEvent(this, hint, movement, null));
        //
        //        // Nullify Section entity
        //        publish(new SectionEvent<GlyphSection>(this, hint, movement, null));

        // Report Glyph entity (which may be null)
        publish(new GlyphEvent(this, hint, movement, getGlyph(id)));
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.String vipGlyphs = new Constant.String(
                "",
                "(Debug) Comma-separated list of VIP glyphs");

    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all constants related to nest
     */
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        final List<Integer> vipGlyphs; // List of IDs for VIP glyphs

        //~ Constructors -------------------------------------------------------
        public Parameters ()
        {
            vipGlyphs = VipUtil.decodeIds(constants.vipGlyphs.getValue());

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }

            if (!vipGlyphs.isEmpty()) {
                logger.info("VIP glyphs: {}", vipGlyphs);
            }
        }
    }
}
