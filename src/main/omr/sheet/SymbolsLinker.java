//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l s L i n k e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code SymbolsLinker} builds relations between symbols at system level.
 *
 * @author Hervé Bitteur
 */
public class SymbolsLinker
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolsLinker.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** Companion factory for symbols inters. */
    private final SymbolFactory factory;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SymbolsLinker object.
     *
     * @param system  the related system
     * @param factory the dedicated symbol factory
     */
    public SymbolsLinker (SystemInfo system,
                          SymbolFactory factory)
    {
        this.system = system;
        this.factory = factory;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // linkSymbols //
    //-------------//
    /**
     * Once all system symbols have been created, build relations between them when
     * relevant.
     */
    public void linkSymbols ()
    {
        logger.debug("System#{} linkSymbols", system.getId());
        factory.linkSymbols();
    }
}
