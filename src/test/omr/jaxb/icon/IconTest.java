//----------------------------------------------------------------------------//
//                                                                            //
//                              I c o n T e s t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.icon;

import omr.Main;
import omr.WellKnowns;

import omr.ui.icon.IconManager;
import omr.ui.icon.SymbolIcon;

import omr.util.BaseTestCase;

import java.io.*;

/**
 * DOCUMENT ME!
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class IconTest
    extends BaseTestCase
{
    //~ Methods ----------------------------------------------------------------

    //------//
    // main //
    //------//
    public static void main (String... args)
        throws FileNotFoundException
    {
        new IconTest().play(args[0]);
    }

    //------//
    // play //
    //------//
    public void play (String fileName)
        throws FileNotFoundException
    {
        SymbolIcon icon = IconManager.getInstance()
                                     .loadFromXmlStream(
            new FileInputStream(new File(fileName)));

        Main.dumping.dump(icon);

        if (icon.bitmap != null) {
            for (String s : icon.bitmap) {
                System.out.println(s);
            }
        }

        IconManager.getInstance()
                   .storeToXmlStream(
            icon,
            new FileOutputStream(new File(fileName + ".out.xml")));
        System.out.println("Store done.");
    }

    //--------------//
    // testMarshall //
    //--------------//
    public void testMarshall ()
        throws FileNotFoundException
    {
        play(
            new File(
                WellKnowns.HOME_FOLDER,
                "src/test/omr/jaxb/icon/icon-data.xml").getPath());
    }
}
