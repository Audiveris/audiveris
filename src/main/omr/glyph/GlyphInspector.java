//-----------------------------------------------------------------------//
//                                                                       //
//                      G l y p h I n s p e c t o r                      //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph;

import omr.ProcessingException;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.score.Score;
import omr.score.ScoreManager;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.StaveInfo;
import omr.sheet.SystemInfo;
import omr.sheet.VerticalsBuilder;
import omr.stick.Stick;
import omr.util.Dumper;
import omr.util.Logger;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Iterator;

/**
 * Class <code>GlyphInspector</code> is dedicated to processing of
 * retrieved glyphs, their recognition based on features as used by a
 * neural network evaluator and a regression-based evaluator.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class GlyphInspector
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(GlyphInspector.class);

    //~ Instance variables ------------------------------------------------

    private final Sheet        sheet;
    private final GlyphLag     vLag;
    private final GlyphBuilder builder;

    //~ Constructors ------------------------------------------------------

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
        this.sheet   = sheet;
        this.builder = builder;

        vLag = sheet.getVerticalLag();
    }

    //~ Methods -----------------------------------------------------------

    //----------------//
    // evaluateGlyphs //
    //----------------//
    /**
     * All symbol glyphs of the sheet, for which we can get a common vote
     * of the evaluators, are assigned the voted shape.
     *
     * @param common if true, common vote (network + regression) is
     * required, otherwise only the network vote is used
     */
    public void evaluateGlyphs (boolean common)
    {
        int acceptNb = 0;
        int knownNb = 0;
        int noiseNb = 0;
        int clutterNb = 0;
        for (int id = sheet.getFirstSymbolId();
             id <= vLag.getLastGlyphId(); id++) {
            Glyph glyph = vLag.getGlyph(id);
            if (glyph != null) {
                if (glyph.getShape() == null) {
                    // Get vote
                    Shape vote;
                    if (common) {
                        vote = commonVote(glyph);
                    } else {
                        vote = GlyphNetwork.getInstance().vote(glyph);
                    }

                    if (vote != null) {
                        glyph.setShape(vote);
                        acceptNb++;
                        if (glyph.isKnown()) {
                            knownNb++;
                        } else if (vote == Shape.NOISE) {
                            noiseNb++;
                        } else if (vote == Shape.CLUTTER) {
                            clutterNb++;
                        }
                    }
                }
            }
        }
        logger.info(acceptNb + " glyph(s) accepted (" +
                    knownNb + " as known, " +
                    noiseNb + " as noise, " +
                    clutterNb + " as clutter)");
    }
    //------------//
    // commonVote //
    //------------//
    /**
     * Look for a common vote, that is the vote must be the same for both
     * evaluators, otherwise a null value is returned
     *
     * @param glyph the glyph to evaluate
     * @return the common vote, or null
     */
    public static Shape commonVote (Glyph glyph)
    {
        final boolean useNetwork    = constants.useNetwork.getValue();
        final boolean useRegression = constants.useRegression.getValue();

        if (useNetwork) {
            Shape nVote = GlyphNetwork.getInstance().vote(glyph);
            if (useRegression) {
                Shape mVote = GlyphRegression.getInstance().vote(glyph);
                if (nVote != null && nVote == mVote) {
                    return nVote;
                }
            } else {
                return nVote;
            }
        } else {
            if (useRegression) {
                return GlyphRegression.getInstance().vote(glyph);
            } else {
                logger.warning("No evaluator activated");
            }
        }

        return null;
    }

    //----------------------//
    // removeSystemUnknowns //
    //----------------------//
    /**
     * On a specified system, look for all unknown glyphs, and remove them
     * from its glyphs collection as well as from the containing lag.
     * Purpose is to prepare room for a new glyph extraction
     *
     * @param system the specified system
     */
    public static void removeSystemUnknowns (SystemInfo system)
    {
        List<Glyph> toremove = new ArrayList<Glyph>();
        for (Glyph glyph : system.getGlyphs()) {
            if (!glyph.isKnown()) {
                toremove.add(glyph);
            }
        }
        // Remove from system list
        system.getGlyphs().removeAll(toremove);

        // Remove from lag
        for (Glyph glyph : toremove) {
            glyph.destroy(/* cutSections => */ true);
        }
    }

    //------------------//
    // sortSystemGlyphs //
    //------------------//
    /**
     * Sort all glyphs in the system, according to the left abscissa of
     * their contour box
     *
     * @param system the system whose glyphs are to be sorted
     */
    public static void sortSystemGlyphs (SystemInfo system)
    {
        Collections.sort(system.getGlyphs(),
                         new Comparator<Glyph>() {
                             public int compare(Glyph o1,
                                                Glyph o2) {
                                 return o1.getContourBox().x
                                     -  o2.getContourBox().x;
                             }
                         });
    }

    //-----------------//
    // sortSystemStems //
    //-----------------//
    /**
     * Sort all stems in the system, according to the left abscissa of
     * their contour box
     *
     * @param system the system whose stems are to be sorted
     */
    public static void sortSystemStems (SystemInfo system)
    {
        Collections.sort(system.getStems(),
                         new Comparator<Stick>() {
                             public int compare(Stick o1,
                                                Stick o2) {
                                 return o1.getContourBox().x
                                     -  o2.getContourBox().x;
                             }
                         });
    }

    //------------------//
    // processVerticals //
    //------------------//
    /**
     * Look for vertical sticks (stems actually, though we could have
     * endings verticals as well), and rebuild glyphs after the stem
     * extraction
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
            // User already warned
            return;                     // Safer
        }

        // Add the stems as glyphs
        for (SystemInfo system : sheet.getSystems()) {
            for (Stick stem : system.getStems()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding stem " + stem);
                }
                system.getGlyphs().add(stem);

                // Compute glyph features
                //GlyphBuilder.computeGlyphFeatures(system, stem);
            }
        }
    }

    //---------------//
    // processLeaves //
    //---------------//
    /**
     * Retrieve leaves that appear thanks to segmentation due to stems
     * extraction.
     */
    public void processLeaves()
    {
        // Nota: Leaves are already added to the proper system glyph
        // collection
        builder.buildInfo();

        // Sort glyphs on their abscissa
        for (SystemInfo system : sheet.getSystems()) {
            sortSystemGlyphs(system);
        }
    }

    //------------------//
    // processCompounds //
    //------------------//
    /**
     * Look for glyphs portions that should be considered as parts of
     * compound glyphs
     *
     * @return the number of successful compounds
     */
    public int processCompounds ()
    {
        int compoundNb = 0;
        for (SystemInfo system : sheet.getSystems()) {
            compoundNb += processSystemCompounds(system);
        }

        logger.info(compoundNb + " compound(s) recognized");
        return compoundNb;
    }

    //------------------------//
    // processSystemCompounds //
    //------------------------//
    /**
     *In the specified system, look for glyphs portions that should be
     * considered as parts of compound glyphs
     *
     * @param system the system where splitted glyphs are looked for
     * @return the number of successful compounds
     */
    public int processSystemCompounds (SystemInfo system)
    {
        int nb = 0;

        // Sort unknown glyphs by decreasing weight
        List<Glyph> glyphs = new ArrayList<Glyph>(system.getGlyphs().size());
        for (Glyph glyph : system.getGlyphs()) {
            if (!glyph.isKnown()) {
                glyphs.add(glyph);
            }
        }
        Collections.sort(glyphs,
                         new Comparator<Glyph>() {
                             public int compare(Glyph o1,
                                                Glyph o2) {
                                 return o2.getWeight()
                                     -  o1.getWeight();
                             }
                         });

        // Process each glyph in turn, by looking at smaller ones
        int index = -1;
        for (Glyph glyph : glyphs) {
            index++;
            // Since the glyphs are modified on the fly ...
            if (glyph.isKnown()) {
                continue;
            }

            // Use a widened contour box
            StaveInfo stave = system.getStaveAtY(glyph.getContourBox().y);
            int dxy = stave.getScale().fracToPixels(constants.boxWiden);
            Rectangle box = compoundBox(glyph.getContourBox(), dxy);

            // Consider neighboring glyphs, which are glyphs whose contour
            // intersect the contour of glyph at hand
            for (Glyph g : glyphs.subList(index +1, glyphs.size())) {
                if (g.isKnown()) {
                    continue;
                }

                if (box.intersects(g.getContourBox())) {
                    // Let's try a compound
                    List<Glyph> parts = Arrays.asList(glyph, g);
                    Glyph compound = builder.buildCompound(parts);
                    if (logger.isDebugEnabled()) {
                        logger.debug(glyph + " & " + g + " -> " + compound);
                    }

                    Shape vote = commonVote(compound);
                    if (vote != null
                        && vote != Shape.NOISE
                        && vote != Shape.CLUTTER) {
                        compound.setShape(vote);
                        builder.insertCompound(compound, parts);
                        nb ++;
                        if (logger.isDebugEnabled()) {
                            logger.debug("Compound " + compound);
                        }
                    }
                }
            }
        }

        return nb;
    }

    //-------------//
    // compoundBox //
    //-------------//
    /**
     * Build a rectangular box, slightly extended to check intersection
     * with neighbouring glyphs
     *
     * @param rect the specified box
     * @param dxy the extension on every side side
     * @return the extended box
     */
    private static Rectangle compoundBox (Rectangle rect,
                                          int       dxy)
    {
        return new Rectangle (rect.x - dxy, rect.y - dxy,
                              rect.width + 2*dxy, rect.height + 2*dxy);
    }

    //-------------------//
    // processUndueStems //
    //-------------------//
    /**
     * Look for all stems that should not be kept, rebuild surrounding
     * glyphs and try to recognize them
     *
     * @return the number of symbols recognized
     */
    public int processUndueStems()
    {
        int symbolNb = 0;
        for (SystemInfo system : sheet.getSystems()) {
            symbolNb += processSystemUndueStems(system);
        }

        logger.info(symbolNb + " symbol(s) from stem cancellation");

        return symbolNb;
    }

    //-------------------------//
    // processSystemUndueStems //
    //-------------------------//
    /**
     * In a specified system, look for all stems that should not be kept,
     * rebuild surrounding glyphs and try to recognize them. If this action
     * does not lead to some recognized symbol, then we restore the stems.
     *
     * @param system the specified system
     * @return the number of symbols recognized
     */
    public int processSystemUndueStems (SystemInfo system)
    {
        logger.debug("processSystemUndueStems " + system);
        int nb = 0;

        // Collect all undue stems
        List<Stick> SuspectedStems = new ArrayList<Stick>();
        for (Iterator<Stick> it = system.getStems().iterator(); it.hasNext();) {
            Stick stem = it.next();
            if (!stem.hasSymbols()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Suspected Stem " + stem);
                }
                SuspectedStems.add(stem);

                // (Temporarily) cut the link from sections to their
                // containing stems
                for (GlyphSection section : stem.getMembers()) {
                    section.setGlyph(null);
                }

                // Remove these stems since nearby stems are used for
                // recognition
                it.remove();
                removeGlyph(stem, system, /*cutSections=>*/ true);
            }
        }

        // Extract brand new glyphs
        extractNewSystemGlyphs(system);

        // Try to recognize each glyph in turn
        List<Glyph> symbols = new ArrayList<Glyph>();
        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == null) {
                //Shape vote = commonVote(glyph);
                Shape vote = GlyphNetwork.getInstance().vote(glyph);
                if (vote != null) {
                    glyph.setShape(vote);
                    if (glyph.isKnown()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("New symbol " + glyph);
                        }
                        symbols.add(glyph);
                        nb++;
                    }
                }
            }
        }

        // Keep stems that have not been replaced by symbols, definitively
        // remove the others
        for (Stick stem : SuspectedStems) {
            // Check if one of its section is now part of a symbol
            boolean known = false;
            Glyph glyph = null;
            for (GlyphSection section : stem.getMembers()) {
                glyph = section.getGlyph();
                if (glyph != null && glyph.isKnown()) {
                    known = true;
                    //removeStem(stem, system, /* cutSections => */ false);
                    break;
                }
            }
            if (!known) {
                // Remove the newly created glyph
                if (glyph != null) {
                    removeGlyph(glyph, system, /* cutSections => */ true);
                }

                // Restore the stem
                system.getStems().add(stem);
                system.getGlyphs().add(stem);

                // Restore the stem <- section link
                for (GlyphSection section : stem.getMembers()) {
                    section.setGlyph(stem);
                }
            }
        }

        // Re-sort stems
        sortSystemStems(system);

        // Extract brand new glyphs
        extractNewSystemGlyphs(system);

        return nb;
    }

    //------------------------//
    // extractNewSystemGlyphs //
    //------------------------//
    /**
     * In the specified system, build new glyphs from unknown sections
     * (sections not linked to a known glyph)
     *
     * @param system the specified system
     */
    public void extractNewSystemGlyphs (SystemInfo system)
    {
        removeSystemUnknowns(system);
        sheet.getGlyphBuilder().retrieveSystemGlyphs(system);
        sortSystemGlyphs(system);
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
        if (logger.isDebugEnabled()) {
            logger.debug ("Removing glyph " + glyph);
        }

        // Remove from system glyph list
        if (!system.getGlyphs().remove(glyph)) {
            logger.warning ("Could not remove glyph from system glyphs"
                            + system.getId());
        }

        // Remove from lag
        glyph.destroy(cutSections);
    }

    //------------//
    // removeStem //
    //------------//
    /**
     * Remove a stem stick
     *
     * @param stem the specified stem
     * @param system the system it belongs to
     * @param cutSections should stem <- section link be cut
     */
    public void removeStem (Glyph      stem,
                            SystemInfo system,
                            boolean    cutSections)
    {
        removeGlyph(stem, system, cutSections);

        // Remove from system stem list
        if (!system.getStems().remove(stem)) {
            logger.warning ("Could not remove stem from system stems "
                            + system.getId());
        }
    }

    //-----------//
    // Constants // -------------------------------------------------------
    //-----------//
    private static class Constants
        extends ConstantSet
    {
        Constant.Boolean useNetwork = new Constant.Boolean
                (true,
                 "Should we use the Neural Network evaluator ?");

        Constant.Boolean useRegression = new Constant.Boolean
                (true,
                 "Should we use the Regression evaluator ?");

        Scale.Fraction boxWiden = new Scale.Fraction
                (0.15,
                 "Box widening to check intersection with compound");

        Constants ()
        {
            initialize();
        }
    }
}
