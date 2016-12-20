//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       K e y B u i l d e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.header;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Grades;
import omr.glyph.Shape;

import omr.math.HiLoPeakFinder;
import omr.math.IntegerFunction;
import omr.math.Range;

import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.header.KeyColumn.PartStatus;

import omr.sig.GradeUtil;
import omr.sig.SIGraph;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.ClefInter;
import omr.sig.inter.ClefInter.ClefKind;
import omr.sig.inter.Inter;
import omr.sig.inter.KeyAlterInter;
import omr.sig.inter.KeyInter;
import omr.sig.relation.ClefKeyRelation;
import omr.sig.relation.KeyAltersRelation;

import omr.ui.Colors;

import omr.util.ChartPlotter;
import omr.util.IntUtil;
import omr.util.Navigable;

import ij.process.ByteProcessor;

import org.jfree.data.xy.XYSeries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class {@code KeyBuilder} retrieves a staff key signature through the vertical
 * projection to x-axis of the foreground pixels in a given abscissa range of a staff.
 * <p>
 * An instance typically handles the initial key signature, perhaps void, at the beginning of a
 * staff.
 * Another instance may be used to process a key signature change located farther in the staff,
 * generally right after a double bar line.
 * <p>
 * A key signature is a sequence of consistent alterations (all sharps or all flats or none) in a
 * predefined order (FCGDAEB for sharps, BEADGCF for flats).
 * In the case of a key signature change, there may be some natural signs to explicitly cancel the
 * previous alterations, although this is not mandatory.
 * <p>
 * <img src="http://www.musicarrangers.com/star-theory/images/p14a.gif">
 * <p>
 * <img src="http://www.musicarrangers.com/star-theory/images/p14b.gif">
 * <p>
 * The relative positioning of alterations in a given signature is identical for all clefs (treble,
 * alto, tenor, bass) with the only exception of the sharp-based signatures in tenor clef.
 * <p>
 * <img src="http://www.musicarrangers.com/star-theory/images/p14c.gif">
 * <p>
 * The main tool is a vertical projection of the StaffHeader pixels onto the x-axis.
 * Vertically, the projection uses an envelope that can embrace any key signature (under any clef),
 * ranging from two interline values above the staff to one interline value below the staff.
 * Horizontally, the goal is to split the projection into slices, one slice for each alteration item
 * to be extracted.
 * <p>
 * Peak detection allows to retrieve "stem-like" portions (one for a flat, two for a sharp).
 * Typical x delta between two stems of a sharp is around 0.5+ interline.
 * Typical x delta between stems of 2 flats (or first stems of 2 sharps) is around 1+ interline.
 * Unfortunately, some flat-delta may be smaller than some sharp-delta...
 * <p>
 * Typical peak height (above the lines cumulated height) is around 2+ interline values.
 * All peaks have similar heights in the same key signature, this may differentiate a key signature
 * from a time signature.
 * A space, if any, between two key signature items is very narrow.
 * <p>
 * Strategy:<ol>
 * <li>Find first significant space right after clef, it's the space that separates the clef from
 * next item (key signature or time signature or first note/rest, etc).
 * This space may not be detected in the projection when the first key signature item is very close
 * to the clef, because their projections on x-axis overlap.
 * If that first space is really wide, consider there is no key signature.
 * <li>The next really wide space, if any, will mark the end of key signature.
 * <li>Look for peaks in the area, make sure each peak corresponds to some stem-like portion.
 * <li>Once all peaks have been retrieved, check delta abscissa between peaks, to differentiate
 * sharps vs flats sequence.
 * Additional help is brought by checking the left side of first peak (it is almost void for a flat
 * and not for a sharp).
 * <li>Determine the number of items.
 * <li>Determine precise splitting of the projection into vertical roi.
 * <li>Looking first at connected components within the key signature area, try to retrieve one good
 * component for each slice, by submitting each glyph compound to shape classifier to validate both
 * segmentation and shape.
 * <li>For slices left empty, force slice segmentation and perform recognition within slice only.
 * <li>Make sure the last key slice is followed by some space rather empty, to disambiguate between
 * the end of a true key signature and an accidental alteration closely followed by a note head.
 * <li>Create one KeyAlterInter instance per item.
 * <li>Create one KeyInter as an ensemble of KeyAlterInter instances.
 * <li>Check each item pitch against the pitches sequences imposed by staff clef candidate(s).
 * Register support relationship between any compatible clef candidate and key signature, then
 * compute contextual grade of clef candidates and finally choose the best clef.
 * If the key signature is not compatible with the chosen clef, then the key signature is destroyed.
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class KeyBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(KeyBuilder.class);

    /** Shapes allowed in a key signature. */
    private static final Set<Shape> keyShapes = EnumSet.of(Shape.FLAT, Shape.SHARP);

    //~ Enumerations -------------------------------------------------------------------------------
    private static enum Attribute
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Wide initial space, cannot contain key. */
        INITIAL_WIDE_SPACE;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** COntaining KeyColumn. */
    private final KeyColumn column;

    /** Dedicated staff to analyze. */
    private final Staff staff;

    /** Key range info. */
    private final StaffHeader.Range range;

    /** The containing system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    private final SIGraph sig;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Related scale. */
    private final Scale scale;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Header key signature or key signature change?. TODO: not yet used, but will be needed */
    private final boolean inHeader;

    /** Staff-free pixel source. */
    private final ByteProcessor staffFreeSource;

    /** Precise beginning abscissa of measure. */
    private final int measureStart;

    /** (Competing) active clef(s) in staff, just before key signature. */
    private final List<ClefInter> clefs = new ArrayList<ClefInter>();

    /** ROI with slices for key search. */
    private final KeyRoi roi;

    /** Projection of foreground pixels, indexed by abscissa. */
    private final IntegerFunction projection;

    /** Peak finder based on derivative HiLos. */
    private final HiLoPeakFinder peakFinder;

    /** Sequence of valid peaks found, ordered by abscissa. */
    private final List<KeyPeak> peaks = new ArrayList<KeyPeak>();

    /** Shape used for key signature. */
    private Shape keyShape;

    /** Resulting key inter, if any. */
    private KeyInter keyInter;

    /** Attributes assigned. */
    private final EnumSet<Attribute> attributes = EnumSet.noneOf(Attribute.class);

    /** (Debug) is this a vip staff?. */
    private final boolean isVip;

    /** Companion in charge of glyph extraction & recognition. */
    private final KeyExtractor extractor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code KeyBuilder} object.
     *
     * @param column       the containing KeyColumn
     * @param staff        the underlying staff
     * @param globalWidth  global plotting width
     * @param measureStart precise beginning abscissa of measure (generally right after bar line).
     * @param browseStart  estimated beginning abscissa for browsing.
     * @param inHeader     true for key signature in header, false for key signature change
     */
    KeyBuilder (KeyColumn column,
                Staff staff,
                int globalWidth,
                int measureStart,
                int browseStart,
                boolean inHeader)
    {
        this.column = column;
        this.staff = staff;
        this.inHeader = inHeader;

        isVip = IntUtil.parseInts(constants.vipStaves.getValue()).contains(staff.getId());
        system = staff.getSystem();
        sig = system.getSig();
        sheet = system.getSheet();
        staffFreeSource = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);

        scale = sheet.getScale();
        params = new Parameters(scale);

        final StaffHeader header = staff.getHeader();

        if (header.keyRange != null) {
            range = header.keyRange;
        } else {
            header.keyRange = (range = new StaffHeader.Range());
            range.browseStart = browseStart;
            range.browseStop = getBrowseStop(globalWidth, measureStart, browseStart);
        }

        this.measureStart = measureStart;

        Rectangle browseRect = getBrowseRect();
        roi = new KeyRoi(staff, browseRect.y, browseRect.height, column.getMaxSliceDist());
        projection = getProjection(browseRect);
        peakFinder = new HiLoPeakFinder("Key", projection, range.browseStart, range.browseStop);
        extractor = new KeyExtractor(staff, range, peaks, roi);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // addPlot //
    //---------//
    /**
     * Draw key signature portion for this staff within header projection.
     *
     * @param plotter header projection plotter to populate
     */
    public void addPlot (ChartPlotter plotter)
    {
        retrieveHiLoPeaks();

        Integer derStart;

        if (range.hasStart()) {
            derStart = range.getStart();
        } else {
            derStart = staff.getKeyStart();
        }

        if (derStart == null) {
            derStart = range.browseStart;
        }

        int derStop = range.getStop();

        // Peaks
        if ((peakFinder.getPeaks() != null) && !peakFinder.getPeaks().isEmpty()) {
            plotter.add(peakFinder.getPeakSeries(derStart, derStop), Colors.CHART_PEAK);
        }

        // HiLos
        plotter.add(peakFinder.getHiloSeries(derStart, derStop), Colors.CHART_HILO, true);

        {
            // Values (w/ threshold)
            XYSeries valueSeries = peakFinder.getValueSeries(
                    projection.getXMin(),
                    projection.getXMax());
            valueSeries.setKey("Key");
            plotter.add(valueSeries, Colors.CHART_VALUE);
        }

        {
            // Derivatives (w/ thresholds)
            XYSeries derSeries = peakFinder.getDerivativeSeries(derStart, derStop);
            plotter.add(derSeries, Colors.CHART_DERIVATIVE);
        }

        List<Integer> alterStarts = staff.getHeader().alterStarts;

        if (alterStarts != null) {
            XYSeries startSeries = new XYSeries("Start", false); // No autosort

            for (int ia = 0; ia < alterStarts.size(); ia++) {
                // Items marks
                double x = alterStarts.get(ia);
                startSeries.add(x, 0);
                startSeries.add(x, staff.getHeight());
                startSeries.add(x, null);
            }

            plotter.add(startSeries, Color.ORANGE);
        }

        if (range.hasStart() || (staff.getKeyStart() != null)) {
            // Area limits
            XYSeries series = new XYSeries("KeyArea", false); // No autosort
            int start = range.hasStart() ? range.getStart() : staff.getKeyStart();
            int stop = range.hasStart() ? range.getStop() : staff.getKeyStop();
            series.add(start, 0);
            series.add(start, staff.getHeight());
            series.add(stop, staff.getHeight());
            series.add(stop, 0);
            plotter.add(series, Color.ORANGE);
        }

        {
            // Browse start for peak threshold
            XYSeries series = new XYSeries("KeyBrowse", false); // No autosort
            int start = range.browseStart;
            int stop = (staff.getKeyStop() != null) ? staff.getKeyStop() : range.getStop();
            series.add(start, 0);
            series.add(start, params.minPeakValue);
            series.add(stop, params.minPeakValue);
            plotter.add(series, Color.BLACK);
        }

        {
            // Space threshold
            XYSeries chunkSeries = new XYSeries("Space", false); // No autosort
            int x = range.browseStart;
            chunkSeries.add(x, params.maxSpaceCumul);
            chunkSeries.add(projection.getXMax(), params.maxSpaceCumul);
            plotter.add(chunkSeries, Color.YELLOW);
        }
    }

    //---------------//
    // adjustPitches //
    //---------------//
    /**
     * Slightly adjust alter pitches to integer values.
     */
    public void adjustPitches ()
    {
        // Use pitches for chosen clef, if any
        if (!clefs.isEmpty()) {
            final ClefInter bestClef = clefs.get(0);
            final int[] stdPitches = KeyInter.getPitchesMap(keyShape).get(bestClef.getKind());

            for (int i = 0; i < roi.size(); i++) {
                KeySlice slice = roi.get(i);
                KeyAlterInter alter = slice.getAlter();

                if (alter == null) {
                    final Set<Shape> shapes = Collections.singleton(keyShape);
                    extractor.extractAlter(slice, shapes, Grades.keyAlterMinGrade2, false);
                }

                if (alter != null) {
                    final int std = stdPitches[i];

                    if (alter.getIntegerPitch() != std) {
                        logger.info(
                                "Staff#{} key slice#{} pitch adjusted from {} to {}",
                                getId(),
                                slice.getId(),
                                String.format("%.1f", alter.getMeasuredPitch()),
                                std);
                        alter.setPitch(std);
                    }
                }
            }
        } else {
            logger.warn("Staff#{} no header clef.", getId());

            Double[] measuredPitches = new Double[roi.size()];

            for (int i = 0; i < roi.size(); i++) {
                KeySlice slice = roi.get(i);
                KeyAlterInter alter = slice.getAlter();

                if (alter != null) {
                    measuredPitches[i] = alter.getMeasuredPitch();
                }
            }

            logger.info("Guessed clef: {}", KeyInter.guessKind(keyShape, measuredPitches, null));
        }
    }

    //----------------//
    // checkReplicate //
    //----------------//
    /**
     * Compare local keyInter with the one from the "best source" staff in part.
     * <ol>
     * <li>If fifths agree, everything is OK, return.
     * <li>Local slices and source slices must correspond, if not, adjust local ones.
     * <li>If a local slice contains no valid alter, extract one using theoretical pitch window.
     * </ol>
     * TODO: In inserting local slices, replicating offset is not precise enough when there is no
     * local slices at all. So try to adjust to local peaks (even if weak), especially for flats.
     *
     * @param sourceBuilder KeyBuilder of source staff
     * @return proper PartStatus value
     */
    public PartStatus checkReplicate (KeyBuilder sourceBuilder)
    {
        final KeyInter sourceKey = sourceBuilder.keyInter;

        if ((keyInter != null) && (keyInter.getFifths() == sourceKey.getFifths())) {
            return PartStatus.OK; // It's OK
        }

        final ClefInter clef = clefs.get(0);

        // Remove local slices if needed
        if ((keyShape != null) && (keyShape != sourceBuilder.keyShape)) {
            roi.clear(); // Sharps vs Flats: slices are too different, remove all of them
        } else {
            for (Iterator<KeySlice> it = roi.iterator(); it.hasNext();) {
                KeySlice slice = it.next();
                KeyAlterInter alter = slice.getAlter();
                final Integer index = column.getGlobalIndex(slice.getStart() - measureStart);

                if ((index == null) || (index >= sourceBuilder.roi.size())) {
                    if (alter != null) {
                        alter.delete();
                    }

                    it.remove();
                } else if (alter == null) {
                    // We have no a good local alter
                    // If starts & widths are not similar, discard all remaining local slices
                    final KeySlice sourceSlice = sourceBuilder.roi.get(index);
                    final int sStart = sourceSlice.getStart();
                    final int sWidth = sourceSlice.getWidth();

                    if ((Math.abs(sStart - slice.getStart()) > params.maxSliceDeltaX)
                        || (Math.abs(sWidth - slice.getWidth()) > params.maxSliceDeltaWidth)) {
                        it.remove();

                        while (it.hasNext()) {
                            slice = it.next();
                            alter = slice.getAlter();

                            if (alter != null) {
                                alter.delete();
                            }

                            it.remove();
                        }
                    }
                }
            }
        }

        // Insert local slices if needed
        for (int is = 0; is < sourceBuilder.getRoi().size(); is++) {
            final KeySlice sourceSlice = sourceBuilder.getRoi().get(is);
            final int offset = column.getGlobalOffset(is);
            final int targetStart = measureStart + offset;
            KeySlice localSlice = roi.getStartSlice(targetStart);

            if (localSlice == null) {
                // Choose start & stop values for the slice to be created
                final KeySlice prevSlice = roi.getStopSlice(targetStart);
                final int start = (prevSlice != null) ? (prevSlice.getStop() + 1) : targetStart;

                int targetStop = (start + sourceSlice.getWidth()) - 1;
                KeySlice nextSlice = roi.getStartSlice(targetStop + 1);
                final int stop = (nextSlice != null) ? (nextSlice.getStart() - 1) : targetStop;

                localSlice = roi.createSlice(start, stop);

                final double height = params.typicalGlyphHeight;
                localSlice.setPitchRect(clef, sourceBuilder.getKeyShape(), height);

                if (!extractor.sliceHasInk(localSlice.getRect())) {
                    // No item can exist here!
                    int lastId = Math.abs(sourceKey.getFifths());

                    if ((lastId > 1) && (sourceSlice.getId() == lastId)) {
                        // We just discard last item in source (and in sibling staves in part)
                        roi.remove(localSlice);

                        return PartStatus.SHRINK;
                    } else {
                        // We distroy any key in this part
                        return PartStatus.DESTROY;
                    }
                }
            }
        }

        // Here, we have the same number of local slices as in source
        // Check each local slice
        keyShape = sourceBuilder.getKeyShape();

        final Set<Shape> shapes = Collections.singleton(keyShape);

        for (KeySlice slice : roi) {
            KeyAlterInter alter = slice.getAlter();

            if ((alter == null) || (alter.getGrade() < Grades.keyAlterMinGrade1)) {
                slice.setPitchRect(clef, keyShape, params.typicalGlyphHeight);
                extractor.extractAlter(slice, shapes, Grades.keyAlterMinGrade2, true);
            }
        }

        // Create a brand new KeyInter with current slices & alters
        if (keyInter != null) {
            keyInter.delete();
        }

        createKeyInter();
        sig.addEdge(clef, keyInter, new ClefKeyRelation());

        return PartStatus.OK;
    }

    //---------//
    // destroy //
    //---------//
    /**
     * Remove any key material: slices potential alter.
     */
    public void destroy ()
    {
        roi.destroy();

        /// Do not nullify keyShape = null;
        //
        if (keyInter != null) {
            keyInter.delete();
            keyInter = null;
        }
    }

    //-------------//
    // finalizeKey //
    //-------------//
    public void finalizeKey ()
    {
        KeySlice lastValidSlice = roi.getLastValidSlice();

        if (lastValidSlice != null) {
            // Adjust key signature stop for this staff
            Rectangle bounds = lastValidSlice.getAlter().getBounds();
            int end = (bounds.x + bounds.width) - 1;
            staff.setKeyStop(end);

            // Create key inter
            if (keyInter == null) {
                createKeyInter();
            }

            staff.getHeader().key = keyInter;
            roi.freezeAlters();

            // Record slices starts in StaffHeader structure (used for plotting only)
            if (!roi.isEmpty()) {
                staff.getHeader().alterStarts = roi.getStarts();
            }
        }
    }

    //----------------//
    // getBrowseStart //
    //----------------//
    /**
     * @return the browseStart
     */
    public Integer getBrowseStart ()
    {
        return range.browseStart;
    }

    //-------//
    // getId //
    //-------//
    public int getId ()
    {
        return staff.getId();
    }

    //-------------//
    // getKeyInter //
    //-------------//
    public KeyInter getKeyInter ()
    {
        return keyInter;
    }

    //-------------//
    // getKeyShape //
    //-------------//
    /**
     * @return the keyShape
     */
    public Shape getKeyShape ()
    {
        return keyShape;
    }

    //-----------------//
    // getMeasureStart //
    //-----------------//
    /**
     * @return the measureStart
     */
    public int getMeasureStart ()
    {
        return measureStart;
    }

    //--------//
    // getRoi //
    //--------//
    /**
     * @return the builder roi
     */
    public KeyRoi getRoi ()
    {
        return roi;
    }

    //----------//
    // getStaff //
    //----------//
    /**
     * @return the staff
     */
    public Staff getStaff ()
    {
        return staff;
    }

    //---------//
    // process //
    //---------//
    /**
     * Process the potential key signature of the underlying staff in isolation.
     * <p>
     * This builds peaks, slices, alters and checks trailing space and clef(s) compatibility.
     */
    public void process ()
    {
        if (isVip) {
            logger.info("VIP process key for S#{} staff#{}", system.getId(), getId()); // BP here!
        }

        List<Range> hiloPeaks = retrieveHiLoPeaks(); // Retrieve all hilo peaks

        browseArea(hiloPeaks); // Pick up raw peaks before end of key area

        refineAreaStart();

        mergePeaks(); // Merge close peaks

        purgeLightPeaks(); // Discard some light peaks

        int signature = inferSignature(); // Infer signature from peaks

        signature = refineSignature(signature); // Check trailing and regular peaks spacing

        List<Integer> starts = computeStarts(signature); // Compute start for each key item

        if (!starts.isEmpty()) {
            allocateSlices(starts); // Allocate (empty) slices
            extractor.retrieveComponents(keyShape); // Check connected components for alters

            // If some slices are still empty, use hard slice extraction
            List<KeySlice> emptySlices = roi.getEmptySlices();

            if (!emptySlices.isEmpty()) {
                logger.debug("Staff#{} empty key slices: {}", getId(), emptySlices);
                extractEmptySlices(emptySlices);

                // NOTA: Some slices may still be empty at this point...
            }

            // Check compatibility with active clef(s) if any
            clefs.addAll(staff.getCompetingClefs(starts.get(0)));

            if (!clefs.isEmpty()) {
                if (!checkWithClefs()) {
                    logger.debug("Staff#{} no clef-key compatibility", getId());
                    destroy();

                    return;
                }
            }

            // For very short key candidate (1 item), check space right after last item
            KeySlice lastValidSlice = roi.getLastValidSlice();

            if ((lastValidSlice != null) && (lastValidSlice.getId() == 1)) {
                if (!checkTrailingSpace()) {
                    destroy();

                    return;
                }
            }
        }

        selectBestClef(); // In case staff clef has not yet been selected
    }

    //---------------//
    // recordSamples //
    //---------------//
    /**
     * Record glyphs used in key building as training samples.
     *
     * @param recordPositives true to record positive glyphs
     * @param recordNegatives true to retrieve negative glyphs
     */
    public void recordSamples (boolean recordPositives,
                               boolean recordNegatives)
    {
        extractor.recordSamples(recordPositives, recordNegatives, keyShape);
    }

    //--------//
    // shrink //
    //--------//
    /**
     * Remove the last valid slice.
     */
    public void shrink ()
    {
        KeySlice lastSlice = roi.getLastValidSlice();
        roi.remove(lastSlice);
        keyInter.shrink();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "KeyBuilder#" + getId();
    }

    //----------------//
    // allocateSlices //
    //----------------//
    /**
     * Using the starting mark found for each alteration item, defines all roi.
     *
     * @param starts
     */
    private void allocateSlices (List<Integer> starts)
    {
        final int count = starts.size();

        for (int i = 0; i < count; i++) {
            int start = starts.get(i);
            int stop = (i < (count - 1)) ? (starts.get(i + 1) - 1) : range.getStop();
            roi.createSlice(start, stop);
        }
    }

    //------------------//
    // applyPitchImpact //
    //------------------//
    private void applyPitchImpact (ClefKind clefKind)
    {
        final double[] pitchedGrades = new double[roi.size()];
        computePitchedGrades(clefKind, pitchedGrades);

        for (int i = 0; i < roi.size(); i++) {
            KeySlice slice = roi.get(i);
            KeyAlterInter alter = slice.getAlter();

            if (alter != null) {
                alter.setGrade(pitchedGrades[i]);
            }
        }
    }

    //------------//
    // browseArea //
    //------------//
    /**
     * Browse the projection to detect the sequence of peaks (similar to stems) and
     * spaces (blanks).
     * <p>
     * We browse in parallel the spaces and the hilo-based peaks and stop before invalid peak or
     * wide space.
     *
     * @param hiloPeaks sequence of peaks detected via derivative hilos
     */
    private void browseArea (List<Range> hiloPeaks)
    {
        if (hiloPeaks.isEmpty()) {
            attributes.add(Attribute.INITIAL_WIDE_SPACE);

            return;
        }

        Iterator<Range> hiloIter = hiloPeaks.iterator();
        Range hl = hiloIter.next();

        int spaceStart = -1; // Space start abscissa
        int spaceStop = -1; // Space stop abscissa

        for (int x = range.browseStart; x <= range.browseStop; x++) {
            int cumul = projection.getValue(x);

            if (cumul > params.maxSpaceCumul) {
                if (spaceStart != -1) {
                    // End of space
                    if (!checkSpace(spaceStart, spaceStop)) {
                        return; // Too wide space encountered
                    }

                    spaceStart = -1;
                }

                if ((hl != null) && (x >= hl.min)) {
                    // Process this hilo
                    if (!createPeak(
                            hl.min,
                            hl.main,
                            hl.max,
                            projection.getValue(hl.main),
                            projection.getArea(hl.min, hl.max))) {
                        return; // Invalid peak encountered
                    }

                    x = hl.max;

                    // Determine next suitable hilo, if any
                    if (hiloIter.hasNext()) {
                        Range next = hiloIter.next();
                        int gap = next.min - hl.max - 1;

                        if (gap <= params.maxInnerPeakGap) {
                            hl = next;
                        } else {
                            logger.debug("Too large inner peak gap");
                            hl = null;
                        }
                    } else {
                        hl = null;
                    }
                }
            } else {
                // For space
                if (spaceStart == -1) {
                    spaceStart = x; // Start of space
                }

                spaceStop = x; // Extend space
            }
        }

        // Finish ongoing space if any
        if (spaceStart != -1) {
            checkSpace(spaceStart, spaceStop);
        }
    }

    //-----------------//
    // checkPeakDeltas //
    //-----------------//
    /**
     * Check delta abscissa between items start peaks, to detect final abnormal delta.
     *
     * @param signature initial signature value
     * @return final signature value
     */
    private int checkPeakDeltas (int signature)
    {
        final int count = Math.abs(signature); // Item count
        final int ipc = (signature < 0) ? 1 : 2; // Peak count per item
        double totalDx = 0;
        double maxDx = Double.MIN_VALUE;
        final double[] dxs = new double[count - 1];

        for (int ip = ipc; ip < peaks.size(); ip += ipc) {
            KeyPeak peak = peaks.get(ip);
            KeyPeak prevPeak = peaks.get(ip - 1);
            double dx = peak.getCenter() - prevPeak.getCenter();
            totalDx += dx;
            maxDx = Math.max(maxDx, dx);
            dxs[(ip / ipc) - 1] = dx;
        }

        final double wMean = count - 2;
        final double meanDx = (totalDx - maxDx) / wMean;
        final double wMax = 3.0;
        final double max = ((wMean * meanDx) + (wMax * params.maxPeakDx)) / (wMean + wMax);
        logger.debug(
                "Staff#{} peak dxs:{} mean:{} maxPeakDx:{} cutOver:{}",
                getId(),
                dxs,
                meanDx,
                params.maxPeakDx,
                max);

        for (int is = 0; is < dxs.length; is++) {
            final double dx = dxs[is];

            if (dx > max) {
                final int ip = ipc * (is + 1);
                final KeyPeak peak = peaks.get(ip);
                logger.debug("Staff#{} key cut before {}", getId(), peak);
                range.shrinkStop(peak.min - 1);
                peaks.retainAll(peaks.subList(0, ip));

                return (signature < 0) ? (-(is + 1)) : (is + 1);
            }
        }

        return signature;
    }

    //------------//
    // checkSpace //
    //------------//
    /**
     * Check space encountered.
     * <ul>
     * <li>Before key start, a large space (> maxFirstSpaceWidth) indicates lack of key.
     * <li>After key start, a large space (> maxInnerSpace) indicates key end has been reached.
     * </ul>
     *
     * @param spaceStart space start abscissa
     * @param spaceStop  space stop abscissa
     * @return true to keep browsing, false to stop immediately
     */
    private boolean checkSpace (int spaceStart,
                                int spaceStop)
    {
        boolean keepOn = true;
        final int spaceWidth = spaceStop - spaceStart + 1;

        if (!range.hasStart()) {
            // This is the very first space found
            if (spaceWidth > params.maxFirstSpaceWidth) {
                // No key signature!
                logger.debug("Staff#{} no key signature.", getId());
                attributes.add(Attribute.INITIAL_WIDE_SPACE);
                keepOn = false;
            } else {
                // Set range start here, since first chunk may be later skipped if lacking peak
                range.setStart(spaceStop + 1);
            }
        } else if (peaks.isEmpty()) {
            range.setStart(spaceStop + 1);
        } else if (spaceWidth > params.maxInnerSpace) {
            range.shrinkStop(spaceStart - 1);
            keepOn = false;
        }

        return keepOn;
    }

    //--------------------//
    // checkTrailingSpace //
    //--------------------//
    /**
     * Check if the last item in key signature has some trailing space (before any head).
     * <p>
     * TODO: since item pitches have not yet been validated, perhaps we should vertically extend the
     * lookup area (at least within pitch tolerance)?
     *
     * @return true if OK
     */
    private boolean checkTrailingSpace ()
    {
        KeySlice lastValid = roi.getLastValidSlice();

        if (lastValid == null) {
            return false;
        }

        KeyAlterInter alter = lastValid.getAlter();
        Rectangle glyphRect = alter.getBounds();
        double pitch = alter.getMeasuredPitch();
        int x = glyphRect.x + glyphRect.width;
        int y = (int) Math.rint(staff.pitchToOrdinate(x, pitch));
        int interline = scale.getInterline();
        Rectangle rect = new Rectangle(x, y - (interline / 2), interline, interline);

        boolean ok = isRatherEmpty(rect);

        if (!ok) {
            logger.debug("Staff#{} slice#{} no trailing space", getId(), lastValid.getId());
        }

        return ok;
    }

    //----------------//
    // checkWithClefs //
    //----------------//
    /**
     * Compare the sequence of candidate key items with the possible active clefs and
     * make the final clef selection.
     * <p>
     * For each possible clef kind, the sequence of items pitches is imposed (for chosen keyShape).
     * The problem is that item pitch is not fully reliable, especially for a flat item.
     * We thus use a pitch window for each item and modify the item grade based on difference
     * between item measured pitch and the clef-based theoretical pitch.
     * <p>
     * Since there are support relations between items of a key signature, the contextual grade of
     * each item will increase with the number of partnering items. Doing so, we will mechanically
     * more easily accept the delta pitch of an item when it is part of a longer key signature.
     *
     * @return true if a clef compatibility has been found
     */
    private boolean checkWithClefs ()
    {
        final double clefRatio = new ClefKeyRelation().getSourceRatio();
        ClefInter bestCompatibleClef = null; // Best clef (among the compatible ones)
        double bestCompatibleClefCtx = 0; // Contextual grade of best clef

        for (int ic = 0; ic < clefs.size(); ic++) {
            ClefInter clef = clefs.get(ic);

            // Pitches expected for active clef kind and key shape
            final ClefKind clefKind = clef.getKind();
            final double[] pitchedGrades = new double[roi.size()];
            final int alterCount = computePitchedGrades(clefKind, pitchedGrades);

            if (alterCount > 0) {
                // TODO: Check resulting key grade? if too low, give up!!!
                final double keyGrade = computeKeyGrade(alterCount, pitchedGrades);

                if (logger.isDebugEnabled()) {
                    logger.info(dumpOf(clefKind, keyGrade, pitchedGrades));
                }

                // Impact of key on clef
                final double keyContribution = GradeUtil.contributionOf(keyGrade, clefRatio);
                final double clefCtx = GradeUtil.contextual(clef.getGrade(), keyContribution);

                if (clefCtx > bestCompatibleClefCtx) {
                    bestCompatibleClefCtx = clefCtx;
                    bestCompatibleClef = clef;
                }
            }
        }

        ClefInter bestClef = null; // Best clef (compatible of not)

        if (bestCompatibleClef != null) {
            double bestClefGrade = -1;

            for (ClefInter clef : clefs) {
                final double grade = (clef == bestCompatibleClef) ? bestCompatibleClefCtx
                        : clef.getGrade();

                if (grade > bestClefGrade) {
                    bestClefGrade = grade;
                    bestClef = clef;
                }
            }

            // Keep only the best clef
            for (ClefInter clef : clefs) {
                if (clef != bestClef) {
                    clef.delete();
                }
            }

            clefs.retainAll(Arrays.asList(bestClef));

            if (bestClef == bestCompatibleClef) {
                // Try to fill missing alters if any
                fillMissingAlters(bestClef);

                // Create keyInter instance, after alters are really applied their pitch impact
                applyPitchImpact(bestClef.getKind());
                createKeyInter(); // -> keyInter
                sig.addEdge(bestClef, keyInter, new ClefKeyRelation());
                sig.computeContextualGrade(keyInter);
            }
        }

        if ((bestCompatibleClef == null) || (bestCompatibleClef != bestClef)) {
            roi.stuffSlicesFrom(0);

            return false;
        } else {
            return true;
        }
    }

    //-----------------//
    // computeKeyGrade //
    //-----------------//
    private double computeKeyGrade (int alterCount,
                                    double[] pitchedGrades)
    {
        final double relRatio = new KeyAltersRelation().getSourceRatio();

        // Contribution brought by each item
        double[] contribs = new double[roi.size()];

        for (int i = 0; i < roi.size(); i++) {
            contribs[i] = GradeUtil.contributionOf(pitchedGrades[i], relRatio);
        }

        // Compute resulting key grade (as average of items contextual grades)
        double keyGrade = 0;

        for (int i = 0; i < pitchedGrades.length; i++) {
            double contribution = 0;

            for (int p = 0; p < contribs.length; p++) {
                if (p != i) {
                    contribution += contribs[p];
                }
            }

            keyGrade += GradeUtil.contextual(pitchedGrades[i], contribution);
        }

        keyGrade /= alterCount;

        return keyGrade;
    }

    //----------------------//
    // computePitchedGrades //
    //----------------------//
    /**
     * Compute the grade of each key alter, applying delta pitch impact WRT clef kind.
     * <p>
     * Threshold for delta pitch grows linearly between 1 & 4 items, and is constant for 4+ items.
     *
     * @param clefKind active clef kind
     * @param alters   (output) array to be populated by each alter final grade
     * @return number of alters found
     */
    private int computePitchedGrades (ClefKind clefKind,
                                      double[] alters)
    {
        final int n = alters.length;

        if (n == 0) {
            return 0;
        }

        // Define dPitch threshold based on alters.length
        final double maxDeltaPitch = (n >= 4) ? params.maxDeltaPitch_4
                : (params.maxDeltaPitch_1
                   + (((params.maxDeltaPitch_4 - params.maxDeltaPitch_1) * (n - 1)) / 3));

        final int[] clefPitches = KeyInter.getPitches(clefKind, keyShape);
        int alterCount = 0;

        for (int i = 0; i < roi.size(); i++) {
            KeySlice slice = roi.get(i);
            KeyAlterInter alter = slice.getAlter();

            if (alter != null) {
                alterCount++;

                double alterPitch = alter.getMeasuredPitch();
                double dPitch = Math.abs(alterPitch - clefPitches[i]);

                // Check single difference
                if (dPitch > maxDeltaPitch) {
                    logger.debug(
                            "Staff#{} slice#{} invalid {} pitch {} vs {} for {}",
                            getId(),
                            slice.getId(),
                            keyShape,
                            String.format("%.1f", alterPitch),
                            clefPitches[i],
                            clefKind);

                    return 0;
                } else {
                    // Apply dPitch impact on alter grade
                    alters[i] = alter.getGrade() * (1 - (dPitch / maxDeltaPitch));
                }
            } else {
                alters[i] = 0;
            }
        }

        return alterCount;
    }

    //---------------//
    // computeStarts //
    //---------------//
    /**
     * Compute the theoretical starting abscissa for each key signature item.
     */
    private List<Integer> computeStarts (int signature)
    {
        List<Integer> starts = new ArrayList<Integer>();

        if (signature > 0) {
            // Sharps
            starts.add(range.getStart());

            for (int i = 2; i < peaks.size(); i += 2) {
                KeyPeak peak = peaks.get(i);
                starts.add((int) Math.ceil(0.5 * (peak.min + peaks.get(i - 1).max)));
            }

            // End of area
            refineAreaStop(getLastPeak(), params.sharpTrail, params.maxSharpTrail);
        } else if (signature < 0) {
            // Flats
            KeyPeak firstPeak = peaks.get(0);

            // Start of area, make sure there is nothing right before first peak
            int flatHeading = ((firstPeak.min + firstPeak.max) / 2) - range.getStart();

            if (flatHeading <= params.maxFlatHeading) {
                starts.add(range.getStart());

                for (int i = 1; i < peaks.size(); i++) {
                    KeyPeak peak = peaks.get(i);
                    starts.add(peak.min);
                }

                // End of area
                refineAreaStop(getLastPeak(), params.flatTrail, params.maxFlatTrail);
            } else {
                logger.debug("Too large heading {} before first flat peak", flatHeading);
            }
        }

        return starts;
    }

    //----------------//
    // createKeyInter //
    //----------------//
    private void createKeyInter ()
    {
        List<KeyAlterInter> alters = new ArrayList<KeyAlterInter>();
        Rectangle box = null;

        for (KeySlice slice : roi) {
            KeyAlterInter alter = slice.getAlter();

            if (alter != null) {
                alters.add(alter);

                if (box == null) {
                    box = alter.getBounds();
                } else {
                    box.add(alter.getBounds());
                }
            }
        }

        // Grade: all alters in a key signature support each other
        for (int i = 0; i < alters.size(); i++) {
            KeyAlterInter alter = alters.get(i);

            for (KeyAlterInter sibling : alters.subList(i + 1, alters.size())) {
                sig.addEdge(alter, sibling, new KeyAltersRelation());
            }
        }

        double grade = 0;

        for (KeyAlterInter alter : alters) {
            grade += sig.computeContextualGrade(alter);
        }

        grade /= alters.size();

        keyInter = new KeyInter(box, grade, getFifths(), alters);
        keyInter.setStaff(staff);
        sig.addVertex(keyInter);

        // Postpone staff header assignment until key is finalized...
    }

    //------------//
    // createPeak //
    //------------//
    /**
     * (Try to) create a peak for a candidate alteration item.
     * <p>
     * Peak is checked for its height and width, its "stem-like" shape, delta abscissa with previous
     * peak, abscissa offset for first peak.
     *
     * @param start  start abscissa
     * @param main   main abscissa
     * @param stop   stop abscissa
     * @param height peak height
     * @param area   peak area
     * @return whether browsing can keep on
     */
    private boolean createPeak (int start,
                                int main,
                                int stop,
                                int height,
                                int area)
    {
        final KeyPeak peak = new KeyPeak(start, main, stop, height, area);
        boolean invalid = false;

        // Check whether this peak could be part of sig, otherwise give up
        if ((height > params.maxPeakCumul) || (peak.getWidth() > params.maxPeakWidth)) {
            logger.debug("Invalid height or width for peak");
            invalid = true;
            range.shrinkStop(peak.min - 1);
        } else {
            // Does this peak correspond to a stem-shaped item? if not, simply ignore it
            if (!isStemLike(peak)) {
                return true;
            }

            // We may have an interesting peak, check distance since previous peak
            KeyPeak lastPeak = getLastPeak();

            if (lastPeak != null) {
                // Check delta abscissa
                double x = (start + stop) / 2.0;
                double dx = x - ((lastPeak.min + lastPeak.max) / 2.0);

                if (dx > params.maxPeakDx) {
                    // A large dx indicates we are beyond end of key signature
                    logger.debug("Too large delta since previous peak");
                    invalid = true;
                    range.shrinkStop(peak.min - 1);
                }
            } else {
                // Very first peak, check offset from theoretical start
                // TODO: this is too strict, check "emptyness" in previous abscissae?
                int offset = start - range.browseStart;

                if (offset > params.maxFirstPeakOffset) {
                    logger.debug("First peak arrives too late");
                    invalid = true;
                    range.shrinkStop(peak.min - 1);
                } else if (!range.hasStart()) {
                    // No space was found before peak, set range.start at beginning of browsing
                    range.setStart(range.browseStart);
                }
            }
        }

        if (invalid) {
            return false;
        } else {
            peaks.add(peak);

            return true;
        }
    }

    //--------//
    // dumpOf //
    //--------//
    private String dumpOf (ClefKind clefKind,
                           double keyGrade,
                           double[] pitchedGrades)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Staff#%-2d %6s", getId(), clefKind));

        for (int i = 0; i < pitchedGrades.length; i++) {
            sb.append(String.format(" %.2f", pitchedGrades[i]));
        }

        sb.append(String.format(" key:%.3f", keyGrade));

        return sb.toString();
    }

    //--------------------//
    // extractEmptySlices //
    //--------------------//
    /**
     * Using the starting mark found for each alteration item, extract each vertical
     * slice and build alteration inter out of each slice.
     *
     * @param emptySlices sequence of empty slices
     */
    private void extractEmptySlices (List<KeySlice> emptySlices)
    {
        for (KeySlice slice : emptySlices) {
            extractor.extractAlter(
                    slice,
                    Collections.singleton(keyShape),
                    Grades.keyAlterMinGrade2,
                    true);
        }
    }

    //-------------------//
    // fillMissingAlters //
    //-------------------//
    /**
     * (Try to) fill slices with missing alters, under known clef.
     * <p>
     * If a slice has no valid alter, use its target pitch to crop pixels using a pitch window.
     *
     * @param clef chosen active clef
     */
    private void fillMissingAlters (ClefInter clef)
    {
        final Set<Shape> shapes = Collections.singleton(keyShape);
        final double[] pitchedGrades = new double[roi.size()];
        computePitchedGrades(clef.getKind(), pitchedGrades);

        for (int i = 0; i < roi.size(); i++) {
            KeySlice slice = roi.get(i);
            KeyAlterInter alter = slice.getAlter();

            if ((alter == null) || (pitchedGrades[i] < Grades.keyAlterMinGrade1)) {
                // Adjust slice rectangle, using theoretical pitch
                slice.setPitchRect(clef, keyShape, params.typicalGlyphHeight);
                extractor.extractAlter(slice, shapes, Grades.keyAlterMinGrade2, true);
            }
        }
    }

    //---------------//
    // getBrowseRect //
    //---------------//
    /**
     * Define the rectangular area to be browsed.
     * <p>
     * The lookup area must embrace all possible key signatures, whatever the staff clef, so it goes
     * from first line to last line of staff, augmented of 2 interline value above and 1 interline
     * value below.
     *
     * @return the rectangular area to be browsed
     */
    private Rectangle getBrowseRect ()
    {
        final int xMin = Math.max(0, measureStart - params.preStaffMargin);
        final int xMax = range.browseStop;

        int yMin = Integer.MAX_VALUE;
        int yMax = Integer.MIN_VALUE;

        for (int x = xMin; x <= xMax; x++) {
            yMin = Math.min(yMin, staff.getFirstLine().yAt(xMin) - (2 * scale.getInterline()));
            yMax = Math.max(yMax, staff.getLastLine().yAt(xMin) + (1 * scale.getInterline()));
        }

        return new Rectangle(xMin, yMin, xMax - xMin + 1, yMax - yMin + 1);
    }

    //---------------//
    // getBrowseStop //
    //---------------//
    /**
     * Determine the abscissa where to stop projection analysis.
     * <p>
     * The analysis range is typically [browseStart .. measureStart+globalWidth] but may end
     * earlier if a (good) bar line is encountered.
     *
     * @param globalWidth  theoretical projection length
     * @param measureStart abscissa at measure start
     * @param browseStart  abscissa at browse start (just after clef)
     * @return the end abscissa
     */
    private int getBrowseStop (int globalWidth,
                               int measureStart,
                               int browseStart)
    {
        int end = measureStart + globalWidth;

        for (BarlineInter bar : staff.getBars()) {
            if (!bar.isGood()) {
                continue;
            }

            int barStart = bar.getBounds().x;

            if ((barStart > browseStart) && (barStart <= end)) {
                logger.debug("Staff#{} stopping key search before {}", getId(), bar);
                end = barStart - 1;

                break;
            }
        }

        return end;
    }

    //-----------//
    // getFifths //
    //-----------//
    /**
     * Staff key signature is dynamically computed using the keyShape and the count of
     * alteration roi.
     *
     * @return the signature as an integer value
     */
    private int getFifths ()
    {
        if (roi.isEmpty() || (keyShape == null)) {
            return 0;
        }

        switch (keyShape) {
        case SHARP:
            return roi.size();

        case FLAT:
            return -roi.size();

        default:
            return 0;
        }
    }

    //-------------//
    // getLastPeak //
    //-------------//
    /**
     * Report the last (valid) peak found.
     *
     * @return the last (valid) peak, if any, or null
     */
    private KeyPeak getLastPeak ()
    {
        if (peaks.isEmpty()) {
            return null;
        }

        return peaks.get(peaks.size() - 1);
    }

    //--------------//
    // getLocalPeak //
    //--------------//
    /**
     * Try to find a local peak within the provided range
     *
     * @param start range start
     * @param stop  range stop
     * @return peak abscissa, or null
     */
    private Integer getLocalPeak (int start,
                                  int stop)
    {
        int bestCumul = -1;
        Integer bestX = null;

        for (int x = start; x <= stop; x++) {
            int cumul = projection.getValue(x);

            if (bestCumul < cumul) {
                bestCumul = cumul;
                bestX = x;
            }
        }

        return bestX;
    }

    //---------------//
    // getProjection //
    //---------------//
    /**
     * Cumulate the foreground pixels for each abscissa value in the lookup area.
     *
     * @param rect the lookup rectangle
     * @return the populated cumulation table
     */
    private IntegerFunction getProjection (Rectangle rect)
    {
        final int xMin = rect.x;
        final int xMax = (rect.x + rect.width) - 1;
        final int yMin = rect.y;
        final int yMax = (rect.y + rect.height) - 1;
        final IntegerFunction table = new IntegerFunction(xMin, xMax);

        for (int x = xMin; x <= xMax; x++) {
            short cumul = 0;

            for (int y = yMin; y <= yMax; y++) {
                if (staffFreeSource.get(x, y) == 0) {
                    cumul++;
                }
            }

            table.setValue(x, cumul);
        }

        return table;
    }

    //---------------//
    // getSmallestDx //
    //---------------//
    private double getSmallestDx (KeyPeak peak,
                                  Collection<KeyPeak> col)
    {
        double smallest = Double.MAX_VALUE;

        for (KeyPeak p : col) {
            if (p != peak) {
                smallest = Math.min(smallest, Math.abs(p.getCenter() - peak.getCenter()));
            }
        }

        return smallest;
    }

    //---------//
    // hasStem //
    //---------//
    /**
     * Report whether the provided rectangular peak area contains a vertical portion
     * of 'coreLength' with a black ratio of at least 'minBlackRatio'.
     * <p>
     * A row is considered as black if it contains at least one black pixel.
     *
     * @param area          the vertical very narrow rectangle of interest
     * @param source        the pixel source
     * @param coreLength    minimum "stem" length
     * @param minBlackRatio minimum ratio of black rows in "stem" length
     * @return true if a "stem" is found
     */
    private boolean hasStem (Rectangle area,
                             ByteProcessor source,
                             int coreLength,
                             double minBlackRatio)
    {
        // Process all rows
        final boolean[] blacks = new boolean[area.height];
        Arrays.fill(blacks, false);

        for (int y = 0; y < area.height; y++) {
            for (int x = 0; x < area.width; x++) {
                if (source.get(area.x + x, area.y + y) == 0) {
                    blacks[y] = true;

                    break;
                }
            }
        }

        // Build a sliding window, of length coreLength
        final int quorum = (int) Math.rint(coreLength * minBlackRatio);
        int count = 0;

        for (int y = 0; y < coreLength; y++) {
            if (blacks[y]) {
                count++;
            }
        }

        if (count >= quorum) {
            return true;
        }

        // Move the window downward
        for (int y = 1, yMax = area.height - coreLength; y <= yMax; y++) {
            if (blacks[y - 1]) {
                count--;
            }

            if (blacks[y + (coreLength - 1)]) {
                count++;
            }

            if (count >= quorum) {
                return true;
            }
        }

        return false;
    }

    //----------------//
    // inferSignature //
    //----------------//
    /**
     * Infer a raw signature value, based on peaks configuration.
     * <p>
     * This is based on the average value of peaks intervals, computed on the short ones (the
     * intervals shorter or equal to the mean value).
     *
     * @return -flats, 0 or +sharps
     */
    private int inferSignature ()
    {
        final int peakCount = peaks.size(); // Number of peaks

        if (peakCount == 0) {
            logger.debug("no peak");

            return 0;
        }

        final int offset = peaks.get(0).min - range.getStart(); // Initial abscissa offset
        final KeyPeak lastPeak = getLastPeak();

        if (peakCount > 1) {
            // Compute mean value of short intervals
            double meanDx = (lastPeak.getCenter() - peaks.get(0).getCenter()) / (peakCount - 1);
            int shorts = 0;
            double sum = 0;

            for (int i = 1; i < peakCount; i++) {
                double interval = peaks.get(i).getCenter() - peaks.get(i - 1).getCenter();

                if (interval <= meanDx) {
                    shorts++;
                    sum += interval;
                }
            }

            final double meanShort = sum / shorts;

            if (meanShort < params.minFlatDelta) {
                keyShape = Shape.SHARP;
            } else if (meanShort > params.maxSharpDelta) {
                keyShape = Shape.FLAT;
            } else {
                keyShape = (offset >= params.offsetThreshold) ? Shape.SHARP : Shape.FLAT;
            }

            // For sharps, peakCount must be an even number
            if (keyShape == Shape.SHARP) {
                if ((peakCount % 2) != 0) {
                    // Discard last peak (TODO: why the last?)
                    range.shrinkStop(lastPeak.min - 1);
                    peaks.retainAll(peaks.subList(0, peakCount - 1));

                    return (peakCount - 1) / 2;
                }

                return peakCount / 2;
            } else {
                return -peakCount;
            }
        } else if (offset <= params.offsetThreshold) {
            // Acceptable flat
            keyShape = Shape.FLAT;

            return -1;
        } else {
            // Non acceptable stuff, so discard this single peak!
            range.shrinkStop(lastPeak.min - 1);
            peaks.clear();

            return 0;
        }
    }

    //---------------//
    // isRatherEmpty //
    //---------------//
    /**
     * Check whether the provided rectangle is free of note head.
     *
     * @param rect the lookup area
     * @return true if rather empty
     */
    private boolean isRatherEmpty (Rectangle rect)
    {
        final int xMin = rect.x;
        final int xMax = (rect.x + rect.width) - 1;
        final int yMin = rect.y;
        final int yMax = (rect.y + rect.height) - 1;
        final int maxCumul = params.maxTrailingCumul;
        final int minWidth = params.minTrailingSpace;

        int spaceStart = -1;

        for (int x = xMin; x <= xMax; x++) {
            int cumul = 0;

            for (int y = yMin; y <= yMax; y++) {
                if (staffFreeSource.get(x, y) == 0) {
                    cumul++;
                }
            }

            if (cumul <= maxCumul) {
                if (spaceStart == -1) {
                    spaceStart = x;
                } else if ((x - spaceStart + 1) >= minWidth) {
                    return true;
                }
            } else {
                spaceStart = -1;
            }
        }

        return false;
    }

    //------------//
    // isStemLike //
    //------------//
    /**
     * Check whether the provided peak of cumulated pixels corresponds to a "stem".
     * <p>
     * We define a lookup rectangle using peak abscissa range.
     * The rectangle is searched for pixels that could make a "stem".
     *
     * @param peak the peak to check
     * @return true if OK
     */
    private boolean isStemLike (KeyPeak peak)
    {
        final Rectangle rect = new Rectangle(peak.min, roi.y, peak.getWidth(), roi.height);

        if (peak.getWidth() <= 2) {
            rect.grow(1, 0); // Slight margin on left & right of peak
        }

        boolean stem = hasStem(rect, staffFreeSource, params.coreStemLength, params.minBlackRatio);

        if (!stem) {
            logger.debug("Staff#{} {} no stem", getId(), peak);
        }

        return stem;
    }

    //------------//
    // mergePeaks //
    //------------//
    private void mergePeaks ()
    {
        for (int i = 1; i < peaks.size(); i++) {
            KeyPeak prevPeak = peaks.get(i - 1);
            KeyPeak peak = peaks.get(i);

            if ((peak.min - prevPeak.max) <= 1) {
                KeyPeak merged = new KeyPeak(
                        prevPeak.min,
                        projection.argMax(prevPeak.min, peak.max),
                        peak.max,
                        Math.max(prevPeak.height, peak.height),
                        projection.getArea(prevPeak.min, peak.max));
                peaks.set(i - 1, merged);
                peaks.remove(i);
                i--;
            }
        }
    }

    //-----------------//
    // purgeLightPeaks //
    //-----------------//
    /**
     * Discard the peaks that are too light compared with the others and have a close
     * neighbor.
     * <p>
     * TODO: Perhaps a better approach could be based on regular pattern of peaks. But how?
     */
    private void purgeLightPeaks ()
    {
        final int peakCount = peaks.size(); // Number of peaks

        if (peakCount <= 1) {
            return;
        }

        // Compute area quorum on all peaks but the smallest one
        List<KeyPeak> sortedPeaks = new ArrayList<KeyPeak>(peaks);
        Collections.sort(sortedPeaks, KeyPeak.byArea);

        int totalArea = 0; // Sum of peaks areas

        for (KeyPeak peak : sortedPeaks.subList(1, peakCount)) {
            totalArea += peak.area;
        }

        final double quorum = (params.peakAreaQuorum * totalArea) / (peakCount - 1);

        // Discard any peak which doesn't reach area quorum
        // and is abscissa-wise close to another peak
        for (Iterator<KeyPeak> it = sortedPeaks.iterator(); it.hasNext();) {
            final KeyPeak peak = it.next();

            if (peak.area >= quorum) {
                break; // End of "light" peaks
            }

            // Check distance to closest peak
            final double dx = getSmallestDx(peak, sortedPeaks);

            if (dx < params.minLightPeakDx) {
                logger.debug("Staff#{} light {}", getId(), peak);
                it.remove();
            }
        }

        peaks.retainAll(sortedPeaks);
    }

    //-----------------//
    // refineAreaStart //
    //-----------------//
    /**
     * Refine key area start, if needed, by using projection from 1 interline above top
     * line down to staff bottom line.
     */
    private void refineAreaStart ()
    {
        if (peaks.isEmpty()) {
            return;
        }

        final int xMin = range.getStart();
        final int xMax = peaks.get(0).min - 1;
        final int yMin = staff.getFirstLine().yAt(xMin) - scale.getInterline();
        final int yMax = staff.getLastLine().yAt(xMin);
        final Rectangle rect = new Rectangle(xMin, yMin, xMax - xMin + 1, yMax - yMin + 1);
        final IntegerFunction staffProj = getProjection(rect);
        int start = xMax + 1;

        for (int x = xMin; x <= xMax; x++) {
            if (staffProj.getValue(x) > params.maxSpaceCumul) {
                start = x;

                break;
            }
        }

        range.setStart(start);
    }

    //----------------//
    // refineAreaStop //
    //----------------//
    /**
     * Adjust the stop abscissa of key sig.
     *
     * @param lastGoodPeak last valid peak found
     * @param typicalTrail typical length after last peak (this depends on alter shape)
     * @param maxTrail     maximum length after last peak
     */
    private void refineAreaStop (KeyPeak lastGoodPeak,
                                 int typicalTrail,
                                 int maxTrail)
    {
        final int xMin = (lastGoodPeak.min + typicalTrail) - 1;
        final int xMax = Math.min(projection.getXMax(), lastGoodPeak.min + maxTrail);

        int minCount = Integer.MAX_VALUE;
        Integer newStop = null;

        for (int x = xMin; x <= xMax; x++) {
            int count = projection.getValue(x);

            if (count < minCount) {
                newStop = x - 1;
                minCount = count;
            }
        }

        if (newStop != null) {
            range.shrinkStop(newStop);
        }
    }

    //----------------//
    // refineSignature //
    //----------------//
    /**
     * Additional tests on key signature, which may get adjusted.
     * <ul>
     * <li>Check if there is enough ink after last peak (depending on key shape).
     * <li>Check if items are regularly spaced.
     * <li>Check if the first invalid peak (if any) is sufficiently far from last good peak so
     * that there is enough room for key trailing space.
     * </ul>
     *
     * @return the signature value, perhaps modified
     */
    private int refineSignature (int signature)
    {
        if (signature == 0) {
            return 0;
        }

        KeyPeak lastPeak = getLastPeak();

        // Where is the precise end of key signature?
        int trail = range.getStop() - lastPeak.min + 1;

        // Check trailing length
        if (signature < 0) {
            if (trail < params.minFlatTrail) {
                logger.debug("Removing too narrow flat");
                peaks.retainAll(peaks.subList(0, peaks.indexOf(lastPeak)));
                range.shrinkStop(lastPeak.min - 1);
                signature += 1;
            }
        } else if (trail < params.minSharpTrail) {
            logger.debug("Removing too narrow sharp");
            // Remove the last 2 peaks
            peaks.retainAll(peaks.subList(0, peaks.indexOf(lastPeak)));
            lastPeak = getLastPeak();
            peaks.retainAll(peaks.subList(0, peaks.indexOf(lastPeak)));
            range.shrinkStop(lastPeak.min - 1);
            signature -= 1;
        }

        if (Math.abs(signature) > 2) {
            // Regulary spaced items?
            signature = checkPeakDeltas(signature);
        }

        return signature;
    }

    //-------------------//
    // retrieveHiLoPeaks //
    //-------------------//
    private List<Range> retrieveHiLoPeaks ()
    {
        final List<Range> hiloPeaks = peakFinder.findPeaks(
                params.maxSpaceCumul + 1, // minValue
                params.minPeakValue, // minTopValue
                params.minPeakDerivative, // minDerivative
                params.minGainRatio); // minGainRatio
        Collections.sort(hiloPeaks, Range.byMain);
        logger.debug("Staff#{} hilos: {}", getId(), hiloPeaks);

        return hiloPeaks;
    }

    //----------------//
    // selectBestClef //
    //----------------//
    /**
     * If best clef has not yet been selected, let's do it now.
     */
    private void selectBestClef ()
    {
        if (clefs.size() != 1) {
            clefs.clear();
            clefs.addAll(staff.getCompetingClefs(range.getStop()));

            for (Inter clef : clefs) {
                sig.computeContextualGrade(clef);
            }

            Collections.sort(clefs, Inter.byReverseBestGrade);

            if (!clefs.isEmpty()) {
                ClefInter bestClef = clefs.get(0);

                for (ClefInter clef : clefs) {
                    if (clef != bestClef) {
                        clef.delete();
                    }
                }

                clefs.retainAll(Arrays.asList(bestClef));
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.String vipStaves = new Constant.String(
                "",
                "(Debug) Comma-separated values of VIP staff IDs");

        private final Scale.LineFraction maxSpaceCumul = new Scale.LineFraction(
                2.0,
                "Maximum cumul value in space (specified WRT staff line thickness)");

        private final Scale.Fraction coreStemLength = new Scale.Fraction(
                2.0,
                "Core length for alteration \"stem\" (flat or sharp)");

        private final Constant.Ratio minBlackRatio = new Constant.Ratio(
                0.75,
                "Minimum ratio of black rows in core length");

        private final Scale.Fraction typicalGlyphHeight = new Scale.Fraction(
                2.5,
                "Typical alteration height (flat or sharp)");

        private final Constant.Ratio peakHeightRatio = new Constant.Ratio(
                0.2, //0.4,
                "Ratio of height to detect peaks");

        private final Constant.Ratio peakAreaQuorum = new Constant.Ratio(
                0.5,
                "Quorum ratio of peak area WRT peaks mean area");

        private final Scale.Fraction preStaffMargin = new Scale.Fraction(
                2.0,
                "Horizontal margin before staff left (for plot display)");

        private final Scale.Fraction maxFirstPeakOffset = new Scale.Fraction(
                2.0,
                "Maximum x offset of first peak (WRT browse start)");

        private final Scale.Fraction maxPeakCumul = new Scale.Fraction(
                4.0,
                "Maximum cumul value to accept peak (absolute value)");

        private final Scale.Fraction minPeakDerivative = new Scale.Fraction(
                0.4, //0.2,
                "Minimum derivative for peak detection");

        private final Constant.Ratio minGainRatio = new Constant.Ratio(
                0.2,
                "Minimum gain ratio when extending peaks");

        private final Scale.Fraction maxPeakWidth = new Scale.Fraction(
                0.45,
                "Maximum width to accept peak (measured at threshold height)");

        private final Scale.Fraction maxFlatHeading = new Scale.Fraction(
                0.4,
                "Maximum heading length before peak for a flat item");

        private final Scale.Fraction flatTrail = new Scale.Fraction(
                1.0,
                "Typical trailing length after peak for a flat item");

        private final Scale.Fraction minFlatTrail = new Scale.Fraction(
                0.7,
                "Minimum trailing length after peak for a flat item");

        private final Scale.Fraction maxFlatTrail = new Scale.Fraction(
                1.3,
                "Maximum trailing length after peak for a flat item");

        private final Scale.Fraction sharpTrail = new Scale.Fraction(
                0.3,
                "Typical trailing length after last peak for a sharp item");

        private final Scale.Fraction minSharpTrail = new Scale.Fraction(
                0.2,
                "Minimum trailing length after last peak for a sharp item");

        private final Scale.Fraction maxSharpTrail = new Scale.Fraction(
                0.5,
                "Maximum trailing length after last peak for a sharp item");

        private final Scale.Fraction minLightPeakDx = new Scale.Fraction(
                0.35, // 0.4 should be OK
                "Minimum delta abscissa with closest peak to keep a light peak");

        private final Scale.Fraction minPeakDx = new Scale.Fraction(
                0.3,
                "Minimum delta abscissa between peaks");

        private final Scale.Fraction maxPeakDx = new Scale.Fraction(
                1.5, // 1.4,
                "Maximum delta abscissa between peaks");

        private final Scale.Fraction maxSharpDelta = new Scale.Fraction(
                0.75,
                "Maximum short peak delta for sharps");

        private final Scale.Fraction minFlatDelta = new Scale.Fraction(
                0.45,
                "Minimum short peak delta for flats");

        private final Scale.Fraction offsetThreshold = new Scale.Fraction(
                0.1,
                "Threshold on first peak offset that differentiates flat & sharp");

        private final Constant.Double maxDeltaPitch_1 = new Constant.Double(
                "pitch",
                0.5,
                "Maximum adjustment in pitch for 1 item");

        private final Constant.Double maxDeltaPitch_4 = new Constant.Double(
                "pitch",
                2.0,
                "Maximum adjustment in pitch for 4+ items");

        private final Scale.Fraction maxTrailingCumul = new Scale.Fraction(
                0.25,
                "Maximum cumul threshold in trailing area");

        private final Scale.Fraction minTrailingSpace = new Scale.Fraction(
                0.25,
                "Minimum space after last key item (before potential note head)");

        private final Scale.Fraction maxSliceDeltaX = new Scale.Fraction(
                0.15,
                "Maximum abscissa delta to replicate slice");

        private final Scale.Fraction maxSliceDeltaWidth = new Scale.Fraction(
                0.15,
                "Maximum width delta to replicate slice");

        // Beware: A too small value might miss the whole key signature
        private final Scale.Fraction maxFirstSpaceWidth = new Scale.Fraction(
                1.75,
                "Maximum initial space before key signature");

        // Beware: A too small value might miss final key signature items
        private final Scale.Fraction maxInnerSpace = new Scale.Fraction(
                0.7,
                "Maximum inner space within key signature");

        // Beware: A too small value might miss final key signature items
        private final Scale.Fraction maxInnerPeakGap = new Scale.Fraction(
                1.25,
                "Maximum inner peak gap within key signature");
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final int preStaffMargin;

        final int maxFirstPeakOffset;

        final int maxFirstSpaceWidth;

        final int maxInnerSpace;

        final int maxInnerPeakGap;

        final double minGainRatio;

        final int minPeakValue;

        final int minPeakDerivative;

        final int maxSpaceCumul;

        final int coreStemLength;

        final double minBlackRatio;

        final int maxPeakCumul;

        final int maxPeakWidth;

        final double peakAreaQuorum;

        final int maxFlatHeading;

        final int flatTrail;

        final int minFlatTrail;

        final int maxFlatTrail;

        final int sharpTrail;

        final int minSharpTrail;

        final int maxSharpTrail;

        final int maxPeakDx;

        final int minPeakDx;

        final double minLightPeakDx;

        final double maxSharpDelta;

        final double minFlatDelta;

        final int offsetThreshold;

        final double typicalGlyphHeight;

        final int minTrailingSpace;

        final int maxTrailingCumul;

        final double maxDeltaPitch_1;

        final double maxDeltaPitch_4;

        final int maxSliceDeltaX;

        final int maxSliceDeltaWidth;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            preStaffMargin = scale.toPixels(constants.preStaffMargin);
            maxFirstPeakOffset = scale.toPixels(constants.maxFirstPeakOffset);
            maxFirstSpaceWidth = scale.toPixels(constants.maxFirstSpaceWidth);
            maxInnerSpace = scale.toPixels(constants.maxInnerSpace);
            maxInnerPeakGap = scale.toPixels(constants.maxInnerPeakGap);
            maxSpaceCumul = scale.toPixels(constants.maxSpaceCumul);
            coreStemLength = scale.toPixels(constants.coreStemLength);
            minBlackRatio = constants.minBlackRatio.getValue();
            maxPeakCumul = scale.toPixels(constants.maxPeakCumul);
            maxPeakWidth = scale.toPixels(constants.maxPeakWidth);
            maxFlatHeading = scale.toPixels(constants.maxFlatHeading);
            flatTrail = scale.toPixels(constants.flatTrail);
            minFlatTrail = scale.toPixels(constants.minFlatTrail);
            maxFlatTrail = scale.toPixels(constants.maxFlatTrail);
            sharpTrail = scale.toPixels(constants.sharpTrail);
            minSharpTrail = scale.toPixels(constants.minSharpTrail);
            maxSharpTrail = scale.toPixels(constants.maxSharpTrail);
            maxPeakDx = scale.toPixels(constants.maxPeakDx);
            minPeakDx = scale.toPixels(constants.minPeakDx);
            minLightPeakDx = scale.toPixelsDouble(constants.minLightPeakDx);
            peakAreaQuorum = constants.peakAreaQuorum.getValue();
            maxSharpDelta = scale.toPixelsDouble(constants.maxSharpDelta);
            minFlatDelta = scale.toPixelsDouble(constants.minFlatDelta);
            offsetThreshold = scale.toPixels(constants.offsetThreshold);
            typicalGlyphHeight = scale.toPixelsDouble(constants.typicalGlyphHeight);
            maxTrailingCumul = scale.toPixels(constants.maxTrailingCumul);
            minTrailingSpace = scale.toPixels(constants.minTrailingSpace);
            maxDeltaPitch_1 = constants.maxDeltaPitch_1.getValue();
            maxDeltaPitch_4 = constants.maxDeltaPitch_4.getValue();
            minPeakDerivative = scale.toPixels(constants.minPeakDerivative);
            minGainRatio = constants.minGainRatio.getValue();
            maxSliceDeltaX = scale.toPixels(constants.maxSliceDeltaX);
            maxSliceDeltaWidth = scale.toPixels(constants.maxSliceDeltaWidth);

            // Maximum alteration contribution (on top of staff lines)
            int whiteSpace = scale.getInterline() - scale.getMainFore();
            double maxAlterContrib = constants.typicalGlyphHeight.getValue() * whiteSpace;
            minPeakValue = (int) Math.rint(
                    (5 * scale.getMainFore())
                    + (constants.peakHeightRatio.getValue() * maxAlterContrib));
        }
    }
}
