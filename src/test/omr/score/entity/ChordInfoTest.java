//----------------------------------------------------------------------------//
//                                                                            //
//                          C h o r d I n f o T e s t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import static omr.score.entity.ChordInfo.*;
import static omr.score.entity.ChordInfo.Degree.DegreeType.*;
import static omr.score.entity.ChordInfo.Kind.Type.*;
import static omr.score.entity.Note.Step.*;

import omr.util.ClassUtil;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Unitary tests of ChordInfo.
 *
 * @author Hervé Bitteur
 */
public class ChordInfoTest
{
    //~ Static fields/initializers ---------------------------------------------

    /** Store output in dedicated file. */
    private static final PrintWriter out = getPrintWriter(
        new File("data/temp/tests.log"));

    //~ Methods ----------------------------------------------------------------

    @Test
    public void test_01 ()
    {
        final String s = "D°";
        t(s, new ChordInfo(s, new Pitch(D), new Kind(DIMINISHED, "°")));
    }

    @Test
    public void test_02 ()
    {
        final String s = "D°7";
        t(
            s,
            new ChordInfo(s, new Pitch(D), new Kind(DIMINISHED_SEVENTH, "°7")));
    }

    @Test
    public void test_03 ()
    {
        final String s = "D\u00F8";
        t(
            s,
            new ChordInfo(s, new Pitch(D), new Kind(HALF_DIMINISHED, "\u00F8")));
    }

    @Test
    public void test_04 ()
    {
        final String s = "Am6";
        t(s, new ChordInfo(s, new Pitch(A), new Kind(MINOR_SIXTH, "m6")));
    }

    @Test
    public void test_05 ()
    {
        final String s = "A/E";
        t(s, new ChordInfo(s, new Pitch(A), new Kind(MAJOR), new Pitch(E)));
    }

    @Test
    public void test_06 ()
    {
        final String s = "F#";
        t(s, new ChordInfo(s, new Pitch(F, 1), new Kind(MAJOR)));
    }

    @Test
    public void test_07 ()
    {
        final String s = "F#m7";
        t(s, new ChordInfo(s, new Pitch(F, 1), new Kind(MINOR_SEVENTH, "m7")));
    }

