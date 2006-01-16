//-----------------------------------------------------------------------//
//                                                                       //
//                           B a s i c T e s t                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.jibx;

import omr.util.BaseTestCase;
import omr.util.Dumper;

import org.jibx.runtime.*;
import org.jibx.runtime.impl.*;

import junit.framework.*;
import static junit.framework.Assert.*;

import java.io.*;

public class BasicTest
    extends BaseTestCase
{
    public static void main (String... args)
    {
        new BasicTest().play(args[0]);
    }

    public void testMarshall()
    {
        play("u:/soft/audiveris/src/test/omr/jibx/basic-data.xml");
    }

    public void play (String fileName)
    {
        try {
            IBindingFactory bfact = BindingDirectory.getFactory(Waiter.class);

            IUnmarshallingContext uctx = bfact.createUnmarshallingContext();
            Waiter waiter = (Waiter) uctx.unmarshalDocument
                (new FileInputStream(fileName), null);

            Dumper.dump(waiter);

            System.out.println("\ntips:");
            for (double e : waiter.purse.tips) {
                System.out.println(e);
            }

            IMarshallingContext mctx = bfact.createMarshallingContext();
            mctx.setIndent(3);
            mctx.marshalDocument(waiter, "UTF-8", null,
                                 new FileOutputStream(fileName +".out.xml"));
        } catch (JiBXException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }
}
