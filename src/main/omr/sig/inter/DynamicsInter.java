//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    D y n a m i c s I n t e r                                   //
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
import omr.glyph.facets.Glyph;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code DynamicsInter} represents a dynamics indication (such as mf).
 *
 * @author Hervé Bitteur
 */
public class DynamicsInter
        extends AbstractInter
        implements StringSymbolInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Map Shape -> Signature. */
    private static final Map<Shape, String> sigs = new EnumMap<Shape, String>(Shape.class);

    static {
//        // Additional characters : m, r, s & z
//        sigs.put(Shape.DYNAMICS_CHAR_M, "m");
//        sigs.put(Shape.DYNAMICS_CHAR_R, "r");
//        sigs.put(Shape.DYNAMICS_CHAR_S, "s");
//        sigs.put(Shape.DYNAMICS_CHAR_Z, "z");
        //
        // True dynamics symbols
        sigs.put(Shape.DYNAMICS_P, "p");
        sigs.put(Shape.DYNAMICS_PP, "pp");
        sigs.put(Shape.DYNAMICS_MP, "mp");
        sigs.put(Shape.DYNAMICS_F, "f");
        sigs.put(Shape.DYNAMICS_FF, "ff");
        sigs.put(Shape.DYNAMICS_MF, "mf");
        sigs.put(Shape.DYNAMICS_FP, "fp");
        sigs.put(Shape.DYNAMICS_SFZ, "sfz");
//        sigs.put(Shape.DYNAMICS_FFF, "fff");
//        sigs.put(Shape.DYNAMICS_FZ, "fz");
//        sigs.put(Shape.DYNAMICS_PPP, "ppp");
//        sigs.put(Shape.DYNAMICS_RF, "rf");
//        sigs.put(Shape.DYNAMICS_RFZ, "rfz");
//        sigs.put(Shape.DYNAMICS_SF, "sf");
//        sigs.put(Shape.DYNAMICS_SFFZ, "sffz");
//        sigs.put(Shape.DYNAMICS_SFP, "sfp");
//        sigs.put(Shape.DYNAMICS_SFPP, "sfpp");
    }

    /** Map Signature -> Shape. */
    private static final Map<String, Shape> shapes = new HashMap<String, Shape>();

    static {
        shapes.put("p", Shape.DYNAMICS_P);
        shapes.put("pp", Shape.DYNAMICS_PP);
        shapes.put("mp", Shape.DYNAMICS_MP);
        shapes.put("f", Shape.DYNAMICS_F);
        shapes.put("ff", Shape.DYNAMICS_FF);
        shapes.put("mf", Shape.DYNAMICS_MF);
        shapes.put("fp", Shape.DYNAMICS_FP);
        shapes.put("sfz", Shape.DYNAMICS_SFZ);
//        shapes.put("fff", Shape.DYNAMICS_FFF);
//        shapes.put("fz", Shape.DYNAMICS_FZ);
//        shapes.put("ppp", Shape.DYNAMICS_PPP);
//        shapes.put("rf", Shape.DYNAMICS_RF);
//        shapes.put("rfz", Shape.DYNAMICS_RFZ);
//        shapes.put("sf", Shape.DYNAMICS_SF);
//        shapes.put("sffz", Shape.DYNAMICS_SFFZ);
//        shapes.put("sfp", Shape.DYNAMICS_SFP);
//        shapes.put("sfpp", Shape.DYNAMICS_SFPP);
    }

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new DynamicsInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public DynamicsInter (Glyph glyph,
                          Shape shape,
                          double grade)
    {
        super(glyph, null, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // getSymbolString //
    //-----------------//
    @Override
    public String getSymbolString ()
    {
        return sigs.get(shape);
    }
}
