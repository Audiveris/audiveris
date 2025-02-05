//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   M e t r o n o m e I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.classifier.ShapeClassifier;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.TEXT;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.ui.ObjectUIModel;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.BeatUnitInter.Note;
import static org.audiveris.omr.sig.inter.BeatUnitInter.Note.noteOf;
import static org.audiveris.omr.sig.inter.Inters.byAbscissa;
import org.audiveris.omr.sig.relation.ChordSentenceRelation;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.text.TextChar;
import org.audiveris.omr.text.TextLine;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.text.TextWord;
import org.audiveris.omr.ui.field.MusicPane;
import org.audiveris.omr.ui.symbol.MetronomeSymbol;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.RegexUtil.getGroup;
import static org.audiveris.omr.util.RegexUtil.group;
import static org.audiveris.omr.util.StringUtil.codesOf;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>MetronomeInter</code> is a sentence that represents a metronome mark.
 * <p>
 * In the current implementation, a metronome mark can contain:
 * <ol>
 * <li>(Optional) a tempo textual indication, like "Allegretto"
 * <li>(Optional) an opening parenthesis, '('
 * <li>A note symbol, like a quarter note or a dotted eighth note to specify the beat unit
 * <li>The equal sign '=' (we also accept the ':' character)
 * <li>(Optional) some text like "ca."
 * <li>A positive number, like "100", to specify the number of beat units per minute (bpm)
 * <li>(Optional) a second number, introduced by '-', like "-120", to specify a maximum bpm value
 * <li>(Optional) some text like "env."
 * <li>(Optional) a closing parenthesis, ')'
 * <li>(Optional) some final text, ignored
 * </ol>
 * <p>
 * Examples of various beat-unit values: <br>
 * <img alt="Examples of various beat-unit values" src=
 * "https://s3-us-west-2.amazonaws.com/courses-images-archive-read-only/wp-content/uploads/sites/950/2015/06/26002610/Screen-Shot-2015-06-23-at-3.03.20-PM.png">
 * <p>
 * Instead of a precise single number, we can have two numbers to indicate an interval: <br>
 * <img alt="Example with an interval value" src=
 * "https://archive.steinberg.help/dorico/v2/en/_shared_picts/picts/dorico/notation_reference/tempo_metronome_mark_range_example.png">
 * <p>
 * Tesseract OCR is not (yet?) able to recognize music notes within a sentence, these notes are thus
 * mistaken with letters. For example, a quarter note is typically mistaken with a capital "J".
 * <p>
 * However, the equal sign ("=") followed by the bpm value or interval are standard text characters
 * and can thus be correctly OCR'd.
 * <p>
 * The trick is organized around this "= bpm" sentence part: the preceding OCR'd character(s) are
 * wrong but their underlying glyph can be extracted and then submitted to the glyph classifier in
 * order to recognize a typical metronome beat unit shape.
 * <p>
 * TODO: There are more complex marks, which could be covered in a future version,
 * but first I need to figure out what they mean precisely.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "metronome")
