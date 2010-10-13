//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e P a i n t e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;
import omr.glyph.text.Sentence;
import omr.glyph.text.TextInfo;

import omr.log.Logger;

import omr.score.Score;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.Arpeggiate;
import omr.score.entity.Articulation;
import omr.score.entity.Beam;
import omr.score.entity.Chord;
import omr.score.entity.Clef;
import omr.score.entity.Coda;
import omr.score.entity.Dynamics;
import omr.score.entity.Fermata;
import omr.score.entity.KeySignature;
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
import omr.score.entity.Wedge;
import omr.score.ui.MusicFont.Alignment;
import omr.score.ui.MusicFont.Alignment.Horizontal;
import static omr.score.ui.MusicFont.Alignment.Horizontal.*;
import omr.score.ui.MusicFont.Alignment.Vertical;
import static omr.score.ui.MusicFont.Alignment.Vertical.*;
import omr.score.ui.MusicFont.CharDesc;
import omr.score.visitor.AbstractScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>ScorePainter</code> is an abstract class that defines common
 * features of a score painter.
 * <p>It is specialized by: <ul>
 * <li>{@link ScorePhysicalPainter} for the presentation of  score
 * entities over the sheet glyphs</li>
 * <li>{@link ScoreLogicalPainter} for the "ideal" score view</li>
 *
 * @author Hervé Bitteur
 */
