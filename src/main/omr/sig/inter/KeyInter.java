//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         K e y I n t e r                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sig.inter;

import omr.glyph.Shape;

import omr.sheet.Staff;
import static omr.sig.inter.AbstractNoteInter.Step.*;
import omr.sig.inter.ClefInter.ClefKind;
import static omr.sig.inter.ClefInter.ClefKind.*;

import omr.util.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code KeyInter} represents a key signature on a staff.
 * <p>
 * <img src="doc-files/KeySignatures.png">
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
    /** Sequence of key components. */
    @XmlElement(name = "key-alter")
    private final List<KeyAlterInter> alters;

    /** Numerical value for signature. */
    @XmlAttribute
    private int fifths;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new KeyInter object.
     *
     * @param bounds the bounding bounds
     * @param grade  the interpretation quality
     * @param fifths signature value (negative for flats, positive for sharps)
     * @param alters sequence of alteration inters
     */
    public KeyInter (Rectangle bounds,
                     double grade,
                     int fifths,
                     List<KeyAlterInter> alters)
    {
        super(null, bounds, null, grade);
        this.alters = alters;
        this.fifths = fifths;

        for (KeyAlterInter alter : alters) {
            alter.setEnsemble(this);
        }
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private KeyInter ()
    {
        super(null, null, null, null);
        this.alters = null;
        this.fifths = 0;
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

    //----------//
    // addAlter //
    //----------//
    /**
     * (method currently not used) Insert a key item in existing key signature.
     *
     * @param alter the key item to insert
     */
    public void addAlter (KeyAlterInter alter)
    {
        // Insert provided alter at proper index
        final int x = alter.getBounds().x;
        int i = 0;

        for (; i < alters.size(); i++) {
            KeyAlterInter current = alters.get(i);

            if (current.getBounds().x >= x) {
                break;
            }
        }

        alters.add(i, alter);
        alter.setEnsemble(this);
        bounds = null;
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

        return bounds;
    }

    //-----------//
    // getFifths //
    //-----------//
    /**
     * Report the integer value that describes the key signature, using range [-1..-7]
     * for flats and range [+1..+7] for sharps.
     *
     * @return the signature
     */
    public int getFifths ()
    {
        return fifths;
    }

    //--------------//
    // getItemPitch //
    //--------------//
    /**
     * Report the pitch position of the nth item, within the given clef kind.
     * 'n' is negative for flats and positive for sharps, and start at 1 for sharps (and at -1 for
     * flats)
     *
     * @param n    the signed index (one-based) of the desired item
     * @param kind the kind (TREBLE, ALTO, BASS or TENOR) of the active clef
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

        return pitches[Math.abs(n)];
    }

    //------------//
    // getMembers //
    //------------//
    @Override
    public List<? extends Inter> getMembers ()
    {
        return alters;
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
    // guessKind //
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
        KeyInter inter = new KeyInter(null, 0, getFifths(), null);
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
}
