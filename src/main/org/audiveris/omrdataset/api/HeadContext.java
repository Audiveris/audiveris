//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      H e a d C o n t e x t                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omrdataset.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Class {@code HeadContext} defines a context specifically meant for the processing of
 * potential head symbols.
 *
 * @author Hervé Bitteur
 */
public class HeadContext
        extends Context<HeadShape>
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(HeadContext.class);

    /** Height for symbol context, in pixels: {@value}. */
    private static final int CONTEXT_HEIGHT = 21;

    /** Width for symbol context, in pixels: {@value}. */
    private static final int CONTEXT_WIDTH = 27;

    /** Number of pixels in a patch: {@value}. */
    private static final int NUM_PIXELS = CONTEXT_HEIGHT * CONTEXT_WIDTH;

    /** Number of classes handled: {@value}. */
    private static final int NUM_CLASSES = HeadShape.values().length;

    /** Maximum <b>Compressed</b> size of a patch (in its .csv.zip file). */
    private static final int MAX_PATCH_COMPRESSED_SIZE = 300;

    private static final HeadShape[] LABELS = HeadShape.values();

    // Singleton
    public static final HeadContext INSTANCE = new HeadContext();

    private static final EnumSet<OmrShape> HEAD_SPECIALS = EnumSet.of(
            OmrShape.accidentalNatural,
            OmrShape.accidentalNaturalSmall,
            OmrShape.accidentalSharp,
            OmrShape.accidentalSharpSmall,
            OmrShape.keyNatural,
            OmrShape.keySharp);

    //~ Instance fields ----------------------------------------------------------------------------
    //~ Constructors -------------------------------------------------------------------------------
    private HeadContext ()
    {

    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public int getContextHeight ()
    {
        return CONTEXT_HEIGHT;
    }

    @Override
    public int getContextWidth ()
    {
        return CONTEXT_WIDTH;
    }

    @Override
    public Class<HeadShape> getLabelClass ()
    {
        return HeadShape.class;
    }

    @Override
    public HeadShape getLabel (OmrShape omrShape)
    {
        return HeadShape.toHeadShape(omrShape);
    }

    @Override
    public HeadShape[] getLabels ()
    {
        return LABELS;
    }

    @Override
    public List<String> getLabelList ()
    {
        final List<String> list = new ArrayList<>(LABELS.length);

        for (HeadShape shape : LABELS) {
            list.add(shape.toString());
        }

        return list;
    }

    @Override
    public HeadShape getLabel (int ordinal)
    {
        return LABELS[ordinal];
    }

    @Override
    public int getMaxPatchCompressedSize ()
    {
        return MAX_PATCH_COMPRESSED_SIZE;
    }

    @Override
    public int getNumClasses ()
    {
        return NUM_CLASSES;
    }

    @Override
    public int getNumPixels ()
    {
        return NUM_PIXELS;
    }

    @Override
    public HeadShape getNone ()
    {
        return HeadShape.none;
    }

    @Override
    public String toString ()
    {
        return "HEAD";
    }

    @Override
    public List<SymbolInfo> getNoneShapes (SheetAnnotations annotations)
    {
        // For HEAD context, consider all naturals & sharps as nones
        List<SymbolInfo> specials = new ArrayList<>();

        for (SymbolInfo symbol : annotations.getOuterSymbolsLiveList()) {
            if (HEAD_SPECIALS.contains(symbol.getOmrShape())) {
                specials.add(symbol);
            }
        }

        return specials;
    }

    @Override
    public List<Point> getNoneLocations (Path sheetNones)
    {
        try {
            if (Files.exists(sheetNones)) {
                final List<Point> locations = new ArrayList<>();
                final InputStream is = Files.newInputStream(sheetNones);

                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, UTF_8))) {
                    String line;

                    while ((line = br.readLine()) != null) {
                        final String[] cols = line.split(",");
                        final int x = Integer.parseInt(cols[0]);
                        final int y = Integer.parseInt(cols[1]);
                        locations.add(new Point(x, y));
                    }
                }

                return locations;
            }
        } catch (Exception ex) {
            logger.warn("Error in getSpecialLocations {}", sheetNones, ex);
        }

        return Collections.EMPTY_LIST;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