    @Test
    public void test_08 ()
    {
        final String s = "G6/D";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(G),
                new Kind(MAJOR_SIXTH, "6"),
                new Pitch(D)));
    }

    @Test
    public void test_09 ()
    {
        final String s = "A11";
        t(s, new ChordInfo(s, new Pitch(A), new Kind(DOMINANT_11_TH, "11")));
    }

    @Test
    public void test_10 ()
    {
        final String s = "G13";
        t(s, new ChordInfo(s, new Pitch(G), new Kind(DOMINANT_13_TH, "13")));
    }

    //    @Test
    //    public void test_77 ()
    //    {
    //        final String s = "A7(-5)";
    //        t(
    //                s,
    //                new ChordInfo(
    //                s,
    //                new Pitch(A),
    //                new Kind(DOMINANT, "7", true),
    //                new Degree(5, -1, alter)));
    //    }
    //
    //    @Test
    //    public void test_78 ()
    //    {
    //        final String s = "A7-5";
    //        t(
    //                s,
    //                new ChordInfo(
    //                s,
    //                new Pitch(A),
    //                new Kind(DOMINANT, "7", true),
    //                new Degree(5, -1, alter)));
    //    }
    @Test
    public void test_101 ()
    {
        final String s = "C.";
        t(s, null);
    }

    @Test
    public void test_11 ()
    {
        final String s = "C#";
        t(s, new ChordInfo(s, new Pitch(C, 1), new Kind(MAJOR)));
    }

    @Test
    public void test_12 ()
    {
        final String s = "Bb";
        t(s, new ChordInfo(s, new Pitch(B, -1), new Kind(MAJOR)));
    }

    @Test
    public void test_13 ()
    {
        final String s = "A" + SHARP;
        t(s, new ChordInfo(s, new Pitch(A, 1), new Kind(MAJOR)));
    }

    @Test
    public void test_14 ()
    {
        final String s = "A" + FLAT;
        t(s, new ChordInfo(s, new Pitch(A, -1), new Kind(MAJOR)));
    }

    @Test
    public void test_15 ()
    {
        final String s = "C/G";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MAJOR), new Pitch(G)));
    }

    @Test
    public void test_16 ()
    {
        final String s = "CMAJ";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MAJOR, "MAJ")));
    }

    @Test
    public void test_17 ()
    {
        final String s = "B" + DELTA;
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(B),
                new Kind(MAJOR_SEVENTH, DELTA, true)));
    }

    @Test
    public void test_18 ()
    {
        final String s = "Bmaj7";
        t(s, new ChordInfo(s, new Pitch(B), new Kind(MAJOR_SEVENTH, "maj7")));
    }

    @Test
    public void test_19 ()
    {
        final String s = "B" + DELTA + "7";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(B),
                new Kind(MAJOR_SEVENTH, DELTA + "7", true)));
    }

    @Test
    public void test_20 ()
    {
        final String s = "BMaj7/D#";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(B),
                new Kind(MAJOR_SEVENTH, "Maj7"),
                new Pitch(D, 1)));
    }

    @Test
    public void test_21 ()
    {
        final String s = "G#m9";
        t(s, new ChordInfo(s, new Pitch(G, 1), new Kind(MINOR_NINTH, "m9")));
    }

    @Test
    public void test_22 ()
    {
        final String s = "Asus2";
        t(
            s,
            new ChordInfo(s, new Pitch(A), new Kind(SUSPENDED_SECOND, "sus2")));
    }

    @Test
    public void test_23 ()
    {
        final String s = "ASUS2";
        t(
            s,
            new ChordInfo(s, new Pitch(A), new Kind(SUSPENDED_SECOND, "SUS2")));
    }

    @Test
    public void test_24 ()
    {
        final String s = "Dbsus4";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(D, -1),
                new Kind(SUSPENDED_FOURTH, "sus4")));
    }

    @Test
    public void test_25 ()
    {
        final String s = "Am";
        t(s, new ChordInfo(s, new Pitch(A), new Kind(MINOR, "m")));
    }

    @Test
    public void test_26 ()
    {
        final String s = "C#7sus4";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(C, 1),
                new Kind(SUSPENDED_FOURTH, "7sus4"),
                new Degree(7, 0, ADD)));
    }

    @Test
    public void test_27 ()
    {
        final String s = "A(9)";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(A),
                new Kind(MAJOR, "", false, true),
                new Degree(9, 0, ADD)));
    }

    @Test
    public void test_28 ()
    {
        final String s = "A7";
        t(s, new ChordInfo(s, new Pitch(A), new Kind(DOMINANT, "7")));
    }

    @Test
    public void test_29 ()
    {
        final String s = "A7(b5)";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(A),
                new Kind(DOMINANT, "7", false, true),
                new Degree(5, -1, ALTER)));
    }

    @Test
    public void test_30 ()
    {
        final String s = "Am7b13";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(A),
                new Kind(MINOR_SEVENTH, "m7b13"),
                new Degree(13, -1, ADD)));
    }

    @Test
    public void test_31 ()
    {
        final String s = "A(#9)";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(A),
                new Kind(MAJOR, "", false, true),
                new Degree(9, 1, ADD)));
    }

    @Test
    public void test_32 ()
    {
        final String s = "B7(#11b9)";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(B),
                new Kind(DOMINANT, "7", false, true),
                new Degree(11, 1, ADD),
                new Degree(9, -1, ADD)));
    }

    @Test
    public void test_33 ()
    {
        final String s = "Amin";
        t(s, new ChordInfo(s, new Pitch(A), new Kind(MINOR, "min")));
    }

    @Test
    public void test_34 ()
    {
        final String s = "A-";
        t(s, new ChordInfo(s, new Pitch(A), new Kind(MINOR, "-", true)));
    }

    @Test
    public void test_35 ()
    {
        final String s = "C+";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(AUGMENTED, "+", true)));
    }

    @Test
    public void test_36 ()
    {
        final String s = "Caug";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(AUGMENTED, "aug")));
    }

    @Test
    public void test_37 ()
    {
        final String s = "Ddim";
        t(s, new ChordInfo(s, new Pitch(D), new Kind(DIMINISHED, "dim")));
    }

    @Test
    public void test_38 ()
    {
        final String s = "Dm(b5)";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(D),
                new Kind(MINOR, "m", false, true),
                new Degree(5, -1, ALTER)));
    }

    @Test
    public void test_39 ()
    {
        final String s = "C";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MAJOR)));
    }

    @Test
    public void test_40 ()
    {
        final String s = "C°7";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(C),
                new Kind(DIMINISHED_SEVENTH, "°7", false, false)));
    }

    @Test
    public void test_41 ()
    {
        final String s = "C+7";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(C),
                new Kind(AUGMENTED_SEVENTH, "+7", true)));
    }

    @Test
    public void test_42 ()
    {
        final String s = "C\u00F8";
        t(
            s,
            new ChordInfo(s, new Pitch(C), new Kind(HALF_DIMINISHED, "\u00F8")));
    }

    @Test
    public void test_43 ()
    {
        final String s = "Cmin(maj7)";
        t(
            s,
            new ChordInfo(s, new Pitch(C), new Kind(MAJOR_MINOR, "min(maj7)")));
    }

    @Test
    public void test_44 ()
    {
        final String s = "Cmin(" + DELTA + "7)";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(C),
                new Kind(MAJOR_MINOR, "min(" + DELTA + "7)")));
    }

    @Test
    public void test_45 ()
    {
        final String s = "C-(" + DELTA + "7)";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(C),
                new Kind(MAJOR_MINOR, "-(" + DELTA + "7)", true)));
    }

    @Test
    public void test_46 ()
    {
        final String s = "Cm(M7)";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MAJOR_MINOR, "m(M7)")));
    }

    @Test
    public void test_47 ()
    {
        final String s = "C6";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MAJOR_SIXTH, "6")));
    }

    @Test
    public void test_48 ()
    {
        final String s = "C" + DELTA + "6";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(C),
                new Kind(MAJOR_SIXTH, DELTA + "6", true)));
    }

    @Test
    public void test_49 ()
    {
        final String s = "Cm6";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MINOR_SIXTH, "m6")));
    }

    @Test
    public void test_50 ()
    {
        final String s = "C-6";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MINOR_SIXTH, "-6", true)));
    }

    @Test
    public void test_51 ()
    {
        final String s = "C9";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(DOMINANT_NINTH, "9")));
    }

    @Test
    public void test_52 ()
    {
        final String s = "Cmaj9";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MAJOR_NINTH, "maj9")));
    }

    @Test
    public void test_53 ()
    {
        final String s = "Cmaj";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MAJOR, "maj")));
    }

    @Test
    public void test_54 ()
    {
        final String s = "C" + DELTA + "9";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(C),
                new Kind(MAJOR_NINTH, DELTA + "9", true)));
    }

    @Test
    public void test_55 ()
    {
        final String s = "Cm9";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MINOR_NINTH, "m9")));
    }

    @Test
    public void test_56 ()
    {
        final String s = "C-9";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MINOR_NINTH, "-9", true)));
    }

    @Test
    public void test_57 ()
    {
        final String s = "C11";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(DOMINANT_11_TH, "11")));
    }

    @Test
    public void test_58 ()
    {
        final String s = "Cmaj11";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MAJOR_11_TH, "maj11")));
    }

    @Test
    public void test_59 ()
    {
        final String s = "C" + DELTA + "11";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(C),
                new Kind(MAJOR_11_TH, DELTA + "11", true)));
    }

    @Test
    public void test_60 ()
    {
        final String s = "Cm11";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MINOR_11_TH, "m11")));
    }

    @Test
    public void test_61 ()
    {
        final String s = "C-11";
        t(
            s,
            new ChordInfo(s, new Pitch(C), new Kind(MINOR_11_TH, "-11", true)));
    }

    @Test
    public void test_62 ()
    {
        final String s = "C13";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(DOMINANT_13_TH, "13")));
    }

    @Test
    public void test_63 ()
    {
        final String s = "Cmaj13";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MAJOR_13_TH, "maj13")));
    }

    @Test
    public void test_64 ()
    {
        final String s = "Cm";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MINOR, "m")));
    }

    @Test
    public void test_65 ()
    {
        final String s = "C" + DELTA + "13";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(C),
                new Kind(MAJOR_13_TH, DELTA + "13", true)));
    }

    @Test
    public void test_66 ()
    {
        final String s = "Cm13";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MINOR_13_TH, "m13")));
    }

    @Test
    public void test_67 ()
    {
        final String s = "C-13";
        t(
            s,
            new ChordInfo(s, new Pitch(C), new Kind(MINOR_13_TH, "-13", true)));
    }

    @Test
    public void test_68 ()
    {
        final String s = "Csus2";
        t(
            s,
            new ChordInfo(s, new Pitch(C), new Kind(SUSPENDED_SECOND, "sus2")));
    }

    @Test
    public void test_69 ()
    {
        final String s = "Csus4";
        t(
            s,
            new ChordInfo(s, new Pitch(C), new Kind(SUSPENDED_FOURTH, "sus4")));
    }

    @Test
    public void test_70 ()
    {
        final String s = "C-";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MINOR, "-", true)));
    }

    @Test
    public void test_71 ()
    {
        final String s = "C°";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(DIMINISHED, "°")));
    }

    @Test
    public void test_72 ()
    {
        final String s = "C7";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(DOMINANT, "7")));
    }

    @Test
    public void test_73 ()
    {
        final String s = "Cmaj7";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MAJOR_SEVENTH, "maj7")));
    }

    @Test
    public void test_74 ()
    {
        final String s = "C" + DELTA;
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(C),
                new Kind(MAJOR_SEVENTH, DELTA, true)));
    }

    @Test
    public void test_75 ()
    {
        final String s = "Cm7";
        t(s, new ChordInfo(s, new Pitch(C), new Kind(MINOR_SEVENTH, "m7")));
    }

    @Test
    public void test_76 ()
    {
        final String s = "C-7";
        t(
            s,
            new ChordInfo(s, new Pitch(C), new Kind(MINOR_SEVENTH, "-7", true)));
    }

    // Chords from "The Girl from Ipanema"
    //------------------------------------
    @Test
    public void tgfi_01 ()
    {
        final String s = "A-7";
        t(
            s,
            new ChordInfo(s, new Pitch(A), new Kind(MINOR_SEVENTH, "-7", true)));
    }

    @Test
    public void tgfi_02 ()
    {
        final String s = "D-";
        t(s, new ChordInfo(s, new Pitch(D), new Kind(MINOR, "-", true)));
    }

