//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e V i e w                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.score.Score;

/**
 * Abstract class <code>ScoreView</code> represents any view of a score,
 * either the ScoreEditor or any other simpler score display.
 *
 * @author Hervé Bitteur
 */
public abstract class ScoreView
{
    //~ Instance fields --------------------------------------------------------

    /** The related score */
    protected final Score score;

    /** The current score layout */
    protected ScoreLayout scoreLayout;

    /** The current set of painting parameters */
    protected PaintingParameters paintingParameters;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ScoreView object.
     *
     * @param score the related score
     * @param layout the initial score layout
     * @param parameters the current set of painting parameters
     */
    public ScoreView (Score              score,
                      ScoreLayout        layout,
                      PaintingParameters parameters)
    {
        this.score = score;
        this.scoreLayout = layout;
        this.paintingParameters = parameters;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // setLayout //
    //-----------//
    /**
     * Dynamically change the layout of the score display
     * @param layout the new score layout
     */
    public void setLayout (ScoreLayout layout)
    {
        this.scoreLayout = layout;
        update();
    }

    //-----------//
    // getLayout //
    //-----------//
    /**
     * Report the current system layout of the view
     * @return the current score layout
     */
    public ScoreLayout getLayout ()
    {
        return scoreLayout;
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Report the score this view is dedicated to
     *
     * @return the related score
     */
    public Score getScore ()
    {
        return score;
    }

    //-------//
    // close //
    //-------//
    /**
     * Close the score view.
     */
    public void close ()
    {
        // void by default
    }

    //-----//
    // set //
    //-----//
    /**
     * Dynamically change the painting parameters
     * @param parameters the new set of parameters
     */
    public void set (PaintingParameters parameters)
    {
        this.paintingParameters = parameters;
        update();
    }

    //    //----------------//
    //    // propertyChange //
    //    //----------------//
    //    @Implement(PropertyChangeListener.class)
    //    public void propertyChange (PropertyChangeEvent evt);

    //--------//
    // update //
    //--------//
    /**
     * Update the display when some parameters or size have changed
     */
    public void update ()
    {
        // Void by default
    }

    //    //-----------------//
    //    // updateSelection //
    //    //-----------------//
    //    public void updateSelection ();
}
