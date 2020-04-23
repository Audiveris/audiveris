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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Context} gathers the needed definitions to deal with the features
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
 * <li>The right portion contains the data needed to <i>trace</i> the sample back to the original
 * sheet and symbol.
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
    public static final int INTERLINE = 10;

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
    public abstract int getContextHeight ();

    public abstract int getContextWidth ();

    public abstract int getNumClasses ();

    public abstract int getNumPixels ();

    public abstract Class<S> getLabelClass ();

    public abstract S getLabel (int ordinal);

    public abstract S getLabel (OmrShape omrShape);

    public abstract S[] getLabels ();

    public abstract List<String> getLabelList ();

    public abstract S getNone ();

    public abstract int getMeanPatchCompressedSize ();

    //~ Methods ------------------------------------------------------------------------------------
    public int getCsv (Metadata item)
    {
        return getCsvMeta() + item.ordinal();
    }

    public int getCsvLabel ()
    {
        return getNumPixels();
    }

    public int getCsvMeta ()
    {
        return getNumPixels() + 1;
    }

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
