---
---
## Essential folders

This is where Audiveris stores user-specific essential parameters:

Under Windows notably, these are _hidden locations_ by default.  
Please do not create or modify these files, unless you are an advanced user and
know what you are doing.

### Config folder

Audiveris defines a `CONFIG_FOLDER` for configuration files
- run.properties (user-modified application options)
- logback.xml (logging configuration)
- plugins.xml (definition of plugins to external programs)
- user-actions.xml (additional GUI actions)

|  OS | `CONFIG_FOLDER` |
| --- | --- |
| **Windows** | %APPDATA%\\AudiverisLtd\\audiveris\\config |
| **Linux** (choice #1)| $XDG_CONFIG_HOME/AudiverisLtd/audiveris |
| **Linux** (choice #2)| $HOME/.config/AudiverisLtd/audiveris |
| **MacOS** | $HOME/Library/Application Support/AudiverisLtd/audiveris |

### Train folder

There is a `TRAIN_FOLDER` that can be populated with user-specific training
material and trained model to override default Audiveris model.
  - basic-classifier.zip (trained model and norms for glyph classifier)
  - samples.zip (global repository of training samples)
  - images.zip (background sheet images of training samples)

``TRAIN_FOLDER`` is defined as the direct `train` sub-folder of `CONFIG_FOLDER`.
