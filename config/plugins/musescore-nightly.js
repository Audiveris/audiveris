/* -------------------------------------------------------------------------- */
/*                                                                            */
/*                   m u s e s c o r e - n i g h t l y. j s                   */
/*                                                                            */
/* -------------------------------------------------------------------------- */

/* Variable to modify according to your environment */
//var pathToExec = "P:/MuseScore/bin/mscore.exe";
//var pathToExec = "C:/Program Files (x86)/MuseScore/bin/mscore.exe";
var pathToExec = "d:/soft/MuseScoreNightly/bin/nightly.exe";

/* Title for menu item */
pluginTitle = 'MuseScore Nightly';

/* Long description for tool tip */
pluginTip = 'Invoke MuseScore Nightly on score XML';

/* Build sequence of command line parameters */
function pluginCli(exportFilePath) {
    importPackage(java.util);
    return Arrays.asList([pathToExec, exportFilePath]);
}
