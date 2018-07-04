//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           K e y R o i                                          //
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
package org.audiveris.omr.sheet.header;

import ij.process.Blitter;
import ij.process.ByteProcessor;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.inter.KeyAlterInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code KeyRoi} handles the region of interest for key retrieval, split into
 * vertical slices.
 *
 * @author Hervé Bitteur
 */
public class KeyRoi
        extends ArrayList<KeySlice>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(KeyRoi.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying staff. */
    private final Staff staff;

    /** Targeted key shape. */
    private final Shape keyShape;

    /** Region top ordinate. */
    public final int y;

    /** Region height. */
    public final int height;

    /** Maximum abscissa distance to theoretical slice. */
    private final int maxSliceDist;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code KeyRoi} object.
     *
     * @param staff        containing staff
     * @param keyShape     SHARP or FLAT
     * @param y            top ordinate
     * @param height       area height
     * @param maxSliceDist acceptable distance from slice side
     */
    public KeyRoi (Staff staff,
                   Shape keyShape,
                   int y,
                   int height,
                   int maxSliceDist)
    {
        this.staff = staff;
        this.keyShape = keyShape;
        this.y = y;
        this.height = height;
        this.maxSliceDist = maxSliceDist;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // attachmentKey //
    //---------------//
    public String attachmentKey (int id)
    {
        final String s = (keyShape == Shape.SHARP) ? "s" : "f";

        return "k" + s + id;
    }

    //-------------//
    // createSlice //
    //-------------//
    /**
     * Create a slice with provided start/stop abscissae and insert it at proper index.
     *
     * @param start slice start
     * @param stop  slice stop
     * @return the created slice
     */
    public KeySlice createSlice (int start,
                                 int stop)
    {
        final Rectangle rect = new Rectangle(start, y, stop - start + 1, height);
        final KeySlice slice = new KeySlice(rect, this);

        if (isEmpty() || (stop > get(size() - 1).getStop())) {
            // Append at end
            add(slice);
            staff.addAttachment(attachmentKey(size()), slice.getRect());
        } else {
            // Insert at proper index
            for (KeySlice sl : this) {
                if (stop <= sl.getStop()) {
                    final int idx = indexOf(sl);
                    add(idx, slice);

                    for (int i = idx; i < size(); i++) {
                        staff.addAttachment(attachmentKey(i + 1), get(i).getRect());
                    }

                    break;
                }
            }
        }

        return slice;
    }

    //---------//
    // destroy //
    //---------//
    /**
     * Remove any key material: slices potential alter and key shape.
     */
    public void destroy ()
    {
        for (KeySlice slice : this) {
            slice.deleteAlter();
        }

        clear(); // Remove all slices

        final String s = (keyShape == Shape.SHARP) ? "s" : "f";
        staff.removeAttachments("k" + s);
    }

    //--------------//
    // freezeAlters //
    //--------------//
    public void freezeAlters ()
    {
        for (KeySlice slice : this) {
            KeyAlterInter alter = slice.getAlter();

            if (alter != null) {
                alter.freeze();
            }
        }
    }

    //---------------//
    // getAreaPixels //
    //---------------//
    /**
     * Report the pixels buffer for the whole key area
     *
     * @param source pixel source (staff free)
     * @param range  start/stop values for key area
     * @return the buffer of area pixels
     */
    public ByteProcessor getAreaPixels (ByteProcessor source,
                                        StaffHeader.Range range)
    {
        Rectangle keyRect = new Rectangle(range.getStart(), y, range.getWidth(), height);
        ByteProcessor keyBuffer = new ByteProcessor(keyRect.width, height);
        keyBuffer.copyBits(source, -keyRect.x, -y, Blitter.COPY);

        return keyBuffer;
    }

    //----------------//
    // getEmptySlices //
    //----------------//
    /**
     * Report the sequence of slices found empty.
     *
     * @return the empty slices
     */
    public List<KeySlice> getEmptySlices ()
    {
        List<KeySlice> emptySlices = null;

        for (KeySlice slice : this) {
            if (slice.getAlter() == null) {
                if (emptySlices == null) {
                    emptySlices = new ArrayList<KeySlice>();
                }

                emptySlices.add(slice);
            }
        }

        if (emptySlices == null) {
            return Collections.emptyList();
        }

        return emptySlices;
    }

    //-------------------//
    // getLastValidSlice //
    //-------------------//
    public KeySlice getLastValidSlice ()
    {
        KeySlice validSlice = null;

        for (KeySlice slice : this) {
            if (!slice.isStuffed() && (slice.getAlter() != null)) {
                validSlice = slice;
            }
        }

        return validSlice;
    }

    //----------------//
    // getSlicePixels //
    //----------------//
    /**
     * Report the pixels buffer for just a slice
     *
     * @param source        pixel source (staff free)
     * @param slice         the current slice
     * @param cropNeighbors true for discarding pixels taken by neighboring slices
     * @return the buffer of slice pixels
     */
    public ByteProcessor getSlicePixels (ByteProcessor source,
                                         KeySlice slice,
                                         boolean cropNeighbors)
    {
        Rectangle sRect = slice.getRect();
        BufferedImage sImage = new BufferedImage(
                sRect.width,
                sRect.height,
                BufferedImage.TYPE_BYTE_GRAY);
        ByteProcessor sBuffer = new ByteProcessor(sImage);
        sBuffer.copyBits(source, -sRect.x, -sRect.y, Blitter.COPY);

        if (cropNeighbors) {
            // Erase good key items from adjacent slices, if any
            final int idx = indexOf(slice);
            final Integer prevIdx = (idx > 0) ? (idx - 1) : null;
            final Integer nextIdx = (idx < (size() - 1)) ? (idx + 1) : null;
            Graphics2D g = null;

            for (Integer i : new Integer[]{prevIdx, nextIdx}) {
                if (i != null) {
                    final KeySlice sl = get(i);

                    if (sl.getAlter() != null) {
                        final Glyph glyph = sl.getAlter().getGlyph();

                        if (glyph.getBounds().intersects(sRect)) {
                            if (g == null) {
                                g = sImage.createGraphics();
                                g.setColor(Color.white);
                            }

                            final Point offset = new Point(
                                    glyph.getLeft() - sRect.x,
                                    glyph.getTop() - sRect.y);
                            logger.debug("Erasing glyph#{} from {}", glyph.getId(), slice);
                            glyph.getRunTable().render(g, offset);
                        }
                    }
                }
            }

            if (g != null) {
                g.dispose();
            }
        }

        return sBuffer;
    }

    //----------//
    // getStaff //
    //----------//
    public final Staff getStaff ()
    {
        return staff;
    }

    //-----------//
    // getStarts //
    //-----------//
    public List<Integer> getStarts ()
    {
        List<Integer> starts = new ArrayList<Integer>();

        for (KeySlice slice : this) {
            starts.add(slice.getRect().x);
        }

        return starts;
    }

    //---------//
    // sliceOf //
    //---------//
    public KeySlice sliceOf (int x)
    {
        for (KeySlice slice : this) {
            if (GeoUtil.xEmbraces(slice.getRect(), x)) {
                return slice;
            }
        }

        return null;
    }

    //-----------------//
    // stuffSlicesFrom //
    //-----------------//
    /**
     * Stuff all remaining slices, starting at provided index
     *
     * @param index provided index
     */
    public void stuffSlicesFrom (int index)
    {
        for (KeySlice slice : subList(index, size())) {
            slice.deleteAlter();
            slice.setStuffed();
        }
    }

    //---------------//
    // getStartSlice //
    //---------------//
    /**
     * Return slice, if any, which starts near the provided abscissa.
     *
     * @param x provided abscissa
     * @return slice found, or null
     */
    KeySlice getStartSlice (int x)
    {
        KeySlice bestSlice = null;
        double bestDist = Double.MAX_VALUE;

        for (KeySlice slice : this) {
            double dist = Math.abs(x - slice.getRect().x);

            if (bestDist > dist) {
                bestDist = dist;
                bestSlice = slice;
            }
        }

        if (bestDist <= maxSliceDist) {
            return bestSlice;
        } else {
            return null;
        }
    }

    //--------------//
    // getStopSlice //
    //--------------//
    /**
     * Return slice, if any, which stops near the provided abscissa.
     *
     * @param x provided abscissa
     * @return slice found, or null
     */
    KeySlice getStopSlice (int x)
    {
        KeySlice bestSlice = null;
        double bestDist = Double.MAX_VALUE;

        for (KeySlice slice : this) {
            Rectangle rect = slice.getRect();
            double dist = Math.abs(x - rect.x - rect.width + 1);

            if (bestDist > dist) {
                bestDist = dist;
                bestSlice = slice;
            }
        }

        if (bestDist <= maxSliceDist) {
            return bestSlice;
        } else {
            return null;
        }
    }
}
