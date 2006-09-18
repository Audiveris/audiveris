//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h I n s p e c t o r                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.ProcessingException;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.Score;
import omr.score.ScoreManager;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;
import omr.sheet.VerticalsBuilder;

import omr.stick.Stick;

import omr.util.Dumper;
import omr.util.Logger;
import omr.util.Predicate;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class <code>GlyphInspector</code> is dedicated to processing of retrieved
 * glyphs, their recognition based on features as used by a neural network
 * evaluator.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphInspector
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger    logger = Logger.getLogger(
        GlyphInspector.class);

    //~ Instance fields --------------------------------------------------------

    private final GlyphBuilder     builder;
    private final GlyphLag         vLag;

    // Predicate to filter only reliable symbols attached to a stem
    private final Predicate<Glyph> reliableStemSymbols = new Predicate<Glyph>() {
        public boolean check (Glyph glyph)
        {
            Shape   shape = glyph.getShape();

            boolean res = glyph.isWellKnown() &&
                          Shape.stemSymbols.contains(shape) &&
                          (shape != Shape.BEAM_CHUNK);

            return res;
        }
    };

    private final Sheet sheet;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // GlyphInspector //
    //----------------//
    /**
     * Create an GlyphInspector instance.
     *
     * @param sheet the sheet to inspect
     * @param builder the glyph builder
     */
    public GlyphInspector (Sheet        sheet,
                           GlyphBuilder builder)
    {
        this.sheet = sheet;
        this.builder = builder;

        vLag = sheet.getVerticalLag();
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // getCleanupMaxGrade //
    //--------------------//
    /**
     * Report the maximum grade for a cleanup
     *
     * @return maximum acceptable grade value
     */
    public static double getCleanupMaxGrade ()
    {
        return constants.cleanupMaxGrade.getValue();
    }

    //-----------------//
    // getLeafMaxGrade //
    //-----------------//
    /**
     * Report the maximum grade for a leaf
     *
     * @return maximum acceptable grade value
     */
    public static double getLeafMaxGrade ()
    {
        return constants.leafMaxGrade.getValue();
    }

    //-------------------//
    // getSymbolMaxGrade //
    //-------------------//
    /**
     * Report the maximum grade for a symbol
     *
     * @return maximum acceptable grade value
     */
    public static double getSymbolMaxGrade ()
    {
        return constants.symbolMaxGrade.getValue();
    }

    //----------------//
    // evaluateGlyphs //
    //----------------//
    /**
     * All symbol glyphs of the sheet, for which we can get a positive vote of
     * the evaluator, are assigned the voted shape.
     *
     * @param maxGrade maximum value for acceptable grade
     */
    public void evaluateGlyphs (double maxGrade)
    {
        int       acceptNb = 0;
        int       knownNb = 0;
        int       noiseNb = 0;
        int       clutterNb = 0;
        int       structureNb = 0;
        Evaluator evaluator = GlyphNetwork.getInstance();

        for (int id = sheet.getFirstSymbolId(); id <= vLag.getLastGlyphId();
             id++) {
            Glyph glyph = vLag.getGlyph(id);

            if (glyph != null) {
                if (glyph.getShape() == null) {
                    glyph.setInterline(sheet.getScale().interline());

                    // Get vote
                    Shape vote = evaluator.vote(glyph, maxGrade);

                    if (vote != null) {
                        glyph.setShape(vote);
                        acceptNb++;

                        if (vote == Shape.NOISE) {
                            noiseNb++;
                        } else if (vote == Shape.CLUTTER) {
                            clutterNb++;
                        } else if (vote == Shape.STRUCTURE) {
                            structureNb++;
                        } else {
                            knownNb++;
                        }
                    }
                }
            }
        }

        logger.info(
            acceptNb + " glyph(s) accepted (" + noiseNb + " as noise, " +
            structureNb + " as structure, " + clutterNb + " as clutter, " +
            knownNb + " as known)");
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
        sheet.getGlyphBuilder()
             .retrieveSystemGlyphs(system);
        system.sortGlyphs();
    }

    //------------------//
    // processCompounds //
    //------------------//
    /**
     * Look for glyphs portions that should be considered as parts of compound
     * glyphs
     *
     * @param maxGrade mamximum acceptance grade
     * @return the number of successful compounds
     */
    public int processCompounds (double maxGrade)
    {
        int compoundNb = 0;

        for (SystemInfo system : sheet.getSystems()) {
            compoundNb += processSystemCompounds(system, maxGrade);
        }

        logger.info(compoundNb + " compound(s) recognized");

        return compoundNb;
    }

    //---------------//
    // processLeaves //
    //---------------//
    /**
     * Retrieve leaves that appear thanks to segmentation due to stems
     * extraction.
     */
    public void processLeaves ()
    {
        // Nota: Leaves are already added to the proper system glyph
        // collection
        builder.buildInfo();

        // Sort glyphs on their abscissa
        for (SystemInfo system : sheet.getSystems()) {
            system.sortGlyphs();
        }
    }

    //------------------------//
    // processSystemCompounds //
    //------------------------//
    /**
     * In the specified system, look for glyphs portions that should be
     * considered as parts of compound glyphs
     *
     * @param system the system where splitted glyphs are looked for
     * @param maxGrade mamximum acceptance grade
     * @return the number of successful compounds
     */
    public int processSystemCompounds (SystemInfo system,
                                       double     maxGrade)
    {
        int         nb = 0;

        // Sort unknown glyphs by decreasing weight
        List<Glyph> glyphs = new ArrayList<Glyph>(system.getGlyphs().size());

        for (Glyph glyph : system.getGlyphs()) {
            if (!glyph.isKnown()) {
                glyphs.add(glyph);
            }
        }

        Collections.sort(
            glyphs,
            new Comparator<Glyph>() {
                    public int compare (Glyph o1,
                                        Glyph o2)
                    {
                        return o2.getWeight() - o1.getWeight();
                    }
                });

        // Process each glyph in turn, by looking at smaller ones
        int       index = -1;
        Evaluator evaluator = GlyphNetwork.getInstance();

        for (Glyph glyph : glyphs) {
            index++;

            // Since the glyphs are modified on the fly ...
            if (glyph.isKnown()) {
                continue;
            }

            // Use an extended contour box
            int       dxy = sheet.getScale()
                                 .toPixels(constants.boxWiden);
            Rectangle box = compoundBox(glyph.getContourBox(), dxy);

            // Consider neighboring glyphs, which are glyphs whose contour
            // intersect the extended contour of glyph at hand
            SUB_GLYPHS: 
            for (Glyph g : glyphs.subList(index + 1, glyphs.size())) {
                if (g.isKnown()) {
                    continue;
                }

                if (box.intersects(g.getContourBox())) {
                    // Let's try a compound
                    List<Glyph> parts = Arrays.asList(glyph, g);
                    Glyph       compound = builder.buildCompound(parts);

                    if (logger.isFineEnabled()) {
                        logger.fine(glyph + " & " + g + " -> " + compound);
                    }

                    Shape vote = evaluator.vote(compound, maxGrade);

                    if ((vote != null) && vote.isWellKnown()) {
                        compound.setShape(vote);
                        builder.insertCompound(compound, parts);
                        nb++;

                        if (logger.isFineEnabled()) {
                            logger.fine("Compound " + compound);
                        }

                        break SUB_GLYPHS;
                    }
                }
            }
        }

        return nb;
    }

    //-------------------------//
    // processSystemUndueStems //
    //-------------------------//
    /**
     * In a specified system, look for all stems that should not be kept,
     * rebuild surrounding glyphs and try to recognize them. If this action does
     * not lead to some recognized symbol, then we restore the stems.
     *
     * @param system the specified system
     * @return the number of symbols recognized
     */
    public int processSystemUndueStems (SystemInfo system)
    {
        logger.fine("processSystemUndueStems " + system);

        int         nb = 0;

        // Collect all undue stems
        List<Glyph> SuspectedStems = new ArrayList<Glyph>();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isStem()) {
                Set<Glyph> goods = new HashSet<Glyph>();
                Set<Glyph> bads = new HashSet<Glyph>();
                glyph.getSymbolsBefore(reliableStemSymbols, goods, bads);
                glyph.getSymbolsAfter(reliableStemSymbols, goods, bads);

                if (goods.size() == 0) {
                    if (logger.isFineEnabled()) {
                        logger.fine("Suspected Stem " + glyph);
                    }

                    SuspectedStems.add(glyph);

                    // Discard "bad" ones
                    for (Glyph g : bads) {
                        g.setShape(null);
                    }
                }
            }
        }

        // Remove these stems since nearby stems are used for recognition
        for (Glyph glyph : SuspectedStems) {
            removeGlyph(glyph, system, /*cutSections=>*/
                        true);
        }

        // Extract brand new glyphs
        extractNewSystemGlyphs(system);

        // Try to recognize each glyph in turn
        List<Glyph>     symbols = new ArrayList<Glyph>();
        final Evaluator evaluator = GlyphNetwork.getInstance();
        final double    maxGrade = getCleanupMaxGrade();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == null) {
                Shape vote = evaluator.vote(glyph, maxGrade);

                if (vote != null) {
                    glyph.setShape(vote);

                    if (glyph.isWellKnown()) {
                        if (logger.isFineEnabled()) {
                            logger.fine("New symbol " + glyph);
                        }

                        symbols.add(glyph);
                        nb++;
                    }
                }
            }
        }

        // Keep stems that have not been replaced by symbols, definitively
        // remove the others
        for (Glyph stem : SuspectedStems) {
            // Check if one of its section is now part of a symbol
            boolean known = false;
            Glyph   glyph = null;

            for (GlyphSection section : stem.getMembers()) {
                glyph = section.getGlyph();

                if ((glyph != null) && glyph.isWellKnown()) {
                    known = true;

                    break;
                }
            }

            if (!known) {
                // Remove the newly created glyph
                if (glyph != null) {
                    removeGlyph(glyph, system, /* cutSections => */
                                true);
                }

                // Restore the stem
                system.getGlyphs()
                      .add(stem);

                // Restore the stem <- section link
                for (GlyphSection section : stem.getMembers()) {
                    section.setGlyph(stem);
                }
            }
        }

        // Extract brand new glyphs
        extractNewSystemGlyphs(system);

        return nb;
    }

    //-------------------//
    // processUndueStems //
    //-------------------//
    /**
     * Look for all stems that should not be kept, rebuild surrounding glyphs
     * and try to recognize them
     *
     * @return the number of symbols recognized
     */
    public int processUndueStems ()
    {
        int symbolNb = 0;

        for (SystemInfo system : sheet.getSystems()) {
            symbolNb += processSystemUndueStems(system);
        }

        logger.info(symbolNb + " symbol(s) from stem cancellation");

        return symbolNb;
    }

    //------------------//
    // processVerticals //
    //------------------//
    /**
     * Look for vertical sticks (stems actually, though we could have endings
     * verticals as well), and rebuild glyphs after the stem extraction
     */
    public void processVerticals ()
    {
        // Get rid of former non-recognized symbols
        for (SystemInfo system : sheet.getSystems()) {
            removeSystemUnknowns(system);
        }

        // Retrieve stem/endings vertical candidates
        try {
            new VerticalsBuilder(sheet);
        } catch (ProcessingException ex) {
            // User has already been warned
        }
    }

    //-------------//
    // removeGlyph //
    //-------------//
    /**
     * Remove a glyph stick
     *
     * @param glyph the specified glyph
     * @param system the system it belongs to
     * @param cutSections should glyph <- section link be cut
     */
    public void removeGlyph (Glyph      glyph,
                             SystemInfo system,
                             boolean    cutSections)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Removing glyph " + glyph);
        }

        // Remove from system glyph list
        if (!system.getGlyphs()
                   .remove(glyph)) {
            logger.warning(
                "Could not remove glyph from system glyphs" + system.getId());
        }

        // Remove from lag
        glyph.destroy(cutSections);
    }

    //-------------//
    // compoundBox //
    //-------------//
    /**
     * Build a rectangular box, slightly extended to check intersection with
     * neighbouring glyphs
     *
     * @param rect the specified box
     * @param dxy the extension on every side side
     * @return the extended box
     */
    private static Rectangle compoundBox (Rectangle rect,
                                          int       dxy)
    {
        return new Rectangle(
            rect.x - dxy,
            rect.y - dxy,
            rect.width + (2 * dxy),
            rect.height + (2 * dxy));
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
        extends ConstantSet
    {
        Scale.Fraction  boxWiden = new Scale.Fraction(
            0.15,
            "Box widening to check intersection with compound");
        Constant.Double cleanupMaxGrade = new Constant.Double(
            1.2,
            "Maximum grade for cleanup phase");
        Constant.Double leafMaxGrade = new Constant.Double(
            1.01,
            "Maximum acceptance grade for a leaf");
        Constant.Double symbolMaxGrade = new Constant.Double(
            1.0001,
            "Maximum acceptance grade for a symbol");

        Constants ()
        {
            initialize();
        }
    }
}
