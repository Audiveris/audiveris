//----------------------------------------------------------------------------//
//                                                                            //
//                         L e d g e r P a t t e r n                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphNetwork;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.lag.Section;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.sheet.HorizontalsBuilder;
import omr.sheet.Ledger;
import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Predicate;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

/**
 * Class {@code LedgerPattern} checks the related system for invalid ledgers.
 *
 * @author Hervé Bitteur
 */
public class LedgerPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LedgerPattern.class);

    /** Specific predicate to filter note shapes */
    private static final Predicate<Shape> notePredicate = new Predicate<Shape>() {
        public boolean check (Shape shape)
        {
            return HorizontalsBuilder.isLedgerNeighborShape(shape);
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Companion in charge of building ledgers */
    private final HorizontalsBuilder builder;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // LedgerPattern //
    //---------------//
    /**
     * Creates a new LedgerPattern object.
     * @param system the related system
     */
    public LedgerPattern (SystemInfo system)
    {
        super("Ledger", system);
        builder = system.getHorizontalsBuilder();
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        final int interChunkDx = scale.toPixels(constants.interChunkDx);
        final int interChunkDy = scale.toPixels(constants.interChunkDy);
        int       nb = 0;

        for (StaffInfo staff : system.getStaves()) {
            Map<Integer, SortedSet<Ledger>> ledgerMap = staff.getLedgerMap();

            for (Iterator<Entry<Integer, SortedSet<Ledger>>> iter = ledgerMap.entrySet()
                                                                             .iterator();
                 iter.hasNext();) {
                Entry<Integer, SortedSet<Ledger>> entry = iter.next();
                int                               pitch = entry.getKey();
                SortedSet<Ledger>                 ledgerSet = entry.getValue();
                List<Glyph>                       ledgerGlyphs = new ArrayList<Glyph>();

                for (Ledger ledger : ledgerSet) {
                    ledgerGlyphs.add(ledger.getStick());
                }

                // Process 
                for (Iterator<Ledger> it = ledgerSet.iterator(); it.hasNext();) {
                    Ledger     ledger = it.next();
                    Glyph      glyph = ledger.getStick();
                    Set<Glyph> neighbors = new HashSet<Glyph>();

                    if (isInvalid(glyph, neighbors)) {
                        // Check if we have other ledgers nearby surrounding
                        // a note
                        Point2D        stop = glyph.getStopPoint();
                        PixelRectangle box = new PixelRectangle(
                            (int) Math.rint(stop.getX()),
                            (int) Math.rint(stop.getY()),
                            interChunkDx,
                            0);
                        box.grow(0, interChunkDy);
                        glyph.addAttachment("-", box);

                        List<Glyph> glyphs = system.lookupIntersectedGlyphs(
                            box);
                        glyphs.removeAll(ledgerGlyphs);

                        if (!glyphs.isEmpty()) {
                            Glyph compound = system.buildTransientCompound(
                                glyphs);
                            system.computeGlyphFeatures(compound);

                            // Check if a note appears in the top evaluations
                            Evaluation vote = GlyphNetwork.getInstance()
                                                          .topVote(
                                compound,
                                constants.maxDoubt.getValue(),
                                system,
                                notePredicate);

                            if (vote != null) {
                                compound = system.addGlyph(compound);
                                compound.setShape(
                                    vote.shape,
                                    Evaluation.ALGORITHM);

                                if (logger.isFineEnabled()) {
                                    logger.fine("Ledger note " + compound);
                                }

                                continue;
                            }
                        }

                        // Here, we have not found any convincing neighbor
                        // Let's invalid this pseudo ledger
                        if (logger.isFineEnabled()) {
                            logger.info("Invalid ledger " + glyph);
                        }

                        glyph.setShape(null);
                        glyph.clearTranslations();

                        // Nullify neighbors evaluations, since they may have
                        // been biased by ledger presence
                        for (Glyph g : neighbors) {
                            if (!g.isManualShape()) {
                                g.resetEvaluation();
                            }
                        }

                        it.remove();
                        nb++;
                    }
                }

                if (ledgerSet.isEmpty()) {
                    iter.remove();
                }
            }
        }

        return nb;
    }

    //-----------//
    // isInvalid //
    //-----------//
    private boolean isInvalid (Glyph      ledgerGlyph,
                               Set<Glyph> neighborGlyphs)
    {
        // A short ledger must be stuck to either a note head or a stem 
        // (or a grace note)
        List<Section> allSections = new ArrayList<Section>();

        for (Section section : ledgerGlyph.getMembers()) {
            allSections.addAll(section.getSources());
            allSections.addAll(section.getTargets());
            allSections.addAll(section.getOppositeSections());
        }

        for (Section sct : allSections) {
            Glyph g = sct.getGlyph();

            if ((g != null) && (g != ledgerGlyph)) {
                neighborGlyphs.add(g);
            }
        }

        for (Glyph glyph : neighborGlyphs) {
            if (HorizontalsBuilder.isLedgerNeighborShape(glyph.getShape())) {
                return false;
            }
        }

        // If this a long ledger, check farther from the staff for a note with
        // a ledger (full or chunk)
        if (builder.isFullLedger(ledgerGlyph)) {
            return false;
        }

        return true;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction   interChunkDx = new Scale.Fraction(
            1.5,
            "Max horizontal distance between ledger chunks");

        //
        Scale.Fraction   interChunkDy = new Scale.Fraction(
            0.2,
            "Max vertical distance between ledger chunks");

        //
        Evaluation.Doubt maxDoubt = new Evaluation.Doubt(
            10d,
            "Maximum doubt for note glyph");
    }
}
