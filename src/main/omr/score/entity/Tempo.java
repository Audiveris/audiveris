//----------------------------------------------------------------------------//
//                                                                            //
//                                 T e m p o                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.util.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Tempo} handles the default tempo value.
 *
 * @author Hervé Bitteur
 */
public class Tempo
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Tempo.class);

    /** Default parameter. */
    public static final Param<Integer> defaultTempo = new Default();

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Integer defaultTempo = new Constant.Integer(
                "QuartersPerMn",
                120,
                "Default tempo, stated in number of quarters per minute");

    }

    //---------//
    // Default //
    //---------//
    private static class Default
            extends Param<Integer>
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public Integer getSpecific ()
        {
            return constants.defaultTempo.getValue();
        }

        @Override
        public boolean setSpecific (Integer specific)
        {
            if (!getSpecific()
                    .equals(specific)) {
                constants.defaultTempo.setValue(specific);
                logger.info("Default tempo is now {}", specific);

                return true;
            } else {
                return false;
            }
        }
    }
}
