//----------------------------------------------------------------------------//
//                                                                            //
//                         G l y p h s B u i l d e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.ConstantSet;

import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.score.entity.Staff;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
    private static final Logger logger = LoggerFactory.getLogger(GlyphsBuilder.class);

    //~ Instance fields --------------------------------------------------------
    /** The dedicated system */
    private final SystemInfo system;

    /** The global sheet scale */
    private final Scale scale;

    /** Global hosting nest for glyphs */
    private final Nest nest;

    /** Margins for a stem */
    private final int stemXMargin;

    private final int stemYMargin;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // GlyphsBuilder //
    //---------------//
    /**
     * Creates a system-dedicated builder of glyphs.
     *
     * @param system the dedicated system
     */
    public GlyphsBuilder (SystemInfo system)
    {
        this.system = system;

        Sheet sheet = system.getSheet();
        scale = sheet.getScale();
        nest = sheet.getNest();

        // Cache parameters
        stemXMargin = scale.toPixels(constants.stemXMargin);
        stemYMargin = scale.toPixels(constants.stemYMargin);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // buildGlyph //
    //------------//
    /**
     * Build a glyph from a collection of sections, with a link back
     * from the sections to the glyph.
     *
     * @param scale    the context scale
     * @param sections the provided members of the future glyph
     * @return the newly built glyph
     */
    public static Glyph buildGlyph (Scale scale,
                                    Collection<Section> sections)
    {
        Glyph glyph = new BasicGlyph(scale.getInterline());

        for (Section section : sections) {
            glyph.addSection(section, Glyph.Linking.LINK_BACK);
        }

        return glyph;
    }

    //----------------//
    // retrieveGlyphs //
    //----------------//
    /**
     * Browse through the provided sections not assigned to known
     * glyphs, and build new glyphs out of connected sections.
     *
     * @param sections the sections to browse
     * @param nest     the nest to host glyphs
     * @param scale    the sheet scale
     */
    public static List<Glyph> retrieveGlyphs (List<Section> sections,
                                              Nest nest,
                                              Scale scale)
    {
        List<Glyph> created = new ArrayList<>();

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

    //----------//
    // addGlyph //
    //----------//
    /**
     * Add a brand new glyph as an active glyph in proper system and nest.
     * 'Active' means that all member sections are set to point back to the
     * containing glyph.
     *
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
     *
     * @param sections the provided members of the future glyph
     * @return the newly built glyph
     */
    public Glyph buildGlyph (Collection<Section> sections)
    {
        return buildGlyph(scale, sections);
    }

    //------------------------//
    // buildTransientCompound //
    //------------------------//
    /**
     * Make a new transient glyph out of a collection of (sub) glyphs,
     * by merging all their member sections.
     *
     * @param parts the collection of (sub) glyphs
     * @return the brand new (compound) glyph
     */
    public Glyph buildTransientCompound (Collection<Glyph> parts)
    {
        // Gather all the sections involved
        Collection<Section> sections = new HashSet<>();

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
     *
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
     *
     * @param glyph the glyph at hand
     */
    public void computeGlyphFeatures (Glyph glyph)
    {
        if (glyph.isVip()) {
            logger.debug("computeGlyphFeatures for {}", glyph.idString());
        }
        // Mass center (which makes sure moments are available)
        glyph.getCentroid();

        Point center = glyph.getAreaCenter();
        Staff staff = system.getScoreSystem()
                .getStaffAt(center);

        // Connected stems
        int stemNb = 0;

        for (HorizontalSide side : HorizontalSide.values()) {
            Glyph stem = lookupStem(side, system.getGlyphs(), glyph);
            glyph.setStem(stem, side);

            if (stem != null) {
                stemNb++;
            }
        }

        glyph.setStemNumber(stemNb);

        // Has a related ledger ?
        glyph.setWithLedger(
                checkDashIntersect(
                system.getGlyphs(),
                ledgerBox(glyph.getBounds())));

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
     *
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
     *
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
     *
     * @param compute if true, compute the characteristics of the created glyphs
     */
    public void retrieveGlyphs (boolean compute)
    {
        // Consider all unknown vertical & horizontal sections
        List<Section> allSections = new ArrayList<>();
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

    //-----------//
    // stemBoxOf //
    //-----------//
    /**
     * Report an enlarged box of a given (stem) glyph.
     *
     * @param stem the stem
     * @return the enlarged stem box
     */
    public Rectangle stemBoxOf (Glyph stem)
    {
        Rectangle box = new Rectangle(stem.getBounds());
        box.grow(stemXMargin, stemYMargin);

        return box;
    }

    //-----------//
    // stemBoxOf //
    //-----------//
    /**
     * Report the stem lookup box on the specified side only
     *
     * @param stem the stem glyph
     * @param side the desired side for the box
     * @return the proper stem side box
     */
    public Rectangle stemBoxOf (Glyph stem,
                                HorizontalSide side)
    {
        Rectangle box = stem.getBounds();
        int width = box.width;
        box.grow(stemXMargin, stemYMargin);
        box.width = 2 * stemXMargin;

        if (side == HorizontalSide.RIGHT) {
            box.x += width;
        }

        return box;
    }

    //--------------------//
    // considerConnection //
    //--------------------//
    /**
     * Consider all sections transitively connected to the provided
     * section in order to populate the provided glyph.
     *
     * @param glyph   the provided glyph
     * @param section the section to consider
     */
    private static void considerConnection (Glyph glyph,
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

    //--------------------//
    // checkDashIntersect //
    //--------------------//
    private boolean checkDashIntersect (Iterable<Glyph> items,
                                        Rectangle box)
    {
        for (Glyph item : items) {
            if (item.getShape() == Shape.LEDGER
                && item.getBounds().intersects(box)) {
                return true;
            }
        }

        return false;
    }

    //-----------//
    // ledgerBox //
    //-----------//
    private Rectangle ledgerBox (Rectangle rect)
    {
        Rectangle box = new Rectangle(rect);
        box.grow(0, stemYMargin);

        return box;
    }

    //------------//
    // lookupStem //
    //------------//
    private Glyph lookupStem (HorizontalSide side,
                              Collection<Glyph> glyphs,
                              Glyph glyph)
    {
        if (glyph.isStem()) {
            return null;
        }

        // Box for stem(s) lookup
        final Rectangle box = stemBoxOf(glyph, side);
        final List<Glyph> stems = new ArrayList<>();

        for (Glyph s : glyphs) {
            // Check bounding box intersection
            if (s.isStem() && s.isActive() && s.getBounds().intersects(box)) {
                // Use section intersection for confirmation
                Rectangle b = stemBoxOf(s);

                for (Section section : glyph.getMembers()) {
                    if (section.intersects(b)) {
                        stems.add(s);
                        break;
                    }
                }
            }
        }

        // Pick best stem found, if any
        if (stems.isEmpty()) {
            return null;
        } else {
            if (stems.size() > 1) {
                Collections.sort(stems, new Comparator<Glyph>()
                {
                    @Override
                    public int compare (Glyph g1,
                                        Glyph g2)
                    {
                        // Use ordinate overlap
                        int overlap1 = box.intersection(g1.getBounds()).height;
                        int overlap2 = box.intersection(g2.getBounds()).height;
                        return Integer.compare(overlap1, overlap2);
                    }
                });
            }
            return stems.get(0);
        }
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
        Scale.Fraction stemXMargin = new Scale.Fraction(
                0.1d, //0.05,
                "Box widening to check intersection with stem");

        //
        Scale.Fraction stemYMargin = new Scale.Fraction(
                0.2d, //0.1,
                "Box heightening to check intersection with stem");

    }
}
