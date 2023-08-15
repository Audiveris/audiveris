//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         K e y I n t e r                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.header.StaffHeader;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.SIGraph;
import static org.audiveris.omr.sig.inter.AbstractNoteInter.NoteStep.A;
import static org.audiveris.omr.sig.inter.AbstractNoteInter.NoteStep.B;
import static org.audiveris.omr.sig.inter.AbstractNoteInter.NoteStep.C;
import static org.audiveris.omr.sig.inter.AbstractNoteInter.NoteStep.D;
import static org.audiveris.omr.sig.inter.AbstractNoteInter.NoteStep.E;
import static org.audiveris.omr.sig.inter.AbstractNoteInter.NoteStep.F;
import static org.audiveris.omr.sig.inter.AbstractNoteInter.NoteStep.G;
import org.audiveris.omr.sig.inter.ClefInter.ClefKind;
import static org.audiveris.omr.sig.inter.ClefInter.ClefKind.ALTO;
import static org.audiveris.omr.sig.inter.ClefInter.ClefKind.BASS;
import static org.audiveris.omr.sig.inter.ClefInter.ClefKind.TENOR;
import static org.audiveris.omr.sig.inter.ClefInter.ClefKind.TREBLE;
import org.audiveris.omr.sig.ui.HorizontalEditor;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.ui.symbol.KeyCancelSymbol;
import org.audiveris.omr.ui.symbol.KeySymbol;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>KeyInter</code> represents a key signature on a staff.
 * <p>
 * <img src="doc-files/KeySignatures.png" alt="Example of key signatures in different clefs">
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "key")
public class KeyInter
        extends AbstractPitchedInter
        implements InterEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(KeyInter.class);

    /** Sharp pitches per clef kind. */
    public static final Map<ClefKind, int[]> SHARP_PITCHES_MAP = new EnumMap<>(ClefKind.class);

    /** Flat pitches per clef kind. */
    public static final Map<ClefKind, int[]> FLAT_PITCHES_MAP = new EnumMap<>(ClefKind.class);

    /** Sharp keys note steps. */
    private static final AbstractNoteInter.NoteStep[] SHARP_STEPS = new AbstractNoteInter.NoteStep[]
    { F, C, G, D, A, E, B };

    /** Flat keys note steps. */
    private static final AbstractNoteInter.NoteStep[] FLAT_STEPS = new AbstractNoteInter.NoteStep[]
    { B, E, A, D, G, C, F };

    static {
        SHARP_PITCHES_MAP.put(TREBLE, new int[]
        { -4, -1, -5, -2, 1, -3, 0 });
        SHARP_PITCHES_MAP.put(ALTO, new int[]
        { -3, 0, -4, -1, 2, -2, 1 });
        SHARP_PITCHES_MAP.put(BASS, new int[]
        { -2, 1, -3, 0, 3, -1, 2 });
        SHARP_PITCHES_MAP.put(TENOR, new int[]
        { 2, -2, 1, -3, 0, -4, -1 });
    }

    static {
        FLAT_PITCHES_MAP.put(TREBLE, new int[]
        { 0, -3, 1, -2, 2, -1, 3 });
        FLAT_PITCHES_MAP.put(ALTO, new int[]
        { 1, -2, 2, -1, 3, 0, 4 });
        FLAT_PITCHES_MAP.put(BASS, new int[]
        { 2, -1, 3, 0, 4, 1, 5 });
        FLAT_PITCHES_MAP.put(TENOR, new int[]
        { -1, -4, 0, -3, 1, -2, 2 });
    }

    //~ Instance fields ----------------------------------------------------------------------------

    /** Numerical value for signature. */
    @XmlAttribute(name = "fifths")
    private Integer fifths;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor needed for JAXB.
     */
    private KeyInter ()
    {
        super(null, null, null, (Double) null, null, null);
    }

    /**
     * Creates a new KeyInter object.
     *
     * @param grade  the interpretation quality
     * @param fifths signature value (negative for flats, 0 for cancel, positive for sharps)
     */
    public KeyInter (Double grade,
                     Integer fifths)
    {
        super((Glyph) null, null, (fifths != null) ? shapeOf(fifths) : null, grade, null, null);
        this.fifths = fifths;
    }

    /**
     * Creates a new KeyInter (ghost) object.
     *
     * @param grade the interpretation quality
     * @param shape the key shape
     */
    public KeyInter (Double grade,
                     Shape shape)
    {
        this(grade, (shape != null) ? valueOf(shape) : 0);
        this.shape = shape;
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

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        MeasureStack stack = sig.getSystem().getStackAt(getCenter());

        if (stack != null) {
            stack.addInter(this);
        }
    }

    //-----------//
    // addMember //
    //-----------//
    @Override
    public void addMember (Inter member)
    {
        if (!(member instanceof KeyAlterInter)) {
            throw new IllegalArgumentException("Only KeyAlterInter can be added to KeyInter");
        }

        final Shape mShape = member.getShape();

        if ((mShape != Shape.SHARP) && (mShape != Shape.NATURAL) && (mShape != Shape.FLAT)) {
            throw new IllegalArgumentException("Attempt to add illegal shape in Key: " + mShape);
        }

        EnsembleHelper.addMember(this, member);
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
        // Initial call, needed to get current key bounds
        super.deriveFrom(symbol, sheet, font, dropLocation);

        // Adapt key (count/location of naturals) to current effective key in staff
        KeySymbol keySymbol = (KeySymbol) getSymbolToDraw(font);

        if (keySymbol == null) {
            return false;
        }

        final ClefInter effectiveClef = getEffectiveClef();

        if (effectiveClef != null) {
            // First call, needed to get bounds of new key symbol
            super.deriveFrom(keySymbol, sheet, font, dropLocation);

            // Modify dropLocation to snap vertically on staff lines according to effective clef
            Double y = getSnapOrdinate(keySymbol.fifths, effectiveClef.getKind());

            if (y != null) {
                dropLocation.y = (int) Math.rint(y);

                // Second call, to update KeyInter ghost using refined location
                super.deriveFrom(keySymbol, sheet, font, dropLocation);
            }
        }

        return true;
    }

    //-------------//
    // getAlterFor //
    //-------------//
    /**
     * Report the alteration to apply to the provided note step, under this key.
     *
     * @param step note step
     * @return the key-based alteration (either -1, 0 or +1)
     */
    public int getAlterFor (AbstractNoteInter.NoteStep step)
    {
        return getAlterFor(step, getFifths());
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            bounds = Entities.getBounds(getMembers());
        }

        if (bounds == null) {
            return null;
        }

        return new Rectangle(bounds);
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new HorizontalEditor(this);
    }

    //------------------//
    // getEffectiveClef //
    //------------------//
    /**
     * Report the clef which is effective at this KeyInter location.
     *
     * @return the effective clef found, or null
     */
    private ClefInter getEffectiveClef ()
    {
        if (staff == null) {
            logger.debug("null staff");

            return null;
        }

        final Point pt = getCenter();

        if (pt == null) {
            logger.debug("null pt");

            return null;
        }

        Measure measure = staff.getPart().getMeasureAt(pt, staff);

        if (measure == null) {
            logger.debug("null measure");

            return null;
        }

        return measure.getClefBefore(getCenter(), staff);
    }

    //-----------//
    // getFifths //
    //-----------//
    /**
     * Report the integer value that describes the key signature, using range [-1..-7]
     * for flats and range [+1..+7] for sharps, 0 for a KEY_CANCEL.
     *
     * @return the signature
     */
    public Integer getFifths ()
    {
        if ((fifths == null) && !isCancel()) {
            if ((sig != null) && sig.containsVertex(this)) {
                final List<Inter> alters = getMembers();
                int count = 0;

                for (Inter alter : alters) {
                    switch (alter.getShape()) {
                    case SHARP:
                        if (count < 0) {
                            throw new IllegalStateException("Sharp and Flat in same Key");
                        }

                        count++;

                        break;

                    case FLAT:
                        if (count > 0) {
                            throw new IllegalStateException("Sharp and Flat in same Key");
                        }

                        count--;

                        break;

                    case NATURAL:
                        break;

                    default:
                        throw new IllegalStateException(
                                "Illegal shape in Key: " + alter.getShape());
                    }
                }

                fifths = count;
            }
        }

        return fifths;
    }

    //------------//
    // getMembers //
    //------------//
    @Override
    public List<Inter> getMembers ()
    {
        return EnsembleHelper.getMembers(this, Inters.byCenterAbscissa);
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "KEY_SIG:" + getFifths();
    }

    //-----------------//
    // getSnapOrdinate //
    //-----------------//
    /**
     * Report the theoretical ordinate of key center so that its members are correctly
     * located on staff.
     * <p>
     * Required properties: staff, bounds, fifths, clefKind
     *
     * @param fifths   key signature
     * @param clefKind effective clef kind (TREBLE, ALTO, ...)
     * @return the proper ordinate if any, null otherwise
     */
    private Double getSnapOrdinate (int fifths,
                                    ClefKind clefKind)
    {
        if ((staff == null) || staff.isTablature()) {
            return null;
        }

        double theoPitch = getPosition(fifths, clefKind) - getAreaPitchOffset(shape);

        return staff.pitchToOrdinate(bounds.getCenterX(), theoPitch);
    }

    //-----------------//
    // getSymbolToDraw //
    //-----------------//
    /**
     * Report the KeySymbol to draw.
     * <p>
     * Constraint: we cannot insert a new key if there is already a key in the target measure.
     * <ul>
     * <li>For a standard key inter, the KeySymbol is its fifths value.
     * <li>But for a KEY_CANCEL, it's the fifths of the preceding key signature in staff
     * </ul>
     *
     * @param font the chosen music font
     * @return the fifths to actually draw, or null if could not be determined
     */
    public ShapeSymbol getSymbolToDraw (MusicFont font)
    {
        if (staff == null) {
            logger.debug("null staff");

            return null;
        }

        final Point pt = getCenter();

        if (pt == null) {
            logger.debug("null pt");

            return null;
        }

        Measure measure = staff.getPart().getMeasureAt(pt, staff);

        if (measure == null) {
            logger.debug("null measure");

            return null;
        }

        final KeyInter key = measure.getKey(staff);

        if ((key != null) && (key != this)) {
            logger.debug("measure already has {}", key);

            return null;
        }

        if (!isCancel()) {
            return shape.getFontSymbol(font).symbol;
        }

        KeyInter keyBefore = null;

        while ((keyBefore == null) || (keyBefore == this)) {
            measure = measure.getPrecedingInSystem();

            if (measure == null) {
                logger.debug("no previous measure");

                return null;
            }

            keyBefore = measure.getKeyBefore(staff);
            logger.debug("keyBefore: {}", keyBefore);
        }

        return new KeyCancelSymbol(keyBefore.getFifths(), font.getMusicFamily());
    }

    //-------------------------//
    // imposeWithinStaffLimits //
    //-------------------------//
    @Override
    public boolean imposeWithinStaffLimits ()
    {
        return true;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" fifths:").append(getFifths());

        return sb.toString();
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        bounds = null;
        fifths = null;

        setGrade(EnsembleHelper.computeMeanContextualGrade(this));
    }

    //----------//
    // isCancel //
    //----------//
    /**
     * Report whether this key is actually a cancellation key (made of naturals).
     *
     * @return true if cancel
     */
    public boolean isCancel ()
    {
        return shape == Shape.KEY_CANCEL;
    }

    //------------//
    // isEditable //
    //------------//
    @Override
    public boolean isEditable ()
    {
        return isManual(); // Only manual keys can be edited globally
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove the key from containing measure (and from staff header if any).
     *
     * @param extensive true for non-manual removals only
     */
    @Override
    public void remove (boolean extensive)
    {
        if (isRemoved()) {
            return;
        }

        // Remove from staff header if relevant
        final StaffHeader header = staff.getHeader();

        if ((header != null) && (header.key == this)) {
            header.key = null;
            staff.getSystem().updateHeadersStop();
        }

        // Remove from containing measure
        final Point center = getCenter();
        final Measure measure = staff.getPart().getMeasureAt(center);

        if (measure != null) {
            measure.removeInter(this);
        }

        super.remove(extensive);
    }

    //--------------//
    // removeMember //
    //--------------//
    @Override
    public void removeMember (Inter member)
    {
        if (!(member instanceof KeyAlterInter)) {
            throw new IllegalArgumentException("Only KeyAlterInter can be removed from Key");
        }

        EnsembleHelper.removeMember(this, member);
    }

    //-----------//
    // replicate //
    //-----------//
    /**
     * Replicate this key in a target staff.
     *
     * @param targetStaff the target staff
     * @return the replicated key, whose bounds may need an update
     */
    public KeyInter replicate (Staff targetStaff)
    {
        KeyInter inter = new KeyInter(null, getFifths());
        inter.setStaff(targetStaff);

        return inter;
    }

    //--------//
    // shrink //
    //--------//
    /**
     * Discard the last alter item in this key.
     */
    public void shrink ()
    {
        final List<Inter> alters = getMembers();

        // Discard last alter
        Inter lastAlter = alters.get(alters.size() - 1);
        lastAlter.remove();
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------//
    // createAdded //
    //-------------//
    /**
     * Create and add a new KeyInter object.
     *
     * @param staff  the containing staff
     * @param alters sequence of alteration inters
     * @return the created KeyInter
     */
    public static KeyInter createAdded (Staff staff,
                                        List<KeyAlterInter> alters)
    {
        SIGraph sig = staff.getSystem().getSig();
        KeyInter keyInter = new KeyInter(null, (Integer) null);
        keyInter.setStaff(staff);
        sig.addVertex(keyInter);

        for (Inter member : alters) {
            keyInter.addMember(member);
        }

        return keyInter;
    }

    //-------------//
    // getAlterFor //
    //-------------//
    /**
     * Report the alteration to apply to the provided note step, under the provided
     * effective key signature.
     *
     * @param step      note step
     * @param signature key signature
     * @return the key-based alteration (either -1, 0 or +1)
     */
    public static int getAlterFor (AbstractNoteInter.NoteStep step,
                                   int signature)
    {
        if (signature > 0) {
            for (int k = 0; k < signature; k++) {
                if (step == SHARP_STEPS[k]) {
                    return 1;
                }
            }
        } else {
            for (int k = 0; k < -signature; k++) {
                if (step == FLAT_STEPS[k]) {
                    return -1;
                }
            }
        }

        return 0;
    }

    //--------------//
    // getItemPitch //
    //--------------//
    /**
     * Report the pitch position of the nth item, within the given clef kind.
     * <p>
     * 'n' is negative for flats and positive for sharps. <br>
     * Legal values for sharps are: +1, +2, +3, +4, +5, +6, +7 <br>
     * While for flats they can be: -1, -2, -3, -4, -5, -6, -7
     * <p>
     * For a not-yet-defined KeyCancel, n == 0 is accepted.
     *
     * @param n    the signed index (one-based) of the desired item
     * @param kind the kind (TREBLE, ALTO, BASS or TENOR) of the effective clef.
     *             If null, TREBLE is assumed.
     * @return the pitch position of the key item (sharp or flat)
     */
    public static int getItemPitch (int n,
                                    ClefKind kind)
    {
        // Hack for key cancel icon: use sharp 1
        if (n == 0) {
            n = 1;
        }

        if (kind == null) {
            kind = TREBLE;
        }

        final Map<ClefKind, int[]> map = (n > 0) ? SHARP_PITCHES_MAP : FLAT_PITCHES_MAP;
        final int[] pitches = map.get(kind);

        return pitches[Math.abs(n) - 1];
    }

    //------------//
    // getPitches //
    //------------//
    /**
     * Report the sequence of pitches imposed by a given clef kind (TREBLE, ALTO, TENOR
     * or BASS) for a given key alteration shape (FLAT or SHARP).
     *
     * @param clefKind the kind of effective clef
     * @param shape    the desired shape for key
     * @return the sequence of pitch values
     */
    public static int[] getPitches (ClefKind clefKind,
                                    Shape shape)
    {
        return getPitchesMap(shape).get(clefKind);
    }

    //---------------//
    // getPitchesMap //
    //---------------//
    /**
     * Report the map of pitch sequences per clef kind, for a given key alteration shape.
     *
     * @param shape key alteration shape (FLAT or SHARP)
     * @return the map per clef kind
     */
    public static Map<ClefKind, int[]> getPitchesMap (Shape shape)
    {
        if (shape == Shape.SHARP) {
            return SHARP_PITCHES_MAP;
        }

        if (shape == Shape.FLAT) {
            return FLAT_PITCHES_MAP;
        }

        throw new IllegalArgumentException("Illegal key shape " + shape);
    }

    //-------------//
    // getPosition //
    //-------------//
    /**
     * Compute the mean pitch position of the global KeyInter for the provided fifths
     * value in the context of provided clefKind.
     *
     * @param fifths   fifths signature
     * @param clefKind clef kind context
     * @return the corresponding mean pitch position of the global sequence of signs
     */
    public static double getPosition (int fifths,
                                      ClefKind clefKind)
    {
        if (fifths == 0) {
            fifths = 1;
        }

        double sum = 0;

        if (fifths > 0) {
            final int[] pitches = SHARP_PITCHES_MAP.get(clefKind);

            for (int i = 0; i < fifths; i++) {
                sum += pitches[i];
            }
        } else {
            final int[] pitches = FLAT_PITCHES_MAP.get(clefKind);

            for (int i = 0; i > fifths; i--) {
                sum -= pitches[-i];
            }
        }

        return sum / fifths;
    }

    //---------------------//
    // getStandardPosition //
    //---------------------//
    /**
     * Compute the standard (TREBLE) mean pitch position of the global KeyInter for the
     * provided fifths value.
     *
     * @param fifths the provided fifths signature
     * @return the corresponding standard mean pitch position of the global sequence of signs
     */
    public static double getStandardPosition (int fifths)
    {
        return getPosition(fifths, TREBLE);
    }

    //-----------//
    // guessKind //
    //-----------//
    /**
     * Try to guess the clef kind, based only on key shape and pitches.
     * <p>
     * This method is not used.
     *
     * @param shape           the shape of key alter signs (FLAT vs SHARP)
     * @param measuredPitches precise pitches of the alter signs
     * @param results         (output) map to receive results per clef kind. Allocated if null.
     * @return the guessed clef kind
     */
    public static ClefKind guessKind (Shape shape,
                                      Double[] measuredPitches,
                                      Map<ClefKind, Double> results)
    {
        Map<ClefKind, int[]> map = (shape == Shape.FLAT) ? FLAT_PITCHES_MAP : SHARP_PITCHES_MAP;

        if (results == null) {
            results = new EnumMap<>(ClefKind.class);
        }

        ClefKind bestKind = null;
        double bestError = Double.MAX_VALUE;

        for (Map.Entry<ClefKind, int[]> entry : map.entrySet()) {
            ClefKind kind = entry.getKey();
            int[] pitches = entry.getValue();
            int count = 0;
            double error = 0;

            for (int i = 0; i < measuredPitches.length; i++) {
                Double measured = measuredPitches[i];

                if (measured != null) {
                    count++;

                    double diff = measured - pitches[i];
                    error += (diff * diff);
                }
            }

            if (count > 0) {
                error /= count;
                error = Math.sqrt(error);
                results.put(kind, error);

                if (error < bestError) {
                    bestError = error;
                    bestKind = kind;
                }
            }
        }

        logger.debug("{} results:{}", bestKind, results);

        return bestKind;
    }

    //---------//
    // shapeOf //
    //---------//
    private static Shape shapeOf (int fifths)
    {
        switch (fifths) {
        case -7:
            return Shape.KEY_FLAT_7;

        case -6:
            return Shape.KEY_FLAT_6;

        case -5:
            return Shape.KEY_FLAT_5;

        case -4:
            return Shape.KEY_FLAT_4;

        case -3:
            return Shape.KEY_FLAT_3;

        case -2:
            return Shape.KEY_FLAT_2;

        case -1:
            return Shape.KEY_FLAT_1;

        case 0:
            return Shape.KEY_CANCEL;

        case 1:
            return Shape.KEY_SHARP_1;

        case 2:
            return Shape.KEY_SHARP_2;

        case 3:
            return Shape.KEY_SHARP_3;

        case 4:
            return Shape.KEY_SHARP_4;

        case 5:
            return Shape.KEY_SHARP_5;

        case 6:
            return Shape.KEY_SHARP_6;

        case 7:
            return Shape.KEY_SHARP_7;

        default:
            throw new IllegalArgumentException("No key shape for fifths " + fifths);
        }
    }

    //---------//
    // valueOf //
    //---------//
    private static int valueOf (Shape shape)
    {
        switch (shape) {
        case KEY_FLAT_7:
            return -7;

        case KEY_FLAT_6:
            return -6;

        case KEY_FLAT_5:
            return -5;

        case KEY_FLAT_4:
            return -4;

        case KEY_FLAT_3:
            return -3;

        case KEY_FLAT_2:
            return -2;

        case KEY_FLAT_1:
            return -1;

        case KEY_CANCEL:
            return 0;

        case KEY_SHARP_1:
            return 1;

        case KEY_SHARP_2:
            return 2;

        case KEY_SHARP_3:
            return 3;

        case KEY_SHARP_4:
            return 4;

        case KEY_SHARP_5:
            return 5;

        case KEY_SHARP_6:
            return 6;

        case KEY_SHARP_7:
            return 7;

        default:
            throw new IllegalArgumentException("No fifth value for " + shape);
        }
    }
}
