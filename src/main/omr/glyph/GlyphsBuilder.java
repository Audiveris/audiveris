//----------------------------------------------------------------------------//
//                                                                            //
//                         G l y p h s B u i l d e r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.constant.ConstantSet;

import omr.score.Staff;
import omr.score.SystemPoint;

import omr.sheet.Dash;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;
import omr.sheet.SystemSplit;

import omr.stick.Stick;

import omr.util.Logger;

import java.awt.Rectangle;
import java.util.*;

/**
 * Class <code>GlyphsBuilder</code> is in charge of building (and removing)
 * glyphs and of updating accordingly the containing entities (GlyphLag and
 * SystemInfo). Though there are vertical and horizontal glyphs, a GlyphBuilder
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
 * <p>It provides the provisioning methods to actually insert or remove a glyph.<ul>
 *
 * <li>A given newly built glyph can be inserted via {@link #insertGlyph}. To
 * insert a compound, which is built by merging several glyphs, a special method
 * {@link #insertCompound} is provided to handle at the same time the removal of
 * the merged glyphs.
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

    /** The containing sheet */
    private final Sheet sheet;

    /** Lag of vertical runs */
    private final GlyphLag vLag;

    /** First glyph id as built by this builder */
    private int firstGlyphId;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // GlyphsBuilder //
    //---------------//
    /**
     * Creates a sheet-dedicated builder of glyphs
     *
     * @param sheet the contextual sheet
     */
    public GlyphsBuilder (Sheet sheet)
    {
        this.sheet = sheet;

        // Reuse vertical lag (from bars step).
        vLag = sheet.getVerticalLag();
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // buildCompound //
    //---------------//
    /**
     * Make a new glyph out of a collection of (sub) glyphs, by merging all
     * their member sections. This compound is temporary, since until it is
     * properly inserted by use of {@link #insertCompound}, this building has no
     * impact on either the containing lag, the containing system, nor the
     * contained sections themselves.
     *
     * @param parts the collection of (sub) glyphs
     * @return the brand new (compound) glyph
     */
    public Glyph buildCompound (List<Glyph> parts)
    {
        // Build a glyph from all sections
        Glyph compound = new Stick();

        for (Glyph glyph : parts) {
            compound.addGlyphSections(glyph, /* linkSections => */
                                      false);
        }

        // Compute glyph parameters
        SystemInfo system = sheet.getSystemAtY(compound.getContourBox().y);
        computeGlyphFeatures(system, compound);

        return compound;
    }

    //------------------------//
    // extractNewSystemGlyphs //
    //------------------------//
    /**
     * In the specified system, build new glyphs from unknown sections (sections
     * not linked to a known glyph)
     *
     * @param system the specified system
     */
    public void extractNewSystemGlyphs (SystemInfo system)
    {
        removeSystemUnknowns(system);
        retrieveSystemGlyphs(system);
    }

    //----------------//
    // insertCompound //
    //----------------//
    /**
     * Insert a brand new compound in proper system, with all its sub-glyphs
     * being destroyed
     *
     * @param compound the brand new (compound) glyph
     * @param parts the list of (sub) glyphs
     */
    public void insertCompound (Glyph       compound,
                                List<Glyph> parts)
    {
        SystemInfo system = sheet.getSystemAtY(compound.getContourBox().y);

        // Get rid of composing glyphs
        for (Glyph glyph : parts) {
            glyph.setShape(Shape.NO_LEGAL_SHAPE);
            removeGlyph(glyph, system, /* cutSections => */
                        false);
        }

        // Insert glyph in proper system at proper location
        insertGlyph(compound, system);
    }

    //-------------//
    // insertGlyph //
    //-------------//
    /**
     * Insert a brand new glyph in proper system and lag. It does not check if
     * the glyph has an assigned shape.
     *
     * @param glyph the brand new glyph
     */
    public void insertGlyph (Glyph      glyph,
                             SystemInfo system)
    {
        // Insert in lag, which assigns an id to the glyph
        vLag.addGlyph(glyph);

        // Make all its sections point to it
        for (GlyphSection section : glyph.getMembers()) {
            section.setGlyph(glyph);
        }

        // Record related scale ?
        if (glyph.getInterline() == 0) {
            glyph.setInterline(system.getScoreSystem().getScale().interline());
        }

        // Insert glyph in proper collection at proper location
        int loc = Glyph.getGlyphIndexAtX(
            system.getGlyphs(),
            glyph.getContourBox().x);
        system.getGlyphs()
              .add(loc, glyph);
    }

    //-------------//
    // removeGlyph //
    //-------------//
    /**
     * Remove a glyph from the containing lag and the containing system glyph
     * list.
     *
     * @param glyph the glyph to remove
     * @param system the (potential) containing system info, or null
     * @param cutSections true to also cut the link between member sections and
     *                    glyph
     */
    public void removeGlyph (Glyph      glyph,
                             SystemInfo system,
                             boolean    cutSections)
    {
        // Remove from system
        int y = glyph.getContourBox().y;

        if (system == null) {
            system = sheet.getSystemAtY(y);
        }

        if (!system.getGlyphs()
                   .remove(glyph)) {
            SystemInfo closest = sheet.getClosestSystem(system, y);

            if (closest != null) {
                if (!closest.getGlyphs()
                            .remove(glyph)) {
                    logger.warning(
                        "Cannot find " + glyph + " close to " + system +
                        " closest was " + closest);
                }
            }
        }

        // Remove from lag
        glyph.destroy(cutSections);
    }

    //----------------------//
    // removeSystemUnknowns //
    //----------------------//
    /**
     * On a specified system, look for all unknown glyphs (including glyphs
     * classified as STRUCTURE shape), and remove them from its glyphs
     * collection as well as from the containing lag.  Purpose is to prepare
     * room for a new glyph extraction
     *
     * @param system the specified system
     */
    public static void removeSystemUnknowns (SystemInfo system)
    {
        List<Glyph> toremove = new ArrayList<Glyph>();

        for (Glyph glyph : system.getGlyphs()) {
            // We remove shapes : null, NOISE, STRUCTURE (not CLUTTER)
            if (!glyph.isWellKnown()) {
                toremove.add(glyph);
            }
        }

        // Remove from system list
        system.getGlyphs()
              .removeAll(toremove);

        // Remove from lag
        for (Glyph glyph : toremove) {
            glyph.destroy( /* cutSections => */
            true);
        }
    }

    //----------------//
    // retrieveGlyphs //
    //----------------//
    /**
     * Retrieve the new glyphs that can be built in all systems of the sheet
     *
     * @return the number of glyphs created
     */
    public Integer retrieveGlyphs ()
    {
        // Make sure horizontals (such as ledgers) & verticals (such as
        // stems) have been retrieved
        sheet.getHorizontals();

        List<SystemInfo> systems = sheet.getSystems();

        // Split vertical sections per system
        SystemSplit.splitVerticalSections(sheet);

        // Now considerConnection each system area on turn
        firstGlyphId = vLag.getLastGlyphId() + 1;

        for (SystemInfo system : systems) {
            retrieveSystemGlyphs(system);
        }

        // Report result
        int nb = vLag.getLastGlyphId() - firstGlyphId + 1;

        if (nb > 0) {
            logger.info(nb + " glyph" + ((nb > 1) ? "s" : "") + " found");
        } else {
            logger.info("No glyph found");
        }

        return new Integer(firstGlyphId);
    }

    //----------------------//
    // retrieveSystemGlyphs //
    //----------------------//
    /**
     * In a given system area, browse through all sections not assigned to known
     * glyphs, and build new glyphs out of connected sections
     *
     * @param system the system area to process
     */
    public void retrieveSystemGlyphs (SystemInfo system)
    {
        // Differentiate new glyphs from existing ones
        int         newGlyphId = vLag.getLastGlyphId() + 1;
        List<Glyph> newGlyphs = new ArrayList<Glyph>();

        // Browse the various unrecognized sections
        for (GlyphSection section : system.getVerticalSections()) {
            // Not already visited ?
            if ((!section.isKnown()) &&
                ((section.getGlyph() == null) ||
                (section.getGlyph()
                        .getId() < newGlyphId))) {
                // Let's build a new glyph around this starting section
                Glyph glyph = new Stick();
                considerConnection(glyph, section);

                // Compute all its characteristics
                computeGlyphFeatures(system, glyph);

                // And insert this newly built glyph at proper location
                insertGlyph(glyph, system);
            }
        }
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
    private boolean checkStemIntersect (List<Glyph> glyphs,
                                        int         maxItemWidth,
                                        Glyph       glyph,
                                        boolean     onLeft)
    {
        // Box for searching for a stem
        Rectangle box;

        if (onLeft) {
            box = leftStemBox(glyph.getContourBox());
        } else {
            box = rightStemBox(glyph.getContourBox());
        }

        int startIdx = Glyph.getGlyphIndexAtX(glyphs, box.x - maxItemWidth);

        if (startIdx < glyphs.size()) {
            int stopIdx = Glyph.getGlyphIndexAtX(glyphs, box.x + box.width + 1); // Not sure we need "+1"

            for (Glyph s : glyphs.subList(startIdx, stopIdx)) {
                // Check box intersection
                if (s.isStem() && s.getContourBox()
                                   .intersects(box)) {
                    // Check real adjacency
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
                }
            }
        }

        return false;
    }

    //----------------------//
    // computeGlyphFeatures //
    //----------------------//
    /**
     * Compute all the features that will be used to recognize the glyph at hand
     * (it's a mix of moments plus a few other characteristics)
     *
     * @param system the system area which contains the glyph
     * @param glyph the glyph at hand
     */
    private void computeGlyphFeatures (SystemInfo system,
                                       Glyph      glyph)
    {
        // Ordinate (approximate value)
        Rectangle box = glyph.getContourBox();
        int       y = box.y;

        // Interline value ?
        if (glyph.getInterline() == 0) {
            glyph.setInterline(sheet.getScale().interline());
        }

        // Mass center (which makes sure moments are available)
        SystemPoint centroid = system.getScoreSystem()
                                     .toSystemPoint(glyph.getCentroid());
        Staff       staff = system.getScoreSystem()
                                  .getStaffAt(centroid);

        // Number of connected stems
        int stemNb = 0;

        if (checkStemIntersect(
            system.getGlyphs(),
            system.getMaxGlyphWidth(),
            glyph,
            /* onLeft => */ true)) {
            stemNb++;
        }

        if (checkStemIntersect(
            system.getGlyphs(),
            system.getMaxGlyphWidth(),
            glyph,
            /* onLeft => */ false)) {
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

    //--------------------//
    // considerConnection //
    //--------------------//
    private void considerConnection (Glyph        glyph,
                                     GlyphSection section)
    {
        // Check whether this section is suitable to expand the glyph
        if (section.isKnown() || (section.getGlyph() == glyph)) {
            return;
        }

        glyph.addSection(section, /* link => */
                         true);

        // Add recursively all linked sections in the lag
        //
        // Incoming ones
        for (GlyphSection source : section.getSources()) {
            considerConnection(glyph, source);
        }

        //
        // Outgoing ones
        for (GlyphSection target : section.getTargets()) {
            considerConnection(glyph, target);
        }
    }

    //-----------//
    // ledgerBox //
    //-----------//
    private Rectangle ledgerBox (Rectangle rect)
    {
        int dy = sheet.getScale()
                      .toPixels(constants.ledgerHeighten);

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
        int dx = sheet.getScale()
                      .toPixels(constants.stemWiden);

        return new Rectangle(rect.x - dx, rect.y, 2 * dx, rect.height);
    }

    //--------------//
    // rightStemBox //
    //--------------//
    private Rectangle rightStemBox (Rectangle rect)
    {
        int dx = sheet.getScale()
                      .toPixels(constants.stemWiden);

        return new Rectangle(
            (rect.x + rect.width) - dx,
            rect.y,
            2 * dx,
            rect.height);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /** Box heightening to check intersection with ledger */
        Scale.Fraction ledgerHeighten = new Scale.Fraction(
            0.1,
            "Box heightening to check intersection with ledger");

        /** Box widening to check intersection with stem */
        Scale.Fraction stemWiden = new Scale.Fraction(
            0.1,
            "Box widening to check intersection with stem");
    }
}
