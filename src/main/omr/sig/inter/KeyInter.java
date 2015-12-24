//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         K e y I n t e r                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.sheet.Staff;
import static omr.sig.inter.AbstractNoteInter.Step.*;

import omr.util.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code KeyInter} represents a key signature on a staff.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "key")
public class KeyInter
        extends AbstractInter
        implements InterEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            KeyInter.class);

    /** Standard (in G clef) pitch positions for the members of the sharp keys. */
    private static final int[] sharpPitches = new int[]{
        -4, // F - Fa
        -1, // C - Do
        -5, // G - Sol
        -2, // D - Ré
        +1, // A - La
        -3, // E - Mi
        0 //   B - Si
    };

    /** Note steps according to sharps key. */
    private static final AbstractNoteInter.Step[] sharpSteps = new AbstractNoteInter.Step[]{
        F, C, G, D, A, E, B
    };

    /** Standard(in G clef) pitch position for the members of the flat keys. */
    private static final int[] flatPitches = new int[]{
        0, //  B - Si
        -3, // E - Mi
        +1, // A - La
        -2, // D - Ré
        +2, // G - Sol
        -1, // C - Do
        +3 //  F - Fa
    };

    /** Note steps according to flats key. */
    private static final AbstractNoteInter.Step[] flatSteps = new AbstractNoteInter.Step[]{
        B, E, A, D, G, C, F
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sequence of key components. */
    @XmlElement(name = "key-alter")
    private final List<KeyAlterInter> alters;

    /** Numerical value for signature. */
    @XmlAttribute
    private final int fifths;

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
    //-------------//
    // getAlterFor //
    //-------------//
    public static int getAlterFor (AbstractNoteInter.Step step,
                                   int signature)
    {
        if (signature > 0) {
            for (int k = 0; k < signature; k++) {
                if (step == sharpSteps[k]) {
                    return 1;
                }
            }
        } else {
            for (int k = 0; k < -signature; k++) {
                if (step == flatSteps[k]) {
                    return -1;
                }
            }
        }

        return 0;
    }

    //-----------------//
    // getItemPosition //
    //-----------------//
    /**
     * Report the pitch position of the nth item, within the given clef.
     * 'n' is negative for flats and positive for sharps, and start at 1 for
     * sharps (and at -1 for flats)
     *
     * @param n        the signed index (one-based) of the desired item
     * @param clefKind the kind (G_CLEF, F_CLEF or C_CLEF) of the active clef
     * @return the pitch position of the item (sharp or flat)
     */
    public static int getItemPosition (int n,
                                       Shape clefKind)
    {
        if (clefKind == null) {
            clefKind = G_CLEF;
        }

        int stdPitch = (int) Math.rint((n >= 0) ? sharpPitches[n - 1] : flatPitches[-n - 1]);

        return stdPitch + clefToDelta(clefKind);
    }

    //---------------------//
    // getStandardPosition //
    //---------------------//
    /**
     * Compute the standard mean pitch position of the provided key
     *
     * @param k the provided key value
     * @return the corresponding standard mean pitch position
     */
    public static double getStandardPosition (int k)
    {
        if (k == 0) {
            return 0;
        }

        double sum = 0;

        if (k > 0) {
            for (int i = 0; i < k; i++) {
                sum += sharpPitches[i];
            }
        } else {
            for (int i = 0; i > k; i--) {
                sum -= flatPitches[-i];
            }
        }

        return sum / k;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-------------//
    // getAlterFor //
    //-------------//
    public int getAlterFor (AbstractNoteInter.Step step)
    {
        if (fifths > 0) {
            for (int k = 0; k < fifths; k++) {
                if (step == sharpSteps[k]) {
                    return 1;
                }
            }
        } else {
            for (int k = 0; k < -fifths; k++) {
                if (step == flatSteps[k]) {
                    return -1;
                }
            }
        }

        return 0;
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
     * @return the signature
     */
    public int getFifths ()
    {
        return fifths;
    }

    //------------//
    // getMembers //
    //------------//
    @Override
    public List<? extends Inter> getMembers ()
    {
        return alters;
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
        KeyInter inter = new KeyInter(null, 0, fifths, null);
        inter.setStaff(targetStaff);

        return inter;
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "KEY_SIG:" + fifths;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" fifths:").append(fifths);

        return sb.toString();
    }

    //-------------//
    // clefToDelta //
    //-------------//
    /**
     * Report the delta in pitch position (wrt standard G_CLEF positions)
     * according to a given clef
     *
     * @param clef the clef
     * @return the delta in pitch position
     */
    private static int clefToDelta (Shape clef)
    {
        switch (getClefKind(clef)) {
        case F_CLEF:
            return 2;

        case C_CLEF:
            return 1;

        default:
        case G_CLEF:
            return 0;
        }
    }

    //-------------//
    // getClefKind //
    //-------------//
    /**
     * Classify clefs by clef kinds, since for example the same key is
     * represented with identical pitch positions for G_CLEF,
     * G_CLEF_8VA and G_CLEF_8VB.
     *
     * @param shape the precise clef shape
     * @return the clef kind
     */
    private static Shape getClefKind (Shape shape)
    {
        switch (shape) {
        case G_CLEF:
        case G_CLEF_SMALL:
        case G_CLEF_8VA:
        case G_CLEF_8VB:
            return G_CLEF;

        case F_CLEF:
        case F_CLEF_SMALL:
        case F_CLEF_8VA:
        case F_CLEF_8VB:
            return F_CLEF;

        case C_CLEF:
            return C_CLEF;

        case PERCUSSION_CLEF:
            return PERCUSSION_CLEF;

        default:
            logger.error("No base kind defined for clef {}", shape);

            return null;
        }
    }
}
