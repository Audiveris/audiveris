//--------------------------------------------------------------------------//
//                                                                          //
//                     S c o r e M a n a g e r T e s t                      //
//                                                                          //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.             //
//  This software is released under the terms of the GNU General Public     //
//  License. Please contact the author at herve.bitteur@laposte.net         //
//  to report bugs & suggestions.                                           //
//--------------------------------------------------------------------------//

package omr.jibx;

import omr.score.Score;
import omr.score.ScoreManager;
import omr.util.Dumper;
import omr.util.BaseTestCase;

import static junit.framework.Assert.*;
import junit.framework.*;

import org.jibx.runtime.*;
import org.jibx.runtime.impl.*;

import java.io.*;

public class ScoreManagerTest
    extends BaseTestCase
{
    public static void main (String... args)
    {
        new ScoreManagerTest().play(args[0]);
    }

    public void testMarshall()
    {
        play("/soft/audiveris/src/test/omr/jibx/score-data.xml");
    }

    private void play (String fName)
    {
        System.out.println(getClass() + " fName=" + fName);

        Score score = ScoreManager.getInstance().load(new File(fName));
        score.dump();
        ScoreManager.getInstance().store(score, new File(fName + ".out.xml"));
    }
}
