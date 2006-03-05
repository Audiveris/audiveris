//-----------------------------------------------------------------------//
//                                                                       //
//                           S c o r e T e s t                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.jibx;

import omr.score.Score;
import omr.util.Dumper;
import omr.util.BaseTestCase;

import static junit.framework.Assert.*;
import junit.framework.*;

import org.jibx.runtime.*;
import org.jibx.runtime.impl.*;

import java.io.*;

public class ScoreTest
    extends BaseTestCase
{
    //------//
    // main //
    //------//
    public static void main (String... args)
    {
        new ScoreTest().play(args[0]);
    }

    //--------------//
    // testMarshall //
    //--------------//
    public void testMarshall()
    {
        play("/soft/audiveris/src/test/omr/jibx/score-data.xml");
    }

    //------//
    // play //
    //------//
    private void play (String fName)
    {
        System.out.println(getClass() + " fName=" + fName);

        try {
            IBindingFactory factory = BindingDirectory.getFactory(Score.class);

            IUnmarshallingContext uctx = factory.createUnmarshallingContext();
            Score score = (Score) uctx.unmarshalDocument
                (new FileInputStream(fName), null);

            //Dumper.dump(score);
            score.setChildrenContainer();
            score.computeChildren();
            score.dump();

            IMarshallingContext mctx = factory.createMarshallingContext();
            mctx.setIndent(4);
            mctx.marshalDocument(score, "UTF-8", null,
                                 new FileOutputStream(fName +".out.xml"));
        } catch (JiBXException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        } catch (FileNotFoundException ex) {
            System.out.println("Cannot find " + fName);
            throw new RuntimeException(ex);
        }
    }
}
