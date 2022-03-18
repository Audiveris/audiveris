//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     R u n B e a m C h e c k                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.BeamPortion;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.step.RunClass;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.OmrFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.Symbols;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;

/**
 * Class {@code RunBeamCheck} checks for stem connection with beams and beam hooks,
 * to retrieve precise locations for nones (beam hooks, beams, beams interval)
 * at half-head horizontal distance from stem.
 *
 * @author Hervé Bitteur
 */
public class RunBeamCheck
        extends RunClass
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(RunBeamCheck.class);

    private static final ShapeSymbol HEAD_SYMBOL = Symbols.getSymbol(Shape.NOTEHEAD_BLACK);

    //~ Instance fields ----------------------------------------------------------------------------
    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RunBeamCheck} object.
     *
     * @param book     book to process
     * @param sheetIds sheet IDS if any
     */
    public RunBeamCheck (Book book,
                         SortedSet<Integer> sheetIds)
    {
        super(book, sheetIds);

    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void process ()
    {
        // Write info into separate folder
        final Path outDir = book.getInputPath().getParent().resolveSibling("HEAD").resolve("nones");

        try {
            for (SheetStub stub : book.getValidStubs()) {
                if (sheetIds != null && !sheetIds.contains(stub.getNumber())) {
                    continue;
                }

                if (!stub.isDone(Step.REDUCTION)) {
                    continue;
                }

                final Set<Point> locations = new LinkedHashSet<>();
                final Sheet sheet = stub.getSheet();

                final int hDist = computeTypicalDistance(sheet);
                logger.debug("hDist: {}", hDist);

                for (SystemInfo system : sheet.getSystems()) {
                    final SIGraph sig = system.getSig();

                    for (Inter beamInter : sig.inters(AbstractBeamInter.class)) {
                        final AbstractBeamInter beam = (AbstractBeamInter) beamInter;
                        final Line2D median = beam.getMedian();

                        for (Relation rel : sig.getRelations(beamInter, BeamStemRelation.class)) {
                            final BeamStemRelation bs = (BeamStemRelation) rel;
                            final BeamPortion portion = bs.getBeamPortion();
                            final StemInter stem = (StemInter) sig.getOppositeInter(beamInter, bs);
                            final Set<HeadInter> heads = stem.getHeads();
                            final Staff staff = heads.iterator().next().getStaff();
                            final int xStem = stem.getCenter().x;

                            // x is imposed by stem & portion of beam
                            final int[] xs;
                            switch (portion) {
                            case CENTER:
                                xs = new int[]{xStem - hDist, xStem + hDist};
                                break;
                            case LEFT:
                                xs = new int[]{xStem + hDist};
                                break;
                            default:
                                xs = new int[]{xStem - hDist};
                                break;
                            }

                            // y is imposed by nearest line / interval
                            for (int x : xs) {
                                final Point2D pt = LineUtil.intersectionAtX(median, x);
                                final int pitch = (int) Math.rint(staff.pitchPositionOf(pt));

                                for (int i = -1; i <= 1; i++) {
                                    int p = pitch + i;
                                    int y = (int) Math.rint(staff.pitchToOrdinate(x, p));
                                    locations.add(new Point(x, y));
                                }
                            }
                        }
                    }
                }

                if (!locations.isEmpty()) {
                    // Export one file per sheet
                    Files.createDirectories(outDir);
                    final String name = sheet.getId();
                    final Path outPath = outDir.resolve(name + ".nones.csv");

                    try (PrintWriter pw = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(Files.newOutputStream(outPath), UTF_8)))) {
                        for (Point location : locations) {
                            final StringBuilder sb = new StringBuilder();
                            sb.append(location.x).append(',').append(location.y);
                            pw.println(sb);
                        }

                        pw.flush();
                    }

                    logger.info("Locations exported as {}", outPath);
                }
            }
        } catch (Exception ex) {
            logger.warn("Error exporting beam negatives {}", ex.toString(), ex);
        }
    }

    //------------------------//
    // computeTypicalDistance //
    //------------------------//
    /**
     * We use half width of noteheadBlack as the typical distance between stem and head.
     *
     * @param sheet containing sheet
     * @return the typical x distance
     */
    private int computeTypicalDistance (Sheet sheet)
    {
        final Scale scale = sheet.getScale();
        final int interline = hasTablature(sheet) ? scale.getSmallInterline() : scale.getInterline();
        final OmrFont font = MusicFont.getBaseFont(interline);
        final TextLayout layout = font.layout(HEAD_SYMBOL);
        final Rectangle2D bounds = layout.getBounds();

        return (int) Math.rint(bounds.getWidth() / 2.0);
    }

    private boolean hasTablature (Sheet sheet)
    {
        for (SystemInfo system : sheet.getSystems()) {
            for (Staff staff : system.getStaves()) {
                if (staff.isTablature()) {
                    return true;
                }
            }
        }

        return false;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
