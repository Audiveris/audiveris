package org.audiveris.omr.dws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * Class {@code Sample} Represents one unit of the output from the detection web service.
 *
 * @author Raphael Emberger
 */
public final class Symbol {

    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Symbol.class);

    //~ Instance fields ----------------------------------------------------------------------------

    private int number;
    private String symbolId;
    private String symbolDesc;
    private Point from;
    private Point to;

    //~ Constructors -------------------------------------------------------------------------------

    public Symbol(int number, String symbolId, String symbolDesc) {
        this.number = number;
        this.symbolId = symbolId;
        this.symbolDesc = symbolDesc;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getNumber //
    //-------------//

    /**
     * Symbol number.
     */
    public int getNumber() {
        return number;
    }

    //-------------//
    // getSymbolId //
    //-------------//

    /**
     * Symbol ID.
     */
    public String getSymbolId() {
        return symbolId;
    }

    //---------------//
    // getSymbolDesc //
    //---------------//

    /**
     * Description of the symbol.
     */
    public String getSymbolDesc() {
        return symbolDesc;
    }

    //---------//
    // getFrom //
    //---------//

    /**
     * First coordinate.
     */
    public Point getFrom() {
        return from;
    }

    //-------//
    // getTo //
    //-------//

    /**
     * Second coordinate.
     */
    public Point getTo() {
        return to;
    }

    //-------------------//
    // assignCoordinates //
    //-------------------//

    /**
     * Assigns arguments as coordinates to the symbol.
     *
     * @param x1 First argument.
     * @param x2 Second argument.
     * @param x3 Third argument.
     * @param x4 Fourth argument.
     */
    public void assignCoordinates(int x1, int x2, int x3, int x4) {
        from = new Point(x1, x2);
        to = new Point(x3, x4);
    }

    //----------//
    // deepCopy //
    //----------//

    /**
     * makes a deep copy of the symbol.
     *
     * @return the copied symbol.
     */
    public Symbol deepCopy() {
        Symbol symbol = new Symbol(number, symbolId, symbolDesc);
        if (from != null) {
            symbol.from = new Point(from.x, from.y);
        }
        if (to != null) {
            symbol.to = new Point(to.x, to.y);
        }
        return symbol;
    }
}
