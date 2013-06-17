//----------------------------------------------------------------------------//
//                                                                            //
//                      S h a p e D e s c r i p t i o n                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;

/**
 * Class {@code ShapeDescription} builds the glyphs features to be used
 * by an evaluator.
 *
 * @author Hervé Bitteur
 */
public abstract class ShapeDescription
{
    //~ Static fields/initializers ---------------------------------------------

    ///private static final Descriptor INSTANCE = new ShapeDescriptorGeo();
    private static final Descriptor INSTANCE = new ShapeDescriptorART();

    //~ Constructors -----------------------------------------------------------
    private ShapeDescription ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // boolAsDouble //
    //--------------//
    public static double boolAsDouble (boolean bool)
    {
        return bool ? 1.0 : 0.0;
    }

    //----------//
    // features //
    //----------//
    /**
     * Report the features that describe a given glyph.
     *
     * @param glyph the glyph to describe
     * @return the glyph shape features, an array of size length()
     */
    public static double[] features (Glyph glyph)
    {
        return INSTANCE.features(glyph);
    }

    //-------------------//
    // getParameterIndex //
    //-------------------//
    /**
     * Report the index of parameters for the provided label.
     *
     * @param label the provided label
     * @return the parameter index
     */
    public static int getParameterIndex (String label)
    {
        return INSTANCE.getFeatureIndex(label);
    }

    //--------------------//
    // getParameterLabels //
    //--------------------//
    /**
     * Report the parameters labels.
     *
     * @return the array of parameters labels
     */
    public static String[] getParameterLabels ()
    {
        return INSTANCE.getFeatureLabels();
    }

    //--------//
    // length //
    //--------//
    /**
     * Report the number of features handled.
     *
     * @return the number of features
     */
    public static int length ()
    {
        return INSTANCE.length();
    }

    //~ Inner Interfaces -------------------------------------------------------
    //
    //------------//
    // Descriptor //
    //------------//
    public static interface Descriptor
    {
        //~ Methods ------------------------------------------------------------

        /**
         * Key method which gathers the various features meant to
         * describe a glyph and recognize a shape.
         *
         * @param glyph the glyph to describe
         * @return the glyph shape features, an array of size length()
         */
        double[] features (Glyph glyph);

        /**
         * Report the index of features for the provided label.
         *
         * @param label the provided label
         * @return the feature index
         */
        int getFeatureIndex (String label);

        /**
         * Report the features labels.
         *
         * @return the array of feature labels
         */
        String[] getFeatureLabels ();

        /**
         * Report the number of features handled.
         *
         * @return the number of features
         */
        int length ();
    }
}
