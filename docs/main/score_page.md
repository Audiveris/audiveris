### Score vs Page

Looking at the musical content of a given sheet image, we can observe that staves are often gathered
into systems (in which staves are played in sync), and that a given sheet image generally contains
several systems (played one after the other).

A system may be left-indented with respect to the other systems in the image, to indicate
the beginning of a movement.
A non-indented system is assumed to belong to the same movement as the previous system
(located just above in current sheet or at the end of the previous sheet).

In Audiveris, this logical containment is modeled as one instance of **Score** per movement
(since "Score" is the word used by MusicXML), the score containing a sequence of one or several
**Page** instances.

Generally, there is exactly one page per sheet, except when:
* The sheet is _invalid_ (containing no music) and thus contains no page.
* An indented system appears in the middle of the sheet, thus beginning a new page.
The sheet then contains two (or more) pages.

Note that an _invalid_ sheet is considered as a score break:
It ends the current score and the next _valid_ sheet, if any, will start another score.
