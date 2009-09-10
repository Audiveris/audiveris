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
 * @author Brenton Partridge
 * @version $Id$
 */
public class JaiLoader
    extends Worker<Void>
{
    //~ Static fields/initializers ---------------------------------------------

    /** The loader itself */
    private static final JaiLoader loader = new JaiLoader();

    static {
        loader.start();
    }

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
        loader.get();
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

    //-----------//
    // construct //
    //-----------//
    @Override
    public Void construct ()
    {
        javax.media.jai.JAI.getBuildVersion();

        return null;
    }
}
