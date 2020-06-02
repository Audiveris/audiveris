//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S i g P a i n t e r                                      //
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
package org.audiveris.omr.sig.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.curve.Curves;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sheet.ui.SheetResultPainter;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractFlagInter;
import org.audiveris.omr.sig.inter.AbstractInterVisitor;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.ArpeggiatoInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
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
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.AlterHeadRelation;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.symbol.Alignment;
import static org.audiveris.omr.ui.symbol.Alignment.*;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.OmrFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.Symbols;
import org.audiveris.omr.ui.symbol.TextFont;
import static org.audiveris.omr.ui.symbol.Symbols.SYMBOL_BRACE_LOWER_HALF;
import static org.audiveris.omr.ui.symbol.Symbols.SYMBOL_BRACE_UPPER_HALF;
import static org.audiveris.omr.ui.symbol.Symbols.SYMBOL_BRACKET_LOWER_SERIF;
import static org.audiveris.omr.ui.symbol.Symbols.SYMBOL_BRACKET_UPPER_SERIF;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.VerticalSide;

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
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Class {@code SigPainter} paints all the {@link Inter} instances of a SIG.
 * <p>
 * Its life ends with the painting of a sheet.
 * <p>
 * Ensembles are generally not painted directly but via their members:
 * <ul>
 * <li>{@link AbstractChordInter}: Notes and stem are painted on their own.
 * SigPainter subclass in {@link SheetResultPainter} adds painting of chord ID and voice.
 * <li>{@link KeyInter}: Each key item member is painted on its own, except for a manual key
 * because such key has no concrete members.
 * <li>{@link SentenceInter} and its subclass {@link LyricLineInter}: Each member word is painted
 * using the sentence mean font.
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

    /** Global stroke for curves (slur, wedge, ending). */
    private final Stroke curveStroke;

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

        // Stroke for curves (slurs, wedges and endings)
        {
            Integer fore = (scale != null) ? scale.getFore() : null;
            float width = (float) ((fore != null) ? fore : Curves.DEFAULT_THICKNESS);
            curveStroke = new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }

        // Stroke for stems
        {
            Integer stem = (scale != null) ? scale.getStemThickness() : null;
            float width = (float) ((stem != null) ? stem : StemInter.DEFAULT_THICKNESS);
            stemStroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
        }

        // Stroke for ledgers
        {
            float width = (float) LedgerInter.DEFAULT_THICKNESS;
            ledgerStroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
        }
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
        final Font font = new Font("Arial", Font.BOLD, UIUtil.adjustedSize(18));
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
            final JLabel label = new JLabel(" /");
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
        final Sheet sheet = sig.getSystem().getSheet();
        final int bracketGrowth = 2 * sheet.getInterline();

        // Use a COPY of vertices, to reduce risks of concurrent modifications (but not all...)
        Set<Inter> copy = new LinkedHashSet<>(sig.vertexSet());

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

    //----------------//
    // drawPartLimits //
    //----------------//
    /**
     * Draw the upper and lower core limits of the system.
     * <p>
     * This is just for visual inspection of these "real" limits for important musical symbols.
     *
     * @param system the system to be processed
     */
    public void drawPartLimits (SystemInfo system)
    {
        g.setColor(Colors.PART_CORE_LIMIT);

        for (Part part : system.getParts()) {
            for (VerticalSide side : VerticalSide.values()) {
                final int dy = part.getCoreMargin(side);

                if (dy != 0) {
                    final AffineTransform savedAT = g.getTransform();
                    final LineInfo line;
                    final AffineTransform at;

                    if (side == VerticalSide.TOP) {
                        line = part.getFirstStaff().getFirstLine();
                        at = AffineTransform.getTranslateInstance(0, -dy);
                    } else {
                        line = part.getLastStaff().getLastLine();
                        at = AffineTransform.getTranslateInstance(0, +dy);
                    }

                    g.transform(at);
                    line.renderLine(g, false, 0);
                    g.setTransform(savedAT);
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
        Set<Relation> rels = (sig != null) ? sig.getRelations(flag, FlagStemRelation.class)
                : Collections.emptySet();

        if (rels.isEmpty()) {
            // The flag exists in sig, but is not yet linked to a stem, use default painting
            visit((Inter) flag);
        } else {
            // Paint the flag precisely on stem abscissa
            StemInter stem = (StemInter) sig.getOppositeInter(flag, rels.iterator().next());
            Point location = new Point(stem.getCenter().x, flag.getCenter().y);
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
    public void visit (AlterInter inter)
    {
        paintHalf(inter, AlterHeadRelation.class);
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

        if (clip != null) {
            g.setClip(clip.intersection(bx));
        }

        // Nb of symbols to draw, one below the other
        int nb = (int) Math.ceil((double) bx.height / dim.height);

        for (int i = 0; i < nb; i++) {
            symbol.paintSymbol(g, font, location, Alignment.TOP_CENTER);
            location.y += dim.height;
        }

        if (clip != null) {
            g.setClip(clip);
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (AugmentationDotInter inter)
    {
        paintHalf(inter, AugmentationRelation.class);
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
    public void visit (BracketConnectorInter connector)
    {
        setColor(connector);
        g.fill(connector.getArea());
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (BracketInter bracket)
    {
        setColor(bracket);

        final MusicFont font = getMusicFont(false);
        final BracketInter.BracketKind kind = bracket.getKind();
        final Line2D median = bracket.getMedian();
        final double width = bracket.getWidth();

        // Serif symbol (its dimension is defined by ratio of trunck width)
        final double widthRatio = 2.7; // Symbol width WRT bar width
        final double heightRatio = widthRatio * 1.25; // Symbol height WRT bar width
        final Dimension serifDim = new Dimension(
                (int) Math.rint(widthRatio * width),
                (int) Math.rint(heightRatio * width));

        Integer trunkTop = null;

        if ((kind == BracketInter.BracketKind.TOP) || (kind == BracketInter.BracketKind.BOTH)) {
            // Upper symbol part
            final TextLayout topLayout = font.layout(SYMBOL_BRACKET_UPPER_SERIF, serifDim);
            final Rectangle2D topRect = topLayout.getBounds();
            final Point2D topLeft = new Point2D.Double(median.getX1() - width / 2,
                                                       median.getY1() + topRect.getY());
            final Rectangle tx = new Rectangle2D.Double(topLeft.getX(),
                                                        topLeft.getY(),
                                                        topRect.getWidth(),
                                                        median.getY1() - topLeft.getY()).getBounds();
            g.setClip(clip.intersection(tx));
            OmrFont.paint(g, topLayout, topLeft, TOP_LEFT);
            trunkTop = tx.y + tx.height;
        }

        Integer trunkBot = null;

        if ((kind == BracketInter.BracketKind.BOTTOM) || (kind == BracketInter.BracketKind.BOTH)) {
            // Lower symbol part
            final TextLayout botLayout = font.layout(SYMBOL_BRACKET_LOWER_SERIF, serifDim);
            final Rectangle2D botRect = botLayout.getBounds();
            final Point2D botLeft = new Point2D.Double(
                    median.getX2() - width / 2,
                    median.getY2() + botRect.getHeight() + botRect.getY());
            final Rectangle bx = new Rectangle2D.Double(botLeft.getX(),
                                                        median.getY2(),
                                                        botRect.getWidth(),
                                                        botLeft.getY() - median.getY2()).getBounds();
            g.setClip(clip.intersection(bx));
            OmrFont.paint(g, botLayout, botLeft, BOTTOM_LEFT);
            trunkBot = bx.y;
        }

        // Trunk area
        final Area trunk = AreaUtil.verticalParallelogram(
                new Point2D.Double(median.getX1(), (trunkTop != null) ? trunkTop : median.getY1()),
                new Point2D.Double(median.getX2(), (trunkBot != null) ? trunkBot : median.getY2()),
                width);
        g.setClip(clip);
        g.fill(trunk);
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
        g.setStroke(curveStroke);
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
        final Line2D midLine = head.getMidLine();

        if (midLine != null) {
            if (splitMirrors()) {
                // Draw head proper half
                int width = head.getBounds().width;
                int xDir = midLine.getY2() > midLine.getY1() ? -1 : +1;
                Path2D p = new Path2D.Double();
                p.append(midLine, false);
                p.lineTo(midLine.getX2() + xDir * width, midLine.getY2());
                p.lineTo(midLine.getX1() + xDir * width, midLine.getY1());
                p.closePath();

                java.awt.Shape oldClip = g.getClip();
                g.clip(p);
                visit((Inter) head);
                g.setClip(oldClip);
            } else {
                visit((Inter) head);
            }

            // Draw midLine using complementary color of head
            Color compColor = UIUtil.complementaryColor(g.getColor());
            Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
            g.setColor(compColor);
            g.draw(midLine);
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

        setColor(inter);

        final ShapeSymbol symbol = Symbols.getSymbol(shape);

        if (symbol != null) {
            final Staff staff = inter.getStaff();
            final MusicFont font;

            if (shape.isHead()) {
                font = getMusicHeadFont(staff);
            } else {
                font = getMusicFont(staff);
            }

            final Rectangle bounds = inter.getBounds();

            if (bounds != null) {
                final Point center = GeoUtil.centerOf(bounds);
                symbol.paintSymbol(g, font, center, Alignment.AREA_CENTER);
            }
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

        Staff staff = inter.getStaff();

        if (staff == null) {
            SystemInfo system = inter.getSig().getSystem();
            staff = system.getClosestStaff(center);
        }

        double y = staff.pitchToOrdinate(center.x, inter.getPitch());

        center.y = (int) Math.rint(y);

        Shape shape = inter.getShape();

        ShapeSymbol symbol = Symbols.getSymbol(shape);

        if (shape
                    == Shape.SHARP) {
            symbol.paintSymbol(g, font, center, Alignment.AREA_CENTER);
        } else {
            Dimension dim = symbol.getDimension(font);
            center.y += dim.width; // Roughly...
            symbol.paintSymbol(g, font, center, Alignment.BOTTOM_CENTER);
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (KeyInter key)
    {
        // Normally, key ensemble is painted via its alter members
        // But for a manual key, there are no members available, so we paint the symbol
        if (key.isManual()) {
            if (key.isCancel()) {
                setColor(key);

                ShapeSymbol symbol = key.getSymbolToDraw();

                if (symbol == null) {
                    symbol = Symbols.getSymbol(Shape.NON_DRAGGABLE);
                }

                final Staff staff = key.getStaff();
                final MusicFont font = getMusicFont(staff);
                final Rectangle bounds = key.getBounds();

                if (bounds != null) {
                    final Point center = GeoUtil.centerOf(bounds);
                    symbol.paintSymbol(g, font, center, Alignment.AREA_CENTER);
                }
            } else {
                visit((Inter) key);
            }
        }
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (LedgerInter ledger)
    {
        setColor(ledger);

        final double thickness = ledger.getThickness();

        if (thickness != 0) {
            g.setStroke(
                    new BasicStroke(
                            (float) Math.rint(thickness),
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_ROUND));
        } else {
            g.setStroke(ledgerStroke); // Should not occur
        }

        g.draw(ledger.getMedian());
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (SentenceInter sentence)
    {
        ///FontInfo lineMeanFont = sentence.getMeanFont();

        for (Inter member : sentence.getMembers()) {
            WordInter word = (WordInter) member;
            ///paintWord(word, lineMeanFont);
            paintWord(word, word.getFontInfo());
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
            g.setStroke(curveStroke);
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

        g.setStroke(stemStroke);

        g.draw(stem.getMedian());
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (StaffBarlineInter inter)
    {
        List<Inter> members = inter.getMembers(); // Needs sig, thus it can't be used for ghost.

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
        for (Inter member : pair.getMembers()) { // Needs sig, thus it can't be used for ghost.
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
        g.setStroke(curveStroke);
        g.draw(wedge.getLine1());
        g.draw(wedge.getLine2());
    }

    //-------//
    // visit //
    //-------//
    @Override
    public void visit (WordInter word)
    {
        // Usually, words are displayed via their containing sentence, using sentence mean font.
        // But in the specific case of a (temporarily) orphan word, we display the word as it is.
        if (word.getSig() == null || word.getEnsemble() == null) {
            FontInfo fontInfo = word.getFontInfo();
            paintWord(word, fontInfo);
        }
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
                          Point2D location,
                          Alignment alignment)
    {
        OmrFont.paint(g, layout, location, alignment);
    }

    //-----------//
    // paintHalf //
    //-----------//
    /**
     * Paint upper and lower parts of a symbol, if it is linked to two shared heads.
     * Otherwise, paint it normally as a whole.
     *
     * @param inter  the inter to paint
     *               (AugmentationDotInter or AlterInter)
     * @param classe the relation class to search between inter and head
     *               (AugmentationRelation or AlterHeadRelation)
     */
    private void paintHalf (Inter inter,
                            Class<? extends Relation> classe)
    {
        final SIGraph sig = inter.getSig();

        if (!splitMirrors() || (sig == null)) {
            visit(inter);

            return;
        }

        final List<HeadInter> heads = new ArrayList<>();

        for (Relation rel : sig.getRelations(inter, classe)) {
            Inter opposite = sig.getOppositeInter(inter, rel);

            if (opposite instanceof HeadInter) {
                heads.add((HeadInter) opposite);
            }
        }

        if ((heads.size() != 2) || (heads.get(0).getMirror() != heads.get(1))) {
            // Standard case where symbol is painted as a whole
            visit(inter);
        } else {
            // Split according to linked shared heads
            final Rectangle box = inter.getBounds();
            final int height = box.height;
            final Point center = inter.getCenter();
            final Shape shape = inter.getShape();
            final Staff staff = inter.getStaff();
            final ShapeSymbol symbol = Symbols.getSymbol(shape);
            final MusicFont font = getMusicFont(staff);
            final Dimension dim = symbol.getDimension(font);
            final int w = dim.width;
            final Point2D ref = inter.getRelationCenter(); // Not always the area center
            final Line2D line = new Line2D.Double(ref.getX() - w, ref.getY(),
                                                  ref.getX() + w, ref.getY());

            // Draw each inter half
            for (HeadInter h : heads) {
                final AbstractChordInter ch = h.getChord();
                final int yDir = ch.getCenter().y > h.getCenter().y ? +1 : -1;
                final Path2D p = new Path2D.Double();
                p.append(line, false);
                p.lineTo(line.getX2(), line.getY2() + yDir * height);
                p.lineTo(line.getX1(), line.getY1() + yDir * height);
                p.closePath();

                final java.awt.Shape oldClip = g.getClip();
                g.clip(p);

                setColor(ch);
                symbol.paintSymbol(g, font, center, Alignment.AREA_CENTER);

                g.setClip(oldClip);
            }
        }
    }

    //-----------//
    // paintWord //
    //-----------//
    protected void paintWord (WordInter word,
                              FontInfo lineMeanFont)
    {
        if (word.getValue().trim().isEmpty()) {
            return;
        }

        if (lineMeanFont == null) {
            logger.warn("No font information for {}", word);
            return;
        }

        Font font = new TextFont(lineMeanFont);
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout layout = new TextLayout(word.getValue(), font, frc);
        setColor(word);

        paint(layout, word.getLocation(), BASELINE_LEFT);
    }
}
