//----------------------------------------------------------------------------//
//                                                                            //
//                       R e g r e s s i o n P a n e l                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui.panel;

import omr.glyph.GlyphNetwork;
import omr.glyph.GlyphRegression;
import omr.glyph.ui.panel.TrainingPanel.DumpAction;

import omr.log.Logger;

import omr.util.Implement;

import java.util.*;

import javax.swing.*;

/**
 * Class <code>RegressionPanel</code> is the user interface that handles the
 * training of the neural network evaluator. It is a dedicated companion of
 * class {@link GlyphTrainer}.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
class RegressionPanel
    extends TrainingPanel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        RegressionPanel.class);

    //~ Instance fields --------------------------------------------------------

    /** Training start time */
    private long startTime;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // RegressionPanel //
    //-----------------//
    /**
     * Creates a new RegressionPanel object.
     *
     * @param task the current training activity
     * @param standardWidth standard width for fields & buttons
     * @param selectionPanel the panel for glyph repository
     */
    public RegressionPanel (GlyphTrainer.Task task,
                            String            standardWidth,
                            SelectionPanel    selectionPanel)
    {
        super(
            task,
            standardWidth,
            GlyphRegression.getInstance(),
            selectionPanel,
            4);
        task.addObserver(this);

        trainAction = new TrainAction("Train");

        defineSpecificLayout();
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // update //
    //--------//
    /**
     * Specific behavior when a new task activity is notified. In addition to
     * {@link TrainingPanel#update}, actions specific to training a neural
     * network are handled here.
     *
     * @param obs the task object
     * @param unused not used
     */
    @Implement(Observer.class)
    @Override
    public void update (Observable obs,
                        Object     unused)
    {
        super.update(obs, unused);

        switch (task.getActivity()) {
        case INACTIVE :
            break;

        case SELECTING :
            break;

        case TRAINING :
            break;
        }
    }

    //----------------------//
    // defineSpecificLayout //
    //----------------------//
    private void defineSpecificLayout ()
    {
        int     r = 7;

        // Training entities
        JButton dumpButton = new JButton(new DumpAction());
        dumpButton.setToolTipText("Dump the evaluator internals");

        JButton trainButton = new JButton(trainAction);
        trainButton.setToolTipText("Train the evaluator from scratch");

        builder.add(dumpButton, cst.xy(3, r));
        builder.add(trainButton, cst.xy(5, r));
    }
}
