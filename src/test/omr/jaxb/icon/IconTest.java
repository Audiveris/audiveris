//----------------------------------------------------------------------------//
//                                                                            //
//                              I c o n T e s t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.icon;

import omr.Main;

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
    {
        new IconTest().play(args[0]);
    }

    //------//
    // play //
    //------//
    public void play (String fileName)
    {
        try {
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //    //-----------//
    //    // playBasic //
    //    //-----------//
    //    public void playBasic (String fileName)
    //    {
    //        try {
    //            XmlMapper  mapper = new XmlMapper(SymbolIcon.class);
    //            SymbolIcon icon = (SymbolIcon) mapper.load(new File(fileName));
    //
    //            Dumper.dump(icon);
    //
    //            mapper.store(icon, new File(fileName + ".out.xml"));
    //            System.out.println("Store done.");
    //        } catch (Exception ex) {
    //            ex.printStackTrace();
    //        }
    //    }

    //--------------//
    // testMarshall //
    //--------------//
    public void testMarshall ()
    {
        play("test/omr/jaxb/icon/icon-data.xml");
    }
}
