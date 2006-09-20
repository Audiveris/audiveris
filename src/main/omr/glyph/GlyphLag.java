//----------------------------------------------------------------------------//
//                                                                            //
//                              G l y p h L a g                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.lag.Lag;
import omr.lag.Oriented;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import static omr.selection.SelectionHint.*;
import omr.selection.SelectionTag;

import omr.util.Logger;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

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

    private static final Logger               logger = Logger.getLogger(
        GlyphSection.class);

    //~ Instance fields --------------------------------------------------------

    /** All glyphs of this GlyphLag, indexed by glyph id */
    protected final SortedMap<Integer, Glyph> glyphs = new TreeMap<Integer, Glyph>();

    /** Selection on glyph, output where found glyph is written */
    protected Selection glyphSelection;

    /** Selection on glyphs, output where found glyphs are written */
    protected Selection glyphSetSelection;

    /** Global id to uniquely identify a glyph */
    protected int globalGlyphId = 0;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // GlyphLag //
    //----------//
    /**
     * Create a glyph lag, with a pre-defined orientation
     *
     * @param orientation the desired orientation of the lag
     */
    public GlyphLag (Oriented orientation)
    {
        super(orientation);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getFirstGlyphId //
    //-----------------//
    /**
     * Report the first glyph id in this lag
     *
     * @return the first glyph id in the glyph lag
     */
    public int getFirstGlyphId ()
    {
        return glyphs.firstKey();
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
        return glyphs.get(id);
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

    //-----------//
    // getGlyphs //
    //-----------//
    /**
     * Export the glyphs of the lag. TBD: should be unmutable ?
     *
     * @return the collection of glyphs
     */
    public Collection<Glyph> getGlyphs ()
    {
        return glyphs.values();
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

    //-------------//
    // createGlyph //
    //-------------//
    /**
     * Create a new glyph instance in the lag
     *
     * @param cl the concrete glyph class to use for instantiation
     * @return the newly created glyph
     */
    public Glyph createGlyph (Class<?extends Glyph> cl)
    {
        try {
            Glyph glyph = cl.newInstance();
            addGlyph(glyph);

            return glyph;
        } catch (InstantiationException ex) {
            ex.printStackTrace();

            return null;
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();

            return null;
        }
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
        // Normal dump of sections
        super.dump(title);

        // Dump of glyphs
        for (Glyph glyph : getGlyphs()) {
            System.out.println(glyph.toString());
        }
    }

    //--------------//
    // lookupGlyphs //
    //--------------//
    /**
     * Look up for a collection of glyphs, knowing the coordinates rectangle
     *
     * @param rect the coordinates rectangle
     *
     * @return the collection of glyphs, which may be empty
     */
    public List<Glyph> lookupGlyphs (Rectangle rect)
    {
        List<Glyph> list = new ArrayList<Glyph>();

        for (Glyph glyph : glyphs.values()) {
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

        sb.append(" glyphs=")
          .append(glyphs.size());

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
        super.update(selection, hint);

        // Additional tasks
        switch (selection.getTag()) {
        case PIXEL :

            if ((hint == LOCATION_ADD) || (hint == LOCATION_INIT)) {
                Rectangle rect = (Rectangle) selection.getEntity();

                if (rect != null) {
                    if ((rect.width > 0) || (rect.height > 0)) {
                        // Look for enclosed glyphs
                        if ((glyphSetSelection != null) &&
                            (glyphSetSelection.countObservers() > 0)) {
                            List<Glyph> glyphs = lookupGlyphs(rect);

                            if (glyphs.size() > 0) {
                                glyphSelection.setEntity(
                                    glyphs.get(glyphs.size() - 1),
                                    hint);
                            } else {
                                glyphSelection.setEntity(null, hint);
                            }

                            glyphSetSelection.setEntity(glyphs, hint);
                        }
                    } else {
                        // If a section has just been found,
                        // forward its related glyph if any
                        if ((glyphSelection != null) &&
                            (glyphSelection.countObservers() > 0) &&
                            (sectionSelection != null) &&
                            (sectionSelection.countObservers() > 1)) { // GlyphLag itself

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

    //----------//
    // addGlyph //
    //----------//
    /**
     * (package access from {@link Glyph}) to add a glyph in the graph
     *
     * @param glyph the newly created glyph
     */
    void addGlyph (Glyph glyph)
    {
        glyphs.put(++globalGlyphId, glyph);
        glyph.setId(globalGlyphId);
        glyph.setLag(this);
    }

    //-------------//
    // removeGlyph //
    //-------------//
    /**
     * (package access from {@link Glyph}) to remove a glyph from the graph
     *
     * @param glyph the glyph to remove
     */
    void removeGlyph (Glyph glyph)
    {
        glyphs.remove(glyph.getId());
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
