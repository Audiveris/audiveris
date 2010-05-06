//----------------------------------------------------------------------------//
//                                                                            //
//                          S h e e t P a i n t e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;

import omr.log.Logger;

import omr.score.Score;
import omr.score.common.PixelRectangle;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.ui.ScoreConstants;
import omr.score.visitor.AbstractScoreVisitor;

import omr.sheet.Ending;
import omr.sheet.Ledger;
import omr.sheet.Sheet;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;

import omr.ui.symbol.ShapeSymbol;
import omr.ui.util.UIUtilities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImageOp;

/**
 * Class <code>SheetPainter</code> defines for every node in Score hierarchy
 * the rendering of related sections (with preset colors) in the dedicated
 * <b>Sheet</b> display.
 *
 * <p>Nota: It has been extended to deal with rendering of initial sheet
 * elements.
 *
 * @author Hervé Bitteur
 */
public class SheetPainter
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetPainter.class);

    //~ Instance fields --------------------------------------------------------

    /** Graphic context */
    private final Graphics2D g;

    /** Saved stroke for restoration at the end of the painting */
    private final Stroke oldStroke;

    /** Are we drawing editable boundaries? */
    private final boolean editableBoundaries;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // SheetPainter //
    //--------------//
    /**
     * Creates a new SheetPainter object.
     *
     * @param g Graphic context
     * @param editableBoundaries flag to draw editable boundaries
     */
    public SheetPainter (Graphics g,
                         boolean  editableBoundaries)
    {
        this.g = (Graphics2D) g;
        this.editableBoundaries = editableBoundaries;

        oldStroke = UIUtilities.setAbsoluteStroke(g, 1f);
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        // Render the measure ending barline, if within the clipping area
        if (measure.getBarline() != null) {
            measure.getBarline()
                   .render(g);
        }

        // Nothing lower than measure
        return false;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        score.acceptChildren(this);

        return false;
    }

    //-------------//
    // visit Staff //
    //-------------//
    @Override
    public boolean visit (Staff staff)
    {
        StaffInfo info = staff.getInfo();

        // Render the staff lines, if within the clipping area
        if ((info != null) && info.render(g)) {
            return true;
        } else {
            return false;
        }
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart part)
    {
        // Render the part starting barline, if any
        if (part.getStartingBarline() != null) {
            part.getStartingBarline()
                .render(g);
        }

        return true;
    }

    //-------------//
    // visit Sheet //
    //-------------//
    /**
     * Although a Sheet is not part of the Score hierarchy, this visitor has
     * been specifically extended to render all physical info of a sheet
     *
     * @param sheet the sheet to render initial elements
     * @return true
     */
    public boolean visit (Sheet sheet)
    {
        // Use specific color
        g.setColor(Color.lightGray);

        Score score = sheet.getScore();

        if ((score != null) && !score.getSystems()
                                     .isEmpty()) {
            // Normal (full) rendering of the score
            score.accept(this);
        } else {
            // Render what we have got so far
            if (sheet.getStaves() != null) {
                for (StaffInfo staff : sheet.getStaves()) {
                    staff.render(g);
                }
            }
        }

        if (sheet.getSystems() != null) {
            for (SystemInfo system : sheet.getSystems()) {
                visit(system);
            }
        } else {
            // Horizontals
            if (sheet.getHorizontals() != null) {
                // Ledgers
                for (Ledger ledger : sheet.getHorizontals()
                                          .getLedgers()) {
                    ledger.render(g);
                }

                // Endings
                for (Ending ending : sheet.getHorizontals()
                                          .getEndings()) {
                    ending.render(g);
                }
            }
        }

        g.setStroke(oldStroke);

        return true;
    }

    //--------------//
    // visit System //
    //--------------//
    @Override
    public boolean visit (ScoreSystem system)
    {
        if (system == null) {
            return false;
        }

        return visit(system.getInfo());
    }

    //------------------//
    // visit SystemInfo //
    //------------------//
    public boolean visit (SystemInfo systemInfo)
    {
        PixelRectangle bounds = systemInfo.getBounds();

        if (bounds == null) {
            return false;
        }

        // Check that this system is visible
        if (bounds.intersects(g.getClipBounds())) {
            g.setColor(Color.lightGray);

            // System boundary
            systemInfo.getBoundary()
                      .render(g, editableBoundaries);

            // Staff lines
            for (StaffInfo staff : systemInfo.getStaves()) {
                staff.render(g);
            }

            g.setColor(Color.black);

            // Stems
            for (Glyph glyph : systemInfo.getGlyphs()) {
                if (glyph.isStem()) {
                    Stick stick = (Stick) glyph;
                    stick.renderLine(g);
                }
            }

            // Ledgers
            for (Ledger ledger : systemInfo.getLedgers()) {
                ledger.render(g);
            }

            // Endings
            for (Ending ending : systemInfo.getEndings()) {
                ending.render(g);
            }

            // Virtual glyphs
            paintVirtualGlyphs(systemInfo);

            return true;
        }

        return false;
    }

    //--------------------//
    // paintVirtualGlyphs //
    //--------------------//
    /**
     * Paint the virtual glyphs on the sheet view
     * @param systemInfo the containing system
     */
    private void paintVirtualGlyphs (SystemInfo systemInfo)
    {
        int    interline = systemInfo.getSheet()
                                     .getScale()
                                     .interline();
        double ratio = interline / (double) ScoreConstants.INTER_LINE;

        for (Glyph glyph : systemInfo.getGlyphs()) {
            if (glyph.isVirtual()) {
                ShapeSymbol symbol = glyph.getShape()
                                          .getNakedShape()
                                          .getSymbol();

                if (symbol != null) {
                    ScoreSystem scoreSystem = systemInfo.getScoreSystem();

                    if (scoreSystem != null) {
                        Point center = glyph.getAreaCenter();
                        Point topLeft = new Point(
                            (int) Math.rint(
                                center.x - ((symbol.getWidth() * ratio) / 2)),
                            (int) Math.rint(
                                center.y - ((symbol.getHeight() * ratio) / 2)));
                        symbol.draw(g, topLeft, interline);
                    }
                }
            }
        }
    }
}
