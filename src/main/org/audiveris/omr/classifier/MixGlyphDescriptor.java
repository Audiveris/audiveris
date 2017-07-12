//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               M i x G l y p h D e s c r i p t o r                              //
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
package org.audiveris.omr.classifier;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.moments.ARTMoments;
import org.audiveris.omr.moments.GeometricMoments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code MixGlyphDescriptor} is a glyph descriptor that mixes ART and Geometric
 * moments.
 *
 * @author Hervé Bitteur
 */
public class MixGlyphDescriptor
        extends GlyphDescriptor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MixGlyphDescriptor.class);

    /** Number of orthogonal moments used. */
    private static final int artCount = ARTMoments.MOMENT_COUNT;

    private static final int geoCount = GeoGlyphDescriptor.MOMENT_COUNT;

    /** Use the ART moments + GEO moments + aspect. */
    private static final int LENGTH = artCount + geoCount + 1;

    public MixGlyphDescriptor ()
    {
        super("mix");
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String[] getFeatureLabels ()
    {
        return LabelsHolder.labels;
    }

    @Override
    public double[] getFeatures (Glyph glyph,
                                 int interline)
    {
        double[] ins = new double[LENGTH];
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

    @Override
    public int length ()
    {
        return LENGTH;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------------//
    // LabelsHolder //
    //--------------//
    /**
     * Descriptive strings for glyph characteristics.
     *
     * NOTA: Keep in sync method {@link #getFeatures}
     */
    private static class LabelsHolder
    {
        //~ Static fields/initializers -------------------------------------------------------------

        /** Index -> Label */
        public static final String[] labels = new String[LENGTH];

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
        }

        //~ Constructors ---------------------------------------------------------------------------
        private LabelsHolder ()
        {
        }
    }
}
