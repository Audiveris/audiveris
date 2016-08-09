//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          F a i l u r e                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.check;

/**
 * Class {@code Failure} is the root of all results that store a failure.
 *
 * @author Hervé Bitteur
 */
public class Failure
{
    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * A readable comment about the failure.
     */
    public final String comment;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new Failure object.
     *
     * @param comment A comment that describe the failure reason
     */
    public Failure (String comment)
    {
        this.comment = comment;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // toString //
    //----------//
    /**
     * Report a readable string about this failure instance
     *
     * @return a descriptive string
     */
    @Override
    public String toString ()
    {
        return "failure:" + comment;
    }
}
