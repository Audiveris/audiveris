//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      A n n o t a t i o n s                                     //
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
package org.audiveris.omr.classifier;

/**
 * Class {@code Annotations} gathers definitions about symbols annotations.
 *
 * @author Hervé Bitteur
 */
public abstract class Annotations
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** File name suffix for whole book annotations: {@value}. */
    public static final String BOOK_ANNOTATIONS_SUFFIX = "-annotations.zip";

    /** File name suffix for single sheet annotations: {@value}. */
    public static final String SHEET_ANNOTATIONS_SUFFIX = "-annotations.xml";

    /** File format for single sheet image: {@value}. */
    public static final String SHEET_IMAGE_FORMAT = "png";

    /** File name suffix for single sheet image: {@value}. */
    public static final String SHEET_IMAGE_SUFFIX = "-image." + SHEET_IMAGE_FORMAT;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Not meant to be instantiated.
     */
    private Annotations ()
    {
    }
}
