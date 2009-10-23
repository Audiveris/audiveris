//----------------------------------------------------------------------------//
//                                                                            //
//                             J a i L o a d e r                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Brenton Partridge 2000-2007. All rights reserved.           //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.log.Logger;

import java.util.concurrent.*;

/**
 * Class <code>JaiLoader</code> is designed to speed up the load time of the
 * first <code>Picture</code> by allowing the <code>JAI</code> class to be
 * preloaded. Because, in some implementations, the <code>JAI</code> class must
 * load a number of renderers, its static initialization can take a long time.
 * On the first call to any static method in this class, this initialization
 * will begin in a low-priority thread, and any call to
 * <code>ensureLoaded</code> is guaranteed to block until the initialization is
 * complete.
 *
 * @author Brenton Partridge (original version)
 * @author Herv&eacute; Bitteur (Callable/Future version)
 * @version $Id$
 */
public class JaiLoader
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(JaiLoader.class);

    /** A future which reflects whether JAI has been initialized **/
    private static final Future<Void> loading = OmrExecutors.getCachedLowExecutor()
                                                            .submit(
        new Callable<Void>() {
                public Void call ()
                    throws Exception
                {
                    javax.media.jai.JAI.getBuildVersion();

                    return null;
                }
            });


    //~ Constructors -----------------------------------------------------------

    //-----------//
    // JaiLoader //
    //-----------//
    private JaiLoader ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // ensureLoaded //
    //--------------//
    /**
     * Blocks until the JAI class has been initialized.
     * If initialization has not yet begun, begins initialization.
     *
     */
    public static void ensureLoaded ()
    {
        try {
            loading.get();
        } catch (Exception ex) {
            logger.severe("Cannot load JAI", ex);
        }
    }

    //---------//
    // preload //
    //---------//
    /**
     * On the first call, starts the initialization.
     */
    public static void preload ()
    {
        // Empty body, the purpose is just to trigger class elaboration
    }
}
