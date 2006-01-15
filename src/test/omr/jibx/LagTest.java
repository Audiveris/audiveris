//-----------------------------------------------------------------------//
//                                                                       //
//                             L a g T e s t                             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.jibx;

import omr.stick.Stick;
import omr.util.Dumper;

import omr.util.BaseTestCase;

import static junit.framework.Assert.*;
import junit.framework.*;

import org.jibx.runtime.*;
import org.jibx.runtime.impl.*;

import java.io.*;

public class LagTest
    extends BaseTestCase
{
    public static void main (String... args)
    {
        new LagTest().play(args[0]);
    }

    public void testMarshall()
    {
        play("u:/soft/audiveris/src/test/omr/jibx/lag-data.xml");
    }

    private void play (String fName)
    {
        System.out.println("LagTest. fName=" + fName);

        try {
            IBindingFactory bfact = BindingDirectory.getFactory(Stick.class);

            IUnmarshallingContext uctx = bfact.createUnmarshallingContext();
            Stick stick = (Stick) uctx.unmarshalDocument
                (new FileInputStream(fName), null);

            Dumper.dump(stick);

            // Just to check
//             omr.math.Moments m = new omr.math.Moments();
//             m.setTable(new Double[] {1d, 2d, 3d, 4d});
//             stick.setMoments(m);

            IMarshallingContext mctx = bfact.createMarshallingContext();
            mctx.setIndent(4);
            mctx.marshalDocument(stick, "UTF-8", null,
                                 new FileOutputStream(fName +".out.xml"));
        } catch (JiBXException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            System.out.println("Cannot find " + fName);
            ex.printStackTrace();
        }
    }
}
