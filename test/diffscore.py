#!/usr/bin/env python
import sys
import music21
import logging
from collections import defaultdict, namedtuple


Note = namedtuple('Note', 'pitch offset duration')


def pianoroll(score):
    result = defaultdict(set)
    for measure in score.recurse().getElementsByClass('Measure'):
        measureBag = result[measure.number]
        for rest in measure.recurse().getElementsByClass('Rest'):
            measureBag.add(Note('rest', rest.offset, rest.quarterLength))
        for note in measure.recurse().getElementsByClass('Note'):
            measureBag.add(Note(note.pitch, note.offset, note.quarterLength))
        for chord in measure.recurse().getElementsByClass('Chord'):
            for pitch in chord.pitches:
                measureBag.add(Note(pitch, chord.offset, chord.quarterLength))
    return result


def diffMeasures(measure1, measure2):
    common = measure1 & measure2
    left = measure1 - measure2
    right = measure2 - measure1
    logging.debug('Common: {}, Left: {}, Right: {}'.format(common, left, right))
    score = len(common) - len(left) - len(right)
    logging.debug('Score:', score)
    return score


def diffRolls(roll1, roll2):
    score = 0
    barNumbers = sorted(set(roll1.keys()) | set(roll2.keys()))
    for n in barNumbers:
        logging.debug('Measure', n)
        score += diffMeasures(roll1.get(n, set()), roll2.get(n, set()))
    return score


def main():
    files = sys.argv[1:3]
    rolls = [pianoroll(music21.converter.parseFile(f)) for f in files]
    logging.info('Total score:', diffRolls(rolls[0], rolls[1]))


if __name__ == '__main__':
    main()
