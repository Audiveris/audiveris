//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   A R T D e s c r i p t o r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

/**
 * Class {@code ArtGlyphDescriptor} defines glyph features based on ART moments plus
 * some geometric moments.
 *
 * @author Hervé Bitteur
 */
public class ArtGlyphDescriptor
        extends GlyphDescriptor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Use the ART moments + 3 GEO + weight + aspect. */
    private static final int LENGTH = ARTMoments.MOMENT_COUNT + 5;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ArtGlyphDescriptor} object.
     */
    public ArtGlyphDescriptor ()
    {
        super("art");
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
        ARTMoments moments = glyph.getARTMoments();
        double[] ins = new double[LENGTH];
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
     * <p>
     * NOTA: To be kept in sync method {@link #getFeatures}
     */
    private static class LabelsHolder
    {

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

            // We append the 3 geometric moments
            labels[i++] = "N11";
            labels[i++] = "N21";
            labels[i++] = "N12";

            // We append weight and aspect
            labels[i++] = "weight";
            labels[i++] = "aspect";
        }

        private LabelsHolder ()
        {
        }
    }
}
