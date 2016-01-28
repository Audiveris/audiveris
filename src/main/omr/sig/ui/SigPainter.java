//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S i g P a i n t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.ui;

import omr.glyph.Shape;

import omr.math.GeoUtil;

import omr.run.Orientation;

import omr.sheet.Scale;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractBeamInter;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.BarConnectorInter;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.BraceInter;
import omr.sig.inter.BracketConnectorInter;
import omr.sig.inter.BracketInter;
import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.ClefInter;
import omr.sig.inter.EndingInter;
import omr.sig.inter.Inter;
import omr.sig.inter.InterVisitor;
import omr.sig.inter.KeyAlterInter;
import omr.sig.inter.KeyInter;
import omr.sig.inter.LedgerInter;
import omr.sig.inter.SentenceInter;
import omr.sig.inter.SlurInter;
import omr.sig.inter.StemInter;
import omr.sig.inter.TimePairInter;
import omr.sig.inter.TimeWholeInter;
import omr.sig.inter.WedgeInter;
import omr.sig.inter.WordInter;
import omr.sig.relation.FlagStemRelation;
import omr.sig.relation.Relation;

import omr.text.FontInfo;

import omr.ui.symbol.Alignment;

import static omr.ui.symbol.Alignment.*;

import omr.ui.symbol.MusicFont;
import omr.ui.symbol.OmrFont;
import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.Symbols;

import static omr.ui.symbol.Symbols.SYMBOL_BRACE_LOWER_HALF;
import static omr.ui.symbol.Symbols.SYMBOL_BRACE_UPPER_HALF;
import static omr.ui.symbol.Symbols.SYMBOL_BRACKET_LOWER_SERIF;
import static omr.ui.symbol.Symbols.SYMBOL_BRACKET_UPPER_SERIF;

import omr.ui.symbol.TextFont;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.CubicCurve2D;
import java.util.HashSet;
import java.util.Set;
import omr.sig.inter.AbstractFlagInter;

/**
 * Class {@code SigPainter} paints all the {@link Inter} instances of a SIG.
 *
 * @author Hervé Bitteur
 */
