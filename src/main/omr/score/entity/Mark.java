//----------------------------------------------------------------------------//
//                                                                            //
//                                  M a r k                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.ui.symbol.Symbol;

import omr.util.Navigable;

import java.awt.Point;

/**
 * Class {@code Mark} encapsulates information to be made visible to the
 * end user on the score display in a very general way.
 *
 * @author Hervé Bitteur
 */
public class Mark
{
    //~ Enumerations -----------------------------------------------------------

    /** Position relative to an entity */
    public static enum Position
    {
        //~ Enumeration constant initializers ----------------------------------

        /** Mark should be horizontally located <b>before</b> the entity */
        BEFORE,
        /** Mark
         * should be horizontally located <b>after</b> the entity */
        AFTER;

    }

    //~ Instance fields --------------------------------------------------------
    /** Containing system */
    @Navigable(false)
    private final ScoreSystem system;

    /** Precise location within system */
    private final Point location;

    /** Position of the mark symbol with respect to the mark location */
    private final Position position;

    /** The symbol of the mark in the MusicFont */
    private final Symbol symbol;

    /** Additional data, perhaps depending on shape for example */
    private final Object data;

    //~ Constructors -----------------------------------------------------------
    //------//
    // Mark //
    //------//
    /**
     * Creates a new instance of Mark
     *
     * @param system   containing system
     * @param location precise locatrion wrt the containing system
     * @param position relative symbol position wrt location
     * @param symbol   MusicFont descriptor to be used
     * @param data     related data or null
     */
    public Mark (ScoreSystem system,
                 Point location,
                 Position position,
                 Symbol symbol,
                 Object data)
    {
        this.system = system;
        this.location = location;
        this.position = position;
        this.symbol = symbol;
        this.data = data;
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    /**
     * Report the related data information, or null
     *
     * @return the data information
     */
    public Object getData ()
    {
        return data;
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * Report the location (wrt containing system) of this mark
     *
     * @return the mark location
     */
    public Point getLocation ()
    {
        return location;
    }

    //-------------//
    // getPosition //
    //-------------//
    /**
     * Report relative position of symbol wrt mark location
     *
     * @return the relative position of symbol
     */
    public Position getPosition ()
    {
        return position;
    }

    //-----------//
    // getSymbol //
    //-----------//
    /**
     * Report the descriptor of the symbol to be displayed
     *
     * @return the MusicFont symbol descriptor
     */
    public Symbol getSymbol ()
    {
        return symbol;
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * Report the containing system of this mark
     *
     * @return the containing system
     */
    public ScoreSystem getSystem ()
    {
        return system;
    }
}
