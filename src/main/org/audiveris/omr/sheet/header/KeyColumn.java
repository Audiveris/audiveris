//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        K e y C o l u m n                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.sheet.header;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.Clustering;
import org.audiveris.omr.math.Population;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.header.KeyBuilder.ShapeBuilder;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.util.ChartPlotter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code KeyColumn} manages the system consistency for a column of staff-based
 * KeyBuilder instances.
 * <p>
 * First, each staff header in the system is independently searched for peaks, then slices.
 * For each staff slice, a connected component is first looked up (phase #1) and, if
 * unsuccessful, then a hard slice-based glyph is searched (phase #2).
 * <p>
 * Second, it is assumed that, within the same containing system: <ol>
 * <li>All staff key signatures start at similar abscissa offset since measure start,
 * <li>All staff key slices have similar widths across staves, even between small and standard
 * staves, and regardless of key alter shape (SHARP, FLAT or NATURAL),
 * <li>Slices are allocated based on detected ink peaks within header projection.
 * Hence, an allocated slice indicates the presence of ink (i.e. a slice is never empty).
 * <li>Sharp-based and flat-based keys can be mixed in a system, but not within the same part.
 * <li>The number of key items may vary across staves, but not within the same part.
 * <li>The longest key signature defines an abscissa range which, whatever the system staff, can
 * contain either a key signature or nothing (no ink).
 * <li>If a slice content in a key area cannot be recognized as key item although it contains ink,
 * this slice is marked as "stuffed".
 * The corresponding slice within the other staves cannot contain any key item either and are thus
 * also marked as "stuffed".
 * <li>In a staff, any slice following a stuffed slice is also a stuffed slice.
 * </ol>
 * At system level, we make sure that any multi-staff part has the same key signature, by
 * "replicating" the best signature to the other staff (or staves).
 * <p>
 * Example of key shapes mixed in the same system:<br>
 * <img src="doc-files/IMSLP00693-1-MixedKeyShapesInSystem.png">
 *
 * @author Hervé Bitteur
 */
public class KeyColumn
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(KeyColumn.class);

    //~ Enumerations -------------------------------------------------------------------------------
    /** Status of key replication within part. */
    public enum PartStatus
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Success. */
        OK,
        /** Slice count to be reduced. */
        SHRINK,
        /** No clef in staff. */
        NO_CLEF,
        /** Replication failed. */
        NO_REPLICATE,
        /** No key in part. */
        DESTROY;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related system. */
    private final SystemInfo system;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Map of key builders. (one per staff) */
    private final Map<Staff, KeyBuilder> builders = new TreeMap<Staff, KeyBuilder>(Staff.byId);

    /** Theoretical abscissa offset for each slice. */
    private List<Integer> globalOffsets;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code KeyColumn} object.
     *
     * @param system underlying system
     */
    public KeyColumn (SystemInfo system)
    {
        this.system = system;
        params = new Parameters(system.getSheet().getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // addPlot //
    //---------//
    /**
     * Draw key signature portion for a given staff within header projection.
     *
     * @param plotter   plotter for header
     * @param staff     desired staff
     * @param projWidth projection width
     * @return a string that describes staff key signature, if any
     */
    public String addPlot (ChartPlotter plotter,
                           Staff staff,
                           int projWidth)
    {
        int measStart = staff.getHeaderStart();
        int browseStart = (staff.getClefStop() != null) ? staff.getClefStop() : measStart;
        KeyBuilder builder = new KeyBuilder(this, staff, projWidth, measStart, browseStart, true);
        builder.addPlot(plotter);

        KeyInter key = staff.getHeader().key;

        return (key != null) ? ("key:" + key.getFifths()) : null;
    }

    //--------------//
    // retrieveKeys //
    //--------------//
    /**
     * Retrieve the column of staves keys in this system.
     *
     * @param projectionWidth desired width for projection
     * @return the ending abscissa offset of keys column WRT measure start, or 0 if none
     */
    public int retrieveKeys (int projectionWidth)
    {
        // Define each staff key-signature area
        for (Staff staff : system.getStaves()) {
            int measStart = staff.getHeaderStart();

            Integer clefStop = staff.getClefStop(); // Not very reliable...
            int browseStart = (clefStop != null) ? (clefStop + 1) : (staff.getHeaderStop() + 1);
            //            int browseStart = staff.getHeaderStop() + 1;
            builders.put(
                    staff,
                    new KeyBuilder(this, staff, projectionWidth, measStart, browseStart, true));
        }

        // Process each staff to get peaks, slices, alters, trailing space, clef compatibility
        for (KeyBuilder builder : builders.values()) {
            builder.process();
        }

        if (system.isMultiStaff()) {
            // Check keys alignment across staves at system level
            if (!checkSystemSlices()) {
                for (KeyBuilder builder : builders.values()) {
                    builder.destroyAll();
                }

                return 0; // No key in system
            }
        }

        // Adjust each individual alter pitch, according to best matching key-sig
        // A staff may have no key-sig while the others have some in the same system
        for (KeyBuilder builder : builders.values()) {
            builder.finalizeKey();
        }

        // Record samples? both positive and negative ones
        if (params.recordPositives || params.recordNegatives) {
            for (KeyBuilder builder : builders.values()) {
                builder.recordSamples(params.recordPositives, params.recordNegatives);
            }
        }

        // Push header key stop
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

    //-----------------//
    // getMaxSliceDist //
    //-----------------//
    final int getMaxSliceDist ()
    {
        return params.maxSliceDist;
    }

    //----------------//
    // getGlobalIndex //
    //----------------//
    /**
     * Determine the corresponding global index for the provided abscissa offset.
     *
     * @param offset slice offset
     * @return the global index, or null
     */
    Integer getGlobalIndex (int offset)
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

        if (bestDist <= getMaxSliceDist()) {
            return bestIndex;
        } else {
            return null;
        }
    }

    //-----------------//
    // getGlobalOffset //
    //-----------------//
    int getGlobalOffset (int index)
    {
        return globalOffsets.get(index);
    }

    //-------------------//
    // checkSystemSlices //
    //-------------------//
    /**
     * Use rule of vertical alignment of keys items within the same system.
     *
     * @return true if OK
     */
    private boolean checkSystemSlices ()
    {
        // Get theoretical abscissa offset for each slice in the system
        final int meanSliceWidth = getGlobalOffsets();

        if (globalOffsets.isEmpty()) {
            return false; // No key sig for the system
        }

        if (logger.isDebugEnabled()) {
            printSliceTable();
        }

        // All staves within the same part should have identical key signatures
        // Strategy: pick up the "best" KeyInter and try to replicate it in the other stave(s)
        PartLoop:
        for (Part part : system.getParts()) {
            List<Staff> staves = part.getStaves();

            if (staves.size() > 1) {
                KeyInter best = getBestIn(staves);

                if (best != null) {
                    final int fifths = best.getFifths();
                    final Staff bestStaff = best.getStaff();
                    final KeyBuilder bestKeyBuilder = builders.get(bestStaff);
                    final ShapeBuilder bestBuilder = bestKeyBuilder.getShapeBuilder(fifths);
                    boolean modified;

                    do {
                        modified = false;

                        StaffLoop:
                        for (Staff staff : staves) {
                            if (staff == bestStaff) {
                                bestKeyBuilder.getShapeBuilder(-fifths).destroy();
                            } else {
                                KeyBuilder builder = builders.get(staff);

                                switch (builder.checkReplicate(bestBuilder)) {
                                case OK:
                                case NO_CLEF:
                                case NO_REPLICATE:
                                    break;

                                case SHRINK:
                                    globalOffsets.remove(globalOffsets.size() - 1);
                                    bestBuilder.shrink();
                                    modified = true;

                                    break StaffLoop;

                                case DESTROY:
                                    return false;
                                }
                            }
                        }
                    } while (modified);
                }
            }
        }

        ///spreadStuff(); // ????????????????????
        if (logger.isDebugEnabled()) {
            printSliceTable();
        }

        return true;
    }

    //-----------//
    // getBestIn //
    //-----------//
    /**
     * Report the best KeyInter instance found in the provided staves.
     *
     * @param staves the (part) staves
     * @return the best keyInter found, perhaps null
     */
    private KeyInter getBestIn (List<Staff> staves)
    {
        KeyInter best = null;
        double bestGrade = -1;

        for (Staff staff : staves) {
            KeyBuilder builder = builders.get(staff);
            KeyInter keyInter = builder.getBestKeyInter();

            if (keyInter != null) {
                double ctxGrade = keyInter.getBestGrade();

                if ((best == null) || (ctxGrade > bestGrade)) {
                    best = keyInter;
                    bestGrade = ctxGrade;
                }
            }
        }

        return best;
    }

    //------------------//
    // getGlobalOffsets //
    //------------------//
    /**
     * Retrieve the theoretical abscissa offset for all slices in the system.
     * This populates the 'globalOffsets' list.
     *
     * @return the mean slice width, computed on all populated slices in all headers in the system.
     */
    private int getGlobalOffsets ()
    {
        int sliceCount = 0;
        int meanSliceWidth = 0;

        // Check that key-sig slices appear rather vertically aligned across system staves
        List<Population> pops = new ArrayList<Population>(); // 1 population per slice index
        List<Double> vals = new ArrayList<Double>(); // All offset values

        for (KeyBuilder builder : builders.values()) {
            KeyInter bestInter = builder.getBestKeyInter();

            if (bestInter != null) {
                final ShapeBuilder shapeBuilder = builder.getShapeBuilder(bestInter.getFifths());
                final KeyRoi roi = shapeBuilder.getRoi();

                for (int i = 0; i < roi.size(); i++) {
                    KeySlice slice = roi.get(i);

                    ///if (slice.getAlter() != null) {
                    int x = slice.getRect().x;
                    int offset = x - builder.getMeasureStart();
                    meanSliceWidth += slice.getRect().width;
                    sliceCount++;

                    while (i >= pops.size()) {
                        pops.add(new Population());
                    }

                    pops.get(i).includeValue(offset);
                    vals.add((double) offset);

                    ///}
                }
            }
        }

        int G = pops.size();
        Clustering.Gaussian[] laws = new Clustering.Gaussian[G];

        for (int i = 0; i < G; i++) {
            Population pop = pops.get(i);
            laws[i] = new Clustering.Gaussian(pop.getMeanValue(), 1.0); //pop.getStandardDeviation());
        }

        // Copy vals list into a table of double's
        double[] table = new double[vals.size()];

        for (int i = 0; i < vals.size(); i++) {
            table[i] = vals.get(i);
        }

        Clustering.EM(table, laws);

        List<Integer> theoreticals = new ArrayList<Integer>();

        for (int k = 0; k < G; k++) {
            Clustering.Gaussian law = laws[k];
            theoreticals.add((int) Math.rint(law.getMean()));
        }

        globalOffsets = theoreticals;

        if (sliceCount > 0) {
            meanSliceWidth = (int) Math.rint(meanSliceWidth / (double) sliceCount);
        }

        logger.debug(
                "System#{} offsets:{} meanWidth:{}",
                system.getId(),
                globalOffsets,
                meanSliceWidth);

        return meanSliceWidth;
    }

    //-----------------//
    // printSliceTable //
    //-----------------//
    /**
     * Based on retrieved global offsets, draw a system table of key slices, annotated
     * with data from each staff.
     */
    private void printSliceTable ()
    {
        StringBuilder title = new StringBuilder();
        title.append(String.format("System#%-2d    ", system.getId()));

        for (int i = 1; i <= globalOffsets.size(); i++) {
            title.append(String.format("---%d---  ", i));
        }

        logger.info("{}", title);

        for (KeyBuilder builder : builders.values()) {
            builder.printSliceTable();
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

        private final Constant.Boolean recordPositiveSamples = new Constant.Boolean(
                false,
                "Should we record positive samples from KeyBuilder?");

        private final Constant.Boolean recordNegativeSamples = new Constant.Boolean(
                false,
                "Should we record negative samples from KeyBuilder?");

        private final Scale.Fraction maxSliceDist = new Scale.Fraction(
                0.5,
                "Maximum abscissa distance to theoretical slice");
    }

    //------------//
    // Parameters //
    //------------//
    private static final class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final boolean recordPositives;

        final boolean recordNegatives;

        final int maxSliceDist;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            recordPositives = constants.recordPositiveSamples.isSet();
            recordNegatives = constants.recordNegativeSamples.isSet();
            maxSliceDist = scale.toPixels(constants.maxSliceDist);
        }
    }
}
//
//    //-------------//
//    // spreadStuff //
//    //-------------//
//    /**
//     * Spread stuffed slices across staves.
//     * A "stuffed" slice is occupied by some ink which is not considered as a key alter, therefore
//     * this abscissa cannot be occupied by a key alter in any other staff within the same system.
//     */
//    private void spreadStuff ()
//    {
//        Integer firstStuffIndex = getFirstStuffIndex();
//
//        if (firstStuffIndex != null) {
//            for (KeyBuilder builder : builders.values()) {
//                final int bid = builder.getId();
//                final KeyRoi roi = builder.getRoi();
//
//                for (int i = 0; i < roi.size(); i++) {
//                    KeySlice slice = roi.get(i);
//
//                    if (!slice.isStuffed()) {
//                        int x = slice.getRect().x;
//                        int offset = x - builder.getMeasureStart();
//                        Integer index = getGlobalIndex(offset);
//
//                        if (index >= firstStuffIndex) {
//                            logger.debug("Staff#{} stuff spread from slice {}", bid, index + 1);
//                            builder.getRoi().stuffSlicesFrom(index);
//
//                            break;
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//
//    //--------------------//
//    // getFirstStuffIndex //
//    //--------------------//
//    private Integer getFirstStuffIndex ()
//    {
//        Integer firstStuffIndex = null;
//
//        for (KeyBuilder builder : builders.values()) {
//            final KeyRoi roi = builder.getRoi();
//
//            for (int i = 0; i < roi.size(); i++) {
//                KeySlice slice = roi.get(i);
//
//                if (slice.isStuffed()) {
//                    int x = slice.getRect().x;
//                    int offset = x - builder.getMeasureStart();
//                    Integer index = getGlobalIndex(offset);
//
//                    if (index != null) {
//                        if ((firstStuffIndex == null) || (firstStuffIndex > index)) {
//                            firstStuffIndex = index;
//                        }
//                    }
//                }
//            }
//        }
//
//        return firstStuffIndex;
//    }
//
