//-----------------------------------------------------------------------//
//                                                                       //
//                        G l y p h B u i l d e r                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph;

import omr.constant.ConstantSet;
import omr.glyph.ui.SymbolGlyphBoard;
import omr.score.Measure;
import omr.score.PagePoint;
import omr.score.Staff;
import omr.score.StaffPoint;
import omr.sheet.Dash;
import omr.sheet.PixelPoint;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;
import omr.sheet.SystemSplit;
import omr.stick.Stick;
import omr.util.Logger;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>GlyphBuilder</code> is in charge of building glyphs, it
 * handles the gathering of remaining glyphs out of a sheet, as well as the
 * building of compound glyphs.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphBuilder
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(GlyphBuilder.class);

    //~ Instance variables ------------------------------------------------

    // The containing sheet
    private final Sheet sheet;

    // Lag of vertical runs
    private GlyphLag vLag;

    // First glyph id as built by this builder
    private int firstGlyphId;

    // Pointer to glyph board
    private SymbolGlyphBoard glyphBoard;

    //~ Constructors ------------------------------------------------------

    //--------------//
    // GlyphBuilder //
    //--------------//
    /**
     * Gathers the remaining glyphs
     *
     * @param sheet the sheet to browse for glyphs
     */
    public GlyphBuilder (Sheet sheet)
    {
        this.sheet = sheet;

        // Reuse vertical lag (from bars step).
        vLag = sheet.getVerticalLag();
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // setBoard //
    //----------//
    public void setBoard (SymbolGlyphBoard glyphBoard)
    {
        this.glyphBoard = glyphBoard;
    }

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Retrieve the new glyphs that can be built in all systems of the sheet
     *
     * @return the number of glyphs created
     */
    public Integer buildInfo()
    {
        // Make sure horizontals (such as ledgers) & verticals (such as
        // stems) have been retrieved
        sheet.getHorizontals();
        List<SystemInfo> systems = sheet.getSystems();

        // Split vertical sections per system
        SystemSplit.splitVerticalSections(sheet);

        // Now consider each system area on turn
        firstGlyphId = vLag.getLastGlyphId() +1;
        for (SystemInfo system : systems) {
            retrieveSystemGlyphs(system);
        }

        logger.info((vLag.getLastGlyphId() - firstGlyphId +1) +
                    " glyph(s) found");

        return new Integer(firstGlyphId);
    }

    //-----------------//
    // getFirstGlyphId //
    //-----------------//
    /**
     * Report the id of the first glyph that will be built (so that they
     * can be differentiated from the pre-existing ones)
     *
     * @return the starting value of the id series
     */
    public int getFirstGlyphId()
    {
        return firstGlyphId;
    }

    //----------------------//
    // retrieveSystemGlyphs //
    //----------------------//
    /**
     * In a given system area, browse through all sections not assigned to
     * known glyphs, and build new glyphs out of connected sections
     *
     * @param system the system area to process
     * @return the number of glyphs newly built in the system
     */
    public int retrieveSystemGlyphs (SystemInfo system)
    {
        // Differentiate new glyphs from existing ones
        int newGlyphId = vLag.getLastGlyphId() +1;

        List<Glyph> newGlyphs = new ArrayList<Glyph>();

        // Browse the various unrecognized sections
        for (GlyphSection section : system.getVerticalSections()) {
            // Not already visited ?
            if ((!section.isKnown()) &&
                (section.getGlyph() == null ||
                 section.getGlyph().getId() < newGlyphId)) {
                newGlyphs.add(buildGlyph(system, section));
            }
        }

        // Sort the system glyphs according to their abscissa
        system.sortGlyphs();

        // Compute features for new glyphs
        for (Glyph glyph : newGlyphs) {
            computeGlyphFeatures(system, glyph);
        }

        return system.getGlyphs().size();
    }

    //------------//
    // buildGlyph //
    //------------//
    private Glyph buildGlyph (SystemInfo   system,
                              GlyphSection section)
    {
        // Create a glyph with all connected sections
        Glyph glyph = vLag.createGlyph(Stick.class);
        consider(glyph, section);

        // Add this glyph to list of system glyphs
        system.getGlyphs().add(glyph);

        if (logger.isFineEnabled()) {
            logger.fine("Created " + glyph);
        }

        return glyph;
    }

    //---------------//
    // buildCompound //
    //---------------//
    /**
     * Make a new glyph out of a collection of (sub) glyphs, by merging all
     * their member sections. This compound is temporary, since until it is
     * properly inserted by use of {@link #insertCompound}, this building
     * has no impact on either the containing lag, nor the contained
     * sections.
     *
     * @param list the list of (sub) glyphs
     * @return the brand new (compound) glyph
     */
    public Glyph buildCompound(List<Glyph> list)
    {
        // Build a glyph from all sections
        Glyph compound = new Glyph();
        for (Glyph glyph : list) {
            compound.addGlyphSections(glyph, /* linkSections => */ false);
        }

        // Compute glyph parameters
        SystemInfo system = sheet.getSystemAtY(compound.getContourBox().y);
        computeGlyphFeatures(system, compound);

        return compound;
    }

    //----------------//
    // insertCompound //
    //----------------//
    /**
     * Insert a brand new compound in proper system, with all its
     * sub-glyphs being destroyed
     *
     * @param compound the brand new (compound) glyph
     * @param parts the list of (sub) glyphs
     */
    public void insertCompound(Glyph compound,
                               List<Glyph> parts)
    {
        // Insert in lag, which assigns an id to the compound
        vLag.addGlyph(compound);

        // Get rid of composing glyphs
        for (Glyph glyph : parts) {
            removeGlyph(glyph, false);
        }

        // Make all its sections point to it
        for (GlyphSection section : compound.getMembers()) {
            section.setGlyph(compound);
        }

        // Insert glyph in proper collection at proper location
        SystemInfo system = sheet.getSystemAtY(compound.getContourBox().y);
        int loc = Glyph.getGlyphIndexAtX
            (system.getGlyphs(), compound.getContourBox().x);
        system.getGlyphs().add(loc, compound);

        // Update glyph board accordingly
        if (glyphBoard != null) {
            glyphBoard.addGlyphId(compound);
        }
    }

    //-------------//
    // removeGlyph //
    //-------------//
    /**
     * Remove a glyph from the containing system glyph list, as well as
     * from the various spinners of the glyph board if any
     *
     * @param glyph the glyph to remove
     * @param cutSections true to cut the link between member sections and
     * glyph
     */
    private void removeGlyph(Glyph glyph,
                             boolean cutSections)
    {
        // Remove from system
        int y = glyph.getContourBox().y;
        SystemInfo system = sheet.getSystemAtY(y);
        if (!system.getGlyphs().remove(glyph)) {
            SystemInfo closest = sheet.getClosestSystem(system, y);
            if (closest != null) {
                if (!closest.getGlyphs().remove(glyph)) {
                    logger.warning("Cannot find " + glyph +
                                 " close to " + system +
                                 " closest was " + closest);
                }
            }
        }

        // Remove from lag
        glyph.destroy(cutSections);

        // Remove from glyph board
        if (glyphBoard != null) {
            glyphBoard.removeGlyphId(glyph.getId());
        }
    }

    //----------//
    // consider //
    //----------//
    private static void consider (Glyph        glyph,
                                  GlyphSection section)
    {
        if (section.isKnown() ||
            section.getGlyph() == glyph) {
            return;
        }

        glyph.addSection(section, /* link => */ true);

        // Add recursively all linked sections in the lag
        //
        // Incoming ones
        for (GlyphSection source : section.getSources()) {
            consider(glyph, source);
        }
        //
        // Outgoing ones
        for (GlyphSection target : section.getTargets()) {
            consider(glyph, target);
        }
    }

    //----------------------//
    // computeGlyphFeatures //
    //----------------------//
    /**
     * Compute all the features that will be used to recognize the glyph at
     * hand (it's a mix of moments plus a few other characteristics)
     *
     * @param system the system area which contains the glyph
     * @param glyph the glyph at hand
     */
    private void computeGlyphFeatures (SystemInfo system,
                                              Glyph      glyph)
    {
        // Ordinate (approximate value)
        Rectangle box = glyph.getContourBox();
        int y = box.y;

        // Nearest/containing staff
        StaffInfo staff = system.getStaffAtY(y);

        // Staff interline value
        glyph.setInterline(sheet.getScale().interline());

        // Mass center (which makes sure moments are available)
        PixelPoint centroid = glyph.getCentroid();
        Scale scale = sheet.getScale();
        PagePoint pgCentroid = scale.toPagePoint(centroid);
        Staff s = system.getScoreSystem().getStaffAt(pgCentroid);
        StaffPoint stCentroid = s.toStaffPoint(pgCentroid);

        // Left and right margins within measure
        Measure measure = s.getMeasureAt(s.toStaffPoint(pgCentroid));

        // Number of connected stems
        int stemNb = 0;
        if (checkStemIntersect(system.getGlyphs(),
                               system.getMaxGlyphWidth(),
                               glyph,
                               /* onLeft => */ true)) {
            stemNb++;
        }
        if (checkStemIntersect(system.getGlyphs(),
                               system.getMaxGlyphWidth(),
                               glyph,
                               /* onLeft => */ false)) {
            stemNb++;
        }
        glyph.setStemNumber(stemNb);

        // Has a related ledger ?
        glyph.setHasLedger(checkDashIntersect(system.getLedgers(),
                                              system.getMaxLedgerWidth(),
                                              ledgerBox(box)));

        // Vertical position wrt staff
        glyph.setPitchPosition(staff.pitchPositionOf(centroid));
    }

    //-------------//
    // leftStemBox //
    //-------------//
    private Rectangle leftStemBox (Rectangle rect)
    {
        int dx = sheet.getScale().toPixels(constants.stemWiden);
        return new Rectangle (rect.x - dx,
                              rect.y,
                              2*dx,
                              rect.height);
    }

    //--------------//
    // rightStemBox //
    //--------------//
    private Rectangle rightStemBox (Rectangle rect)
    {
        int dx = sheet.getScale().toPixels(constants.stemWiden);
        return new Rectangle (rect.x + rect.width - dx,
                              rect.y,
                              2*dx,
                              rect.height);
    }

    //-----------//
    // ledgerBox //
    //-----------//
    private Rectangle ledgerBox (Rectangle rect)
    {
        int dy = sheet.getScale().toPixels(constants.ledgerHeighten);
        return new Rectangle (rect.x,
                              rect.y - dy,
                              rect.width,
                              rect.height + 2*dy);
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
        Rectangle   box;
        if (onLeft) {
            box = leftStemBox(glyph.getContourBox());
        } else {
            box = rightStemBox(glyph.getContourBox());
        }

        int startIdx = Glyph.getGlyphIndexAtX
            (glyphs, box.x - maxItemWidth);

        if (startIdx < glyphs.size()) {
            int stopIdx = Glyph.getGlyphIndexAtX
                (glyphs, box.x + box.width +1); // Not sure we need "+1"

            for (Glyph s : glyphs.subList(startIdx, stopIdx)) {
                // Check box intersection
                if (s.isStem() &&
                    s.getContourBox().intersects(box)) {
                    // Check real adjacency
                    for (GlyphSection section : glyph.getMembers()) {
                        if (onLeft) {
                            for (GlyphSection source : section.getSources()) {
                                if (source.getGlyph() == s) {
                                    return true;
                                }

                            }
                        } else {
                            for (GlyphSection target : section.getTargets()) {
                                if (target.getGlyph() == s) {
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

    //--------------------//
    // checkDashIntersect //
    //--------------------//
    private static boolean checkDashIntersect (List<? extends Dash> items,
                                               int            maxItemWidth,
                                               Rectangle      box)
    {
        int startIdx = Dash.getDashIndexAtX
            (items, box.x - maxItemWidth);

        if (startIdx < items.size()) {
            int stopIdx = Dash.getDashIndexAtX
                (items, box.x + box.width +1); // Not sure we need "+1"
            for (Dash item : items.subList(startIdx, stopIdx)) {
                if (item.getContourBox().intersects(box)) {
                    return true;
                }
            }
        }
        return false;
    }

    //~ Classes -----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
        extends ConstantSet
    {
        Scale.Fraction stemWiden = new Scale.Fraction
                (0.1,
                 "Box widening to check intersection with stem");

        Scale.Fraction ledgerHeighten = new Scale.Fraction
                (0.1,
                 "Box heightening to check intersection with ledger");

        Constants ()
        {
            initialize();
        }
    }
}
