---
layout: default
title: SIG
grand_parent: Main Features
parent: Main Entities
nav_order: 4
---
### Symbol Interpretation Graph

#### Relation

A Relation instance formalizes a relationship between a source Inter instance and a (different)
target Inter instance.

There are 2 main kinds of relation:
* A positive relation represents a **mutual support** between two Inter instances.  
![](../assets/images/head_stem.png)
Here is a typical example: a black head interpretation and a stem interpretation nearby with a
suitable _HeadStemRelation_ between them, shown as a "_HeadStem_"-labelled green segment
(the "_Relation_" suffix is always omitted in relation name display).  
Support relations _increase_ the contextual grade of their linked Inter instances.
Doing so, even rather low-quality interpretations, when well combined through supporting relations,
may end up with acceptable contextual grade.

* A negative relation represents a **mutual exclusion** between two Inter instances.  
It tells that the two Inter instances cannot coexist in the final configuration, so at least one of
them will disappear at some point in the transcription process.

The image below shows a higher level of relation:
* We can see two Inter instances (a treble clef followed by a 2-flat key) linked by a supporting
_ClefKeyRelation_ instance.  
* This _ClefKeyRelation_ exists because the vertical positions (pitches) of the flat signs
configuration in this key (B then E) are compatible with a treble clef.
* If ever there was a competing bass clef candidate, there would be of course an _ExclusionRelation_
between the two competing clef candidates, and also another _ExclusionRelation_ between the bass
clef and this key.

![](../assets/images/clef_key.png)

#### SIG

A **S**ymbol **I**nterpretation **G**raph (SIG), is simply a graph with Inter instances as vertices
and Relation instances as edges.

The SIG plays the central role in Audiveris V5.
Its main purpose is to host all candidate Interpretations and to formalize and manage the Relations
(mutual exclusions, supporting relations) within the population of candidate interpretations.

There is one SIG per system, and at some points in OMR pipeline (typically the REDUCTION step and
the LINKS step), the SIG is _reduced_ so that all exclusions are resolved and no Inter with weak
contextual grade remains in its graph.
