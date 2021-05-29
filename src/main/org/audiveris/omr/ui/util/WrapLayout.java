//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       W r a p L a y o u t                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.ui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * FlowLayout subclass that fully supports wrapping of components.
 *
 * @author Rob Camick
 *
 * @see <a href="https://tips4java.wordpress.com/2008/11/06/wrap-layout/">Wrap Layout</a>
 */
public class WrapLayout
        extends FlowLayout
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(WrapLayout.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private Dimension preferredLayoutSize;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Constructs a new <code>WrapLayout</code> with a left
     * alignment and a default 5-unit horizontal and vertical gap.
     */
    public WrapLayout ()
    {
        super();
    }

    /**
     * Constructs a new <code>FlowLayout</code> with the specified
     * alignment and a default 5-unit horizontal and vertical gap.
     * <p>
     * The value of the alignment argument must be one of
     * <code>WrapLayout.LEADING</code>, <code>WrapLayout.CENTER</code>,
     * or <code>WrapLayout.TRAILING</code>.
     *
     * @param align the alignment value
     */
    public WrapLayout (int align)
    {
        super(align);
    }

    /**
     * Creates a new flow layout manager with the indicated alignment
     * and the indicated horizontal and vertical gaps.
     * <p>
     * The value of the alignment argument must be one of
     * <code>WrapLayout.LEADING</code>, <code>WrapLayout.CENTER</code>,
     * or <code>WrapLayout.TRAILING</code>.
     *
     * @param align the alignment value
     * @param hgap  the horizontal gap between components
     * @param vgap  the vertical gap between components
     */
    public WrapLayout (int align,
                       int hgap,
                       int vgap)
    {
        super(align, hgap, vgap);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    //    /**
    //     * Layout the components in the Container using the layout logic of the parent
    //     * FlowLayout class.
    //     *
    //     * @param target the Container using this WrapLayout
    //     */
    //    @Override
    //    public void layoutContainer (Container target)
    //    {
    //        Dimension size = preferredLayoutSize(target);
    //        // When a frame is minimized or maximized the preferred size of the
    //        // Container is assumed not to change. Therefore we need to force a
    //        // validate() to make sure that space, if available, is allocated to
    //        // the panel using a WrapLayout.
    //        if (size.equals(preferredLayoutSize)) {
    //            super.layoutContainer(target);
    //        } else {
    //            preferredLayoutSize = size;
    //            Container top = target;
    //
    //            while (top.getParent() != null) {
    //                top = top.getParent();
    //            }
    //
    //            top.validate();
    //        }
    //    }
    //
    /**
     * Returns the preferred dimensions for this layout given the
     * <i>visible</i> components in the specified target container.
     *
     * @param target the component which needs to be laid out
     * @return the preferred dimensions to lay out the
     *         sub-components of the specified container
     */
    @Override
    public Dimension preferredLayoutSize (Container target)
    {
        return layoutSize(target, true);
    }

    /**
     * Returns the minimum dimensions needed to layout the <i>visible</i>
     * components contained in the specified target container.
     *
     * @param target the component which needs to be laid out
     * @return the minimum dimensions to lay out the
     *         sub-components of the specified container
     */
    @Override
    public Dimension minimumLayoutSize (Container target)
    {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);

        return minimum;
    }

    /**
     * Returns the minimum or preferred dimension needed to layout the target
     * container.
     *
     * @param target    target to get layout size for
     * @param preferred should preferred size be calculated
     * @return the dimension to layout the target container
     */
    private Dimension layoutSize (Container target,
                                  boolean preferred)
    {
        synchronized (target.getTreeLock()) {
            //  Each row must fit with the width allocated to the container.
            //  When the container width = 0, the preferred width of the container
            //  has not yet been calculated so lets ask for the maximum.
            Container container = target;

            while ((container.getSize().width == 0) && (container.getParent() != null)) {
                container = container.getParent();
            }

            logger.debug("WRAP_LAYOUT containerName: {}", container.getName());

            int targetWidth = container.getSize().width;

            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
            int maxWidth = targetWidth - horizontalInsetsAndGap;

            //  Fit components into the allowed width
            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int nmembers = target.getComponentCount();

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);

                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                    //  Can't add the component to current row. Start a new row.
                    if ((rowWidth + d.width) > maxWidth) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    //  Add a horizontal gap for all components after the first
                    if (rowWidth != 0) {
                        rowWidth += hgap;
                    }

                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }

            addRow(dim, rowWidth, rowHeight);

            dim.width += horizontalInsetsAndGap;
            dim.height += (insets.top + insets.bottom + (vgap * 2));

            //	When using a scroll pane or the DecoratedLookAndFeel we need to
            //  make sure the preferred size is less than the size of the
            //  target container so shrinking the container size works
            //  correctly. Removing the horizontal gap is an easy way to do this.
            Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);

            if ((scrollPane != null) && target.isValid()) {
                dim.width -= (hgap + 1);
            }

            logger.debug("WRAP_LAYOUT containerSize:{} dim:{}", container.getSize(), dim);

            return dim;
        }
    }

    /*
     * A new row has been completed. Use the dimensions of this row
     * to update the preferred size for the container.
     *
     * @param dim update the width and height when appropriate
     * @param rowWidth the width of the row to add
     * @param rowHeight the height of the row to add
     */
    private void addRow (Dimension dim,
                         int rowWidth,
                         int rowHeight)
    {
        dim.width = Math.max(dim.width, rowWidth);

        if (dim.height > 0) {
            dim.height += getVgap();
        }

        dim.height += rowHeight;
    }
}
