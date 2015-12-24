//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       K e y B u i l d e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.header;

import omr.classifier.Classifier;
import omr.classifier.Evaluation;
import omr.classifier.GlyphClassifier;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphCluster;
import omr.glyph.GlyphFactory;
import omr.glyph.GlyphLink;
import omr.glyph.GlyphIndex;
import omr.glyph.Glyphs;
import omr.glyph.Grades;
import omr.glyph.Shape;

import omr.math.Clustering;
import omr.math.Population;
import omr.math.Projection;

import static omr.run.Orientation.VERTICAL;

import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.header.HeaderBuilder.Plotter;

import omr.sig.SIGraph;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.ClefInter;
import omr.sig.inter.ClefInter.ClefKind;
import omr.sig.inter.Inter;
import omr.sig.inter.KeyAlterInter;
import omr.sig.inter.KeyInter;
import omr.sig.relation.ClefKeyRelation;
import omr.sig.relation.Exclusion;
import omr.sig.relation.KeyAltersRelation;
import omr.sig.relation.Relation;

import omr.util.Navigable;

import ij.process.Blitter;
import ij.process.ByteProcessor;

import org.jfree.data.xy.XYSeries;

import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class {@code KeyBuilder} retrieves a staff key signature through the projection
 * to x-axis of the foreground pixels in a given abscissa range of a staff.
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
 * <img src="http://www.musicarrangers.com/star-theory/images/p14a.gif" />
 * <p>
 * <img src="http://www.musicarrangers.com/star-theory/images/p14b.gif" />
 * <p>
 * The relative positioning of alterations in a given signature is identical for all clefs (treble,
 * alto, tenor, bass) with the only exception of the sharp-based signatures in tenor clef.
 * <p>
 * <img src="http://www.musicarrangers.com/star-theory/images/p14c.gif" />
 * <p>
 * The main tool is a projection of the StaffHeader pixels onto the x-axis.
 * Vertically, the projection uses an envelope that can embrace any key signature (under any clef),
 * from two interline values above the staff to one interline value below the staff.
 * Horizontally, the goal is to split the projection into slices, one slice for each alteration item
 * to be extracted.
 * <p>
 * Peak detection allows to detect alteration "stems" (one for a flat, two for a sharp).
 * Typical x delta between two stems of a sharp is around 0.5+ interline.
 * Typical x delta between stems of 2 flats (or first stems of 2 sharps) is around 1+ interline.
 * Unfortunately, some flat-delta may be smaller than some sharp-delta...
 * <p>
 * Typical peak height (above the lines height) is around 2+ interline values.
 * All peaks have similar heights in the same key-sig, this may differentiate a key-sig from a
 * time-sig.
 * A space, if any, between two key-sig items is very narrow.
 * <p>
 * Strategy:<ol>
 * <li>Find first significant space right after clef, it's the space that separates the clef from
 * next item (key-sig or time-sig or first note/rest, etc).
 * This space may not be detected in the projection when the first key-sig item is very close to the
 * clef, because their projections on x-axis overlap.
 * If that space is really wide, consider there is no key-sig.
 * <li>The next really wide space, if any, will mark the end of key-sig.
 * <li>Look for peaks in the area, check peak width at threshold height, make sure their heights
 * are similar.
 * <li>Once all peaks have been retrieved, check delta abscissa between peaks, to differentiate
 * sharps vs flats sequence.
 * Additional help is brought by checking the left side of first peak (it is almost void for a flat
 * and not for a sharp).
 * <li>Determine the number of items.
 * <li>Determine precise horizontal slicing of the projection into items.
 * <li>Extract each item glyph and submit it to shape classifier for verification and vertical
 * positioning.
 * <li>Create one KeyInter instance.
 * <li>Create one KeyAlterInter instance per item.
 * <li>Verify each item pitch in the staff (to be later matched against staff clef).
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

    //~ Instance fields ----------------------------------------------------------------------------
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

    /** Header key-sig or key-sig change?. TODO: not yet used, but will be needed */
    private final boolean inHeader;

    /** Shape classifier to use. */
    private final Classifier evaluator = GlyphClassifier.getInstance();

    /** Staff-free pixel source. */
    private final ByteProcessor staffFreeSource;

    /** Precise beginning abscissa of measure. */
    private final int measureStart;

    /** Projection of foreground pixels, indexed by abscissa. */
    private final Projection projection;

    /** Minimum ordinate for key-sig area. */
    private int areaTop = Integer.MAX_VALUE;

    /** Maximum ordinate for key-sig area. */
    private int areaBottom = Integer.MIN_VALUE;

    /** Cumulated pixels, over space threshold, in area range. */
    private int areaWeight;

    /** Sequence of peaks found. */
    private final List<KeyEvent.Peak> peaks = new ArrayList<KeyEvent.Peak>();

    /** Sequence of spaces and peaks. (for debugging) */
    private final List<KeyEvent> events = new ArrayList<KeyEvent>();

    /** Shape used for key signature. */
    private Shape keyShape;

    /** Sequence of alteration slices. */
    private final List<Slice> slices = new ArrayList<Slice>();

    /** Resulting key inter, if any. */
    private KeyInter keyInter;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new KeyBuilder object.
     *
     * @param staff        the underlying staff
     * @param globalWidth  global plotting width
     * @param measureStart precise beginning abscissa of measure (generally right after bar line).
     * @param browseStart  estimated beginning abscissa for browsing.
     * @param inHeader     true for the key-sig in StaffHeader, false for a key-sig change
     */
    public KeyBuilder (Staff staff,
                       int globalWidth,
                       int measureStart,
                       int browseStart,
                       boolean inHeader)
    {
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

        // Cumulate pixels for each abscissa in range
        projection = getProjection();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // addPlot //
    //---------//
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

        if (range.start != -1) {
            // Area limits
            XYSeries series = new XYSeries("KeyArea");
            int start = range.start;
            int stop = (range.stop != 0) ? range.stop : xMax;
            series.add(start, -Plotter.MARK);
            series.add(start, staff.getHeight());
            series.add(stop, staff.getHeight());
            series.add(stop, -Plotter.MARK);
            plotter.add(series, Color.ORANGE, false);
        }

        {
            // Browse start for peak threshold
            XYSeries series = new XYSeries("KeyBrowse");
            int x = range.browseStart;
            series.add(x, -Plotter.MARK);
            series.add(x, params.minPeakCumul);
            series.add((range.stop != 0) ? range.stop : xMax, params.minPeakCumul);
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
    public void adjustPitches ()
    {
        if (slices.isEmpty()) {
            return;
        }

        //        StringBuilder sb = new StringBuilder();
        //
        //        for (Slice slice : slices) {
        //            KeyAlterInter alter = slice.alter;
        //
        //            if (sb.length() > 0) {
        //                sb.append(", ");
        //            }
        //
        //            if (alter != null) {
        //                sb.append(alter.getPitch());
        //            } else {
        //                sb.append("null");
        //            }
        //        }
        //
        //        logger.info("S#{} {}", staff.getId(), sb);
        //
        // Collect pitches measured from the underlying glyphs of alteration items
        Double[] mPitches = new Double[slices.size()];

        for (int i = 0; i < slices.size(); i++) {
            KeyAlterInter alter = slices.get(i).getAlter();
            mPitches[i] = (alter != null) ? alter.getMeasuredPitch() : null;
        }

        // Guess clef kind from pattern of measured pitches
        Map<ClefKind, Double> results = new EnumMap<ClefKind, Double>(ClefKind.class);
        ClefKind guess = ClefInter.guessKind(keyShape, mPitches, results);

        // (Slightly) adjust pitches if needed
        int[] stdPitches = (keyShape == Shape.SHARP) ? ClefInter.sharpsMap.get(guess)
                : ClefInter.flatsMap.get(guess);

        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            KeyAlterInter alter = slice.getAlter();

            if (alter != null) {
                int std = stdPitches[i];

                if (alter.getIntegerPitch() != std) {
                    logger.info(
                            "{}Staff#{} slice index:{} pitch adjusted from {} to {}",
                            sheet.getLogPrefix(),
                            staff.getId(),
                            i,
                            alter.getIntegerPitch(),
                            std);
                    alter.setPitch(std);
                }
            } else {
                logger.info("null alter");
            }
        }

        // Create key inter
        createKeyInter();

        // Compare clef(s) candidates and key signature for this staff
        checkWithClefs(guess, results);

        //TODO: Boost key components. This is a hack
        // Perhaps we could simply remove the key alters from the sig, and now play with the key
        // as an ensemble instead.
        for (Slice slice : slices) {
            KeyAlterInter alter = slice.getAlter();

            if (alter != null) {
                alter.increase(0.25); ///////////// !!!!!!!!!!!!!!!!!!
            }
        }

        // Record slices starts in StaffHeader structure
        if (!slices.isEmpty()) {
            List<Integer> starts = new ArrayList<Integer>();

            for (Slice slice : slices) {
                starts.add(slice.getRect().x);
            }

            staff.getHeader().alterStarts = starts;
        }

        // Adjust key-sig stop for this staff
        for (int i = slices.size() - 1; i >= 0; i--) {
            KeyBuilder.Slice slice = slices.get(i);
            KeyAlterInter inter = slice.getAlter();

            if (inter != null) {
                Rectangle bounds = inter.getBounds();
                int end = (bounds.x + bounds.width) - 1;
                staff.setKeyStop(end);

                break;
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

    //-----------//
    // getFifths //
    //-----------//
    /**
     * Staff key signature is dynamically computed using the keyShape and the count of
     * alteration slices.
     *
     * @return the signature as an int
     */
    public int getFifths ()
    {
        if (slices.isEmpty()) {
            return 0;
        }

        if (keyShape == Shape.SHARP) {
            return slices.size();
        } else {
            return -slices.size();
        }
    }

    //-------//
    // getId //
    //-------//
    public int getId ()
    {
        return staff.getId();
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

    //-----------//
    // getSlices //
    //-----------//
    /**
     * @return the slices
     */
    public List<Slice> getSlices ()
    {
        return slices;
    }

    //-------------//
    // insertSlice //
    //-------------//
    /**
     * Insert a slice at provided index.
     *
     * @param index             provided index
     * @param theoreticalOffset theoretical offset WRT measure start
     */
    public void insertSlice (int index,
                             int theoreticalOffset)
    {
        Slice nextSlice = slices.get(index);
        Slice slice = createSlice(measureStart + theoreticalOffset, nextSlice.getRect().x - 1);
        slices.add(index, slice);

        // Reassign staff slice attachments
        for (int i = 0; i < slices.size(); i++) {
            staff.addAttachment(Integer.toString(i + 1), slices.get(i).getRect());
        }

        // Process this new slice and try to assign a valid alter.
        extractAlter(slice, Collections.singleton(keyShape), Grades.keyAlterMinGrade2);
    }

    //---------//
    // process //
    //---------//
    /**
     * Process the potential key signature of the assigned staff.
     */
    public void process ()
    {
        logger.debug("Key processing for S#{} staff#{}", system.getId(), staff.getId());

        // Retrieve spaces and peaks
        browseArea();

        // Infer signature
        int signature = retrieveSignature();

        if (signature != 0) {
            // Check/modify signature
            signature = checkSignature(signature);
        }

        if (signature != 0) {
            // Compute precise borders for each sig item
            List<Integer> starts = computeBorders(signature);

            if (!starts.isEmpty()) {
                // Extract and evaluate item-based glyphs
                extractSlices(starts);
            } else {
                signature = 0;
            }
        }

        // Normalized area weight
        if (!peaks.isEmpty()) {
            KeyEvent.Peak lastPeak = peaks.get(peaks.size() - 1);

            if (lastPeak.isInvalid() && (events.size() > 1)) {
                KeyEvent event = events.get(events.size() - 2);

                if (event instanceof KeyEvent.Space) {
                    KeyEvent.Space space = (KeyEvent.Space) event;
                    areaWeight = space.getWeight();
                }
            }
        }

        double nw = scale.pixelsToAreaFrac(areaWeight);
        logger.debug(
                "Staff#{} sig:{} meas:{} start:{} stop:{} normWeight:{} {}",
                staff.getId(),
                signature,
                measureStart,
                range.start,
                range.stop,
                String.format("%.1f", nw),
                events);
    }

    //-----------//
    // reprocess //
    //-----------//
    /**
     * Re-launch the processing, using an updated browsing abscissa.
     *
     * @param browseStart new browsing abscissa
     */
    public void reprocess (int browseStart)
    {
        range.browseStart = browseStart;
        reset();
        process();
    }

    //-----------//
    // scanSlice //
    //-----------//
    /**
     * Inspect the provided range and try to define a slice there.
     * <p>
     * The slice is inserted only if significant pixels can be found, and an alter is assigned to
     * the slice if classification is successful.
     *
     * @param start start of range abscissa
     * @param stop  stop of range abscissa
     * @return the created slice if any, with its slice.alter if successful
     */
    public Slice scanSlice (int start,
                            int stop)
    {
        if (isRangeVoid(start, stop)) {
            // Nothing interesting there
            return null;
        } else {
            Slice slice = createSlice(start, stop);
            slices.add(slice);

            // Append to staff slice attachments
            staff.addAttachment(Integer.toString(slices.size()), slice.getRect());

            // Process this new slice
            Set<Shape> shapes = (keyShape != null) ? Collections.singleton(keyShape) : keyShapes;
            KeyAlterInter alter = extractAlter(slice, shapes, Grades.keyAlterMinGrade2);

            if (alter != null) {
                keyShape = alter.getShape();
            }

            return slice;
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "KeyBuilder#" + staff.getId();
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
        // Maximum abscissa range to be browsed
        final int xMin = range.browseStart;
        final int xMax = range.browseStop;

        // Space parameters
        int maxSpaceCumul = params.maxSpaceCumul;
        int spaceStart = -1; // Space start abscissa
        int spaceStop = -1; // Space stop abscissa

        // Peak parameters
        final int minPeakCumul = params.minPeakCumul;
        int peakStart = -1; // Peak start abscissa
        int peakStop = -1; // Peak stop abscissa
        int peakHeight = 0; // Peak height

        for (int x = xMin; x <= xMax; x++) {
            int cumul = projection.getValue(x);

            // For peak
            if (cumul >= minPeakCumul) {
                if (spaceStart != -1) {
                    // End of space
                    if (!createSpace(spaceStart, spaceStop)) {
                        // Too wide space encountered
                        return;
                    }

                    spaceStart = -1;
                }

                if (peakStart == -1) {
                    // Beginning of peak
                    peakStart = x;
                }

                peakStop = x;
                peakHeight = Math.max(peakHeight, cumul);
            } else {
                if (peakStart != -1) {
                    // End of peak
                    if (!createPeak(peakStart, peakStop, peakHeight)) {
                        // Invalid peak encountered
                        return;
                    }

                    peakStart = -1;
                    peakHeight = 0;
                }

                // For space
                if (cumul <= maxSpaceCumul) {
                    // Below threshold, we are in a space
                    if (spaceStart == -1) {
                        // Start of space
                        spaceStart = x;
                    }

                    spaceStop = x;
                } else {
                    if (spaceStart != -1) {
                        // End of space
                        if (!createSpace(spaceStart, spaceStop)) {
                            // Too wide space encountered
                            return;
                        }

                        spaceStart = -1;
                    }
                }
            }

            // Update area weight?
            if (range.start != 0) {
                areaWeight += cumul;
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
     * Additional tests on key-sig, which may get adjusted.
     *
     * @return the signature value, perhaps modified
     */
    private int checkSignature (int signature)
    {
        // Case of final invalid peak
        KeyEvent.Peak lastPeak = peaks.get(peaks.size() - 1);

        if (lastPeak.isInvalid()) {
            // Where is the precise end of key-sig?
            // Check x delta between previous (good) peak and this one
            range.stop = lastPeak.start - 1;

            KeyEvent.Peak goodPeak = lastGoodPeak();
            int trail = range.stop - goodPeak.start + 1;

            // Check trailing length
            if (signature < 0) {
                if (trail < params.minFlatTrail) {
                    logger.debug("Removing too narrow flat");
                    signature += 1;
                }
            } else {
                if (trail < params.minSharpTrail) {
                    logger.debug("Removing too narrow sharp");
                    signature -= 1;
                }
            }
        }

        return signature;
    }

    //----------------//
    // checkWithClefs //
    //----------------//
    private void checkWithClefs (ClefKind guess,
                                 Map<ClefKind, Double> results)
    {
        // Look for clef on left side in staff (together with its competing clefs)
        List<Inter> staffClefs = sig.inters(staff, ClefInter.class);
        Collections.sort(staffClefs, Inter.byAbscissa);

        int res = Collections.binarySearch(staffClefs, keyInter, Inter.byAbscissa);
        int indexClef = -res - 2;

        if (indexClef >= 0) {
            ClefInter lastClef = (ClefInter) staffClefs.get(indexClef);
            Set<Relation> excs = sig.getExclusions(lastClef);
            List<ClefInter> clefs = new ArrayList<ClefInter>();

            for (Relation rel : excs) {
                Inter inter = Graphs.getOppositeVertex(sig, rel, lastClef);

                if (inter instanceof ClefInter) {
                    clefs.add((ClefInter) inter);
                }
            }

            clefs.add(lastClef);

            logger.debug(
                    "staffClefs: {} index:{} lastClef:{} set:{}",
                    staffClefs,
                    indexClef,
                    lastClef,
                    clefs);

            for (Iterator<ClefInter> it = clefs.iterator(); it.hasNext();) {
                ClefInter clef = it.next();

                if (clef.getKind() == guess) {
                    sig.addEdge(clef, keyInter, new ClefKeyRelation());
                } else {
                    sig.insertExclusion(clef, keyInter, Exclusion.Cause.INCOMPATIBLE);
                    ///clef.delete();
                    it.remove();
                }
            }
        }
    }

    //----------------//
    // computeBorders //
    //----------------//
    /**
     * Compute the precise abscissae that represent the borders of key-sig items.
     */
    private List<Integer> computeBorders (int signature)
    {
        List<Integer> starts = new ArrayList<Integer>();

        if (signature > 0) {
            // Sharps
            starts.add(range.start);

            for (int i = 2; i < peaks.size(); i += 2) {
                KeyEvent.Peak peak = peaks.get(i);

                if (peak.isInvalid()) {
                    break;
                }

                starts.add((int) Math.ceil(0.5 * (peak.start + peaks.get(i - 1).stop)));
            }

            // End of area
            refineStop(lastGoodPeak(), params.sharpTrail, params.maxSharpTrail);
        } else if (signature < 0) {
            // Flats
            KeyEvent.Peak firstPeak = peaks.get(0);

            //            // Check derivative at first peak (TODO: just part of impacts)
            //            int der = maxDerivative(firstPeak);
            //
            //            if (der < params.minFlatDerivative) {
            //                logger.info("Too low derivative of first flat peak");
            //
            //                return starts;
            //            }
            //
            // Start of area, make sure there is nothing right before first peak
            int flatHeading = ((firstPeak.start + firstPeak.stop) / 2) - range.start;

            if (flatHeading > params.maxFlatHeading) {
                logger.debug("Too large heading before first flat peak");

                return starts;
            }

            starts.add(range.start);

            for (int i = 1; i < peaks.size(); i++) {
                KeyEvent.Peak peak = peaks.get(i);

                if (peak.isInvalid()) {
                    break;
                }

                starts.add(peak.start);
            }

            // End of area
            refineStop(lastGoodPeak(), params.flatTrail, params.maxFlatTrail);
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

        for (Slice slice : slices) {
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

        // Grade: all alters in a key-sig support each other
        for (int i = 0; i < alters.size(); i++) {
            KeyAlterInter alter = alters.get(i);

            for (KeyAlterInter sibling : alters.subList(i + 1, alters.size())) {
                sig.addEdge(alter, sibling, new KeyAltersRelation());
            }
        }

        double grade = 0;

        for (KeyAlterInter alter : alters) {
            grade += sig.computeContextualGrade(alter, false);
        }

        grade /= alters.size();

        keyInter = new KeyInter(box, grade, getFifths(), alters);
        keyInter.setStaff(staff);
        sig.addVertex(keyInter);
        staff.getHeader().key = keyInter;
    }

    //------------//
    // createPeak //
    //------------//
    /**
     * (Try to) create a peak for a candidate alteration item.
     * The peak is checked for its height and its width.
     *
     * @param start  start abscissa
     * @param stop   stop abscissa
     * @param height peak height
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
            // We may have an interesting peak, check distance since previous peak
            KeyEvent.Peak prevPeak = peaks.isEmpty() ? null : peaks.get(peaks.size() - 1);

            if (prevPeak != null) {
                // Check delta abscissa
                double x = (start + stop) / 2.0;
                double dx = x - ((prevPeak.start + prevPeak.stop) / 2.0);

                if (dx > params.maxPeakDx) {
                    // A large dx indicates we are beyond end of key-sig
                    logger.debug("Too large delta since previous peak");
                    peak.setInvalid();
                    keepOn = false;
                }
            } else {
                // Very first peak, check offset from theoretical start
                // TODO: this is too strict, check emptyness in previous abscissae
                int offset = start - range.browseStart;

                if (offset > params.maxFirstPeakOffset) {
                    logger.debug("First peak arrives too late");
                    peak.setInvalid();
                    keepOn = false;
                } else if (range.start == 0) {
                    // Set range.start at beginning of browsing, since no space was found before peak
                    range.start = range.browseStart;
                }
            }
        }

        events.add(peak);
        peaks.add(peak);

        return keepOn;
    }

    //-------------//
    // createSlice //
    //-------------//
    private Slice createSlice (int start,
                               int stop)
    {
        Rectangle rect = new Rectangle(start, areaTop, stop - start + 1, areaBottom - areaTop + 1);

        return new Slice(rect);
    }

    //-------------//
    // createSpace //
    //-------------//
    /**
     * (Try to) create a new space between items. (clef, alterations, time-sig, ...)
     *
     * @param spaceStart start space abscissa
     * @param spaceStop  stop space abscissa
     * @return true to keep browsing, false to stop immediately
     */
    private boolean createSpace (int spaceStart,
                                 int spaceStop)
    {
        boolean keepOn = true;
        KeyEvent.Space space = new KeyEvent.Space(spaceStart, spaceStop, areaWeight);

        if (range.start == 0) {
            // This is the very first space found
            if (space.getWidth() > params.maxFirstSpaceWidth) {
                // No key signature!
                logger.debug("Staff#{} no key signature.", staff.getId());
                space.setWide();
                keepOn = false;
            } else {
                // Set range.start here, since first chunk may be later skipped if lacking peak
                range.start = space.stop + 1;
            }
        } else {
            // Make sure we have some item
            if (peaks.isEmpty()) {
                range.start = space.stop + 1;
            } else {
                // Second (wide) space stops it
                if (space.getWidth() > params.maxInnerSpace) {
                    range.stop = space.start;
                    space.setWide();
                    keepOn = false;
                }
            }
        }

        events.add(space);

        return keepOn;
    }

    //--------------//
    // extractAlter //
    //--------------//
    /**
     * Using the provided abscissa range, extract the relevant foreground pixels
     * from the NO_STAFF image and evaluate possible glyph instances.
     *
     * @param slice        the slice to process
     * @param targetShapes the set of shapes to try
     * @param minGrade     minimum acceptable grade
     * @return the Inter created if any
     */
    private KeyAlterInter extractAlter (Slice slice,
                                        Set<Shape> targetShapes,
                                        double minGrade)
    {
        Rectangle rect = slice.getRect();
        ByteProcessor buf = new ByteProcessor(rect.width, rect.height);
        buf.copyBits(staffFreeSource, -rect.x, -rect.y, Blitter.COPY);

        RunTable runTable = new RunTableFactory(VERTICAL).createTable(buf);
        List<Glyph> glyphs = GlyphFactory.buildGlyphs(runTable, rect.getLocation());

        purgeGlyphs(glyphs, rect);

        KeyAdapter adapter = new KeyAdapter(glyphs, targetShapes);
        new GlyphCluster(adapter, null).decompose();

        if (adapter.bestEval != null) {
            double grade = Inter.intrinsicRatio * adapter.bestEval.grade;

            if (grade >= minGrade) {
                ///sheet.getGlyphIndex().registerStandaloneGlyph(adapter.bestGlyph);
                logger.debug("Glyph#{} {}", adapter.bestGlyph.getId(), adapter.bestEval);

                KeyAlterInter alterInter = KeyAlterInter.create(
                        adapter.bestGlyph,
                        adapter.bestEval.shape,
                        grade,
                        staff);

                if (alterInter != null) {
                    alterInter.setGlyph(sheet.getGlyphIndex().registerOriginal(adapter.bestGlyph));
                    sig.addVertex(alterInter);
                    slice.alter = alterInter;

                    return alterInter;
                }
            }
        }

        return null;
    }

    //---------------//
    // extractSlices //
    //---------------//
    /**
     * Using the starting mark found for each alteration item, extract each vertical
     * slice and build alteration inter out of each slice.
     *
     * @param starts sequence of starting abscissae
     */
    private void extractSlices (List<Integer> starts)
    {
        final int count = starts.size();

        for (int i = 0; i < count; i++) {
            int start = starts.get(i);
            int stop = (i < (count - 1)) ? (starts.get(i + 1) - 1) : range.stop;
            Slice slice = createSlice(start, stop);

            KeyAlterInter inter = extractAlter(
                    slice,
                    Collections.singleton(keyShape),
                    Grades.keyAlterMinGrade);

            if (inter != null) {
                slices.add(slice);
                staff.addAttachment(Integer.toString(i + 1), slice.getRect());
            }
        }
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
                logger.debug("Staff#{} stopping key search before {}", staff.getId(), bar);
                end = barStart - 1;

                break;
            }
        }

        return end;
    }

    //---------------//
    // getProjection //
    //---------------//
    /**
     * Cumulate the foreground pixels for each abscissa value in the lookup area.
     * <p>
     * The lookup area must embrace all possible key signatures, whatever the staff clef, so it
     * goes from first line to last line of staff, augmented of 2 interline value above and 1
     * interline value below.
     *
     * @return the populated cumulation table
     */
    private Projection getProjection ()
    {
        final int xMin = Math.max(0, measureStart - params.preStaffMargin);
        final int xMax = range.browseStop;

        Projection table = new Projection.Short(xMin, xMax);

        for (int x = xMin; x <= xMax; x++) {
            areaTop = Math.min(
                    areaTop,
                    staff.getFirstLine().yAt(xMin) - (2 * scale.getInterline()));
            areaBottom = Math.max(
                    areaBottom,
                    staff.getLastLine().yAt(xMin) + (1 * scale.getInterline()));

            short cumul = 0;

            for (int y = areaTop; y <= areaBottom; y++) {
                if (staffFreeSource.get(x, y) == 0) {
                    cumul++;
                }
            }

            table.increment(x, cumul);
        }

        return table;
    }

    //-------------//
    // isRangeVoid //
    //-------------//
    /**
     * Check whether the provided abscissa range is void (no significant chunk is found
     * above lines cumulated pixels).
     *
     * @param start range start abscissa
     * @param stop  range stop abscissa
     * @return true if void, false if not void
     */
    private boolean isRangeVoid (int start,
                                 int stop)
    {
        int meanHeight = 0;
        int count = 0;

        for (int x = start; x <= stop; x++) {
            int cumul = projection.getValue(x);

            if (cumul > 0) {
                count++;
                meanHeight += cumul;
            }
        }

        if (count > 0) {
            meanHeight = (int) Math.rint(meanHeight / (double) count);
            logger.debug("isRangeVoid start:{} stop:{} meanHeight:{}", start, stop, meanHeight);
        }

        // TODO: use a specific constant?
        return meanHeight <= (params.maxSpaceCumul / 2);
    }

    //--------------//
    // lastGoodPeak //
    //--------------//
    /**
     * Report the last valid peak found
     *
     * @return the last valid peak, if any
     */
    private KeyEvent.Peak lastGoodPeak ()
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

    //---------------//
    // maxDerivative //
    //---------------//
    /**
     * Report the maximum derivative value over a peak range
     *
     * @param peak the peak to measure
     * @return the highest derivative value
     */
    private int maxDerivative (KeyEvent.Peak peak)
    {
        int der = 0;

        for (int x = peak.start - 1; x <= peak.stop; x++) {
            der = Math.max(der, projection.getDerivative(x));
        }

        return der;
    }

    //-------------//
    // purgeGlyphs //
    //-------------//
    /**
     * Purge the population of glyph candidates as much as possible, since the cost
     * of their later combinations is worse than exponential.
     * <p>
     * Those of width 1 and stuck on right side of slice can be safely removed, since they
     * certainly belong to the stem of the next slice.
     * <p>
     * Those composed of just one (isolated) pixel are also removed, although this is more
     * questionable.
     *
     * @param glyphs the collection to purge
     * @param rect   the slice rectangle
     */
    private void purgeGlyphs (List<Glyph> glyphs,
                              Rectangle rect)
    {
        //TODO: use constants!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        final int xMax = (rect.x + rect.width) - 1;
        final int minWeight = 2;

        List<Glyph> toRemove = new ArrayList<Glyph>();

        for (Glyph glyph : glyphs) {
            if ((glyph.getWeight() < minWeight) || (glyph.getBounds().x == xMax)) {
                toRemove.add(glyph);
            }
        }

        if (!toRemove.isEmpty()) {
            glyphs.removeAll(toRemove);
        }
    }

    //------------//
    // refineStop //
    //------------//
    /**
     * Adjust the stop abscissa of key sig.
     *
     * @param lastGoodPeak last valid peak found
     * @param typicalTrail typical length after last peak (this depends on alter shape)
     * @param maxTrail     maximum length after last peak
     */
    private void refineStop (KeyEvent.Peak lastGoodPeak,
                             int typicalTrail,
                             int maxTrail)
    {
        final int xMin = (lastGoodPeak.start + typicalTrail) - 1;
        final int xMax = Math.min(projection.getStop(), lastGoodPeak.start + maxTrail);

        int minCount = Integer.MAX_VALUE;

        for (int x = xMin; x <= xMax; x++) {
            int count = projection.getValue(x);

            if (count < minCount) {
                range.stop = x - 1;
                minCount = count;
            }
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reset the class relevant parameters, so that a new browsing can be launched.
     */
    private void reset ()
    {
        for (Slice slice : slices) {
            KeyAlterInter alter = slice.getAlter();

            if (alter != null) {
                alter.delete();
            }
        }

        peaks.clear();
        events.clear();
        slices.clear();

        range.start = 0;
        range.stop = 0;
        areaWeight = 0;
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
        if (peaks.isEmpty()) {
            return 0;
        }

        int last = peaks.size() - 1;

        if (peaks.get(last).isInvalid()) {
            if (last > 0) {
                last--;
            } else {
                logger.debug("no valid peak");

                return 0;
            }
        }

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
            int offset = peaks.get(0).start - range.start;

            if (meanShort < params.minFlatDelta) {
                keyShape = Shape.SHARP;
            } else if (meanShort > params.maxSharpDelta) {
                keyShape = Shape.FLAT;
            } else {
                keyShape = (offset > params.offsetThreshold) ? Shape.SHARP : Shape.FLAT;
            }

            //                logger.info(
            //                        String.format(
            //                                "%s delta:%.1f minFlat:%.1f maxSharp:%.1f offset:%.2f vs %.2f",
            //                                shape,
            //                                scale.pixelsToFrac(meanShort),
            //                                constants.minFlatDelta.getValue(),
            //                                constants.maxSharpDelta.getValue(),
            //                                scale.pixelsToFrac(offset),
            //                                constants.offsetThreshold.getValue()));
            //
            //TODO: for sharps, peaks count should be an even number
            return (keyShape == Shape.SHARP) ? ((last + 1) / 2) : (-(last + 1));
        } else {
            keyShape = Shape.FLAT;

            return -1;
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Column //
    //--------//
    /**
     * Manages the system consistency for a column of staves KeyBuilder instances.
     */
    public static class Column
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final SystemInfo system;

        /** Scale-dependent parameters. */
        private final Parameters params;

        /** Map of key builders. (one per staff) */
        private final Map<Staff, KeyBuilder> builders = new TreeMap<Staff, KeyBuilder>(Staff.byId);

        /** Theoretical abscissa offset for each slice. */
        private List<Integer> globalOffsets;

        //~ Constructors ---------------------------------------------------------------------------
        public Column (SystemInfo system)
        {
            this.system = system;

            params = new Parameters(system.getSheet().getScale());
        }

        //~ Methods --------------------------------------------------------------------------------
        //---------//
        // addPlot //
        //---------//
        public String addPlot (Plotter plotter,
                               Staff staff)
        {
            int measureStart = staff.getHeaderStart();
            KeyBuilder builder = new KeyBuilder(staff, 0, measureStart, 0, true);

            builder.addPlot(plotter);

            KeyInter key = staff.getHeader().key;

            return (key != null) ? ("key:" + key.getFifths()) : null;
        }

        //--------------//
        // retrieveKeys //
        //--------------//
        /**
         * Retrieve the column of staves keys.
         *
         * @param projectionWidth desired width for projection
         * @return the ending abscissa offset of keys column WRT measure start
         */
        public int retrieveKeys (int projectionWidth)
        {
            // Define each staff key-sig area
            for (Staff staff : system.getStaves()) {
                int measureStart = staff.getHeaderStart();
                int clefStop = staff.getClefStop();
                int browseStart = (clefStop != 0) ? ((clefStop + staff.getHeaderStop()) / 2)
                        : staff.getHeaderStop();

                builders.put(
                        staff,
                        new KeyBuilder(staff, projectionWidth, measureStart, browseStart, true));
            }

            // Process each staff separately
            for (KeyBuilder builder : builders.values()) {
                builder.process();
            }

            // Make sure non-empty key areas do have keys
            //TODO
            //
            // Check keys alignment at system level, if applicable
            if (system.getStaves().size() > 1) {
                checkKeysAlignment();
            }

            // Adjust each individual alter pitch, according to best matching key-sig
            for (KeyBuilder builder : builders.values()) {
                builder.adjustPitches();
            }

            // Push StaffHeader
            int maxKeyOffset = 0;

            for (Staff staff : system.getStaves()) {
                int measureStart = staff.getHeaderStart();
                Integer keyStop = staff.getKeyStop();

                if (keyStop != null) {
                    maxKeyOffset = Math.max(maxKeyOffset, keyStop - measureStart);
                }
            }

            return maxKeyOffset;
        }

        //--------------------//
        // checkKeysAlignment //
        //--------------------//
        /**
         * Verify vertical alignment of keys within the same system.
         */
        private void checkKeysAlignment ()
        {
            final Sheet sheet = system.getSheet();

            // Get theoretical abscissa offset for each slice in the system
            int meanSliceWidth = getGlobalOffsets();

            // Missing initial slices?
            // Check that each initial inter is located at proper offset
            for (KeyBuilder builder : builders.values()) {
                for (int i = 0; i < builder.getSlices().size(); i++) {
                    int x = builder.getSlices().get(i).getRect().x;
                    int offset = x - builder.getMeasureStart();
                    Integer index = getBestSliceIndex(offset);

                    if (index != null) {
                        if (index > i) {
                            // Insert missing slice!
                            logger.debug(
                                    "{}Staff#{} slice inserted at index:{}",
                                    sheet.getLogPrefix(),
                                    builder.getId(),
                                    i);
                            builder.insertSlice(i, globalOffsets.get(i));
                        }
                    } else {
                        // Slice too far on left
                        logger.debug(
                                "{}Staff#{} misaligned slice index:{} x:{}",
                                sheet.getLogPrefix(),
                                builder.getId(),
                                i,
                                x);

                        int newStart = builder.getMeasureStart() + globalOffsets.get(0);
                        Integer browseStart = builder.getBrowseStart();

                        if (browseStart != null) {
                            newStart = (browseStart + newStart) / 2; // Safer
                        }

                        builder.reprocess(newStart);
                    }
                }
            }

            // Missing trailing slices?
            for (KeyBuilder builder : builders.values()) {
                List<KeyBuilder.Slice> slices = builder.getSlices();

                if (slices.size() < globalOffsets.size()) {
                    for (int i = slices.size(); i < globalOffsets.size(); i++) {
                        int x = (builder.getMeasureStart() + globalOffsets.get(i)) - 1;
                        logger.debug(
                                "Staff#{} Should investigate slice {} at {}",
                                builder.getId(),
                                i,
                                x);

                        Slice slice = builder.scanSlice(x, (x + meanSliceWidth) - 1);

                        if (slice == null) {
                            // Nothing found at all, so stop slice sequence here
                            break;
                        }
                    }
                }
            }

            // At this point in the system column, all relevant slices (slices with some pixels)
            // have been allocated, but some slices may have no recognized alter assigned.
            // Perhaps a phase #3 with relaxed params could be tried? TODO
            for (KeyBuilder builder : builders.values()) {
                List<KeyBuilder.Slice> slices = builder.getSlices();

                for (int i = 0; i < slices.size(); i++) {
                    Slice slice = slices.get(i);
                    Inter inter = slice.getAlter();

                    if (inter == null) {
                        logger.info("Staff#{} weird key slice at index {}", builder.getId(), i);
                    }
                }
            }
        }

        //-------------------//
        // getBestSliceIndex //
        //-------------------//
        private Integer getBestSliceIndex (int offset)
        {
            Integer bestIndex = null;
            double bestDist = Double.MAX_VALUE;

            for (int i = 0; i < globalOffsets.size(); i++) {
                int gOffset = globalOffsets.get(i);
                double dist = Math.abs(gOffset - offset);

                if (bestDist > dist) {
                    bestDist = dist;
                    bestIndex = i;
                }
            }

            if (bestDist < params.maxSliceDist) {
                return bestIndex;
            } else {
                return null;
            }
        }

        //------------------//
        // getGlobalOffsets //
        //------------------//
        /**
         * Retrieve the theoretical offset abscissa for all slices in the system.
         *
         * @return the mean slice width
         */
        private int getGlobalOffsets ()
        {
            int sliceCount = 0;
            int meanSliceWidth = 0;

            // Check that key-sig items appear vertically aligned between staves
            List<Population> pops = new ArrayList<Population>();
            List<Double> vals = new ArrayList<Double>();

            for (KeyBuilder builder : builders.values()) {
                ///StringBuilder sb = new StringBuilder();
                ///sb.append("S#").append(projector.staff.getId());
                for (int i = 0; i < builder.getSlices().size(); i++) {
                    KeyBuilder.Slice slice = builder.getSlices().get(i);
                    sliceCount++;
                    meanSliceWidth += slice.getRect().width;

                    int x = slice.getRect().x;
                    int offset = x - builder.getMeasureStart();

                    ///sb.append(" ").append(i).append(":").append(offset);
                    final Population pop;

                    if (i >= pops.size()) {
                        pops.add(new Population());
                    }

                    pop = pops.get(i);
                    pop.includeValue(offset);
                    vals.add((double) offset);
                }

                ///logger.debug(sb.toString());
            }

            int G = pops.size();
            Clustering.Gaussian[] laws = new Clustering.Gaussian[G];

            for (int i = 0; i < G; i++) {
                Population pop = pops.get(i);
                laws[i] = new Clustering.Gaussian(pop.getMeanValue(), 1.0); //pop.getStandardDeviation());
            }

            double[] table = new double[vals.size()];

            for (int i = 0; i < vals.size(); i++) {
                table[i] = vals.get(i);
            }

            double[] pi = Clustering.EM(table, laws);

            List<Integer> theoreticals = new ArrayList<Integer>();

            for (int k = 0; k < G; k++) {
                Clustering.Gaussian law = laws[k];
                theoreticals.add((int) Math.rint(law.getMean()));
            }

            globalOffsets = theoreticals;

            if (sliceCount > 0) {
                meanSliceWidth = (int) Math.rint(meanSliceWidth / (double) sliceCount);
            }

            logger.debug("globalOffsets:{} meanSliceWidth:{}", globalOffsets, meanSliceWidth);

            return meanSliceWidth;
        }
    }

    //-------//
    // Slice //
    //-------//
    /**
     * Represents a rectangular slice of a key-sig, likely to contain an alteration item.
     */
    public static class Slice
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Rectangular slice definition. */
        private final Rectangle rect;

        /** Retrieved alter item, if any. */
        private KeyAlterInter alter;

        //~ Constructors ---------------------------------------------------------------------------
        public Slice (Rectangle rect)
        {
            this.rect = rect;
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * @return the rectangle definition
         */
        public Rectangle getRect ()
        {
            return rect;
        }

        /**
         * @return the alter, if any
         */
        KeyAlterInter getAlter ()
        {
            return alter;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction maxSliceDist = new Scale.Fraction(
                0.5,
                "Maximum x distance to theoretical slice");

        private final Scale.LineFraction maxSpaceCumul = new Scale.LineFraction(
                2.0,
                "Maximum cumul value in space (specified WRT staff line thickness)");

        private final Scale.Fraction typicalAlterationHeight = new Scale.Fraction(
                3.0,
                "Typical alteration height (flat or sharp)");

        private final Constant.Ratio peakHeightRatio = new Constant.Ratio(
                0.5,
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

        private final Scale.Fraction minFlatDerivative = new Scale.Fraction(
                0.8,
                "Minimum derivative at peak for a flat item");

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

        private final Scale.Fraction minFirstSpaceWidth = new Scale.Fraction(
                0.2,
                "Minimum initial space before key signature");

        private final Scale.Fraction maxGlyphGap = new Scale.Fraction(
                1.5,
                "Maximum distance between two glyphs of a single alter symbol");

        private final Scale.Fraction maxGlyphHeight = new Scale.Fraction(
                3.5,
                "Maximum glyph height");

        private final Scale.AreaFraction minGlyphWeight = new Scale.AreaFraction(
                0.3,
                "Minimum glyph weight");

        // Beware: A too small value might miss the whole key-sig
        private final Scale.Fraction maxFirstSpaceWidth = new Scale.Fraction(
                1.75,
                "Maximum initial space before key signature");

        // Beware: A too small value might miss some key-sig items
        private final Scale.Fraction maxInnerSpace = new Scale.Fraction(
                0.7,
                "Maximum inner space within key signature");
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final int maxSliceDist;

        final int preStaffMargin;

        final int maxFirstPeakOffset;

        final int minFirstSpaceWidth;

        final int maxFirstSpaceWidth;

        final int maxInnerSpace;

        final int minPeakCumul;

        final int maxSpaceCumul;

        final int maxPeakCumul;

        final int maxPeakWidth;

        final int minFlatDerivative;

        final int maxFlatHeading;

        final int flatTrail;

        final int minFlatTrail;

        final int maxFlatTrail;

        final int sharpTrail;

        final int minSharpTrail;

        final int maxSharpTrail;

        final int maxPeakDx;

        final double maxSharpDelta;

        final double minFlatDelta;

        final double offsetThreshold;

        final double maxGlyphGap;

        final double maxGlyphHeight;

        final int minGlyphWeight;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxSliceDist = scale.toPixels(constants.maxSliceDist);
            preStaffMargin = scale.toPixels(constants.preStaffMargin);
            maxFirstPeakOffset = scale.toPixels(constants.maxFirstPeakOffset);
            minFirstSpaceWidth = scale.toPixels(constants.minFirstSpaceWidth);
            maxFirstSpaceWidth = scale.toPixels(constants.maxFirstSpaceWidth);
            maxInnerSpace = scale.toPixels(constants.maxInnerSpace);
            maxSpaceCumul = scale.toPixels(constants.maxSpaceCumul);
            maxPeakCumul = scale.toPixels(constants.maxPeakCumul);
            maxPeakWidth = scale.toPixels(constants.maxPeakWidth);
            minFlatDerivative = scale.toPixels(constants.minFlatDerivative);
            maxFlatHeading = scale.toPixels(constants.maxFlatHeading);
            flatTrail = scale.toPixels(constants.flatTrail);
            minFlatTrail = scale.toPixels(constants.minFlatTrail);
            maxFlatTrail = scale.toPixels(constants.maxFlatTrail);
            sharpTrail = scale.toPixels(constants.sharpTrail);
            minSharpTrail = scale.toPixels(constants.minSharpTrail);
            maxSharpTrail = scale.toPixels(constants.maxSharpTrail);
            maxPeakDx = scale.toPixels(constants.maxPeakDx);
            maxSharpDelta = scale.toPixelsDouble(constants.maxSharpDelta);
            minFlatDelta = scale.toPixelsDouble(constants.minFlatDelta);
            offsetThreshold = scale.toPixelsDouble(constants.offsetThreshold);
            maxGlyphGap = scale.toPixelsDouble(constants.maxGlyphGap);
            maxGlyphHeight = scale.toPixelsDouble(constants.maxGlyphHeight);
            minGlyphWeight = scale.toPixels(constants.minGlyphWeight);

            // Maximum alteration contribution (on top of staff lines)
            double maxAlterContrib = constants.typicalAlterationHeight.getValue() * (scale.getInterline()
                                                                                     - scale.getMainFore());
            minPeakCumul = (int) Math.rint(
                    (5 * scale.getMainFore())
                    + (constants.peakHeightRatio.getValue() * maxAlterContrib));
        }
    }

    //-------------//
    // KeyAdapter //
    //-------------//
    /**
     * Handles the integration between glyph clustering class and key-sig environment.
     */
    private class KeyAdapter
            implements GlyphCluster.Adapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Graph of the connected glyphs, with their distance edges if any. */
        private final SimpleGraph<Glyph, GlyphLink> graph;

        /** Relevant shapes. */
        private final EnumSet<Shape> targetShapes = EnumSet.noneOf(Shape.class);

        private Evaluation bestEval = null;

        private Glyph bestGlyph = null;

        private int trials = 0; // (debug)

        //~ Constructors ---------------------------------------------------------------------------
        public KeyAdapter (List<Glyph> glyphs,
                           Set<Shape> targetShapes)
        {
            this.targetShapes.addAll(targetShapes);
            graph = Glyphs.buildLinks(glyphs, params.maxGlyphGap);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void evaluateGlyph (Glyph glyph)
        {
            trials++;

            Evaluation[] evals = evaluator.getNaturalEvaluations(glyph, sheet.getInterline());

            for (Shape shape : targetShapes) {
                Evaluation eval = evals[shape.ordinal()];

                if ((bestEval == null) || (bestEval.grade < eval.grade)) {
                    bestEval = eval;
                    bestGlyph = glyph;
                }
            }
        }

        @Override
        public List<Glyph> getNeighbors (Glyph part)
        {
            return Graphs.neighborListOf(graph, part);
        }

        @Override
        public GlyphIndex getNest ()
        {
            return sheet.getGlyphIndex();
        }

        @Override
        public List<Glyph> getParts ()
        {
            return new ArrayList<Glyph>(graph.vertexSet());
        }

        @Override
        public boolean isSizeAcceptable (Rectangle box)
        {
            return box.height <= params.maxGlyphHeight;
        }

        @Override
        public boolean isWeightAcceptable (int weight)
        {
            return weight >= params.minGlyphWeight;
        }
    }
}
