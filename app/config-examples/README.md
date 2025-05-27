
# config-examples

Strictly speaking, the files in this folder are not used by the application.

They are provided here solely as *examples* for the end user.  
To be effective, an example file should be copied
(keeping the same name but with its content generally customized)
from this `config-examples` folder to the user `config` folder,
where it will be detected at runtime by the Audiveris application.

The precise location of the user `config` folder depends on the operating system.  
Please refer to the handbook [Config folder][user-config] section.

## Relevant files

### `plugins.xml`

This file defines one or several `plugins` for the Audiveris application.

The purpose of a `plugin` is to ease the handover of some MusicXML data
from Audiveris application (the producer)
to an external program such as `Finale` or `MuseScore` (the consumer).

Please refer to the handbook [Plugins][plugins] section.

### `user-actions.xml`

This file plays a role similar to the `system-actions.xml` file located in the `res` folder (which is always loaded at runtime).

The `system-actions.xml` file defines the various actions provided in the user interface and their gathering into pulldown menus.

A `user-actions.xml` file, if found in the user config folder, would define
additional actions and their insertion in the pulldown menus.



[plugins]:      https://audiveris.github.io/audiveris/_pages/guides/advanced/plugins/
[user-config]:  https://audiveris.github.io/audiveris/_pages/reference/folders/essential/#config-folder
