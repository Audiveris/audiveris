//----------------------------------------------------------------------------//
//                                                                            //
//                         L e d g e r P a t t e r n                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.CompoundBuilder;
import omr.glyph.Evaluation;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.lag.Section;

import omr.run.Orientation;

import omr.sheet.HorizontalsBuilder;
import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.EnumSet;
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
    private static final Logger logger = LoggerFactory.getLogger(LedgerPattern.class);

    /** Shapes acceptable for a ledger neighbor */
    public static final EnumSet<Shape> ledgerNeighbors = EnumSet.noneOf(
            Shape.class);

    static {
        //        ledgerNeighbors.add(Shape.GRACE_NOTE_SLASH);
        //        ledgerNeighbors.add(Shape.GRACE_NOTE_NO_SLASH);
        ledgerNeighbors.addAll(ShapeSet.Notes.getShapes());
        ledgerNeighbors.addAll(ShapeSet.NoteHeads.getShapes());
    }

    //~ Instance fields --------------------------------------------------------
    /** Companion in charge of building ledgers */
    private final HorizontalsBuilder builder;

    /** Scale-dependent parameters */
    final int interChunkDx;

    final int interChunkDy;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // LedgerPattern //
    //---------------//
    /**
     * Creates a new LedgerPattern object.
     *
     * @param system the related system
     */
    public LedgerPattern (SystemInfo system)
    {
        super("Ledger", system);
        builder = system.getHorizontalsBuilder();
        interChunkDx = scale.toPixels(constants.interChunkDx);
        interChunkDy = scale.toPixels(constants.interChunkDy);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int nb = 0;

        for (StaffInfo staff : system.getStaves()) {
            Map<Integer, SortedSet<Glyph>> ledgerMap = staff.getLedgerMap();

            for (Iterator<Entry<Integer, SortedSet<Glyph>>> iter = ledgerMap.
                    entrySet().iterator();
                    iter.hasNext();) {
                Entry<Integer, SortedSet<Glyph>> entry = iter.next();
                SortedSet<Glyph> ledgerSet = entry.getValue();
                List<Glyph> ledgerGlyphs = new ArrayList<>();

                for (Glyph ledger : ledgerSet) {
                    ledgerGlyphs.add(ledger);
                }

                // Process 
                for (Iterator<Glyph> it = ledgerSet.iterator(); it.hasNext();) {
                    Glyph ledger = it.next();
                    Set<Glyph> neighbors = new HashSet<>();

                    if (isInvalid(ledger, neighbors)) {
                        // Check if we can forge a ledger-compatible neighbor
                        Glyph compound = system.buildCompound(
                                ledger,
                                false,
                                system.getGlyphs(),
                                new LedgerAdapter(
                                system,
                                Grades.ledgerNoteMinGrade,
                                ledgerNeighbors,
                                ledgerGlyphs));

                        if (compound == null) {
                            // Here, we have not found any convincing neighbor
                            // Let's invalid this pseudo ledger
                            logger.debug("Invalid ledger {}", ledger);
                            ledger.setShape(null);
                            it.remove();
                            nb++;
                        }
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
    private boolean isInvalid (Glyph ledgerGlyph,
                               Set<Glyph> neighborGlyphs)
    {
        // A short ledger must be stuck to either a note head or a stem 
        // (or a grace note)
        List<Section> allSections = new ArrayList<>();

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
            Shape shape = glyph.getShape();

            if ((shape == Shape.STEM) || ledgerNeighbors.contains(shape)) {
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

        Scale.Fraction interChunkDx = new Scale.Fraction(
                1.5,
                "Max horizontal distance between ledger chunks");

        //
        Scale.Fraction interChunkDy = new Scale.Fraction(
                0.2,
                "Max vertical distance between ledger chunks");
    }

    //---------------//
    // LedgerAdapter //
    //---------------//
    /**
     * Adapter to actively search a ledger-compatible entity near the ledger
     * chunk.
     */
    private final class LedgerAdapter
            extends CompoundBuilder.TopShapeAdapter
    {
        //~ Instance fields ----------------------------------------------------

        private final List<Glyph> ledgerGlyphs;

        //~ Constructors -------------------------------------------------------
        public LedgerAdapter (SystemInfo system,
                              double minGrade,
                              EnumSet<Shape> desiredShapes,
                              List<Glyph> ledgerGlyphs)
        {
            super(system, minGrade, desiredShapes);
            this.ledgerGlyphs = ledgerGlyphs;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public Rectangle computeReferenceBox ()
        {
            Point2D stop = seed.getStopPoint(Orientation.HORIZONTAL);
            Rectangle rect = new Rectangle(
                    (int) Math.rint(stop.getX()),
                    (int) Math.rint(stop.getY()),
                    interChunkDx,
                    0);
            rect.grow(0, interChunkDy);
            seed.addAttachment("-", rect);

            return rect;
        }

        @Override
        public Evaluation getChosenEvaluation ()
        {
            return new Evaluation(chosenEvaluation.shape, Evaluation.ALGORITHM);
        }

        @Override
        public boolean isCandidateSuitable (Glyph glyph)
        {
            return !ledgerGlyphs.contains(glyph);
        }
    }
}
