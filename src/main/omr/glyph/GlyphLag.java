//----------------------------------------------------------------------------//
//                                                                            //
//                              G l y p h L a g                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.BasicStick;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;

import omr.lag.Lag;
import omr.lag.Oriented;
import omr.lag.Section;

import omr.log.Logger;

import omr.selection.GlyphEvent;
import omr.selection.GlyphSetEvent;

import java.awt.Rectangle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class <code>GlyphLag</code> is a lag of {@link GlyphSection} instances which
 * can be aggregated into {@link Glyph} instances. A GlyphLag keeps an internal
 * collection of all defined glyphs.
 *
 * <p>A glyph is made of member sections and always keeps a collection of its
 * member sections. Sections are made of runs of pixels and thus sections do not
 * overlap. Different glyphs can have sections in common, and in that case they
 * overlap, however only one of these glyphs is the current "owner" of these
 * common sections. It is known as being "active" while the others are inactive:
 * <ul>
 * <li>Active glyph: The member sections point to the glyph.</li>
 * <li>Inactive glyph: The member sections don't point to this glyph (they
 * usually point to some other (active) glyph).</li>
 * </ul>
 * </p>
 *
 * <p>Selecting a (foreground) pixel, thus selects its containing section, and
 * its active glyph if any.</p>
 *
 * @author Hervé Bitteur
 */
public class GlyphLag
    extends Lag<GlyphLag, GlyphSection>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphLag.class);

    //~ Instance fields --------------------------------------------------------

    /**
     * Smart glyph map, based on a physical glyph signature, and thus usable
     * across several glyph extractions, to ensure glyph unicity whatever the
     * sequential ID it is assigned.
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
     * Nota: The glyph reference within the section is kept in sync
     */
    private final ConcurrentHashMap<GlyphSection, Glyph> activeMap = new ConcurrentHashMap<GlyphSection, Glyph>();

    /**
     * Collection of active glyphs. This is derived from the activeMap, to give
     * direct access to all the active glyphs. It is kept in sync with activeMap.
     */
    private Set<Glyph> activeGlyphs;

    /** Global id to uniquely identify a glyph */
    private final AtomicInteger globalGlyphId = new AtomicInteger(0);

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
            activeGlyphs = Glyphs.sortedSet(activeMap.values());
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

    //------------------//
    // getSelectedGlyph //
    //------------------//
    public Glyph getSelectedGlyph ()
    {
        return (Glyph) getSelectionService()
                           .getSelection(GlyphEvent.class);
    }

    //---------------------//
    // getSelectedGlyphSet //
    //---------------------//
    @SuppressWarnings("unchecked")
    public Set<Glyph> getSelectedGlyphSet ()
    {
        return (Set<Glyph>) getSelectionService()
                                .getSelection(GlyphSetEvent.class);
    }

    //----------//
    // addGlyph //
    //----------//
    /**
     * Add a glyph in the graph, making sure we do not duplicate any existing
     * glyph (a glyph being really defined by the set of its member sections)
     *
     * @param glyph the glyph to add to the lag
     * @return the actual glyph (already existing or brand new)
     */
    public Glyph addGlyph (Glyph glyph)
    {
        // First check this physical glyph does not already exist
        Glyph original = getOriginal(glyph);

        if (original != null) {
            if (original != glyph) {
                // Reuse the existing glyph
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "new avatar of #" + original.getId() + " members=" +
                        Section.toString(glyph.getMembers()) + " original=" +
                        Section.toString(original.getMembers()));
                }

                glyph = original;
            }
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

        // Make absolutely all its sections point back to it
        glyph.linkAllSections();

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
    public Set<Glyph> lookupGlyphs (Collection<?extends Glyph> collection,
                                    Rectangle                  rect)
    {
        Set<Glyph> set = new LinkedHashSet<Glyph>();

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
                set.add(glyph);
            }
        }

        return set;
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
    public Set<Glyph> lookupGlyphs (Rectangle rect)
    {
        return lookupGlyphs(getActiveGlyphs(), rect);
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

    //----------------------//
    // transferManualGlyphs //
    //----------------------//
    /**
     * Transfer all manually assigned shapes from the old lag, and store them in
     * this lag
     * @param oldLag the lag to "copy" manual glyphs from
     */
    public void transferManualGlyphs (GlyphLag oldLag)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "Transfering manual glyphs from " + oldLag + " to " + this);
        }

        Set<Glyph> transfered = new HashSet<Glyph>();

        for (Glyph alien : oldLag.getActiveGlyphs()) {
            if (alien.isManualShape()) {
                Glyph glyph = transferAlienGlyph(alien);

                // Transfer shape info (what else?)
                glyph.setShape(alien.getShape(), alien.getDoubt());

                transfered.add(glyph);
            }
        }

        if (!transfered.isEmpty()) {
            logger.info(
                "Transfered " + transfered.size() + " glyph" +
                ((transfered.size() == 1) ? "" : "s") + " from previous lag");
        }
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
     * @param alien the assigned glyph
     */
    synchronized void mapSection (GlyphSection section,
                                  Glyph        glyph)
    {
        if (glyph != null) {
            activeMap.put(section, glyph);
        } else {
            activeMap.remove(section);
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

    //--------------------//
    // transferAlienGlyph //
    //--------------------//
    /**
     * Transfer a glyph (from another lag) to this one
     * @param alien the glyph to be copied from the other lag
     * @return the (reified) glyph in this lag
     */
    private Glyph transferAlienGlyph (Glyph alien)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Transfering " + alien);
        }

        Set<GlyphSection> newSections = new HashSet<GlyphSection>();

        for (GlyphSection section : alien.getMembers()) {
            GlyphSection newSection = getVertexBySignature(
                section.getSignature());

            if (newSection == null) {
                logger.warning("Could not retrieve section " + section);

                return null;
            }

            newSections.add(newSection);
        }

        // Create Glyph from sections
        Glyph glyph = null;

        if (alien instanceof Stick) {
            glyph = new BasicStick(alien.getInterline());
        } else {
            glyph = new BasicGlyph(alien.getInterline());
        }

        for (GlyphSection section : newSections) {
            glyph.addSection(section, true);
        }

        // Add/get original glyph
        Glyph orgGlyph = addGlyph(glyph);

        return orgGlyph;
    }
}
