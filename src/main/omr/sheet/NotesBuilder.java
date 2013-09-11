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
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;
import omr.grid.StaffInfo.IndexedLedger;

import omr.image.ChamferDistanceInteger;
import omr.image.PixelBuffer;
import omr.image.WatershedGrayLevel;

import omr.lag.BasicLag;
import omr.lag.JunctionAllPolicy;
import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.SectionsBuilder;

import omr.run.Orientation;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.sig.BeamInter;
import omr.sig.BlackHeadInter;
import omr.sig.Inter;
import omr.sig.SIGraph;

import omr.util.Navigable;

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
    /** The dedicated system */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    private final SIGraph sig;

    /** The related sheet */
    @Navigable(false)
    private final Sheet sheet;

    /** Sheet scale */
    @Navigable(false)
    private final Scale scale;

    /** Scale-dependent constants. */
    private final Parameters params;

    /** Spots for this system. */
    private List<Glyph> spots;

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
     *
     * <p>Such head interpretations are then enriched with relation to a
     * suitable stem nearby.
     */
    public void buildBlackHeads ()
    {
        // Retrieve suitable spots
        spots = getSuitableSpots();

        // Split large spots into separate head candidates
        splitLargeSpots();

        // Run checks on single-head candidates
        for (Glyph glyph : spots) {
            BlackHeadInter inter = checkSingleHead(glyph);

            if (inter != null) {
                glyph.setShape(Shape.NOTEHEAD_BLACK);
            }
        }
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

        // OK!
        double grade = 1.0; // To be refined!

        BlackHeadInter inter = new BlackHeadInter(glyph, grade);
        sig.addVertex(inter);

        return inter;
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
    private List<Glyph> getSuitableSpots ()
    {
        // Spots for this system
        final List<Glyph> spots = new ArrayList<Glyph>();

        SpotLoop:
        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == Shape.SPOT) {
                // Discard spots that are wider than 2 heads
                Rectangle glyphBox = glyph.getBounds();

                if (glyphBox.width > params.maxSpotWidth) {
                    continue SpotLoop;
                }

                // Discard spots with good beam interpretation
                Set<Inter> inters = glyph.getInterpretations();

                if (!inters.isEmpty()) {
                    for (Inter inter : inters) {
                        if (inter instanceof BeamInter) {
                            BeamInter beam = (BeamInter) inter;

                            if (beam.isGood()) {
                                continue SpotLoop;
                            }
                        }
                    }
                }

                // Notes cannot be too close to stave left side
                int xGap = glyph.getCentroid().x - system.getLeft();

                if (xGap < params.minDistanceFromStaffLeftSide) {
                    if (glyph.isVip() || logger.isDebugEnabled()) {
                        logger.info(
                                "Spot#{} too close to staff left side, gap:{}",
                                glyph.getId(),
                                xGap);
                    }

                    continue SpotLoop;
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

                    continue SpotLoop;
                }

                spots.add(glyph);
            }
        }

        return spots;
    }

    //-------//
    // merge //
    //-------//
    /**
     * Modify the provided image by inserting separation lines as
     * background pixels.
     *
     * @param image the image to modify
     * @param lines the watershed lines that indicate where split should occur
     */
    private void merge (PixelBuffer image,
                        boolean[][] lines)
    {
        final int width = image.getWidth();
        final int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (lines[x][y]) {
                    image.setPixel(x, y, (byte) 255);
                }
            }
        }
    }

    //------------//
    // regenerate //
    //------------//
    /**
     * All the provided parts were built on temporary lags, this
     * method rebuild them into the single splitLag.
     *
     * @param parts the temporary parts
     * @return the new parts, with same pixels, but in splitLag
     */
    private List<Glyph> regenerate (List<Glyph> parts)
    {
        // Generate a single PixelBuffer with all parts
        // then retrieve final glyphs out of this buffer
        final Rectangle globalBox = Glyphs.getBounds(parts);
        final Point globalOrg = globalBox.getLocation();
        final PixelBuffer globalBuf = new PixelBuffer(globalBox.getSize());

        for (Glyph part : parts) {
            final Point org = part.getBounds()
                    .getLocation();
            globalBuf.injectBuffer(
                    part.getImage(),
                    new Point(org.x - globalOrg.x, org.y - globalOrg.y));
        }

        Lag splitLag = sheet.getSplitLag();
        SectionsBuilder sectionsBuilder = new SectionsBuilder(
                splitLag,
                new JunctionAllPolicy());
        RunsTable splitTable = new RunsTableFactory(
                Orientation.VERTICAL,
                globalBuf,
                0).createTable("split");

        List<Section> sections = sectionsBuilder.createSections(
                splitTable,
                false);

        List<Glyph> newParts = GlyphsBuilder.retrieveGlyphs(
                sections,
                sheet.getNest(),
                GlyphLayer.SPOT,
                scale);

        for (Glyph part : newParts) {
            part.translate(globalOrg);
        }

        return newParts;
    }

    //-----------------//
    // splitLargeSpots //
    //-----------------//
    /**
     * Browse all spots for large ones and split them as needed.
     */
    private void splitLargeSpots ()
    {
        List<Glyph> toAdd = new ArrayList<Glyph>();
        List<Glyph> toRemove = new ArrayList<Glyph>();

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
                    toAdd.addAll(regenerate(parts));
                    toRemove.add(glyph);
                }
            }
        }

        logger.debug("Removed: {}, added: {}", toRemove.size(), toAdd.size());

        if (!toAdd.isEmpty()) {
            spots.addAll(toAdd);
            spots.removeAll(toRemove);

            // Assign proper system to each part
            for (Glyph part : toAdd) {
                system.addGlyph(part);

                for (Section section : part.getMembers()) {
                    section.setSystem(system);
                }
            }
        }
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
        Point glyphOrigin = glyph.getBounds()
                .getLocation();

        // We compute distances to background (white) pixels
        PixelBuffer img = glyph.getImage();
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
            // No split was found
            return Arrays.asList(glyph);
        }

        // Perform the split, using a temporary lag
        merge(img, result);

        Lag tLag = new BasicLag(
                "tLag",
                SpotsBuilder.SPOT_ORIENTATION);
        SectionsBuilder sectionsBuilder = new SectionsBuilder(
                tLag,
                new JunctionAllPolicy());
        RunsTable splitTable = new RunsTableFactory(
                Orientation.VERTICAL,
                img,
                0).createTable("tSplit");

        List<Section> sections = sectionsBuilder.createSections(
                splitTable,
                false);

        List<Glyph> parts = GlyphsBuilder.retrieveGlyphs(
                sections,
                null,
                null,
                scale);

        for (Glyph part : parts) {
            part.translate(glyphOrigin);
        }

        // Check if some parts need further split
        List<Glyph> goodParts = new ArrayList<Glyph>();

        for (Glyph part : parts) {
            double headCount = (double) part.getWeight() / params.typicalWeight;
            int cnt = (int) Math.rint(headCount);

            if (cnt >= 2) {
                List<Glyph> subParts = splitSpot(part);
                goodParts.addAll(subParts);
            } else {
                goodParts.add(part);
            }
        }

        return goodParts;
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
                1.2,
                "Typical weight for a black head");

        final Scale.Fraction minDistanceFromStaffLeftSide = new Scale.Fraction(
                4.0,
                "Minimum distance from left side of the staff");

        final Scale.Fraction minMeanWidth = new Scale.Fraction(
                1.0,
                "Minimum mean width for a black head");

        final Scale.Fraction maxSpotWidth = new Scale.Fraction(
                3.0,
                "Maximum width for a multi-head spot");

        final Scale.Fraction maxOutDx = new Scale.Fraction(
                0,
                "Maximum horizontal gap between head and stem");

        final Scale.Fraction maxInDx = new Scale.Fraction(
                0.2,
                "Maximum horizontal overlap between head and stem");

        final Scale.Fraction maxDy = new Scale.Fraction(
                1.0,
                "Maximum vertical gap between head and stem");

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

        final int minDistanceFromStaffLeftSide;

        final int minMeanWidth;

        final int maxSpotWidth;

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
            minDistanceFromStaffLeftSide = scale.toPixels(
                    constants.minDistanceFromStaffLeftSide);
            minMeanWidth = scale.toPixels(constants.minMeanWidth);
            maxSpotWidth = scale.toPixels(constants.maxSpotWidth);

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }
}
