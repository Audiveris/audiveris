//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            Z o o m                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui.view;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code Zoom} encapsulates a zoom ratio, which is typically the ratio between
 * display values (such as the size of the display of an entity) and model values
 * (such as the size of the entity itself).
 *
 * For example, a Zoom with ratio set to a 2.0 value would double the display of a given entity.
 * <p>
 * Since this class is meant to be used when handling display tasks, it also provides utility
 * methods to go back and forth between display and model values, for simple items such as primitive
 * data (double), Point, Dimension and Rectangle.
 * <p>
 * It handles an internal collection of change listeners, that are entities registered to be
 * notified whenever a new ratio value is set.
 * <p>
 * A new value is programmatically set by calling the {@link #setRatio} method. The newly defined
 * ratio value is then pushed to all registered observers.
 * <p>
 * A {@link LogSlider} can be connected to this zoom entity, to provide UI for both output and
 * input.
 *
 * @author Hervé Bitteur
 */
public class Zoom
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Zoom.class);

    // To assign a unique Id
    private static int globalId;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Unique event, created lazily */
    protected ChangeEvent changeEvent = null;

    /** Potential logarithmic slider to drive this zoom */
    protected LogSlider slider;

    /** Collection of event listeners */
    protected Set<ChangeListener> listeners = new LinkedHashSet<ChangeListener>();

    /** Current ratio value */
    protected double ratio;

    // Unique Id (to ease debugging)
    private int id = ++globalId;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a zoom entity, with a default ratio value of 1.
     */
    public Zoom ()
    {
        this(1);
    }

    /**
     * Create a zoom entity, with the provided initial ratio value.
     *
     * @param ratio the initial ratio value
     */
    public Zoom (double ratio)
    {
        logger.debug("Zoom created with ratio {}", ratio);
        setRatio(ratio);
    }

    /**
     * Create a zoom entity, with the provided initial ratio value. and a
     * related slider
     *
     * @param slider the related slider
     * @param ratio  the initial ratio value
     */
    public Zoom (LogSlider slider,
                 double ratio)
    {
        logger.debug("Zoom created" + " slider={} ratio={}", slider, ratio);
        setSlider(slider);
        setRatio(ratio);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------------//
    // addChangeListener //
    //-------------------//
    /**
     * Register a change listener, to be notified when the zoom value is
     * changed
     *
     * @param listener the listener to be notified
     */
    public void addChangeListener (ChangeListener listener)
    {
        listeners.add(listener);
        logger.debug("addChangeListener {} -> {}", listener, listeners.size());
    }

    //------------------//
    // fireStateChanged //
    //------------------//
    /**
     * In charge of forwarding the change notification to all registered
     * listeners
     */
    public void fireStateChanged ()
    {
        for (ChangeListener listener : listeners) {
            if (changeEvent == null) {
                changeEvent = new ChangeEvent(this);
            }

            logger.debug("{} Firing {}", this, listener);
            listener.stateChanged(changeEvent);
        }
    }

    //----------//
    // getRatio //
    //----------//
    /**
     * Return the current zoom ratio. Ratio is defined as display / source.
     *
     * @return the ratio
     */
    public double getRatio ()
    {
        return ratio;
    }

    //----------------------//
    // removeChangeListener //
    //----------------------//
    /**
     * Unregister a change listener
     *
     * @param listener the listener to remove
     * @return true if actually removed
     */
    public boolean removeChangeListener (ChangeListener listener)
    {
        return listeners.remove(listener);
    }

    //-------//
    // scale //
    //-------//
    /**
     * Scale all the elements of provided point
     *
     * @param pt the point to be scaled
     */
    public void scale (Point pt)
    {
        pt.x = scaled(pt.x);
        pt.y = scaled(pt.y);
    }

    //-------//
    // scale //
    //-------//
    /**
     * Scale all the elements of provided dimension
     *
     * @param dim the dimension to be scaled
     */
    public void scale (Dimension dim)
    {
        dim.width = scaled(dim.width);
        dim.height = scaled(dim.height);
    }

    //-------//
    // scale //
    //-------//
    /**
     * Scale all the elements of provided rectangle
     *
     * @param rect the rectangle to be scaled
     */
    public void scale (Rectangle rect)
    {
        rect.x = scaled(rect.x);
        rect.y = scaled(rect.y);
        rect.width = scaled(rect.width);
        rect.height = scaled(rect.height);
    }

    //-------//
    // scale //
    //-------//
    /**
     * Scale provided line
     *
     * @param line the line to be scaled
     */
    public void scale (Line2D line)
    {
        line.setLine(
                scaled(line.getX1()),
                scaled(line.getY1()),
                scaled(line.getX2()),
                scaled(line.getY2()));
    }

    //--------//
    // scaled //
    //--------//
    /**
     * Coordinate computation, Source &rarr; Display
     *
     * @param val a source value
     *
     * @return the (scaled) display value
     */
    public int scaled (double val)
    {
        return (int) Math.rint(val * ratio);
    }

    //--------//
    // scaled //
    //--------//
    /**
     * Coordinate computation, Source &rarr; Display
     *
     * @param pt source point
     *
     * @return the corresponding (scaled) point
     */
    public Point scaled (Point pt)
    {
        Point p = new Point(pt);
        scale(p);

        return p;
    }

    //--------//
    // scaled //
    //--------//
    /**
     * Coordinate computation, Source &rarr; Display
     *
     * @param dim source dimension
     *
     * @return the corresponding (scaled) dimension
     */
    public Dimension scaled (Dimension dim)
    {
        Dimension d = new Dimension(dim);
        scale(d);

        return d;
    }

    //--------//
    // scaled //
    //--------//
    /**
     * Coordinate computation, Source &rarr; Display
     *
     * @param rect source rectangle
     *
     * @return the corresponding (scaled) rectangle
     */
    public Rectangle scaled (Rectangle rect)
    {
        Rectangle r = new Rectangle(rect);
        scale(r);

        return r;
    }

    //----------//
    // setRatio //
    //----------//
    /**
     * Change the display zoom ratio. Nota, if the zoom is coupled with a
     * slider, this slider has the final word concerning the precise zoom
     * value, since the slider uses integer (or fractional) values.
     *
     * @param ratio the new ratio
     */
    public void setRatio (double ratio)
    {
        logger.debug("setRatio ratio={}", ratio);

        // Propagate to slider (useful to keep slider in sync when ratio is
        // set programmatically)
        if (slider != null) {
            slider.setDoubleValue(ratio);
        } else {
            forceRatio(ratio);
        }
    }

    //-----------//
    // setSlider //
    //-----------//
    /**
     * Define a related logarithmic slider, as a UI to adjust the zoom
     * value
     *
     * @param slider the related slider UI
     */
    public void setSlider (final LogSlider slider)
    {
        this.slider = slider;
        logger.debug("setSlider");

        if (slider != null) {
            slider.setFocusable(false);
            slider.setDoubleValue(ratio);

            slider.addChangeListener(
                    new ChangeListener()
            {
                @Override
                public void stateChanged (ChangeEvent e)
                {
                    // Forward the new zoom ratio
                    if (constants.continuousSliderReading.getValue()
                        || !slider.getValueIsAdjusting()) {
                        double newRatio = slider.getDoubleValue();
                        logger.debug("Slider firing zoom newRatio={}", newRatio);

                        // Stop condition to avoid endless loop between
                        // slider and zoom
                        if (Math.abs(newRatio - ratio) > .001) {
                            forceRatio(newRatio);
                        }
                    }
                }
            });
        }
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a quick description
     *
     * @return the zoom information
     */
    @Override
    public String toString ()
    {
        return "{Zoom#" + id + " listeners=" + listeners.size() + " ratio=" + ratio + "}";
    }

    //-------------//
    // truncScaled //
    //-------------//
    /**
     * Coordinate computation, Source &rarr; Display, but with a truncation
     * rather than rounding.
     *
     * @param val a source value
     * @return the (scaled) display value
     */
    public int truncScaled (double val)
    {
        return (int) Math.floor(val * ratio);
    }

    //---------------//
    // truncUnscaled //
    //---------------//
    /**
     * Coordinate computation, Display &rarr; Source, but with a truncation
     * rather than rounding.
     *
     * @param val a display value
     * @return the corresponding (unscaled) source coordinate
     */
    public int truncUnscaled (double val)
    {
        return (int) Math.floor(val / ratio);
    }

    //---------//
    // unscale //
    //---------//
    /**
     * Unscale a point
     *
     * @param pt the point to unscale
     */
    public void unscale (Point pt)
    {
        pt.x = unscaled(pt.x);
        pt.y = unscaled(pt.y);
    }

    //----------//
    // unscaled //
    //----------//
    /**
     * Coordinate computation Display &rarr; Source
     *
     * @param val a display value
     * @return the corresponding (unscaled) source coordinate
     */
    public int unscaled (double val)
    {
        return (int) Math.rint(val / ratio);
    }

    //----------//
    // unscaled //
    //----------//
    /**
     * Point computation Display &rarr; Source
     *
     * @param pt a display point
     * @return the corresponding (unscaled) source point
     */
    public Point unscaled (Point pt)
    {
        return new Point((int) Math.rint(pt.x / ratio), (int) Math.rint(pt.y / ratio));
    }

    //------------//
    // forceRatio //
    //------------//
    /**
     * Impose and propagate the new ratio
     *
     * @param ratio the new ratio
     */
    private void forceRatio (double ratio)
    {
        logger.debug("forceRatio ratio={}", ratio);
        this.ratio = ratio;

        // Propagate to listeners
        fireStateChanged();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean continuousSliderReading = new Constant.Boolean(
                true,
                "Should we allow continuous reading of the zoom slider");
    }
}
