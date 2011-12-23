//----------------------------------------------------------------------------//
//                                                                            //
//                             B a s i c N e s t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
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

import omr.log.Logger;

import omr.math.Histogram;

import omr.run.Orientation;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

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

import org.bushe.swing.event.EventSubscriber;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class {@code BasicNest} implements a {@link Nest}.
 *
 * @author Hervé Bitteur
 */
public class BasicNest
    implements Nest, EventSubscriber<UserEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BasicNest.class);

    /** Events read on location service */
    public static final Class[] locEventsRead = new Class[] { LocationEvent.class };

    /** Events read on nest (glyph) service */
    public static final Class[] glyEventsRead = new Class[] {
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
    private final ConcurrentHashMap<GlyphSignature, Glyph> originals = new ConcurrentHashMap<GlyphSignature, Glyph>();

    /**
     * Collection of all glyphs ever inserted in this Nest, indexed by
     * glyph id. No non-virtual glyph is ever removed from this map.
     */
    private final ConcurrentHashMap<Integer, Glyph> allGlyphs = new ConcurrentHashMap<Integer, Glyph>();

    /**
     * Current map of section -> glyphs.
     * This defines the glyphs that are currently active, since there is at
     * least one section pointing to them (the sections collection is immutable).
     * Nota: The glyph reference within the section is kept in sync
     */
    private final ConcurrentHashMap<Section, Glyph> activeMap = new ConcurrentHashMap<Section, Glyph>();

    /**
     * Collection of active glyphs.
     * This is derived from the activeMap, to give direct access to all the
     * active glyphs, and is kept in sync with activeMap.
     * It also contains the virtual glyphs since these are always active.
     */
    private Set<Glyph> activeGlyphs;

    /** Collection of virtual glyphs (with no underlying sections). */
    private Set<Glyph> virtualGlyphs = new HashSet<Glyph>();

    /** Global id to uniquely identify a glyph. */
    private final AtomicInteger globalGlyphId = new AtomicInteger(0);

    /** Location service (read & write). */
    private SelectionService locationService;

    /** Hosted glyph service (Glyph, GlyphId and GlyphSet). */
    protected final SelectionService glyphService;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // BasicNest //
    //-----------//
    /**
     * Create a glyph nest.
     * @param name the distinguished name for this instance
     */
    public BasicNest (String name,
                      Sheet  sheet)
    {
        this.name = name;
        this.sheet = sheet;

        params = new Parameters();
        glyphService = new SelectionService(name, Nest.eventsWritten);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getActiveGlyphs //
    //-----------------//
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
    public Collection<Glyph> getAllGlyphs ()
    {
        return Collections.unmodifiableCollection(allGlyphs.values());
    }

    //----------//
    // getGlyph //
    //----------//
    public Glyph getGlyph (Integer id)
    {
        return allGlyphs.get(id);
    }

    //    //--------------------//
    //    // transferAlienGlyph //
    //    //--------------------//
    //    /**
    //     * Transfer a glyph (from another lag) to this one
    //     * @param alien the glyph to be copied from the other lag
    //     * @return the (reified) glyph in this lag
    //     */
    //    private Glyph transferAlienGlyph (Glyph alien)
    //    {
    //        if (logger.isFineEnabled()) {
    //            logger.fine("Transfering " + alien);
    //        }
    //
    //        Set<GlyphSection> newSections = new HashSet<GlyphSection>();
    //
    //        for (GlyphSection section : alien.getMembers()) {
    //            GlyphSection newSection = getVertexBySignature(
    //                section.getSignature());
    //
    //            if (newSection == null) {
    //                logger.warning("Could not retrieve section " + section);
    //
    //                return null;
    //            }
    //
    //            newSections.add(newSection);
    //        }
    //
    //        // Create Glyph from sections
    //        Glyph glyph = null;
    //
    //        if (alien instanceof Stick) {
    //            glyph = new BasicStick(alien.getInterline());
    //        } else {
    //            glyph = new BasicGlyph(alien.getInterline());
    //        }
    //
    //        for (GlyphSection section : newSections) {
    //            glyph.addSection(section, Glyph.Linking.LINK_BACK);
    //        }
    //
    //        // Add/get original glyph
    //        Glyph orgGlyph = addGlyph(glyph);
    //
    //        return orgGlyph;
    //    }

    //-----------------//
    // getGlyphService //
    //-----------------//
    public SelectionService getGlyphService ()
    {
        return glyphService;
    }

    //--------------//
    // getHistogram //
    //--------------//
    public Histogram<Integer> getHistogram (Orientation       orientation,
                                            Collection<Glyph> glyphs)
    {
        Histogram<Integer> histo = new Histogram<Integer>();

        if (!glyphs.isEmpty()) {
            PixelRectangle box = Glyphs.getContourBox(glyphs);
            Roi            roi = new BasicRoi(box);
            histo = roi.getSectionHistogram(
                orientation,
                Glyphs.sectionsOf(glyphs));
        }

        return histo;
    }

    //---------//
    // getName //
    //---------//
    public String getName ()
    {
        return name;
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
        if (oldGlyph.getSignature()
                    .compareTo(signature) == 0) {
            return oldGlyph;
        } else {
            if (logger.isFineEnabled()) {
                logger.info("Obsolete signature for " + oldGlyph);
            }

            return null;
        }
    }

    //------------------//
    // getSelectedGlyph //
    //------------------//
    public Glyph getSelectedGlyph ()
    {
        return (Glyph) getGlyphService()
                           .getSelection(GlyphEvent.class);
    }

    //---------------------//
    // getSelectedGlyphSet //
    //---------------------//
    @SuppressWarnings("unchecked")
    public Set<Glyph> getSelectedGlyphSet ()
    {
        return (Set<Glyph>) getGlyphService()
                                .getSelection(GlyphSetEvent.class);
    }

    //-------------//
    // setServices //
    //-------------//
    public void setServices (SelectionService locationService)
    {
        this.locationService = locationService;

        for (Class eventClass : locEventsRead) {
            locationService.subscribeStrongly(eventClass, this);
        }

        for (Class eventClass : glyEventsRead) {
            glyphService.subscribeStrongly(eventClass, this);
        }
    }

    //-------//
    // isVip //
    //-------//
    public boolean isVip (Glyph glyph)
    {
        return params.vipGlyphs.contains(glyph.getId());
    }

    //----------//
    // addGlyph //
    //----------//
    public Glyph addGlyph (Glyph glyph)
    {
        glyph = registerGlyph(glyph);

        // Make absolutely all its sections point back to it
        glyph.linkAllSections();

        if (glyph.isVip()) {
            logger.info("Glyph#" + glyph.getId() + " added");
        }

        return glyph;
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump (String title)
    {
        if (title != null) {
            System.out.println(title);
        }

        // Dump of active glyphs
        System.out.println(
            "\nActive glyphs (" + getActiveGlyphs().size() + ") :");

        for (Glyph glyph : getActiveGlyphs()) {
            System.out.println(glyph.toString());
        }

        // Dump of inactive glyphs
        Collection<Glyph> inactives = new ArrayList<Glyph>(getAllGlyphs());
        inactives.removeAll(getActiveGlyphs());
        System.out.println("\nInactive glyphs (" + inactives.size() + ") :");

        for (Glyph glyph : inactives) {
            System.out.println(glyph.toString());
        }
    }

    //--------------//
    // lookupGlyphs //
    //--------------//
    public Set<Glyph> lookupGlyphs (PixelRectangle rect)
    {
        return Glyphs.lookupGlyphs(getActiveGlyphs(), rect);
    }

    //-------------------------//
    // lookupIntersectedGlyphs //
    //-------------------------//
    public Set<Glyph> lookupIntersectedGlyphs (PixelRectangle rect)
    {
        return Glyphs.lookupIntersectedGlyphs(getActiveGlyphs(), rect);
    }

    //--------------------//
    // lookupVirtualGlyph //
    //--------------------//
    public Glyph lookupVirtualGlyph (PixelPoint point)
    {
        for (Glyph virtual : virtualGlyphs) {
            if (virtual.getContourBox()
                       .contains(point)) {
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
     * @param glyph the assigned glyph
     */
    public synchronized void mapSection (Section section,
                                         Glyph   glyph)
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
            logger.warning(getClass().getName() + " onEvent error", ex);
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
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "new avatar of #" + original.getId() +
                        Sections.toString(" members", glyph.getMembers()) +
                        Sections.toString(" original", original.getMembers()));
                }

                glyph = original;
                glyph.setPartOf(null);
            }
        } else {
            // Create a brand new glyph
            final int id = generateId();
            glyph.setId(id);
            glyph.setNest(this);
            originals.put(glyph.getSignature(), glyph);
            allGlyphs.put(id, glyph);

            if (isVip(glyph)) {
                glyph.setVip();
            }

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Registered glyph #" + glyph.getId() + " as original " +
                    glyph.getSignature());
            }
        }

        // Special for virtual glyphs
        if (glyph.isVirtual()) {
            virtualGlyphs.add(glyph);
        }

        //        if (glyph.isVip()) {
        //            logger.info("Glyph#" + glyph.getId() + " registered");
        //        }
        return glyph;
    }

    //--------------------//
    // removeVirtualGlyph //
    //--------------------//
    public synchronized void removeVirtualGlyph (VirtualGlyph glyph)
    {
        originals.remove(glyph.getSignature(), glyph);
        allGlyphs.remove(glyph.getId(), glyph);
        virtualGlyphs.remove(glyph);
        activeGlyphs = null;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Nest");

        sb.append(" ")
          .append(name);

        // Active/All glyphs
        if (!allGlyphs.isEmpty()) {
            sb.append(" glyphs=")
              .append(getActiveGlyphs().size())
              .append("/")
              .append(allGlyphs.size());
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
     * @param classe the specific classe
     * @return the number of subscribers interested in the specific class
     */
    protected int subscribersCount (Class<?extends NestEvent> classe)
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
     *  Interest in sheet location => [active] glyph(s)
     * @param locationEvent
     */
    private void handleEvent (LocationEvent locationEvent)
    {
        SelectionHint  hint = locationEvent.hint;
        MouseMovement  movement = locationEvent.movement;
        PixelRectangle rect = locationEvent.getData();

        if ((hint != LOCATION_ADD) && (hint != LOCATION_INIT)) {
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
                          : glyphsFound.iterator()
                                       .next();
            publish(new GlyphEvent(this, hint, movement, glyph));

            // Publish GlyphSet
            publish(new GlyphSetEvent(this, hint, movement, glyphsFound));
        } else {
            // This is just a point
            Glyph glyph = lookupVirtualGlyph(
                new PixelPoint(rect.getLocation()));

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
     * @param glyphEvent
     */
    private void handleEvent (GlyphEvent glyphEvent)
    {
        SelectionHint hint = glyphEvent.hint;
        MouseMovement movement = glyphEvent.movement;
        Glyph         glyph = glyphEvent.getData();

        if ((hint == GLYPH_INIT) || (hint == GLYPH_MODIFIED)) {
            // Display glyph contour
            if (glyph != null) {
                PixelRectangle box = glyph.getContourBox();
                publish(new LocationEvent(this, hint, movement, box));
            }
        }

        // In glyph-selection mode, for non-transient glyphs
        // (and only if we have interested subscribers)
        if ((hint != GLYPH_TRANSIENT) &&
            !ViewParameters.getInstance()
                           .isSectionSelectionEnabled() &&
            (subscribersCount(GlyphSetEvent.class) > 0)) {
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
            } else {
                // Overwriting the set of glyphs
                if (glyph != null) {
                    // Make a one-glyph set
                    glyphs = Glyphs.sortedSet(glyph);
                } else {
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
     * @param glyphSetEvent
     */
    private void handleEvent (GlyphSetEvent glyphSetEvent)
    {
        if (ViewParameters.getInstance()
                          .isSectionSelectionEnabled()) {
            // Section mode
            return;
        }

        // Glyph mode
        MouseMovement movement = glyphSetEvent.movement;
        Set<Glyph>    glyphs = glyphSetEvent.getData();
        Glyph         compound = null;

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
                logger.warning(
                    "Glyphs from different systems " + Glyphs.toString(glyphs));
            }
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Glyph ID => glyph
     * @param glyphIdEvent
     */
    private void handleEvent (GlyphIdEvent glyphIdEvent)
    {
        SelectionHint hint = glyphIdEvent.hint;
        MouseMovement movement = glyphIdEvent.movement;
        int           id = glyphIdEvent.getData();

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
            vipGlyphs = decode(constants.vipGlyphs.getValue());

            if (logger.isFineEnabled()) {
                Main.dumping.dump(this);
            }

            if (!vipGlyphs.isEmpty()) {
                logger.info("VIP glyphs: " + vipGlyphs);
            }
        }

        //~ Methods ------------------------------------------------------------

        private List<Integer> decode (String str)
        {
            List<Integer>   ids = new ArrayList<Integer>();

            // Retrieve the list of ids
            StringTokenizer st = new StringTokenizer(str, ",");

            while (st.hasMoreTokens()) {
                try {
                    ids.add(Integer.decode(st.nextToken().trim()));
                } catch (Exception ex) {
                    logger.warning("Illegal glyph id", ex);
                }
            }

            return ids;
        }
    }
}
