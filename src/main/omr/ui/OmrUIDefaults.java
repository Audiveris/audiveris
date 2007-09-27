//----------------------------------------------------------------------------//
//                                                                            //
//                         O m r U I D e f a u l t s                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.ui;

import javax.swing.*;

/**
 * Class <code>OmrUIDefaults</code> handles all the user interface defaults for
 * the OMR application
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class OmrUIDefaults
    extends UIDefaults
{
    //~ Static fields/initializers ---------------------------------------------

    private static volatile OmrUIDefaults INSTANCE;

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    public static OmrUIDefaults getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new OmrUIDefaults();
        }

        return INSTANCE;
    }

    //------------//
    // getKeyCode //
    //------------//
    /**
     * Report the numeric key code for a given description string 
     *
     * @param key the key description
     * @return the key code
     */
    public Integer getKeyCode (String key)
    {
        KeyStroke ks = getKeyStroke(key);

        return (ks != null) ? new Integer(ks.getKeyCode()) : null;
    }

    //--------------//
    // getKeyStroke //
    //--------------//
    /**
     * Report the keyboard action described by a given string
     *
     * @param key the key description
     * @return the keyboard action
     */
    public KeyStroke getKeyStroke (String key)
    {
        return KeyStroke.getKeyStroke(getString(key));
    }
}
