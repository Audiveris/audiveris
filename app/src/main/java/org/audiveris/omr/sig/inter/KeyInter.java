//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         K e y I n t e r                                        //
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.Fraction;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
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
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.KeyAltersRelation;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>KeyInter</code> represents a key signature on a staff.
 * <p>
 * <img src="doc-files/KeySignatures.png" alt="Example of key signatures in different clefs">
 * <p>
 * Audiveris data model considers that within a key signature, the shape of all items
 * (SHARP, FLAT, NATURAL) must be identical.
 * <p>
 * In particular, it ignores "courtesy" NATURAL items at the beginning of a new key.
 * It only accepts NATURAL items in a 'cancel' key (made only of NATURAL items).
 * The role of such 'cancel' key is to cancel the effective key and switch to C maj / A min,
 * with no SHARP's or FLAT's.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "key")
public class KeyInter
        extends AbstractPitchedInter
        implements InterEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(KeyInter.class);

    /** Sharp pitches per clef kind. */
    public static final Map<ClefKind, int[]> SHARP_PITCHES_MAP = new EnumMap<>(ClefKind.class);
    static {
        SHARP_PITCHES_MAP.put(TREBLE, new int[] { -4, -1, -5, -2, 1, -3, 0 });
        SHARP_PITCHES_MAP.put(ALTO, new int[] { -3, 0, -4, -1, 2, -2, 1 });
        SHARP_PITCHES_MAP.put(BASS, new int[] { -2, 1, -3, 0, 3, -1, 2 });
        SHARP_PITCHES_MAP.put(TENOR, new int[] { 2, -2, 1, -3, 0, -4, -1 });
    }

    /** Sharp keys note steps. */
    private static final AbstractNoteInter.NoteStep[] SHARP_STEPS =
            new AbstractNoteInter.NoteStep[] { F, C, G, D, A, E, B };

    /** Flat pitches per clef kind. */
    public static final Map<ClefKind, int[]> FLAT_PITCHES_MAP = new EnumMap<>(ClefKind.class);
    static {
        FLAT_PITCHES_MAP.put(TREBLE, new int[] { 0, -3, 1, -2, 2, -1, 3 });
        FLAT_PITCHES_MAP.put(ALTO, new int[] { 1, -2, 2, -1, 3, 0, 4 });
        FLAT_PITCHES_MAP.put(BASS, new int[] { 2, -1, 3, 0, 4, 1, 5 });
        FLAT_PITCHES_MAP.put(TENOR, new int[] { -1, -4, 0, -3, 1, -2, 2 });
    }

    /** Flat keys note steps. */
    private static final AbstractNoteInter.NoteStep[] FLAT_STEPS =
            new AbstractNoteInter.NoteStep[] { B, E, A, D, G, C, F };

    //~ Instance fields ----------------------------------------------------------------------------

    /** Numerical value for signature. */
    @XmlAttribute(name = "fifths")
    private Integer fifths;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor needed for JAXB.
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
        super(null, null, (fifths != null) ? shapeOf(fifths) : null, grade, null, null);
        this.fifths = fifths;
    }

    /**
     * Creates a new KeyInter object.
     *
     * @param grade     the interpretation quality
     * @param fifths    signature value (negative for flats, positive for sharps,
     *                  fifths of the to-be-canceled key for naturals)
     * @param itemShape the item shape (SHARP, FLAT or NATURAL)
     */
    public KeyInter (Double grade,
                     Integer fifths,
                     Shape itemShape)
    {
        super(null, null, shapeOf(fifths, itemShape), grade, null, null);
        this.fifths = fifths;
    }

    /**
     * Creates a new KeyInter (ghost) object.
     *
     * @param grade the interpretation quality
     * @param shape the key shape
     * @see ShapeSet#Keys
     */
    public KeyInter (Double grade,
                     Shape shape)
    {
        this(grade, (shape != null) ? fifthsOf(shape) : 0);
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

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        final MeasureStack stack = sig.getSystem().getStackAt(getCenter());

        if (stack != null) {
            stack.addInter(this);
        }
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
        final KeySymbol keySymbol = (KeySymbol) getSymbolToDraw(font);

        if (keySymbol == null) {
            return false;
        }

        final ClefInter effectiveClef = getEffectiveClef(staff, getCenter());

        if (effectiveClef != null) {
            // First call, needed to get bounds of new key symbol
            super.deriveFrom(keySymbol, sheet, font, dropLocation);

            // Modify dropLocation to snap vertically on staff lines according to effective clef
            final Double y = getSnapOrdinate(keySymbol.fifths, effectiveClef.getKind());

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

    //-----------//
    // getFifths //
    //-----------//
    /**
     * Report the integer value that describes the key signature, using:
     * <ul>
     * <li>range [-1..-7] for flat signs
     * <li>range [+1..+7] for sharp signs
     * <li>0 for a KEY_CANCEL, regardless of the number of natural signs
     * </ul>
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
                        case SHARP -> {
                            if (count < 0) {
                                throw new IllegalStateException("Sharp and Flat in same Key");
                            }
                            count++;
                        }

                        case FLAT -> {
                            if (count > 0) {
                                throw new IllegalStateException("Sharp and Flat in same Key");
                            }
                            count--;
                        }

                        case NATURAL -> {}

                        default -> throw new IllegalStateException(
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

        final double theoPitch = getPosition(fifths, clefKind) - getAreaPitchOffset(shape);

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

        final Measure measure = staff.getPart().getMeasureAt(pt, staff);

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
        } else {
            final Integer fifths = getNaturalFifths(this);

            if (fifths == null) {
                logger.debug("No key before {}", this);
                return null;
            }

            return new KeyCancelSymbol(fifths, font.getMusicFamily());
        }
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
        return new StringBuilder(super.internals()) //
                .append(" fifths:").append(getFifths())//
                .toString();
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
     * Report whether this key is actually a cancel key (made of naturals).
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
        final KeyInter inter = new KeyInter(null, getFifths());
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

    //------------//
    // canPropose //
    //------------//
    /**
     * Check whether the provided inters can compose a key signature.
     * <p>
     * This method is used by the user interface, to potentially propose the building of a key,
     * based on user selected members.
     *
     * @param inters the selected inters
     * @return the populated KeyConfig, null otherwise
     */
    public static KeyConfig canPropose (Collection<Inter> inters)
    {
        final List<AlterInter> alters = new ArrayList<>();
        Shape shape = null;
        Staff staff = null;

        // Only AlterInter (thus including KeyAlterInter) instances are allowed
        for (Inter inter : inters) {
            if (inter instanceof AlterInter alter) {
                alters.add(alter);

                if (shape == null) {
                    shape = alter.getShape();
                } else if (shape != alter.getShape()) {
                    logger.debug("{} shape different from {}", inter, shape);
                    return null;
                }

                if (staff == null) {
                    staff = inter.getStaff();
                } else if (staff != inter.getStaff()) {
                    logger.debug("Different staves");
                    return null;
                }

                if (alter instanceof KeyAlterInter keyAlter) {
                    // Already part of a key?
                    final Inter ensemble = keyAlter.getEnsemble();
                    if (ensemble instanceof KeyInter) {
                        logger.debug("Already part of a key");
                        return null;
                    }
                }
            } else {
                logger.debug("{} cannot be a Key member", inter);
                return null;
            }
        }

        // Sort candidates by abscissa
        Collections.sort(alters, Inters.byFullAbscissa);

        return checkConfiguration(true, alters);
    }

    //    //-------------//
    //    // canPropose // TO BE REMOVED, replaced by checkConfiguration !!!!!!!!!!!!!!!
    //    //-------------//
    //    /**
    //     * Check whether the provided sequence of AlterInter's can compose a key signature.
    //     * <p>
    //     * All members are assumed to be of the same shape (SHARP or FLAT or NATURAL).
    //     *
    //     * @param alters the homogeneous sequence of AlterInter's, sorted by abscissa
    //     * @return the populated KeyConfig, null otherwise
    //     */
    //    public static KeyConfig canPropose (List<AlterInter> alters)
    //    {
    //        final AlterInter firstAlter = alters.get(0);
    //        final Shape shape = firstAlter.getShape();
    //        final Staff staff = firstAlter.getStaff();
    //
    //        // Check their pitches with respect to the effective clef
    //        final ClefInter clef = getEffectiveClef(staff, firstAlter.getCenter());
    //        if (clef == null) {
    //            logger.info("No effective clef before {}", firstAlter);
    //            return null;
    //        }
    //
    //        final ClefKind clefKind = clef.getKind();
    //        final int nb = alters.size();
    //
    //        final Integer fifths;
    //        final int[] pitches;
    //        if (shape == Shape.NATURAL) {
    //            fifths = getNaturalFifths(firstAlter);
    //
    //            if (fifths == null) {
    //                logger.info("No effective key before {}", firstAlter);
    //                return null;
    //            }
    //
    //            if (Math.abs(fifths) != nb) {
    //                logger.debug("{} natural(s) found vs {} expected", nb, Math.abs(fifths));
    //                return null;
    //            }
    //
    //            final Shape currentShape = fifths < 0 ? Shape.FLAT : Shape.SHARP;
    //            pitches = getPitches(clefKind, currentShape);
    //        } else {
    //            fifths = (shape == Shape.SHARP) ? nb : -nb;
    //            pitches = getPitches(clefKind, shape);
    //        }
    //
    //        final double maxPitchDiff = constants.maxPitchDiff.getValue();
    //
    //        for (int i = 0; i < nb; i++) {
    //            final AlterInter alter = alters.get(i);
    //            final double diff = Math.abs(alter.getPitch() - pitches[i]);
    //            logger.debug("{} pitchDiff: {}", alter, diff);
    //
    //            if (diff > maxPitchDiff) {
    //                logger.debug("Invalid pitch for {}", alter);
    //                return null;
    //            }
    //        }
    //
    //        return new KeyConfig(shape, fifths);
    //    }

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

    //----------//
    // fifthsOf //
    //----------//
    public static int fifthsOf (Shape shape)
    {
        return switch (shape) {
            case KEY_FLAT_7 -> -7;
            case KEY_FLAT_6 -> -6;
            case KEY_FLAT_5 -> -5;
            case KEY_FLAT_4 -> -4;
            case KEY_FLAT_3 -> -3;
            case KEY_FLAT_2 -> -2;
            case KEY_FLAT_1 -> -1;
            case KEY_CANCEL -> 0;
            case KEY_SHARP_1 -> 1;
            case KEY_SHARP_2 -> 2;
            case KEY_SHARP_3 -> 3;
            case KEY_SHARP_4 -> 4;
            case KEY_SHARP_5 -> 5;
            case KEY_SHARP_6 -> 6;
            case KEY_SHARP_7 -> 7;
            default -> throw new IllegalArgumentException("No fifths value for " + shape);
        };
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

    //------------------//
    // getEffectiveClef //
    //------------------//
    /**
     * Report the clef which is effective at this location.
     *
     * @return the effective clef found, or null
     * @param staff the related staff
     * @param point the precise point
     */
    private static ClefInter getEffectiveClef (Staff staff,
                                               Point point)
    {
        Objects.requireNonNull(staff, "Staff is null");
        Objects.requireNonNull(point, "Point is null");
        final Measure measure = staff.getPart().getMeasureAt(point, staff);

        if (measure == null) {
            logger.debug("null measure");

            return null;
        }

        return measure.getClefBefore(point, staff);
    }

    //-----------------//
    // getEffectiveKey //
    //-----------------//
    /**
     * Report the key which is effective at this location.
     *
     * @return the effective key found, or null
     * @param staff the related staff
     * @param point the precise point
     */
    private static KeyInter getEffectiveKey (Staff staff,
                                             Point point)
    {
        Objects.requireNonNull(staff, "Staff is null");
        Objects.requireNonNull(point, "Point is null");

        final Measure measure = staff.getPart().getMeasureAt(point, staff);

        if (measure == null) {
            logger.debug("null measure");

            return null;
        }

        return measure.getKeyBefore(staff);
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

    //------------------//
    // getNaturalFifths //
    //------------------//
    /**
     * For naturals key, we use the fifths of the to-be-canceled key.
     *
     * @param inter the candidate key or one of its members
     * @return the fifths value to use, or null if the to-be-canceled key could not be found
     */
    private static Integer getNaturalFifths (Inter inter)
    {
        final KeyInter currentKey = getEffectiveKey(inter.getStaff(), inter.getCenter());

        return (currentKey != null) ? currentKey.getFifths() : null;
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

    //------------------//
    // lookupCandidates //
    //------------------//
    /**
     * Look for potential KeyInter candidates in the provided system (beyond headers).
     *
     * @param system the system to inspect
     */
    public static void lookupCandidates (SystemInfo system)
    {
        final SIGraph sig = system.getSig();
        final List<Inter> sysAlters = sig.inters(AlterInter.class); // System alters
        final double maxPitchDiff = constants.maxPitchDiff.getValue();

        for (Staff staff : system.getStaves()) {
            // Skip staff header
            final StaffHeader header = staff.getHeader();
            final KeyInter headerKey = (header != null) ? header.key : null;
            final List<Inter> headerAlters = headerKey != null ? headerKey.getMembers()
                    : Collections.emptyList();

            final List<AlterInter> staffAlters = new ArrayList<>(); // Staff alters
            sysAlters.forEach(a -> {
                if (a.getStaff() == staff && !headerAlters.contains(a)) {
                    // Filter out spurious candidates via their absolute pitch
                    // TODO: we could be even more strict, using effective clef and item shape!
                    final double absPitch = Math.abs(staff.pitchPositionOf(a.getCenter()));
                    if (absPitch <= 5 + maxPitchDiff) {
                        staffAlters.add((AlterInter) a);
                    } else {
                        logger.debug("Spurious key candidate member {}", a);
                    }
                }
            });
            Collections.sort(staffAlters, Inters.byAbscissa);

            // NOTA: This method is run before the inters created during SYMBOLS step are reduced
            // Hence, we have to filter out overlapping AlterInter's
            filterOverlappingAlters(staffAlters);

            logger.debug("staff: {} alters: {}", staff.getId(), Inters.ids(staffAlters));

            for (int i = 0; i < staffAlters.size(); i++) {
                final KeyConfig keyConfig = checkConfiguration(
                        false,
                        staffAlters.subList(i, staffAlters.size()));

                if (keyConfig != null) {
                    final List<AlterInter> members = staffAlters.subList(
                            i,
                            i + Math.abs(keyConfig.fifths));
                    buildKey(keyConfig, members);
                    i += members.size() - 1;
                }
            }
        }
    }

    //--------------------//
    // checkConfiguration //
    //--------------------//
    /**
     * Check for a key configuration at the very beginning of the provided sequence of alters.
     *
     * @param isManual (input) true for user-initiated action, false for engine
     * @param alters   (input) the provided alters sequence
     * @return the key configuration detected, perhaps null
     */
    private static KeyConfig checkConfiguration (boolean isManual,
                                                 List<AlterInter> alters)
    {
        final AlterInter firstAlter = alters.get(0);
        final Shape firstShape = firstAlter.getShape();
        final Staff staff = firstAlter.getStaff();
        final SystemInfo system = staff.getSystem();
        final Scale scale = system.getSheet().getScale();
        final double maxPitchDiff = constants.maxPitchDiff.getValue();
        final double minXGapToHead = scale.toPixels(constants.minXGapToHead);
        final double maxInternalXGap = scale.toPixels(constants.maxInternalXGap);

        final Point pt = firstAlter.getCenter();
        final ClefInter clef = getEffectiveClef(staff, pt);

        if (clef == null) {
            logger.warn("No effective clef before {}", firstAlter);
            return null;
        }

        final ClefKind clefKind = clef.getKind();

        Integer cancelFifths = null; // Used only for a cancel key

        // Determine target pitches to check vertical position
        final int[] pitches;
        if (firstShape == Shape.NATURAL) {
            // Special case for Natural (cancel key): we use fifths/pitches of the effective key
            cancelFifths = getNaturalFifths(firstAlter);

            if (cancelFifths == null) {
                logger.info("No effective key before {}", firstAlter);
                return null;
            }

            final Shape currentShape = cancelFifths < 0 ? Shape.FLAT : Shape.SHARP;
            pitches = getPitches(clefKind, currentShape);
        } else {
            pitches = getPitches(clefKind, firstShape);
        }

        // Look for following signs
        // We can have several NATURAL signs followed by SHARP/FLAT signs
        final List<AlterInter> keyMembers = new ArrayList<>();
        Rectangle lastBox = null;

        for (int j = 0, jBreak = Math.min(alters.size(), 7); j < jBreak; j++) {
            final AlterInter alter = alters.get(j);

            // Check vertical location
            final double diff = Math.abs(alter.getPitch() - pitches[j]);
            if (diff > maxPitchDiff) {
                logger.debug("Non compatible pitch for {}", alter.getId());
                break;
            }

            if (j > 0) {
                // Check horizontal gap with previous sign
                final int xGap = GeoUtil.xGap(lastBox, alter.getBounds());
                if (xGap > maxInternalXGap) {
                    logger.debug("xGap from previous alter too large for {}", alter.getId());
                    break;
                }

                final Shape shape = alter.getShape();
                if (shape != firstShape) {
                    break;
                }
            }

            // Include as a member
            keyMembers.add(alter);
            lastBox = alter.getBounds();
        }

        final int nb = keyMembers.size();
        if (nb == 0) {
            return null;
        }

        if (firstShape == Shape.NATURAL) {
            final int expected = Math.abs(cancelFifths);
            if (nb < expected) {
                logger.debug("{} natural(s) found vs {} expected", nb, expected);
                return null;
            }
        }

        // In the case of an isolated alter, we check for lack of head nearby,
        // because the isolated alter might just be an accidental
        if (nb == 1) {
            final HeadInter head = firstAlter.getAlteredHead();
            if (head != null) {
                // Check horizontal gap to chord bounds
                final HeadChordInter chord = head.getChord();
                final Rectangle chordBox = chord.getHeadsBounds();
                final int xGap = GeoUtil.xGap(firstAlter.getBounds(), chordBox);
                if (xGap < minXGapToHead) {
                    logger.debug("xGap to head too small for ", firstAlter.getId());
                    return null;
                }
            }
        }

        return new KeyConfig(
                firstShape,
                (firstShape == Shape.SHARP) ? nb : (firstShape == Shape.FLAT) ? -nb : cancelFifths);
    }

    //----------//
    // buildKey //
    //----------//
    /**
     * Actually build the key signature from the provided configuration
     * and the related (AlterInter) members.
     * <p>
     * Assumption: all the needed tests have been passed beforehand by checkConfiguration().
     *
     * @param keyAlters the alters of the future key
     */
    private static void buildKey (KeyConfig keyConfig,
                                  List<AlterInter> keyAlters)
    {
        logger.debug("buildKey on {}", Inters.ids(keyAlters));
        final int size = keyAlters.size();
        final AlterInter firstAlter = keyAlters.get(0);
        final SIGraph sig = firstAlter.getSig();
        final Staff staff = firstAlter.getStaff();

        // Convert each AlterInter to KeyAlterInter
        final List<KeyAlterInter> members = new ArrayList<>();
        keyAlters.forEach(alter -> members.add(new KeyAlterInter(alter)));
        keyAlters.forEach(alter -> alter.remove());
        members.forEach(m -> sig.addVertex(m));

        // All members in a key signature support each other
        for (int i = 0; i < members.size(); i++) {
            final KeyAlterInter alter = members.get(i);

            for (KeyAlterInter sibling : members.subList(i + 1, members.size())) {
                sig.addEdge(alter, sibling, new KeyAltersRelation());
            }
        }

        members.forEach(m -> sig.computeContextualGrade(m));

        // Create key, using mean contextual grade of members
        // Compute key bounds (and thus center) before inserting it
        double grade = 0;
        final Rectangle bounds = firstAlter.getBounds();
        for (KeyAlterInter m : members) {
            grade += m.getContextualGrade();
            bounds.add(m.getBounds());
        }
        grade /= size;

        final KeyInter key = new KeyInter(grade, keyConfig.fifths, keyConfig.shape);
        key.setStaff(staff);
        key.setBounds(bounds);
        sig.addVertex(key);

        // Link members
        members.forEach(m -> sig.addEdge(key, m, new Containment()));

        logger.info("Key built {}", key);
    }

    //-------------------------//
    // filterOverlappingAlters //
    //-------------------------//
    /**
     * Filter the provided sequence of AlterInter candidates, by resolving any overlap.
     *
     * @param alters (input/output) A sequence of alters, sorted by abscissa
     */
    private static void filterOverlappingAlters (List<AlterInter> alters)
    {
        final double minOverlapIou = constants.minOverlapIou.getValue();

        for (ListIterator<AlterInter> it1 = alters.listIterator(); it1.hasNext();) {
            final int idx = it1.nextIndex();
            final AlterInter left = it1.next();
            final Rectangle leftBox = left.getBounds();

            for (ListIterator<AlterInter> it2 = alters.listIterator(idx + 1); it2.hasNext();) {
                final AlterInter right = it2.next();
                final Rectangle rightBox = right.getBounds();

                if (leftBox.x + leftBox.width <= rightBox.x) {
                    break; // Since the sequence is sorted by abscissa
                }

                if (GeoUtil.iou(leftBox, rightBox) >= minOverlapIou) {
                    // We have an overlap, we keep the best graded candidate
                    if (left.getGrade() >= right.getGrade()) {
                        it2.remove();
                    } else {
                        it1.remove();
                        break;
                    }
                }
            }
        }
    }

    //---------//
    // shapeOf //
    //---------//
    public static Shape shapeOf (int fifths)
    {
        return switch (fifths) {
            case -7 -> Shape.KEY_FLAT_7;
            case -6 -> Shape.KEY_FLAT_6;
            case -5 -> Shape.KEY_FLAT_5;
            case -4 -> Shape.KEY_FLAT_4;
            case -3 -> Shape.KEY_FLAT_3;
            case -2 -> Shape.KEY_FLAT_2;
            case -1 -> Shape.KEY_FLAT_1;
            case 0 -> Shape.KEY_CANCEL;
            case 1 -> Shape.KEY_SHARP_1;
            case 2 -> Shape.KEY_SHARP_2;
            case 3 -> Shape.KEY_SHARP_3;
            case 4 -> Shape.KEY_SHARP_4;
            case 5 -> Shape.KEY_SHARP_5;
            case 6 -> Shape.KEY_SHARP_6;
            case 7 -> Shape.KEY_SHARP_7;
            default -> throw new IllegalArgumentException("No key shape for fifths " + fifths);
        };
    }

    //---------//
    // shapeOf //
    //---------//
    public static Shape shapeOf (int fifths,
                                 Shape itemShape)
    {
        return switch (itemShape) {
            case SHARP, FLAT -> shapeOf(fifths);
            case NATURAL -> Shape.KEY_CANCEL;
            default -> throw new IllegalArgumentException(
                    "No key shape for item shape " + itemShape);
        };
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Double maxPitchDiff = new Constant.Double(
                "pitch",
                0.5,
                "Maximum acceptable difference in pitch");

        private final Fraction minXGapToHead = new Fraction(
                1.5,
                "Minimum abscissa gap between sign and a following head");

        private final Fraction maxInternalXGap = new Fraction(
                0.5,
                "Maximum abscissa gap between two key signs");

        private final Constant.Ratio minOverlapIou = new Constant.Ratio(
                0.4,
                "Minimum IOU to detect overlap between Alter candidates");
    }

    //-----------//
    // KeyConfig //
    //-----------//
    /**
     * Descriptor to allocate proper key.
     */
    public static class KeyConfig
    {
        /** The item shape to display: either SHARP, FLAT or NATURAL. */
        public Shape shape;

        /**
         * The fifths value.
         * For a cancel key (made of NATURAL items), it's the fifths value of the key to cancel.
         */
        public int fifths;

        /**
         * Create a KeyConfig object.
         *
         * @param shape  either SHARP, FLAT or NATURAL
         * @param fifths the corresponding fifths value
         */
        public KeyConfig (Shape shape,
                          int fifths)
        {
            this.shape = shape;
            this.fifths = fifths;
        }

        @Override
        public String toString ()
        {
            return new StringBuilder("KeyConfig{") //
                    .append(shape).append(' ').append(fifths) //
                    .append('}').toString();
        }
    }
}
