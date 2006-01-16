//-----------------------------------------------------------------------//
//                                                                       //
//                   R u b b e r Z o o m e d P a n e l                   //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.util.Logger;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class <code>RubberZoomedPanel</code> is a combination two linked
 * entities: a {@link ZoomedPanel} and a {@link Rubber}. Its
 * <i>paintComponent</i> method is declared final to ensure that the
 * rendering is done in proper sequence, with the rubber rectangle rendered
 * at the end on top of any other stuff.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class RubberZoomedPanel
    extends ZoomedPanel
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(RubberZoomedPanel.class);

    //~ Instance variables ------------------------------------------------

    /** Rubber band mouse handling */
    protected Rubber rubber;

    //~ Constructors ------------------------------------------------------

    //-------------------//
    // RubberZoomedPanel //
    //-------------------//
    /**
     * Create a bare RubberZoomedPanel, assuming zoom and rubber will be
     * assigned later.
     */
    public RubberZoomedPanel ()
    {
        if (logger.isDebugEnabled()) {
            logger.debug("new RubberZoomedPanel");
        }
    }

    //-------------------//
    // RubberZoomedPanel //
    //-------------------//
    /**
     * Create a RubberZoomedPanel, with the specified Rubber to interact
     * via the mouse.
     *
     * @param rubber the rubber instance to be linked to this panel
     */
    public RubberZoomedPanel (Zoom   zoom,
                              Rubber rubber)
    {
        super(zoom);
        setRubber(rubber);

        if (logger.isDebugEnabled()) {
            logger.debug("new RubberZoomedPanel"
                         + " zoom=" + zoom
                         + " rubber=" + rubber);
        }
    }

    //~ Methods -----------------------------------------------------------

    //-----------//
    // reDisplay //
    //-----------//
    @Override
        public void reDisplay ()
    {
        if (logger.isDebugEnabled()) {
            logger.debug("reDisplay rubber=" + rubber);
        }

        if (rubber != null) {
            setFocusRectangle(rubber.getRectangle());
        } else {
            setFocusRectangle(null);
        }
    }

    //---------------//
    // setFocusPoint //
    //---------------//
    @Override
        public void setFocusPoint (Point pt)
    {
        // Modify the rubber accordingly
        if (rubber != null) {
            if (pt != null) {
                rubber.resetOrigin(pt.x, pt.y);
            }
        }

        super.setFocusPoint(pt);
    }

    //-------------------//
    // setFocusRectangle //
    //-------------------//
    @Override
        public void setFocusRectangle (Rectangle rect)
    {
        // Modify the rubber accordingly
        if (rubber != null) {
            rubber.resetRectangle(rect);
        }

        super.setFocusRectangle(rect);
    }

    //-----------//
    // setRubber //
    //-----------//
    /**
     * Allows to provide the rubber instance, only after this
     * RubberZoomedPanel has been built. This can be use to solve circular
     * elaboration problems.
     *
     * @param rubber the rubber instance to be used
     */
    public void setRubber (Rubber rubber)
    {
        this.rubber = rubber;
    }

    //----------------//
    // paintComponent //
    //----------------//
    /**
     * Final method, called by Swing. If something has to be changed in the
     * rendering of the model, override the render method instead.
     *
     * @param g the graphic context
     */
    @Override
    protected final void paintComponent (Graphics g)
    {
        // Background first
        super.paintComponent(g);

        // Then, drawing specific to the view (to be provided in subclass)
        render(g);

        // Finally the rubber, now that everything else has been drawn
        if (rubber != null) {
            rubber.render(g);
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * This is just a place holder, the real rendering must be provided by
     * a subclass to actually render the object displayed, since the rubber
     * is automatically rendered after this one.
     *
     * @param g the graphic context
     */
    protected void render (Graphics g)
    {
    }
}