public class SigPainter
        implements InterVisitor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SigPainter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Graphic context. */
    protected final Graphics2D g;

    /** Clip rectangle. */
    private final Rectangle clip;

    /** Global scale. */
    private final Scale scale;

    /** Music font properly scaled. */
    private final MusicFont musicFont;

    /** Global stroke for staff lines. */
    private final Stroke lineStroke;

    /** Global stroke for stems. */
    private final Stroke stemStroke;

    //~ Constructors -------------------------------------------------------------------------------
    //
    //    /** Global stroke for ledgers. */
    //    private Stroke ledgerStroke;
    //
    /**
     * Creates a new {@code SigPainter} object.
     *
     * @param g     Graphic context
     * @param scale the global scale
     */
    public SigPainter (Graphics g,
                       Scale scale)
    {
        this.g = (Graphics2D) g;
        this.clip = g.getClipBounds();
        this.scale = scale;

        // Determine proper font
        musicFont = (scale != null) ? MusicFont.getFont(scale.getInterline()) : null;

        // Determine staff lines parameters
        lineStroke = new BasicStroke(
                (scale != null) ? scale.getMainFore() : 2f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND);

        stemStroke = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    public void process (SIGraph sig)
    {
        // Use a COPY of vertices, to reduce risks of concurrent modifications (but not all...)
        Set<Inter> copy = new HashSet<Inter>(sig.vertexSet());

        for (Inter inter : copy) {
            if (!inter.isDeleted()) {
                Rectangle bounds = inter.getBounds();

                if ((bounds != null) && ((clip == null) || clip.intersects(bounds))) {
                    inter.accept(this);
                }
            }
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (AbstractBeamInter beam)
    {
        setColor(beam);
        g.fill(beam.getArea());
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (AbstractFlagInter flag)
    {
        setColor(flag);

        SIGraph sig = flag.getSig();
        Set<Relation> rels = sig.getRelations(flag, FlagStemRelation.class);

        if (rels.isEmpty()) {
            // The flag exists in sig, but is not yet linked to a stem, use default painting
            visit((Inter) flag);
        } else {
            // Paint the flag precisely on stem abscissa
            StemInter stem = (StemInter) sig.getOppositeInter(flag, rels.iterator().next());
            Point location = new Point(stem.getCenterLeft().x, flag.getCenter().y);
            ShapeSymbol symbol = Symbols.getSymbol(flag.getShape());

            if (symbol != null) {
                symbol.paintSymbol(g, musicFont, location, Alignment.MIDDLE_LEFT);
            } else {
                logger.error("No symbol to paint {}", flag);
            }
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (AbstractHeadInter head)
    {
        // Consider it as a plain inter
        visit((Inter) head);
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (BarConnectorInter connector)
    {
        setColor(connector);
        g.fill(connector.getArea());
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (BarlineInter barline)
    {
        setColor(barline);
        g.fill(barline.getArea());
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (BraceInter brace)
    {
        setColor(brace);

        final Rectangle box = brace.getBounds(); ///braceBox(part);
        final Point center = GeoUtil.centerOf(box);
        final Dimension halfDim = new Dimension(box.width, box.height / 2);
        OmrFont.paint(g, musicFont.layout(SYMBOL_BRACE_UPPER_HALF, halfDim), center, BOTTOM_CENTER);
        OmrFont.paint(g, musicFont.layout(SYMBOL_BRACE_LOWER_HALF, halfDim), center, TOP_CENTER);
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (BracketConnectorInter connection)
    {
        setColor(connection);
        g.fill(connection.getArea());
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (BracketInter bracket)
    {
        setColor(bracket);

        // Serif symbol
        final double widthRatio = 2.7; // Symbol width WRT bar width
        final double heightRatio = widthRatio * 1.25; // Symbol height WRT bar width
        final double barRatio = 2.3; // Symbol bar height WRT bar width

        final Rectangle box = bracket.getBounds();
        final Rectangle glyphBox = bracket.getGlyph().getBounds();
        final BracketInter.BracketKind kind = bracket.getKind();
        final double width = bracket.getWidth();
        final Dimension dim = new Dimension(
                (int) Math.rint(widthRatio * width),
                (int) Math.rint(heightRatio * width));

        Integer top = null;

        if ((kind == BracketInter.BracketKind.TOP) || (kind == BracketInter.BracketKind.BOTH)) {
            // Draw upper symbol part
            final Point left = new Point(box.x, glyphBox.y + (int) Math.rint(barRatio * width));
            OmrFont.paint(g, musicFont.layout(SYMBOL_BRACKET_UPPER_SERIF, dim), left, BOTTOM_LEFT);
            top = left.y;
        }

        Integer bottom = null;

        if ((kind == BracketInter.BracketKind.BOTTOM) || (kind == BracketInter.BracketKind.BOTH)) {
            // Draw lower symbol part
            final Point left = new Point(
                    box.x,
                    (glyphBox.y + glyphBox.height) - (int) Math.rint(barRatio * width));
            OmrFont.paint(g, musicFont.layout(SYMBOL_BRACKET_LOWER_SERIF, dim), left, TOP_LEFT);
            bottom = left.y;
        }

        // Bracket area
        Rectangle bx = null;

        if (top != null) {
            bx = bracket.getArea().getBounds();
            bx = bx.intersection(new Rectangle(bx.x, top, bx.width, bx.height));
        }

        if (bottom != null) {
            if (bx == null) {
                bx = bracket.getArea().getBounds();
            }

            bx = bx.intersection(new Rectangle(bx.x, bx.y, bx.width, bottom - bx.y));
        }

        if (bx != null) {
            g.setClip(clip.intersection(bx));
        }

        g.fill(bracket.getArea());

        if (bx != null) {
            g.setClip(clip);
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (ClefInter clef)
    {
        visit((Inter) clef);
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (EndingInter ending)
    {
        setColor(ending);
        g.setStroke(lineStroke);
        g.draw(ending.getLine());

        if (ending.getLeftLeg() != null) {
            g.draw(ending.getLeftLeg());
        }

        if (ending.getRightLeg() != null) {
            g.draw(ending.getRightLeg());
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (AbstractChordInter inter)
    {
        // Nothing: let note & stem be painted on their own
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (Inter inter)
    {
        if (inter.getShape() == null) {
            return;
        }

        setColor(inter);

        ///Glyph glyph = inter.getGlyph();
        ///Point center = (glyph != null) ? glyph.getCentroid() : GeoUtil.centerOf(inter.getBounds());
        ShapeSymbol symbol = Symbols.getSymbol(inter.getShape());

        if (symbol != null) {
            Point center = GeoUtil.centerOf(inter.getBounds());
            symbol.paintSymbol(g, musicFont, center, Alignment.AREA_CENTER);
        } else {
            logger.error("No symbol to paint {}", inter);
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (KeyAlterInter inter)
    {
        setColor(inter);

        Point center = GeoUtil.centerOf(inter.getBounds());
        SystemInfo system = inter.getSig().getSystem();
        Staff staff = system.getClosestStaff(center);
        double y = staff.pitchToOrdinate(center.x, inter.getPitch());
        center.y = (int) Math.rint(y);

        Shape shape = inter.getShape();
        ShapeSymbol symbol = Symbols.getSymbol(shape);

        if (shape == Shape.SHARP) {
            symbol.paintSymbol(g, musicFont, center, Alignment.AREA_CENTER);
        } else {
            Dimension dim = symbol.getDimension(musicFont);
            center.y += dim.width;
            symbol.paintSymbol(g, musicFont, center, Alignment.BOTTOM_CENTER);
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (KeyInter key)
    {
        // Nothing: Let each key item be painted on its own
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (LedgerInter ledger)
    {
        setColor(ledger);
        g.setStroke(
                new BasicStroke(
                        (float) Math.rint(ledger.getGlyph().getMeanThickness(Orientation.HORIZONTAL)),
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_ROUND));
        ledger.getGlyph().renderLine(g);
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (SentenceInter sentence)
    {
        setColor(sentence);

        FontInfo lineMeanFont = sentence.getMeanFont();

        for (Inter member : sentence.getMembers()) {
            WordInter word = (WordInter) member;
            paintWord(word, lineMeanFont);
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (SlurInter slur)
    {
        CubicCurve2D curve = slur.getCurve();

        if (curve != null) {
            setColor(slur);
            g.setStroke(lineStroke);
            g.draw(curve);
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (StemInter stem)
    {
        setColor(stem);

        //TODO: use proper stem thickness! (see ledger)
        g.setStroke(stemStroke);
        stem.getGlyph().renderLine(g);
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (TimePairInter pair)
    {
        for (Inter member : pair.getMembers()) {
            visit((Inter) member);
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (TimeWholeInter inter)
    {
        visit((Inter) inter);
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (WedgeInter wedge)
    {
        setColor(wedge);
        g.setStroke(lineStroke);
        g.draw(wedge.getLine1());
        g.draw(wedge.getLine2());
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (WordInter word)
    {
        // Nothing, painting is handled from sentence
    }

    //-------//
    // paint //
    //-------//
    /**
     * This is the general paint method for drawing a symbol layout, at a specified
     * location, using a specified alignment
     *
     * @param layout    what: the symbol, perhaps transformed
     * @param location  where: the precise location in the display
     * @param alignment how: the way the symbol is aligned wrt the location
     */
    protected void paint (TextLayout layout,
                          Point location,
                          Alignment alignment)
    {
        OmrFont.paint(g, layout, location, alignment);
    }

    //----------//
    // setColor //
    //----------//
    /**
     * Use color that depends on shape with an alpha value that depends on interpretation
     * grade.
     *
     * @param inter the interpretation to colorize
     */
    protected void setColor (Inter inter)
    {
        // void by default
    }

    //-----------//
    // paintWord //
    //-----------//
    private void paintWord (WordInter word,
                            FontInfo lineMeanFont)
    {
        if (lineMeanFont != null) {
            Font font = new TextFont(lineMeanFont);

            ///Font font = new TextFont(word.getFontInfo());
            FontRenderContext frc = g.getFontRenderContext();
            TextLayout layout = new TextLayout(word.getValue(), font, frc);

            if (word.getValue().length() > 2) {
                paint(layout, word.getLocation(), BASELINE_LEFT);
            } else {
                paint(layout, word.getCenter(), AREA_CENTER);
            }
        }
    }
}
