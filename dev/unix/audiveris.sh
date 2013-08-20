#!/bin/sh

# Run Audiveris with default or specific memory parameters
# (uncomment the chosen line and update the parameters as you wish)
# see https://audiveris.kenai.com/docs/manual/handbook.html#jvm-arguments

# default
LC_ALL=C javaws https://audiveris.kenai.com/jnlp/launch.jnlp

# specific
#LC_ALL=C javaws -J-Xms512m -J-Xmx1024m https://audiveris.kenai.com/jnlp/launch.jnlp

