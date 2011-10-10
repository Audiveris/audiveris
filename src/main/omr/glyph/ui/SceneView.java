//----------------------------------------------------------------------------//
//                                                                            //
//                             S c e n e V i e w                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Scene;

import omr.graph.DigraphView;

import omr.lag.Lag;
import omr.lag.Section;

import omr.log.Logger;

import omr.selection.UserEvent;

import omr.ui.util.UIUtilities;
import omr.ui.view.RubberPanel;

import omr.util.Implement;
import omr.util.WeakPropertyChangeListener;

import org.bushe.swing.event.EventSubscriber;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code SceneView} is a  view that combines the display of several lags
 * to repreesent a scene of glyphs
 *
 * @author Hervé Bitteur
 */
public class SceneView
    extends RubberPanel
    implements DigraphView, PropertyChangeListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SceneView.class);

    //~ Instance fields --------------------------------------------------------

    /** The underlying scene */
    protected final Scene scene;

    /** Related glyphs controller */
    protected final GlyphsController controller;

    /** The sequence of lags */
    protected final List<Lag> lags;
    
    /** Additional event subscribers */
    protected final List<EventSubscriber<UserEvent>> subscribers = new ArrayList<EventSubscriber<UserEvent>>();

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // SceneView //
    //-----------//
    /**
     * Create a scene view
     * @param the underlying scene of glyphs
     * @param lags the various lags to be displayed
     */
    public SceneView (Scene            scene,
                      GlyphsController controller,
                      List<Lag>        lags)
    {
        this.scene = scene;
        this.controller = controller;
        this.lags = lags;

        setName(scene.getName() + "-View");

        setBackground(Color.white);

        // (Weakly) listening on ViewParameters properties
        ViewParameters.getInstance()
                      .addPropertyChangeListener(
            new WeakPropertyChangeListener(this));
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getController //
    //---------------//
    public GlyphsController getController ()
    {
        return controller;
    }

    //--------------------//
    // addEventSubscriber //
    //--------------------//
    public void addEventSubscriber (EventSubscriber<UserEvent> subscriber,
                                    Class[]                    eventClasses)
    {
    }

    //----------------//
    // propertyChange //
    //----------------//
    @Implement(PropertyChangeListener.class)
    public void propertyChange (PropertyChangeEvent evt)
    {
        // Whatever the property change, we simply repaint the view
        repaint();
    }

    //---------//
    // refresh //
    //---------//
    public void refresh ()
    {
        repaint();
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the scene in the provided Graphics context, which may be already
     * scaled
     * @param g the graphics context
     */
    @Override
    public void render (Graphics2D g)
    {
        // Should we draw the section borders?
        final boolean drawBorders = ViewParameters.getInstance()
                                                  .isSectionSelectionEnabled();

        // Stroke for borders
        final Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1f);

        for (Lag lag : lags) {
            // Render all sections, using the colors they have been assigned
            for (Section section : lag.getVertices()) {
                section.render(g, drawBorders);
            }
        }

        // Paint additional items, such as recognized items, etc...
        renderItems(g);

        // Restore stroke
        g.setStroke(oldStroke);
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Room for rendering additional items, on top of the basic lag itself.
     * This default implementation paints the selected section set if any
     * @param g the graphic context
     */
    protected void renderItems (Graphics2D g)
    {
        // Void by default
    }
}
