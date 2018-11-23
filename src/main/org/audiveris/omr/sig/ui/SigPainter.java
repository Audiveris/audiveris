//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S i g P a i n t e r                                      //
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
package org.audiveris.omr.sig.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractFlagInter;
import org.audiveris.omr.sig.inter.AbstractInterVisitor;
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
import org.audiveris.omr.sig.inter.KeyAlterInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
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
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
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
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Class {@code SigPainter} paints all the {@link Inter} instances of a SIG.
 * <p>
 * Its life cycle ends with the painting of a sheet.
 * <p>
 * Remarks on no-op visit() for:
 * <ul>
 * <li>AbstractChordInter: Notes and stem are painted on their own
 * <li>KeyInter: Each key item is painted on its own
 * <li>WordInter: Painting is handled from sentence
 * </ul>
 *
 * @author Hervé Bitteur
 */
public abstract class SigPainter
        extends AbstractInterVisitor
{

    private static final Logger logger = LoggerFactory.getLogger(SigPainter.class);

    /** Sequence of colors for voices. */
    private static final int alpha = 200;

    private static final Color[] voiceColors = new Color[]{
        /** 1 Purple */
        new Color(128, 64, 255, alpha),
        /** 2 Green */
        new Color(0, 255, 0, alpha),
        /** 3 Brown */
        new Color(165, 42, 42, alpha),
        /** 4 Magenta */
        new Color(255, 0, 255, alpha),
        /** 5 Cyan */
        new Color(0, 255, 255, alpha),
        /** 6 Orange */
        new Color(255, 200, 0, alpha),
        /** 7 Pink */
        new Color(255, 150, 150, alpha),
        /** 8 BlueGreen */
        new Color(0, 128, 128, alpha)};

    /** Graphic context. */
    protected final Graphics2D g;

    /** Clip rectangle. */
    private final Rectangle clip;

    /** Music font for large staves. */
    private final MusicFont musicFontLarge;

    /** Music font for small staves, if any. */
    private final MusicFont musicFontSmall;

    /** Music font for heads in large staves. */
    private final MusicFont musicHeadFontLarge;

    /** Music font for heads in small staves, if any. */
    private final MusicFont musicHeadFontSmall;

    /** Global stroke for staff lines. */
    private final Stroke lineStroke;

    /** Global stroke for stems. */
    private final Stroke stemStroke;

    /** Global stroke for ledgers, with no glyph. */
    private final Stroke ledgerStroke;

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
            musicHeadFontLarge = musicHeadFontSmall = null;
        } else {
            // Standard size
            final int large = scale.getInterline();
            musicFontLarge = MusicFont.getBaseFont(large);
            musicHeadFontLarge = MusicFont.getHeadFont(scale, large);

            // Smaller size
            final Integer small = scale.getSmallInterline();
            musicFontSmall = (small != null) ? MusicFont.getBaseFont(small) : null;
            musicHeadFontSmall = (small != null) ? MusicFont.getHeadFont(scale, small) : null;
        }

        // Determine lines parameters
        lineStroke = new BasicStroke(
                (scale != null) ? scale.getFore() : 2f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND);
        stemStroke = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
        ledgerStroke = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
    }

    //---------------//
    // getVoicePanel //
    //---------------//
    /**
     * Build a panel which displays all defined voice ID colors.
     * <p>
     * Separate numbers for first staff and second staff as: 1234 - 5678
     *
     * @return the populated voice panel
     */
    public static JPanel getVoicePanel ()
    {
        final int length = voiceColors.length;
        final Font font = new Font("SansSerif", Font.BOLD, 22);
        final Color background = Color.WHITE;
        final StringBuilder sbc = new StringBuilder();

        for (int i = 0; i <= length; i++) {
            if (i != 0) {
                sbc.append(",");
            }

            sbc.append("10dlu");
        }

        final FormLayout layout = new FormLayout(sbc.toString(), "pref");
        final Panel panel = new Panel();
        final PanelBuilder builder = new PanelBuilder(layout, panel);
        final CellConstraints cst = new CellConstraints();

        // Adjust dimensions
        final Dimension cellDim = new Dimension(5, 22);
        panel.setInsets(3, 0, 0, 3); // TLBR

        final int mid = length / 2;

        for (int c = 1; c <= length; c++) {
            final Color color = new Color(voiceColors[c - 1].getRGB()); // Remove alpha
            final JLabel label = new JLabel("" + c, JLabel.CENTER);
            label.setPreferredSize(cellDim);
            label.setFont(font);
            label.setOpaque(true);
            label.setBackground(background);
            label.setForeground(color);

            int col = (c <= mid) ? c : (c + 1);
            builder.add(label, cst.xy(col, 1));
        }
        // Separation between staves
        {
            final Color color = Color.BLACK;
            final JLabel label = new JLabel("=");
            label.setPreferredSize(cellDim);
            label.setFont(font);
            label.setOpaque(true);
            label.setBackground(background);
            label.setForeground(color);
            builder.add(label, cst.xy(mid + 1, 1));
        }

        return panel;
    }

    //---------//
    // process //
    //---------//
    public void process (SIGraph sig)
    {
        final int bracketGrowth = 2 * sig.getSystem().getSheet().getInterline();

        // Use a COPY of vertices, to reduce risks of concurrent modifications (but not all...)
        Set<Inter> copy = new LinkedHashSet<Inter>(sig.vertexSet());

        for (Inter inter : copy) {
            if (!inter.isRemoved()) {
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
            logger.warn("Error painting {} {}", beam, ex.toString(), ex);
        }
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
        if (head.getMirror() != null) {
            if (splitMirrors()) {
                // Draw head proper half
                Line2D line = head.getMidLine();
                int width = head.getBounds().width;
                int xDir = line.getY2() > line.getY1() ? -1 : +1;

                Path2D p = new Path2D.Double();
                p.append(line, false);
                p.lineTo(line.getX2() + xDir * width, line.getY2());
                p.lineTo(line.getX1() + xDir * width, line.getY1());
                p.closePath();

                java.awt.Shape oldClip = g.getClip();
                g.clip(p);
                visit((Inter) head);
                g.setClip(oldClip);
            } else {
                visit((Inter) head);
            }

            // Draw a sign using complementary color of head
            Color compColor = UIUtil.complementaryColor(g.getColor());
            Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
            g.setColor(compColor);
            g.draw(head.getMidLine());
            g.setStroke(oldStroke);
        } else {
            visit((Inter) head);
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (Inter inter)
    {
        final Shape shape = inter.getShape();

        if (shape == null) {
            return;
        }

        final ShapeSymbol symbol = Symbols.getSymbol(shape);
        setColor(inter);

        if (symbol != null) {
            final Staff staff = inter.getStaff();
            final MusicFont font;

            if (shape.isHead()) {
                font = getMusicHeadFont(staff);
            } else {
                font = getMusicFont(staff);
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

        final Glyph glyph = stem.getGlyph();

        if (glyph != null) {
            glyph.renderLine(g);
        } else {
            g.fill(stem.getBounds());
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (StaffBarlineInter inter)
    {
        List<Inter> members = inter.getMembers();

        if (!members.isEmpty()) {
            for (Inter member : members) {
                member.accept(this);
            }
        } else if (inter.getShape() != null) {
            visit((Inter) inter);
        }
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

    //----------//
    // setColor //
    //----------//
    /**
     * Use color adapted to current inter and global viewing parameters.
     *
     * @param inter the interpretation to colorize
     */
    protected abstract void setColor (Inter inter);

    //--------------//
    // splitMirrors //
    //--------------//
    /**
     * Tell whether shared heads are split.
     *
     * @return true if so
     */
    protected abstract boolean splitMirrors ();

    //---------//
    // colorOf //
    //---------//
    /**
     * Report the color to use when painting elements related to the provided voice
     *
     * @param voice the provided voice
     * @return the color to use
     */
    protected Color colorOf (Voice voice)
    {
        // Use table of colors, circularly.
        int index = (voice.getId() - 1) % voiceColors.length;

        return voiceColors[index];
    }

    //--------------//
    // getMusicFont //
    //--------------//
    /**
     * Select proper size of music font, according to provided staff size.
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
     * Select proper size of music font, according to provided staff.
     *
     * @param staff related staff
     * @return selected music font
     */
    protected MusicFont getMusicFont (Staff staff)
    {
        return getMusicFont((staff != null) ? staff.isSmall() : false);
    }

    //------------------//
    // getMusicHeadFont //
    //------------------//
    protected MusicFont getMusicHeadFont (Staff staff)
    {
        return getMusicHeadFont((staff != null) ? staff.isSmall() : false);
    }

    //------------------//
    // getMusicHeadFont //
    //------------------//
    protected MusicFont getMusicHeadFont (boolean small)
    {
        return small ? musicHeadFontSmall : musicHeadFontLarge;
    }

    //
    //    //------------------//
    //    // getMusicVoidFont //
    //    //------------------//
    //    /**
    //     * Select proper size of music font for void heads, according to provided staff.
    //     *
    //     * @param small true for small staff
    //     * @return selected music font
    //     */
    //    protected MusicFont getMusicVoidFont (boolean small)
    //    {
    //        return small ? musicVoidFontSmall : musicVoidFontLarge;
    //    }
    //
    //    //------------------//
    //    // getMusicVoidFont //
    //    //------------------//
    //    /**
    //     * Select proper size of music font for void heads, according to provided staff size.
    //     *
    //     * @param staff related staff
    //     * @return selected music font
    //     */
    //    protected MusicFont getMusicVoidFont (Staff staff)
    //    {
    //        return getMusicVoidFont((staff != null) ? staff.isSmall() : false);
    //    }
    //
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
            setColor(word);

            if (word.getValue().length() > 2) {
                paint(layout, word.getLocation(), BASELINE_LEFT);
            } else {
                paint(layout, word.getCenter(), AREA_CENTER);
            }
        }
    }
}
