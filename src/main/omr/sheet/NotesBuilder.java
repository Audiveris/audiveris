//----------------------------------------------------------------------------//
//                                                                            //
//                            N o t e s B u i l d e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.Glyphs;
import omr.glyph.GlyphsBuilder;
import omr.glyph.Nest;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;
import omr.grid.StaffInfo.IndexedLedger;

import omr.image.ChamferDistanceInteger;
import omr.image.MorphoProcessor;
import omr.image.PixelBuffer;
import omr.image.StructureElement;
import omr.image.WatershedGrayLevel;

import omr.lag.BasicLag;
import omr.lag.JunctionAllPolicy;
import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.SectionsBuilder;

import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.sig.BeamInter;
import omr.sig.BlackHeadInter;
import omr.sig.Inter;
import omr.sig.SIGraph;

import omr.util.Navigable;
import omr.util.Predicate;
import omr.util.StopWatch;

import net.jcip.annotations.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Class {@code NotesBuilder} is in charge, at system level, of
 * retrieving the possible interpretations of black heads.
 * <p>
 * The main difficulty is the need to split large spots that were created during
 * spot extraction and that may correspond to several heads stuck together.
 * The watershed algorithm often works correctly but not always.
 * In the case of simple vertical spot, it is safer to define the split
 * directly along computed ordinates.
 *
 * @author Hervé Bitteur
 */
