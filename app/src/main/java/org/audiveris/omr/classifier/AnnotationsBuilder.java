//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A n n o t a t i o n s B u i l d e r                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractNumberInter;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.ArticulationInter;
import org.audiveris.omr.sig.inter.BarConnectorInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.BraceInter;
import org.audiveris.omr.sig.inter.BracketConnectorInter;
import org.audiveris.omr.sig.inter.BracketInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.KeyAlterInter;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.PedalInter;
import org.audiveris.omr.sig.inter.RepeatDotInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeCustomInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.ChordArticulationRelation;
import org.audiveris.omr.sig.relation.ChordWedgeRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.ui.symbol.KeyCancelSymbol;
import org.audiveris.omr.ui.symbol.KeySharpSymbol;
import org.audiveris.omr.ui.symbol.KeySymbol;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.OmrFont;
import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.SheetAnnotations;
import org.audiveris.omrdataset.api.SheetAnnotations.SheetInfo;
import org.audiveris.omrdataset.api.SymbolInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

/**
 * Class <code>AnnotationsBuilder</code> processes a sheet to build the symbols Annotations
 * for an OMR DataSet.
 *
 * @author Hervé Bitteur
 */
public class AnnotationsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AnnotationsBuilder.class);

    /** Inter excluded classes. */
    private static final Set<Class> excludedInterClasses = new HashSet<>();

    static {
        excludedInterClasses.add(AbstractChordInter.class);
        excludedInterClasses.add(BarConnectorInter.class);
        excludedInterClasses.add(BeamGroupInter.class);
        excludedInterClasses.add(BracketConnectorInter.class);
        excludedInterClasses.add(BracketInter.class); // TODO: should be defined in OmrShape?
        excludedInterClasses.add(EndingInter.class);
        //excludedInterClasses.add(KeyInter.class);
        excludedInterClasses.add(RepeatDotInter.class); // Processed via BarlineInter
        excludedInterClasses.add(SentenceInter.class);
        excludedInterClasses.add(StaffBarlineInter.class);
        excludedInterClasses.add(TimeNumberInter.class); // Processed via TimePairInter
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
     * Creates a new <code>AnnotationsBuilder</code> object.
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
     * @throws IOException        for any IO error
     * @throws JAXBException      for any JAXB error
     * @throws XMLStreamException for any XML error
     */
    public void processSheet ()
        throws IOException, JAXBException, XMLStreamException
    {
        // Global informations
        annotations.setVersion("1.0");
        annotations.setComplete(false);
        annotations.setSource(WellKnowns.TOOL_NAME + " " + WellKnowns.TOOL_REF);
        annotations.setSheetInfo(
                new SheetInfo(
                        sheet.getId() + Annotations.SHEET_IMAGE_EXTENSION,
                        new Dimension(sheet.getWidth(), sheet.getHeight())));

        // Populate system by system
        for (SystemInfo system : sheet.getSystems()) {
            new SystemAnnotator(system).processSystem();
        }

        // Marshall the result
        annotations.marshall(path);
        logger.info("Sheet annotated as {}", path);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    /**
     * Check whether the provided Inter subclass is excluded for Annotations.
     *
     * @param interClass the inter class
     * @return true if excluded
     */
    private static boolean isExcluded (Class<?> interClass)
    {
        for (Class<?> classe : excludedInterClasses) {
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
        /** Related system. */
        private final SystemInfo system;

        /** System sig. */
        private final SIGraph sig;

        /** All system note heads, sorted by abscissa. */
        private List<Inter> allHeads;

        SystemAnnotator (SystemInfo system)
        {
            this.system = system;
            sig = system.getSig();
        }

        /**
         * Export a barline group.
         * <p>
         * We export something only when called on the first barline within the group.
         *
         * @param bar a barline
         */
        private void exportBarlineGroup (BarlineInter bar)
        {
            final SortedSet<Inter> items = bar.getGroupItems();

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
                OmrShape oShape = OmrShapeMapping.omrShapeOf(bar.getShape());
                annotations.addSymbol(
                        new SymbolInfo(oShape, interline, bar.getId(), null, bar.getBounds()));
            } else {
                List<SymbolInfo> inners = new ArrayList<>();

                for (Inter item : items) {
                    OmrShape oShape = OmrShapeMapping.omrShapeOf(item.getShape());
                    inners.add(
                            new SymbolInfo(
                                    oShape,
                                    interline,
                                    item.getId(),
                                    null,
                                    item.getBounds()));
                }

                // Determine the outer shape, if any
                final OmrShape oShape = getBarGroupShape(items);
                if (oShape != null) {
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
                } else {
                    for (SymbolInfo inner : inners) {
                        annotations.addSymbol(inner);
                    }
                }
            }
        }

        /**
         * (Try to) generate the symbol for the provided inter.
         *
         * @param inter the provided inter
         */
        private void exportInter (Inter inter)
        {
            if (inter.isImplicit()) {
                return;
            }

            final Class interClass = inter.getClass();

            // Excluded class?
            if (isExcluded(interClass)) {
                logger.debug("{} class is excluded.", inter);

                return;
            }

            final Shape interShape = inter.getShape();
            final Rectangle interBounds = inter.getBounds();
            Staff staff = inter.getStaff();
            final OmrShape omrShape;

            // Specific classes
            if (inter instanceof KeyInter key) {
                if (key.isManual()) {
                    exportManualKey(key); // Export individual symbols
                }

                return;
            } else if (inter instanceof AbstractTimeInter time) {
                exportTime(time);

                return;
            } else if (inter instanceof BarlineInter barline) {
                exportBarlineGroup(barline);

                return;
            } else if (inter instanceof KeyAlterInter key) {
                omrShape = getOmrShape(key);
            } else if (inter instanceof ArticulationInter artic) {
                omrShape = getOmrShape(artic);
            } else if (inter instanceof ClefInter clef) {
                omrShape = getOmrShape(clef);
            } else if (inter instanceof SlurInter slur) {
                omrShape = slur.isTie() ? OmrShape.tie : OmrShape.slur;
            } else {
                // Standard case
                omrShape = OmrShapeMapping.omrShapeOf(interShape);

                if (omrShape == null) {
                    logger.info("{} shape is not mapped.", inter);

                    return;
                }

                if (inter instanceof LedgerInter) {
                    // Make sure we have no note head centered on this ledger
                    if (ledgerHasHead((LedgerInter) inter)) {
                        return;
                    }
                }
            }

            if (omrShape == null) {
                logger.warn("{} has no OmrShape, ignored.", inter);

                return;
            }

            if (staff == null) {
                if (inter instanceof BraceInter brace) {
                    // Simply pick up the first staff embraced by this brace
                    staff = brace.getFirstStaff();
                } else if (inter instanceof StemInter stem) {
                    // Use staff of first head found
                    staff = getStemStaff(stem);
                } else if (inter instanceof PedalInter pedal) {
                    // Use bottom staff of related chord if any
                    staff = getPedalStaff(pedal);
                } else if (inter instanceof AbstractBeamInter) {
                    // Use a related stem
                    for (Relation rel : sig.getRelations(inter, BeamStemRelation.class)) {
                        final StemInter stem = (StemInter) sig.getOppositeInter(inter, rel);
                        staff = stem.getStaff();
                        if (staff != null) {
                            break;
                        }
                    }
                } else if (inter instanceof SlurInter slur) {
                    // Use a related head
                    for (Relation rel : sig.getRelations(slur, SlurHeadRelation.class)) {
                        final HeadInter head = (HeadInter) sig.getOppositeInter(slur, rel);
                        staff = head.getStaff();
                        if (staff != null) {
                            break;
                        }
                    }
                } else if (inter instanceof WedgeInter wedge) {
                    // Use a related chord
                    for (Relation rel : sig.getRelations(wedge, ChordWedgeRelation.class)) {
                        final Inter chord = sig.getOppositeInter(wedge, rel);
                        staff = chord.getStaff();
                        if (staff != null) {
                            break;
                        }
                    }
                }
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
         * Export a manual key (for which there are no individual inters).
         * <p>
         * We need to compute the bounding box for each individual symbol.
         *
         * @param key the manual key inter
         */
        private void exportManualKey (KeyInter key)
        {
            final int id = key.getId();
            final int interline = key.getStaff().getSpecificInterline();
            final MusicFamily family = sheet.getStub().getMusicFamily();
            final int pointSize = OmrFont.getPointSize(interline);
            final MusicFont font = MusicFont.getMusicFont(family, pointSize);

            final KeySymbol symbol = (KeySymbol) key.getSymbolToDraw(font);
            final OmrShape oShape = (symbol instanceof KeyCancelSymbol) //
                    ? OmrShape.keyNatural
                    : (symbol instanceof KeySharpSymbol) //
                            ? OmrShape.keySharp
                            : OmrShape.keyFlat;
            final int fifths = symbol.fifths; // Different from key fifths for the cancel key!
            final KeySymbol.MyParams p = symbol.getParams(font);

            // Set loc to (x = left side, y = mid line)
            final Point2D loc = key.getCenter2D();
            PointUtil.add(
                    loc,
                    -p.rect.getWidth() / 2,
                    -KeyInter.getStandardPosition(fifths) * p.stepDy);

            final int sign = Integer.signum(fifths);
            for (int k = 1; k <= (fifths * sign); k++) {
                final int pitch = KeyInter.getItemPitch(k * sign, null);
                final Rectangle box = new Rectangle(
                        (int) Math.rint(loc.getX() + ((k - 1) * p.itemDx)),
                        (int) Math.rint(loc.getY() + (pitch * p.stepDy) - p.itemRect.height / 2.0),
                        p.itemRect.width,
                        p.itemRect.height);
                annotations.addSymbol(new SymbolInfo(oShape, interline, id, null, box));
            }
        }

        /**
         * Export a TimeSignature.
         * <p>
         * It the pair is known as a predefined combo, we generate one outer symbol (the pair) with
         * two inner symbols (num & den).
         * Otherwise, we simply generate two stand-alone symbols (num & den).
         *
         * @param time the inter to process
         */
        private void exportTime (AbstractTimeInter time)
        {
            final int interline = time.getStaff().getSpecificInterline();

            if (time instanceof TimePairInter pair) {
                for (Inter inter : pair.getMembers()) {
                    final Integer val = TimeNumberInter.valueOf(inter.getShape());
                    exportTimeNumber(val, inter.getBounds(), interline, inter.getId());
                }
            } else if (time instanceof TimeCustomInter custom) {
                final Rectangle wBox = custom.getSymbolBounds(interline);

                final Rectangle half = new Rectangle(wBox.x, wBox.y, wBox.width, wBox.height / 2);
                exportTimeNumber(custom.getNumerator(), half, interline, custom.getId());

                half.translate(0, half.height);
                exportTimeNumber(custom.getDenominator(), half, interline, custom.getId());
            } else if (time instanceof TimeWholeInter whole) {
                final int id = whole.getId();
                final Rectangle wBox = whole.getBounds();
                final OmrShape oShape = OmrShapeMapping.omrShapeOf(whole.getShape());

                if (oShape != null) {
                    // COMMON_TIME, CUT_TIME
                    annotations.addSymbol(new SymbolInfo(oShape, interline, id, null, wBox));
                } else {
                    // TIME_FOUR_FOUR, TIME_TWELVE_EIGHT, etc ...
                    final TimeRational rat = AbstractTimeInter.rationalOf(whole.getShape());
                    Rectangle half = new Rectangle(wBox.x, wBox.y, wBox.width, wBox.height / 2);
                    exportTimeNumber(rat.num, half, interline, id);

                    half.translate(0, half.height);
                    exportTimeNumber(rat.den, half, interline, id);
                }
            } else {
                logger.error("Unsupported time object: " + time);
            }
        }

        /**
         * Export a numeric time value.
         *
         * @param val       time value between 1 and 99
         * @param box       symbol bounds
         * @param interline related staff interline
         * @param id        relevant inter ID
         */
        private void exportTimeNumber (int val,
                                       Rectangle box,
                                       int interline,
                                       int id)
        {
            if (val <= 9) {
                // Export 1 digit
                final Shape shape = AbstractNumberInter.shapeOf(val);
                final OmrShape oShape = OmrShapeMapping.omrShapeOf(shape);
                annotations.addSymbol(new SymbolInfo(oShape, interline, id, null, box));
            } else {
                // Export 2 digits
                final Rectangle half = new Rectangle(box.x, box.y, box.width / 2, box.height);
                final int dec = val / 10;
                Shape shape = AbstractNumberInter.shapeOf(dec);
                OmrShape oShape = OmrShapeMapping.omrShapeOf(shape);
                annotations.addSymbol(new SymbolInfo(oShape, interline, id, null, half));

                half.translate(half.width, 0);
                final int unit = val % 10;
                shape = AbstractNumberInter.shapeOf(unit);
                oShape = OmrShapeMapping.omrShapeOf(shape);
                annotations.addSymbol(new SymbolInfo(oShape, interline, id, null, half));
            }
        }

        /**
         * Determine the proper OmrShape for the provided bar group.
         *
         * @param items inters that compose the barline group (perhaps including dots)
         * @return the corresponding OmrShape if any, null otherwise
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
                        return null;
                        //                        if (lastShape == Shape.THICK_BARLINE) {
                        //                            return OmrShape.barlineHeavyHeavy;
                        //                        } else {
                        //                            return OmrShape.barlineReverseFinal;
                        //                        }
                    } else {
                        if (lastShape == Shape.THICK_BARLINE) {
                            return null; // OmrShape.barlineFinal;
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
                if ((inter.getStaff() != null) && !inter.getStaff().isTablature()) {
                    above = inter.getStaff().pitchPositionOf(inter.getCenter()) < 0;
                }
            }

            if (above == null) {
                return null;
            }

            return switch (inter.getShape()) {
                case ACCENT -> above //
                        ? OmrShape.articAccentAbove
                        : OmrShape.articAccentBelow;
                case STACCATO -> above //
                        ? OmrShape.articStaccatoAbove
                        : OmrShape.articStaccatoBelow;
                case TENUTO -> above //
                        ? OmrShape.articTenutoAbove
                        : OmrShape.articTenutoBelow;
                case STACCATISSIMO -> above //
                        ? OmrShape.articStaccatissimoAbove
                        : OmrShape.articStaccatissimoBelow; // Very rare?
                case STACCATISSIMO_BELOW -> above //
                        ? OmrShape.articStaccatissimoAbove // Very rare?
                        : OmrShape.articStaccatissimoBelow;
                case MARCATO -> above //
                        ? OmrShape.articMarcatoAbove
                        : OmrShape.articMarcatoBelow; // Very rare?
                case MARCATO_BELOW -> above //
                        ? OmrShape.articMarcatoAbove // Very rare?
                        : OmrShape.articMarcatoBelow;
                default -> null;
            };
        }

        /**
         * Report the OmrShape for a given ClefInter.
         *
         * @param inter the provided ClefInter
         * @return the proper OmrShape value
         */
        private OmrShape getOmrShape (ClefInter inter)
        {
            return switch (inter.getShape()) {
                case G_CLEF, G_CLEF_SMALL, G_CLEF_8VA, G_CLEF_8VB -> OmrShape.gClef;
                case C_CLEF -> inter.getIntegerPitch() == 0 //
                        ? OmrShape.cClefAlto
                        : OmrShape.cClefTenor;
                case F_CLEF, F_CLEF_SMALL, F_CLEF_8VA, F_CLEF_8VB -> OmrShape.fClef;
                case PERCUSSION_CLEF -> OmrShape.unpitchedPercussionClef1;
                default -> null;
            };
        }

        /**
         * Report the OmrShape for a given KeyAlterInter.
         *
         * @param inter the provided KeyAlterInter
         * @return the proper OmrShape value
         */
        private OmrShape getOmrShape (KeyAlterInter inter)
        {
            return switch (inter.getShape()) {
                case FLAT -> OmrShape.keyFlat;
                case NATURAL -> OmrShape.keyNatural;
                case SHARP -> OmrShape.keySharp;
                default -> null;
            };
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
         * Check whether there is a note head centered on provided ledger.
         *
         * @param ledger provided ledger
         * @return true if there is a note head centered on ledger
         */
        private boolean ledgerHasHead (LedgerInter ledger)
        {
            final Rectangle ledgerBox = ledger.getBounds();
            final Staff staff = ledger.getStaff();
            if (staff.isTablature()) {
                return false;
            }

            final int ledgerPitch = (int) Math.rint(staff.pitchPositionOf(ledger.getCenter()));
            final List<Inter> heads = Inters.intersectedInters(
                    allHeads,
                    GeoOrder.BY_ABSCISSA,
                    ledgerBox);

            for (Inter inter : heads) {
                final HeadInter head = (HeadInter) inter;
                final int headPitch = head.getIntegerPitch();

                if (headPitch == ledgerPitch) {
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
