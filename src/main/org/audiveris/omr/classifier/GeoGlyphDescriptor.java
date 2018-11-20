//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              G e o G l y p h D e s c r i p t o r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import org.audiveris.omr.moments.GeometricMoments;

/**
 * Class {@code GeoGlyphDescriptor} defines glyph features based on Geometric moments.
 *
 * @author Hervé Bitteur
 */
public class GeoGlyphDescriptor
        extends GlyphDescriptor
{

    /** Number of geometric moments used. */
    public static final int MOMENT_COUNT = 10;

    /** Use the 10 first geometric moments + aspect. */
    private static final int LENGTH = MOMENT_COUNT + 1;

    /**
     * Creates a new {@code GeoGlyphDescriptor} object.
     */
    public GeoGlyphDescriptor ()
    {
        super("geo");
    }

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

        // We take all the first moments
        double[] k = glyph.getGeometricMoments(interline).getValues();

        System.arraycopy(k, 0, ins, 0, MOMENT_COUNT);

        // We append aspect
        int i = MOMENT_COUNT;
        /* 10 */ ins[i++] = (double) glyph.getHeight() / glyph.getWidth();

        return ins;
    }

    @Override
    public int length ()
    {
        return LENGTH;
    }

    //--------------//
    // LabelsHolder //
    //--------------//
    /**
     * Descriptive strings for glyph characteristics.
     * <p>
     * NOTA: Keep in sync method {@link #getFeatures}
     */
    private static class LabelsHolder
    {

        /** Index -> Label */
        public static final String[] labels = new String[LENGTH];

        static {
            // We take all the first moments
            for (int i = 0; i < MOMENT_COUNT; i++) {
                labels[i] = GeometricMoments.getLabel(i);
            }

            // We append aspect
            int i = MOMENT_COUNT;
            /* 10 */ labels[i++] = "aspect";
        }

        private LabelsHolder ()
        {
        }
    }
}
