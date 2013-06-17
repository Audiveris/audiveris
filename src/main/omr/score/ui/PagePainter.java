//----------------------------------------------------------------------------//
//                                                                            //
//                           P a g e P a i n t e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.facets.Glyph;

import omr.math.GeoUtil;

import omr.score.Score;
import omr.score.entity.AbstractNotation;
import omr.score.entity.Arpeggiate;
import omr.score.entity.Articulation;
import omr.score.entity.Beam;
import omr.score.entity.Chord;
import omr.score.entity.Clef;
import omr.score.entity.Coda;
import omr.score.entity.Dynamics;
import omr.score.entity.Fermata;
import omr.score.entity.KeySignature;
import omr.score.entity.LyricsItem;
import omr.score.entity.MeasureElement;
import omr.score.entity.Note;
import omr.score.entity.Ornament;
import omr.score.entity.Pedal;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Segno;
import omr.score.entity.Slur;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.entity.Text;
import omr.score.entity.TimeSignature;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.entity.Tuplet;
import omr.score.entity.Voice;
import omr.score.entity.Wedge;
import omr.score.visitor.AbstractScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.text.FontInfo;
import omr.text.TextLine;
import omr.text.TextWord;

import omr.ui.symbol.Alignment;
import static omr.ui.symbol.Alignment.*;
import static omr.ui.symbol.Alignment.Horizontal.*;
import static omr.ui.symbol.Alignment.Vertical.*;
import omr.ui.symbol.MusicFont;
import omr.ui.symbol.OmrFont;
import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.Symbols;
import static omr.ui.symbol.Symbols.*;
import omr.ui.symbol.TextFont;
import omr.ui.util.UIUtil;

import omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class {@code PagePainter} is an abstract class that defines common
 * features of a page painter.
 * <p>It is specialized by: <ul>
 * <li>{@link PagePhysicalPainter} for the presentation of page entities over
 * the sheet glyphs</li>
 * <li>We used to also have a PageLogicalPainter for the "ideal" score view</li>
 *
 * @author Hervé Bitteur
 */
