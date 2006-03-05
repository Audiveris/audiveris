//--------------------------------------------------------------------------//
//                                                                          //
//                        S c o r e V i e w T e s t                         //
//                                                                          //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.             //
//  This software is released under the terms of the GNU General Public     //
//  License. Please contact the author at herve.bitteur@laposte.net         //
//  to report bugs & suggestions.                                           //
//--------------------------------------------------------------------------//

package omr.score;


import java.awt.Rectangle;
import java.io.File;
import javax.swing.*;
import omr.glyph.Shape;
import omr.ui.icon.IconManager;
import omr.util.BaseTestCase;

public class ScoreViewTest
    extends BaseTestCase
{
    private ScoreView view;
    private JFrame frame;

    public static void main (String... args)
    {
        if (args.length > 0) {
            new ScoreViewTest().play(args[0]);
        } else {
            java.lang.System.out.println("*** ScoreViewTest. Expected a file name");
        }
    }

    public void testDisplay()
    {
        play("/soft/audiveris/save/Country.xml");
    }

    public void play (String fileName)
    {
        Score score = ScoreManager.getInstance().load(new File(fileName));

        view = new ScoreView(score);
        view.getPane().getView().getZoom().fireStateChanged();
        frame = new JFrame(getClass().getName() + " - " + fileName);
        frame.getContentPane().add(view.getPane().getComponent());

        frame.pack();
        frame.setBounds(new Rectangle(20, 20, 800, 200));
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ////score.dump();
    }
}
