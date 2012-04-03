//----------------------------------------------------------------------------//
//                                                                            //
//                      S y m b o l s B l a c k L i s t                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.WellKnowns;

import omr.log.Logger;

import omr.util.BlackList;

/**
 * Class {@code SymbolsBlackList} is a special {@link BlackList} meant
 * to handle the collection of symbols as artificial glyphs.
 *
 * @author Hervé Bitteur
 */
public class SymbolsBlackList
    extends BlackList
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        SymbolsBlackList.class);

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // SymbolsBlackList //
    //------------------//
    /**
     * Creates a new SymbolsBlackList object.
     */
    public SymbolsBlackList ()
    {
        super(WellKnowns.SYMBOLS_FOLDER);
    }
}
