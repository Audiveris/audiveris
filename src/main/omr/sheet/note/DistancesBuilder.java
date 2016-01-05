//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 D i s t a n c e s B u i l d e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.note;

import omr.OMR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Symbol.Group;

import omr.image.ChamferDistance;
import omr.image.DistanceTable;

import omr.run.Orientation;

import omr.selection.AnchoredTemplateEvent;
import omr.selection.SelectionService;

import omr.sheet.Picture;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.grid.LineInfo;
import omr.sheet.ui.DistanceBoard;
import omr.sheet.ui.ScrollImageView;
import omr.sheet.ui.SheetTab;
import omr.sheet.ui.TemplateBoard;
import omr.sheet.ui.TemplateView;

import omr.sig.inter.LedgerInter;

import omr.ui.BoardsPane;

import static omr.util.HorizontalSide.LEFT;
import static omr.util.HorizontalSide.RIGHT;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.SortedMap;

/**
 * Class {@code DistancesBuilder} provides the distance table to be used for notes
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

    /** Table of distances to fore. */
    private DistanceTable table;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code DistancesBuilder} object.
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
    public DistanceTable buildDistances ()
    {
        // Compute the distance-to-foreground transform image
        Picture picture = sheet.getPicture();
        ByteProcessor buffer = picture.getSource(Picture.SourceKey.BINARY);
        table = new ChamferDistance.Short().computeToFore(buffer);

        // "Erase" staff lines, ledgers, stems
        paintLines();

        // Display distances image in a template view?
        if ((OMR.getGui() != null) && constants.displayTemplates.isSet()) {
            SelectionService templateService = new SelectionService(
                    "templateService",
                    new Class[]{AnchoredTemplateEvent.class});
            BufferedImage img = table.getImage(sheet.getScale().getInterline() / 2);
            TemplateBoard templateBoard = new TemplateBoard(sheet, table, templateService);
            sheet.getAssembly().addViewTab(
                    SheetTab.TEMPLATE_TAB,
                    new ScrollImageView(sheet, new TemplateView(sheet, img, table, templateService)),
                    new BoardsPane(new DistanceBoard(sheet, table), templateBoard));
            templateBoard.stateChanged(null); // To feed template service
        }

        return table;
    }

    //------------//
    // paintGlyph //
    //------------//
    private void paintGlyph (Glyph glyph)
    {
        glyph.getRunTable().render(table, ChamferDistance.VALUE_UNKNOWN, glyph.getTopLeft());
    }

    //------------//
    // paintLines //
    //------------//
    /**
     * Paint the "neutralized" lines (staff lines, ledgers, stems) with a special value,
     * so that template matching can ignore these locations.
     */
    private void paintLines ()
    {
        // Neutralize foreground due to staff lines / ledgers and stems
        for (SystemInfo system : sheet.getSystems()) {
            for (Staff staff : system.getStaves()) {
                // "Erase" staff lines
                for (LineInfo line : staff.getLines()) {
                    // Paint the line glyph
                    Glyph glyph = line.getGlyph();
                    paintGlyph(glyph);

                    // Also paint this line even at crossings with vertical objects
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
                            table.setValue(x, y, ChamferDistance.VALUE_UNKNOWN);
                        }
                    }
                }

                // "Erase" ledgers
                SortedMap<Integer, List<LedgerInter>> ledgerMap = staff.getLedgerMap();

                for (List<LedgerInter> ledgers : ledgerMap.values()) {
                    for (LedgerInter ledger : ledgers) {
                        paintGlyph(ledger.getGlyph());
                    }
                }
            }

            // "Erase" stem seeds
            List<Glyph> systemSeeds = system.lookupGroupedGlyphs(Group.VERTICAL_SEED);

            for (Glyph seed : systemSeeds) {
                paintGlyph(seed);
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

        private final Constant.Boolean displayTemplates = new Constant.Boolean(
                false,
                "Should we display the templates tab?");
    }
}
