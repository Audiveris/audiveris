#!/bin/sh

# Simple tool for automatic generation of plugin scripts

echo "Welcome to Audiveris plugin generation tool v1!"
printf "%b\n"

if [ -z "$XDG_CONFIG_HOME" ]; then
    export XDG_CONFIG_HOME="$HOME/.config"
fi

# Ensure that Audiveris plugins folder exists
AUDIVERIS_PLUGINS_FOLDER="$XDG_CONFIG_HOME/AudiverisLtd/audiveris/plugins"
mkdir -p "$AUDIVERIS_PLUGINS_FOLDER"
cd "$AUDIVERIS_PLUGINS_FOLDER"

# Ask the user for parameters
read -p "Please enter plugin's filename (example: musescore):" -e PLUG_NAME
read -p "Please enter tool's execution command (example: mscore):" -e PLUG_COM
read -p "Please enter plugin's title (example: MuseScore):" -e PLUG_TITLE
read -p "Please enter a short plugin description:" -e PLUG_DESC

printf "%b\n" "Thank you! Generating script..."

# Generate the basic plugin script
PLUG_SCRIPT="$AUDIVERIS_PLUGINS_FOLDER/$PLUG_NAME.js"
echo 'var pathToExec = "'$PLUG_COM'";' > $PLUG_SCRIPT
echo "pluginTitle = '$PLUG_TITLE';" >> $PLUG_SCRIPT
echo "pluginTip = '$PLUG_DESC';" >> $PLUG_SCRIPT
echo "function pluginCli(exportFilePath) {" >> $PLUG_SCRIPT
echo "    importPackage(java.util);" >> $PLUG_SCRIPT
echo "    return Arrays.asList([pathToExec, exportFilePath]);" >> $PLUG_SCRIPT
echo "}" >> $PLUG_SCRIPT

echo "Done."
