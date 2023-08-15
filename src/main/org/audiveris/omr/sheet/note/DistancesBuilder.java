//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 D i s t a n c e s B u i l d e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sheet.note;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.image.ChamferDistance;
import org.audiveris.omr.image.DistanceTable;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.ui.DistanceBoard;
import org.audiveris.omr.sheet.ui.ScrollImageView;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.sheet.ui.TemplateBoard;
import org.audiveris.omr.sheet.ui.TemplateView;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.selection.AnchoredTemplateEvent;
import org.audiveris.omr.ui.selection.SelectionService;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.process.ByteProcessor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.SortedMap;

/**
 * Class <code>DistancesBuilder</code> provides the distance table to be used for notes
 * retrieval.
 *
 * @author Hervé Bitteur
 */
public class DistancesBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(DistancesBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Related sheet. */
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>DistancesBuilder</code> object.
     *
     * @param sheet related sheet
     */
    public DistancesBuilder (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------------//
    // buildDistances //
    //----------------//
    /**
     * Build the table of distances.
     *
     * @return the table of distance values
     */
    public DistanceTable buildDistances ()
    {
        // Compute the distance-to-foreground transform image
        final ByteProcessor rawBuf = sheet.getPicture().getSource(Picture.SourceKey.BINARY);
        final BufferedImage image = rawBuf.getBufferedImage();

        // "Erase" staff lines, ledgers, stems with background value in image
        paintLines(new ImagePainter(image)); // This modifies (shared) buffer

        // Table of distances to relevant foreground.
        final ByteProcessor buffer = new ByteProcessor(image);
        final DistanceTable table = new ChamferDistance.Short().computeToFore(buffer);

        // "Erase" staff lines, ledgers, stems with neutralized value in table
        paintLines(new TablePainter(table));

        // Display distances image in a template view?
        if ((OMR.gui != null) && constants.displayTemplates.isSet()) {
            SelectionService templateService = new SelectionService("templateService", new Class[]
            { AnchoredTemplateEvent.class });
            BufferedImage img = table.getImage(sheet.getScale().getInterline() / 2);
            TemplateBoard templateBoard = new TemplateBoard(sheet, table, templateService);
            sheet.getStub().getAssembly().addViewTab(
                    SheetTab.TEMPLATE_TAB,
                    new ScrollImageView(
                            sheet,
                            new TemplateView(sheet, img, table, templateService)),
                    new BoardsPane(new DistanceBoard(sheet, table), templateBoard));
            templateBoard.stateChanged(null); // To feed template service
        }

        return table;
    }

    //------------//
    // paintLines //
    //------------//
    /**
     * Paint the "neutralized" lines (staff lines, ledgers, stems) with a special value,
     * so that template matching can ignore these locations.
     */
    private void paintLines (Painter painter)
    {
        // Neutralize foreground due to staff lines / ledgers and stems
        for (SystemInfo system : sheet.getSystems()) {
            for (Staff staff : system.getStaves()) {
                if (staff.isTablature()) {
                    continue;
                }

                // "Erase" staff lines
                for (LineInfo line : staff.getLines()) {
                    // Paint the line glyph.
                    // Note this does not erase staff line pixels at crossing objects
                    Glyph glyph = line.getGlyph();
                    painter.paintGlyph(glyph);

                    // Also paint the whole line, even at crossing objects.
                    // If we don't do that, void heads grades are slightly underestimated,
                    // and black heads slightly overestimated.
                    double halfLine = 0.5 * glyph.getMeanThickness(Orientation.HORIZONTAL);
                    Point2D leftPt = line.getEndPoint(LEFT);
                    Point2D rightPt = line.getEndPoint(RIGHT);
                    int xMin = (int) Math.floor(leftPt.getX());
                    int xMax = (int) Math.ceil(rightPt.getX());

                    for (int x = xMin; x <= xMax; x++) {
                        double yl = line.yAt((double) x);
                        int yMin = (int) Math.rint(yl - halfLine);
                        int yMax = (int) Math.rint(yl + halfLine);

                        for (int y = yMin; y <= yMax; y++) {
                            painter.paintPixel(x, y);
                        }
                    }
                }

                // "Erase" ledgers
                SortedMap<Integer, List<LedgerInter>> ledgerMap = staff.getLedgerMap();

                for (List<LedgerInter> ledgers : ledgerMap.values()) {
                    for (LedgerInter ledger : ledgers) {
                        painter.paintGlyph(ledger.getGlyph());
                    }
                }
            }

            // "Erase" stem seeds
            List<Glyph> systemSeeds = system.getGroupedGlyphs(GlyphGroup.VERTICAL_SEED);

            for (Glyph seed : systemSeeds) {
                painter.paintGlyph(seed);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean displayTemplates = new Constant.Boolean(
                false,
                "Should we display the templates tab?");
    }

    //--------------//
    // ImagePainter //
    //--------------//
    private static class ImagePainter
            implements Painter
    {

        final Graphics2D g;

        public ImagePainter (BufferedImage img)
        {
            g = img.createGraphics();
            g.setColor(Color.WHITE);
        }

        @Override
        public void paintGlyph (Glyph glyph)
        {
            glyph.getRunTable().render(g, glyph.getTopLeft());
        }

        @Override
        public void paintPixel (int x,
                                int y)
        {
            g.fillRect(x, y, 1, 1);
        }
    }

    //---------//
    // Painter //
    //---------//
    private static interface Painter
    {

        void paintGlyph (Glyph glyph);

        void paintPixel (int x,
                         int y);
    }

    //--------------//
    // TablePainter //
    //--------------//
    private static class TablePainter
            implements Painter
    {

        final DistanceTable table;

        public TablePainter (DistanceTable table)
        {
            this.table = table;
        }

        @Override
        public void paintGlyph (Glyph glyph)
        {
            glyph.getRunTable().render(table, ChamferDistance.VALUE_UNKNOWN, glyph.getTopLeft());
        }

        @Override
        public void paintPixel (int x,
                                int y)
        {
            table.setValue(x, y, ChamferDistance.VALUE_UNKNOWN);
        }

    }
}
