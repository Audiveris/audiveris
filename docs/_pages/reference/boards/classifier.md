---
layout: default
title: Classifier board
parent: Boards
nav_order: 8
---
# Basic Classifier board

![](../../assets/images/basic_classifier_board.png)

In current Audiveris version, the basic classifier is a Glyph classifier.

If a glyph is selected by whatever means, it is automatically submitted to the classifier
and the top 5 shapes are displayed as active buttons, ordered by decreasing grade value.

Notice that the sum of grade values may exceed 1.0 (because no SoftMax filter is applied).

A simple click on a shape button assigns the related shape to the glyph at hand.
(To be strictly correct, an `Inter` instance is created with proper shape and glyph).
