//----------------------------------------------------------------------------//
//                                                                            //
//                         P a i n t i n g L a y e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;


/**
 * Enum {@code PaintingLayer} defines layers to be painted
 */
public enum PaintingLayer {
    /** Input data: image or glyphs */
    INPUT,
    /** Both input and output */
    INPUT_OUTPUT, 
    /** Output data: score entities */
    OUTPUT;
}
