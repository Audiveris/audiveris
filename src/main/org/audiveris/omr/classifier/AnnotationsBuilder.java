//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A n n o t a t i o n s B u i l d e r                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.classifier;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.ArticulationInter;
import org.audiveris.omr.sig.inter.BarConnectorInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.BraceInter;
import org.audiveris.omr.sig.inter.BracketConnectorInter;
import org.audiveris.omr.sig.inter.BracketInter;
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.FermataArcInter;
import org.audiveris.omr.sig.inter.FermataDotInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.KeyAlterInter;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.PedalInter;
import org.audiveris.omr.sig.inter.RepeatDotInter;
import org.audiveris.omr.sig.inter.SegmentInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallBeamInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.ChordArticulationRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.SheetAnnotations;
import org.audiveris.omrdataset.api.SheetAnnotations.SheetInfo;
import org.audiveris.omrdataset.api.SymbolInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.xml.bind.JAXBException;

/**
 * Class {@code AnnotationsBuilder} processes a sheet to build the symbols Annotations
 * for an OMR DataSet.
 *
 * @author Hervé Bitteur
 */
public class AnnotationsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AnnotationsBuilder.class);

    /** Inter excluded classes. */
    private static final Set<Class> excludedInterClasses = new HashSet<Class>();

    static {
        excludedInterClasses.add(AbstractChordInter.class);
        excludedInterClasses.add(BarConnectorInter.class);
        excludedInterClasses.add(BeamInter.class);
        excludedInterClasses.add(BeamHookInter.class); // TODO: should be defined in OmrShape?
        excludedInterClasses.add(BracketConnectorInter.class);
        excludedInterClasses.add(BracketInter.class); // TODO: should be defined in OmrShape?
        excludedInterClasses.add(EndingInter.class);
        excludedInterClasses.add(FermataArcInter.class);
        excludedInterClasses.add(FermataDotInter.class);
        excludedInterClasses.add(KeyInter.class);
        excludedInterClasses.add(RepeatDotInter.class); // Processed via BarlineInter
        excludedInterClasses.add(SegmentInter.class);
        excludedInterClasses.add(SentenceInter.class);
        excludedInterClasses.add(SlurInter.class);
        excludedInterClasses.add(SmallBeamInter.class);
        ///excludedInterClasses.add(StemInter.class);
        excludedInterClasses.add(TimeNumberInter.class); // Processed via TimePairInter
        excludedInterClasses.add(WedgeInter.class);
        excludedInterClasses.add(WordInter.class);
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** The sheet to process. */
    private final Sheet sheet;

    /** Target path for sheet annotations file. */
    private final Path path;

    /** The annotations structure being built. */
    private final SheetAnnotations annotations = new SheetAnnotations();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AnnotationsBuilder} object.
     *
     * @param sheet the sheet to export
     * @param path  path to annotations file
     */
    public AnnotationsBuilder (Sheet sheet,
                               Path path)
    {
        this.sheet = sheet;
        this.path = path;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Process the sheet to generate the corresponding annotations.
     *
     * @throws IOException   for any IO error
     * @throws JAXBException for any JAXB error
     */
    public void processSheet ()
            throws IOException, JAXBException
    {
        // Global informations
        annotations.setVersion("1.0");
        annotations.setComplete(false);
        annotations.setSource(WellKnowns.TOOL_NAME + " " + WellKnowns.TOOL_REF);
        annotations.setSheetInfo(
                new SheetInfo(
                        sheet.getId() + Annotations.SHEET_IMAGE_SUFFIX,
                        new Dimension(sheet.getWidth(), sheet.getHeight())));

        // Populate system by system
        for (SystemInfo system : sheet.getSystems()) {
            new SystemAnnotator(system).processSystem();
        }

        // Marshall the result
        annotations.marshall(path);
        logger.info("Sheet annotated as {}", path);
    }

    /**
     * Check whether the provided Inter subclass is excluded for Annotations.
     *
     * @param interClass the inter class
     * @return true if excluded
     */
    private static boolean isExcluded (Class interClass)
    {
        for (Class classe : excludedInterClasses) {
            if (classe.isAssignableFrom(interClass)) {
                return true;
            }
        }

        return false;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------------//
    // SystemAnnotator //
    //-----------------//
    /**
     * Process a system for annotations.
     */
    private class SystemAnnotator
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Related system. */
        private final SystemInfo system;

        /** System sig. */
        private final SIGraph sig;

        /** All system note heads, sorted by abscissa. */
        private List<Inter> allHeads;

        //~ Constructors ---------------------------------------------------------------------------
        public SystemAnnotator (SystemInfo system)
        {
            this.system = system;
            sig = system.getSig();
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Export a barline group, including repeat dots if any.
         * <p>
         * We export something only when called on the first barline within the group.
         * We build the whole group, including repeat dots.
         *
         * @param bar a barline
         */
        private void exportBarlineGroup (BarlineInter bar)
        {
            SortedSet<Inter> items = bar.getGroupItems();

            // Make sure first barline is ours
            for (Inter item : items) {
                if (item instanceof BarlineInter) {
                    if (item != bar) {
                        return;
                    }

                    break;
                }
            }

            final int interline = bar.getStaff().getSpecificInterline();

            if (items.size() == 1) {
                // Isolated barline
                OmrShape oShape = OmrShapeMapping.SHAPE_TO_OMRSHAPE.get(bar.getShape());
                annotations.addSymbol(
                        new SymbolInfo(oShape, interline, bar.getId(), null, bar.getBounds()));
            } else {
                List<SymbolInfo> inners = new ArrayList<SymbolInfo>();

                for (Inter item : items) {
                    OmrShape oShape = OmrShapeMapping.SHAPE_TO_OMRSHAPE.get(item.getShape());
                    inners.add(
                            new SymbolInfo(oShape, interline, item.getId(), null, item.getBounds()));
                }

                // Determine the outer shape
                OmrShape oShape = getBarGroupShape(items);
                SymbolInfo outer = new SymbolInfo(
                        oShape,
                        interline,
                        null,
                        null,
                        Inters.getBounds(items));

                for (SymbolInfo inner : inners) {
                    outer.addInnerSymbol(inner);
                }

                annotations.addSymbol(outer);
            }
        }

        /**
         * (Try to) generate the symbol for the provided inter.
         *
         * @param inter the provided inter
         */
        private void exportInter (Inter inter)
        {
            final Class interClass = inter.getClass();

            // Excluded class?
            if (isExcluded(interClass)) {
                logger.debug("{} class is excluded.", inter);

                return;
            }

            Shape interShape = inter.getShape();
            Rectangle interBounds = inter.getBounds();
            Staff staff = inter.getStaff();
            OmrShape omrShape;

            // Specific classes
            if (inter instanceof TimePairInter) {
                exportTimePair((TimePairInter) inter);

                return;
            } else if (inter instanceof BarlineInter) {
                exportBarlineGroup((BarlineInter) inter);

                return;
            } else if (inter instanceof KeyAlterInter) {
                omrShape = getOmrShape((KeyAlterInter) inter);
            } else if (inter instanceof ArticulationInter) {
                omrShape = getOmrShape((ArticulationInter) inter);
            } else {
                omrShape = OmrShapeMapping.SHAPE_TO_OMRSHAPE.get(interShape);

                if (omrShape == null) {
                    logger.info("{} shape is not mapped.", inter);

                    return;
                }

                if (inter instanceof LedgerInter) {
                    // Make sure we have no note head centered on this ledger
                    if (ledgerHasHead((LedgerInter) inter)) {
                        return;
                    }
                } else if (inter instanceof BarlineInter) {
                    // Substitute a barlineDouble when relevant
                    BarlineInter sibling = ((BarlineInter) inter).getSibling();

                    if (sibling != null) {
                        if (inter.getCenter().x < sibling.getCenter().x) {
                            omrShape = OmrShape.barlineDouble;
                            interBounds = interBounds.union(sibling.getBounds());
                        } else {
                            return;
                        }
                    }
                }
            }

            if (staff == null) {
                if (inter instanceof BraceInter) {
                    // Simply pick up the first staff embraced by this brace
                    staff = ((BraceInter) inter).getFirstStaff();
                } else if (inter instanceof StemInter) {
                    // Use staff of first head found
                    staff = getStemStaff((StemInter) inter);
                } else if (inter instanceof PedalInter) {
                    // Use bottom staff of related chord if any
                    staff = getPedalStaff((PedalInter) inter);
                }
            }

            if (omrShape == null) {
                logger.warn("{} has no OmrShape, ignored.", inter);

                return;
            }

            if (staff == null) {
                logger.info("{} has no related staff, ignored.", inter);

                return;
            }

            final int interline = staff.getSpecificInterline();
            annotations.addSymbol(
                    new SymbolInfo(omrShape, interline, inter.getId(), null, interBounds));
        }

        /**
         * Export a TimePairInter (time signature defined as a pair num/den).
         * <p>
         * It the pair is known as a predefined combo, we generate one outer symbol (the pair) with
         * two inner symbols (num & den).
         * Otherwise, we simply generate two stand-alone symbols (num & den).
         *
         * @param pair the inter to process
         */
        private void exportTimePair (TimePairInter pair)
        {
            final int interline = pair.getStaff().getSpecificInterline();
            final List<SymbolInfo> inners = new ArrayList<SymbolInfo>();

            for (Inter inter : pair.getMembers()) {
                OmrShape oShape = OmrShapeMapping.SHAPE_TO_OMRSHAPE.get(inter.getShape());

                if (oShape != null) {
                    inners.add(
                            new SymbolInfo(oShape, interline, inter.getId(), null, inter.getBounds()));
                }
            }

            final OmrShape pairShape = OmrShapeMapping.getTimeCombo((TimePairInter) pair);

            if (pairShape != null) {
                SymbolInfo outer = new SymbolInfo(
                        pairShape,
                        interline,
                        pair.getId(),
                        null,
                        pair.getBounds());

                for (SymbolInfo inner : inners) {
                    outer.addInnerSymbol(inner);
                }

                annotations.addSymbol(outer);
            } else {
                logger.info("{} is not a predefined time combo.", pair);

                for (SymbolInfo inner : inners) {
                    annotations.addSymbol(inner);
                }
            }
        }

        /**
         * Determine the proper OmrShape for the provided bar group.
         *
         * @param items inters that compose the barline group (perhaps including dots)
         * @return the corresponding OmrShape
         */
        private OmrShape getBarGroupShape (SortedSet<Inter> items)
        {
            final Inter first = items.first();
            final Inter last = items.last();

            if (first instanceof RepeatDotInter) {
                if (last instanceof RepeatDotInter) {
                    return OmrShape.repeatRightLeft;
                } else {
                    return OmrShape.repeatRight;
                }
            } else {
                if (last instanceof RepeatDotInter) {
                    return OmrShape.repeatLeft;
                } else {
                    // No repeat dot on either side
                    final Shape firstShape = first.getShape();
                    final Shape lastShape = last.getShape();

                    if (firstShape == Shape.THICK_BARLINE) {
                        if (lastShape == Shape.THICK_BARLINE) {
                            return OmrShape.barlineHeavyHeavy;
                        } else {
                            return OmrShape.barlineReverseFinal;
                        }
                    } else {
                        if (lastShape == Shape.THICK_BARLINE) {
                            return OmrShape.barlineFinal;
                        } else {
                            return OmrShape.barlineDouble;
                        }
                    }
                }
            }
        }

        /**
         * Report the OmrShape for a given ArticulationInter.
         *
         * @param inter the provided articulation
         * @return the proper OmrShape value
         */
        private OmrShape getOmrShape (ArticulationInter inter)
        {
            Boolean above = null;

            for (Relation rel : sig.getRelations(inter, ChordArticulationRelation.class)) {
                final HeadChordInter chord = (HeadChordInter) sig.getOppositeInter(inter, rel);
                above = inter.getCenter().y < chord.getCenter().y;

                break;
            }

            if (above == null) {
                // No relation, use position WRT related staff
                if (inter.getStaff() != null) {
                    above = inter.getStaff().pitchPositionOf(inter.getCenter()) < 0;
                }
            }

            if (above == null) {
                return null;
            }

            switch (inter.getShape()) {
            case ACCENT:
                return above ? OmrShape.articAccentAbove : OmrShape.articAccentBelow;

            case STACCATO:
                return above ? OmrShape.articStaccatoAbove : OmrShape.articStaccatoBelow;

            case TENUTO:
                return above ? OmrShape.articTenutoAbove : OmrShape.articTenutoBelow;

            case STACCATISSIMO:
                return above ? OmrShape.articStaccatissimoAbove : OmrShape.articStaccatissimoBelow;

            case STRONG_ACCENT:
                return above ? OmrShape.articMarcatoAbove : OmrShape.articMarcatoBelow;
            }

            return null;
        }

        /**
         * Report the OmrShape for a given KeyAlterInter.
         *
         * @param inter the provided KeyAlterInter
         * @return the proper OmrShape value
         */
        private OmrShape getOmrShape (KeyAlterInter inter)
        {
            switch (inter.getShape()) {
            case FLAT:
                return OmrShape.keyFlat;

            case NATURAL:
                return OmrShape.keyNatural;

            case SHARP:
                return OmrShape.keySharp;
            }

            return null;
        }

        /**
         * Report a reasonable staff for a given pedal
         *
         * @param pedal the given pedal
         * @return head staff or null
         */
        private Staff getPedalStaff (PedalInter pedal)
        {
            AbstractChordInter chord = pedal.getChord();
            Staff staff = null;

            if (chord != null) {
                staff = chord.getBottomStaff();
            }

            if (staff == null) {
                staff = system.getStaffAtOrAbove(pedal.getCenter());
            }

            return staff;
        }

        /**
         * Report a reasonable staff for a given stem
         *
         * @param stem the given stem
         * @return head staff or null
         */
        private Staff getStemStaff (StemInter stem)
        {
            for (Relation relation : sig.getRelations(stem, HeadStemRelation.class)) {
                Inter head = sig.getEdgeSource(relation);

                return head.getStaff();
            }

            return null;
        }

        /**
         * Check whether there is a note centered on provided ledger.
         *
         * @param ledger provided ledger
         * @return true if there is a note head centered on ledger
         */
        private boolean ledgerHasHead (LedgerInter ledger)
        {
            final Rectangle ledgerBox = ledger.getBounds();
            final Staff staff = ledger.getStaff();
            final Integer index = staff.getLedgerIndex(ledger);
            final int ledgerPitch = Staff.getLedgerPitchPosition(index);
            final List<Inter> heads = Inters.intersectedInters(
                    allHeads,
                    GeoOrder.BY_ABSCISSA,
                    ledgerBox);

            for (Inter inter : heads) {
                final HeadInter head = (HeadInter) inter;
                final int notePitch = head.getIntegerPitch();

                if (notePitch == ledgerPitch) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Process the system at hand.
         */
        private void processSystem ()
        {
            allHeads = sig.inters(ShapeSet.Heads);
            Collections.sort(allHeads, Inters.byAbscissa);

            for (Inter inter : sig.vertexSet()) {
                exportInter(inter);
            }
        }
    }
}
