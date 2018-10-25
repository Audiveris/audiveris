//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G l y p h F a c t o r y                                    //
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
package org.audiveris.omr.glyph;

import ij.process.ByteProcessor;

import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.run.MarkedRun;

import static org.audiveris.omr.run.Orientation.VERTICAL;

import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.util.ByteUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class {@code GlyphFactory} builds a collection of glyphs out of a provided {@link
 * RunTable}.
 * <p>
 * A instance of this factory class is dedicated to the one-shot processing of a source.
 * It can be used only once.
 * <p>
 * Comments refer to 'sequences', which are synonymous of columns for vertical runs, and of rows
 * for horizontal runs.
 *
 * @author Hervé Bitteur
 */
public class GlyphFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Source runs. */
    private final RunTable runTable;

    /** Absolute offset of runTable topLeft corner. */
    private final Point offset;

    /** Target group for all created glyphs. */
    private final GlyphGroup group;

    /** Global list of all glyphs created. */
    private final List<Glyph> created = new ArrayList<Glyph>();

    /** Global id to assign glyph marks. */
    private int globalMark;

    /** Specific run table implementation, meant for marking runs. */
    private final List<List<MarkedRun>> markedTable;

    /** Merges (child => parent). (numerical invariant: child > parent) */
    private final Map<Integer, Integer> merges = new HashMap<Integer, Integer>();

    /** Most efficient way to use merging information. */
    private int[] lut;

    //~ Constructors -------------------------------------------------------------------------------
    private GlyphFactory (RunTable runTable,
                          Point offset,
                          GlyphGroup group)
    {
        this.runTable = runTable;
        this.offset = (offset != null) ? offset : new Point(0, 0);
        this.group = group;

        // Allocate & initialize markedTable
        markedTable = new ArrayList<List<MarkedRun>>(runTable.getSize());

        for (int iseq = 0, size = runTable.getSize(); iseq < size; iseq++) {
            markedTable.add(new ArrayList<MarkedRun>());
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // buildGlyph //
    //------------//
    /**
     * Build one glyph from a collection of glyph parts.
     *
     * @param parts the provided glyph parts
     * @return the glyph compound
     */
    public static Glyph buildGlyph (Collection<? extends Glyph> parts)
    {
        final Rectangle box = Glyphs.getBounds(parts);
        final ByteProcessor buffer = new ByteProcessor(box.width, box.height);
        ByteUtil.raz(buffer); // buffer.invert();

        for (Glyph part : parts) {
            part.getRunTable().write(buffer, part.getLeft() - box.x, part.getTop() - box.y);
        }

        final RunTable runTable = new RunTableFactory(VERTICAL).createTable(buffer);

        return new Glyph(box.x, box.y, runTable);
    }

    //-------------//
    // buildGlyphs //
    //-------------//
    /**
     * Create a collection of glyphs out of the provided RunTable.
     *
     * @param runTable the source table of runs
     * @param offset   offset of runTable WRT absolute origin
     * @return the list of glyphs created
     */
    public static List<Glyph> buildGlyphs (RunTable runTable,
                                           Point offset)
    {
        return new GlyphFactory(runTable, offset, null).process();
    }

    //-------------//
    // buildGlyphs //
    //-------------//
    /**
     * Create a collection of glyphs out of the provided RunTable.
     *
     * @param runTable the source table of runs
     * @param offset   offset of runTable WRT absolute origin
     * @param group    targeted group, if any
     * @return the list of glyphs created
     */
    public static List<Glyph> buildGlyphs (RunTable runTable,
                                           Point offset,
                                           GlyphGroup group)
    {
        return new GlyphFactory(runTable, offset, group).process();
    }

    /**
     * Build all the ancestor glyphs from the markedTable.
     */
    private void buildAllGlyphs ()
    {
        logger.debug("glyphs: {}", globalMark - merges.size());

        // Allocate & initialize glyph bufs
        final List<List<Sequence>> bufs = new ArrayList<List<Sequence>>(lut.length);

        for (int i = 0, len = lut.length; i < len; i++) {
            if ((i > 0) && (lut[i] == i)) {
                bufs.add(new ArrayList<Sequence>());
            } else {
                bufs.add(null);
            }
        }

        // Dispatch each run to its proper glyph buffer
        for (int iSeq = 0, size = runTable.getSize(); iSeq < size; iSeq++) {
            final List<MarkedRun> seq = markedTable.get(iSeq);

            for (MarkedRun run : seq) {
                final int ancestor = lut[run.getMark()];
                final List<Sequence> buf = bufs.get(ancestor);

                if (buf.isEmpty() || (buf.get(buf.size() - 1).iSeq != iSeq)) {
                    buf.add(new Sequence(iSeq));
                }

                final Sequence bufSeq = buf.get(buf.size() - 1);
                bufSeq.runs.add(run);
            }
        }

        // Each allocated buf corresponds to one separated glyph
        for (int i = 1, len = lut.length; i < len; i++) {
            final List<Sequence> buf = bufs.get(i);

            if (buf != null) {
                buildGlyph(i, buf);
            }
        }
    }

    /**
     * Build the glyph for provided ancestor mark.
     *
     * @param mark the mark value for this ancestor glyph
     * @param buf  its populated sequences of runs
     */
    private void buildGlyph (int mark,
                             List<Sequence> buf)
    {
        // Determine glyph bounds within buf
        final int iSeqMin = buf.get(0).iSeq;
        final int iSeqMax = buf.get(buf.size() - 1).iSeq;

        int startMin = Integer.MAX_VALUE;
        int stopMax = 0;

        for (Sequence seq : buf) {
            startMin = Math.min(startMin, seq.runs.get(0).getStart());
            stopMax = Math.max(stopMax, seq.runs.get(seq.runs.size() - 1).getStop());
        }

        final int dx = (runTable.getOrientation() == VERTICAL) ? iSeqMin : startMin;
        final int dy = (runTable.getOrientation() == VERTICAL) ? startMin : iSeqMin;
        final int width = (runTable.getOrientation() == VERTICAL) ? (iSeqMax - iSeqMin + 1)
                : (stopMax - startMin + 1);
        final int height = (runTable.getOrientation() == VERTICAL) ? (stopMax - startMin + 1)
                : (iSeqMax - iSeqMin + 1);

        // Allocate table with proper dimension
        RunTable table = new RunTable(runTable.getOrientation(), width, height);

        // Populate table with RLE sequences
        for (Sequence seq : buf) {
            for (MarkedRun run : seq.runs) {
                run.setStart(run.getStart() - startMin);
            }

            table.setSequence(seq.iSeq - iSeqMin, seq.runs);
        }

        // Store created glyph
        final Glyph glyph = new Glyph(offset.x + dx, offset.y + dy, table);
        glyph.addGroup(group);
        created.add(glyph);
    }

    /**
     * Build a LUT to provide, for any run mark, the mark of its containing ancestor.
     */
    private void createLut ()
    {
        lut = new int[1 + globalMark]; // Cell #0 is not used

        for (int i = 0; i <= globalMark; i++) {
            lut[i] = i;
        }

        // Make any redirected child point directly to its ultimate ancestor
        for (Entry<Integer, Integer> entry : merges.entrySet()) {
            int child = entry.getKey();
            Integer parent = entry.getValue();
            Integer ancestor;

            do { // Walk down the merges path
                ancestor = parent;
                parent = merges.get(parent);
            } while (parent != null);

            lut[child] = ancestor;
        }
    }

    /**
     * Remember that runs marked with 'max' or 'min' values belong to the same glyph.
     * This equivalence is recorded only in the max => min direction.
     *
     * @param max the larger mark value
     * @param min the smaller mark value
     */
    private void merge (int max,
                        int min)
    {
        Integer old = merges.get(max);

        if (old == null) {
            merges.put(max, min); // Target is max=>min
        } else if (min > old) { // Target is max=>min---old
            merge(min, old);
            merges.put(max, min);
        } else if (old != min) {
            merge(old, min); // Target is max=>old---min
        }
    }

    /**
     * Retrieve all glyphs from the provided table of runs.
     *
     * @param runTable the table of runs
     * @return the list of created glyphs
     */
    private List<Glyph> process ()
    {
        //        StopWatch watch = new StopWatch("GlyphFactory");
        //
        //        try {
        //            watch.start("scan");
        scanTable();
        //
        //            watch.start("lut");
        createLut();
        //
        //            watch.start("glyphs");
        buildAllGlyphs();

        return created;

        //        } finally {
        //            watch.print();
        //        }
    }

    /**
     * Populate the 'markedTable', a temporary representation of runs with their
     * connectivity recorded in 'merges'.
     * <p>
     * Browse the sequences of the input run table, detect run overlap from one sequence to the
     * next, and flag each run with proper glyph mark.
     */
    private void scanTable ()
    {
        // Scan each pair of consecutive sequences
        for (int iSeq = 0, size = runTable.getSize(); iSeq < size; iSeq++) {
            final List<MarkedRun> prevSeq = (iSeq > 0) ? markedTable.get(iSeq - 1) : null;
            final List<MarkedRun> nextSeq = markedTable.get(iSeq);
            final int maxPIdx = (iSeq > 0) ? (prevSeq.size() - 1) : (-1);
            int pIdxActive = 0; // Active run index in prev sequence

            for (Iterator<Run> it = runTable.iterator(iSeq); it.hasNext();) {
                // Allocate the MarkedRun that corresponds to current Run
                final Run run = it.next();
                final int nextStart = run.getStart();
                final int nextStop = run.getStop();
                final MarkedRun nextRun = new MarkedRun(nextStart, run.getLength(), 0);
                nextSeq.add(nextRun);

                // Browse marked runs from previous sequence
                for (int pIdx = pIdxActive; pIdx <= maxPIdx; pIdx++) {
                    final MarkedRun prevRun = prevSeq.get(pIdx);

                    if (prevRun.getStart() > nextStop) {
                        break;
                    }

                    if (prevRun.getStop() >= nextStart) {
                        final int prevMark = prevRun.getMark();
                        final int nextMark = nextRun.getMark();

                        if (nextMark == 0) {
                            nextRun.setMark(prevMark);
                        } else {
                            final int min = Math.min(prevMark, nextMark);
                            final int max = Math.max(prevMark, nextMark);

                            if (min != max) {
                                merge(max, min); // Record equivalence between these 2 marks
                            }
                        }

                        pIdxActive = pIdx;
                    }
                }

                // No overlap found, hence use a new mark
                if (nextRun.getMark() == 0) {
                    nextRun.setMark(++globalMark);
                }
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // Sequence //
    //----------//
    /**
     * A sequence of marked runs, that all belong to the same glyph, together with the
     * sequence index in runTable.
     */
    private static class Sequence
    {
        //~ Instance fields ------------------------------------------------------------------------

        final int iSeq; // Index in runTable

        final List<MarkedRun> runs = new ArrayList<MarkedRun>(); // Sequence of glyph marked runs

        //~ Constructors ---------------------------------------------------------------------------
        public Sequence (int iSeq)
        {
            this.iSeq = iSeq;
        }
    }
}
//
//        private void dumpTable (String title)
//        {
//            System.out.println(title);
//
//            for (int iSeq = 0, size = markedTable.size(); iSeq < size; iSeq++) {
//                System.out.println("iSeq = " + iSeq);
//
//                List<MarkedRun> seq = markedTable.get(iSeq);
//
//                for (MarkedRun run : seq) {
//                    System.out.println("   run = " + run);
//                }
//            }
//        }
//
