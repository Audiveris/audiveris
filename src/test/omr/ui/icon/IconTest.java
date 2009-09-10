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
//-----------------------------------------------------------------------//
//
package omr.ui.icon;

import omr.glyph.Shape;

import omr.util.BaseTestCase;
import omr.util.Dumper;
import static junit.framework.Assert.*;

import java.awt.Dimension;
import java.awt.Image;
import java.io.*;

import javax.swing.ImageIcon;

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

    //---------//
    // convert //
    //---------//
//    public void convert ()
//    {
//        // Convert icon files (raw -> xml)
//        for (Shape shape : Shape.values()) {
//            SymbolIcon icon = (SymbolIcon) shape.getIcon();
//
//            if (icon != null) {
//                System.out.println("Processing " + shape);
//                IconManager.getInstance()
//                           .storeSymbolIcon((SymbolIcon) shape.getIcon());
//            } else {
//                System.out.println("***Skipping " + shape);
//            }
//        }
//    }

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

            IconManager.getInstance()
                       .storeToXmlStream(
                icon,
                new FileOutputStream(new File(fileName + ".out.xml")));
            System.out.println("Store done.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //-----------//
    // playBasic //
    //-----------//
    public void playBasic (String fileName)
    {
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
    }

    //--------------//
    // testMarshall //
    //--------------//
    public void testMarshall ()
    {
        play("/soft/audiveris/src/test/omr/ui/icon/icon-data.xml");

        //convert();
    }
}
