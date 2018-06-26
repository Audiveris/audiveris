//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         K e y I n t e r                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.SIGraph;
import static org.audiveris.omr.sig.inter.AbstractNoteInter.Step.*;
import org.audiveris.omr.sig.inter.ClefInter.ClefKind;
import static org.audiveris.omr.sig.inter.ClefInter.ClefKind.*;
import org.audiveris.omr.util.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code KeyInter} represents a key signature on a staff.
 * <p>
 * <img src="doc-files/KeySignatures.png" alt="Example of key signatures in different clefs">
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "key")
public class KeyInter
        extends AbstractInter
        implements InterEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(KeyInter.class);

    /** Sharp keys note steps. */
    private static final AbstractNoteInter.Step[] SHARP_STEPS = new AbstractNoteInter.Step[]{
        F, C, G, D, A, E, B
    };

    /** Sharp pitches per clef kind. */
    public static final Map<ClefKind, int[]> SHARP_PITCHES_MAP = new EnumMap<ClefKind, int[]>(
            ClefKind.class);

    static {
        SHARP_PITCHES_MAP.put(TREBLE, new int[]{-4, -1, -5, -2, 1, -3, 0});
        SHARP_PITCHES_MAP.put(ALTO, new int[]{-3, 0, -4, -1, 2, -2, 1});
        SHARP_PITCHES_MAP.put(BASS, new int[]{-2, 1, -3, 0, 3, -1, 2});
        SHARP_PITCHES_MAP.put(TENOR, new int[]{2, -2, 1, -3, 0, -4, -1});
    }

    /** Flat keys note steps. */
    private static final AbstractNoteInter.Step[] FLAT_STEPS = new AbstractNoteInter.Step[]{
        B, E, A, D, G, C, F
    };

    /** Flat pitches per clef kind. */
    public static final Map<ClefKind, int[]> FLAT_PITCHES_MAP = new EnumMap<ClefKind, int[]>(
            ClefKind.class);

    static {
        FLAT_PITCHES_MAP.put(TREBLE, new int[]{0, -3, 1, -2, 2, -1, 3});
        FLAT_PITCHES_MAP.put(ALTO, new int[]{1, -2, 2, -1, 3, 0, 4});
        FLAT_PITCHES_MAP.put(BASS, new int[]{2, -1, 3, 0, 4, 1, 5});
        FLAT_PITCHES_MAP.put(TENOR, new int[]{-1, -4, 0, -3, 1, -2, 2});
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Numerical value for signature. */
    @XmlAttribute
    private int fifths;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new KeyInter object.
     *
     * @param grade  the interpretation quality
     * @param fifths signature value (negative for flats, positive for sharps)
     */
    public KeyInter (double grade,
                     int fifths)
    {
        super((Glyph) null, null, null, grade);

        this.fifths = fifths;
    }

    /**
     * Creates a new KeyInter (ghost) object.
     *
     * @param grade the interpretation quality
     * @param shape the key shape
     */
    public KeyInter (double grade,
                     Shape shape)
    {
        this(grade, (shape != null) ? valueOf(shape) : 0);
        this.shape = shape;
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private KeyInter ()
    {
        super(null, null, null, null);
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
        double grade = 0;

        for (KeyAlterInter alter : alters) {
            grade += sig.computeContextualGrade(alter);
        }

        grade /= alters.size();

        KeyInter keyInter = new KeyInter(grade, 0);
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
     * active key signature.
     *
     * @param step      note step
     * @param signature key signature
     * @return the key-based alteration (either -1, 0 or +1)
     */
    public static int getAlterFor (AbstractNoteInter.Step step,
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

    //-------------//
    // getAlterFor //
    //-------------//
    /**
     * Report the alteration to apply to the provided note step, under this key.
     *
     * @param step note step
     * @return the key-based alteration (either -1, 0 or +1)
     */
    public int getAlterFor (AbstractNoteInter.Step step)
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

        return new Rectangle(bounds);
    }

    //-----------//
    // getFifths //
    //-----------//
    /**
     * Report the integer value that describes the key signature, using range [-1..-7]
     * for flats and range [+1..+7] for sharps.
     * We accept naturals as well.
     *
     * @return the signature
     */
    public int getFifths ()
    {
        if (fifths == 0) {
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

    //--------------//
    // getItemPitch //
    //--------------//
    /**
     * Report the pitch position of the nth item, within the given clef kind.
     * <p>
     * 'n' is negative for flats and positive for sharps. <br>
     * Legal values for sharps are: +1, +2, +3, +4, +5, +6, +7 <br>
     * While for flats they can be: -1, -2, -3, -4, -5, -6, -7
     *
     * @param n    the signed index (one-based) of the desired item
     * @param kind the kind (TREBLE, ALTO, BASS or TENOR) of the active clef.
     *             If null, TREBLE is assumed.
     * @return the pitch position of the key item (sharp or flat)
     */
    public static int getItemPitch (int n,
                                    ClefKind kind)
    {
        if (kind == null) {
            kind = TREBLE;
        }

        Map<ClefKind, int[]> map = (n > 0) ? SHARP_PITCHES_MAP : FLAT_PITCHES_MAP;
        int[] pitches = map.get(kind);

        return pitches[Math.abs(n) - 1];
    }

    //------------//
    // getMembers //
    //------------//
    @Override
    public List<Inter> getMembers ()
    {
        return EnsembleHelper.getMembers(this, Inters.byCenterAbscissa);
    }

    //------------//
    // getPitches //
    //------------//
    /**
     * Report the sequence of pitches imposed by a given clef kind (TREBLE, ALTO, TENOR
     * or BASS) for a given key alteration shape (FLAT or SHARP).
     *
     * @param clefKind the kind of active clef
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

    //---------------------//
    // getStandardPosition //
    //---------------------//
    /**
     * Compute the standard (TREBLE) mean pitch position of the provided key
     *
     * @param k the provided key signature
     * @return the corresponding standard mean pitch position of the key
     */
    public static double getStandardPosition (int k)
    {
        if (k == 0) {
            return 0;
        }

        double sum = 0;

        if (k > 0) {
            final int[] pitches = SHARP_PITCHES_MAP.get(TREBLE);

            for (int i = 0; i < k; i++) {
                sum += pitches[i];
            }
        } else {
            final int[] pitches = FLAT_PITCHES_MAP.get(TREBLE);

            for (int i = 0; i > k; i--) {
                sum -= pitches[-i];
            }
        }

        return sum / k;
    }

    //-----------//
    // guessKind // Not used!
    //-----------//
    public static ClefKind guessKind (Shape shape,
                                      Double[] measuredPitches,
                                      Map<ClefKind, Double> results)
    {
        Map<ClefKind, int[]> map = (shape == Shape.FLAT) ? FLAT_PITCHES_MAP : SHARP_PITCHES_MAP;

        if (results == null) {
            results = new EnumMap<ClefKind, Double>(ClefKind.class);
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

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        bounds = null;
        fifths = 0;
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
        KeyInter inter = new KeyInter(0, getFifths());
        inter.setStaff(targetStaff);

        return inter;
    }

    //-----------//
    // setFifths //
    //-----------//
    /**
     * (method currently not used) Adjust the signature integer value.
     *
     * @param fifths the fifths to set
     */
    public void setFifths (int fifths)
    {
        this.fifths = fifths;
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "KEY_SIG:" + getFifths();
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

    //---------//
    // valueOf //
    //---------//
    private static int valueOf (Shape shape)
    {
        switch (shape) {
        case KEY_FLAT_1:
            return -1;

        case KEY_FLAT_2:
            return -2;

        case KEY_FLAT_3:
            return -3;

        case KEY_FLAT_4:
            return -4;

        case KEY_FLAT_5:
            return -5;

        case KEY_FLAT_6:
            return -6;

        case KEY_FLAT_7:
            return -7;

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
