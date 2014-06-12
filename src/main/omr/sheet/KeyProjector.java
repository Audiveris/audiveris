//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     K e y P r o j e c t o r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphClassifier;
import omr.glyph.GlyphCluster;
import omr.glyph.GlyphLayer;
import omr.glyph.GlyphLink;
import omr.glyph.GlyphNest;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.ShapeEvaluator;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.lag.BasicLag;
import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.SectionsBuilder;

import omr.run.Orientation;

import omr.sig.ClefInter;
import omr.sig.KeyAlterInter;
import omr.sig.SIGraph;
import static omr.util.HorizontalSide.LEFT;
import omr.util.Navigable;

import ij.process.Blitter;
import ij.process.ByteProcessor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.swing.WindowConstants;

/**
 * Class {@code KeyProjector} retrieves a staff key signature through the projection
 * to x-axis of the foreground pixels in a given abscissa range of a staff.
 * <p>
 * An instance typically handles the initial key signature, perhaps void, at the beginning of a
 * staff.
 * Another instance may be used to process a key signature change located farther in the staff.
 *
 * @author Hervé Bitteur
 */
public class KeyProjector
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(KeyProjector.class);

    /** Shapes allowed in a key signature. */
    private static final Set<Shape> keyShapes = EnumSet.of(Shape.FLAT, Shape.SHARP);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Dedicated staff to analyze. */
    private final StaffInfo staff;

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

    /** Shape classifier to use. */
    private final ShapeEvaluator evaluator = GlyphClassifier.getInstance();

    /** Binary pixel source. (for projection of pixels in area) */
    private final ByteProcessor binarySource;

    /** Staff-free pixel source. (for extraction of alteration glyphs) */
    private final ByteProcessor staffFreeSource;

    /** Count of cumulated foreground pixels, indexed by abscissa. */
    private final short[] cumuls;

    /** Precise beginning abscissa of measure. */
    private final int measureStart;

    /** Estimated ending abscissa of cumuls. */
    private final int cumulEnd;

    /** Lag for glyph sections. */
    private final Lag lag = new BasicLag("key", Orientation.VERTICAL);

    /** Estimated beginning for browsing. (typically the end of clef, if any). */
    private Integer browseStart;

    /** Minimum ordinate for key-sig area. */
    private int areaTop;

    /** Maximum ordinate for key-sig area. */
    private int areaBottom;

    /** Detected start abscissa, if any, of first pixel in key-sig area. */
    private Integer areaStart;

    /** Detected stop abscissa, if any, of last pixel in key-sig area. */
    private Integer areaStop;

    /** Sequence of peaks found. */
    private final List<KeyEvent.Peak> peaks = new ArrayList<KeyEvent.Peak>();

    /** Sequence of spaces and peaks. (for debugging) */
    private final List<KeyEvent> events = new ArrayList<KeyEvent>();

    /** Shape used for key signature. */
    private Shape keyShape;

    /** Sequence of alteration slices. */
    private final List<Slice> slices = new ArrayList<Slice>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new KeyProjector object.
     *
     * @param staff        the underlying staff
     * @param measureStart precise beginning abscissa of measure (generally right after bar line).
     * @param cumulEnd     estimated ending abscissa for cumulated pixels
     * @param browseStart  estimated beginning abscissa for browsing, if any, null otherwise.
     */
    public KeyProjector (StaffInfo staff,
                         int measureStart,
                         int cumulEnd,
                         Integer browseStart)
    {
        this.staff = staff;
        this.measureStart = measureStart;
        this.cumulEnd = cumulEnd;
        this.browseStart = browseStart;

        system = staff.getSystem();
        sig = system.getSig();
        sheet = system.getSheet();
        binarySource = sheet.getPicture().getSource(Picture.SourceKey.BINARY);
        staffFreeSource = sheet.getPicture().getSource(Picture.SourceKey.STAFF_LINE_FREE);

        scale = sheet.getScale();
        params = new Parameters(scale);

        // Cumulate pixels for each abscissa in range
        cumuls = computeCumuls();
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        Double[] mPitches = new Double[slices.size()];

        for (int i = 0; i < slices.size(); i++) {
            KeyAlterInter alter = slices.get(i).alter;

            if (alter != null) {
                mPitches[i] = alter.getMeasuredPitch();
            } else {
                mPitches[i] = null;
            }
        }

        // Guess clef
        ClefInter.Kind guess = ClefInter.guessKind(keyShape, mPitches);

        int[] stdPitches = (keyShape == Shape.SHARP) ? ClefInter.sharpsMap.get(guess)
                : ClefInter.flatsMap.get(guess);

        // Adjust pitches if needed
        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            KeyAlterInter alter = slice.alter;
            int std = stdPitches[i];

            if (alter.getPitch() != std) {
                logger.info(
                        "{}Staff#{} slice index:{} pitch adjusted from {} to {}",
                        sheet.getLogPrefix(),
                        staff.getId(),
                        i,
                        alter.getPitch(),
                        std);
                alter.setPitch(std);
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
        return browseStart;
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

    //--------------//
    // getSignature //
    //--------------//
    /**
     * Staff key signature is dynamically computed using the keyShape and the count of
     * alteration slices.
     *
     * @return the signature as an int
     */
    public int getSignature ()
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

        // Process this new slice
        extractAlter(slice, Collections.singleton(keyShape));
    }

    //------//
    // plot //
    //------//
    /**
     * Display a chart of the projection.
     */
    public void plot ()
    {
        new Plotter().plot();
    }

    //---------//
    // process //
    //---------//
    /**
     * Process the potential key signature of the assigned staff.
     */
    public void process ()
    {
        logger.debug("Processing S#{} staff#{}", system.getId(), staff.getId());

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

            // Extract and evaluate item-based glyphs
            extractSlices(starts);
        }

        logger.debug("Staff#{} sig:{} m:{} {}", staff.getId(), signature, measureStart, events);
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
        this.browseStart = browseStart;
        reset();
        process();
    }

    //-----------//
    // scanSlice //
    //-----------//
    /**
     * Inspect the provided range and try to define a slice there.
     *
     * @param start start of range abscissa
     * @param stop  stop of range abscissa
     * @return true if successful
     */
    public boolean scanSlice (int start,
                              int stop)
    {
        if (isRangeVoid(start, stop)) {
            // Nothing interesting there
            return false;
        } else {
            Slice slice = createSlice(start, (start + stop) - 1);
            slices.add(slice);

            // Reassign staff slice attachments
            for (int i = 0; i < slices.size(); i++) {
                staff.addAttachment(Integer.toString(i + 1), slices.get(i).getRect());
            }

            // Process this new slice
            if (keyShape != null) {
                extractAlter(slice, Collections.singleton(keyShape));
            } else {
                KeyAlterInter alter = extractAlter(slice, keyShapes);
                keyShape = alter.getShape();
            }

            return true;
        }
    }

    //------------//
    // browseArea //
    //------------//
    /**
     * Browse the histogram to detect peaks (similar to stems) and spaces (blanks).
     */
    private void browseArea ()
    {
        // Maximum abscissa range to be browsed
        final int xMin = getBrowsingStart();
        final int xMax = cumulEnd;

        // Space parameters
        int maxSpaceCumul = params.maxFirstSpaceCumul; // Initial threshold
        int spaceStart = -1; // Space start abscissa
        int spaceStop = -1; // Space stop abscissa
        int spaceArea = 0; // Space area

        // Peak parameters
        final int minPeakCumul = params.minPeakCumul;
        int peakStart = -1; // Peak start abscissa
        int peakStop = -1; // Peak stop abscissa
        int peakHeight = 0; // Peak height

        for (int x = xMin; x <= xMax; x++) {
            int cumul = cumuls[x];

            if (areaStart != null) {
                maxSpaceCumul = params.maxSpaceCumul;
            }

            // For peak
            if (cumul >= minPeakCumul) {
                if (spaceStart != -1) {
                    // End of space
                    if (!createSpace(spaceStart, spaceStop, spaceArea)) {
                        // Too wide space encountered
                        return;
                    }

                    spaceStart = -1;
                    spaceArea = 0;
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

                    spaceArea += (maxSpaceCumul - cumul);
                    spaceStop = x;
                } else {
                    if (spaceStart != -1) {
                        // End of space
                        if (!createSpace(spaceStart, spaceStop, spaceArea)) {
                            // Too wide space encountered
                            return;
                        }

                        spaceStart = -1;
                        spaceArea = 0;
                    }
                }
            }
        }

        // Finish ongoing space if any
        if (spaceStart != -1) {
            createSpace(spaceStart, spaceStop, spaceArea);
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
            areaStop = lastPeak.start - 1;

            KeyEvent.Peak goodPeak = lastGoodPeak();
            int trail = areaStop - goodPeak.start + 1;

            // Check trailing length
            if (signature < 0) {
                if (trail < params.minFlatTrail) {
                    logger.info("Removing too narrow flat");
                    signature += 1;
                }
            } else {
                if (trail < params.minSharpTrail) {
                    logger.info("Removing too narrow sharp");
                    signature -= 1;
                }
            }
        }

        return signature;
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
            starts.add(areaStart);

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

            // Start of area, make sure there is nothing right before first peak
            KeyEvent.Peak firstPeak = peaks.get(0);
            int flatHeading = firstPeak.start - areaStart;

            if (flatHeading > params.maxFlatHeading) {
                logger.info("Too large heading before flat peak");
                signature = 0;

                return starts;
            }

            starts.add(areaStart);

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

    //---------------//
    // computeCumuls //
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
    private short[] computeCumuls ()
    {
        final int xMin = Math.max(0, measureStart - params.preStaffMargin);
        final int xMax = cumulEnd;

        short[] table = new short[xMax + 1];
        areaTop = staff.getFirstLine().yAt(xMin) - (2 * scale.getInterline());
        areaBottom = staff.getLastLine().yAt(xMin) + (1 * scale.getInterline());

        for (int x = xMin; x <= xMax; x++) {
            short cumul = 0;

            for (int y = areaTop; y <= areaBottom; y++) {
                if (binarySource.get(x, y) == 0) {
                    cumul++;
                }
            }

            table[x] = cumul;
        }

        return table;
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

        // We need a space before the very first peak
        if (areaStart == null) {
            return true;
        }

        KeyEvent.Peak peak = new KeyEvent.Peak(start, stop, height);

        // Check whether this peak could be part of sig, otherwise give up
        if ((height > params.maxPeakCumul) || (peak.getWidth() > params.maxPeakWidth)) {
            logger.debug("Invalid height or width for peak");
            peak.setInvalid();
            keepOn = false;
        } else {
            //TODO: check derivative for a really sharp peak?

            // We may have an interesting peak, check distance since previous peak
            KeyEvent.Peak prevPeak = peaks.isEmpty() ? null : peaks.get(peaks.size() - 1);

            if (prevPeak != null) {
                // Check delta abscissa
                double x = (start + stop) / 2.0;
                double dx = x - ((prevPeak.start + prevPeak.stop) / 2.0);

                if (dx > params.maxPeakDx) {
                    // A large dx indicates we are beyond end of key-sig
                    // So, retrieve the precise end of key-sig last item
                    logger.debug("Too large delta");
                    peak.setInvalid();
                    keepOn = false;
                } else {
                }
            } else {
                // Very first peak, check offset from theoretical start
                // TODO: this is too strict, check emptyness in previous abscissae
                int offset = start - getBrowsingStart();

                if (offset > params.maxFirstPeakOffset) {
                    logger.debug("First peak arrives too late");
                    peak.setInvalid();
                    keepOn = false;
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
     * @param spaceArea  (not used) total area below threshold
     * @return true to keep browsing, false to stop immediately
     */
    private boolean createSpace (int spaceStart,
                                 int spaceStop,
                                 int spaceArea)
    {
        boolean keepOn = true;
        KeyEvent.Space space = new KeyEvent.Space(spaceStart, spaceStop, spaceArea);

        if (areaStart == null) {
            if (space.getWidth() > params.maxFirstSpaceWidth) {
                // No key signature!
                logger.debug("Staff#{} no key signature.", staff.getId());
                space.setWide();
                keepOn = false;
            } else {
                // Set areaStart here, since first chunk may be later skipped if lacking peak
                areaStart = space.stop + 1;
            }

            // clefEnd = (space.start + space.stop) / 2; // approximately
        } else {
            // Make sure we have some item
            if (peaks.isEmpty()) {
                areaStart = space.stop + 1;

                // clefEnd = (space.start + space.stop) / 2; // approximately
            } else {
                // Second (wide) space stops it
                if (space.getWidth() > params.maxInnerSpace) {
                    areaStop = space.start;
                    space.setWide();
                    keepOn = false;
                }
            }
        }

        events.add(space);

        return keepOn;
    }

    //------------//
    // derivative //
    //------------//
    /**
     * Report the first derivative of cumulated values at abscissa x.
     *
     * @param x abscissa (assumed to be within sheet width)
     * @return computed derivative at x
     */
    private int derivative (int x)
    {
        if (x == 0) {
            return 0;
        }

        return cumuls[x] - cumuls[x - 1];
    }

    //--------------//
    // extractAlter //
    //--------------//
    /**
     * Using the provided abscissa range, extract the relevant foreground pixels
     * from the STAFF_LINE_FREE image and evaluate possible glyph instances.
     *
     * @param slice        the slice to process
     * @param targetShapes the set of shapes to try
     * @return the Inter created if any
     */
    private KeyAlterInter extractAlter (Slice slice,
                                        Set<Shape> targetShapes)
    {
        Rectangle rect = slice.getRect();
        ByteProcessor buf = new ByteProcessor(rect.width, rect.height);
        buf.copyBits(staffFreeSource, -rect.x, -rect.y, Blitter.COPY);

        SectionsBuilder builder = new SectionsBuilder(lag, new JunctionRatioPolicy());
        List<Section> sections = builder.createSections(buf, rect.getLocation());
        List<Glyph> glyphs = sheet.getNest().retrieveGlyphs(
                sections,
                GlyphLayer.SYMBOL,
                true, // false, // True for debugging only
                Glyph.Linking.NO_LINK);
        purgeGlyphs(glyphs, rect);

        MyAdapter adapter = new MyAdapter(glyphs, targetShapes);
        new GlyphCluster(adapter).decompose();

        ///logger.info("cutSlice {} glyphs:{} trials:{}", rect, glyphs.size(), adapter.trials);
        if (adapter.bestEval != null) {
            ///sheet.getNest().registerGlyph(adapter.bestGlyph);
            logger.debug("Glyph#{} {}", adapter.bestGlyph.getId(), adapter.bestEval);

            KeyAlterInter alterInter = KeyAlterInter.create(
                    adapter.bestGlyph,
                    adapter.bestEval.shape,
                    adapter.bestEval.grade,
                    staff);

            if (alterInter != null) {
                sig.addVertex(alterInter);
                slice.alter = alterInter;
            }

            return alterInter;
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
            int stop = (i < (count - 1)) ? (starts.get(i + 1) - 1) : areaStop;
            Slice slice = createSlice(start, stop);
            staff.addAttachment(Integer.toString(i + 1), slice.getRect());
            slices.add(slice);
            extractAlter(slice, Collections.singleton(keyShape));
        }
    }

    //------------------//
    // getBrowsingStart //
    //------------------//
    /**
     * Report from which abscissa we are actually browsing the cumulated values.
     * This is by default the measure start, but can be overridden if a specific browseStart
     * value has been defined.
     *
     * @return the actual start abscissa used
     */
    private int getBrowsingStart ()
    {
        return (browseStart != null) ? browseStart : measureStart;
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
            int cumul = cumuls[x];

            if (cumul > params.linesThreshold) {
                count++;
                meanHeight += cumul;
            }
        }

        if (count > 0) {
            meanHeight = (int) Math.rint(meanHeight / (double) count);
            logger.debug("isRangeVoid start:{} stop:{} meanHeight:{}", start, stop, meanHeight);
        }

        // TODO: use a specific constant?
        return meanHeight <= ((params.linesThreshold + params.maxSpaceCumul) / 2);
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
     * @param typicalTrail typical length after last peak (this depends of shape)
     * @param maxTrail     maximum length after last peak
     */
    private void refineStop (KeyEvent.Peak lastGoodPeak,
                             int typicalTrail,
                             int maxTrail)
    {
        final int xMin = (lastGoodPeak.start + typicalTrail) - 1;
        final int xMax = Math.min(cumuls.length - 1, lastGoodPeak.start + maxTrail);

        int minCount = Integer.MAX_VALUE;

        for (int x = xMin; x <= xMax; x++) {
            int count = cumuls[x];

            if (count < minCount) {
                areaStop = x - 1;
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
            KeyAlterInter alter = slice.alter;

            if (alter != null) {
                sig.removeVertex(alter);
            }
        }

        peaks.clear();
        events.clear();
        slices.clear();

        areaStart = null;
        areaStop = null;
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
            int offset = peaks.get(0).start - areaStart;

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
            //TODO: for sharps, peaks count should be an even number
            return (keyShape == Shape.SHARP) ? ((last + 1) / 2) : (-(last + 1));
        } else {
            keyShape = Shape.FLAT;

            return -1;
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
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
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Scale.Fraction preStaffMargin = new Scale.Fraction(
                2.0,
                "Horizontal margin before staff left (for plot display)");

        final Scale.Fraction maxFirstPeakOffset = new Scale.Fraction(
                2.0,
                "Maximum x offset of first peak (WRT theoretical clef end)");

        final Scale.Fraction minPeakCumul = new Scale.Fraction(
                1.5,
                "Minimum cumul value to detect peak (on top of lines)");

        final Scale.Fraction maxPeakCumul = new Scale.Fraction(
                4.0,
                "Maximum cumul value to accept peak (absolute value)");

        final Scale.Fraction maxPeakWidth = new Scale.Fraction(
                0.4,
                "Maximum width to accept peak (measured at threshold height)");

        final Scale.Fraction maxFlatHeading = new Scale.Fraction(
                0.5,
                "Maximum heading length before peak for a flat item");

        final Scale.Fraction flatTrail = new Scale.Fraction(
                1.0,
                "Typical trailing length after peak for a flat item");

        final Scale.Fraction minFlatTrail = new Scale.Fraction(
                0.8,
                "Minimum trailing length after peak for a flat item");

        final Scale.Fraction maxFlatTrail = new Scale.Fraction(
                1.3,
                "Maximum trailing length after peak for a flat item");

        final Scale.Fraction sharpTrail = new Scale.Fraction(
                0.3,
                "Typical trailing length after last peak for a sharp item");

        final Scale.Fraction minSharpTrail = new Scale.Fraction(
                0.2,
                "Minimum trailing length after last peak for a sharp item");

        final Scale.Fraction maxSharpTrail = new Scale.Fraction(
                0.5,
                "Maximum trailing length after last peak for a sharp item");

        final Scale.Fraction maxPeakDx = new Scale.Fraction(
                1.4,
                "Maximum delta abscissa between peaks");

        final Scale.Fraction maxSharpDelta = new Scale.Fraction(
                0.75,
                "Maximum short peak delta for sharps");

        final Scale.Fraction minFlatDelta = new Scale.Fraction(
                0.6,
                "Minimum short peak delta for flats");

        final Scale.Fraction offsetThreshold = new Scale.Fraction(
                0.1,
                "Threshold on first peak offset that differentiates flat & sharp");

        final Scale.Fraction maxSpaceCumul = new Scale.Fraction(
                0.4,
                "Maximum cumul value in space (on top of lines)");

        final Scale.Fraction maxFirstSpaceCumul = new Scale.Fraction(
                0.7,
                "Maximum cumul value in first space (on top of lines)");

        final Scale.Fraction minFirstSpaceWidth = new Scale.Fraction(
                0.2,
                "Minimum initial space before key signature");

        final Scale.Fraction maxGlyphGap = new Scale.Fraction(
                1.5,
                "Maximum distance between two glyphs of a single alter symbol");

        final Scale.Fraction maxGlyphHeight = new Scale.Fraction(3.5, "Maximum glyph height");

        final Scale.AreaFraction minGlyphWeight = new Scale.AreaFraction(
                0.3,
                "Minimum glyph weight");

        // Beware: A too small value might miss the whole key-sig
        final Scale.Fraction maxFirstSpaceWidth = new Scale.Fraction(
                1.75,
                "Maximum initial space before key signature");

        // Beware: A too small value might miss some key-sig items
        final Scale.Fraction maxInnerSpace = new Scale.Fraction(
                0.7,
                "Maximum inner space within key signature");
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final int preStaffMargin;

        final int maxFirstPeakOffset;

        final int minFirstSpaceWidth;

        final int maxFirstSpaceWidth;

        final int maxInnerSpace;

        final int linesThreshold;

        final int minPeakCumul;

        final int maxFirstSpaceCumul;

        final int maxSpaceCumul;

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

        final double maxSharpDelta;

        final double minFlatDelta;

        final double offsetThreshold;

        final double maxGlyphGap;

        final double maxGlyphHeight;

        final int minGlyphWeight;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            preStaffMargin = scale.toPixels(constants.preStaffMargin);
            maxFirstPeakOffset = scale.toPixels(constants.maxFirstPeakOffset);
            minFirstSpaceWidth = scale.toPixels(constants.minFirstSpaceWidth);
            maxFirstSpaceWidth = scale.toPixels(constants.maxFirstSpaceWidth);
            maxInnerSpace = scale.toPixels(constants.maxInnerSpace);
            linesThreshold = 5 * scale.getMainFore();
            minPeakCumul = linesThreshold + scale.toPixels(constants.minPeakCumul);
            maxSpaceCumul = linesThreshold + scale.toPixels(constants.maxSpaceCumul);
            maxFirstSpaceCumul = linesThreshold + scale.toPixels(constants.maxFirstSpaceCumul);
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
            maxSharpDelta = scale.toPixelsDouble(constants.maxSharpDelta);
            minFlatDelta = scale.toPixelsDouble(constants.minFlatDelta);
            offsetThreshold = scale.toPixelsDouble(constants.offsetThreshold);
            maxGlyphGap = scale.toPixelsDouble(constants.maxGlyphGap);
            maxGlyphHeight = scale.toPixelsDouble(constants.maxGlyphHeight);
            minGlyphWeight = scale.toPixels(constants.minGlyphWeight);
        }
    }

    //-----------//
    // MyAdapter //
    //-----------//
    /**
     * Handles the integration between glyph clustering class and key-sig environment.
     */
    private class MyAdapter
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
        public MyAdapter (List<Glyph> glyphs,
                          Set<Shape> targetShapes)
        {
            this.targetShapes.addAll(targetShapes);
            graph = Glyphs.buildLinks(
                    glyphs,
                    new Glyphs.LinkAdapter()
                    {
                        @Override
                        public double getAcceptableDistance (Glyph glyph)
                        {
                            return params.maxGlyphGap;
                        }
                    });
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void evaluateGlyph (Glyph glyph)
        {
            trials++;

            Evaluation[] evals = evaluator.getNaturalEvaluations(glyph);

            for (Shape shape : targetShapes) {
                Evaluation eval = evals[shape.ordinal()];

                if ((bestEval == null) || (bestEval.grade < eval.grade)) {
                    bestEval = eval;
                    bestGlyph = glyph;
                }
            }
        }

        @Override
        public List<Glyph> getGlyphs ()
        {
            return new ArrayList<Glyph>(graph.vertexSet());
        }

        @Override
        public List<Glyph> getNeighbors (Glyph glyph)
        {
            return Graphs.neighborListOf(graph, glyph);
        }

        @Override
        public GlyphNest getNest ()
        {
            return sheet.getNest();
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

    //---------//
    // Plotter //
    //---------//
    /**
     * Handles the display of projection chart.
     */
    private class Plotter
    {
        //~ Instance fields ------------------------------------------------------------------------

        final XYSeriesCollection dataset = new XYSeriesCollection();

        // Chart
        final JFreeChart chart = ChartFactory.createXYLineChart(
                sheet.getId() + " key-sig staff#" + staff.getId(), // Title
                "Abscissae sig:" + getSignature() + " " + scale, // X-Axis label
                "Counts", // Y-Axis label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // orientation
                true, // Show legend
                false, // Show tool tips
                false // urls
        );

        final XYPlot plot = (XYPlot) chart.getPlot();

        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Series index
        int index = -1;

        //~ Methods --------------------------------------------------------------------------------
        public void plot ()
        {
            plot.setRenderer(renderer);

            final int xMin = Math.max(0, staff.getAbscissa(LEFT) - params.preStaffMargin);
            final int xMax = cumuls.length - 1;

            {
                // Values
                XYSeries cumulSeries = new XYSeries("Cumuls");

                for (int x = xMin; x <= xMax; x++) {
                    cumulSeries.add(x, cumuls[x]);
                }

                add(cumulSeries, Color.RED, false);
            }

            {
                // Derivatives
                XYSeries derivativeSeries = new XYSeries("Derivatives");

                for (int x = xMin; x <= xMax; x++) {
                    derivativeSeries.add(x, derivative(x));
                }

                add(derivativeSeries, Color.BLUE, false);
            }

            if (browseStart != null) {
                // Minimum space start
                XYSeries spaceStart = new XYSeries("Start");
                int x = browseStart;
                spaceStart.add(x, 0);
                spaceStart.add(x, staff.getHeight());
                add(spaceStart, Color.GRAY, false);
            }

            // key sig area
            if (areaStop != null) {
                // Ending abscissa
                XYSeries endings = new XYSeries("Stop");
                int x = areaStop;
                endings.add(x, staff.getHeight());
                endings.add(x, 0);
                add(endings, Color.ORANGE, false);
            }

            // Items marks
            for (Slice slice : getSlices()) {
                XYSeries sep = new XYSeries("Mark");
                double x = slice.getRect().x;
                sep.add(x, params.linesThreshold);
                sep.add(x, staff.getHeight());
                add(sep, Color.CYAN, false);
            }

            // Peak min threshold
            if (areaStart != null) {
                XYSeries minSeries = new XYSeries("Peak");

                minSeries.add((int) areaStart, params.minPeakCumul);

                if (areaStop != null) {
                    minSeries.add((int) areaStop, params.minPeakCumul);
                } else {
                    minSeries.add(xMax, params.minPeakCumul);
                }

                add(minSeries, Color.GREEN, false);
            }

            {
                // First space threshold
                XYSeries chunkSeries = new XYSeries("firstSpace");
                chunkSeries.add(getBrowsingStart(), params.maxFirstSpaceCumul);
                chunkSeries.add(
                        (areaStart != null) ? (double) areaStart : (double) xMax,
                        params.maxFirstSpaceCumul);
                add(chunkSeries, Color.YELLOW, false);
            }

            {
                // Space threshold
                XYSeries chunkSeries = new XYSeries("Space");
                int x = (areaStart != null) ? areaStart : getBrowsingStart();
                chunkSeries.add(x, params.maxSpaceCumul);
                chunkSeries.add(xMax, params.maxSpaceCumul);
                add(chunkSeries, Color.YELLOW, false);
            }

            {
                // Cumulated staff lines (assuming a 5-line staff)
                XYSeries linesSeries = new XYSeries("Lines");
                linesSeries.add(xMin, params.linesThreshold);
                linesSeries.add(xMax, params.linesThreshold);
                add(linesSeries, Color.MAGENTA, true);
            }

            // Hosting frame
            String title = sheet.getId() + " key-sig staff#" + staff.getId();
            ChartFrame frame = new ChartFrame(title, chart, true);
            frame.pack();
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLocation(new Point(20 * staff.getId(), 20 * staff.getId()));
            frame.setVisible(true);
        }

        private void add (XYSeries series,
                          Color color,
                          boolean displayShapes)
        {
            dataset.addSeries(series);
            renderer.setSeriesPaint(++index, color);
            renderer.setSeriesShapesVisible(index, displayShapes);
        }
    }
}
