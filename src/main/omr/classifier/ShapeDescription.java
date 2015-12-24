//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S h a p e D e s c r i p t i o n                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier;

import omr.glyph.Glyph;

/**
 * Class {@code ShapeDescription} builds the glyph features to be used by a classifier.
 *
 * @author Hervé Bitteur
 */
public abstract class ShapeDescription
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** This instance defines which descriptor is used by application. */
    private static final Descriptor INSTANCE
            // = new ShapeDescriptorGeo() //
            // = new ShapeDescriptorART() //
            = new ShapeDescriptorMix() //
            ;

    //~ Constructors -------------------------------------------------------------------------------
    private ShapeDescription ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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
     * Report the features that describe a given glyph (or shape sample).
     *
     * @param glyph     the entity to describe
     * @param interline the global sheet interline
     * @return the silhouette features, an array of size length()
     */
    public static double[] features (Glyph glyph,
                                     int interline)
    {
        return INSTANCE.features(glyph, interline);
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the name of the descriptor used.
     *
     * @return the descriptor name
     */
    public static String getName ()
    {
        return INSTANCE.getName();
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

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //
    //------------//
    // Descriptor //
    //------------//
    public static interface Descriptor
    {
        //~ Methods --------------------------------------------------------------------------------

        /**
         * Key method which gathers the various features meant to describe a glyph or a
         * shape sample and recognize a shape.
         *
         * @param glyph     the glyph (or sample) to describe
         * @param interline the global sheet interline
         * @return the glyph shape features, an array of size length()
         */
        double[] features (Glyph glyph,
                           int interline);

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
         * Report a descriptive name for this descriptor
         *
         * @return a typical name
         */
        String getName ();

        /**
         * Report the number of features handled.
         *
         * @return the number of features
         */
        int length ();
    }
}
