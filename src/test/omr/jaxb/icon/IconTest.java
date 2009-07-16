//----------------------------------------------------------------------------//
//                                                                            //
//                              I c o n T e s t                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.jaxb.icon;

import omr.ui.icon.IconManager;
import omr.ui.icon.SymbolIcon;

import omr.util.BaseTestCase;
import omr.util.Dumper;
import static junit.framework.Assert.*;

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

            Dumper.dump(icon);

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
        play("/soft/audiveris/src/test/omr/jaxb/icon/icon-data.xml");

        //convert();
    }
}
