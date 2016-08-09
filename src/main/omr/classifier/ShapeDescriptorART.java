//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              S h a p e D e s c r i p t o r A R T                               //
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
package omr.classifier;

import omr.glyph.Glyph;

import omr.moments.ARTMoments;
import omr.moments.GeometricMoments;

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
    public double[] features (Glyph glyph,
                              int interline)
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
        GeometricMoments geos = glyph.getGeometricMoments(interline);
        ins[i++] = geos.getN11();
        ins[i++] = geos.getN21();
        ins[i++] = geos.getN12();

        // We append weight and aspect
        ins[i++] = glyph.getNormalizedWeight(interline);
        ins[i++] = (double) glyph.getHeight() / glyph.getWidth();

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
