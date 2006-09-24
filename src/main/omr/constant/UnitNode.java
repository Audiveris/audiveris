//----------------------------------------------------------------------------//
//                                                                            //
//                              U n i t N o d e                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.constant;

import omr.util.Logger;

/**
 * Class <code>UnitNode</code> represents a unit (class) in the hierarchy of
 * nodes. It represents a class and can have either a Logger, a ConstantSet, or
 * both.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
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
     *
     * @param name the fully qualified class/unit name
     */
    public UnitNode (String name)
    {
        super(name);
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // setConstantSet //
    //----------------//
    /**
     * Assigns the provided ConstantSet to this enclosing unit
     *
     * @param set the ConstantSet to be assigned
     */
    void setConstantSet (ConstantSet set)
    {
        this.set = set;
    }

    //----------------//
    // getConstantSet //
    //----------------//
    /**
     * Retrieves the ConstantSet associated to the unit (if any)
     *
     * @return the ConstantSet instance, or null
     */
    ConstantSet getConstantSet ()
    {
        return set;
    }

    //-----------//
    // setLogger //
    //-----------//
    /**
     * Assigns the provided Logger to the unit
     *
     * @param logger the Logger instance
     */
    void setLogger (Logger logger)
    {
        this.logger = logger;
    }

    //-----------//
    // getLogger //
    //-----------//
    /**
     * Retrieves the Logger instance associated to the unit (if any)
     *
     * @return the Logger instance, or null
     */
    Logger getLogger ()
    {
        return logger;
    }
}
