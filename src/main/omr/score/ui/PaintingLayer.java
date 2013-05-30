//----------------------------------------------------------------------------//
//                                                                            //
//                         P a i n t i n g L a y e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

/**
 * Enum {@code PaintingLayer} defines layers to be painted
 */
public enum PaintingLayer
{

    /** Input data: image or glyphs */
    INPUT("layer-input.png"),
    /** Both input and output */
    INPUT_OUTPUT("layer-input-output.png"),
    /** Output data: score entities */
    OUTPUT("layer-output.png");

    /**
     * Creates a new PaintingLayer object.
     *
     * @param imageName name of the related image
     */
    PaintingLayer (String imageName)
    {
        this.imageName = imageName;
    }

    private final String imageName;

    public String getImageName ()
    {
        return imageName;
    }
}
