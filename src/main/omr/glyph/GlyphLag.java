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

import omr.selection.Selection;
import omr.selection.SelectionHint;
import static omr.selection.SelectionHint.*;
import omr.selection.SelectionTag;

import omr.util.Logger;

import java.awt.Rectangle;
import java.util.*;

/**
 * Class <code>GlyphLag</code> is a lag of {@link GlyphSection} instances which
 * can be aggregated into {@link Glyph}instances. A GlyphLag keeps an internal
 * collection of all defined glyphs.
 *
 * <dl>
 * <dt><b>Selection Inputs:</b> (On top of {@link Lag} inputs)</dt><ul>
 * <li>PIXEL Location (if LOCATION_INIT)
 * <li>*_SECTION (if SECTION_INIT)
 * <li>*_GLYPH (if GLYPH_INIT)
 * <li>*_GLYPH_ID
 * </ul>
 *
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>PIXEL Contour
 * <li>*_RUN
 * <li>*_SECTION
 * <li>*_GLYPH
 * <li>GLYPH_SET
 * </ul>
 * </dl>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 * @see #setGlyphSelection
 */
public class GlyphLag
    extends Lag<GlyphLag, GlyphSection>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphSection.class);

    //~ Instance fields --------------------------------------------------------

    /**
     * Smart glyph map, usable across several glyph extractions, to ensure glyph
     * unicity. A glyph is never removed, it can be active or inactive.
     */
    private final SortedMap<GlyphSignature, Glyph> originals = new TreeMap<GlyphSignature, Glyph>();

    /**
     * Collection of all glyphs ever inserted in this GlyphLag, indexed by
     * glyph id. No glyph is ever removed from this map.
     */
    private final SortedMap<Integer, Glyph> allGlyphs = new TreeMap<Integer, Glyph>();

    /**
     * Current map of section -> glyphs. This defines the glyphs that are
     * currently active, since there is at least one section pointing to them
     * (and the sections collection is immutable).
     */
    private final SortedMap<GlyphSection, Glyph> glyphMap = new TreeMap<GlyphSection, Glyph>();

    /**
     * Collection of active glyphs. This is derived from the glyphMap, to give
     * direct access to all the active glyphs. It is kept in sync with glyphMap.
     */
    private SortedSet<Glyph> activeGlyphs;

    /** Selection on glyph, output where found glyph is written */
    private transient Selection glyphSelection;

    /** Selection on glyphs, output where found glyphs are written */
    private transient Selection glyphSetSelection;

    /** Global id to uniquely identify a glyph */
    private int globalGlyphId = 0;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // GlyphLag //
    //----------//
    /**
     * Create a glyph lag, with a pre-defined orientation
     *
     * @param name the distinguished name for this instance
     * @param orientation the desired orientation of the lag
     */
    public GlyphLag (String   name,
                     Oriented orientation)
    {
        super(name, orientation);
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
    public Collection<Glyph> getActiveGlyphs ()
    {
        if (activeGlyphs == null) {
            activeGlyphs = new TreeSet<Glyph>(glyphMap.values());
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

    //-------------------//
    // setGlyphSelection //
    //-------------------//
    /**
     * Inject dependency about the output selection for a found glyph
     *
     * @param glyphSelection the output glyph selection
     */
    public void setGlyphSelection (Selection glyphSelection)
    {
        this.glyphSelection = glyphSelection;
    }

    //----------------------//
    // setGlyphSetSelection //
    //----------------------//
    /**
     * Inject dependency about the output selection for found glyph set
     *
     * @param glyphSetSelection the output glyph set selection
     */
    public void setGlyphSetSelection (Selection glyphSetSelection)
    {
        this.glyphSetSelection = glyphSetSelection;
    }

    //----------------//
    // getLastGlyphId //
    //----------------//
    /**
     * Report the latest glyph id so far in this lag
     *
     * @return the latest glyph id in the glyph lag
     */
    public int getLastGlyphId ()
    {
        return globalGlyphId;
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
        Glyph original = originals.get(glyph.getSignature());

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
            allGlyphs.put(++globalGlyphId, glyph);
            glyph.setId(globalGlyphId);
            glyph.setLag(this);
            originals.put(glyph.getSignature(), glyph);

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Registered glyph #" + glyph.getId() + " as original");
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
     * Prints out major internal info about this glyph lag.
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

        //for (Glyph glyph : glyphs.values()) {
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

        // Active/All glyphs
        sb.append(" glyphs=")
          .append(getActiveGlyphs().size())
          .append("/")
          .append(allGlyphs.size());

        if (this.getClass()
                .getName()
                .equals(GlyphLag.class.getName())) {
            sb.append("}");
        }

        return sb.toString();
    }

    //--------//
    // update //
    //--------//
    /**
     * Call-back triggered on selection notification.  We forward glyph
     * information.
     *
     * @param selection the notified Selection
     * @param hint potential notification hint
     */
    @Override
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        // Keep normal lag behavior
        // (interest in sheet location, section and section ID)
        super.update(selection, hint);

        // Additional tasks
        switch (selection.getTag()) {
        // Interest in sheet location => active glyph(s)
        case SHEET_RECTANGLE :

            if ((hint == LOCATION_ADD) || (hint == LOCATION_INIT)) {
                Rectangle rect = (Rectangle) selection.getEntity();

                if (rect != null) {
                    if ((rect.width > 0) || (rect.height > 0)) {
                        // This is a non-degenerated rectangle
                        // Look for enclosed active glyphs
                        if ((glyphSetSelection != null) &&
                            (glyphSetSelection.countObservers() > 0)) {
                            List<Glyph> glyphsFound = lookupGlyphs(
                                getActiveGlyphs(),
                                rect);

                            if (glyphsFound.size() > 0) {
                                glyphSelection.setEntity(
                                    glyphsFound.get(glyphsFound.size() - 1),
                                    hint);
                            } else {
                                glyphSelection.setEntity(null, hint);
                            }

                            glyphSetSelection.setEntity(glyphsFound, hint);
                        }
                    } else {
                        // This is just a point
                        // If a section has just been found,
                        // forward its assigned glyph if any
                        if ((glyphSelection != null) &&
                            (glyphSelection.countObservers() > 0) &&
                            (sectionSelection != null) &&
                            (sectionSelection.countObservers() > 0)) { // TBD GlyphLag itself

                            Glyph        glyph = null;
                            GlyphSection section = (GlyphSection) sectionSelection.getEntity();

                            if (section != null) {
                                glyph = section.getGlyph();
                            }

                            glyphSelection.setEntity(glyph, hint);
                        }
                    }
                }
            }

            break;

        // Interest in Section => assigned glyph
        case HORIZONTAL_SECTION :
        case VERTICAL_SECTION :

            if (hint == SECTION_INIT) {
                // Select related Glyph if any
                GlyphSection section = (GlyphSection) selection.getEntity();

                if (section != null) {
                    glyphSelection.setEntity(section.getGlyph(), hint);
                }
            }

            break;

        // Interest in Glyph => glyph contour
        case HORIZONTAL_GLYPH :
        case VERTICAL_GLYPH : {
            Glyph glyph = (Glyph) selection.getEntity();

            if ((hint == GLYPH_INIT) || (hint == GLYPH_MODIFIED)) {
                // Display glyph contour
                if (glyph != null) {
                    locationSelection.setEntity(glyph.getContourBox(), hint);
                }
            }

            if (selection.getTag() == SelectionTag.VERTICAL_GLYPH) {
                // Update (vertical) glyph set
                updateGlyphSet(glyph, hint);
            }
        }

        break;

        // Interest in Glyph ID => glyph
        case HORIZONTAL_GLYPH_ID :
        case VERTICAL_GLYPH_ID : {
            // Lookup a glyph with proper ID
            if (glyphSelection != null) {
                // Nullify Run & Section entities
                runSelection.setEntity(null, hint);
                sectionSelection.setEntity(null, hint);

                // Report Glyph entity
                Integer id = (Integer) selection.getEntity();
                glyphSelection.setEntity(getGlyph(id), hint);
            }
        }

        break;

        default :
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
     * @param glyph the assigned glyph
     */
    void mapSection (GlyphSection section,
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

    //----------------//
    // updateGlyphSet //
    //----------------//
    private void updateGlyphSet (Glyph         glyph,
                                 SelectionHint hint)
    {
        if ((glyphSetSelection != null) &&
            (glyphSetSelection.countObservers() > 0)) {
            // Get current glyph set
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // Compiler warning

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

                    glyphSetSelection.setEntity(glyphs, hint);
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

                glyphSetSelection.setEntity(glyphs, hint);
            }
        }
    }
}