public abstract class PagePainter
        extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(PagePainter.class);

    /** The alignment used by default */
    protected static final Alignment defaultAlignment = AREA_CENTER;

    /** A transformation to half scale (used for slot time annotation) */
    protected static final AffineTransform halfAT = AffineTransform.
            getScaleInstance(
            0.5,
            0.5);

    /** Font for annotations */
    protected static final Font basicFont = new Font(
            "Sans Serif",
            Font.PLAIN,
            constants.basicFontSize.getValue());

    /** Abscissa offset, in pixels, for annotation near system */
    protected static final int annotationDx = 15;

    /** Ordinate offset, in pixels, for annotation near staff or system */
    protected static final int annotationDy = 15;

    // Painting parameters
    protected static final PaintingParameters parameters = PaintingParameters.
            getInstance();

    /** Sequence of colors for voices. TODO: Choose better colors, with alpha! */
    private static final int alpha = 150;

    private static final Color[] voiceColors = new Color[]{
        /** Cyan */
        new Color(0, 255, 255, alpha),
        /** Orange */
        new Color(255, 200, 0, alpha),
        /** Pink */
        new Color(255, 175, 175, alpha),
        /** Green */
        new Color(0, 255, 0, alpha),
        /** Magenta */
        new Color(255, 0, 255, alpha),
        /** Blue */
        new Color(0, 0, 255, alpha),
        /** Yellow */
        new Color(255, 255, 0, alpha)
    };

    //~ Instance fields --------------------------------------------------------
    /** Clipping area */
    protected final Rectangle oldClip;

    /** Flag for painting staff lines */
    protected final boolean linePainting;

    // Graphic context
    protected final Graphics2D g;

    // Global color
    protected final Color defaultColor;

    // Painting voices with different colors?
    protected final boolean coloredVoices;

    // Should we draw annotations?
    protected final boolean annotated;

    // Related score
    protected Score score;

    // Specific font for music symbols
    protected MusicFont musicFont;

    // Global scale
    protected Scale scale;

    // For staff lines
    protected int lineThickness;

    protected Stroke lineStroke;

    // For stems
    protected float stemThickness;

    protected float stemHalfThickness;

    protected Stroke stemStroke;

    // For beams
    protected float beamThickness;

    protected float beamHalfThickness;

    // The system being currently painted
    protected ScoreSystem system;

    protected SystemInfo systemInfo;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // PagePainter //
    //--------------//
    /**
     * Creates a new PagePainter object.
     *
     * @param graphics      Graphic context
     * @param color         the default color
     * @param coloredVoices true for voices with different colors
     * @param linePainting  true for painting staff lines
     * @param annotated     true if annotations are to be drawn
     */
    public PagePainter (Graphics graphics,
                        Color color,
                        boolean coloredVoices,
                        boolean linePainting,
                        boolean annotated)
    {
        g = (Graphics2D) graphics.create();

        oldClip = g.getClipBounds();

        this.defaultColor = color;
        this.coloredVoices = coloredVoices;
        this.linePainting = linePainting;
        this.annotated = annotated;

        // Use a specific color for all score entities
        g.setColor(color);

        // Anti-aliasing
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Default font for annotations
        g.setFont(basicFont);
    }

    //~ Methods ----------------------------------------------------------------
    //------------------//
    // visit Arpeggiate //
    //------------------//
    @Override
    public boolean visit (Arpeggiate arpeggiate)
    {
        try {
            // Voice color?
            handleVoiceColor(arpeggiate);

            // Draw an arpeggiate symbol with proper height
            // Using half-height of arpeggiate character as the elementary unit
            // We need clipping to draw half characters
            final Rectangle box = arpeggiate.getBox();
            box.height -= 2; // Gives better results

            // How many half arpeggiate symbols do we need?
            final int halfHeight = scale.getInterline();
            final int count = (int) Math.rint(
                    (double) box.height / halfHeight);
            final TextLayout layout = musicFont.layout(ARPEGGIATO);
            final Point start = new Point(box.x, box.y + box.height);

            // Draw count * half symbols, bottom up
            for (int i = 0; i < count; i++) {
                // Define a clipping area
                final Rectangle area = new Rectangle(
                        start.x,
                        start.y - halfHeight,
                        box.width,
                        halfHeight);
                area.grow(6, 6); // Add some margin to avoid gaps

                final Rectangle clip = oldClip.intersection(area);

                // Anything to draw in the clipping area?
                if ((clip.width > 0) && (clip.height > 0)) {
                    g.setClip(clip);
                    layout.draw(g, start.x, start.y);
                }

                // Move up half height
                start.y -= halfHeight;
            }

            // Restore oldClip
            g.setClip(oldClip);
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + arpeggiate,
                    ex);
        }

        return true;
    }

    //--------------------//
    // visit Articulation //
    //--------------------//
    @Override
    public boolean visit (Articulation articulation)
    {
        return visit((MeasureElement) articulation);
    }

    //------------//
    // visit Beam //
    //------------//
    @Override
    public boolean visit (Beam beam)
    {
        try {
            final Point left = new Point(
                    beam.getPoint(HorizontalSide.LEFT));
            final Point right = new Point(
                    beam.getPoint(HorizontalSide.RIGHT));
            final int dx = (int) Math.rint(stemHalfThickness);
            final int dy = (int) Math.rint(beamHalfThickness);

            // Compute precise abscissae values
            if (beam.isHook()) {
                // Just a hook stuck to a stem on one side
                if (!beam.getChords().isEmpty()) {
                    Chord chord = beam.getChords().get(0);

                    if (chord.getCenter().x < beam.getCenter().x) {
                        left.x -= dx;
                    } else {
                        right.x += dx;
                    }
                } else {
                    //                beam.addError(
                    //                    beam.getGlyphs().iterator().next(),
                    //                    "Beam hook with no related chord");
                    return false;
                }
            } else {
                // Standard beam stuck to 2 stems, one on either side
                left.x -= dx;
                right.x += dx;
            }

            // Use a filled polygon to paint the beam
            final Polygon polygon = new Polygon();
            polygon.addPoint(left.x, left.y - dy);
            polygon.addPoint(left.x, left.y + dy);
            polygon.addPoint(right.x, right.y + dy);
            polygon.addPoint(right.x, right.y - dy);

            // Use related voices (if already set)
            Set<Voice> voices = new LinkedHashSet<>();
            for (Chord chord : beam.getChords()) {
                Voice voice = chord.getVoice();
                if (voice != null) {
                    voices.add(voice);
                }
            }

            if (!voices.isEmpty()) {
                // Paint all colors, one on top of the other
                for (Voice voice : voices) {
                    g.setColor(colorOf(voice));
                    g.fill(polygon);
                }
            } else {
                // Paint with default color
                g.setColor(defaultColor);
                g.fill(polygon);
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + beam,
                    ex);
        }

        return true;
    }

    //-------------//
    // visit Chord //
    //-------------//
    @Override
    public boolean visit (Chord chord)
    {
        try {
            // Voice indication ?
            handleVoiceColor(chord);

            // Flags ?
            final int fn = chord.getFlagsNumber();

            if (fn > 0) {
                Point tail = chord.getTailLocation();
                Point head = chord.getHeadLocation();

                // We draw from tail
                boolean goesUp = head.y < tail.y;
                paint(
                        Chord.getFlagShape(fn, goesUp),
                        location(tail, chord),
                        goesUp ? BOTTOM_LEFT : TOP_LEFT);
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + chord,
                    ex);
        }

        return true;
    }

    //------------//
    // visit Clef //
    //------------//
    @Override
    public boolean visit (Clef clef)
    {
        try {
            paint(clef.getShape(), clef.getReferencePoint());
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + clef,
                    ex);
        }

        return true;
    }

    //------------//
    // visit Coda //
    //------------//
    @Override
    public boolean visit (Coda coda)
    {
        return visit((MeasureElement) coda);
    }

    //----------------//
    // visit Dynamics //
    //----------------//
    @Override
    public boolean visit (Dynamics dynamics)
    {
        return visit((MeasureElement) dynamics);
    }

    //---------------//
    // visit Fermata //
    //---------------//
    @Override
    public boolean visit (Fermata fermata)
    {
        return visit((MeasureElement) fermata);
    }

    //--------------------//
    // visit KeySignature //
    //--------------------//
    @Override
    public boolean visit (KeySignature keySignature)
    {
        try {
            final Staff staff = keySignature.getStaff();
            final Shape clefKind = keySignature.getClefKind();
            final int key = keySignature.getKey();
            final int sign = Integer.signum(key);
            final Shape shape = (key < 0) ? FLAT : SHARP;
            final TextLayout layout = musicFont.layout(shape);
            final Rectangle box = keySignature.getBox();
            final int unitDx = getKeySigItemDx();

            if (box == null) {
                ///logger.warn("Null box for " + keySignature);
                ///keySignature.addError("Null box for " + keySignature);
                return false;
            }

            // Flats : use vertical stick on left
            // Sharps : use center of the two vertical sticks
            final Alignment alignment = new Alignment(
                    BASELINE,
                    (key < 0) ? LEFT : CENTER);
            Point point = new Point(box.x, 0);

            for (int i = 1; i <= (key * sign); i++) {
                int n = i * sign;
                double pitch = KeySignature.getItemPosition(n, clefKind);
                Integer ref = keySignature.getItemPixelAbscissa(n);

                if (ref != null) {
                    ///logger.info(n + ":" + ref + " for " + keySignature);
                    point = new Point(ref, 0);
                }

                paint(layout, location(point, staff, pitch), alignment);
                point.x += unitDx; // Fall-back if ref is not known
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + keySignature,
                    ex);
        }

        return true;
    }

    //----------------------//
    // visit MeasureElement //
    //----------------------//
    @Override
    public boolean visit (MeasureElement measureElement)
    {
        handleVoiceColor(measureElement);

        try {
            if (measureElement.getShape() != null) {
                try {
                    paint(
                            musicFont.layout(
                            measureElement.getShape(),
                            measureElement.getDimension()),
                            measureElement.getReferencePoint());
                } catch (ConcurrentModificationException ignored) {
                } catch (Exception ex) {
                    logger.warn("Cannot paint " + measureElement, ex);
                }
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting "
                    + measureElement,
                    ex);
        }

        return true;
    }

    //------------//
    // visit Note //
    //------------//
    @Override
    public boolean visit (Note note)
    {
        try {
            final Chord chord = note.getChord();
            final Glyph stem = chord.getStem();
            final Shape shape = note.getShape();
            final Point center = note.getCenter();

            // Note head
            if (stem != null) {
                // Note is attached to a stem, link note display to the stem
                paint(shape,
                        noteLocation(note),
                        (center.x < chord.getTailLocation().x) ? MIDDLE_RIGHT
                        : MIDDLE_LEFT);
            } else {
                // Standard display
                paint(shape.getPhysicalShape(), noteLocation(note));
            }

            // Accidental ?
            final Glyph accid = note.getAccidental();

            if (accid != null) {
                paint(accid.getShape(),
                        accidentalLocation(note, accid),
                        BASELINE_CENTER);
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + note,
                    ex);
        }

        return true;
    }

    //----------------//
    // visit Ornament //
    //----------------//
    @Override
    public boolean visit (Ornament ornament)
    {
        return visit((MeasureElement) ornament);
    }

    //-------------//
    // visit Pedal //
    //-------------//
    @Override
    public boolean visit (Pedal pedal)
    {
        return visit((MeasureElement) pedal);
    }

    //-------------//
    // visit Segno //
    //-------------//
    @Override
    public boolean visit (Segno segno)
    {
        return visit((MeasureElement) segno);
    }

    //------------//
    // visit Slur //
    //------------//
    @Override
    public boolean visit (Slur slur)
    {
        if (coloredVoices) {
            g.setColor(defaultColor);

            Voice voice = null;

            if (slur.isTie()) {
                Note note = slur.getLeftNote();

                if (note != null) {
                    Chord chord = note.getChord();
                    voice = chord.getVoice();
                }

                note = slur.getRightNote();

                if (note != null) {
                    Chord chord = note.getChord();

                    if (voice == null) {
                        voice = chord.getVoice();
                    } else if ((chord.getVoice() != null)
                               && (chord.getVoice() != voice)) {
                        ///slur.addError("Tie with different voices");
                    }
                }

                if (voice != null) {
                    g.setColor(colorOf(voice));
                }
            }
        }

        try {
            Stroke oldStroke = g.getStroke();
            g.setStroke(lineStroke);
            g.draw(slur.getCurve());
            g.setStroke(oldStroke);
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + slur,
                    ex);
        }

        return true;
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart part)
    {
        g.setColor(defaultColor);

        try {
            // We don't draw dummy parts?
            if (part.isDummy()) {
                return false;
            }

            // Draw a brace/bracket?
            if (part.getBrace() != null) {
                if (part.getBrace().getShape() == Shape.BRACE) {
                    // We have nice half braces in MusicalSymbols font
                    final Rectangle box = braceBox(part);
                    final Point center = GeoUtil.centerOf(box);
                    final Dimension halfDim = new Dimension(
                            box.width,
                            box.height / 2);
                    paint(
                            musicFont.layout(SYMBOL_BRACE_UPPER_HALF, halfDim),
                            center,
                            BOTTOM_CENTER);
                    paint(
                            musicFont.layout(SYMBOL_BRACE_LOWER_HALF, halfDim),
                            center,
                            TOP_CENTER);
                } else if (part.getBrace().getShape() == Shape.BRACKET) {
                    TextLayout trunk = musicFont.layout(Shape.THICK_BARLINE);
                    double width = trunk.getBounds().getWidth();
                    double barWidth = 0.5 * part.getScale().getInterline();
                    AffineTransform at = AffineTransform.getScaleInstance(barWidth / width, 1);

                    final Line2D line = bracketLine(part);
                    Point topLeft = new Point(
                            (int) Math.rint(line.getX1() - barWidth / 2),
                            (int) Math.rint(line.getY1()));
                    Point botLeft = new Point(
                            (int) Math.rint(line.getX2() - barWidth / 2),
                            (int) Math.rint(line.getY2()));

                    TextLayout upper = musicFont.layout(SYMBOL_BRACKET_UPPER_SERIF.getString(), at);
                    TextLayout lower = musicFont.layout(SYMBOL_BRACKET_LOWER_SERIF.getString(), at);
                    paint(upper, topLeft, BASELINE_LEFT);
                    paint(lower, botLeft, BASELINE_LEFT);

                    BarPainter barPainter = BarPainter.getBarPainter(Shape.THICK_BARLINE);
                    barPainter.draw(g, line.getP1(), line.getP2(), part);
                }
            }

            // Render the part starting barline, if any
            if (part.getStartingBarline() != null) {
                part.getStartingBarline().accept(this);
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
    // visit Tuplet //
    //--------------//
    @Override
    public boolean visit (Tuplet tuplet)
    {
        return visit((MeasureElement) tuplet);
    }

    //------------//
    // visit Text //
    //------------//
    @Override
    public boolean visit (Text text)
    {
        try {
            g.setColor(defaultColor);
            TextLine sentence = text.getSentence();
            if (sentence.getWords().isEmpty()) {
                // This may occur with obsolete Text, linked to old sentence
                // When painting between TEXTS and PAGES steps
                return false;
            }

            if (text instanceof LyricsItem) {
                // Just print the single LyricsItem word (and not the full line)
                paintWord(((LyricsItem) text).getWord());
            } else {
                // Print the whole line
                for (TextWord word : sentence.getWords()) {
                    paintWord(word);
                }
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + text,
                    ex);
        }

        return true;
    }

    //------------------//
    // handleVoiceColor //
    //------------------//
    private void handleVoiceColor (MeasureElement measureElement)
    {
        if (coloredVoices) {
            g.setColor(defaultColor);

            if (measureElement instanceof AbstractNotation) {
                handleVoiceColor(measureElement.getChord());
            }
        }

    }

    //------------------//
    // handleVoiceColor //
    //------------------//
    private void handleVoiceColor (Chord chord)
    {
        if (coloredVoices) {
            g.setColor(defaultColor);

            if (chord != null) {
                Voice voice = chord.getVoice();

                if (voice != null) {
                    g.setColor(colorOf(voice));
                }
            }
        }
    }
    //-----------//
    // paintWord //
    //-----------//

    private void paintWord (TextWord word)
    {
        FontInfo meanFont = word.getTextLine().getMeanFont();

        if (meanFont != null) {
            Font font = new TextFont(meanFont);
            FontRenderContext frc = g.getFontRenderContext();
            TextLayout layout = new TextLayout(word.getValue(), font, frc);
            paint(layout, word.getLocation(), BASELINE_LEFT);
        }
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    @Override
    public boolean visit (TimeSignature timeSignature)
    {
        g.setColor(defaultColor);

        try {
            final Shape shape = timeSignature.getShape();
            final Point center = timeSignature.getCenter();
            final Staff staff = timeSignature.getStaff();
            final int dy = staff.pitchToPixels(-2);

            if (shape == Shape.NO_LEGAL_TIME) {
                // If this is an illegal shape, do not draw anything.
                // TODO: we could draw a special sign for this
                return false;
            }

            // Special symbol?
            if ((shape == COMMON_TIME) || (shape == CUT_TIME)) {
                paint(shape, center);
            } else {
                // Paint numerator
                paintTimeNumber(
                        timeSignature.getNumerator(),
                        new Point(center.x, center.y - dy));

                // Paint denominator
                paintTimeNumber(
                        timeSignature.getDenominator(),
                        new Point(center.x, center.y + dy));
            }
        } catch (InvalidTimeSignature ex) {
            logger.warn("Invalid time signature", ex);
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            //            timeSignature.addError(
            //                timeSignature.getGlyphs().iterator().next(),
            //                "Error painting timeSignature " + ex);
        }

        return true;
    }

    //-------------//
    // visit Wedge //
    //-------------//
    @Override
    public boolean visit (Wedge wedge)
    {
        g.setColor(defaultColor);

        try {
            if (wedge.isStart()) {
                final Rectangle box = wedge.getGlyph().getBounds();

                Point single;
                Point top;
                Point bot;

                if (wedge.getShape() == Shape.CRESCENDO) {
                    single = new Point(box.x, box.y + (box.height / 2));
                    top = new Point(box.x + box.width, box.y);
                    bot = new Point(box.x + box.width, box.y + box.height);
                } else {
                    single = new Point(
                            box.x + box.width,
                            box.y + (box.height / 2));
                    top = new Point(box.x, box.y);
                    bot = new Point(box.x, box.y + box.height);
                }

                paintLine(single, top);
                paintLine(single, bot);
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + wedge,
                    ex);
        }

        return true;
    }

    //--------------------//
    // accidentalLocation //
    //--------------------//
    /**
     * Report the precise location to be used for a given accidental sigh
     * (with respect to the related note)
     *
     * @param note       the related note
     * @param accidental the accidental glyph
     * @return the location to be used for painting
     */
    protected abstract Point accidentalLocation (Note note,
                                                 Glyph accidental);

    //----------//
    // braceBox //
    //----------//
    /**
     * Report the precise box to be used for a given brace
     *
     * @param part the related part
     * @return the box to be used for painting
     */
    protected abstract Rectangle braceBox (SystemPart part);

    //-------------//
    // bracketLine //
    //-------------//
    /**
     * Report the vertical driving line (barline) to be used for a
     * given bracket
     *
     * @param part the related part
     * @return the line to drive the painting
     */
    protected abstract Line2D bracketLine (SystemPart part);

    //--------------//
    // noteLocation //
    //--------------//
    /**
     * Report the precise location to be used for a given note
     *
     * @param note the related note
     * @return the location to be used for painting
     */
    protected abstract Point noteLocation (Note note);

    //-------------//
    // basicLayout //
    //-------------//
    /**
     * Build a TextLayout from a String of BasicFont characters
     * (transformed by the provided AffineTransform if any)
     *
     * @param str the string of proper codes
     * @param fat potential affine transformation
     * @return the (sized) TextLayout ready to be drawn
     */
    protected TextLayout basicLayout (String str,
                                      AffineTransform fat)
    {
        FontRenderContext frc = g.getFontRenderContext();
        Font font = (fat == null) ? basicFont
                : basicFont.deriveFont(fat);

        return new TextLayout(str, font, frc);
    }

    //-----------------//
    // getKeySigItemDx //
    //-----------------//
    /**
     * Report the theoretical abscissa gap between items of one key signature
     * (the individual sharp or flat signs)
     *
     * @return the theoretical dx between items
     */
    protected int getKeySigItemDx ()
    {
        return scale.toPixels(constants.keySigItemDx);
    }

    //----------------//
    // initParameters //
    //----------------//
    /**
     * Initialization sequence common to ScorePhysicalPainter and
     * ScoreLogicalPainter
     */
    protected void initParameters ()
    {
        // Determine staff lines parameters
        lineThickness = scale.getMainFore();
        lineStroke = new BasicStroke(
                lineThickness,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND);

        // Determine stems parameters
        stemThickness = scale.getMainFore();
        stemHalfThickness = stemThickness / 2;
        stemStroke = new BasicStroke(
                stemThickness,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND);

        // Set stroke for lines
        if (linePainting) {
            g.setStroke(lineStroke);
        } else {
            UIUtil.setAbsoluteStroke(g, 1f);
        }
    }

    //----------//
    // location //
    //----------//
    /**
     * Build the desired absolute drawing point, the abscissa being adjusted to
     * fit on the provided chord stem, and the ordinate being computed from the
     * pitch position with respect to the containing staff
     *
     * @param sysPoint the (approximate) system-based drawing point
     * @param chord    the chord whose stem must be stuck to the (note) symbol
     * @param staff    the containing staff
     * @param pitch    the pitch position with respect to the staff
     * @return the Point, as precise as possible in X & Y
     */
    protected Point location (Point sysPoint,
                              Chord chord,
                              Staff staff,
                              double pitch)
    {
        return new Point(
                preciseAbscissa(sysPoint, chord),
                staff.getTopLeft().y + staff.pitchToPixels(pitch));
    }

    //----------//
    // location //
    //----------//
    /**
     * Build the desired absolute drawing point, the abscissa being adjusted to
     * fit on the provided chord stem
     *
     * @param sysPoint the (approximate) system-based drawing point
     * @param chord    the chord whose stem must be stuck to the (note) symbol
     * @return the Point, as precise as possible in X
     */
    protected Point location (Point sysPoint,
                              Chord chord)
    {
        return new Point(preciseAbscissa(sysPoint, chord), sysPoint.y);
    }

    //----------//
    // location //
    //----------//
    /**
     * Build the desired absolute drawing point, the ordinate being computed
     * from the pitch position with respect to the containing staff
     *
     * @param sysPoint the (approximate) system-based drawing point
     * @param staff    the containing staff
     * @param pitch    the pitch position with respect to the staff
     * @return the Point, as precise as possible in Y
     */
    protected Point location (Point sysPoint,
                              Staff staff,
                              double pitch)
    {
        return new Point(
                sysPoint.x,
                staff.getTopLeft().y + staff.pitchToPixels(pitch));
    }

    //-------//
    // paint //
    //-------//
    /**
     * This is the general paint method for drawing a symbol layout, at a
     * specified location, using a specified alignment
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

    //-------//
    // paint //
    //-------//
    /**
     * A convenient painting method, using default alignment
     * (CENTER + MIDDLE)
     *
     * @param layout   what: the symbol, perhaps transformed
     * @param location where: the precise location in the display
     */
    protected void paint (TextLayout layout,
                          Point location)
    {
        if (layout != null) {
            paint(layout, location, defaultAlignment);
        } else {
            logger.debug("null layout");
        }
    }

    //-------//
    // paint //
    //-------//
    /**
     * Paint a symbol
     *
     * @param shape     the symbol shape
     * @param location  the precise location
     * @param alignment alignment wrt the symbol
     */
    protected void paint (Shape shape,
                          Point location,
                          Alignment alignment)
    {
        ShapeSymbol symbol = Symbols.getSymbol(shape);

        if (symbol != null) {
            symbol.paintSymbol(g, musicFont, location, alignment);
        }
    }

    //-------//
    // paint //
    //-------//
    /**
     * Paint a symbol with default alignment
     *
     * @param shape    the symbol shape
     * @param location the precise location
     */
    protected void paint (Shape shape,
                          Point location)
    {
        paint(shape, location, defaultAlignment);
    }

    //-----------//
    // paintLine //
    //-----------//
    /**
     * Draw a line from one Point to another Point
     *
     * @param from first point
     * @param to   second point
     */
    protected void paintLine (Point from,
                              Point to)
    {
        if ((from != null) && (to != null)) {
            g.drawLine(from.x, from.y, to.x, to.y);
        } else {
            logger.warn("line not painted due to null reference");
        }
    }

    //-----------------//
    // paintTimeNumber //
    //-----------------//
    /**
     * Paint a (time) number using the coordinates in units of its center point
     * within the containing system part
     *
     * @param number the number whose icon must be painted
     * @param center the center of desired location
     */
    protected void paintTimeNumber (int number,
                                    Point center)
    {
        int[] codes = ShapeSymbol.numberCodes(number);
        String str = new String(codes, 0, codes.length);
        MusicFont.paint(g, musicFont.layout(str), center, AREA_CENTER);
    }

    //-----------------//
    // preciseAbscissa //
    //-----------------//
    /**
     * Compute the rather precise abscissa, adjacent to the provided chord stem,
     * on the side implied by the specified approximate sysPoint
     *
     * @param sysPoint the (note) approximate center
     * @param chord    the chord/stem the note should be stuck to
     * @return the precise value for x
     */
    protected int preciseAbscissa (Point sysPoint,
                                   Chord chord)
    {
        // Compute symbol abscissa according to chord stem
        int stemX = chord.getTailLocation().x;
        double dx = stemHalfThickness - 2d; // slight adjustment

        if (sysPoint.x < stemX) {
            // Symbol is on left side of stem
            return (int) (stemX - dx);
        } else {
            // Symbol is on right side of stem
            return (int) (stemX + dx);
        }
    }

    //---------//
    // colorOf //
    //---------//
    /**
     * Report the color to use when painting elements related to the provided
     * voice
     *
     * @param voice the provided voice
     * @return the color to use
     */
    private Color colorOf (Voice voice)
    {
        if (coloredVoices) {
            // Use table of colors, circularly.
            int index = (voice.getId() - 1) % voiceColors.length;

            return voiceColors[index];
        } else {
            return defaultColor;
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Font for annotations */
        Constant.Integer basicFontSize = new Constant.Integer(
                "points",
                30,
                "Standard font size for annotations");

        /** dx between items in a key signature */
        final Scale.Fraction keySigItemDx = new Scale.Fraction(
                1.1,
                "dx between items in a key signature");

    }
}
