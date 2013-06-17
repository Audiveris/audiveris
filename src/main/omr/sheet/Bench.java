//----------------------------------------------------------------------------//
//                                                                            //
//                                 B e n c h                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.score.ScoreBench;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Class {@code Bench} defines the general features of a bench, used 
 * by each individual {@link SheetBench} and the containing {@link
 * ScoreBench}.
 *
 * @author Hervé Bitteur
 */
public abstract class Bench
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Bench.class);

    //~ Instance fields --------------------------------------------------------
    /** The internal set of properties */
    protected final Properties props = new Properties();

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new Bench object.
     */
    public Bench ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // addProp //
    //---------//
    /**
     * This is a specific setProperty functionality, that creates unique
     * keys by appending numbered suffixes
     *
     * @param radix the provided radix (to which proper suffix will be appended)
     * @param value the property value
     */
    protected void addProp (String radix,
                            String value)
    {
        if ((value == null) || (value.length() == 0)) {
            return;
        }

        String key = null;
        int index = 0;

        do {
            key = keyOf(radix, ++index);
        } while (props.containsKey(key));

        props.setProperty(key, value);

        logger.debug("addProp key:{} value:{}", key, value);
    }

    //------------//
    // flushBench //
    //------------//
    /**
     * Flush the current content of bench to disk
     */
    protected abstract void flushBench ();

    //-------//
    // keyOf //
    //-------//
    protected String keyOf (String radix,
                            int index)
    {
        return String.format("%s.%02d", radix, index);
    }
}
