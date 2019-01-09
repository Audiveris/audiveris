//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S h a p e C l a s s i f i e r                                 //
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

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.util.OmrExecutors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Class {@code ShapeClassifier} points to the actual classifier instance in use.
 *
 * @author Hervé Bitteur
 */
public abstract class ShapeClassifier
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ShapeClassifier.class);

    /** A future which reflects whether instance has been initialized. */
    private static final Future<Void> loading = OmrExecutors.getCachedLowExecutor().submit(
            new Callable<Void>()
    {
        @Override
        public Void call ()
                throws Exception
        {
            try {
                logger.info("Allocating instances for ShapeClassifier...");
                ShapeClassifier.getPatchClassifier();
                //                ShapeClassifier.getInstance();
                //                ShapeClassifier.getSecondInstance();
                logger.info("ShapeClassifier instances allocated.");
            } catch (Exception ex) {
                logger.warn("Error pre-loading ShapeClassifier", ex);
                throw ex;
            }

            return null;
        }
    });

    /** Not meant to be instantiated. */
    private ShapeClassifier ()
    {
    }

    /**
     * Report the classifier instance in use.
     *
     * @return the first classifier
     */
    public static Classifier getInstance ()
    {
        logger.warn("ShapeClassifier.getInstance() is no longer operational");

        return null;

        //        if (useDeepClassifier()) {
        //            return DeepClassifier.getInstance();
        //        } else {
        //            return BasicClassifier.getInstance();
        //        }
    }

    /**
     * Report the second classifier instance in use.
     *
     * @return the second classifier
     */
    public static Classifier getSecondInstance ()
    {
        logger.warn("ShapeClassifier.getSecondInstance() is no longer operational");

        return null;

        //        if (useDeepClassifier()) {
        //            return BasicClassifier.getInstance();
        //        } else {
        //            return DeepClassifier.getInstance();
        //        }
    }

    //    /**
    //     * Report the second classifier instance in use.
    //     *
    //     * @return the second classifier
    //     */
    //    public static Classifier getSecondInstance ()
    //    {
    //        if (useDeepClassifier()) {
    //            return BasicClassifier.getInstance();
    //        } else {
    //            return DeepClassifier.getInstance();
    //        }
    //    }
    //
    //---------//
    // preload //
    //---------//
    /**
     * Empty static method, just to trigger class elaboration (and thus INSTANCE).
     */
    public static void preload ()
    {
    }

    /**
     * Tell whether we are using DeepClassifier (rather than old BasicClassifier).
     *
     * @return true for DeepClassifier, false for BasicClassifier
     */
//    public static boolean useDeepClassifier ()
//    {
//        return constants.useDeepClassifier.isSet();
//    }

    //--------------------//
    // getPatchClassifier //
    //--------------------//
    private static PatchClassifier getPatchClassifier ()
    {
        return PatchClassifier.getInstance();
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        //
        //        private final Constant.Boolean useDeepClassifier = new Constant.Boolean(
        //                false,
        //                "Should we use DeepClassifier? (rather than old BasicClassifier)");
    }
}
