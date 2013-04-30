//----------------------------------------------------------------------------//
//                                                                            //
//                              U n i t N o d e                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.constant;

import org.slf4j.Logger;
/**
 * Class {@code UnitNode} represents a unit (class) in the hierarchy of
 * nodes.
 * It represents a class and can have either a Logger, a ConstantSet, or both.
 *
 * @author Hervé Bitteur
 */
public class UnitNode
    extends Node
{
    //~ Instance fields --------------------------------------------------------

    /** The contained Constant set if any */
    private ConstantSet set;

    /** The logger if any */
    private Logger logger;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // UnitNode //
    //----------//
    /**
     * Create a new UnitNode.
     * @param name the fully qualified class/unit name
     */
    public UnitNode (String name)
    {
        super(name);
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getConstantSet //
    //----------------//
    /**
     * Retrieves the ConstantSet associated to the unit (if any).
     * @return the ConstantSet instance, or null
     */
    public ConstantSet getConstantSet ()
    {
        return set;
    }

    //-----------//
    // getLogger //
    //-----------//
    /**
     * Retrieves the Logger instance associated to the unit (if any).
     * @return the Logger instance, or null
     */
    public Logger getLogger ()
    {
        return logger;
    }

    //----------------//
    // setConstantSet //
    //----------------//
    /**
     * Assigns the provided ConstantSet to this enclosing unit.
     * @param set the ConstantSet to be assigned
     */
    public void setConstantSet (ConstantSet set)
    {
        this.set = set;
    }

    //-----------//
    // setLogger //
    //-----------//
    /**
     * Assigns the provided Logger to the unit.
     * @param logger the Logger instance
     */
    public void setLogger (Logger logger)
    {
        this.logger = logger;
    }
}