//    @Test
//    public void tgfi_03 ()
//    {
//        final String s = "D-" + DELTA + "7";
//        t(
//            s,
//            new ChordInfo(
//                s,
//                new Pitch(D),
//                new Kind(MAJOR_MINOR, "-" + DELTA + "7", true)));
//    }
//
    @Test
    public void tgfi_04 ()
    {
        final String s = "D-7";
        t(
            s,
            new ChordInfo(s, new Pitch(D), new Kind(MINOR_SEVENTH, "-7", true)));
    }

    @Test
    public void tgfi_05 ()
    {
        final String s = "D-7/C";
        t(
            s,
            new ChordInfo(
                s,
                new Pitch(D),
                new Kind(MINOR_SEVENTH, "-7", true),
                new Pitch(C)));
    }

//    @Test
//    public void tgfi_06 ()
//    {
//        final String s = "B-7b5"; // half-diminished in fact?
//        t(
//            s,
//            new ChordInfo(
//                s,
//                new Pitch(B),
//                new Kind(MINOR_SEVENTH, "-7", true),
//                new Degree(5, -1, ALTER)));
//    }
//
//    @Test
//    public void tgfi_07 ()
//    {
//        final String s = "Bb" + DELTA + "7(#11)";
//        t(
//            s,
//            new ChordInfo(
//                s,
//                new Pitch(B, -1),
//                new Kind(MAJOR_SEVENTH, DELTA + "7", true),
//                new Degree(11, 1, ADD)));
//    }
//
    //
    //    @Test
    //    public void tgfi_08 ()
    //    {
    //        final String s = "Bbsus9";
    //        t(
    //            s,
    //            new ChordInfo(
    //                s,
    //                new Pitch(B, -1),
    //                new Kind(SUSPENDED_NINTH, "sus9")));
    //    }
    @Test
    public void tgfi_09 ()
    {
        final String s = "Eb13";
        t(
            s,
            new ChordInfo(s, new Pitch(E, -1), new Kind(DOMINANT_13_TH, "13")));
    }

