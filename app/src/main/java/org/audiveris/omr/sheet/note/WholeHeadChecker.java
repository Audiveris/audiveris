//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 W h o l e H e a d C h e c k e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.sheet.note;

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.image.PixelSource;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.inter.HeadInter;

import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Class <code>WholeHeadChecker</code> checks whether a stem-less head candidate is the ink of a
 * genuine whole note.
 * <p>
 * Whole heads are not looked for by {@link NoteHeadsBuilder}, because the stem-less templates cost
 * more heads to the wholes they mistake than they find whole heads.
 * A whole note thus shows up as a stem-less void head, graded by a template that does not fit it:
 * a whole head is wider than a half head, and a bold engraving may leave only a tiny hole.
 * <p>
 * So rather than match a template, we look at the ink the candidate lies on.
 * It is a whole head if this ink is a single blob which is as high as a head, clearly wider than a
 * half head is, and which encloses a hole (a stem-less black head has none).
 */
public class WholeHeadChecker
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    /** Indices in the blob data. */
    private static final int COUNT = 0;

    private static final int LEFT = 1;

    private static final int TOP = 2;

    private static final int RIGHT = 3;

    private static final int BOTTOM = 4;

    private static final int TOUCHES = 5;

    //~ Instance fields ----------------------------------------------------------------------------

    private final Scale scale;

    /** Ink, with staff lines removed. */
    private final ByteProcessor ink;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a <code>WholeHeadChecker</code> object.
     *
     * @param sheet the containing sheet
     */
    public WholeHeadChecker (Sheet sheet)
    {
        scale = sheet.getScale();
        ink = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // check //
    //-------//
    /**
     * Check the ink under the provided stem-less head candidate.
     *
     * @param head the candidate head
     * @return the bounds of the whole head ink if the candidate is a whole head, null otherwise
     */
    public Rectangle check (HeadInter head)
    {
        final int interline = scale.getInterline();
        final int margin = scale.toPixels(constants.searchMargin);
        final Rectangle box = head.getBounds();
        final Rectangle win = new Rectangle(box);
        win.grow(margin, margin);
        win.setBounds(win.intersection(new Rectangle(0, 0, ink.getWidth(), ink.getHeight())));

        // The blob under the candidate: the largest connected ink within the window
        final boolean[] seen = new boolean[win.width * win.height];
        int[] best = null;

        for (int y = box.y; y < (box.y + box.height); y++) {
            for (int x = box.x; x < (box.x + box.width); x++) {
                if (win.contains(x, y) && isFore(x, y) && !seen[index(win, x, y)]) {
                    final int[] blob = flood(win, seen, x, y);

                    if ((best == null) || (blob[COUNT] > best[COUNT])) {
                        best = blob;
                    }
                }
            }
        }

        // A blob that reaches the window border is merged with something else, a slur for example
        if ((best == null) || (best[TOUCHES] != 0)) {
            return null;
        }

        final Rectangle blobBox = new Rectangle(
                best[LEFT],
                best[TOP],
                best[RIGHT] - best[LEFT] + 1,
                best[BOTTOM] - best[TOP] + 1);
        final double ratio = (double) blobBox.width / blobBox.height;

        if ((blobBox.height < (constants.minHeightRatio.getValue() * interline))
                || (blobBox.height > (constants.maxHeightRatio.getValue() * interline))
                || (ratio < constants.minWidthRatio.getValue())
                || (ratio > constants.maxWidthRatio.getValue())) {
            return null;
        }

        final double minHole = constants.minHoleRatio.getValue() * interline * interline;

        return (holeArea(blobBox) >= minHole) ? blobBox : null;
    }

    //-------//
    // flood //
    //-------//
    /**
     * Flood the ink blob that contains the provided point, within the window.
     *
     * @return the blob data, indexed by COUNT, LEFT, TOP, RIGHT, BOTTOM and TOUCHES
     */
    private int[] flood (Rectangle win,
                         boolean[] seen,
                         int x0,
                         int y0)
    {
        final int[] res = new int[] { 0, x0, y0, x0, y0, 0 };
        final Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[] { x0, y0 });
        seen[index(win, x0, y0)] = true;

        while (!stack.isEmpty()) {
            final int[] p = stack.pop();
            res[COUNT]++;
            res[LEFT] = Math.min(res[LEFT], p[0]);
            res[TOP] = Math.min(res[TOP], p[1]);
            res[RIGHT] = Math.max(res[RIGHT], p[0]);
            res[BOTTOM] = Math.max(res[BOTTOM], p[1]);

            if ((p[0] == win.x) || (p[1] == win.y) || (p[0] == (win.x + win.width - 1))
                    || (p[1] == (win.y + win.height - 1))) {
                res[TOUCHES] = 1;
            }

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    final int x = p[0] + dx;
                    final int y = p[1] + dy;

                    if (win.contains(x, y) && isFore(x, y) && !seen[index(win, x, y)]) {
                        seen[index(win, x, y)] = true;
                        stack.push(new int[] { x, y });
                    }
                }
            }
        }

        return res;
    }

    //----------//
    // holeArea //
    //----------//
    /**
     * Report the area of background enclosed by the ink in the provided box.
     * <p>
     * This is the background in the box which cannot be reached from the box border.
     */
    private int holeArea (Rectangle box)
    {
        final boolean[] outside = new boolean[box.width * box.height];
        final Deque<int[]> stack = new ArrayDeque<>();

        for (int x = box.x; x < (box.x + box.width); x++) {
            seedOutside(box, outside, stack, x, box.y);
            seedOutside(box, outside, stack, x, box.y + box.height - 1);
        }

        for (int y = box.y; y < (box.y + box.height); y++) {
            seedOutside(box, outside, stack, box.x, y);
            seedOutside(box, outside, stack, box.x + box.width - 1, y);
        }

        while (!stack.isEmpty()) {
            final int[] p = stack.pop();
            seedOutside(box, outside, stack, p[0] + 1, p[1]);
            seedOutside(box, outside, stack, p[0] - 1, p[1]);
            seedOutside(box, outside, stack, p[0], p[1] + 1);
            seedOutside(box, outside, stack, p[0], p[1] - 1);
        }

        int area = 0;

        for (int y = box.y; y < (box.y + box.height); y++) {
            for (int x = box.x; x < (box.x + box.width); x++) {
                if (!isFore(x, y) && !outside[index(box, x, y)]) {
                    area++;
                }
            }
        }

        return area;
    }

    private int index (Rectangle box,
                       int x,
                       int y)
    {
        return ((y - box.y) * box.width) + (x - box.x);
    }

    private boolean isFore (int x,
                            int y)
    {
        return ink.get(x, y) == PixelSource.FOREGROUND;
    }

    private void seedOutside (Rectangle box,
                              boolean[] outside,
                              Deque<int[]> stack,
                              int x,
                              int y)
    {
        if (box.contains(x, y) && !isFore(x, y) && !outside[index(box, x, y)]) {
            outside[index(box, x, y)] = true;
            stack.push(new int[] { x, y });
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction searchMargin = new Scale.Fraction(
                0.75,
                "Margin around a candidate head, to look for its ink blob");

        private final Constant.Ratio minHeightRatio = new Constant.Ratio(
                0.7,
                "Minimum whole head height, as ratio of interline");

        private final Constant.Ratio maxHeightRatio = new Constant.Ratio(
                1.5,
                "Maximum whole head height, as ratio of interline");

        private final Constant.Ratio minWidthRatio = new Constant.Ratio(
                1.35,
                "Minimum whole head width, as ratio of its height (a half head is about 1.2)");

        private final Constant.Ratio maxWidthRatio = new Constant.Ratio(
                2.2,
                "Maximum whole head width, as ratio of its height");

        private final Constant.Ratio minHoleRatio = new Constant.Ratio(
                0.005,
                "Minimum hole area of a whole head, as ratio of interline squared");
    }
}
