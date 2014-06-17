//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      K e y s B u i l d e r                                     //
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

import omr.grid.StaffInfo;

import omr.math.Clustering;
import omr.math.GeoOrder;
import omr.math.Population;

import omr.sig.BarGroupRelation;
import omr.sig.BarlineInter;
import omr.sig.Inter;
import omr.sig.Relation;
import omr.sig.SIGraph;
import static omr.util.HorizontalSide.LEFT;
import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class {@code KeysBuilder} retrieves the key signature at the beginning of each staff
 * in a given system.
 * It can also be used to retrieve a key signature change located right after a double bar line.
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
 * A staff always begins with the standard sequence of components: (clef, key-sig?, time-sig?).
 * It is referred to as the "DMZ" in this program, because it cannot contain notes.
 * Within the same system, key signatures may vary from one staff to the other, however the DMZ
 * components (whether present or absent) stay vertically aligned across the system staves.
 * This feature can be used to validate the horizontal limits of key-sig among system staves, and
 * the horizontal limits of items slices.
 * This could be used also to look for clef or for time-sig.
 * <p>
 * The main tool is a projection of the DMZ onto the x-axis.
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
 * <li>Find first significant space right after minDmzWidth offset, it's the space that separates
 * the clef from next item (key-sig or time-sig or first note/rest, etc).
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
 * <li>Create one KeySigInter instance?
 * <li>Create one KeyAlterInter instance per item.
 * <li>Verify each item pitch in the staff (to be later matched against staff clef).
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class KeysBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(KeysBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    private final SIGraph sig;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Map of staff projectors. (one projector per staff) */
    private final Map<StaffInfo, KeyProjector> projectors = new TreeMap<StaffInfo, KeyProjector>(
            StaffInfo.byId);

    /** Theoretical abscissa offset for each slice. */
    private List<Integer> globalOffsets;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new KeysBuilder object.
     *
     * @param system the system to findClef
     */
    public KeysBuilder (SystemInfo system)
    {
        this.system = system;
        sig = system.getSig();
        sheet = system.getSheet();

        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // buildKeys //
    //-----------//
    /**
     * Process the DMZ that starts all staves in the system.
     */
    public void buildKeys ()
    {
        logger.debug("buildKeys for S#{}", system.getId());

        // Compute DMZ starts abscissae based on bar lines or staff starts
        computeDmzStarts();

        // Retrieve DMZ clefs
        int maxClefOffset = 0;

        for (StaffInfo staff : system.getStaves()) {
            int measureStart = staff.getDmzStart();

            // Retrieve staff clef
            new ClefBuilder(staff, measureStart).findClef();
            maxClefOffset = Math.max(maxClefOffset, staff.getClefStop() - measureStart);
        }

        refineDmz(maxClefOffset);

        // Retrieve DMZ key-sigs
        for (StaffInfo staff : system.getStaves()) {
            int measureStart = staff.getDmzStart();
            int browseStart = staff.getDmzStop();
            projectors.put(staff, new KeyProjector(staff, measureStart, browseStart, true));
        }

        // Process each staff separately
        for (KeyProjector projector : projectors.values()) {
            projector.process();
        }

        // Check staves consistency within system, if applicable
        if (system.getStaves().size() > 1) {
            checkSystemConsistency();
        }

        // Adjust each individual alter pitch, according to best matching key-sig
        for (KeyProjector projector : projectors.values()) {
            projector.adjustPitches();
        }

        // Push DMZ right after key-sig
        int maxKeyOffset = 0;

        for (StaffInfo staff : system.getStaves()) {
            int measureStart = staff.getDmzStart();
            Integer keyStop = staff.getKeyStop();

            if (keyStop != null) {
                maxKeyOffset = Math.max(maxKeyOffset, keyStop - measureStart);
            }
        }

        refineDmz(maxKeyOffset);

        // Compare clef and key
        //TODO
        // Retrieve DMZ time-sig
        //TODO
        int maxTimeOffset = 0;
        refineDmz(maxTimeOffset);
    }

    //------//
    // plot //
    //------//
    /**
     * Display the projection of the desired staff.
     *
     * @param staff the desired staff
     */
    public void plot (StaffInfo staff)
    {
        KeyProjector projector = projectors.get(staff);

        if (projector != null) {
            projector.plot();
        } else {
            logger.info("No key-projector for staff#{} yet", staff.getId());
        }
    }

    //------------------------//
    // checkSystemConsistency //
    //------------------------//
    /**
     * Verify global consistency of staves within the same system.
     */
    private void checkSystemConsistency ()
    {
        // Get theoretical abscissa offset for each slice in the system
        int meanSliceWidth = getGlobalOffsets();

        // Missing initial slice
        // Check that each initial inter is located at proper offset
        for (KeyProjector projector : projectors.values()) {
            for (int i = 0; i < projector.getSlices().size(); i++) {
                int x = projector.getSlices().get(i).getRect().x;
                int offset = x - projector.getMeasureStart();
                Integer index = getBestSliceIndex(offset);

                if (index != null) {
                    if (index > i) {
                        // Insert missing slice!
                        logger.info(
                                "{}Staff#{} slice inserted at index:{}",
                                sheet.getLogPrefix(),
                                projector.getId(),
                                i);
                        projector.insertSlice(i, globalOffsets.get(i));
                    }
                } else {
                    // Slice too far on left
                    logger.info(
                            "{}Staff#{} misaligned slice index:{} x:{}",
                            sheet.getLogPrefix(),
                            projector.getId(),
                            i,
                            x);

                    int newStart = projector.getMeasureStart() + globalOffsets.get(0);
                    Integer browseStart = projector.getBrowseStart();

                    if (browseStart != null) {
                        newStart = (browseStart + newStart) / 2; // Safer
                    }

                    projector.reprocess(newStart);
                }
            }
        }

        // TODO: Missing trailing slice(s)?
        for (KeyProjector projector : projectors.values()) {
            List<KeyProjector.Slice> slices = projector.getSlices();

            if (slices.size() < globalOffsets.size()) {
                for (int i = slices.size(); i < globalOffsets.size(); i++) {
                    int x = (projector.getMeasureStart() + globalOffsets.get(i)) - 1;
                    logger.debug("S#{} Should investigate slice {} at {}", projector.getId(), i, x);

                    boolean ok = projector.scanSlice(x, (x + meanSliceWidth) - 1);

                    if (!ok) {
                        break;
                    }
                }
            }
        }
    }

    //------------------//
    // computeDmzStarts //
    //------------------//
    /**
     * Computes the starting abscissa for each staff DMZ area, typically the point right
     * after the right-most bar line of the starting bar group.
     * TODO: could this be a more general routine in StaffInfo?
     *
     * @return measureStart at beginning of staff
     */
    private void computeDmzStarts ()
    {
        /** System bar lines, sorted on abscissa. */
        final List<Inter> systemBars = sig.inters(BarlineInter.class);
        Collections.sort(systemBars, Inter.byAbscissa);

        int margin = sheet.getScale().getInterline(); // Roughly

        for (StaffInfo staff : system.getStaves()) {
            Point2D leftPt = staff.getFirstLine().getEndPoint(LEFT);
            Rectangle luBox = new Rectangle(
                    (int) Math.floor(leftPt.getX()),
                    (int) Math.rint(leftPt.getY() + (staff.getHeight() / 2)),
                    margin,
                    0);
            luBox.grow(0, margin);

            TreeSet<Inter> bars = new TreeSet<Inter>(Inter.byAbscissa);
            bars.addAll(sig.intersectedInters(systemBars, GeoOrder.BY_ABSCISSA, luBox));

            if (bars.isEmpty()) {
                // No bar line found, so use the beginning abscissa of lines
                staff.setDmzStart((int) Math.rint(leftPt.getX()));
            } else {
                // Retrieve all bar lines grouped at beginning of staff
                Set<Inter> toAdd = new HashSet<Inter>();

                for (Inter inter : bars) {
                    Set<Relation> gRels = sig.getRelations(inter, BarGroupRelation.class);

                    for (Relation rel : gRels) {
                        toAdd.add(sig.getOppositeInter(inter, rel));
                    }
                }

                bars.addAll(toAdd);

                // Pick up the right-most bar line in the group
                BarlineInter last = (BarlineInter) bars.last();
                int right = last.getCenterRight().x + 1;
                logger.debug("Staff#{} right:{} bars: {}", staff.getId(), right, bars);

                staff.setDmzStart(right);
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

        for (KeyProjector projector : projectors.values()) {
            ///StringBuilder sb = new StringBuilder();
            ///sb.append("S#").append(projector.staff.getId());
            for (int i = 0; i < projector.getSlices().size(); i++) {
                KeyProjector.Slice slice = projector.getSlices().get(i);
                sliceCount++;
                meanSliceWidth += slice.getRect().width;

                int x = slice.getRect().x;
                int offset = x - projector.getMeasureStart();

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
            logger.debug("{} * {}", pi[k], law);
            theoreticals.add((int) Math.rint(law.getMean()));
        }

        globalOffsets = theoreticals;

        if (sliceCount > 0) {
            meanSliceWidth = (int) Math.rint(meanSliceWidth / (double) sliceCount);
        }

        logger.debug("globalOffsets:{} meanSliceWidth:{}", globalOffsets, meanSliceWidth);

        return meanSliceWidth;
    }

    //-----------//
    // refineDmz //
    //-----------//
    /**
     * Refine the DMZ end at system levem.
     * Use the fact that DMZ areas are vertically aligned within a system, even if the key-sig may
     * vary between staves. So we retrieve the largest offset since measureStart and use it to set
     * the DMZ end of each staff.
     */
    private void refineDmz (int largestOffset)
    {
        // Push this value to all staves
        if (largestOffset > 0) {
            for (StaffInfo staff : system.getStaves()) {
                staff.setDmzStop(staff.getDmzStart() + largestOffset);
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

        final Scale.Fraction minDmzWidth = new Scale.Fraction(
                3.0,
                "Minimum DMZ width (from staff left to end of clef)");

        final Scale.Fraction maxSliceDist = new Scale.Fraction(
                0.5,
                "Maximum x distance to theoretical slice");
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final int minDmzWidth;

        final int maxSliceDist;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            minDmzWidth = scale.toPixels(constants.minDmzWidth);
            maxSliceDist = scale.toPixels(constants.maxSliceDist);
        }
    }
}
