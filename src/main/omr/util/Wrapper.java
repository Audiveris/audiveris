//----------------------------------------------------------------------------//
//                                                                            //
//                               W r a p p e r                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;


/**
 * Class <code>Wrapper</code> is used to wrap an output value
 *
 * @param <T> The specific type for carried value
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Wrapper<T>
{
    //~ Instance fields --------------------------------------------------------

    /** The wrapped value */
    public T value;
}
