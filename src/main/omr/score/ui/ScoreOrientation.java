//----------------------------------------------------------------------------//
//                                                                            //
//                      S c o r e O r i e n t a t i o n                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;


/**
 * Class <code>ScoreOrientation</code> defines the orientation used for systems
 * layout in the score view
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public enum ScoreOrientation {
    /** Systems displayed side by side */
    HORIZONTAL("Horizontal"),
    /** System displayed one above the other */
    VERTICAL("Vertical");
    //
    public final String description;

    //------------------//
    // ScoreOrientation //
    //------------------//
    private ScoreOrientation (String description)
    {
        this.description = description;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return description;
    }
}
