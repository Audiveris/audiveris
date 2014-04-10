//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P a g e E r a s e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.image.ShapeDescriptor;
import omr.image.Template;
import omr.image.Template.Key;

import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;
import omr.lag.SectionsBuilder;

import omr.math.GeoUtil;

import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.score.visitor.AbstractScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import static omr.sheet.SymbolsFilter.SYMBOL_ORIENTATION;
import omr.sheet.SystemInfo;

import omr.sig.AbstractBeamInter;
import omr.sig.AbstractNoteInter;
import omr.sig.BarConnectionInter;
import omr.sig.BarlineInter;
import omr.sig.BraceInter;
import omr.sig.EndingInter;
import omr.sig.Inter;
import omr.sig.InterVisitor;
import omr.sig.LedgerInter;
import omr.sig.SIGraph;
import omr.sig.SlurInter;
import omr.sig.StemInter;
import omr.sig.WedgeInter;

import omr.ui.symbol.Alignment;
import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.Symbols;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import static java.awt.BasicStroke.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.CubicCurve2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code PageEraser} erases selected shapes on the provided graphics environment.
 *
 * @author Hervé Bitteur
 */
public class PageEraser
        extends AbstractScoreVisitor
        implements InterVisitor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(PageEraser.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Page buffer. */
    private final ByteProcessor buffer;

    // Graphic context
    private final Graphics2D g;

    // Related sheet
    private final Sheet sheet;

    // Should we process (erase and save) weak shapes?
    private final boolean processWeaks;

    // Current system list of weak glyphs
    private List<Glyph> systemWeaks;

    // Specific font for music symbols
    private final MusicFont musicFont;

    // Vertical margin added above and below any staff DMZ
    private final int dmzDyMargin;

    private final float marginThickness;

    // Stroke for margins
    private final Stroke marginStroke;

    // Stroke for lines (endings, wedges, slurs)
    private final Stroke lineStroke;

    // Lag used by optional sections
    private final Lag symLag;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new PageEraser object.
     * <ul>
     * <li>All the "strong" instances of relevant shapes are always erased.</li>
     * <li>The "weak" ones can be processed depending on value of boolean 'processWeaks'.
     * If true, such glyphs are erased but saved as optional glyphs.
     * Doing so, the symbols builder will be able to try all combinations with, as well as without,
     * these optional weak glyphs.</li>
     * </ul>
     *
     * @param buffer       page buffer
     * @param g            graphics context on buffer
     * @param sheet        related sheet
     * @param processWeaks true if weak inters must be erased (and saved apart)
     */
    public PageEraser (ByteProcessor buffer,
                       Graphics2D g,
                       Sheet sheet,
                       boolean processWeaks)
    {
        this.buffer = buffer;
        this.g = g;
        this.sheet = sheet;
        this.processWeaks = processWeaks;

        symLag = sheet.getLag(Lags.SYMBOL_LAG);

        // Properly scaled font
        Scale scale = sheet.getScale();
        int symbolSize = scale.toPixels(constants.symbolSize);
        musicFont = MusicFont.getFont(symbolSize);

        dmzDyMargin = scale.toPixels(constants.staffVerticalMargin);

        marginThickness = (float) scale.toPixelsDouble(constants.lineMargin);
        marginStroke = new BasicStroke(marginThickness, CAP_SQUARE, JOIN_MITER);

        ///stemStroke = new BasicStroke(scale.getMainStem() + marginThickness, CAP_SQUARE, JOIN_MITER);
        float lineThickness = scale.getMainFore() + (2 * marginThickness);
        lineStroke = new BasicStroke(lineThickness, CAP_SQUARE, JOIN_MITER);

        g.setColor(Color.WHITE);

        // Anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // eraseGlyphs //
    //-------------//
    public void eraseGlyphs (Collection<Glyph> glyphs)
    {
        for (Glyph glyph : glyphs) {
            for (Section section : glyph.getMembers()) {
                section.render(g, false, Color.WHITE);
            }
        }
    }

    //-------------//
    // eraseShapes //
    //-------------//
    /**
     * Erase from image graphics all instances of provided shapes.
     *
     * @param shapes the shapes to process
     * @return the erased bad inter instances per system
     */
    public Map<SystemInfo, List<Glyph>> eraseShapes (Collection<Shape> shapes)
    {
        final Map<SystemInfo, List<Glyph>> weaksMap = (!processWeaks) ? null
                : new TreeMap<SystemInfo, List<Glyph>>();

        for (SystemInfo system : sheet.getSystems()) {
            final SIGraph sig = system.getSig();
            final List<Inter> strongs = new ArrayList<Inter>();
            final List<Inter> weaks = processWeaks ? new ArrayList<Inter>() : null;
            systemWeaks = null;

            for (Inter inter : sig.vertexSet()) {
                if (!inter.isDeleted() && shapes.contains(inter.getShape())) {
                    if (canHide(inter)) {
                        strongs.add(inter);
                    } else if (weaks != null) {
                        weaks.add(inter);
                    }
                }
            }

            // Simply erase the strongs
            for (Inter inter : strongs) {
                inter.accept(this);
            }

            // Erase each staff DMZ?
            if (constants.useDmz.isSet()) {
                for (StaffInfo staff : system.getStaves()) {
                    eraseDmz(staff);
                }
            }

            // Should we erase and save the weaks?
            if (weaksMap != null) {
                systemWeaks = new ArrayList<Glyph>();
                weaksMap.put(system, systemWeaks);

                for (Inter inter : weaks) {
                    inter.accept(this);
                }
            }

            systemWeaks = null;
        }

        return weaksMap;
    }

    @Override
    public void visit (Inter inter)
    {
        ShapeSymbol symbol = Symbols.getSymbol(inter.getShape());
        Glyph glyph = inter.getGlyph();
        Point center = (glyph != null) ? glyph.getCentroid()
                : GeoUtil.centerOf(inter.getBounds());
        symbol.paintSymbol(g, musicFont, center, Alignment.AREA_CENTER);
    }

    @Override
    public void visit (AbstractBeamInter beam)
    {
        g.fill(beam.getArea());
        g.setStroke(marginStroke);
        g.draw(beam.getArea());
    }

    @Override
    public void visit (StemInter stem)
    {
        processGlyph(stem.getGlyph());
    }

    @Override
    public void visit (LedgerInter ledger)
    {
        processGlyph(ledger.getGlyph());
    }

    @Override
    public void visit (SlurInter slur)
    {
        CubicCurve2D curve = slur.getInfo().getCurve();

        if (curve != null) {
            g.setStroke(lineStroke);
            g.draw(curve);
        }
    }

    @Override
    public void visit (BarlineInter inter)
    {
        g.fill(inter.getArea());
        g.setStroke(marginStroke);
        g.draw(inter.getArea());
    }

    @Override
    public void visit (BarConnectionInter inter)
    {
        g.fill(inter.getArea());
        g.setStroke(marginStroke);
        g.draw(inter.getArea());
    }

    @Override
    public void visit (WedgeInter wedge)
    {
        g.setStroke(lineStroke);
        g.draw(wedge.getLine1());
        g.draw(wedge.getLine2());
    }

    @Override
    public void visit (EndingInter ending)
    {
        g.setStroke(lineStroke);
        g.draw(ending.getLine());

        if (ending.getLeftLeg() != null) {
            g.draw(ending.getLeftLeg());
        }

        if (ending.getRightLeg() != null) {
            g.draw(ending.getRightLeg());
        }
    }

    @Override
    public void visit (BraceInter inter)
    {
        //void
    }

    @Override
    public void visit (AbstractNoteInter inter)
    {
        if (systemWeaks == null) {
            // Use plain symbol painting
            visit((Inter) inter);
        } else {
            // Use descriptor here
            final ShapeDescriptor desc = inter.getDescriptor();
            final int pitch = inter.getPitch();
            final boolean hasLine = (pitch % 2) == 0;
            final Template tpl = desc.getTemplate(new Key(inter.getShape(), hasLine));
            final Rectangle box = inter.getBounds();
            List<Point> fores = tpl.getForegroundPixels(box, buffer);

            // Erase foreground pixels
            for (Point p : fores) {
                g.fillRect(box.x + p.x, box.y + p.y, 1, 1);
            }

            // Save foreground pixels for optional glyphs
            savePixels(box, fores);
        }
    }

    //---------//
    // canHide //
    //---------//
    /**
     * Check if we can safely hide the inter (which cannot be mistaken for a symbol).
     * TODO: Quick hack, to be better implemented.
     *
     * @param inter the inter to check
     * @return true if we can safely hide the inter
     */
    private boolean canHide (Inter inter)
    {
        Double ctxGrade = inter.getContextualGrade();

        if (ctxGrade == null) {
            ctxGrade = inter.getGrade();
        }

        if (inter instanceof StemInter) {
            return ctxGrade >= 0.8;
        }

        if (inter instanceof AbstractNoteInter) {
            return ctxGrade >= 0.7;
        }

        return inter.isGood();
    }

    //----------//
    // eraseDmz //
    //----------//
    /**
     * Erase from image the DMZ part of a staff
     *
     * @param staff the staff to process
     */
    private void eraseDmz (StaffInfo staff)
    {
        int dmzEnd = staff.getDmzEnd();
        int top = staff.getFirstLine().yAt(dmzEnd) - dmzDyMargin;
        int bot = staff.getLastLine().yAt(dmzEnd) + dmzDyMargin;
        g.fillRect(0, top, dmzEnd, bot - top + 1);
    }

    //--------------//
    // processGlyph //
    //--------------//
    /**
     * Process glyph-based inter
     *
     * @param glyph the inter underlying glyph
     */
    private void processGlyph (Glyph glyph)
    {
        // Use pixels of underlying glyph
        for (Section section : glyph.getMembers()) {
            section.render(g, false, Color.WHITE);
        }

        if (systemWeaks != null) {
            // The glyph may be made of several parts, so it's safer to restart from sections
            List<Glyph> glyphs = sheet.getNest().retrieveGlyphs(
                    glyph.getMembers(),
                    GlyphLayer.SYMBOL,
                    true,
                    Glyph.Linking.NO_LINK);

            for (Glyph g : glyphs) {
                systemWeaks.add(g);
            }
        }
    }

    //------------//
    // savePixels //
    //------------//
    /**
     * Save the provided pixels as optional glyphs.
     *
     * @param box   the absolute bounding box
     * @param fores foreground pixels with coordinates relative to bounding box
     */
    private void savePixels (Rectangle box,
                             List<Point> fores)
    {
        ByteProcessor buf = new ByteProcessor(box.width, box.height);
        buf.invert();

        for (Point p : fores) {
            buf.set(p.x, p.y, 0);
        }

        // Runs
        RunsTable runTable = new RunsTableFactory(SYMBOL_ORIENTATION, buf, 0).createTable(
                "optionals");

        // Sections
        SectionsBuilder sectionsBuilder = new SectionsBuilder(symLag, new JunctionRatioPolicy());
        List<Section> sections = sectionsBuilder.createSections(runTable, false);

        // Translate sections to absolute coordinates
        for (Section section : sections) {
            section.translate(box.getLocation());
        }

        // Glyphs
        List<Glyph> glyphs = sheet.getNest().retrieveGlyphs(
                sections,
                GlyphLayer.SYMBOL,
                true,
                Glyph.Linking.NO_LINK);

        for (Glyph glyph : glyphs) {
            systemWeaks.add(glyph);
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

        final Scale.Fraction symbolSize = new Scale.Fraction(
                1.1, //1.2, // 1.0,
                "Symbols size to use for eraser");

        final Scale.Fraction lineMargin = new Scale.Fraction(0.1, "Margin drawn around lines");

        final Scale.Fraction staffVerticalMargin = new Scale.Fraction(
                2.0,
                "Margin erased above & below staff DMZ area");

        final Constant.Boolean useDmz = new Constant.Boolean(
                false,
                "Should we erase the DMZ at staff start");
    }
}
