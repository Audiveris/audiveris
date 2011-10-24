//----------------------------------------------------------------------------//
//                                                                            //
//                         L e d g e r P a t t e r n                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.glyph.Shape;
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.lag.Section;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class {@code LedgerPattern} checks the related system for invalid ledgers.
 *
 * @author Hervé Bitteur
 */
public class LedgerPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LedgerPattern.class);

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
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int nb = 0;

        for (Iterator<Glyph> it = system.getLedgers()
                                        .iterator(); it.hasNext();) {
            if (isInvalid(it.next())) {
                it.remove();
                nb++;
            }
        }

        if (nb > 0) {
            for (StaffInfo staff : system.getStaves()) {
                staff.checkLedgers();
            }
        }

        return nb;
    }

    //-----------//
    // isInvalid //
    //-----------//
    private boolean isInvalid (Glyph ledgerGlyph)
    {
        // A short ledger is stuck to either a note head or a stem 
        // (TODO: or a grace note)
        List<Section> neighbors = new ArrayList<Section>();

        for (Section section : ledgerGlyph.getMembers()) {
            neighbors.addAll(section.getSources());
            neighbors.addAll(section.getTargets());
            neighbors.addAll(section.getOppositeSections());
        }

        Set<Section> toCheck = new HashSet<Section>();

        for (Section sct : neighbors) {
            if (sct.getGlyph() != ledgerGlyph) {
                toCheck.add(sct);
            }
        }

        for (Section sct : toCheck) {
            Glyph glyph = sct.getGlyph();

            if (glyph == null) {
                continue;
            }

            Shape shape = glyph.getShape();

            if (shape == null) {
                continue;
            }

            if ((shape == Shape.COMBINING_STEM) ||
                //                (shape == Shape.GRACE_NOTE_SLASH) ||
            //                (shape == Shape.GRACE_NOTE_NO_SLASH) ||
            ShapeRange.NoteHeads.contains(shape)) {
                return false;
            }
        }

        // Here, we have not found any convincing neighbor
        // Let's invalid this pseudo ledger
        if (logger.isFineEnabled()) {
            logger.info("Invalid ledger " + ledgerGlyph);
        }

        ledgerGlyph.setShape(null);

        // Nullify neighbors evaluations, since they may have been biased by
        // ledger presence
        for (Section sct : toCheck) {
            Glyph glyph = sct.getGlyph();

            if (glyph != null) {
                if (!glyph.isManualShape()) {
                    glyph.resetEvaluation();
                }
            }
        }

        return true;
    }
}
