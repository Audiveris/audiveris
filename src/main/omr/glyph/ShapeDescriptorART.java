//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              S h a p e D e s c r i p t o r A R T                               //
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

import omr.moments.ARTMoments;
import omr.moments.GeometricMoments;

import omr.run.Orientation;

import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code ShapeDescriptorART} defines shape descriptions based on ART moments.
 *
 * @author Hervé Bitteur
 */
public class ShapeDescriptorART
        implements ShapeDescription.Descriptor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Number of ART moments used. */
    public static final int momentCount = -1 + (ARTMoments.ANGULAR * ARTMoments.RADIAL);

    /** Use the ART moments + 3 GEO + weight + aspect. */
    private static final int length = momentCount + 5;

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // features //
    //----------//
    @Override
    public double[] features (Glyph glyph)
    {
        ARTMoments moments = glyph.getARTMoments();
        double[] ins = new double[length];
        int i = 0;

        // We take the orthogonal moments
        for (int p = 0; p < ARTMoments.ANGULAR; p++) {
            for (int r = 0; r < ARTMoments.RADIAL; r++) {
                if ((p != 0) || (r != 0)) {
                    ins[i++] = moments.getMoment(p, r);
                }
            }
        }

        // We append 3 geometric moments
        GeometricMoments geos = glyph.getGeometricMoments();
        ins[i++] = geos.getN11();
        ins[i++] = geos.getN21();
        ins[i++] = geos.getN12();

        // We append weight and aspect
        ins[i++] = glyph.getNormalizedWeight();
        ins[i++] = glyph.getAspect(Orientation.VERTICAL);

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
        return "ART";
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
            int i = 0;

            // We take all the ART moments
            for (int p = 0; p < ARTMoments.ANGULAR; p++) {
                for (int r = 0; r < ARTMoments.RADIAL; r++) {
                    if ((p != 0) || (r != 0)) {
                        labels[i++] = String.format("F%02d%1d", p, r);
                    }
                }
            }

            // We append the 3 geometric moments
            labels[i++] = "N11";
            labels[i++] = "N21";
            labels[i++] = "N12";

            // We append weight and aspect
            labels[i++] = "weight";
            labels[i++] = "aspect";

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
