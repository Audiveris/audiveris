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

import omr.classifier.Classifier;
import omr.classifier.Evaluation;
import omr.classifier.GlyphClassifier;
import omr.classifier.SampleRepository;
import omr.classifier.SampleSheet;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphCluster;
import omr.glyph.GlyphCluster.AbstractAdapter;
import omr.glyph.GlyphFactory;
import omr.glyph.GlyphIndex;
import omr.glyph.GlyphLink;
import omr.glyph.Glyphs;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.Symbol.Group;

import omr.math.GeoUtil;
import omr.math.Projection;
import static omr.run.Orientation.VERTICAL;
import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.sheet.Book;
import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.header.HeaderBuilder.Plotter;

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

import omr.util.Navigable;

import ij.process.ByteProcessor;

import org.jfree.data.xy.XYSeries;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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

    /** Shape classifier to use. */
    private final Classifier classifier = GlyphClassifier.getInstance();

    /** Staff-free pixel source. */
    private final ByteProcessor staffFreeSource;

    /** Precise beginning abscissa of measure. */
    private final int measureStart;

    /** (Competing) active clef(s) in staff, just before key signature. */
    private final List<ClefInter> clefs = new ArrayList<ClefInter>();

    /** ROI with slices for key search. */
    private final KeyRoi roi;

    /** Projection of foreground pixels, indexed by abscissa. */
    private final Projection projection;

    /** Sequence of peaks found.
     * It may end with a sequence of invalid peaks, which are kept to indicate pixels presence */
    private final List<KeyEvent.Peak> peaks = new ArrayList<KeyEvent.Peak>();

    /** Sequence of spaces and peaks. (for debugging only) */
    private final List<KeyEvent> events = new ArrayList<KeyEvent>();

    /** Shape used for key signature. */
    private Shape keyShape;

    /** Resulting key inter, if any. */
    private KeyInter keyInter;

    /** All glyphs submitted to classifier. */
    private final Set<Glyph> glyphCandidates = new HashSet<Glyph>();

    /** Attributes assigned. */
    private final EnumSet<Attribute> attributes = EnumSet.noneOf(Attribute.class);

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
    public void addPlot (Plotter plotter)
    {
        final int xMin = projection.getStart();
        final int xMax = projection.getStop();

        {
            // Values
            XYSeries cumulSeries = new XYSeries("Key");

            for (int x = xMin; x <= xMax; x++) {
                cumulSeries.add(x, projection.getValue(x));
            }

            plotter.add(cumulSeries, Color.RED, false);
        }

        List<Integer> alterStarts = staff.getHeader().alterStarts;

        if (alterStarts != null) {
            for (int ia = 0; ia < alterStarts.size(); ia++) {
                // Items marks
                XYSeries sep = new XYSeries("A" + (ia + 1));
                double x = alterStarts.get(ia);
                sep.add(x, -Plotter.MARK);
                sep.add(x, staff.getHeight());
                plotter.add(sep, Color.CYAN, false);
            }
        }

        if (range.hasStart() || (staff.getKeyStart() != null)) {
            // Area limits
            XYSeries series = new XYSeries("KeyArea");
            int start = range.hasStart() ? range.getStart() : staff.getKeyStart();
            int stop = range.hasStart() ? range.getStop() : staff.getKeyStop();
            series.add(start, -Plotter.MARK);
            series.add(start, staff.getHeight());
            series.add(stop, staff.getHeight());
            series.add(stop, -Plotter.MARK);
            plotter.add(series, Color.ORANGE, false);
        }

        {
            // Browse start for peak threshold
            XYSeries series = new XYSeries("KeyBrowse");
            int start = range.browseStart;
            int stop = (staff.getKeyStop() != null) ? staff.getKeyStop() : range.getStop();
            series.add(start, -Plotter.MARK);
            series.add(start, params.minPeakCumul);
            series.add(stop, params.minPeakCumul);
            plotter.add(series, Color.BLACK, false);
        }

        {
            // Space threshold
            XYSeries chunkSeries = new XYSeries("Space");
            int x = range.browseStart;
            chunkSeries.add(x, params.maxSpaceCumul);
            chunkSeries.add(xMax, params.maxSpaceCumul);
            plotter.add(chunkSeries, Color.YELLOW, false);
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
        // Use pitches for chosen clef
        final ClefInter bestClef = clefs.get(0);
        final int[] stdPitches = KeyInter.getPitchesMap(keyShape).get(bestClef.getKind());

        for (int i = 0; i < roi.size(); i++) {
            KeySlice slice = roi.get(i);
            KeyAlterInter alter = slice.getAlter();

            if (alter != null) {
                final int std = stdPitches[i];

                if (alter.getIntegerPitch() != std) {
                    logger.info(
                            "Staff#{} slice#{} pitch adjusted from {} to {}",
                            getId(),
                            slice.getId(),
                            String.format("%.1f", alter.getMeasuredPitch()),
                            std);
                    alter.setPitch(std);
                }
            }
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
     * @return true when OK, false when part key must be destroyed
     */
    public boolean checkReplicate (KeyBuilder sourceBuilder)
    {
        if ((keyInter != null) && (keyInter.getFifths() == sourceBuilder.keyInter.getFifths())) {
            return true; // It's OK
        }

        final ClefInter clef = clefs.get(0);

        // Remove local slices if needed
        for (Iterator<KeySlice> it = roi.iterator(); it.hasNext();) {
            KeySlice slice = it.next();
            Integer index = column.getGlobalIndex(slice.getStart() - measureStart);

            if ((index == null) || (index >= sourceBuilder.roi.size())) {
                it.remove();
            }
        }

        // Insert local slices if needed
        for (KeySlice sourceSlice : sourceBuilder.getRoi()) {
            int sourceOffset = sourceSlice.getStart() - sourceBuilder.getMeasureStart();
            int targetStart = measureStart + sourceOffset;
            KeySlice localSlice = roi.getStartSlice(targetStart);

            if (localSlice == null) {
                // Choose start & stop values for the slice to be created
                KeySlice prevSlice = roi.getStopSlice(targetStart);
                int start = (prevSlice != null) ? (prevSlice.getStop() + 1) : targetStart;

                int targetStop = (start + sourceSlice.getWidth()) - 1;
                KeySlice nextSlice = roi.getStartSlice(targetStop + 1);
                int stop = (nextSlice != null) ? (nextSlice.getStart() - 1) : targetStop;

                localSlice = roi.createSlice(start, stop);

                final double height = params.typicalGlyphHeight;
                localSlice.setPitchRect(clef, sourceBuilder.getKeyShape(), height);

                int ink = getInk(localSlice.getRect());

                if (ink < params.minGlyphWeight) {
                    // No item can be there, hence we destroy part keys
                    return false;
                }
            }
        }

        // Check each local slice
        keyShape = sourceBuilder.getKeyShape();

        final Set<Shape> shapes = Collections.singleton(keyShape);

        for (KeySlice slice : roi) {
            KeyAlterInter alter = slice.getAlter();

            if ((alter == null) || (alter.getGrade() < Grades.keyAlterMinGrade1)) {
                slice.setPitchRect(clef, keyShape, params.typicalGlyphHeight);
                extractAlter(slice, shapes, Grades.keyAlterMinGrade2);
            }
        }

        return true;
    }

    //---------//
    // destroy //
    //---------//
    /**
     * Remove any key material: slices potential alter and key shape.
     */
    public void destroy ()
    {
        roi.destroy();
        keyShape = null;

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
     * Process the potential key signature of the underlying staff.
     * <p>
     * This builds peaks, slices, alters and checks trailing space and clef(s) compatibility.
     */
    public void process ()
    {
        logger.debug("Key processing for S#{} staff#{}", system.getId(), getId());

        // Retrieve & check peaks
        browseArea();

        // Infer signature from peaks
        int signature = retrieveSignature();

        if (signature != 0) {
            // Check/modify signature (by checking trailing length, and rough trailing space)
            signature = checkSignature(signature);
        }

        if (signature != 0) {
            // Compute start for each sig item
            List<Integer> starts = computeStarts(signature);

            // Allocate (empty) slices
            allocateSlices(starts);

            // First, look for suitable items in key area, using connected components
            retrieveComponents();

            // If some slices are still empty, use hard slice extraction
            List<KeySlice> emptySlices = roi.getEmptySlices();

            if (!emptySlices.isEmpty()) {
                logger.debug("Staff#{} empty key slices: {}", getId(), emptySlices);
                extractEmptySlices(emptySlices);

                // NOTA: Some slices may still be empty at this point...
            }

            // Check there is some space on right end of key
            if (!checkTrailingSpace()) {
                destroy();
            } else {
                // Check compatibility with active clef(s)
                clefs.addAll(staff.getCompetingClefs(starts.get(0)));

                if (!checkWithClefs()) {
                    logger.debug("Staff#{} no clef-key compatibility", getId());
                    destroy();
                }
            }
        }

        if (clefs.isEmpty()) {
            clefs.addAll(staff.getCompetingClefs(range.getStop()));

            for (Inter clef : clefs) {
                sig.computeContextualGrade(clef);
            }

            Collections.sort(clefs, Inter.byReverseBestGrade);
            clefs.retainAll(Arrays.asList(clefs.get(0)));
        }
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
        final Book book = sheet.getStub().getBook();
        final SampleRepository repository = book.getSampleRepository();

        if (repository == null) {
            return;
        }

        final SampleSheet sampleSheet = repository.findSampleSheet(sheet);
        final int interline = staff.getSpecificInterline();

        // Positive samples (assigned to keyShape)
        for (KeySlice slice : roi) {
            KeyAlterInter alter = slice.getAlter();

            if (alter != null) {
                final Glyph glyph = alter.getGlyph();

                if (recordPositives) {
                    final double pitch = staff.pitchPositionOf(glyph.getCentroid());
                    repository.addSample(keyShape, glyph, interline, sampleSheet, pitch);
                }

                glyphCandidates.remove(glyph);
            }
        }

        if (recordNegatives) {
            // Negative samples (assigned to CLUTTER)
            for (Glyph glyph : glyphCandidates) {
                final double pitch = staff.pitchPositionOf(glyph.getCentroid());
                repository.addSample(Shape.CLUTTER, glyph, interline, sampleSheet, pitch);
            }
        }
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
     * Browse the histogram to detect the sequence of peaks (similar to stems) and
     * spaces (blanks).
     */
    private void browseArea ()
    {
        // Space parameters
        int maxSpaceCumul = params.maxSpaceCumul;
        int spaceStart = -1; // Space start abscissa
        int spaceStop = -1; // Space stop abscissa
        boolean valleyHit = false; // Have we found a valley yet?

        // Peak parameters
        int peakStart = -1; // Peak start abscissa
        int peakStop = -1; // Peak stop abscissa
        int peakHeight = 0; // Peak height

        for (int x = range.browseStart; x <= range.browseStop; x++) {
            int cumul = projection.getValue(x);

            // For peak
            if (cumul >= params.minPeakCumul) {
                if (!valleyHit) {
                    continue;
                }

                if (spaceStart != -1) {
                    // End of space
                    if (!createSpace(spaceStart, spaceStop)) {
                        return; // Too wide space encountered
                    }

                    spaceStart = -1;
                }

                if (peakStart == -1) {
                    peakStart = x; // Beginning of peak
                }

                peakStop = x;
                peakHeight = Math.max(peakHeight, cumul);
            } else if (!valleyHit) {
                valleyHit = true;
            } else {
                if (peakStart != -1) {
                    // End of peak
                    if (!createPeak(peakStart, peakStop, peakHeight)) {
                        return; // Invalid peak encountered
                    }

                    peakStart = -1;
                    peakHeight = 0;
                }

                // For space
                if (cumul <= maxSpaceCumul) {
                    // Below threshold, we are in a space
                    if (spaceStart == -1) {
                        spaceStart = x; // Start of space
                    }

                    spaceStop = x;
                } else if (spaceStart != -1) {
                    // End of space
                    if (!createSpace(spaceStart, spaceStop)) {
                        return; // Too wide space encountered
                    }

                    spaceStart = -1;
                }
            }
        }

        // Finish ongoing space if any
        if (spaceStart != -1) {
            createSpace(spaceStart, spaceStop);
        } else if (peakStart != -1) {
            // Finish ongoing peak if any (this is rather unlikely...)
            createPeak(peakStart, peakStop, peakHeight);
        }
    }

    //----------------//
    // checkSignature //
    //----------------//
    /**
     * Additional tests on key signature, which may get adjusted.
     * <p>
     * We check if there is enough ink after last peak (depending on key shape).
     * We check also if the first invalid peak (if any) is sufficiently far from last good peak so
     * that there is enough room for key trailing space.
     *
     * @return the signature value, perhaps modified
     */
    private int checkSignature (int signature)
    {
        KeyEvent.Peak lastGoodPeak = getLastValidPeak();
        KeyEvent.Peak firstInvalidPeak = getFirstInvalidPeak();

        if (firstInvalidPeak != null) {
            // Where is the precise end of key signature?
            // Check x delta between previous (good) peak and this (invalid) one
            range.setStop(firstInvalidPeak.start - 1);

            int trail = range.getStop() - lastGoodPeak.start + 1;

            // Check trailing length
            if (signature < 0) {
                if (trail < params.minFlatTrail) {
                    logger.debug("Removing too narrow flat");
                    lastGoodPeak.setInvalid(); // Invalidate peak
                    range.setStop(getFirstInvalidPeak().start - 1);
                    signature += 1;
                }
            } else if (trail < params.minSharpTrail) {
                logger.debug("Removing too narrow sharp");
                // Invalidate last 2 peaks
                lastGoodPeak.setInvalid();
                lastGoodPeak = getLastValidPeak();
                lastGoodPeak.setInvalid();
                range.setStop(getFirstInvalidPeak().start - 1);
                signature -= 1;
            }
        }

        firstInvalidPeak = getFirstInvalidPeak();
        lastGoodPeak = getLastValidPeak();

        if ((lastGoodPeak != null) && (firstInvalidPeak != null)) {
            // Check minimum trailing space
            if ((firstInvalidPeak.start - lastGoodPeak.stop - 1) < params.minTrailingSpace) {
                logger.debug("Staff#{} no minimum trailing space", getId());

                return 0;
            }
        }

        return signature;
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
     * Compare the sequence of candidate key items with the possible active clefs.
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
                logger.info(dumpOf(clefKind, keyGrade, pitchedGrades));

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
                    logger.info(
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
                KeyEvent.Peak peak = peaks.get(i);

                if (peak.isInvalid()) {
                    break;
                }

                starts.add((int) Math.ceil(0.5 * (peak.start + peaks.get(i - 1).stop)));
            }

            // End of area
            refineAreaStop(getLastValidPeak(), params.sharpTrail, params.maxSharpTrail);
        } else if (signature < 0) {
            // Flats
            KeyEvent.Peak firstPeak = peaks.get(0);

            // Start of area, make sure there is nothing right before first peak
            int flatHeading = ((firstPeak.start + firstPeak.stop) / 2) - range.getStart();

            if (flatHeading <= params.maxFlatHeading) {
                starts.add(range.getStart());

                for (int i = 1; i < peaks.size(); i++) {
                    KeyEvent.Peak peak = peaks.get(i);

                    if (peak.isInvalid()) {
                        break;
                    }

                    starts.add(peak.start);
                }

                // End of area
                refineAreaStop(getLastValidPeak(), params.flatTrail, params.maxFlatTrail);
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
     * The peak is checked for its height and its width.
     * If two (raw) peaks are too close, they are merged into a single peak.
     *
     * @param start  start abscissa
     * @param stop   stop abscissa
     * @param height peak height
     * @return whether key processing can keep on
     */
    private boolean createPeak (int start,
                                int stop,
                                int height)
    {
        boolean keepOn = true;
        KeyEvent.Peak peak = new KeyEvent.Peak(start, stop, height);

        // Check whether this peak could be part of sig, otherwise give up
        if ((height > params.maxPeakCumul) || (peak.getWidth() > params.maxPeakWidth)) {
            logger.debug("Invalid height or width for peak");
            peak.setInvalid();
            keepOn = false;
        } else {
            // Does this peak correspond to a stem-shaped item? if not, simply ignore it
            if (!isStemLike(peak)) {
                return true;
            }

            // We may have an interesting peak, check distance since previous peak
            KeyEvent.Peak prevPeak = peaks.isEmpty() ? null : peaks.get(peaks.size() - 1);

            if (prevPeak != null) {
                // Check delta abscissa
                double x = (start + stop) / 2.0;
                double dx = x - ((prevPeak.start + prevPeak.stop) / 2.0);

                if (dx > params.maxPeakDx) {
                    // A large dx indicates we are beyond end of key signature
                    logger.debug("Too large delta since previous peak");
                    peak.setInvalid();
                    keepOn = false;
                } else if (dx < params.minPeakDx) {
                    // Too small dx: merge with previous peak (which may get too wide)
                    logger.debug("Extending {} to {}", prevPeak, stop);
                    prevPeak.stop = stop;
                    prevPeak.height = Math.max(prevPeak.height, height);

                    if (prevPeak.getWidth() > params.maxPeakWidth) {
                        logger.debug("Invalid width for peak");
                        prevPeak.setInvalid();

                        return false;
                    }

                    return true;
                }
            } else {
                // Very first peak, check offset from theoretical start
                // TODO: this is too strict, check emptyness in previous abscissae
                int offset = start - range.browseStart;

                if (offset > params.maxFirstPeakOffset) {
                    logger.debug("First peak arrives too late");
                    peak.setInvalid();
                    keepOn = false;
                } else if (!range.hasStart()) {
                    // Set range.start at beginning of browsing, since no space was found before peak
                    range.setStart(range.browseStart);
                }
            }
        }

        events.add(peak);
        peaks.add(peak);

        return keepOn;
    }

    //-------------//
    // createSpace //
    //-------------//
    /**
     * (Try to) create a new space between items. (clef, alterations, time-sig, ...)
     *
     * @param spaceStart space start abscissa
     * @param spaceStop  space stop abscissa
     * @return true to keep browsing, false to stop immediately
     */
    private boolean createSpace (int spaceStart,
                                 int spaceStop)
    {
        boolean keepOn = true;
        KeyEvent.Space space = new KeyEvent.Space(spaceStart, spaceStop);

        if (!range.hasStart()) {
            // This is the very first space found
            if (space.getWidth() > params.maxFirstSpaceWidth) {
                // No key signature!
                logger.debug("Staff#{} no key signature.", getId());
                attributes.add(Attribute.INITIAL_WIDE_SPACE);
                keepOn = false;
            } else {
                // Set range.getStart() here, since first chunk may be later skipped if lacking peak
                range.setStart(space.stop + 1);
            }
        } else if (peaks.isEmpty()) {
            range.setStart(space.stop + 1);
        } else if (space.getWidth() > params.maxInnerSpace) {
            range.setStop(space.start);
            keepOn = false;
        }

        events.add(space);

        return keepOn;
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

    //--------------//
    // extractAlter //
    //--------------//
    /**
     * In the provided slice, extract the relevant foreground pixels from the NO_STAFF
     * image and evaluate possible glyph instances.
     *
     * @param slice        the slice to process
     * @param targetShapes the set of shapes to try
     * @param minGrade     minimum acceptable grade
     * @return the Inter created if any
     */
    private KeyAlterInter extractAlter (KeySlice slice,
                                        Set<Shape> targetShapes,
                                        double minGrade)
    {
        Rectangle sliceRect = slice.getRect();
        ByteProcessor sliceBuf = roi.getSlicePixels(staffFreeSource, slice);
        RunTable runTable = new RunTableFactory(VERTICAL).createTable(sliceBuf);
        List<Glyph> parts = GlyphFactory.buildGlyphs(runTable, sliceRect.getLocation());
        purgeParts(parts, (sliceRect.x + sliceRect.width) - 1);
        system.registerGlyphs(parts, Group.ALTER_PART);

        SingleAdapter adapter = new SingleAdapter(slice, parts, targetShapes, minGrade);
        new GlyphCluster(adapter, null).decompose();

        if (slice.eval != null) {
            double grade = Inter.intrinsicRatio * slice.eval.grade;

            if (grade >= minGrade) {
                if ((slice.alter == null) || (slice.alter.getGlyph() != slice.glyph)) {
                    logger.debug("Glyph#{} {}", slice.glyph.getId(), slice.eval);

                    KeyAlterInter alterInter = KeyAlterInter.create(
                            slice.glyph,
                            slice.eval.shape,
                            grade,
                            staff);
                    sig.addVertex(alterInter);
                    slice.alter = alterInter;
                    logger.debug("{}", slice);
                }

                return slice.alter;
            }
        }

        return null;
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
            extractAlter(slice, Collections.singleton(keyShape), Grades.keyAlterMinGrade2);
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
                extractAlter(slice, shapes, Grades.keyAlterMinGrade2);
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

    //---------------------//
    // getFirstInvalidPeak //
    //---------------------//
    /**
     * Report the first invalid peak found.
     *
     * @return the first invalid peak, if any, or null
     * @see #getLastValidPeak
     */
    private KeyEvent.Peak getFirstInvalidPeak ()
    {
        for (KeyEvent.Peak peak : peaks) {
            if (peak.isInvalid()) {
                return peak;
            }
        }

        return null;
    }

    //--------//
    // getInk //
    //--------//
    /**
     * Report the amount of ink in the provided rectangle of the staff-free buffer.
     *
     * @param rect provided rectangle
     * @return number of foreground pixels, off staff lines
     */
    private int getInk (Rectangle rect)
    {
        final int xMin = rect.x;
        final int xMax = (rect.x + rect.width) - 1;
        final int yMin = rect.y;
        final int yMax = (rect.y + rect.height) - 1;
        int weight = 0;

        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                if (staffFreeSource.get(x, y) == 0) {
                    weight++;
                }
            }
        }

        return weight;
    }

    //------------------//
    // getLastValidPeak //
    //------------------//
    /**
     * Report the last valid peak found.
     *
     * @return the last valid peak, if any, or null
     * @see #getFirstInvalidPeak
     */
    private KeyEvent.Peak getLastValidPeak ()
    {
        KeyEvent.Peak good = null;

        for (KeyEvent.Peak peak : peaks) {
            if (peak.isInvalid()) {
                break;
            }

            good = peak;
        }

        return good;
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
     * @return the populated cumulation table
     */
    private Projection getProjection (Rectangle browseRect)
    {
        final int xMin = browseRect.x;
        final int xMax = (browseRect.x + browseRect.width) - 1;
        final int yMin = browseRect.y;
        final int yMax = (browseRect.y + browseRect.height) - 1;
        final Projection table = new Projection.Short(xMin, xMax);

        for (int x = xMin; x <= xMax; x++) {
            short cumul = 0;

            for (int y = yMin; y <= yMax; y++) {
                if (staffFreeSource.get(x, y) == 0) {
                    cumul++;
                }
            }

            table.increment(x, cumul);
        }

        return table;
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
    private boolean isStemLike (KeyEvent.Peak peak)
    {
        final Rectangle rect = new Rectangle(peak.start, roi.y, peak.getWidth(), roi.height);

        if (peak.getWidth() <= 2) {
            rect.grow(1, 0); // Slight margin on left & right of peak
        }

        boolean stem = hasStem(rect, staffFreeSource, params.coreStemLength, params.minBlackRatio);

        if (!stem) {
            logger.debug("Staff#{} {} no stem", getId(), peak);
        }

        return stem;
    }

    //-----------------//
    // purgeCandidates //
    //-----------------//
    /**
     * Make sure that no part is shared by several candidates.
     */
    private void purgeCandidates (List<Candidate> candidates)
    {
        final List<Candidate> toRemove = new ArrayList<Candidate>();
        Collections.sort(candidates);

        for (int i = 0; i < candidates.size(); i++) {
            final Candidate candidate = candidates.get(i);

            for (Glyph part : candidate.parts) {
                toRemove.clear();

                for (Candidate c : candidates.subList(i + 1, candidates.size())) {
                    if (c.parts.contains(part)) {
                        toRemove.add(c);
                    }
                }

                candidates.removeAll(toRemove);
            }
        }
    }

    //------------//
    // purgeParts //
    //------------//
    /**
     * Purge the population of candidate parts as much as possible, since the cost
     * of their later combinations is exponential.
     * <p>
     * Those of width 1 and stuck on right side of slice can be safely removed, since they
     * certainly belong to the stem of the next slice.
     * <p>
     * Those composed of just one (isolated) pixel are also removed, although this is more
     * questionable.
     *
     * @param parts the collection to purge
     * @param xMax  maximum abscissa in area
     */
    private void purgeParts (List<Glyph> parts,
                             int xMax)
    {
        final List<Glyph> toRemove = new ArrayList<Glyph>();

        for (Glyph glyph : parts) {
            if ((glyph.getWeight() < params.minPartWeight) || (glyph.getBounds().x == xMax)) {
                toRemove.add(glyph);
            }
        }

        if (!toRemove.isEmpty()) {
            parts.removeAll(toRemove);
        }

        if (parts.size() > params.maxPartCount) {
            Collections.sort(parts, Glyphs.byReverseWeight);
            parts.retainAll(parts.subList(0, params.maxPartCount));
        }
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
    private void refineAreaStop (KeyEvent.Peak lastGoodPeak,
                                 int typicalTrail,
                                 int maxTrail)
    {
        final int xMin = (lastGoodPeak.start + typicalTrail) - 1;
        final int xMax = Math.min(projection.getStop(), lastGoodPeak.start + maxTrail);

        int minCount = Integer.MAX_VALUE;

        for (int x = xMin; x <= xMax; x++) {
            int count = projection.getValue(x);

            if (count < minCount) {
                range.setStop(x - 1);
                minCount = count;
            }
        }
    }

    //---------------//
    // registerParts //
    //---------------//
    /**
     * Make sure any part glyph is registered (meant for easier visual checking).
     *
     * @param parts the parts to register
     */
    private void registerParts (List<Glyph> parts)
    {
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();

        for (ListIterator<Glyph> li = parts.listIterator(); li.hasNext();) {
            Glyph glyph = li.next();
            glyph = glyphIndex.registerOriginal(glyph);
            glyph.addGroup(Group.ALTER_PART);
            system.addFreeGlyph(glyph);
            li.set(glyph);
        }
    }

    //--------------------//
    // retrieveComponents //
    //--------------------//
    /**
     * Look into sig area for key items, based on connected components.
     */
    private void retrieveComponents ()
    {
        logger.debug("Key for staff#{}", getId());

        // Key-signature area pixels
        ByteProcessor keyBuf = roi.getAreaPixels(staffFreeSource, range);
        RunTable runTable = new RunTableFactory(VERTICAL).createTable(keyBuf);
        List<Glyph> parts = GlyphFactory.buildGlyphs(
                runTable,
                new Point(range.getStart(), roi.y));

        purgeParts(parts, range.getStop());
        registerParts(parts);

        // Formalize parts relationships in a global graph
        SimpleGraph<Glyph, GlyphLink> globalGraph = Glyphs.buildLinks(parts, params.maxPartGap);
        List<Set<Glyph>> sets = new ConnectivityInspector<Glyph, GlyphLink>(
                globalGraph).connectedSets();
        logger.debug("Staff#{} sets:{}", getId(), sets.size());

        List<Candidate> allCandidates = new ArrayList<Candidate>();

        for (Set<Glyph> set : sets) {
            // Use only the subgraph for this set
            SimpleGraph<Glyph, GlyphLink> subGraph = GlyphCluster.getSubGraph(set, globalGraph);
            MultipleAdapter adapter = new MultipleAdapter(
                    subGraph,
                    Collections.singleton(keyShape), // TODO: include NATURAL shape as well?
                    Grades.keyAlterMinGrade1);
            new GlyphCluster(adapter, null).decompose();
            logger.debug("Staff#{} set:{} trials:{}", getId(), set.size(), adapter.trials);
            allCandidates.addAll(adapter.candidates);
        }

        purgeCandidates(allCandidates);

        for (Candidate candidate : allCandidates) {
            final KeySlice slice = roi.sliceOf(candidate.glyph.getCentroid().x);

            if ((slice.eval == null) || (slice.eval.grade < candidate.eval.grade)) {
                slice.eval = candidate.eval;
                slice.glyph = candidate.glyph;
            }
        }

        for (KeySlice slice : roi) {
            if (slice.eval != null) {
                double grade = Inter.intrinsicRatio * slice.eval.grade;
                KeyAlterInter alterInter = KeyAlterInter.create(
                        slice.glyph,
                        slice.eval.shape,
                        grade,
                        staff);
                sig.addVertex(alterInter);
                slice.alter = alterInter;
            }

            logger.debug("{}", slice);
        }
    }

    //-------------------//
    // retrieveSignature //
    //-------------------//
    /**
     * Retrieve the staff signature value.
     * This is based on the average value of peaks intervals, computed on the short ones (the
     * intervals shorter or equal to the mean value).
     *
     * @return -flats, 0 or +sharps
     */
    private int retrieveSignature ()
    {
        KeyEvent.Peak lastGoodPeak = getLastValidPeak();

        if (lastGoodPeak == null) {
            logger.debug("no valid peak");

            return 0;
        }

        int last = peaks.indexOf(lastGoodPeak); // Index of last good peak
        int offset = peaks.get(0).start - range.getStart(); // Initial abscissa offset

        if (last > 0) {
            // Compute mean value of short intervals
            double meanDx = (peaks.get(last).getCenter() - peaks.get(0).getCenter()) / last;
            int shorts = 0;
            double sum = 0;

            for (int i = 1; i <= last; i++) {
                double interval = peaks.get(i).getCenter() - peaks.get(i - 1).getCenter();

                if (interval <= meanDx) {
                    shorts++;
                    sum += interval;
                }
            }

            double meanShort = sum / shorts;

            if (meanShort < params.minFlatDelta) {
                keyShape = Shape.SHARP;
            } else if (meanShort > params.maxSharpDelta) {
                keyShape = Shape.FLAT;
            } else {
                keyShape = (offset > params.offsetThreshold) ? Shape.SHARP : Shape.FLAT;
            }

            // For sharps, peaks count must be an even number
            if (keyShape == Shape.SHARP) {
                if (((last + 1) % 2) != 0) {
                    // Discard last peak
                    lastGoodPeak.setInvalid();
                    range.setStop(getFirstInvalidPeak().start - 1);
                    last--;
                }

                return (last + 1) / 2;
            } else {
                return -(last + 1);
            }
        } else if (offset <= params.offsetThreshold) {
            // Acceptable flat
            keyShape = Shape.FLAT;

            return -1;
        } else {
            // Non acceptable stuff, so discard this single peak!
            lastGoodPeak.setInvalid();
            range.setStop(getFirstInvalidPeak().start - 1);

            return 0;
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Candidate //
    //-----------//
    private static class Candidate
            implements Comparable<Candidate>
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Glyph glyph;

        final Set<Glyph> parts;

        final Evaluation eval;

        //~ Constructors ---------------------------------------------------------------------------
        public Candidate (Glyph glyph,
                          Set<Glyph> parts,
                          Evaluation eval)
        {
            this.glyph = glyph;
            this.parts = parts;
            this.eval = eval;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int compareTo (Candidate that)
        {
            return Double.compare(that.eval.grade, this.eval.grade);
        }
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

        final int minPeakCumul;

        final int maxSpaceCumul;

        final int coreStemLength;

        final double minBlackRatio;

        final int maxPeakCumul;

        final int maxPeakWidth;

        final int maxFlatHeading;

        final int flatTrail;

        final int minFlatTrail;

        final int maxFlatTrail;

        final int sharpTrail;

        final int minSharpTrail;

        final int maxSharpTrail;

        final int maxPeakDx;

        final int minPeakDx;

        final double maxSharpDelta;

        final double minFlatDelta;

        final double offsetThreshold;

        final int maxPartCount;

        final int minPartWeight;

        final double maxPartGap;

        final double minGlyphWidth;

        final double maxGlyphWidth;

        final double minGlyphHeight;

        final double typicalGlyphHeight;

        final double maxGlyphHeight;

        final int minGlyphWeight;

        final int maxGlyphWeight;

        final int minTrailingSpace;

        final int maxTrailingCumul;

        final double maxDeltaPitch_1;

        final double maxDeltaPitch_4;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            preStaffMargin = scale.toPixels(constants.preStaffMargin);
            maxFirstPeakOffset = scale.toPixels(constants.maxFirstPeakOffset);
            maxFirstSpaceWidth = scale.toPixels(constants.maxFirstSpaceWidth);
            maxInnerSpace = scale.toPixels(constants.maxInnerSpace);
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
            maxSharpDelta = scale.toPixelsDouble(constants.maxSharpDelta);
            minFlatDelta = scale.toPixelsDouble(constants.minFlatDelta);
            offsetThreshold = scale.toPixelsDouble(constants.offsetThreshold);
            maxPartCount = constants.maxPartCount.getValue();
            minPartWeight = scale.toPixels(constants.minPartWeight);
            maxPartGap = scale.toPixelsDouble(constants.maxPartGap);
            minGlyphWidth = scale.toPixelsDouble(constants.minGlyphWidth);
            maxGlyphWidth = scale.toPixelsDouble(constants.maxGlyphWidth);
            minGlyphHeight = scale.toPixelsDouble(constants.minGlyphHeight);
            typicalGlyphHeight = scale.toPixelsDouble(constants.typicalGlyphHeight);
            maxGlyphHeight = scale.toPixelsDouble(constants.maxGlyphHeight);
            minGlyphWeight = scale.toPixels(constants.minGlyphWeight);
            maxGlyphWeight = scale.toPixels(constants.maxGlyphWeight);
            maxTrailingCumul = scale.toPixels(constants.maxTrailingCumul);
            minTrailingSpace = scale.toPixels(constants.minTrailingSpace);
            maxDeltaPitch_1 = constants.maxDeltaPitch_1.getValue();
            maxDeltaPitch_4 = constants.maxDeltaPitch_4.getValue();

            // Maximum alteration contribution (on top of staff lines)
            int whiteSpace = scale.getInterline() - scale.getMainFore();
            double maxAlterContrib = constants.typicalGlyphHeight.getValue() * whiteSpace;
            minPeakCumul = (int) Math.rint(
                    (5 * scale.getMainFore())
                    + (constants.peakHeightRatio.getValue() * maxAlterContrib));
        }
    }

    //--------------------//
    // AbstractKeyAdapter //
    //--------------------//
    /**
     * Abstract adapter for retrieving items.
     */
    private abstract class AbstractKeyAdapter
            extends AbstractAdapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Minimum acceptable intrinsic grade. */
        protected final double minGrade;

        /** Relevant shapes. */
        protected final EnumSet<Shape> targetShapes = EnumSet.noneOf(Shape.class);

        //~ Constructors ---------------------------------------------------------------------------
        public AbstractKeyAdapter (SimpleGraph<Glyph, GlyphLink> graph,
                                   Set<Shape> targetShapes,
                                   double minGrade)
        {
            super(graph);
            this.targetShapes.addAll(targetShapes);
            this.minGrade = minGrade;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean isTooHeavy (int weight)
        {
            return weight > params.maxGlyphWeight;
        }

        @Override
        public boolean isTooLarge (Rectangle bounds)
        {
            return (bounds.width > params.maxGlyphWidth)
                   || (bounds.height > params.maxGlyphHeight);
        }

        @Override
        public boolean isTooLight (int weight)
        {
            return weight < params.minGlyphWeight;
        }

        @Override
        public boolean isTooSmall (Rectangle bounds)
        {
            return (bounds.width < params.minGlyphWidth)
                   || (bounds.height < params.minGlyphHeight);
        }

        protected abstract void keepCandidate (Glyph glyph,
                                               Set<Glyph> parts,
                                               Evaluation eval);

        protected boolean embracesSlicePeaks (KeySlice slice,
                                              Glyph glyph)
        {
            final Rectangle sliceBox = slice.getRect();
            final int sliceStart = sliceBox.x;
            final int sliceStop = (sliceBox.x + sliceBox.width) - 1;
            final Rectangle glyphBox = glyph.getBounds();

            // Make sure that the glyph width embraces the slice peak(s)
            for (KeyEvent.Peak peak : peaks) {
                if (peak.isInvalid()) {
                    break;
                }

                final double peakCenter = peak.getCenter();

                if ((sliceStart <= peakCenter) && (peakCenter <= sliceStop)) {
                    // Is this slice peak embraced by glyph?
                    if (!GeoUtil.xEmbraces(glyphBox, peakCenter)) {
                        return false;
                    }
                }
            }

            return true;
        }

        protected void evaluateSliceGlyph (KeySlice slice,
                                           Glyph glyph,
                                           Set<Glyph> parts)
        {
            if (isTooSmall(glyph.getBounds())) {
                return;
            }

            if (!embracesSlicePeaks(slice, glyph)) {
                return;
            }

            trials++;

            if (glyph.getId() == 0) {
                glyph = sheet.getGlyphIndex().registerOriginal(glyph);
                system.addFreeGlyph(glyph);
            }

            if (glyph.isVip()) {
                logger.info("VIP evaluateSliceGlyph for {}", glyph);
            }

            glyphCandidates.add(glyph);

            Evaluation[] evals = classifier.getNaturalEvaluations(glyph, sheet.getInterline());

            for (Shape shape : targetShapes) {
                Evaluation eval = evals[shape.ordinal()];
                double grade = Inter.intrinsicRatio * eval.grade;

                if (grade >= minGrade) {
                    logger.debug("glyph#{} width:{} {}", glyph.getId(), glyph.getWidth(), eval);
                    keepCandidate(glyph, parts, eval);
                }
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

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
                0.4,
                "Ratio of height to detect peaks");

        private final Scale.Fraction preStaffMargin = new Scale.Fraction(
                2.0,
                "Horizontal margin before staff left (for plot display)");

        private final Scale.Fraction maxFirstPeakOffset = new Scale.Fraction(
                2.0,
                "Maximum x offset of first peak (WRT browse start)");

        private final Scale.Fraction maxPeakCumul = new Scale.Fraction(
                4.0,
                "Maximum cumul value to accept peak (absolute value)");

        private final Scale.Fraction maxPeakWidth = new Scale.Fraction(
                0.4,
                "Maximum width to accept peak (measured at threshold height)");

        private final Scale.Fraction maxFlatHeading = new Scale.Fraction(
                0.4,
                "Maximum heading length before peak for a flat item");

        private final Scale.Fraction flatTrail = new Scale.Fraction(
                1.0,
                "Typical trailing length after peak for a flat item");

        private final Scale.Fraction minFlatTrail = new Scale.Fraction(
                0.8,
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

        private final Scale.Fraction minPeakDx = new Scale.Fraction(
                0.3,
                "Mainmum delta abscissa between peaks");

        private final Scale.Fraction maxPeakDx = new Scale.Fraction(
                1.4,
                "Maximum delta abscissa between peaks");

        private final Scale.Fraction maxSharpDelta = new Scale.Fraction(
                0.75,
                "Maximum short peak delta for sharps");

        private final Scale.Fraction minFlatDelta = new Scale.Fraction(
                0.5,
                "Minimum short peak delta for flats");

        private final Scale.Fraction offsetThreshold = new Scale.Fraction(
                0.1,
                "Threshold on first peak offset that differentiates flat & sharp");

        private final Constant.Integer maxPartCount = new Constant.Integer(
                "Glyphs",
                8,
                "Maximum number of parts considered for an alter symbol");

        private final Scale.AreaFraction minPartWeight = new Scale.AreaFraction(
                0.01,
                "Minimum weight for an alter part");

        private final Scale.Fraction maxPartGap = new Scale.Fraction(
                1.5,
                "Maximum distance between two parts of a single alter symbol");

        private final Scale.Fraction minGlyphWidth = new Scale.Fraction(
                0.5,
                "Minimum glyph width");

        private final Scale.Fraction maxGlyphWidth = new Scale.Fraction(
                2.0,
                "Maximum glyph width");

        private final Scale.Fraction minGlyphHeight = new Scale.Fraction(
                1.0,
                "Minimum glyph height");

        private final Scale.Fraction maxGlyphHeight = new Scale.Fraction(
                3.5,
                "Maximum glyph height");

        private final Scale.AreaFraction minGlyphWeight = new Scale.AreaFraction(
                0.2,
                "Minimum glyph weight");

        private final Scale.AreaFraction maxGlyphWeight = new Scale.AreaFraction(
                2.0,
                "Maximum glyph weight");

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
                0.5,
                "Minimum space width after last key item");

        // Beware: A too small value might miss the whole key signature
        private final Scale.Fraction maxFirstSpaceWidth = new Scale.Fraction(
                1.75,
                "Maximum initial space before key signature");

        // Beware: A too small value might miss final key signature items
        private final Scale.Fraction maxInnerSpace = new Scale.Fraction(
                0.7,
                "Maximum inner space within key signature");
    }

    //-----------------//
    // MultipleAdapter //
    //-----------------//
    /**
     * Adapter for retrieving all items of the key (in key area).
     */
    private class MultipleAdapter
            extends AbstractKeyAdapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        final List<Candidate> candidates = new ArrayList<Candidate>();

        //~ Constructors ---------------------------------------------------------------------------
        public MultipleAdapter (SimpleGraph<Glyph, GlyphLink> graph,
                                Set<Shape> targetShapes,
                                double minGrade)
        {
            super(graph, targetShapes, minGrade);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void evaluateGlyph (Glyph glyph,
                                   Set<Glyph> parts)
        {
            // Retrieve impacted slice
            final KeySlice slice = roi.sliceOf(glyph.getCentroid().x);

            if (slice != null) {
                evaluateSliceGlyph(slice, glyph, parts);
            }
        }

        @Override
        protected void keepCandidate (Glyph glyph,
                                      Set<Glyph> parts,
                                      Evaluation eval)
        {
            candidates.add(new Candidate(glyph, parts, eval));
        }
    }

    //---------------//
    // SingleAdapter //
    //---------------//
    /**
     * Adapter for retrieving one key item (in a slice).
     */
    private class SingleAdapter
            extends AbstractKeyAdapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Related slice. */
        private final KeySlice slice;

        //~ Constructors ---------------------------------------------------------------------------
        public SingleAdapter (KeySlice slice,
                              List<Glyph> parts,
                              Set<Shape> targetShapes,
                              double minGrade)
        {
            super(Glyphs.buildLinks(parts, params.maxPartGap), targetShapes, minGrade);
            this.slice = slice;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void evaluateGlyph (Glyph glyph,
                                   Set<Glyph> parts)
        {
            evaluateSliceGlyph(slice, glyph, parts);
        }

        @Override
        protected void keepCandidate (Glyph glyph,
                                      Set<Glyph> parts,
                                      Evaluation eval)
        {
            if ((slice.eval == null) || (slice.eval.grade < eval.grade)) {
                slice.eval = eval;
                slice.glyph = glyph;
            }
        }
    }
}
