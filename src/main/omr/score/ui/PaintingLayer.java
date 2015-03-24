//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   P a i n t i n g L a y e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

/**
 * Enum {@code PaintingLayer} defines layers to be painted
 */
public enum PaintingLayer
{

    /** Input: image or glyphs. */
    INPUT("layer-input.png"),
    /** Union of input and output. */
    INPUT_OUTPUT("layer-input-output.png"),
    /** Output: score entities. */
    OUTPUT("layer-output.png");

    //---------------//
    // PaintingLayer //
    //---------------//
    /**
     * Creates a new PaintingLayer object.
     *
     * @param imageName name of the related image
     */
    PaintingLayer (String imageName)
    {
        this.imageName = imageName;
    }

    /** Name of file where related icon can be read. */
    private final String imageName;

    //--------------//
    // getImageName //
    //--------------//
    public String getImageName ()
    {
        return imageName;
    }
}