//    @Test
//    public void tgfi_10 ()
//    {
//        final String s = "Db" + DELTA + "7(#11)";
//        t(
//            s,
//            new ChordInfo(
//                s,
//                new Pitch(D, -1),
//                new Kind(MAJOR_SEVENTH, DELTA + "7", true),
//                new Degree(11, 1, ADD)));
//    }
//
    //----------------//
    // getPrintWriter //
    //----------------//
    private static PrintWriter getPrintWriter (File file)
    {
        try {
            final BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), "UTF8"));

            return new PrintWriter(bw);
        } catch (Exception ex) {
            System.err.println("Error creating " + file + ex);

            return null;
        }
    }

    //---//
    // t //
    //---//
    private void t (String    text,
                    ChordInfo exp)
    {
        // Print method name
        StackTraceElement elem = ClassUtil.getCallingFrame();

        if (elem != null) {
            System.out.println();
            System.out.println("method   : " + elem.getMethodName());
            out.println();
            out.println("method   : " + elem.getMethodName());
            out.flush();
        }

        System.out.println("input    : " + text);
        out.println("input    : " + text);
        out.flush();

        System.out.println("expected : " + exp);
        out.println("expected : " + exp);
        out.flush();

        ChordInfo result = ChordInfo.create(text);
        System.out.println("output   : " + result);
        out.println("output   : " + result);
        out.flush();

        if (exp != null) {
            assertNotNull("Null result for text " + text, result);
            assertEquals(exp.toString(), result.toString());
        } else {
            assertNull(result);
        }
    }
}
