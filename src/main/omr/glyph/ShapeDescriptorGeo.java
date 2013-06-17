//----------------------------------------------------------------------------//
//                                                                            //
//                    S h a p e D e s c r i p t o r G e o                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;

import omr.moments.GeometricMoments;

import omr.run.Orientation;

import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code ShapeDescriptorGeo} defines shape description based
 * on Geometric moments.
 *
 * @author Hervé Bitteur
 */
public class ShapeDescriptorGeo
        implements ShapeDescription.Descriptor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Number of geometric moments used */
    private static final int momentCount = 10;

    /** Use the 10 first geometric moments + legder + stems + aspect */
    private static final int length = momentCount + 3;

    //~ Methods ----------------------------------------------------------------
    //----------//
    // features //
    //----------//
    @Override
    public double[] features (Glyph glyph)
    {
        double[] ins = new double[length];

        // We take all the first moments
        Double[] k = glyph.getGeometricMoments()
                .getValues();

        for (int i = 0; i < momentCount; i++) {
            ins[i] = k[i];
        }

        // We append ledger presence, stem count and aspect
        int i = momentCount;
        /* 10 */ ins[i++] = ShapeDescription.boolAsDouble(glyph.isWithLedger());
        /* 11 */ ins[i++] = glyph.getStemNumber();
        /* 12 */ ins[i++] = glyph.getAspect(Orientation.VERTICAL);

        return ins;
    }

    //-----------------//
    // getFeatureIndex //
    //-----------------//
    @Override
    public int getFeatureIndex (String label)
    {
        return LabelsHolder.indices.get(label);
    }

    //------------------//
    // getFeatureLabels //
    //------------------//
    @Override
    public String[] getFeatureLabels ()
    {
        return LabelsHolder.labels;
    }

    //--------//
    // length //
    //--------//
    @Override
    public int length ()
    {
        return length;
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------------//
    // LabelsHolder //
    //--------------//
    /**
     * Descriptive strings for glyph characteristics.
     *
     * NOTA: Keep in sync method {@link #features}
     */
    private static class LabelsHolder
    {
        //~ Static fields/initializers -----------------------------------------

        /** Label -> Index */
        public static final Map<String, Integer> indices = new HashMap<>();

        /** Index -> Label */
        public static final String[] labels = new String[length];

        static {
            // We take all the first moments
            for (int i = 0; i < momentCount; i++) {
                labels[i] = GeometricMoments.getLabel(i);
            }

            // We append flags and step position
            int i = momentCount;
            /* 10 */ labels[i++] = "ledger";
            /* 11 */ labels[i++] = "stemNb";
            /* 12 */ labels[i++] = "aspect";

            ////* 13 */ labels[i++] = "pitch";

            //
            for (int j = 0; j < labels.length; j++) {
                indices.put(labels[j], j);
            }
        }

        private LabelsHolder ()
        {
        }
    }
}
