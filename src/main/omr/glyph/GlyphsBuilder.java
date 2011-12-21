//----------------------------------------------------------------------------//
//                                                                            //
//                         G l y p h s B u i l d e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.ConstantSet;

import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.Staff;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Class {@code GlyphsBuilder} is, at a system level, in charge of
 * building (and removing) glyphs and of updating accordingly the
 * containing entities (Nest and SystemInfo).
 *
 * <p>It does not handle the shape of a glyph (this higher-level task is
 * handled by {@link GlyphInspector} among others).
 * But it does handle all the physical characteristics of a glyph via {@link
 * #computeGlyphFeatures} (moments, plus additional data such as ledger, stem).
 *
 * <p>It typically handles via {@link #retrieveGlyphs} the building of glyphs
 * out of the remaining sections of a sheet (since this is done using the
 * physical edges between the sections).
 *
 * <p>It provides provisioning methods to actually insert or remove a glyph:
 * <ul>
 *
 * <li>A given newly built glyph can be inserted via {@link #addGlyph}</li>
 *
 * <li>Similarly {@link #removeGlyph} allows the removal of an existing glyph.
 * <B>Nota:</B> Remember that the sections that compose a glyph are not removed,
 * only the glyph is removed. The link from the contained sections back to the
 * containing glyph is set to null.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class GlyphsBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphsBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** The dedicated system */
    private final SystemInfo system;

    /** The global sheet scale */
    private final Scale scale;

    /** Global hosting nest for glyphs */
    private final Nest nest;

    /** Margins for a stem */
    private final int stemWiden;
    private final int stemHeighten;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // GlyphsBuilder //
    //---------------//
    /**
     * Creates a system-dedicated builder of glyphs.
     * @param system the dedicated system
     */
    public GlyphsBuilder (SystemInfo system)
    {
        this.system = system;

        Sheet sheet = system.getSheet();
        scale = sheet.getScale();
        nest = sheet.getNest();

        // Cache parameters
        stemWiden = scale.toPixels(constants.stemWiden);
        stemHeighten = scale.toPixels(constants.stemHeighten);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // addGlyph //
    //----------//
    /**
     * Add a brand new glyph as an active glyph in proper system and nest.
     * 'Active' means that all member sections are set to point back to the
     * containing glyph.
     * @param glyph the brand new glyph
     * @return the original glyph as inserted in the glyph nest
     */
    public Glyph addGlyph (Glyph glyph)
    {
        glyph = nest.addGlyph(glyph);

        system.addToGlyphsCollection(glyph);

        return glyph;
    }

    //------------//
    // buildGlyph //
    //------------//
    /**
     * Build a glyph from a collection of sections, with a link back
     * from the sections to the glyph, using the system scale.
     * @param sections the provided members of the future glyph
     * @return the newly built glyph
     */
    public Glyph buildGlyph (Collection<Section> sections)
    {
        return buildGlyph(scale, sections);
    }

    //------------//
    // buildGlyph //
    //------------//
    /**
     * Build a glyph from a collection of sections, with a link back
     * from the sections to the glyph.
     * @param scale the context scale
     * @param sections the provided members of the future glyph
     * @return the newly built glyph
     */
    public static Glyph buildGlyph (Scale               scale,
                                    Collection<Section> sections)
    {
        Glyph glyph = new BasicGlyph(scale.getInterline());

        for (Section section : sections) {
            glyph.addSection(section, Glyph.Linking.LINK_BACK);
        }

        return glyph;
    }

    //------------------------//
    // buildTransientCompound //
    //------------------------//
    /**
     * Make a new transient glyph out of a collection of (sub) glyphs,
     * by merging all their member sections.
     * @param parts the collection of (sub) glyphs
     * @return the brand new (compound) glyph
     */
    public Glyph buildTransientCompound (Collection<Glyph> parts)
    {
        // Gather all the sections involved
        Collection<Section> sections = new HashSet<Section>();

        for (Glyph part : parts) {
            sections.addAll(part.getMembers());
        }

        return buildTransientGlyph(sections);
    }

    //---------------------//
    // buildTransientGlyph //
    //---------------------//
    /**
     * Make a new transient glyph out of a collection of sections.
     * @param sections the collection of sections
     * @return the brand new transientglyph
     */
    public Glyph buildTransientGlyph (Collection<Section> sections)
    {
        // Build a glyph from all sections
        Glyph compound = new BasicGlyph(scale.getInterline());

        for (Section section : sections) {
            compound.addSection(section, Glyph.Linking.NO_LINK_BACK);
        }

        // Make sure we get access to original forbidden shapes if any
        Glyph original = nest.getOriginal(compound);

        if (original != null) {
            compound = original;
        }

        // Compute glyph parameters
        computeGlyphFeatures(compound);

        return compound;
    }

    //----------------------//
    // computeGlyphFeatures //
    //----------------------//
    /**
     * Compute all the features that will be used to recognize the
     * glyph at hand.
     * (it's a mix of moments plus a few other characteristics).
     * @param glyph the glyph at hand
     */
    public void computeGlyphFeatures (Glyph glyph)
    {
        // Mass center (which makes sure moments are available)
        glyph.getCentroid();

        PixelPoint center = glyph.getAreaCenter();
        Staff      staff = system.getScoreSystem()
                                 .getStaffAt(center);

        // Connected stems
        int stemNb = 0;

        for (HorizontalSide side : HorizontalSide.values()) {
            Glyph stem = lookupStem(side, system.getGlyphs(), glyph);

            if (stem != null) {
                glyph.setStem(stem, side);
                stemNb++;
            }
        }

        glyph.setStemNumber(stemNb);

        // Has a related ledger ?
        glyph.setWithLedger(
            checkDashIntersect(
                system.getLedgers(),
                ledgerBox(glyph.getContourBox())));

        // Vertical position wrt staff
        glyph.setPitchPosition(staff.pitchPositionOf(center));
    }

    //---------------//
    // registerGlyph //
    //---------------//
    /**
     * Just register this glyph (as inactive) in order to persist glyph
     * info such as TextInfo.
     * Use {@link #addGlyph} instead to fully add the glyph as active.
     * @param glyph the glyph to just register
     * @return the proper (original) glyph
     * @see #addGlyph
     */
    public Glyph registerGlyph (Glyph glyph)
    {
        // Insert in nest, which assigns an id to the glyph
        Glyph oldGlyph = nest.registerGlyph(glyph);

        system.addToGlyphsCollection(oldGlyph);

        return oldGlyph;
    }

    //-------------//
    // removeGlyph //
    //-------------//
    /**
     * Remove a glyph from the containing system glyph list.
     * @param glyph the glyph to remove
     */
    public void removeGlyph (Glyph glyph)
    {
        system.removeFromGlyphsCollection(glyph);

        // Cut link from its member sections, if pointing to this glyph
        glyph.cutSections();
    }

    //----------------//
    // retrieveGlyphs //
    //----------------//
    /**
     * In a given system area, browse through all sections not assigned
     * to known glyphs, and build new glyphs out of connected sections.
     * @param compute if true, compute the characteristics of the created glyphs
     */
    public void retrieveGlyphs (boolean compute)
    {
        // Consider all unknown vertical & horizontal sections
        List<Section> allSections = new ArrayList<Section>();
        allSections.addAll(system.getVerticalSections());
        allSections.addAll(system.getHorizontalSections());

        List<Glyph> glyphs = retrieveGlyphs(allSections, nest, scale);

        // Record them into the system
        for (Glyph glyph : glyphs) {
            system.addToGlyphsCollection(glyph);

            // Make sure all aggregated sections belong to the same system
            SystemInfo alienSystem = glyph.getAlienSystem(system);

            if (alienSystem != null) {
                removeGlyph(glyph);

                // Publish the error on north side only of the boundary
                SystemInfo north = (system.getId() < alienSystem.getId())
                                   ? system : alienSystem;
                north.getScoreSystem()
                     .addError(glyph, "Glyph crosses system south boundary");
            }
        }

        if (compute) {
            // Force update for features of ALL system glyphs, since the mere
            // existence of new glyphs may impact the characteristics of others
            // (example of stems nearby)
            for (Glyph glyph : system.getGlyphs()) {
                computeGlyphFeatures(glyph);
            }
        }
    }

    //----------------//
    // retrieveGlyphs //
    //----------------//
    /**
     * Browse through the provided sections not assigned to known
     * glyphs, and build new glyphs out of connected sections.
     * @param sections the sections to browse
     * @param nest the nest to host glyphs
     * @param scale the sheet scale
     */
    public static List<Glyph> retrieveGlyphs (List<Section> sections,
                                              Nest          nest,
                                              Scale         scale)
    {
        List<Glyph> created = new ArrayList<Glyph>();

        // Reset section processed flag
        for (Section section : sections) {
            if (!section.isKnown()) {
                section.setProcessed(false);
            } else {
                section.setProcessed(true);
            }
        }

        // Browse the various unrecognized sections
        for (Section section : sections) {
            // Not already visited ?
            if (!section.isProcessed()) {
                // Let's build a new glyph around this starting section
                Glyph glyph = new BasicGlyph(scale.getInterline());
                considerConnection(glyph, section);

                // Insert this newly built glyph into nest (no system invloved)
                glyph = nest.addGlyph(glyph);
                created.add(glyph);
            }
        }

        return created;
    }

    //-----------//
    // stemBoxOf //
    //-----------//
    /**
     * Report a enlarged box of a given (stem) glyph.
     * @param stem the stem
     * @return the enlarged stem box
     */
    public PixelRectangle stemBoxOf (Glyph stem)
    {
        PixelRectangle box = new PixelRectangle(stem.getContourBox());
        box.grow(stemWiden, stemHeighten);

        return box;
    }

    //--------------------//
    // checkDashIntersect //
    //--------------------//
    private boolean checkDashIntersect (Iterable<Glyph> items,
                                        PixelRectangle  box)
    {
        for (Glyph item : items) {
            if (item.getContourBox()
                    .intersects(box)) {
                return true;
            }
        }

        return false;
    }

    //--------------------//
    // considerConnection //
    //--------------------//
    /**
     * Consider all sections transitively connected to the provided
     * section in order to populate the provided glyph.
     * @param glyph the provided glyph
     * @param section the section to consider
     */
    private static void considerConnection (Glyph   glyph,
                                            Section section)
    {
        // Check whether this section is suitable to expand the glyph
        if (!section.isProcessed()) {
            section.setProcessed(true);

            glyph.addSection(section, Glyph.Linking.NO_LINK_BACK);

            // Add recursively all linked sections in the lag

            // Incoming ones
            for (Section source : section.getSources()) {
                considerConnection(glyph, source);
            }

            // Outgoing ones
            for (Section target : section.getTargets()) {
                considerConnection(glyph, target);
            }

            // Sections from other orientation
            for (Section other : section.getOppositeSections()) {
                considerConnection(glyph, other);
            }
        }
    }

    //-----------//
    // ledgerBox //
    //-----------//
    private PixelRectangle ledgerBox (PixelRectangle rect)
    {
        PixelRectangle box = new PixelRectangle(rect);
        box.grow(0, stemHeighten);

        return box;
    }

    //------------//
    // lookupStem //
    //------------//
    private Glyph lookupStem (HorizontalSide    side,
                              Collection<Glyph> glyphs,
                              Glyph             glyph)
    {
        if (glyph.isStem()) {
            return null;
        }

        // Box for searching for a stem
        PixelRectangle box = stemLUBox(side, glyph);

        for (Glyph s : glyphs) {
            // Check bounding box intersection
            if (s.isStem() && s.getContourBox()
                               .intersects(box)) {
                // Check close distance
                PixelRectangle b = stemBoxOf(s);

                for (Section section : glyph.getMembers()) {
                    if (section.getContourBox()
                               .intersects(b)) {
                        return s;
                    }
                }
            }
        }

        return null;
    }

    //-----------//
    // stemLUBox //
    //-----------//
    /**
     * Report the stem lookup box on the provided side of a rectangle
     * @param rect the given (glyph) rectangle
     * @return the proper stem box
     */
    private PixelRectangle stemLUBox (HorizontalSide side,
                                      Glyph          glyph)
    {
        PixelRectangle box = glyph.getContourBox();
        int            width = box.width;
        box.grow(stemWiden, stemHeighten);
        box.width = 2 * stemWiden;

        if (side == HorizontalSide.RIGHT) {
            box.x += width;
        }

        return box;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction ledgerHeighten = new Scale.Fraction(
            0.1,
            "Box heightening to check intersection with ledger");

        //
        Scale.Fraction stemWiden = new Scale.Fraction(
            0.1,
            "Box widening to check intersection with stem");

        //
        Scale.Fraction stemHeighten = new Scale.Fraction(
            0.2,
            "Box heightening to check intersection with stem");
    }
}
