//----------------------------------------------------------------------------//
//                                                                            //
//                         G l y p h s B u i l d e r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.common.SystemPoint;
import omr.score.entity.Staff;

import omr.sheet.Dash;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.stick.Stick;

import java.awt.Rectangle;
import java.util.*;

/**
 * Class <code>GlyphsBuilder</code> is, at a system level, in charge of
 * building (and removing) glyphs and of updating accordingly the containing
 * entities (GlyphLag and SystemInfo).
 *
 * <p>Though there are vertical and horizontal glyphs, a GlyphBuilder
 * is meant to handle only vertical glyphs, since it plays only with the sheet
 * vertical lag and with the system vertical sections.
 *
 * <p>It does not handle the shape of a glyph (this higher-level task is handled
 * by {@link GlyphInspector} among others). But it does handle all the physical
 * characteristics of a glyph via {@link #computeGlyphFeatures} (moments, plus
 * additional data such as ledger, stem).
 *
 * <p>It typically handles via {@link #retrieveGlyphs} the building of glyphs
 * out of the remaining sections of a sheet (since this is done using the
 * physical edges between the sections).
 *
 * <p>It provides the provisioning methods to actually insert or remove a glyph.
 * <ul>
 *
 * <li>A given newly built glyph can be inserted via {@link #addGlyph}
 *
 * <li>Similarly {@link #removeGlyph} allows the removal of an existing glyph.
 * <B>Nota:</B> Remember that the sections that compose a glyph are not removed,
 * only the glyph is removed. The link from the contained sections back to the
 * containing glyph is updated or not according to the proper method parameter.
 *
 * </ul>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
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

    /** Lag of vertical runs */
    private final GlyphLag vLag;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // GlyphsBuilder //
    //---------------//
    /**
     * Creates a system-dedicated builder of glyphs
     *
     * @param system the dedicated system
     */
    public GlyphsBuilder (SystemInfo system)
    {
        this.system = system;

        Sheet sheet = system.getSheet();
        scale = sheet.getScale();

        // Reuse vertical lag (from bars step).
        vLag = sheet.getVerticalLag();
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // addGlyph //
    //----------//
    /**
     * Add a brand new glyph as an active glyph in proper system and lag.
     * It does not check if the glyph has an assigned shape.
     * If the glyph is a compound, its parts are made pointing back to it and
     * are made no longer active glyphs.
     *
     * @param glyph the brand new glyph
     * @return the original glyph as inserted in the glyph lag
     */
    public Glyph addGlyph (Glyph glyph)
    {
        // Get rid of composing parts if any
        for (Glyph part : glyph.getParts()) {
            part.setPartOf(glyph);
            part.setShape(Shape.GLYPH_PART);
            removeGlyph(part);
        }

        // Insert in lag, which assigns an id to the glyph
        Glyph oldGlyph = vLag.addGlyph(glyph);

        if (oldGlyph != glyph) {
            // Perhaps some members to carry over (TODO: check this!)
            oldGlyph.copyStemInformation(glyph);
        }

        system.addToGlyphsCollection(oldGlyph);

        return oldGlyph;
    }

    //------------//
    // buildGlyph //
    //------------//
    /**
     * Build a glyph from a collection of sections, with a link back from the
     * sections to the glyph
     * @param sections the provided members of the future glyph
     * @return the newly built glyph
     */
    public Glyph buildGlyph (Collection<GlyphSection> sections)
    {
        Glyph glyph = new Stick(scale.interline());

        for (GlyphSection section : sections) {
            glyph.addSection(section, true);
        }

        return glyph;
    }

    //------------------------//
    // buildTransientCompound //
    //------------------------//
    /**
     * Make a new glyph out of a collection of (sub) glyphs, by merging all
     * their member sections. This compound is transient, since until it is
     * properly inserted by use of {@link #addGlyph}, this building has no
     * impact on either the containing lag, the containing system, nor the
     * contained parts or the contained sections themselves.
     *
     * @param parts the collection of (sub) glyphs
     * @return the brand new (compound) glyph
     */
    public Glyph buildTransientCompound (Collection<Glyph> parts)
    {
        // Build a glyph from all sections
        Glyph compound = new Stick(scale.interline());

        for (Glyph glyph : parts) {
            compound.addGlyphSections(glyph, /* linkSections => */
                                      false);

            if (compound.getLag() == null) {
                compound.setLag(glyph.getLag());
            }
        }

        // Register (a copy of) the parts in the compound itself
        compound.setParts(parts);

        // Compute glyph parameters
        computeGlyphFeatures(compound);

        return compound;
    }

    //---------------------//
    // buildTransientGlyph //
    //---------------------//
    /**
     * Make a new glyph out of a collection of sections.
     * This glyph is transient, since until it is properly inserted by use of
     * {@link #addGlyph}, this building has no impact on either the containing
     * lag, the containing system, nor the contained sections themselves.
     *
     * @param sections the collection of sections
     * @return the brand new transientglyph
     */
    public Glyph buildTransientGlyph (Collection<GlyphSection> sections)
    {
        // Build a glyph from all sections
        Glyph compound = new Stick(scale.interline());

        for (GlyphSection section : sections) {
            compound.addSection(section, /* linkSections => */
                                false);

            if (compound.getLag() == null) {
                compound.setLag(section.getGraph());
            }
        }

        // Compute glyph parameters
        computeGlyphFeatures(compound);

        return compound;
    }

    //----------------------//
    // computeGlyphFeatures //
    //----------------------//
    /**
     * Compute all the features that will be used to recognize the glyph at hand
     * (it's a mix of moments plus a few other characteristics)
     *
     * @param glyph the glyph at hand
     */
    public void computeGlyphFeatures (Glyph glyph)
    {
        // Ordinate (approximate value)
        Rectangle   box = glyph.getContourBox();
        int         y = box.y;

        // Mass center (which makes sure moments are available)
        SystemPoint centroid = system.getScoreSystem()
                                     .toSystemPoint(glyph.getCentroid());
        Staff       staff = system.getScoreSystem()
                                  .getStaffAt(centroid);

        // Connected stems
        glyph.setLeftStem(null);
        glyph.setRightStem(null);

        int stemNb = 0;

        // Look left
        if (checkStemIntersect(system.getGlyphs(), glyph, true)) {
            stemNb++;
        }

        // Look right
        if (checkStemIntersect(system.getGlyphs(), glyph, false)) {
            stemNb++;
        }

        glyph.setStemNumber(stemNb);

        // Has a related ledger ?
        glyph.setWithLedger(
            checkDashIntersect(
                system.getLedgers(),
                system.getMaxLedgerWidth(),
                ledgerBox(box)));

        // Vertical position wrt staff
        glyph.setPitchPosition(staff.pitchPositionOf(centroid));
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
        if (!system.removeFromGlyphsCollection(glyph)) {
            //            logger.warning(
            //                "Glyph #" + glyph.getId() + " not found in system #" +
            //                system.getId());
        }

        // Cut link from its member sections, if pointing to this glyph
        glyph.cutSections();
    }

    //----------------//
    // retrieveGlyphs //
    //----------------//
    /**
     * In a given system area, browse through all sections not assigned to known
     * glyphs, and build new glyphs out of connected sections
     *
     * @return the number of glyphs built in this system
     */
    public int retrieveGlyphs ()
    {
        int               nb = 0;
        Set<GlyphSection> visitedSections = new LinkedHashSet<GlyphSection>();

        // Browse the various unrecognized sections
        for (GlyphSection section : system.getVerticalSections()) {
            // Not already visited ?
            if (!section.isKnown() && !visitedSections.contains(section)) {
                // Let's build a new glyph around this starting section
                Glyph glyph = new Stick(scale.interline());
                considerConnection(glyph, section, visitedSections);

                // And insert this newly built glyph at proper location
                glyph = addGlyph(glyph);
                nb++;
            }
        }

        // Force update for features of ALL system glyphs, since the mere
        // existence of new glyphs may impact the characteristics of others
        // (example of stems nearby)
        for (Glyph glyph : system.getGlyphs()) {
            computeGlyphFeatures(glyph);
        }

        return nb;
    }

    //--------------------//
    // checkDashIntersect //
    //--------------------//
    private boolean checkDashIntersect (List<?extends Dash> items,
                                        int                 maxItemWidth,
                                        Rectangle           box)
    {
        int startIdx = Dash.getDashIndexAtX(items, box.x - maxItemWidth);

        if (startIdx < items.size()) {
            int stopIdx = Dash.getDashIndexAtX(items, box.x + box.width + 1); // Not sure we need "+1"

            for (Dash item : items.subList(startIdx, stopIdx)) {
                if (item.getContourBox()
                        .intersects(box)) {
                    return true;
                }
            }
        }

        return false;
    }

    //--------------------//
    // checkStemIntersect //
    //--------------------//
    private boolean checkStemIntersect (Collection<Glyph> glyphs,
                                        Glyph             glyph,
                                        boolean           onLeft)
    {
        // Box for searching for a stem
        Rectangle box;

        if (onLeft) {
            box = leftStemBox(glyph.getContourBox());
        } else {
            box = rightStemBox(glyph.getContourBox());
        }

        for (Glyph s : glyphs) {
            // Check bounding box intersection
            if (s.isStem() && s.getContourBox()
                               .intersects(box)) {
                // Check adjacency
                for (GlyphSection section : glyph.getMembers()) {
                    if (onLeft) {
                        for (GlyphSection source : section.getSources()) {
                            if (source.getGlyph() == s) {
                                glyph.setLeftStem(s);

                                return true;
                            }
                        }
                    } else {
                        for (GlyphSection target : section.getTargets()) {
                            if (target.getGlyph() == s) {
                                glyph.setRightStem(s);

                                return true;
                            }
                        }
                    }
                }

                // Check close distance
                Rectangle b = stemBoxOf(s);

                for (GlyphSection section : glyph.getMembers()) {
                    if (section.getContour()
                               .intersects(b.x, b.y, b.width, b.height)) {
                        if (onLeft) {
                            glyph.setLeftStem(s);
                        } else {
                            glyph.setRightStem(s);
                        }

                        return true;
                    }
                }
            }
        }

        return false;
    }

    //--------------------//
    // considerConnection //
    //--------------------//
    private void considerConnection (Glyph             glyph,
                                     GlyphSection      section,
                                     Set<GlyphSection> visitedSections)
    {
        // Check whether this section is suitable to expand the glyph
        if (!section.isKnown() && !visitedSections.contains(section)) {
            visitedSections.add(section);

            glyph.addSection(section, /* link => */
                             true);

            // Add recursively all linked sections in the lag
            //
            // Incoming ones
            for (GlyphSection source : section.getSources()) {
                considerConnection(glyph, source, visitedSections);
            }

            //
            // Outgoing ones
            for (GlyphSection target : section.getTargets()) {
                considerConnection(glyph, target, visitedSections);
            }
        }
    }

    //-----------//
    // ledgerBox //
    //-----------//
    private Rectangle ledgerBox (Rectangle rect)
    {
        int dy = scale.toPixels(constants.ledgerHeighten);

        return new Rectangle(
            rect.x,
            rect.y - dy,
            rect.width,
            rect.height + (2 * dy));
    }

    //-------------//
    // leftStemBox //
    //-------------//
    private Rectangle leftStemBox (Rectangle rect)
    {
        int dx = scale.toPixels(constants.stemWiden);
        int dy = scale.toPixels(constants.stemHeighten);

        return new Rectangle(
            rect.x - dx,
            rect.y - dy,
            2 * dx,
            rect.height + (2 * dy));
    }

    //--------------//
    // rightStemBox //
    //--------------//
    private Rectangle rightStemBox (Rectangle rect)
    {
        int dx = scale.toPixels(constants.stemWiden);
        int dy = scale.toPixels(constants.stemHeighten);

        return new Rectangle(
            (rect.x + rect.width) - dx,
            rect.y - dy,
            2 * dx,
            rect.height + (2 * dy));
    }

    //-----------//
    // stemBoxOf //
    //-----------//
    private Rectangle stemBoxOf (Glyph s)
    {
        int       dx = scale.toPixels(constants.stemWiden);
        int       dy = scale.toPixels(constants.stemHeighten);
        Rectangle rect = s.getContourBox();

        return new Rectangle(
            rect.x - dx,
            rect.y - dy,
            rect.width + (2 * dx),
            rect.height + (2 * dy));
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Box heightening to check intersection with ledger */
        Scale.Fraction ledgerHeighten = new Scale.Fraction(
            0.1,
            "Box heightening to check intersection with ledger");

        /** Box widening to check intersection with stem */
        Scale.Fraction stemWiden = new Scale.Fraction(
            0.2,
            "Box widening to check intersection with stem");

        /** Box heightening to check intersection with stem */
        Scale.Fraction stemHeighten = new Scale.Fraction(
            0.2,
            "Box heightening to check intersection with stem");
    }
}