public class MetronomeInter
        extends SentenceInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(MetronomeInter.class);

    private static final String TEMPO = "tempo";

    private static final String PAR_START = "parStart"; // Opening parenthesis

    private static final String NOTE = "note";

    private static final String EQUAL = "equal";

    private static final String BPM1 = "bpm1";

    private static final String BPM_EXT = "bpmext";

    private static final String BPM2 = "bpm2";

    private static final String BPM_TEXT = "bpmtext";

    private static final String PAR_STOP = "parStop"; // Closing parenthesis

    private static final String GARBAGE = "garbage";

    private static final String spacePat = "\\s*";

    /**
     * Pattern for tempo textual indication.
     * It stops before an opening parenthesis or a beat unit code.
     * It includes a final space.
     */
    private static final String tempoPat = group(TEMPO, "[^\\(" + beatUnitRegexp() + "]*\\s");

    /** Pattern for opening parenthesis. */
    private static final String parPatStart = group(PAR_START, "\\(");

    /** Pattern for note. */
    ///private static final String notePat = group(NOTE, "[^=\\s]+");
    private static final String notePat = group(NOTE, "[^=]+");

    /** Pattern for equal. */
    private static final String equalPat = group(EQUAL, "[=:]");

    /** Pattern for bpm numerical specification, a single value or an interval: 123[-456]. */
    private static final String bpmPat = group(BPM1, "[0-9]+") + spacePat + group(
            BPM_EXT,
            "-" + spacePat + group(BPM2, "[0-9]+")) + "?";

    /**
     * Pattern for bpm full text specification.
     * The spec starts at '=' excluded and stops at either ')' excluded or the end of the sentence.
     * It is meant to grab text portions like: "ca. 100", "110", "120-140", "130 env."
     */
    private static final String bpmTextPat = group(
            BPM_TEXT,
            "[^0-9]*" + spacePat + bpmPat + spacePat + "[^\\)]*");

    /** Pattern for closing parenthesis. */
    private static final String parPatStop = group(PAR_STOP, "\\)");

    /** Pattern for potential ending garbage. */
    private static final String garbagePat = group(GARBAGE, ".*");

    /** Pattern for the whole metronome mark. */
    private static final String metroPat = tempoPat + "?" //
            + parPatStart + "?" + spacePat //
            + notePat + spacePat //
            + equalPat + spacePat //
            + bpmTextPat + parPatStop + "?" //
            + spacePat + garbagePat;

    private static final Pattern metroPattern = Pattern.compile(metroPat);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Underlying model, if any. */
    private Model model;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    @SuppressWarnings("unused")
    private MetronomeInter ()
    {
    }

    /**
     * Creates a new <code>MetronomeInter</code> object meant for manual assignment.
     *
     * @param grade inter grade
     */
    public MetronomeInter (Double grade)
    {
        super(TextRole.Metronome, grade);
        shape = Shape.METRONOME;
    }

    /**
     * Create a new <code>MetronomeInter</code> object from a former SentenceInter.
     *
     * @param s the sentence to be "replaced"
     */
    public MetronomeInter (SentenceInter s)
    {
        super(s.getBounds(), s.getGrade(), s.getMeanFont(), TextRole.Metronome);
        shape = Shape.METRONOME;
    }

    /**
     * Create a new <code>MetronomeInter</code> object from an OCR'd line
     *
     * @param line the OCR'd text line
     */
    private MetronomeInter (TextLine line)
    {
        super(line.getBounds(), line.getGrade(), line.getMeanFont(), TextRole.Metronome);
        shape = Shape.METRONOME;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //------------//
    // buildModel //
    //------------//
    /**
     * Build the model from metronome value and word members.
     *
     * @return the model built
     */
    private Model buildModel ()
    {
        final String value = super.getValue();
        final Model m = parseValue(value, false); // Get logical informations

        if (m == null) {
            return null;
        }

        // Complete model with physical informations

        final List<Inter> members = getMembers();
        Collections.sort(members, byAbscissa); // Safer
        boolean afterBeat = false; // Have we processed the beat unit yet?

        for (Inter member : members) {
            final WordInter word = (WordInter) member;
            final String val = word.getValue();

            if (word instanceof BeatUnitInter beatUnit) {
                m.unitFontSize = beatUnit.getFontInfo().pointsize;

                final SheetStub stub = staff.getSystem().getSheet().getStub();
                final MusicFamily musicFamily = stub.getMusicFamily();
                final FontInfo fi = new FontInfo(m.unitFontSize, musicFamily.getFontName());
                final MusicFont f = new MusicFont(fi);
                final Note note = Note.noteOf(m.unit);
                final String str = note.getString();
                final TextLayout layout = f.layout(str);
                final Rectangle2D rect = layout.getBounds();
                final Rectangle buRect = beatUnit.getBounds();
                m.baseCenter = new Point2D.Double(
                        buRect.x + buRect.width / 2,
                        buRect.y - rect.getY());

                afterBeat = true;
            } else {
                if (!afterBeat) {
                    if (m.tempoFontSize == null) {
                        m.tempoFontSize = word.getFontInfo().pointsize;
                    }
                } else {
                    if (m.bpmFontSize == null) {
                        m.bpmFontSize = word.getFontInfo().pointsize;
                    }
                }
            }
        }

        logger.debug("buildModel. {}", m);
        return m;
    }

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public boolean deriveFrom (ShapeSymbol symbol,
                               Sheet sheet,
                               MusicFont font,
                               Point dropLocation)
    {
        logger.trace("deriveFrom dropLocation:{}", dropLocation);
        final MetronomeSymbol metroSymbol = (MetronomeSymbol) symbol;

        model = metroSymbol.getModel(font, dropLocation);
        logger.trace("deriveFrom {}", model);

        setBounds(model.box.getBounds());

        return true;
    }

    //-------------//
    // getBeatUnit //
    //-------------//
    /**
     * Report the beat unit specified in this metronome mark.
     *
     * @return the 'note' used as beat unit
     */
    private BeatUnitInter getBeatUnit ()
    {
        for (Inter member : getMembers()) {
            if (member instanceof BeatUnitInter beatUnit) {
                return beatUnit;
            }
        }

        return null;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            final List<Inter> members = getMembers();

            if (!members.isEmpty()) {
                bounds = Entities.getBounds(members);
            } else if (glyph != null) {
                bounds = glyph.getBounds();
            }
        }

        return (bounds != null) ? new Rectangle(bounds) : null;
    }

    //--------//
    // getBpm //
    //--------//
    /**
     * Report the bpm value (if single) or the mean bpm value (if interval)
     *
     * @return the integer to be used as bpm
     */
    private Integer getBpm ()
    {
        if (model == null) {
            model = buildModel();
        }

        if (model.bpm2 != null) { // Interval
            return (model.bpm1 + model.bpm2) / 2;
        }

        return model.bpm1; // Single value
    }

    //    //---------//
    //    // getBpm1 //
    //    //---------//
    //    /**
    //     * Report the (minimum) bpm value.
    //     *
    //     * @return the bpm (minimum) value
    //     */
    //    private Integer getBpm1 ()
    //    {
    //        if (model == null) {
    //            model = buildModel();
    //        }
    //
    //        return model.bpm1; // Single value
    //    }

    //------------//
    // getBpmText //
    //------------//
    /**
     * Report the full bpm specification, that is the text that follows the equal sign.
     * <p>
     * This is either a number or an interval (min-max), perhaps introduced and/or followed by some
     * text, but excluding the closing parenthesis if any.
     * <p>
     * Examples:
     * <ul>
     * <li>"60"
     * <li>"90-100"
     * <li>"Ca. 120-144"
     * <li>"69 env."
     * </ul>
     *
     * @return the full bpm specification string or an empty string if none
     */
    public String getBpmText ()
    {
        final Matcher matcher = metroPattern.matcher(getValue());

        if (!matcher.matches()) {
            return "";
        }

        return getBpmText(matcher);
    }

    private String getBpmText (Matcher matcher)
    {
        return getGroup(matcher, BPM_TEXT);
    }

    //-----------------//
    // getDisplayValue //
    //-----------------//
    /**
     * Report the metronome content, meant for display in InterBoard.
     *
     * @return text content
     */
    public String getDisplayValue ()
    {
        if (model == null) {
            model = buildModel();
        }

        String val = getValue();

        // Discard the spaces introduced by getValue() after and before parentheses if any
        val = val.replaceAll("\\( ", "(");
        val = val.replaceAll(" \\)", ")");

        return val;
    }

    //---------//
    // getNote //
    //---------/
    /**
     * Report the note symbol used as beat unit.
     *
     * @return the note symbol
     */
    public Note getNote ()
    {
        final BeatUnitInter beatUnit = getBeatUnit();

        if (beatUnit == null) {
            return null;
        }

        return beatUnit.getNote();
    }

    //----------------------//
    // getQuartersPerMinute //
    //----------------------//
    /**
     * Report the number of <b>quarters</b> per minute,
     * based on the note symbol and the number of beats per minute (bpm).
     *
     * @return the tempo, expressed in quarters per minute
     */
    public int getQuartersPerMinute ()
    {
        final Rational r = getBeatUnit().getNote().quarterValue().times(getBpm());
        return rounded(r.doubleValue());
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        final String str = getValue();
        final Matcher matcher = metroPattern.matcher(str);
        final StringBuilder sb = new StringBuilder();

        if (!matcher.matches()) {
            sb.append("INVALID");
            if (model == null || model.unit == null)
                sb.append(", no unit");
            if (getGroup(matcher, EQUAL).isBlank())
                sb.append(", no '='");
            if (model == null || model.bpm1 == null)
                sb.append(", no bpm");
        } else {
            final Note note = getNote();
            sb.append((note != null) ? note.toShape() : "no unit") //
                    .append(' ').append(getBpmText(matcher));
        }

        return sb.toString();
    }

    //--------------//
    // getTempoText //
    //--------------//
    /**
     * Report the tempo indication (such as: Andante) that may precede the metronome mark.
     *
     * @return the tempo text, perhaps empty
     */
    public String getTempoText ()
    {
        final String str = getValue();
        final Matcher matcher = metroPattern.matcher(str);

        if (!matcher.matches()) {
            return "";
        }

        return getGroup(matcher, TEMPO);
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the metronome content, built out of the contained words and make sure the model
     * has been built.
     *
     * @return text content
     */
    @Override
    public String getValue ()
    {
        if (model == null) {
            model = buildModel();
        }

        return super.getValue();
    }

    //----------------//
    // hasParentheses //
    //----------------//
    /**
     * Report whether the metronome is wrapped with parentheses (at least one).
     *
     * @return true if so
     */
    public boolean hasParentheses ()
    {
        if (model == null) {
            model = buildModel();
        }

        return model.parentheses;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        if (model == null) {
            model = buildModel();
        }

        final StringBuilder sb = new StringBuilder(super.internals());

        if (getNote() != null) {
            sb.append(" beat:").append(getNote());
        }

        if (model.bpm1 != null) {
            sb.append(" bpm1:").append(model.bpm1);
        }

        if (model.bpm2 != null) {
            sb.append(" bpm2:").append(model.bpm2);
        }

        if (model.parentheses) {
            sb.append(" parentheses");
        }

        return sb.toString();
    }

    //------//
    // link //
    //------//
    /**
     * Try to link this metronome sentence.
     *
     * @param system the related system
     */
    @Override
    public void link (SystemInfo system)
    {
        try {
            if (isVip()) {
                logger.info("VIP link {}", this);
            }

            if (!sig.hasRelation(this, ChordSentenceRelation.class)) {
                // Map metronome with proper chord below
                final Collection<Link> links = searchLinks(system);

                if (!links.isEmpty()) {
                    links.iterator().next().applyTo(this);
                } else {
                    logger.info("No chord available for {} {}", this, getValue());
                }
            }

        } catch (Exception ex) {
            logger.warn("Error in link {} {}", this, ex.toString(), ex);
        }
    }

    //------------//
    // parseValue //
    //------------//
    /**
     * Parse the provided metronome sentence value, to populate a model.
     * <p>
     * This method is called when the user commits a modification in the Inter board.
     *
     * @param value the whole sentence value (perhaps containing music words!)
     * @param plain true for pure text, false for text and music
     * @return a model populated with logical informations, null if failed
     */
    private Model parseValue (String value,
                              boolean plain)
    {
        logger.debug("parseValue: \"{}\" [{}]", value, codesOf(value));

        final Matcher matcher = metroPattern.matcher(value);
        if (!matcher.matches()) {
            logger.debug("Not a metronome matching string: \"{}\"", value);
            return null;
        }

        final Model m = new Model();
        m.tempo = getGroup(matcher, TEMPO).trim();

        final String noteStr = getGroup(matcher, NOTE).trim();
        if (!plain) {
            // Convert string codes into note
            final Note note = Note.decode(noteStr);
            logger.debug("noteStr: \"{}\" [{}] note: {}", noteStr, codesOf(noteStr, false), note);

            if (note != null) {
                m.unit = note.toShape();
            } else {
                logger.info("No beat unit in metronome line \"{}\" str: \"{}\"", value, noteStr);
            }
        }

        // Bpm values
        m.bpmText = getGroup(matcher, BPM_TEXT).trim();

        // BPM1
        final String bpm1Str = getGroup(matcher, BPM1);
        try {
            m.bpm1 = Integer.decode(bpm1Str);
            logger.debug("bpm1Str: \"{}\" bpm1: {}", bpm1Str, m.bpm1);
        } catch (NumberFormatException ex) {
            logger.info("Invalid bpm in metronome portion: \"{}\"", bpm1Str);
        }

        // BPM2?
        final String bpm2Str = getGroup(matcher, BPM2);
        if (!bpm2Str.isEmpty()) {
            try {
                m.bpm2 = Integer.decode(bpm2Str);
                logger.debug("bpm2Str: \"{}\" bpm2: {}", bpm2Str, m.bpm2);
            } catch (NumberFormatException ex) {
                logger.info("Invalid bpm2 in metronome portion: \"{}\"", bpm2Str);
            }
        }

        // Parentheses?
        final String parStart = getGroup(matcher, PAR_START);
        final String parStop = getGroup(matcher, PAR_STOP);
        logger.debug("parStart: \"{}\" parStop: \"{}\"", parStart, parStop);
        m.parentheses = !parStart.isEmpty() || !parStop.isEmpty();

        return m;
    }

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel,
                                          Wrapper<Inter> toPublish)
    {
        logger.debug("preAdd {}", model);
        final List<UITask> tasks = new ArrayList<>(super.preAdd(cancel, toPublish));

        // Build members from model
        final SIGraph theSig = staff.getSystem().getSig();
        final SheetStub stub = staff.getSystem().getSheet().getStub();

        final MusicFamily musicFamily = stub.getMusicFamily();
        final FontInfo musicInfo = new FontInfo(model.unitFontSize, musicFamily.getFontName());

        final TextFamily textFamily = stub.getTextFamily();
        final FontInfo textInfo = new FontInfo(model.bpmFontSize, textFamily.getFontName());

        final BeatUnitInter beatUnit = new BeatUnitInter(model.unit, 1.0);
        beatUnit.setFontInfo(musicInfo);
        final int beatAdvance = beatUnit.getAdvance();

        final TextFont textFont = new TextFont(textInfo);
        TextLayout layout = textFont.layout(" ");
        final int space = rounded(layout.getAdvance());

        final WordInter bpmWord = new WordInter(Shape.TEXT, 1.0);
        bpmWord.setValue("= " + model.bpmText);
        bpmWord.setFontInfo(textInfo);

        final int y = rounded(model.baseCenter.getY());
        beatUnit.setLocation(new Point(rounded(model.box.getX()), y));
        bpmWord.setLocation(new Point(rounded(model.box.getX() + beatAdvance + space), y));

        tasks.add(
                new AdditionTask(
                        theSig,
                        beatUnit,
                        null,
                        Arrays.asList(new Link(this, new Containment(), false))));
        tasks.add(
                new AdditionTask(
                        theSig,
                        bpmWord,
                        null,
                        Arrays.asList(new Link(this, new Containment(), false))));

        return tasks;
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final Point center = getCenter();

        if (staff == null) {
            staff = system.getStaffAtOrBelow(center);
        }

        final Point ref = new Point(staff.getAbscissa(HorizontalSide.LEFT), center.y);

        // We target the first chord in the first stack(s) of the containing system,
        // regardless of the metronome precise abscissa
        for (MeasureStack stack : system.getStacks()) {
            final AbstractChordInter chord = stack.getStandardChordBelow(ref, null);

            if (chord != null) {
                return Collections.singleton(new Link(chord, new ChordSentenceRelation(), false));
            }
        }

        return Collections.emptySet();
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Assign a new value and change all members accordingly.
     * <p>
     * This method is called when the user commits a modification in the Inter board.
     *
     * @param newValue the new value
     * @return the new member words
     */
    public List<WordInter> setValue (String newValue)
    {
        final String oldValue = getValue();

        if (newValue.equals(oldValue)) {
            logger.debug("No modification made");
            return null;
        }

        // Get logical informations
        final Model newModel = parseValue(newValue, false);

        // Complete the model with physical informations
        if (model != null) {
            newModel.baseCenter = model.baseCenter;
        }

        if (newModel.baseCenter == null) {
            // No too stupid: center abscissa and baseline of middle word
            final List<Inter> words = getMembers();
            if (!words.isEmpty()) {
                final int idx = words.size() / 2;
                final WordInter word = (WordInter) words.get(idx);
                final Point2D loc = word.getLocation();
                newModel.baseCenter = new Point2D.Double(word.getCenter().x, loc.getY());
            } else {
                newModel.baseCenter = getCenter(); // Better than nothing...
            }
        }

        final int mfs = meanFont.pointsize;
        if (model != null) {
            newModel.tempoFontSize = (model.tempoFontSize != null) ? model.tempoFontSize : mfs;
            newModel.unitFontSize = (model.unitFontSize != null) ? model.unitFontSize : mfs;
            newModel.bpmFontSize = (model.bpmFontSize != null) ? model.bpmFontSize : mfs;
        } else {
            newModel.tempoFontSize = newModel.unitFontSize = newModel.bpmFontSize = mfs;
        }

        logger.debug("newModel: {}", newModel);

        return buildNewWords(newModel, staff.getSystem());
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //----------------//
    // beatUnitRegexp //
    //----------------//
    /**
     * Build the beat unit regexp
     *
     * @return the regexp string, like: "\\x{FFFF}" for each code
     */
    private static String beatUnitRegexp ()
    {
        return MusicPane.musicCodes.stream().map(c -> "\\x{" + Integer.toHexString(c) + "}") //
                .collect(Collectors.joining());
    }

    //--------//
    // create //
    //--------//
    /**
     * Create a MetronomeInter instance from the provided text line.
     * <p>
     * Some of its items may need to be further adjusted by the end user.
     * <p>
     * The caller is responsible for SIG insertion of the created metronome inter and of its
     * member words.
     *
     * @param line   the provided text line
     * @param system the related system
     * @param quiet  quiet mode
     * @param words  (output) filled with the created member words
     * @return a metronome inter
     */
    public static MetronomeInter create (TextLine line,
                                         SystemInfo system,
                                         boolean quiet,
                                         List<WordInter> words)
    {
        final Context ctx = new Context();
        ctx.sheet = system.getSheet();
        ctx.line = line;

        final Reporter reporter = new Reporter(quiet);
        final MetronomeInter metro = new MetronomeInter(line);

        try {
            final GlyphIndex glyphIndex = ctx.sheet.getGlyphIndex();
            final String str = line.getValue();

            final Matcher matcher = metroPattern.matcher(str);
            if (!matcher.matches()) {
                reporter.info("Invalid line: " + str); // We can continue
            }

            // Index to the word that contains the '=' equal sign
            final int equalIndex = equalIndex(line);
            if (equalIndex == -1) {
                reporter.alert("No '=' character found");
            }

            // Note shape
            // We retrieve the 'characters' glyph located just before the equal sign.
            final String noteStr = getGroup(matcher, NOTE).trim();
            logger.debug("create. noteStr:\"{}\" codes[{}]", noteStr, codesOf(noteStr));

            // Perhaps the note 'characters' are in the same word as the '=' sign
            ctx.noteWord = line.getWords().get(equalIndex);
            ctx.charIndex = ctx.noteWord.getValue().indexOf(noteStr);

            if (ctx.charIndex == -1) {
                // Note not found, let's look in the word before
                ctx.noteWord = line.getWords().get(equalIndex - 1);
                ctx.charIndex = ctx.noteWord.getValue().indexOf(noteStr);
            }

            if (ctx.charIndex == -1) {
                reporter.alert("Note characters not found in line: " + line);
            }

            ctx.charCount = noteStr.length();
            ctx.noteGlyph = getNoteGlyph(glyphIndex, ctx.noteWord, ctx.charIndex, ctx.charCount);
            if (ctx.noteGlyph == null) {
                reporter.alert("No underlying glyph for note in line: " + line);
            } else {
                ctx.noteGlyph = glyphIndex.registerOriginal(ctx.noteGlyph);

                ctx.note = recognizeNote(ctx.noteGlyph, system);
                logger.debug("note: {}", ctx.note);
                if (ctx.note == null) {
                    reporter.alert("Non recognized note for glyph#" + ctx.noteGlyph.getId());
                }
            }

            // BPM1
            final String bpm1Str = getGroup(matcher, BPM1);
            try {
                Integer bpm1 = Integer.decode(bpm1Str);
                logger.debug("bpm1: {}", bpm1);
            } catch (NumberFormatException ex) {
                reporter.alert("Non recognized bpm in \"" + bpm1Str + "\"");
            }

            // BPM2
            final String bpm2Str = getGroup(matcher, BPM2);
            if (!bpm2Str.isEmpty()) {
                try {
                    Integer bpm2 = Integer.decode(bpm2Str);
                    logger.debug("bpm2: {}", bpm2);
                } catch (NumberFormatException ex) {
                    reporter.alert("Non recognized bpm2 in \"" + bpm2Str + "\"");
                }
            }
        } catch (ParsingException ignored) {}

        // Build the member words even if the metronome is still invalid
        words.addAll(buildWords(ctx));

        if (ctx.noteWord != null) {
            // To be protected against symbol competitors
            metro.freeze();
            words.forEach(w -> w.freeze());
        }

        return metro;
    }

    //---------------//
    // buildNewWords //
    //---------------//
    /**
     * Generate metronome member words from the provided new model.
     * <p>
     * Items are placed around the baseCenter reference point, separated by standard spaces.
     * Target structure is:
     *
     * <pre>
     * tempo text|(B|=|bpm text)
     * ..........S..s.s.........
     * 'S' means space (w/ tempo font)
     * 's' means space (w/ bpm font)
     * 'tempo text' is only one word, using tempo font
     * The potential "(" is separate (w/ same font as bpm)
     * 'bpm text' is only one word, perhaps ended by ")", w/ bpm font
     * </pre>
     *
     * @param m the new model
     * @return the generated words, ready for insertion
     */
    private static List<WordInter> buildNewWords (Model m,
                                                  SystemInfo system)
    {
        final SheetStub stub = system.getSheet().getStub();

        final MusicFamily musicFamily = stub.getMusicFamily();
        final TextFamily textFamily = stub.getTextFamily();

        final List<WordInter> newWords = new ArrayList<>();
        final int y = rounded(m.baseCenter.getY());
        double xMin = m.baseCenter.getX();
        double xMax = m.baseCenter.getX();

        // We start on beat unit
        if (m.unit != null) {
            final FontInfo fi = new FontInfo(m.unitFontSize, musicFamily.getFontName());
            final MusicFont f = new MusicFont(fi);
            final Note note = Note.noteOf(m.unit);
            final String str = note.getString();
            final TextLayout layout = f.layout(str);
            final Rectangle2D rect = layout.getBounds();
            final Point loc = rounded(m.baseCenter.getX() - rect.getWidth() / 2, y);
            final Rectangle box = rounded(
                    loc.x,
                    m.baseCenter.getY() + rect.getY(),
                    rect.getWidth(),
                    rect.getHeight());
            newWords.add(new BeatUnitInter(null, box, 1.0, str, f, note, loc));
            xMin = loc.x;
            xMax = loc.x + rect.getWidth();
        } else {
            // TODO: use a beat unit place-holder???
        }

        // Moving forwards from unit
        final FontInfo fiBpm = new FontInfo(m.bpmFontSize, textFamily.getFontName());
        final TextFont fBpm = new TextFont(fiBpm);

        if (m.bpmText != null) {
            xMax += fBpm.layout(" ").getAdvance();

            final String val = "= " + m.bpmText + (m.parentheses ? ")" : "");
            final TextLayout layout = fBpm.layout(val);
            final Rectangle2D rect = layout.getBounds();
            xMax += rect.getX();

            final Rectangle box = rounded(
                    xMax,
                    m.baseCenter.getY() + rect.getY(),
                    rect.getWidth(),
                    rect.getHeight());
            newWords.add(new WordInter(null, box, TEXT, 1.0, val, fiBpm, rounded(xMax, y)));
        }

        // Moving backwards from unit
        if (m.parentheses) {
            final String val = "(";
            final TextLayout layout = fBpm.layout(val);
            final Rectangle2D rect = layout.getBounds();
            xMin -= layout.getAdvance();

            final Rectangle box = rounded(
                    xMin,
                    m.baseCenter.getY() + rect.getY(),
                    rect.getWidth(),
                    rect.getHeight());
            newWords.add(new WordInter(null, box, TEXT, 1.0, val, fiBpm, rounded(xMin, y)));
        }

        if (m.tempo != null && !m.tempo.isBlank()) {
            final FontInfo fi = new FontInfo(m.tempoFontSize, textFamily.getFontName());
            final TextFont f = new TextFont(fi);
            final TextLayout layout = f.layout(m.tempo);
            final Rectangle2D rect = layout.getBounds();
            xMin -= layout.getAdvance();

            final Rectangle box = rounded(
                    xMin,
                    m.baseCenter.getY() + rect.getY(),
                    rect.getWidth(),
                    rect.getHeight());
            newWords.add(new WordInter(null, box, TEXT, 1.0, m.tempo, fi, rounded(xMin, y)));
        }

        Collections.sort(newWords, Inters.byCenterAbscissa);
        return newWords;
    }

    //------------//
    // buildWords //
    //------------//
    /**
     * Build the member words of the metronome sentence.
     *
     * @param ctx context built while running the metronome create() method
     * @return the list of created WordInter instances (perhaps including a BeatUnitInter instance)
     */
    private static List<WordInter> buildWords (Context ctx)
    {
        final List<WordInter> created = new ArrayList<>();
        final GlyphIndex glyphIndex = ctx.sheet.getGlyphIndex();

        for (TextWord word : ctx.line.getWords()) {
            if (word == ctx.noteWord) { // This is the word that contains the note
                // Stuff before note?
                if (ctx.charIndex > 0) {
                    created.add(extractText(glyphIndex, word, 0, ctx.charIndex));
                }

                // Note itself
                if (ctx.note != null) {
                    final Scale scale = ctx.sheet.getScale();
                    final MusicFamily family = ctx.sheet.getStub().getMusicFamily();
                    final MusicFont musicFont = MusicFont.getBaseFont(family, scale.getInterline());
                    final Rectangle bounds = ctx.noteGlyph.getBounds();
                    final Point2D location = LineUtil.intersectionAtX(word.getBaseline(), bounds.x);
                    created.add(
                            new BeatUnitInter(
                                    ctx.noteGlyph,
                                    bounds,
                                    1.0,
                                    ctx.note.getString(),
                                    musicFont,
                                    ctx.note,
                                    PointUtil.rounded(location)));
                }

                // Stuff after note?
                final String content = word.getValue();
                final int nextIndex = ctx.charIndex + ctx.charCount;
                if (content.length() > nextIndex) {
                    created.add(extractText(glyphIndex, word, nextIndex, content.length()));
                }
            } else { // This is just a plain word
                final WordInter wi = new WordInter(word);
                wi.setValue(wi.getValue().replace(':', '='));
                created.add(wi);
            }
        }

        return created;
    }

    //------------//
    // equalIndex //
    //------------//
    /**
     * In the input line, report the index of the text word that contains the equal sign.
     *
     * @param line the input line
     * @return the index of the "=" (or ":") word in line, or -1 if not found
     */
    private static int equalIndex (TextLine line)
    {
        final List<TextWord> words = line.getWords();

        for (int i = 0; i < words.size(); i++) {
            final String value = words.get(i).getValue();
            if (value.contains("=") || value.contains(":")) {
                return i;
            }
        }

        return -1;
    }

    //-------------//
    // extractText //
    //-------------//
    /**
     * Build a WordInter from a portion of the provided TextWord.
     *
     * @param glyphIndex the sheet index for glyphs
     * @param word       the source text word
     * @param beginIndex the beginning character index, inclusive.
     * @param endIndex   the ending character index, exclusive.
     * @return the created WordInter instance
     */
    private static WordInter extractText (GlyphIndex glyphIndex,
                                          TextWord word,
                                          int beginIndex,
                                          int endIndex)
    {
        final List<TextChar> chars = word.getChars().subList(beginIndex, endIndex);
        final Set<Glyph> parts = new LinkedHashSet<>();
        chars.forEach(c -> parts.addAll(glyphIndex.getContainedEntities(c.getBounds())));

        final Glyph glyph = glyphIndex.registerOriginal(GlyphFactory.buildGlyph(parts));
        final Rectangle bounds = glyph.getBounds();
        final Point2D location = LineUtil.intersectionAtX(word.getBaseline(), bounds.x);

        return new WordInter(
                glyph,
                glyph.getBounds(),
                Shape.TEXT,
                word.getConfidence() * Grades.intrinsicRatio,
                word.getValue().substring(beginIndex, endIndex).replace(':', '='),
                word.getFontInfo(),
                PointUtil.rounded(location));
    }

    //-------------------//
    // fullValidityCheck //
    //-------------------//
    /**
     * A debug/test tool to check a given input string.
     *
     * @param input the string to check
     * @return the match result
     */
    public static boolean fullValidityCheck (String input)
    {
        final Matcher matcher = metroPattern.matcher(input);
        System.out.println(String.format("\n\"%s\"", input));
        System.out.println(String.format(" codes[%s]", codesOf(input, true)));

        final boolean result = matcher.matches();

        if (result) {
            // Dump all groups
            for (String group : new String[] { TEMPO, PAR_START, NOTE, EQUAL, BPM_TEXT, BPM1,
                    BPM_EXT, BPM2, PAR_STOP, GARBAGE }) {
                final String str = getGroup(matcher, group);

                final String n;
                if (group.equals(NOTE)) {
                    final Note note = Note.decode(str);
                    n = (note != null) ? note.name() : "null";
                } else {
                    n = "";
                }

                System.out.println(
                        String.format("   %10s %d \"%s\" %s", group, str.length(), str, n));
            }
        } else {
            System.out.println("Not a metronome matching string.");
        }

        return result;
    }

    //--------------//
    // getNoteGlyph //
    //--------------//
    /**
     * Extract the underlying glyph of the note 'characters'.
     *
     * @param glyphIndex all sheet glyphs
     * @param noteWord   the text word that contains the note 'characters'
     * @param charIndex  the 'characters' index in the text word
     * @param length     the count of characters to extract
     * @return the note glyph
     */
    private static Glyph getNoteGlyph (GlyphIndex glyphIndex,
                                       TextWord noteWord,
                                       int charIndex,
                                       int length)
    {
        Rectangle noteBox = null;

        for (int i = 0; i < length; i++) {
            final TextChar noteChar = noteWord.getChars().get(charIndex + i);
            logger.debug("noteChar: {}", noteChar);
            final Rectangle charBox = noteChar.getBounds();

            if (noteBox == null) {
                noteBox = charBox;
            } else {
                noteBox = noteBox.union(charBox);
            }
        }

        final List<Glyph> glyphs = glyphIndex.getContainedEntities(noteBox);

        if (glyphs.isEmpty()) {
            return null;
        }

        return GlyphFactory.buildGlyph(glyphs);
    }

    //----------//
    // isLikely //
    //----------//
    /**
     * Check whether the provided text line is likely to be a metronome mark.
     *
     * @param line the text line to check
     * @return true if so
     */
    public static boolean isLikely (TextLine line)
    {
        final String str = line.getValue();

        if (logger.isDebugEnabled()) {
            fullValidityCheck(str);
        }

        final Matcher matcher = metroPattern.matcher(str);

        return matcher.matches();
    }

    //---------------//
    // recognizeNote //
    //---------------//
    /**
     * Try to recognize the provided glyph as a beat unit note symbol.
     *
     * @param noteGlyph the glyph to process
     * @param system    the related system
     * @return the note recognized, or null if failed
     */
    private static Note recognizeNote (Glyph noteGlyph,
                                       SystemInfo system)
    {
        logger.debug("Note glyph: {}", noteGlyph);

        final int evalNb = constants.maxEvaluationRank.getValue();
        final Evaluation[] evals = ShapeClassifier.getInstance().evaluate(
                noteGlyph,
                system,
                evalNb,
                0.0,
                null);

        for (int i = 0; i < evalNb; i++) {
            final Evaluation eval = evals[i];
            final Note note = noteOf(eval.shape);

            if (note != null) {
                return note;
            }
        }

        return null;
    }

    // Rounding utilities
    private static int rounded (double v)
    {
        return (int) Math.rint(v);
    }

    private static Point rounded (double x,
                                  double y)
    {
        return new Point(rounded(x), rounded(y));
    }

    private static Rectangle rounded (double x,
                                      double y,
                                      double w,
                                      double h)
    {
        return new Rectangle(rounded(x), rounded(y), rounded(w), rounded(h));
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // Reporter //
    //----------//
    private static class Reporter
    {
        final boolean quiet;

        public Reporter (boolean quiet)
        {
            this.quiet = quiet;
        }

        public void alert (String message)
            throws ParsingException
        {
            if (quiet) {
                logger.debug("Metronome. {}", message); // Meant for debugging only
            } else {
                info(message);
            }

            throw new ParsingException(message); // Stop processing
        }

        public void info (String message)
        {
            logger.info("Metronome. {}", message); // Feedback to the end user
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer maxEvaluationRank = new Constant.Integer(
                "none",
                5,
                "Maximum acceptable rank for note recognition");
    }

    //-------//
    // Model //
    //-------//
    public static class Model
            implements ObjectUIModel
    {
        // Logical informations

        public String tempo; // Such as "Adagio"

        public Shape unit; // Such as METRO_QUARTER

        public String bpmText; // Such as "ca. 140"

        public Integer bpm1; // Single (or minimum) bpm

        public Integer bpm2; // Maximum bpm

        public boolean parentheses; // Parentheses?

        // Physical informations

        public Integer tempoFontSize; // Font size for tempo

        public Integer unitFontSize; // Font size for beat unit

        public Integer bpmFontSize; // Font size for "=" and for bpm text

        public Point2D baseCenter; // Unit baseline center

        public Rectangle2D box; // Metronome global bounds (useful only when dragging)

        @Override
        public void translate (double dx,
                               double dy)
        {
            PointUtil.add(baseCenter, dx, dy);
            GeoUtil.translate2D(box, dx, dy);
        }

        @Override
        public String toString ()
        {
            return new StringBuilder("Model{") //
                    .append("tempo:\"").append(tempo).append('\"') //
                    .append(" unit:").append(unit) //
                    .append(" bpmText:\"").append(bpmText).append('\"') //
                    .append(" bpm1:").append(bpm1) //
                    .append(" bpm2:").append(bpm2) //
                    .append(" par:").append(parentheses) //
                    .append(" tempoFS:").append(tempoFontSize) //
                    .append(" unitFS:").append(unitFontSize) //
                    .append(" bpmFS:").append(bpmFontSize) //
                    .append(" baseCenter:").append(baseCenter) //
                    .append(" box:").append(box) //
                    .append('}').toString();
        }
    }

    //---------//
    // Context //
    //---------//
    private static class Context
    {
        Sheet sheet;

        TextLine line;

        TextWord noteWord;

        Integer charIndex;

        Integer charCount;

        Glyph noteGlyph;

        Note note;
    }

    //------------------//
    // ParsingException //
    //------------------//
    private static class ParsingException
            extends Exception
    {
        public ParsingException (String message)
        {
            super(message);
        }
    }
}
