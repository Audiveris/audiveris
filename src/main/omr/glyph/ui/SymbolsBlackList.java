//----------------------------------------------------------------------------//
//                                                                            //
//                      S y m b o l s B l a c k L i s t                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.WellKnowns;

import omr.log.Logger;

import omr.util.BlackList;

/**
 * Class {@code SymbolsBlackList} is a special {@link BlackList} meant to
 * handle the collection of symbols as artificial glyphs.
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

    //    //-----------//
    //    // listFiles //
    //    //-----------//
    //    @Override
    //    public File[] listFiles (FileFilter filter)
    //    {
    //        if (logger.isFineEnabled()) {
    //            logger.fine("Retrieving legal symbols in " + dir);
    //        }
    //
    //        // Getting all legal files & dirs, w/ additional filter if any
    //        List<File> legals = new ArrayList<File>();
    //        File[]     files = dir.listFiles(blackFilter);
    //
    //        for (Shape shape : ShapeRange.allSymbols) {
    //            File file = new File(
    //                dir,
    //                shape.name() + GlyphRepository.SYMBOL_EXTENSION);
    //
    //            if ((Symbols.getSymbol(shape) != null) &&
    //                isLegal(file) &&
    //                ((filter == null) || filter.accept(file))) {
    //                legals.add(file);
    //            }
    //        }
    //
    //        // Return legals as an array
    //        return legals.toArray(new File[legals.size()]);
    //    }
}
