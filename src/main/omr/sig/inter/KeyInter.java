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

import omr.sheet.Staff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.List;

/**
 * Class {@code KeyInter} represents a key signature on a staff.
 *
 * @author Hervé Bitteur
 */
public class KeyInter
        extends AbstractInter
        implements InterEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(KeyInter.class);

    /** Standard (in G clef) pitch positions for the members of the sharp keys */
    private static final int[] sharpPitches = new int[]{
        -4, // F - Fa
        -1, // C - Do
        -5, // G - Sol
        -2, // D - Ré
        +1, // A - La
        -3, // E - Mi
        0 // B - Si
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sequence of key components. */
    private final List<KeyAlterInter> alters;

    /** Numerical value for signature. */
    private final int signature;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new KeyInter object.
     *
     * @param box       the bounding box
     * @param grade     the interpretation quality
     * @param signature signature value (negative for flats, positive for sharps)
     * @param alters    sequence of alteration inters
     */
    public KeyInter (Rectangle box,
                     double grade,
                     int signature,
                     List<KeyAlterInter> alters)
    {
        super(null, box, null, grade);
        this.alters = alters;
        this.signature = signature;
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
    // getMembers //
    //------------//
    @Override
    public List<? extends Inter> getMembers ()
    {
        return alters;
    }

    //--------------//
    // getSignature //
    //--------------//
    /**
     * @return the signature
     */
    public int getSignature ()
    {
        return signature;
    }

    //-----------//
    // replicate //
    //-----------//
    /**
     * Replicate this key in a target staff.
     *
     * @param targetStaff the target staff
     * @return the replicated key, whose box may need an update
     */
    public KeyInter replicate (Staff targetStaff)
    {
        KeyInter inter = new KeyInter(null, 0, signature, null);
        inter.setStaff(targetStaff);

        return inter;
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "KEY_SIG:" + signature;
    }
}
