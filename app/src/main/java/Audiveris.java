//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       A u d i v e r i s                                        //
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

/**
 * Class <code>Audiveris</code> is simply a convenient entry point to OMR, which
 * delegates the call to {@link org.audiveris.omr.Main#main}.
 *
 * @author Hervé Bitteur
 */
public final class Audiveris
{
    //~ Constructors -------------------------------------------------------------------------------

    /** To avoid instantiation. */
    private Audiveris ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //------//
    // main //
    //------//
    /**
     * The main entry point, which just calls {@link org.audiveris.omr.Main#main}.
     *
     * @param args These arguments are simply passed to Main
     */
    public static void main (final String[] args)
    {
        org.audiveris.omr.Main.main(fixWindowsArgs(args));
    }

    //-----------------//
    // fixWindowsArgs  //
    //-----------------//
    /**
     * On Windows, the JVM launcher builds <code>args</code> by converting the real
     * (Unicode) command line into the process's legacy ANSI code page, <b>before</b>
     * the JVM even starts. Characters not representable in that code page (e.g. 'ă' on
     * a Western-European system) are silently best-fit mapped to a lookalike ASCII
     * character, which then makes file lookups fail.
     * <p>
     * This cannot be fixed via JVM properties such as <code>sun.jnu.encoding</code>,
     * since the corruption already happened at the OS/C-runtime level. Instead,
     * re-fetch the true Unicode command line straight from Windows itself
     * (<code>GetCommandLineW</code> + <code>CommandLineToArgvW</code>) and use its
     * trailing tokens &mdash; token count and order are unaffected by the ANSI
     * conversion, only the characters within a token are &mdash; as the real
     * arguments.
     *
     * @param args the (possibly corrupted) arguments as provided by the JVM
     * @return the corrected arguments on Windows, or the input unchanged otherwise
     *         or on any failure to retrieve the native command line
     */
    private static String[] fixWindowsArgs (final String[] args)
    {
        if ((args.length == 0)
                || !System.getProperty("os.name", "").toLowerCase().contains("windows")) {
            return args;
        }

        com.sun.jna.Pointer argv = null;

        try {
            final com.sun.jna.WString cmdLine = Kernel32.INSTANCE.GetCommandLineW();
            final com.sun.jna.ptr.IntByReference argc = new com.sun.jna.ptr.IntByReference();
            argv = Shell32.INSTANCE.CommandLineToArgvW(cmdLine, argc);

            if ((argv == null) || (argc.getValue() < args.length)) {
                return args;
            }

            final int offset = argc.getValue() - args.length;
            final String[] fixed = new String[args.length];

            for (int i = 0; i < args.length; i++) {
                final com.sun.jna.Pointer strPtr = argv.getPointer(
                        (long) (offset + i) * com.sun.jna.Native.POINTER_SIZE);
                fixed[i] = strPtr.getWideString(0);
            }

            return fixed;
        } catch (Throwable ex) {
            // JNA or native call unavailable/failed: fall back to the (possibly corrupted) args
            return args;
        } finally {
            if (argv != null) {
                Kernel32.INSTANCE.LocalFree(argv);
            }
        }
    }

    //~ Inner Interfaces -----------------------------------------------------------------------------

    /** Minimal JNA binding for the needed kernel32 entries. */
    private interface Kernel32 extends com.sun.jna.Library
    {
        Kernel32 INSTANCE = com.sun.jna.Native.load("kernel32", Kernel32.class);

        com.sun.jna.WString GetCommandLineW ();

        com.sun.jna.Pointer LocalFree (com.sun.jna.Pointer hMem);
    }

    /** Minimal JNA binding for the needed shell32 entries. */
    private interface Shell32 extends com.sun.jna.Library
    {
        Shell32 INSTANCE = com.sun.jna.Native.load("shell32", Shell32.class);

        com.sun.jna.Pointer CommandLineToArgvW (
                com.sun.jna.WString cmdLine,
                com.sun.jna.ptr.IntByReference pNumArgs);
    }
}
