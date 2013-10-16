//----------------------------------------------------------------------------//
//                                                                            //
//                          S h e e t P a i n t e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.math.GeoUtil;

import omr.score.entity.Measure;
import omr.score.entity.Page;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;
import omr.score.visitor.AbstractScoreVisitor;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.sig.BeamInter;
import omr.sig.BlackHeadInter;
import omr.sig.Inter;
import omr.sig.InterVisitor;
import omr.sig.LedgerInter;
import omr.sig.SIGraph;
import omr.sig.StemInter;

import omr.ui.Colors;
import omr.ui.symbol.Alignment;
import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.Symbols;
import omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ConcurrentModificationException;

/**
 * Class {@code SheetPainter} defines for every node in Page hierarchy
 * the rendering of related sections (with preset colors) in the
 * dedicated <b>Sheet</b> display.
 *
 * <p>Nota: It has been extended to deal with rendering of initial sheet
 * elements.
 *
 * @author Hervé Bitteur
 */
public class SheetPainter
        extends AbstractScoreVisitor
        implements InterVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SheetPainter.class);

    //~ Instance fields --------------------------------------------------------
    /** Graphic context. */
    private final Graphics2D g;

    /** Clip rectangle. */
    private Rectangle clip;

    /** Alpha composite for interpretations. */
    private AlphaComposite composite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            0.5f);

    /** Default full composite. */
    private AlphaComposite fullComposite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            1f);

    /** Saved stroke for restoration at the end of the painting. */
    private final Stroke oldStroke;

    /** Are we drawing editable boundaries?. */
    private final boolean editableBoundaries;

    /** Music font properly scaled. */
    private MusicFont musicFont;

    /** Stroke for ledgers. */
    private Stroke ledgerStroke;

    /** Stroke for stems. */
    private Stroke stemStroke;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // SheetPainter //
    //--------------//
    /**
     * Creates a new SheetPainter object.
     *
     * @param g                  Graphic context
     * @param editableBoundaries flag to draw editable boundaries
     */
    public SheetPainter (Graphics g,
                         boolean editableBoundaries)
    {
        this.g = (Graphics2D) g;
        this.clip = g.getClipBounds();
        this.editableBoundaries = editableBoundaries;

        oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
        stemStroke = new BasicStroke(
                3f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND);
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        try {
            // Render the measure ending barline
            if (measure.getBarline() != null) {
                measure.getBarline()
                        .renderLine(g);
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + measure,
                    ex);
        }

        // Nothing lower than measure
        return false;
    }

    //------------//
    // visit Page //
    //------------//
    @Override
    public boolean visit (Page page)
    {
        try {
            Sheet sheet = page.getSheet();

            ledgerStroke = new BasicStroke(
                    sheet.getScale().getMainFore(),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND);

            // Use specific color
            g.setColor(Color.BLACK);

            if (!page.getSystems()
                    .isEmpty()) {
                // Small protection about changing data...
                if (sheet.getScale() == null) {
                    return false;
                }

                // Determine proper font
                musicFont = MusicFont.getFont(sheet.getInterline());

                // Normal (full) rendering of the score
                page.acceptChildren(this);
            } else {
                // Render what we have got so far
                sheet.getStaffManager()
                        .render(g);
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + page,
                    ex);
        } finally {
            g.setStroke(oldStroke);
        }

        return false;
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart part)
    {
        try {
            // Render the part starting barline, if any
            if (part.getStartingBarline() != null) {
                part.getStartingBarline()
                        .renderLine(g);
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + part,
                    ex);
        }

        return true;
    }

    //--------------//
    // visit System //
    //--------------//
    @Override
    public boolean visit (ScoreSystem system)
    {
        try {
            if (system == null) {
                return false;
            }

            return visit(system.getInfo());
        } catch (ConcurrentModificationException ignored) {
            return false;
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + system,
                    ex);

            return false;
        }
    }

    //------------------//
    // visit SystemInfo //
    //------------------//
    public boolean visit (SystemInfo systemInfo)
    {
        try {
            Rectangle bounds = systemInfo.getBounds();

            if (bounds == null) {
                return false;
            }

            // Check that this system is visible
            if (bounds.intersects(clip)) {
                g.setColor(Color.BLACK);
                // System boundary
                systemInfo.getBoundary()
                        .render(g, editableBoundaries);

                // Staff lines
                for (StaffInfo staff : systemInfo.getStaves()) {
                    staff.renderAttachments(g);
                }

                // All interpretations for this system
                SIGraph sig = systemInfo.getSig();
                g.setComposite(composite);

                for (Inter inter : sig.vertexSet()) {
                    if (clip.intersects(inter.getBounds())) {
                        inter.accept(this);
                    }
                }

                g.setComposite(fullComposite);

                // Virtual glyphs (should be in SIG?)
                paintVirtualGlyphs(systemInfo);

                return true;
            }
        } catch (ConcurrentModificationException ignored) {
            return false;
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting "
                    + systemInfo.idString(),
                    ex);
        }

        return false;
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (Inter inter)
    {
        setColor(inter);

        ShapeSymbol symbol = Symbols.getSymbol(inter.getShape());
        Glyph glyph = inter.getGlyph();
        Point center = (glyph != null) ? glyph.getCentroid()
                : GeoUtil.centerOf(inter.getBounds());
        symbol.paintSymbol(g, musicFont, center, Alignment.AREA_CENTER);
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (StemInter stem)
    {
        setColor(stem);
        g.setStroke(stemStroke);

        stem.getGlyph()
                .renderLine(g);

        g.setStroke(oldStroke);
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (LedgerInter ledger)
    {
        setColor(ledger);
        g.setStroke(ledgerStroke);

        ledger.getGlyph()
                .renderLine(g);

        g.setStroke(oldStroke);
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (BeamInter beam)
    {
        setColor(beam);
        g.fill(beam.getArea());
    }

    //--------------------//
    // paintVirtualGlyphs //
    //--------------------//
    /**
     * Paint the virtual glyphs on the sheet view
     *
     * @param systemInfo the containing system
     */
    private void paintVirtualGlyphs (SystemInfo systemInfo)
    {
        Color oldColor = g.getColor();
        g.setColor(Colors.ENTITY_VIRTUAL);

        for (Glyph glyph : systemInfo.getGlyphs()) {
            if (glyph.isVirtual()) {
                ShapeSymbol symbol = Symbols.getSymbol(glyph.getShape());

                if (symbol == null) {
                    systemInfo.getScoreSystem()
                            .addError(
                            glyph,
                            "No symbol for " + glyph.idString());
                } else {
                    symbol.paintSymbol(
                            g,
                            musicFont,
                            glyph.getAreaCenter(),
                            Alignment.AREA_CENTER);
                }
            }
        }

        g.setColor(oldColor);
    }

    //----------//
    // setColor //
    //----------//
    /**
     * Use color that depends on shape with an alpha value that
     * depends on interpretation grade.
     *
     * @param inter the interpretation to colorize
     */
    private void setColor (Inter inter)
    {
        // Shape base color
        final Color base = inter.getShape()
                .getColor();

        // Alpha value based on grade: 0..1 -> 0..255
        final int alpha = Math.min(
                255,
                Math.max(0, (int) Math.rint(255 * inter.getGrade())));
        final Color color = new Color(
                base.getRed(),
                base.getGreen(),
                base.getBlue(),
                alpha);
        g.setColor(color);
    }
}
