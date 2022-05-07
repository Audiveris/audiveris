//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          C o n t e x t                                         //
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
package org.audiveris.omrdataset.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Class <b>Context</b> gathers the needed definitions to deal with the features
 * of a given sample, as used for the training of a patch classifier.
 * <p>
 * There should be several sub-classes of this one, typically HeadContext and GeneralContext,
 * each with its specific patch size and shape enum.
 * <p>
 * In particular, we describe here the precise content of one line of a feature CSV file.
 * Any such line comprises two portions:
 * <ul>
 * <li>The left portion contains just the data needed to <i>train</i> the classifier model.
 * <ol>
 * <li>The (CONTEXT_HEIGHT * CONTEXT_WIDTH) pixels gray values of the patch image centered on the
 * symbol center in the image scaled to predefined INTERLINE.
 * <li>The symbol label (shape index): 0 for none, etc
 * </ol>
 * <li>The right portion contains the metadata needed to <i>trace</i> the sample back to the
 * original sheet and symbol.
 * This information is mandatory to analyze the root sample(s) that have led to some abnormal
 * behavior in the training process.
 * <ol>
 * <li>Collection id
 * <li>Archive id, within the collection
 * <li>Sheet id, within the archive
 * <li>Symbol id, within the sheet
 * <li>Abscissa of symbol in original image
 * <li>Ordinate of symbol in original image
 * <li>Width of symbol in original image
 * <li>Height of symbol in original image
 * <li>Related interline value of symbol in original image
 * (typically 23 for ZHAW collection, 20 for MuseScore collection).
 * For sheets with varying heights of staves, this value can vary between symbols within the
 * same sheet.
 * </ol>
 * </ul>
 *
 * @author Hervé Bitteur
 *
 * @param <S> precise shape type
 */
public abstract class Context<S extends Enum>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Context.class);

    /** Predefined interline value: {@value}. */
    public static final int INTERLINE = 20;

    /** Value used for background pixel feature: {@value}. */
    public static final int BACKGROUND = 0;

    /** Value used for foreground pixel feature: {@value}. */
    public static final int FOREGROUND = 255;

    /** Metadata (all data past the label). */
    public static enum Metadata
    {
        /** True Shape, can be different from label. */
        TRUE_SHAPE,

        /** Collection ID. */
        COLLECTION,

        /** Archive ID within collection. */
        ARCHIVE,

        /** Sheet ID within archive. */
        SHEET_ID,

        /** Symbol ID within sheet. */
        SYMBOL_ID,

        /** X. */
        X,

        /** Y. */
        Y,

        /** Width. */
        WIDTH,

        /** Height. */
        HEIGHT,

        /** Interline value in original image. */
        INTERLINE,

        /** Wrong Label. */
        WRONG_LABEL;
    }

    //-------------//
    // ContextType //
    //-------------//
    public static enum ContextType
    {
        HEAD,
        GENERAL;
    }

    //~ Constructors -------------------------------------------------------------------------------
    protected Context ()
    {
    }

    //~ Abstract methods ---------------------------------------------------------------------------
    /**
     * Report the patch height used by this context.
     *
     * @return patch height
     */
    public abstract int getContextHeight ();

    /**
     * Report the patch width used by this context.
     *
     * @return patch width
     */
    public abstract int getContextWidth ();

    /**
     * Report the number of classes handled in this context.
     *
     * @return count of handled shapes
     */
    public abstract int getNumClasses ();

    /**
     * Report the number of pixels in context patch.
     *
     * @return height * width
     */
    public abstract int getNumPixels ();

    /**
     * Report the underlying enum shape class.
     *
     * @return the precise shape class
     */
    public abstract Class<S> getLabelClass ();

    /**
     * Report the enum value for provided ordinal number.
     *
     * @param ordinal provided ordinal number
     * @return the corresponding label value
     */
    public abstract S getLabel (int ordinal);

    /**
     * Report the label, if any, that correspond to the global true OmrShape.
     * <p>
     * For a given context, like HeadContext, we are interested only in head shapes, thus all other
     * OmrShape values are mapped to null.
     *
     * @param omrShape the provided true OmrShape
     * @return the label corresponding to the true OmrShape, or null
     */
    public abstract S getLabel (OmrShape omrShape);

    /**
     * Report the array of all enum label values.
     *
     * @return all label values
     */
    public abstract S[] getLabels ();

    /**
     * Report the list of all enum label values.
     *
     * @return all label values
     */
    public abstract List<String> getLabelList ();

    /**
     * Report the label value for none.
     *
     * @return none label
     */
    public abstract S getNone ();

    /**
     * Depending on patch size, report an over-estimate of the size for one sample features
     * in a zip-compressed file.
     *
     * @return maximum estimated size of one compressed line
     */
    public abstract int getMaxPatchCompressedSize ();

    /**
     * Report the symbols of some shapes ignored by this context that should be injected
     * as none symbols.
     *
     * @param annotations (input) the sheet symbols
     * @return the symbols to inject as nones
     */
    public List<SymbolInfo> getNoneShapes (SheetAnnotations annotations)
    {
        return Collections.emptyList(); // By default
    }

    /**
     * Report the locations where none symbols should be injected for this context.
     *
     * @param sheetNones (input) path to the potential file of none locations (provided by some
     *                   external tool like Audiveris OMR)
     * @return the collection of none locations
     */
    public List<Point> getNoneLocations (Path sheetNones)
    {
        return Collections.emptyList(); // By default
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report the index in CSV list of the provided metadata item.
     *
     * @param item provided metadata item
     * @return corresponding index in CSV list
     */
    public int getCsv (Metadata item)
    {
        return getCsvMeta() + item.ordinal();
    }

    /**
     * Report the index in CSV list of the label field.
     *
     * @return index of label field (right after the pixel fields)
     */
    public int getCsvLabel ()
    {
        return getNumPixels();
    }

    /**
     * Report the index in CSV list where metadata fields start.
     *
     * @return beginning of metadata fields in CSV list
     */
    public int getCsvMeta ()
    {
        return getNumPixels() + 1;
    }

    /**
     * Report whether the provided true OmrShape value is ignored in this context.
     *
     * @param omrShape the provided true OmrShape value
     * @return true if ignored
     */
    public boolean ignores (OmrShape omrShape)
    {
        return getLabel(omrShape) == null;
    }

    /**
     * Print out the labels ordinal and name.
     */
    public void printLabels ()
    {
        for (S label : getLabels()) {
            System.out.printf("%3d %s%n", label.ordinal(), label.toString());
        }
    }
}
