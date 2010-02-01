//----------------------------------------------------------------------------//
//                                                                            //
//                                  M a r k                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Shape;

import omr.score.common.SystemPoint;

import omr.util.Navigable;

/**
 * Class <code>Mark</code> encapsulates information to be made visible to the
 * end user on the score display in a very general way.
 *
 * @author Hervé Bitteur
 */
public class Mark
{
    //~ Enumerations -----------------------------------------------------------

    /** Position relative to an entity */
    public static enum Position {
        //~ Enumeration constant initializers ----------------------------------


        /** Mark should be horizontally located <b>before</b> the entity */
        BEFORE,
        /** Mark should be horizontally located <b>after</b> the entity */
        AFTER;
    }

    //~ Instance fields --------------------------------------------------------

    /** Containing system */
    @Navigable(false)
    private final ScoreSystem system;

    /** Precise location within system */
    private final SystemPoint location;

    /** Position of the mark symbol with respect to the mark location */
    private final Position position;

    /** The shape of the mark, should be an icon in fact TODO */
    private final Shape shape;

    /** Additional data, perhaps depending on shape for example */
    private final Object data;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Mark //
    //------//
    /**
     * Creates a new instance of Mark
     * @param system containing system
     * @param location precise locatrion wrt the containing system
     * @param position relative symbol position wrt location
     * @param shape symbol shape to be used
     * @param data related data or null
     */
    public Mark (ScoreSystem system,
                 SystemPoint location,
                 Position    position,
                 Shape       shape,
                 Object      data)
    {
        this.system = system;
        this.location = location;
        this.position = position;
        this.shape = shape;
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
    public SystemPoint getLocation ()
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

    //----------//
    // getShape //
    //----------//
    /**
     * Report the shape of the symbol to be displayed
     *
     * @return the symbol shape
     */
    public Shape getShape ()
    {
        return shape;
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
