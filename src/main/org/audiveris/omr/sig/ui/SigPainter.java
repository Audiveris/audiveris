//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S i g P a i n t e r                                      //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractFlagInter;
import org.audiveris.omr.sig.inter.ArpeggiatoInter;
import org.audiveris.omr.sig.inter.BarConnectorInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BraceInter;
import org.audiveris.omr.sig.inter.BracketConnectorInter;
import org.audiveris.omr.sig.inter.BracketInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterVisitor;
import org.audiveris.omr.sig.inter.KeyAlterInter;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.ui.symbol.Alignment;
import static org.audiveris.omr.ui.symbol.Alignment.*;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.OmrFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.Symbols;
import static org.audiveris.omr.ui.symbol.Symbols.SYMBOL_BRACE_LOWER_HALF;
import static org.audiveris.omr.ui.symbol.Symbols.SYMBOL_BRACE_UPPER_HALF;
import static org.audiveris.omr.ui.symbol.Symbols.SYMBOL_BRACKET_LOWER_SERIF;
import static org.audiveris.omr.ui.symbol.Symbols.SYMBOL_BRACKET_UPPER_SERIF;
import org.audiveris.omr.ui.symbol.TextFont;

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
import java.util.LinkedHashSet;
import java.util.Set;

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

    /** Music font for large staves. */
    private final MusicFont musicFontLarge;

    /** Music font for small staves, if any. */
    private final MusicFont musicFontSmall;

    /** Global stroke for staff lines. */
    private final Stroke lineStroke;

    /** Global stroke for stems. */
    private final Stroke stemStroke;

    /** Global stroke for ledgers, with no glyph. */
    private final Stroke ledgerStroke;

    //~ Constructors -------------------------------------------------------------------------------
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

        // Determine proper music fonts
        if (scale == null) {
            musicFontLarge = musicFontSmall = null;
        } else {
            musicFontLarge = MusicFont.getFont(scale.getInterline());

            Integer small = scale.getSmallInterline();
            musicFontSmall = (small != null) ? MusicFont.getFont(small) : null;
        }

        // Determine lines parameters
        lineStroke = new BasicStroke(
                (scale != null) ? scale.getFore() : 2f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND);
        stemStroke = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
        ledgerStroke = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    public void process (SIGraph sig)
    {
        final int bracketGrowth = 2 * sig.getSystem().getSheet().getInterline();

        // Use a COPY of vertices, to reduce risks of concurrent modifications (but not all...)
        Set<Inter> copy = new LinkedHashSet<Inter>(sig.vertexSet());

        for (Inter inter : copy) {
            if (!inter.isDeleted()) {
                Rectangle bounds = inter.getBounds();

                if (bounds != null) {
                    // Dirty hack to make sure bracket serifs are fully painted
                    // (despite the fact that bracket serif is not included in their bounds)
                    if (inter.getShape() == Shape.BRACKET) {
                        bounds.grow(bracketGrowth, bracketGrowth);
                    }

                    if ((clip == null) || clip.intersects(bounds)) {
                        inter.accept(this);
                    }
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
        try {
            setColor(beam);
            g.fill(beam.getArea());
        } catch (Exception ex) {
            logger.warn("Error painting {} {}", beam, ex.toString());
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
                final MusicFont font = getMusicFont(flag.getStaff());
                symbol.paintSymbol(g, font, location, Alignment.MIDDLE_LEFT);
            } else {
                logger.error("No symbol to paint {}", flag);
            }
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (ArpeggiatoInter arpeggiato)
    {
        setColor(arpeggiato);

        final Rectangle bx = arpeggiato.getBounds();
        Point location = new Point(bx.x + (bx.width / 2), bx.y);
        ShapeSymbol symbol = Symbols.getSymbol(arpeggiato.getShape());
        MusicFont font = getMusicFont(arpeggiato.getStaff());
        Dimension dim = symbol.getDimension(font);

        bx.grow(dim.width, 0); // To avoid any clipping on x
        g.setClip(clip.intersection(bx));

        // Nb of symbols to draw, one below the other
        int nb = (int) Math.ceil((double) bx.height / dim.height);

        for (int i = 0; i < nb; i++) {
            symbol.paintSymbol(g, font, location, Alignment.TOP_CENTER);
            location.y += dim.height;
        }

        g.setClip(clip);
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

        final Rectangle box = brace.getBounds();
        final Point center = GeoUtil.centerOf(box);
        final Dimension halfDim = new Dimension(box.width, box.height / 2);
        final MusicFont font = getMusicFont(false);
        OmrFont.paint(g, font.layout(SYMBOL_BRACE_UPPER_HALF, halfDim), center, BOTTOM_CENTER);
        OmrFont.paint(g, font.layout(SYMBOL_BRACE_LOWER_HALF, halfDim), center, TOP_CENTER);
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
        final MusicFont font = getMusicFont(false);

        Integer top = null;

        if ((kind == BracketInter.BracketKind.TOP) || (kind == BracketInter.BracketKind.BOTH)) {
            // Draw upper symbol part
            final Point left = new Point(box.x, glyphBox.y + (int) Math.rint(barRatio * width));
            OmrFont.paint(g, font.layout(SYMBOL_BRACKET_UPPER_SERIF, dim), left, BOTTOM_LEFT);
            top = left.y;
        }

        Integer bottom = null;

        if ((kind == BracketInter.BracketKind.BOTTOM) || (kind == BracketInter.BracketKind.BOTH)) {
            // Draw lower symbol part
            final Point left = new Point(
                    box.x,
                    (glyphBox.y + glyphBox.height) - (int) Math.rint(barRatio * width));
            OmrFont.paint(g, font.layout(SYMBOL_BRACKET_LOWER_SERIF, dim), left, TOP_LEFT);
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
    public void visit (HeadInter head)
    {
        // Consider it as a plain inter
        visit((Inter) head);
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

        final ShapeSymbol symbol = Symbols.getSymbol(inter.getShape());
        setColor(inter);

        if (symbol != null) {
            final MusicFont font;

            if (inter.getShape() == Shape.NOTEHEAD_VOID) {
                font = MusicFont.getFont(
                        inter.getStaff().getSpecificInterline() + MusicFont.NOTEHEAD_VOID_EXTENT);
            } else {
                font = getMusicFont(inter.getStaff());
            }

            final Point center = GeoUtil.centerOf(inter.getBounds());
            symbol.paintSymbol(g, font, center, Alignment.AREA_CENTER);
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
        final MusicFont font = getMusicFont(inter.getStaff());
        setColor(inter);

        Point center = GeoUtil.centerOf(inter.getBounds());
        SystemInfo system = inter.getSig().getSystem();
        Staff staff = system.getClosestStaff(center);
        double y = staff.pitchToOrdinate(center.x, inter.getPitch());
        center.y = (int) Math.rint(y);

        Shape shape = inter.getShape();
        ShapeSymbol symbol = Symbols.getSymbol(shape);

        if (shape == Shape.SHARP) {
            symbol.paintSymbol(g, font, center, Alignment.AREA_CENTER);
        } else {
            Dimension dim = symbol.getDimension(font);
            center.y += dim.width;
            symbol.paintSymbol(g, font, center, Alignment.BOTTOM_CENTER);
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
        try {
            setColor(ledger);

            final Glyph glyph = ledger.getGlyph();

            if (glyph != null) {
                g.setStroke(
                        new BasicStroke(
                                (float) Math.rint(glyph.getMeanThickness(Orientation.HORIZONTAL)),
                                BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_ROUND));
                glyph.renderLine(g);
            } else {
                g.setStroke(ledgerStroke);

                final Rectangle b = ledger.getBounds();
                g.drawLine(b.x, b.y + (b.height / 2), b.x + b.width, b.y + (b.height / 2));
            }
        } catch (Exception ex) {
            logger.warn("Error painting {} {}", ledger, ex, ex);
        }
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

    //--------------//
    // getMusicFont //
    //--------------//
    /**
     * Select proper size of music font, according to related staff size.
     *
     * @param small true for small staff
     * @return selected music font
     */
    protected MusicFont getMusicFont (boolean small)
    {
        return small ? musicFontSmall : musicFontLarge;
    }

    //--------------//
    // getMusicFont //
    //--------------//
    /**
     * Select proper size of music font, according to related staff size.
     *
     * @param staff related staff
     * @return selected music font
     */
    protected MusicFont getMusicFont (Staff staff)
    {
        return getMusicFont((staff != null) ? staff.isSmall() : false);
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