public abstract class ScorePainter
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScorePainter.class);

    /** The alignment used by default */
    protected static final Alignment defaultAlignment = new Alignment(
        Horizontal.CENTER,
        Vertical.MIDDLE);
    protected static final Alignment       leftTop = new Alignment(
        Horizontal.LEFT,
        Vertical.TOP);
    protected static final Alignment       leftBottom = new Alignment(
        Horizontal.LEFT,
        Vertical.BOTTOM);

    /** A transformation to flip horizontally  (x' = -x) */
    protected static final AffineTransform horizontalFlip = new AffineTransform(
        -1,
        0,
        0,
        1,
        0,
        0);

    /** Font for annotations */
    protected static final Font basicFont = new Font(
        "Sans Serif",
        Font.PLAIN,
        constants.basicFontSize.getValue());

    /** Color for music symbols */
    public static Color musicColor = constants.musicColor.getValue();

    //~ Instance fields --------------------------------------------------------

    // Graphic context 
    protected final Graphics2D g;

    // Related score 
    protected Score       score;

    // Specific font for music symbols 
    protected Font        musicFont;

    // Global scale
    protected Scale       scale;

    // For staff lines 
    protected int         lineThickness;
    protected Stroke      lineStroke;

    // For stems
    protected float       stemThickness;
    protected float       stemHalfThickness;
    protected Stroke      stemStroke;

    // For beams
    protected float       beamThickness;
    protected float       beamHalfThickness;

    // The system being currently painted
    protected ScoreSystem system;
    protected SystemInfo  systemInfo;

    // Delta ordinate that corresponds to FLAG_2
    protected int FLAG_2_DY;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScorePainter //
    //--------------//
    /**
     * Creates a new ScorePainter object.
     *
     * @param graphics Graphic context
     */
    public ScorePainter (Graphics graphics)
    {
        g = (Graphics2D) graphics.create();

        // Anti-aliasing
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // visit Arpeggiate //
    //------------------//
    @Override
    public boolean visit (Arpeggiate arpeggiate)
    {
        // Draw an arpeggiate symbol with proper height
        // Using half-height of arpeggiate character as the elementary unit
        // We need clipping to draw half characters
        final Rectangle      oldClip = g.getClipBounds();
        final PixelRectangle box = arpeggiate.getBox();

        // How many half arpeggiate symbols do we need?
        final TextLayout  layout = layout(ARPEGGIATO);
        final Rectangle2D rect = layout.getBounds();
        final int         count = (int) Math.rint(
            (2 * box.height) / rect.getHeight());
        final Point       start = new Point(box.x, box.y + box.height);
        final int         width = (int) Math.rint(rect.getWidth());
        final int         halfHeight = (int) Math.rint(rect.getHeight() / 2);

        // Draw count * half symbols, bottom up
        for (int i = 0; i < count; i++) {
            final Rectangle clip = oldClip.intersection(
                new Rectangle(start.x, start.y - halfHeight, width, halfHeight));

            // Anything to draw?
            if ((clip.width > 0) && (clip.height > 0)) {
                g.setClip(clip);
                layout.draw(g, start.x, start.y);
            }

            // Move up half height (-1 pixel to avoid any gap)
            start.y -= (halfHeight - 1);
        }

        // Restore clip
        g.setClip(oldClip);

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
        final PixelPoint left = new PixelPoint(beam.getLeftPoint());
        final PixelPoint right = new PixelPoint(beam.getRightPoint());
        final int        dx = (int) Math.rint(stemHalfThickness);
        final int        dy = (int) Math.rint(beamHalfThickness);

        // Compute precise abscissae values
        if (beam.isHook()) {
            // Just a hook stuck to a stem on one side
            Chord chord = beam.getChords()
                              .first();

            if (chord.getCenter().x < beam.getCenter().x) {
                left.x -= dx;
            } else {
                right.x += dx;
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
        g.fill(polygon);

        return true;
    }

    //-------------//
    // visit Chord //
    //-------------//
    @Override
    public boolean visit (Chord chord)
    {
        if (chord.getStem() == null) {
            return true;
        }

        final PixelPoint tail = chord.getTailLocation();
        final PixelPoint head = chord.getHeadLocation();

        if ((tail == null) || (head == null)) {
            chord.addError("No head - tail defined for chord");

            return false;
        }

        // Flags ?
        final int fn = chord.getFlagsNumber();

        if (fn == 0) {
            return true;
        }

        final PixelPoint loc = location(tail, chord);

        if (tail.y < head.y) {
            // Flags down
            final TextLayout flag1 = layout(COMBINING_FLAG_1);
            final TextLayout flag2 = layout(COMBINING_FLAG_2);
            final Alignment  align = leftTop;

            for (int i = 0; i < (fn / 2); i++) {
                paint(flag2, loc, align);
                loc.y += FLAG_2_DY;
            }

            if ((fn % 2) != 0) {
                paint(flag1, loc, align);
            }
        } else {
            // Flags up
            final TextLayout flag1up = layout(COMBINING_FLAG_1_UP);
            final TextLayout flag2up = layout(COMBINING_FLAG_2_UP);
            final Alignment  align = leftBottom;

            for (int i = 0; i < (fn / 2); i++) {
                paint(flag2up, loc, align);
                loc.y -= FLAG_2_DY;
            }

            if ((fn % 2) != 0) {
                paint(flag1up, loc, align);
            }
        }

        return true;
    }

    //------------//
    // visit Clef //
    //------------//
    @Override
    public boolean visit (Clef clef)
    {
        final Staff             staff = clef.getStaff();
        final Shape             shape = clef.getShape();
        final String            str = MusicFont.getCharDesc(shape)
                                               .getString();
        final PixelRectangle    box = clef.getBox();
        final FontRenderContext frc = g.getFontRenderContext();
        TextLayout              layout = new TextLayout(str, musicFont, frc);
        TextLayout              ottava = null; // The '8' glyph to be drawn

        // Adjust ratio so that the symbol fits the underlying glyph
        final Rectangle2D bounds = layout.getBounds();

        if (ShapeRange.OttavaClefs.contains(shape)) {
            // Add height of Alta/Bassa ottava
            ottava = layout(MusicFont.ALTA_BASSA_DESC.getString());
            bounds.setRect(
                bounds.getX(),
                bounds.getY(),
                bounds.getWidth(),
                bounds.getHeight() + ottava.getBounds().getHeight());
        }

        // Use scaling based only of height ratio (except for percussion clef)
        final AffineTransform at = AffineTransform.getScaleInstance(
            (shape == PERCUSSION_CLEF) ? (box.width / bounds.getWidth())
                        : (box.height / bounds.getHeight()),
            box.height / bounds.getHeight());
        layout = layout(str, at);

        Rectangle rect = null;

        if (shape == PERCUSSION_CLEF) {
            // Baseline is center
            rect = paint(layout, location(clef.getCenter()));
        } else if (shape == F_CLEF) {
            //            // Either use contour of the underlying glyph
            //            PixelPoint topCenter = system.toPixelPoint(
            //                new PixelPoint(clef.getCenter().x, clef.getBox().y));

            // Or fit the two points of F_CLEF around pitch: -2
            PixelPoint topCenter = location(clef.getCenter(), staff, -2);
            topCenter.y -= (layout.getBounds()
                                  .getHeight() / 3.3);
            rect = paint(layout, topCenter, new Alignment(CENTER, TOP));
        } else {
            // Baseline is end of staff
            rect = paint(
                layout,
                location(clef.getCenter(), staff, 4),
                new Alignment(CENTER, BASELINE));
        }

        // Alta / Bassa to be drawn?
        if ((shape == G_CLEF_OTTAVA_ALTA) || (shape == F_CLEF_OTTAVA_ALTA)) {
            PixelPoint topCenter = new PixelPoint(
                rect.x + (rect.width / 2),
                rect.y);
            paint(ottava, topCenter, leftBottom);
        } else if ((shape == G_CLEF_OTTAVA_BASSA) ||
                   (shape == F_CLEF_OTTAVA_BASSA)) {
            PixelPoint bottomCenter = new PixelPoint(
                rect.x + (rect.width / 2),
                rect.y + rect.height);
            paint(ottava, bottomCenter, leftTop);
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
        final Staff          staff = keySignature.getStaff();
        final Shape          clefKind = keySignature.getClefKind();
        final int            key = keySignature.getKey();
        final int            sign = Integer.signum(key);
        final Shape          shape = (key < 0) ? FLAT : SHARP;
        final TextLayout     layout = layout(shape);
        final PixelRectangle box = keySignature.getBox();
        final int            unitDx = getKeySigItemDx();

        if (box == null) {
            logger.warning("Null box for " + keySignature);

            return false;
        }

        // Flats : use vertical stick on left
        // Sharps : use center of the two vertical sticks
        final Alignment alignment = new Alignment(
            (key < 0) ? LEFT : CENTER,
            BASELINE);
        PixelPoint      point = new PixelPoint(box.x, 0);

        for (int i = 1; i <= (key * sign); i++) {
            int     n = i * sign;
            double  pitch = KeySignature.getItemPosition(n, clefKind);
            Integer ref = keySignature.getItemPixelAbscissa(n);

            if (ref != null) {
                ///logger.info(n + ":" + ref + " for " + keySignature);
                point = new PixelPoint(ref, 0);
            }

            paint(layout, location(point, staff, pitch), alignment);
            point.x += unitDx; // Fall-back if ref is not known
        }

        return true;
    }

    //----------------------//
    // visit MeasureElement //
    //----------------------//
    @Override
    public boolean visit (MeasureElement measureElement)
    {
        if (measureElement.getShape() != null) {
            try {
                paint(
                    layout(measureElement.getShape(), measureElement.getBox()),
                    location(measureElement.getReferencePoint()));
            } catch (Exception ex) {
                logger.warning("Cannot paint " + measureElement, ex);
            }
        }

        return true;
    }

    //------------//
    // visit Note //
    //------------//
    @Override
    public boolean visit (Note note)
    {
        final Chord      chord = note.getChord();
        final Glyph      stem = chord.getStem();
        final Shape      shape = note.getShape();
        final PixelPoint center = note.getCenter();
        Shape            displayShape; // What is really displayed

        // Note head
        if (stem != null) {
            // Note is attached to a stem, link the note display to the stem one
            if (ShapeRange.HeadAndFlags.contains(shape)) {
                displayShape = NOTEHEAD_BLACK;
            } else {
                displayShape = shape;
            }

            paint(
                layout(displayShape),
                noteLocation(note),
                new Alignment(
                    (center.x < chord.getTailLocation().x) ? RIGHT : LEFT,
                    MIDDLE));
        } else {
            // Use special display icons for some shapes
            displayShape = shape.getPhysicalShape();
            paint(layout(displayShape), noteLocation(note));
        }

        // Accidental ?
        final Glyph accid = note.getAccidental();

        if (accid != null) {
            paint(
                layout(accid.getShape()),
                accidentalLocation(note, accid),
                new Alignment(CENTER, BASELINE));
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
        g.draw(slur.getCurve());

        return true;
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart part)
    {
        // Should we draw dummy parts?
        if (part.isDummy() &&
            !PaintingParameters.getInstance()
                               .isDummyPainting()) {
            return false;
        }

        // Draw a brace?
        if (part.getBrace() != null) {
            final String            str = "{";
            final FontRenderContext frc = g.getFontRenderContext();
            Font                    font = TextInfo.basicFont.deriveFont(100f);
            TextLayout              layout = new TextLayout(str, font, frc);
            final Rectangle2D       rect = layout.getBounds();
            final PixelRectangle    braceBox = braceBox(part);
            final AffineTransform   fat = AffineTransform.getScaleInstance(
                braceBox.width / rect.getWidth(),
                braceBox.height / rect.getHeight());
            font = font.deriveFont(fat);
            layout = new TextLayout(str, font, frc);
            paint(layout, braceBox.getCenter());
        }

        // Render the part starting barline, if any
        if (part.getStartingBarline() != null) {
            part.getStartingBarline()
                .accept(this);
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
        // Force y alignment for items of the same sentence
        final Sentence          sentence = text.getSentence();
        final PixelPoint        location = sentence.getLocation();
        final PixelPoint        refPoint = text.getReferencePoint();
        final String            str = text.getContent();
        final FontRenderContext frc = g.getFontRenderContext();
        Font                    font = text.getFont()
                                           .deriveFont(
            (float) text.getFontSize());
        TextLayout              layout = new TextLayout(str, font, frc);
        final Rectangle2D       rect = layout.getBounds();
        final PixelRectangle    box = text.getBox();
        final double            xRatio = box.width / rect.getWidth();

        if (xRatio > 100) {
            logger.warning("BINGO");
        }

        double yRatio = box.height / rect.getHeight();

        if (logger.isFineEnabled()) {
            logger.fine(
                "xRatio:" + (float) xRatio + " yRatio:" + (float) yRatio + " " +
                text);
        }

        // Sign of something wrong
        if (yRatio > 1.3) {
            yRatio = 1;
        }

        final AffineTransform fat = AffineTransform.getScaleInstance(
            xRatio,
            yRatio);
        font = font.deriveFont(fat);
        layout = new TextLayout(str, font, frc);
        paint(
            layout,
            new PixelPoint(refPoint.x, location.y),
            new Alignment(LEFT, BASELINE));

        return true;
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    @Override
    public boolean visit (TimeSignature timeSignature)
    {
        try {
            final Shape      shape = timeSignature.getShape();
            final PixelPoint center = timeSignature.getCenter();
            final Staff      staff = timeSignature.getStaff();
            final int        dy = staff.pitchToPixels(-2);

            if (shape == Shape.NO_LEGAL_TIME) {
                // If this is an illegal shape, do not draw anything.
                // TODO: we could draw a special sign for this
                return false;
            }

            // Special symbol?
            if ((shape == COMMON_TIME) || (shape == CUT_TIME)) {
                paint(layout(shape), location(center));
            } else {
                // Paint numerator
                paintTimeNumber(
                    timeSignature.getNumerator(),
                    new PixelPoint(center.x, center.y - dy));

                // Paint denominator
                paintTimeNumber(
                    timeSignature.getDenominator(),
                    new PixelPoint(center.x, center.y + dy));
            }
        } catch (InvalidTimeSignature ex) {
            logger.warning("Invalid time signature", ex);
        }

        return true;
    }

    //-------------//
    // visit Wedge //
    //-------------//
    @Override
    public boolean visit (Wedge wedge)
    {
        if (wedge.isStart()) {
            final PixelRectangle box = wedge.getGlyph()
                                            .getContourBox();

            PixelPoint           single;
            PixelPoint           top;
            PixelPoint           bot;

            if (wedge.getShape() == Shape.CRESCENDO) {
                single = new PixelPoint(box.x, box.y + (box.height / 2));
                top = new PixelPoint(box.x + box.width, box.y);
                bot = new PixelPoint(box.x + box.width, box.y + box.height);
            } else {
                single = new PixelPoint(
                    box.x + box.width,
                    box.y + (box.height / 2));
                top = new PixelPoint(box.x, box.y);
                bot = new PixelPoint(box.x, box.y + box.height);
            }

            paintLine(single, top);
            paintLine(single, bot);
        }

        return true;
    }

    //--------------------//
    // accidentalLocation //
    //--------------------//
    /**
     * Report the precise location to be used for a given accidental sigh
     * (with respect to the related note)
     * @param note the related note
     * @param accidental the accidental glyph
     * @return the location to be used for painting
     */
    protected abstract PixelPoint accidentalLocation (Note  note,
                                                      Glyph accidental);

    //----------//
    // braceBox //
    //----------//
    /**
     * Report the precise box to be used for a given brace
     * @param part the related part
     * @return the box to be used for painting
     */
    protected abstract PixelRectangle braceBox (SystemPart part);

    //--------------//
    // noteLocation //
    //--------------//
    /**
     * Report the precise location to be used for a given note
     * @param note the related note
     * @return the location to be used for painting
     */
    protected abstract PixelPoint noteLocation (Note note);

    //-----------------//
    // getKeySigItemDx //
    //-----------------//
    /**
     * Report the theoretical abscissa gap between items of one key signature
     * (the individual sharp or flat signs)
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
        lineThickness = scale.mainFore();
        lineStroke = new BasicStroke(
            lineThickness,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND);

        // Determine stems parameters
        stemThickness = scale.mainFore();
        stemHalfThickness = stemThickness / 2;
        stemStroke = new BasicStroke(
            stemThickness,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND);

        // Determine beams parameters
        if (score.getBeamThickness() != null) {
            beamThickness = score.getBeamThickness();
            beamHalfThickness = beamThickness / 2;
        }

        // Set stroke for lines
        g.setStroke(lineStroke);
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a String of SToccata characters (transformed by
     * the provided AffineTransform if any)
     * @param str the string of proper codes
     * @param fat potential affine transformation
     * @return the (sized) TextLayout ready to be drawn
     */
    protected TextLayout layout (String          str,
                                 AffineTransform fat)
    {
        FontRenderContext frc = g.getFontRenderContext();
        Font              font = (fat == null) ? musicFont
                                 : musicFont.deriveFont(fat);

        return new TextLayout(str, font, frc);
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a String of SToccata characters
     * @param str the string of proper codes
     * @return the TextLayout ready to be drawn
     */
    protected TextLayout layout (String str)
    {
        return layout(str, null);
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a Shape, using its related String of SToccata
     * characters, and potentially sized by an AffineTransform
     * @param shape the shape to be drawn with SToccata chars
     * @param fat potential affine transformation
     * @return the (sized) TextLayout ready to be drawn
     */
    protected TextLayout layout (Shape           shape,
                                 AffineTransform fat)
    {
        CharDesc desc = MusicFont.getCharDesc(shape);

        if (desc == null) {
            logger.warning("No SToccata desc for " + shape);

            return null;
        }

        //Hack for OLD_QUARTER_REST, use a flipped (EIGHTH_REST) shape
        if (shape == OLD_QUARTER_REST) {
            if (fat != null) {
                fat.concatenate(horizontalFlip);
            } else {
                fat = horizontalFlip;
            }
        }

        return layout(desc.getString(), fat);
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a Shape, using its related String of SToccata
     * characters, and properly sized to fit the box of the underlying glyph
     * @param shape the shape to be drawn with SToccata chars
     * @param glyphBox the glyph box to fit as much as possible
     * @return the adjusted TextLayout ready to be drawn
     */
    protected TextLayout layout (Shape          shape,
                                 PixelRectangle glyphBox)
    {
        CharDesc desc = MusicFont.getCharDesc(shape);

        if (desc == null) {
            return null;
        }

        String            str = desc.getString();
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout        layout = new TextLayout(str, musicFont, frc);

        // Compute proper affine transformation
        Rectangle2D     rect = layout.getBounds();
        AffineTransform fat = AffineTransform.getScaleInstance(
            glyphBox.width / rect.getWidth(),
            glyphBox.height / rect.getHeight());

        return layout(str, fat);
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a Shape, using its related String of SToccata
     * characters
     * @param shape the shape to be drawn with SToccata chars
     * @return the TextLayout ready to be drawn
     */
    protected TextLayout layout (Shape shape)
    {
        return layout(shape, (AffineTransform) null);
    }

    //----------//
    // location //
    //----------//
    /**
     * Build the desired absolute drawing point corresponding to a provided
     * system-based point
     * @param sysPoint the system-based drawing point
     * @return the proper PixelPoint
     */
    protected PixelPoint location (PixelPoint sysPoint)
    {
        return sysPoint;
    }

    //----------//
    // location //
    //----------//
    /**
     * Build the desired absolute drawing point, the abscissa being adjusted to
     * fit on the provided chord stem, and the ordinate being computed from the
     * pitch position with respect to the containing staff
     * @param sysPoint the (approximate) system-based drawing point
     * @param chord the chord whose stem must be stuck to the (note) symbol
     * @param staff the containing staff
     * @param pitch the pitch position with respect to the staff
     * @return the PixelPoint, as precise as possible in X & Y
     */
    protected PixelPoint location (PixelPoint sysPoint,
                                   Chord      chord,
                                   Staff      staff,
                                   double     pitch)
    {
        return location(
            new PixelPoint(
                preciseAbscissa(sysPoint, chord),
                staff.getTopLeft().y + staff.pitchToPixels(pitch)));
    }

    //----------//
    // location //
    //----------//
    /**
     * Build the desired absolute drawing point, the abscissa being adjusted to
     * fit on the provided chord stem
     * @param sysPoint the (approximate) system-based drawing point
     * @param chord the chord whose stem must be stuck to the (note) symbol
     * @return the PixelPoint, as precise as possible in X
     */
    protected PixelPoint location (PixelPoint sysPoint,
                                   Chord      chord)
    {
        return location(
            new PixelPoint(preciseAbscissa(sysPoint, chord), sysPoint.y));
    }

    //----------//
    // location //
    //----------//
    /**
     * Build the desired absolute drawing point, the ordinate being computed
     * from the pitch position with respect to the containing staff
     * @param sysPoint the (approximate) system-based drawing point
     * @param staff the containing staff
     * @param pitch the pitch position with respect to the staff
     * @return the PixelPoint, as precise as possible in Y
     */
    protected PixelPoint location (PixelPoint sysPoint,
                                   Staff      staff,
                                   double     pitch)
    {
        return location(
            new PixelPoint(
                sysPoint.x,
                staff.getTopLeft().y + staff.pitchToPixels(pitch)));
    }

    //-------//
    // paint //
    //-------//
    /**
     * This is the general paint method for drawing a symbol layout, at a
     * specified location, using a specified alignment
     * @param layout what: the symbol, perhaps transformed
     * @param location where: the precise location in the display
     * @param alignment how: the way the symbol is aligned wrt the location
     * @return the rectangular bounds of the painting done
     */
    protected Rectangle paint (TextLayout layout,
                               PixelPoint location,
                               Alignment  alignment)
    {
        try {
            // Compute symbol origin
            Rectangle2D bounds = layout.getBounds();
            Point2D     toOrigin = MusicFont.toOrigin(bounds, alignment);
            Point2D     origin = new Point2D.Double(
                location.x + toOrigin.getX(),
                location.y + toOrigin.getY());

            // Draw the symbol
            layout.draw(g, (float) origin.getX(), (float) origin.getY());

            // Report the actual drawn symbol rectangle in user space
            return new Rectangle(
                (int) Math.rint(bounds.getX() + origin.getX()),
                (int) Math.rint(bounds.getY() + origin.getY()),
                (int) Math.rint(bounds.getWidth()),
                (int) Math.rint(bounds.getHeight()));
        } catch (Exception ex) {
            logger.warning("Cannot paint at " + location, ex);

            return null;
        }
    }

    //-------//
    // paint //
    //-------//
    /**
     * A convenient painting method, using default alignment (CENTER + MIDDLE)
     * @param layout what: the symbol, perhaps transformed
     * @param location where: the precise location in the display
     * @return the rectangular bounds of the painting done
     */
    protected Rectangle paint (TextLayout layout,
                               PixelPoint location)
    {
        return paint(layout, location, defaultAlignment);
    }

    //------------//
    // paintBrace //
    //------------//
    protected void paintBrace (PixelPoint top,
                               int        height)
    {
        Font                  font = TextInfo.basicFont.deriveFont(100f);
        TextLayout            layout = new TextLayout(
            "{",
            font,
            g.getFontRenderContext());
        Rectangle2D           rect = layout.getBounds();

        final AffineTransform at = AffineTransform.getScaleInstance(
            1f,
            height / rect.getHeight());
        font = font.deriveFont(at);
        layout = new TextLayout("{", font, g.getFontRenderContext());
        rect = layout.getBounds();

        PixelPoint pt = new PixelPoint(
            top.x - (int) rect.getX() - (int) rect.getWidth(),
            top.y - (int) rect.getY());
        layout.draw(g, pt.x, pt.y);
    }

    //-----------//
    // paintLine //
    //-----------//
    /**
     * Draw a line from one PixelPoint to another PixelPoint
     *
     * @param from first point
     * @param to second point
     */
    protected void paintLine (PixelPoint from,
                              PixelPoint to)
    {
        if ((from != null) && (to != null)) {
            g.drawLine(from.x, from.y, to.x, to.y);
        } else {
            logger.warning("line not painted due to null reference");
        }
    }

    //-----------------//
    // paintTimeNumber //
    //-----------------//
    /**
     * Paint a (time) number using the coordinates in units of its center point
     * within the containing system part
     *
     * @param shape the shape whose icon must be painted
     * @param point system-based given point in units
     */
    protected void paintTimeNumber (int        number,
                                    PixelPoint sysCenter)
    {
        int   base = MusicFont.getCodes(TIME_ZERO)[0];
        int[] codes = (number > 9) ? new int[2] : new int[1];
        int   index = 0;

        if (number > 9) {
            codes[index++] = base + (number / 10);
        }

        codes[index] = base + (number % 10);

        String str = new String(codes, 0, codes.length);
        paint(
            layout(str),
            location(sysCenter),
            new Alignment(Horizontal.CENTER, Vertical.MIDDLE));
    }

    //-----------------//
    // preciseAbscissa //
    //-----------------//
    /**
     * Compute the rather precise abscissa, adjacent to the provided chord stem,
     * on the side implied by the specified approximate sysPoint
     * @param sysPoint the (note) approximate center
     * @param chord the chord/stem the note should be stuck to
     * @return the precise value for x
     */
    protected int preciseAbscissa (PixelPoint sysPoint,
                                   Chord      chord)
    {
        // Compute symbol abscissa according to chord stem
        int    stemX = chord.getTailLocation().x;
        double dx = stemHalfThickness - 1; // Added -1

        if (sysPoint.x < stemX) {
            // Symbol is on left side of stem
            return (int) (stemX - dx);
        } else {
            // Symbol is on right side of stem
            return (int) (stemX + dx);
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
            20,
            "Standard font size for annotations");

        /** Alpha parameter for slot axis transparency (0 .. 255) */
        final Constant.Integer slotAlpha = new Constant.Integer(
            "ByteLevel",
            150,
            "Alpha parameter for slot axis transparency (0 .. 255)");

        /** Color for score entities */
        final Constant.Color musicColor = new Constant.Color(
            "#aaffaa",
            "Color for score entities");

        /** dx between items in a key signature */
        final Scale.Fraction keySigItemDx = new Scale.Fraction(
            1.1,
            "dx between items in a key signature");
    }
}
