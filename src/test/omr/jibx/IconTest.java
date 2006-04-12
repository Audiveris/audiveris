//-----------------------------------------------------------------------//
//                                                                       //
//                            I c o n T e s t                            //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.jibx;

import omr.glyph.Shape;
import omr.ui.icon.IconManager;
import omr.ui.icon.SymbolIcon;
import omr.util.BaseTestCase;
import omr.util.Dumper;
import omr.util.XmlMapper;

import static junit.framework.Assert.*;

import java.io.*;
import java.awt.Dimension;
import java.awt.Image;
import javax.swing.ImageIcon;

public class IconTest
    extends BaseTestCase
{
    //------//
    // main //
    //------//
    public static void main (String... args)
    {
        new IconTest().play(args[0]);
    }

    //--------------//
    // testMarshall //
    //--------------//
    public void testMarshall()
    {
        play("/soft/audiveris/src/test/omr/jibx/icon-data.xml");
        //convert();
    }

    //-----------//
    // playBasic //
    //-----------//
    public void playBasic (String fileName)
    {
        try {
            XmlMapper mapper = new XmlMapper(SymbolIcon.class);
            SymbolIcon icon = (SymbolIcon) mapper.load(new File(fileName));

            Dumper.dump(icon);

            mapper.store(icon, new File(fileName +".out.xml"));
            System.out.println("Store done.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //------//
    // play //
    //------//
    public void play (String fileName)
    {
        try {
            SymbolIcon icon = IconManager.loadFromXmlStream
                (new FileInputStream(new File(fileName)));

            Dumper.dump(icon);

            IconManager.storeToXmlStream
                (icon, new FileOutputStream(new File(fileName +".out.xml")));
            System.out.println("Store done.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    //---------//
    // convert //
    //---------//
    public void convert()
    {
        // Convert icon files (raw -> xml)
        for (Shape shape : Shape.values()) {
            SymbolIcon icon = (SymbolIcon) shape.getIcon();
            if (icon != null) {
                System.out.println ("Processing " + shape);
               IconManager.storeIcon((SymbolIcon) shape.getIcon());
            } else {
                System.out.println ("***Skipping " + shape);
            }
        }
    }
}
