//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              S h a p e D e s c r i p t o r G e o                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;

import omr.moments.GeometricMoments;

import omr.run.Orientation;

import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code ShapeDescriptorGeo} defines shape description based on Geometric moments.
 *
 * @author Hervé Bitteur
 */
public class ShapeDescriptorGeo
        implements ShapeDescription.Descriptor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Number of geometric moments used. */
    public static final int momentCount = 10;

    /** Use the 10 first geometric moments + aspect. */
    private static final int length = momentCount + 1;

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // features //
    //----------//
    @Override
    public double[] features (Glyph glyph)
    {
        double[] ins = new double[length];

        // We take all the first moments
        double[] k = glyph.getGeometricMoments().getValues();

        for (int i = 0; i < momentCount; i++) {
            ins[i] = k[i];
        }

        // We append aspect
        int i = momentCount;
        /* 10 */ ins[i++] = glyph.getAspect(Orientation.VERTICAL);

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

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "GEO";
    }

    //--------//
    // length //
    //--------//
    @Override
    public int length ()
    {
        return length;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
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
        //~ Static fields/initializers -------------------------------------------------------------

        /** Label -> Index */
        public static final Map<String, Integer> indices = new HashMap<String, Integer>();

        /** Index -> Label */
        public static final String[] labels = new String[length];

        static {
            // We take all the first moments
            for (int i = 0; i < momentCount; i++) {
                labels[i] = GeometricMoments.getLabel(i);
            }

            // We append aspect
            int i = momentCount;
            /* 10 */ labels[i++] = "aspect";

            for (int j = 0; j < labels.length; j++) {
                indices.put(labels[j], j);
            }
        }

        //~ Constructors ---------------------------------------------------------------------------
        private LabelsHolder ()
        {
        }
    }
}
