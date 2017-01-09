//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               S h a p e D e s c r i p t o r M i x                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.classifier;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.moments.ARTMoments;
import org.audiveris.omr.moments.GeometricMoments;

import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code ShapeDescriptorMix} is a descriptor that mixes ART and Geo moments.
 *
 * @author Hervé Bitteur
 */
public class ShapeDescriptorMix
        implements ShapeDescription.Descriptor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Number of orthogonal moments used. */
    private static final int artCount = ShapeDescriptorART.momentCount;

    private static final int geoCount = ShapeDescriptorGeo.momentCount;

    /** Use the ART moments + GEO moments + aspect. */
    private static final int length = artCount + geoCount + 1;

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // features //
    //----------//
    @Override
    public double[] features (Glyph glyph,
                              int interline)
    {
        double[] ins = new double[length];
        ARTMoments arts = glyph.getARTMoments();
        int i = 0;

        // We take the ART moments
        for (int p = 0; p < ARTMoments.ANGULAR; p++) {
            for (int r = 0; r < ARTMoments.RADIAL; r++) {
                if ((p != 0) || (r != 0)) {
                    ins[i++] = arts.getMoment(p, r);
                }
            }
        }

        // We append the geometric moments
        GeometricMoments geos = glyph.getGeometricMoments(interline);
        double[] values = geos.getValues();

        for (int k = 0; k < geoCount; k++) {
            ins[i++] = values[k];
        }

        // We append (vertical) aspect
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
        return "MIX";
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

            // We append all the geometric moments
            for (int k = 0; k < geoCount; k++) {
                labels[i++] = GeometricMoments.getLabel(k);
            }

            // We append aspect
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