@NotThreadSafe
public class NotesBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            NotesBuilder.class);

    //~ Instance fields --------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    @Navigable(false)
    private final SIGraph sig;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** The sheet glyph nest. */
    @Navigable(false)
    private final Nest nest;

    /** Sheet scale. */
    @Navigable(false)
    private final Scale scale;

    /** Scale-dependent constants. */
    private final Parameters params;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // NotesBuilder //
    //--------------//
    /**
     * Creates a new NotesBuilder object.
     *
     * @param system the dedicated system
     */
    public NotesBuilder (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();
        sheet = system.getSheet();
        nest = sheet.getNest();
        scale = sheet.getScale();
        params = new Parameters(scale);

        if (system.getId() == 1) {
            Main.dumping.dump(params);
        }
    }

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // buildBlackHeads //
    //-----------------//
    /**
     * Find possible interpretations of black heads among system spots.
     * This assumes we already have stem candidates and ledger candidates
     * as well as staff lines.
     * <p>We browse through all spots of minimum weight and which cannot be
     * beams.
     * Spots that could be packs of heads (because of their global weight) are
     * split into separate head candidates using a watershed algorithm.
     * Each head candidate is then checked for minimum weight, for ledger
     * (or staff line) in suitable relative position, and for acceptable shape.
     * Successful head candidates are stored as interpretations.
     */
    public void buildBlackHeads ()
    {
        StopWatch watch = new StopWatch("NotesBuilder S#" + system.getId());

        // Retrieve suitable spots from beam-focused spots
        watch.start("getSuitableSpots");

        Set<Glyph> beamSpots = getSuitableSpots();
        logger.info("S#{} beamSpots: {}", system.getId(), beamSpots.size());

        // Extract head-focused spots
        watch.start("getHeadSpots");

        List<Glyph> headSpots = getHeadSpots(beamSpots);
        logger.info("S#{} headSpots: {}", system.getId(), headSpots.size());

        // Split large spots into separate head candidates
        watch.start("splitLargeSpots");
        headSpots = splitLargeSpots(headSpots);

        // Run checks on single-head candidates
        watch.start("checkHeads");

        int heads = checkHeads(headSpots);
        logger.info("S#{} heads: {}", system.getId(), heads);

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //------------//
    // checkHeads //
    //------------//
    /**
     * Check all single-head spots to come up with acceptable head
     * interpretations.
     *
     * @param headSpots the list of source spots
     * @return the number of acceptable heads
     */
    private int checkHeads (List<Glyph> headSpots)
    {
        double totalWeight = 0;
        int headNb = 0;

        for (Glyph glyph : headSpots) {
            BlackHeadInter inter = checkSingleHead(glyph);

            if (inter != null) {
                glyph.setShape(Shape.NOTEHEAD_BLACK); // Useful?
                headNb++;
                totalWeight += glyph.getWeight();
            }
        }

        if (headNb != 0) {
            double headWeight = scale.pixelsToAreaFrac(totalWeight / headNb);
            logger.info(
                    "S#{} mean head weight: {}",
                    system.getId(),
                    String.format("%.2f", headWeight));
        }

        return headNb;
    }

    //-----------------//
    // checkSingleHead //
    //-----------------//
    /**
     * Run checks on single head candidate.
     * We have a possible head interpretation only if the candidate has a
     * minimum weight, is correctly located WRT staff line (or ledger) and looks
     * like a black note head.
     *
     * @param glyph the note head candidate
     * @return the related interpretation or null
     */
    private BlackHeadInter checkSingleHead (Glyph glyph)
    {
        if (glyph.isVip()) {
            logger.info("BINGO checkSingleHead");
        }

        // Minimum weight
        if (glyph.getWeight() < params.minHeadWeight) {
            return null;
        }

        // Pitch position for single-head spots
        final Point centroid = glyph.getCentroid();
        final StaffInfo staff = system.getStaffAt(centroid);
        final NotePosition pos = staff.getNotePosition(centroid);
        final double pitchPosition = pos.getPitchPosition();
        logger.debug("Head#{} {}", glyph.getId(), pos);

        // Notes outside staves need a ledger
        if (Math.abs(pitchPosition) > 5.5) {
            // Nearby ledger, if any
            IndexedLedger ledger = pos.getLedger();

            if ((ledger == null)
                || (Math.abs(
                    pitchPosition - StaffInfo.getLedgerPitchPosition(ledger.index)) > 1.5)) {
                logger.debug(
                        "Head#{} too far from staff w/o ledger",
                        glyph.getId());

                return null;
            }
        }

        // Check for a suitable head shape
        // TODO

        // Check vertical distance to satff line or ledger
        // TODO

        // OK!
        double grade = 1.0; // To be refined!

        BlackHeadInter inter = new BlackHeadInter(glyph, grade);
        sig.addVertex(inter);

        return inter;
    }

    //---------------//
    // extractGlyphs //
    //---------------//
    /**
     * Convenient method to retrieve all glyphs contained in a provided
     * image.
     *
     * @param img       the populated image
     * @param imgOrigin absolute origin of image
     * @param lag       the target lag, if any
     * @param nest      the target glyph nest, if any
     * @return the collection of glyph instances ready to use
     */
    private List<Glyph> extractGlyphs (PixelBuffer img,
                                       Point imgOrigin,
                                       Lag lag,
                                       Nest nest)
    {
        if (lag == null) {
            lag = new BasicLag("tLag", SpotsBuilder.SPOT_ORIENTATION);
        }

        // Populate runs out of img pixels
        RunsTable table = new RunsTableFactory(
                SpotsBuilder.SPOT_ORIENTATION,
                img,
                0).createTable("imgTable");

        // Populate sections out of runs
        SectionsBuilder sectionsBuilder = new SectionsBuilder(
                lag,
                new JunctionAllPolicy());
        List<Section> sections = sectionsBuilder.createSections(table, false);

        // Populate glyphs out of sections
        List<Glyph> glyphs = GlyphsBuilder.retrieveGlyphs(
                sections,
                null,
                GlyphLayer.SPOT,
                scale);

        // Translate img-relative coordinates to absolute coordinates
        for (Glyph glyph : glyphs) {
            glyph.translate(imgOrigin);
        }

        // Inject glyphs into glyph nest, if any
        if (nest != null) {
            List<Glyph> newGlyphs = new ArrayList<Glyph>(glyphs.size());

            for (Glyph glyph : glyphs) {
                newGlyphs.add(system.addGlyphAndMembers(glyph));
            }

            glyphs = newGlyphs;
        }

        return glyphs;
    }

    //--------------//
    // getHeadSpots //
    //--------------//
    /**
     * Starting from filtered beam-oriented spots, extract spots using
     * a larger (head-oriented) structuring element.
     *
     * @param beamSpots the initial spots (beam-oriented)
     * @return the new head-oriented spots
     */
    private List<Glyph> getHeadSpots (Set<Glyph> beamSpots)
    {
        final Lag headLag = sheet.getHeadLag();
        final List<Glyph> headSpots = new ArrayList<Glyph>();
        final int[] offset = {0, 0};
        final int interline = scale.getInterline();
        final float radius = (interline - 3) / 2f; // => head focus
        final StructureElement se = new StructureElement(0, 1, radius, offset);
        logger.info("S#{} heads retrieval, radius: {}", system.getId(), radius);

        for (Glyph glyph : beamSpots) {
            MorphoProcessor mp = new MorphoProcessor(se);
            PixelBuffer img = glyph.getImage();
            mp.close(img);

            Point origin = glyph.getBounds()
                    .getLocation();
            List<Glyph> glyphs = extractGlyphs(img, origin, headLag, nest);

            for (Glyph g : glyphs) {
                g.setShape(Shape.HEAD_SPOT);
                headSpots.add(g);
            }
        }

        return headSpots;
    }

    //------------------//
    // getSuitableSpots //
    //------------------//
    /**
     * Retrieve which spots (among all system spots) are suitable for
     * note heads candidates.
     *
     * @return the collection of suitable spots
     */
    private Set<Glyph> getSuitableSpots ()
    {
        return Glyphs.lookupGlyphs(
                system.getGlyphs(),
                new Predicate<Glyph>()
        {
            @Override
            public boolean check (Glyph glyph)
            {
                return isSuitable(glyph);
            }
        });
    }

    //------------//
    // isSuitable //
    //------------//
    private boolean isSuitable (Glyph glyph)
    {
        if (glyph.isVip()) {
            logger.info("isSuitable for {}", glyph);
        }

        // Only SPOT-shaped glyphs
        if (glyph.getShape() != Shape.BEAM_SPOT) {
            return false;
        }

        // Minimum weight
        if (glyph.getWeight() < params.minHeadWeight) {
            return false;
        }

        // Discard spots that are much too wide
        Rectangle glyphBox = glyph.getBounds();

        if (glyphBox.width > params.maxSpotWidth) {
            return false;
        }

        // Discard spots with good beam interpretation
        Set<Inter> inters = glyph.getInterpretations();

        if (!inters.isEmpty()) {
            for (Inter inter : inters) {
                if (inter instanceof BeamInter) {
                    BeamInter beam = (BeamInter) inter;

                    if (beam.isGood()) {
                        return false;
                    }
                }
            }
        }

        // Notes cannot be too close to stave left side
        int xGap = glyph.getCentroid().x - system.getLeft();

        if (xGap < params.minGapFromStaffLeft) {
            if (glyph.isVip() || logger.isDebugEnabled()) {
                logger.info(
                        "Spot#{} too close to staff left side, gap:{}",
                        glyph.getId(),
                        xGap);
            }

            return false;
        }

        // Avoid thick barlines (to be improved)
        //TODO: vertical, height >= staff height, mean width << head width
        int meanWidth = glyph.getBounds().width;

        if (meanWidth < params.minMeanWidth) {
            if (glyph.isVip() || logger.isDebugEnabled()) {
                logger.info(
                        "Spot#{} too narrow {} vs {}",
                        glyph.getId(),
                        meanWidth,
                        params.minMeanWidth);
            }

            return false;
        }

        return true;
    }

    //------------//
    // regenerate //
    //------------//
    /**
     * All the provided parts were built on temporary lags, this
     * method rebuild them into the single splitLag.
     * We generate a single PixelBuffer with all parts then retrieve final
     * glyphs out of this buffer.
     *
     * @param parts the temporary parts
     * @return the new parts, with same pixels, but in splitLag
     */
    private List<Glyph> regenerate (List<Glyph> parts)
    {
        final Rectangle globalBox = Glyphs.getBounds(parts);
        final Point globalOrg = globalBox.getLocation();
        final PixelBuffer globalBuf = new PixelBuffer(globalBox.getSize());

        // Inject each part into the global buffer
        for (Glyph part : parts) {
            final Point org = part.getBounds()
                    .getLocation();
            globalBuf.injectBuffer(
                    part.getImage(),
                    new Point(org.x - globalOrg.x, org.y - globalOrg.y));
        }

        // Retrieve all glyphs out of the global buffer
        return extractGlyphs(globalBuf, globalOrg, sheet.getSplitLag(), nest);
    }

    //-------//
    // split //
    //-------//
    /**
     * Split the provided glyph/img using best algorithm
     *
     * @param glyph the glyph to split
     * @param img   the glyph image (to be modified by the split)
     * @return true if some split was done
     */
    private boolean split (Glyph glyph,
                           PixelBuffer img)
    {
        if (glyph.isVip()) {
            logger.debug("split {}", glyph);
        }

        // Check if a direct split is the way to go
        final double vertSlope = glyph.getLine()
                .getInvertedSlope();

        if (Math.abs(vertSlope) <= params.maxSlopeForDirectSplit) {
            final Rectangle glyphBox = glyph.getBounds();
            final double weightCount = (double) glyph.getWeight() / params.typicalWeight;
            final double heightCount = (double) glyphBox.height / scale.getInterline();
            final double meanCount = (weightCount + heightCount) / 2;

            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Split for {} vSlope:{} weight:{} height:{} mean:{}",
                        glyph,
                        String.format("%.2f", vertSlope),
                        String.format("%.2f", weightCount),
                        String.format("%.2f", heightCount),
                        String.format("%.2f", meanCount));
            }

            final int count = (int) Math.rint(meanCount);
            final double height = glyphBox.height / (double) count;

            for (int i = 1; i < count; i++) {
                int y = (int) Math.rint(i * height);

                for (int x = 0, w = img.getWidth(); x < w; x++) {
                    img.setPixel(x, y, (byte) 255);
                }
            }

            return true;
        }

        // Fall back using watershed algorithm
        return watershedSplit(img);
    }

    //-----------------//
    // splitLargeSpots //
    //-----------------//
    /**
     * Browse all spots for large ones and split them as needed.
     */
    private List<Glyph> splitLargeSpots (List<Glyph> spots)
    {
        final List<Glyph> kept = new ArrayList<Glyph>();

        for (Glyph glyph : spots) {
            if (glyph.isVip()) {
                logger.info("BINGO splitLargeSpots spot#{}", glyph.getId());
            }

            // Rough number of heads
            final double headCount = (double) glyph.getWeight() / params.typicalWeight;
            final int count = (int) Math.rint(headCount);

            logger.debug(
                    "Head#{} count:{}",
                    glyph.getId(),
                    String.format("%.2f", headCount));

            if (count >= 2) {
                List<Glyph> parts = splitSpot(glyph);

                if (parts.size() > 1) {
                    // We have to regenerate parts in the common splitLag
                    List<Glyph> newParts = regenerate(parts);

                    // Assign proper system to each part
                    for (Glyph part : newParts) {
                        part = system.addGlyphAndMembers(part);
                        part.setShape(Shape.HEAD_SPOT);
                        kept.add(part);
                    }
                }
            } else {
                kept.add(glyph);
            }
        }

        return kept;
    }

    //-----------//
    // splitSpot //
    //-----------//
    /**
     * Split a (large) spot recursively into smaller parts.
     * A part is further split only if its weight is significantly larger than
     * the typical weight of a note head.
     *
     * @param glyph the spot to split
     * @return the collection of parts
     */
    private List<Glyph> splitSpot (Glyph glyph)
    {
        Rectangle glyphBox = glyph.getBounds();
        Point glyphOrigin = glyphBox.getLocation();
        PixelBuffer img = glyph.getImage();

        // Split attempt
        if (!split(glyph, img)) {
            logger.warn("*** Could not split {}", glyph);

            return Arrays.asList(glyph);
        }

        List<Glyph> parts = extractGlyphs(img, glyphOrigin, null, null);

        // Check if some parts need further split
        List<Glyph> goodParts = new ArrayList<Glyph>();

        for (Glyph part : parts) {
            double headCount = (double) part.getWeight() / params.typicalWeight;
            int cnt = (int) Math.rint(headCount);

            if (cnt >= 2) {
                List<Glyph> subParts = splitSpot(part); // Recursion
                goodParts.addAll(subParts);
            } else {
                goodParts.add(part);
            }
        }

        return goodParts;
    }

    //----------------//
    // watershedSplit //
    //----------------//
    private boolean watershedSplit (PixelBuffer img)
    {
        // We compute distances to background (white) pixels
        ChamferDistanceInteger chamferDistance = new ChamferDistanceInteger();
        int[][] dists = chamferDistance.computeToBack(img);
        WatershedGrayLevel instance = new WatershedGrayLevel(dists, true);
        boolean[][] result = null;

        // Try to split the glyph into at least 2 parts
        int count = 1;

        for (int step = 10; step >= 1; step--) {
            result = instance.process(step);
            count = instance.getRegionCount();

            if (count > 1) {
                break;
            }
        }

        if (count == 1) {
            return false;
        } else {
            for (int y = 0, h = img.getHeight(); y < h; y++) {
                for (int x = 0, w = img.getWidth(); x < w; x++) {
                    if (result[x][y]) {
                        img.setPixel(x, y, (byte) 255);
                    }
                }
            }

            return true;
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        final Scale.AreaFraction minHeadWeight = new Scale.AreaFraction(
                0.8,
                "Minimum weight for a black head");

        final Scale.AreaFraction typicalWeight = new Scale.AreaFraction(
                1.33,
                "Typical weight for a black head");

        final Scale.Fraction minGapFromStaffLeft = new Scale.Fraction(
                4.0,
                "Minimum distance from left side of the staff");

        final Scale.Fraction minMeanWidth = new Scale.Fraction(
                1.0,
                "Minimum mean width for a black head");

        final Scale.Fraction maxSpotWidth = new Scale.Fraction(
                4.0,
                "Maximum width for a multi-head spot");

        final Constant.Double maxSlopeForDirectSplit = new Constant.Double(
                "tangent",
                0.2,
                "Maximum vertical slope to use a direct split");

    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all pre-scaled constants.
     */
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        final int minHeadWeight;

        final int typicalWeight;

        final int minGapFromStaffLeft;

        final int minMeanWidth;

        final int maxSpotWidth;

        final double maxSlopeForDirectSplit;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            minHeadWeight = scale.toPixels(constants.minHeadWeight);
            typicalWeight = scale.toPixels(constants.typicalWeight);
            minGapFromStaffLeft = scale.toPixels(constants.minGapFromStaffLeft);
            minMeanWidth = scale.toPixels(constants.minMeanWidth);
            maxSpotWidth = scale.toPixels(constants.maxSpotWidth);
            maxSlopeForDirectSplit = constants.maxSlopeForDirectSplit.getValue();

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }
}
