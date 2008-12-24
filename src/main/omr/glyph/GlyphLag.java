//----------------------------------------------------------------------------//
//                                                                            //
//                              G l y p h L a g                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.lag.Lag;
import omr.lag.Oriented;
import omr.lag.Section;

import omr.log.Logger;

import omr.selection.GlyphEvent;
import omr.selection.GlyphIdEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.MouseMovement;
import omr.selection.RunEvent;
import omr.selection.SectionEvent;
import omr.selection.SelectionHint;
import static omr.selection.SelectionHint.*;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.util.Implement;

import org.bushe.swing.event.EventSubscriber;

import java.awt.Rectangle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class <code>GlyphLag</code> is a lag of {@link GlyphSection} instances which
 * can be aggregated into {@link Glyph}instances. A GlyphLag keeps an internal
 * collection of all defined glyphs.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphLag
    extends Lag<GlyphLag, GlyphSection>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphLag.class);

    //~ Instance fields --------------------------------------------------------

    /**
     * Smart glyph map, usable across several glyph extractions, to ensure glyph
     * unicity. A glyph is never removed, it can be active or inactive.
     */
    private final ConcurrentHashMap<GlyphSignature, Glyph> originals = new ConcurrentHashMap<GlyphSignature, Glyph>();

    /**
     * Collection of all glyphs ever inserted in this GlyphLag, indexed by
     * glyph id. No glyph is ever removed from this map.
     */
    private final ConcurrentHashMap<Integer, Glyph> allGlyphs = new ConcurrentHashMap<Integer, Glyph>();

    /**
     * Current map of section -> glyphs. This defines the glyphs that are
     * currently active, since there is at least one section pointing to them
     * (and the sections collection is immutable).
     */
    private final ConcurrentHashMap<GlyphSection, Glyph> glyphMap = new ConcurrentHashMap<GlyphSection, Glyph>();

    /**
     * Collection of active glyphs. This is derived from the glyphMap, to give
     * direct access to all the active glyphs. It is kept in sync with glyphMap.
     */
    private volatile SortedSet<Glyph> activeGlyphs;

    /** Global id to uniquely identify a glyph */
    private AtomicInteger globalGlyphId = new AtomicInteger(0);

    //~ Constructors -----------------------------------------------------------

    //----------//
    // GlyphLag //
    //----------//
    /**
     * Create a glyph lag, with a pre-defined orientation
     *
     * @param name the distinguished name for this instance
     * @param sectionClass the precise class for instantiating sections
     * @param orientation the desired orientation of the lag
     */
    public GlyphLag (String                       name,
                     Class<?extends GlyphSection> sectionClass,
                     Oriented                     orientation)
    {
        super(name, sectionClass, orientation);
        selfSubscribe(GlyphEvent.class);
        selfSubscribe(GlyphIdEvent.class);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getActiveGlyphs //
    //-----------------//
    /**
     * Export the unmodifiable collection of active glyphs of the lag.
     *
     * @return the collection of glyphs for which at least a section is assigned
     */
    public synchronized Collection<Glyph> getActiveGlyphs ()
    {
        if (activeGlyphs == null) {
            //synchronized (this) {
            if (activeGlyphs == null) {
                activeGlyphs = new TreeSet<Glyph>(glyphMap.values());
            }

            //}
        }

        return Collections.unmodifiableCollection(activeGlyphs);
    }

    //--------------//
    // getAllGlyphs //
    //--------------//
    /**
     * Export the whole unmodifiable collection of glyphs of the lag.
     *
     * @return the collection of glyphs, both active and inactive
     */
    public Collection<Glyph> getAllGlyphs ()
    {
        return Collections.unmodifiableCollection(allGlyphs.values());
    }

    //----------//
    // getGlyph //
    //----------//
    /**
     * Retrieve a glyph via its Id among the lag collection of glyphs
     *
     * @param id the glyph id to search for
     * @return the glyph found, or null otherwise
     */
    public Glyph getGlyph (Integer id)
    {
        return allGlyphs.get(id);
    }

    //-------------//
    // getOriginal //
    //-------------//
    /**
     * Return the original glyph, if any, that the provided glyph duplicates
     * @param glyph the provided glyph
     * @return the original for this glyph, if any, otherwise null
     */
    public Glyph getOriginal (Glyph glyph)
    {
        return getOriginal(glyph.getSignature());
    }

    //-------------//
    // getOriginal //
    //-------------//
    /**
     * Return the original glyph, if any,  that corresponds to the provided
     * signature
     * @param signature the provided signature
     * @return the original glyph for this signature, if any, otherwise null
     */
    public Glyph getOriginal (GlyphSignature signature)
    {
        return originals.get(signature);
    }

    //----------//
    // addGlyph //
    //----------//
    /**
     * Add a glyph in the graph, making sure we do not duplicate an existing
     * glyph (a glyph being really defined by the set of its member sections)
     *
     * @param glyph the glyph to add to the lag
     * @return the actual glyph (existing or brand new)
     */
    public Glyph addGlyph (Glyph glyph)
    {
        // First check this glyph does not already exist
        Glyph original = getOriginal(glyph);

        if ((original != null) && (original != glyph)) {
            // Reuse the existing glyph
            if (logger.isFineEnabled()) {
                logger.fine(
                    "new avatar of #" + original.getId() + " members =" +
                    Section.toString(glyph.getMembers()) + " original=" +
                    Section.toString(original.getMembers()));
            }

            glyph = original;
        } else {
            // Create a brand new glyph
            final int id = generateId();
            glyph.setId(id);
            glyph.setLag(this);
            originals.put(glyph.getSignature(), glyph);
            allGlyphs.put(id, glyph);

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Registered glyph #" + glyph.getId() + " as original " +
                    glyph.getSignature());
            }
        }

        // Make all its sections point to it
        for (GlyphSection section : glyph.getMembers()) {
            section.setGlyph(glyph);
        }

        return glyph;
    }

    //------//
    // dump //
    //------//
    /**
     * Print out major internal info about this glyph lag.
     *
     * @param title a specific title to be used for the dump
     */
    @Override
    public void dump (String title)
    {
        // Normal dump of all sections
        ///super.dump(title);

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
    /**
     * Look up in a collection of glyphs for <b>all</b> glyphs contained in a
     * provided rectangle
     *
     * @param collection the collection of glyphs to be browsed
     * @param rect the coordinates rectangle
     *
     * @return the glyphs found, which may be an empty list
     */
    public List<Glyph> lookupGlyphs (Collection<?extends Glyph> collection,
                                     Rectangle                  rect)
    {
        List<Glyph> list = new ArrayList<Glyph>();

        for (Glyph glyph : collection) {
            boolean inRect = true;
            sectionTest: 
            for (GlyphSection section : glyph.getMembers()) {
                if (!rect.contains(section.getContourBox())) {
                    inRect = false;

                    break sectionTest;
                }
            }

            if (inRect) {
                list.add(glyph);
            }
        }

        return list;
    }

    //--------------//
    // lookupGlyphs //
    //--------------//
    /**
     * Look up for <b>all</b> active glyphs contained in a provided rectangle
     *
     * @param rect the coordinates rectangle
     *
     * @return the glyphs found, which may be an empty list
     */
    public List<Glyph> lookupGlyphs (Rectangle rect)
    {
        return lookupGlyphs(getActiveGlyphs(), rect);
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered on selection notification.  We forward glyph
     * information.
     *
     * @param event the notified event
     */
    @Override
    @SuppressWarnings("unchecked")
    @Implement(EventSubscriber.class)
    public void onEvent (UserEvent event)
    {
        // Ignore RELEASING
        if (event.movement == MouseMovement.RELEASING) {
            return;
        }

        //        logger.info(
        //            "GlyphLag/" + getClass().getSimpleName() + " " + getName() + ":" +
        //            event);

        // Keep normal lag behavior
        // (interest in sheet location, section and section ID)
        super.onEvent(event);

        // Additional tasks
        if (event instanceof SheetLocationEvent) {
            // Interest in sheet location => active glyph(s)
            SheetLocationEvent sheetLocation = (SheetLocationEvent) event;

            if ((sheetLocation.hint == LOCATION_ADD) ||
                (sheetLocation.hint == LOCATION_INIT)) {
                Rectangle rect = sheetLocation.rectangle;

                if (rect != null) {
                    if ((rect.width > 0) || (rect.height > 0)) {
                        // This is a non-degenerated rectangle
                        // Look for enclosed active glyphs
                        if (subscribersCount(GlyphSetEvent.class) > 0) {
                            List<Glyph> glyphsFound = lookupGlyphs(rect);

                            if (glyphsFound.size() > 0) {
                                publish(
                                    new GlyphEvent(
                                        this,
                                        sheetLocation.hint,
                                        sheetLocation.movement,
                                        glyphsFound.get(glyphsFound.size() - 1)));
                            } else {
                                publish(
                                    new GlyphEvent(
                                        this,
                                        sheetLocation.hint,
                                        sheetLocation.movement,
                                        null));
                            }

                            publish(
                                new GlyphSetEvent(
                                    this,
                                    sheetLocation.hint,
                                    sheetLocation.movement,
                                    glyphsFound));
                        }
                    } else {
                        // This is just a point
                        // If a section has just been found,
                        // forward its assigned glyph if any
                        if ((subscribersCount(GlyphEvent.class) > 0) &&
                            (subscribersCount(SectionEvent.class) > 0)) { // TBD GlyphLag itself

                            Glyph                      glyph = null;
                            SectionEvent<GlyphSection> sectionEvent = (SectionEvent<GlyphSection>) eventService.getLastEvent(
                                SectionEvent.class);
                            GlyphSection               section = (sectionEvent != null)
                                                                 ? sectionEvent.section
                                                                 : null;

                            if (section != null) {
                                glyph = section.getGlyph();
                            }

                            publish(
                                new GlyphEvent(
                                    this,
                                    sheetLocation.hint,
                                    null,
                                    glyph));
                        }
                    }
                }
            }
        } else if (event instanceof SectionEvent) {
            // Interest in Section => assigned glyph
            SectionEvent<GlyphSection> sectionEvent = (SectionEvent<GlyphSection>) event;

            if (sectionEvent.hint == SECTION_INIT) {
                // Select related Glyph if any
                GlyphSection section = sectionEvent.section;

                if (section != null) {
                    publish(
                        new GlyphEvent(
                            this,
                            sectionEvent.hint,
                            sectionEvent.movement,
                            section.getGlyph()));
                }
            }
        } else if (event instanceof GlyphEvent) {
            // Interest in Glyph => glyph contour
            GlyphEvent glyphEvent = (GlyphEvent) event;
            Glyph      glyph = glyphEvent.glyph;

            if ((glyphEvent.hint == GLYPH_INIT) ||
                (glyphEvent.hint == GLYPH_MODIFIED)) {
                // Display glyph contour
                if (glyph != null) {
                    ///locationService.setEntity(glyph.getContourBox(), hint);
                    locationService.publish(
                        new SheetLocationEvent(
                            this,
                            glyphEvent.hint,
                            glyphEvent.movement,
                            glyph.getContourBox()));
                }
            }

            if (glyphEvent.hint != GLYPH_TRANSIENT) {
                // Update (vertical) glyph set
                updateGlyphSet(glyph, glyphEvent.hint);
            }
        } else if (event instanceof GlyphIdEvent) {
            // Interest in Glyph ID => glyph
            GlyphIdEvent glyphIdEvent = (GlyphIdEvent) event;

            // Lookup a glyph with proper ID
            // Nullify Run & Section entities
            publish(new RunEvent(this, null));
            publish(new SectionEvent(this, glyphIdEvent.hint, null));

            // Report Glyph entity
            Integer id = glyphIdEvent.getData();
            publish(
                new GlyphEvent(this, glyphIdEvent.hint, null, getGlyph(id)));
        }
    }

    //----------//
    // toString //
    //----------//
    /**
     * Return a readable description
     *
     * @return the descriptive string
     */
    @Override
    public String toString ()
    {
        StringBuffer sb = new StringBuffer(256);

        sb.append(super.toString());

        //        // Active/All glyphs
        //        sb.append(" glyphs=")
        //          .append(getActiveGlyphs().size())
        //          .append("/")
        //          .append(allGlyphs.size());
        if (this.getClass()
                .getName()
                .equals(GlyphLag.class.getName())) {
            sb.append("}");
        }

        return sb.toString();
    }

    //-----------//
    // getPrefix //
    //-----------//
    /**
     * Return a distinctive string, to be used as a prefix in toString() for
     * example.
     *
     * @return the prefix string
     */
    @Override
    protected String getPrefix ()
    {
        return "GlyphLag";
    }

    //------------//
    // mapSection //
    //------------//
    /**
     * (package access from {@link GlyphSection})
     * Map a section to a glyph, making the glyph active
     *
     * @param section the section to map
     * @param glyph the assigned glyph
     */
    synchronized void mapSection (GlyphSection section,
                                  Glyph        glyph)
    {
        if (glyph != null) {
            glyphMap.put(section, glyph);
        } else {
            glyphMap.remove(section);
        }

        // Invalidate the collection of active glyphs
        activeGlyphs = null;
    }

    //------------//
    // generateId //
    //------------//
    private int generateId ()
    {
        return globalGlyphId.incrementAndGet();
    }

    //----------------//
    // updateGlyphSet //
    //----------------//
    private void updateGlyphSet (Glyph         glyph,
                                 SelectionHint hint)
    {
        if (subscribersCount(GlyphSetEvent.class) > 0) {
            // Get current glyph set
            GlyphSetEvent glyphsEvent = (GlyphSetEvent) eventService.getLastEvent(
                GlyphSetEvent.class);
            List<Glyph>   glyphs = (glyphsEvent != null)
                                   ? glyphsEvent.getData() : null;

            if (glyphs == null) {
                glyphs = new ArrayList<Glyph>();
            }

            if (hint == LOCATION_ADD) {
                // Adding / Removing
                if (glyph != null) {
                    // Add to (or remove from) glyph set
                    if (glyphs.contains(glyph)) {
                        glyphs.remove(glyph);
                    } else {
                        glyphs.add(glyph);
                    }

                    publish(new GlyphSetEvent(this, hint, null, glyphs));
                }
            } else {
                // Overwriting
                if (glyph != null) {
                    // Make a one-glyph set
                    glyphs.clear();
                    glyphs.add(glyph);
                } else if (glyphs.size() > 0) {
                    // Empty the glyph set
                    glyphs.clear();
                }

                publish(new GlyphSetEvent(this, hint, null, glyphs));
            }
        }
    }
}
