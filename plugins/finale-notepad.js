/* -------------------------------------------------------------------------- */
/*                                                                            */
/*                      f i n a l e - n o t e p a d . j s                     */
/*                                                                            */
/* -------------------------------------------------------------------------- */

/* Variable to modify according to your environment */
var pathToExec = "C:/Program Files (x86)/Finale NotePad 2012/Finale NotePad.exe";

/* Title for menu item */
pluginTitle = 'Finale Notepad';

/* Long description for tool tip */
pluginTip = 'Invoke Finale Notepad on score XML';

/* Build sequence of command line parameters */
function pluginCli(exportFilePath) {
    importPackage(java.util);
    return Arrays.asList([pathToExec, exportFilePath]);
}
