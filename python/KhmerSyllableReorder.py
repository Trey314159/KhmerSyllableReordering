#! /usr/bin/env python3
# -*- coding: utf-8 -*-

# MIT License
#
# Copyright (c) 2021 Trey Jones
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

import re


class KhmerSyllableReorder:

    CONS       = '[\u1780-\u17A2]'                    # consonants
    RO         = '\u179A'                             #   ro
    INDEPVOWEL = '[\u17A3-\u17B3]'                    # independent vowels
    DEPVOWEL   = '[\u17B6-\u17C5]'                    # dependent vowels
    COENG      = '\u17D2'                             # coeng
    DIACRITIC  = '[\u17C6-\u17D1\u17DD]'              # diacritics
    REGSHIFTER = '[\u17C9\u17CA]'                     #   register shifters
    ROBAT      = '\u17CC'                             #   robat
    ZEROWIDTH  = '[\u200B-\u200D\u00AD\u2063]'        #   zero-width elements
    SPACING    = '[\u17C7\u17C8]'                     #   spacing diacritics
    NONSPACING = '[\u17C6\u17CB\u17CD-\u17D1\u17DD]'  #   non-spacing diacritics

    CONS_OR_INDEPVOWEL = CONS + '|' + INDEPVOWEL
    COENG_RO = COENG + RO

    # A syllable is a consonant or independent vowel followed by any sequence
    # of coeng(s)+consonant clusters, coeng(s)+independent vowel clusters,
    # dependent vowels, diacritics, or zero-width elements.

    SYLLPAT = '(?:{0})(?:{1}+(?:{0})|(?:{2}|{3}|{4})+)*'.format(CONS_OR_INDEPVOWEL,
            COENG, DEPVOWEL, DIACRITIC, ZEROWIDTH)

    # A "coeng chunk" is a coeng (or several) plus a cons or indep vowel, and
    # an optional register shifter. Match a coeng chunk or individual
    # character.

    CHUNKPAT = '(?:{0}+(?:{1}){2}?)|.'.format(COENG, CONS_OR_INDEPVOWEL,
            REGSHIFTER)

    def __init__(self):
        pass

    def reorderText(self, text):
        """
        Reorder Khmer syllables in a chunk of text. Break the string into
        the orthographic syllables, and reorder them individually.

        Keyword arguments:
        text -- the input text
        """

        text = self.regularizeText(text)
        syll = re.split(r'(' + self.SYLLPAT + ')', text)
        syll = [self.reorderSyll(s) for s in syll if s]
        text = ''.join(syll)
        return text

    def reorderSyll(self, syll):
        """
        Reorder the individual characters in a single Khmer orthographic
        syllable.

        Keyword arguments:
        syll -- the input text
        """

        if len(syll) > 1 and re.match(self.CONS_OR_INDEPVOWEL, syll):
            base = syll[:1]
            chunks = re.split(r'(' + self.CHUNKPAT + ')', syll[1:])
            chunks = [c for c in chunks if c]

            dv = []
            coeng = []
            nonsp = []
            sp = []
            rs = []
            robat = []

            for c in chunks:
                if re.match(self.DEPVOWEL, c):
                    dv.append(c)
                elif re.match(self.COENG, c):
                    coeng.append(re.sub(r'' + self.COENG + r'+',
                                 self.COENG, c))
                elif re.match(self.NONSPACING, c):
                    nonsp.append(c)
                elif re.match(self.SPACING, c):
                    sp.append(c)
                elif re.match(self.REGSHIFTER, c):
                    rs.append(c)
                elif re.match(self.ROBAT, c):
                    robat.append(c)

            # re-order supplementary consonants (ro is always last)

            numCoeng = len(coeng) - 1
            for i in range(numCoeng):
                if coeng[i] and re.match(self.COENG_RO, coeng[i]):
                    coeng.append(coeng[i])
                    coeng[i] = ''

            # join the elements: base character + register shifters + robat +
            # coeng chunks + dependent vowels + non-spacing diacritics +
            # spacing diacritics

            chunks = [base] + rs + robat + coeng + dv + nonsp + sp

            for i in range(1, len(chunks)):
                if chunks[i] == chunks[i - 1]:
                    chunks[i - 1] = ''

            chunks = [c for c in chunks if c]
            syll = ''.join(chunks)

            syll = syll.replace('\u17C1\u17B8', '\u17BE')  # replace េ + ី with ើ
            syll = syll.replace('\u17B8\u17C1', '\u17BE')  # replace ី + េ with ើ
            syll = syll.replace('\u17C1\u17B6', '\u17C4')  # replace េ + ា  w/ ោ

            return syll
        return syll

    def regularizeText(self, text):
        """
        Replace or delete obsolete, deprecated, invisible, and variant
        characters.

        Keyword arguments:
        text -- the input text
        """

        text = text.replace('\u17A8', '\u17A7\u1780')  # replace obsolete ligature ឨ with ឧក
        text = text.replace('\u17A3', '\u17A2')        # replace deprecated independent vowel ឣ with អ
        text = text.replace('\u17A4', '\u17A2\u17B6')  # replace deprecated independent vowel digraph ឤ with អា
        text = text.replace('\u17B2', '\u17B1')        # replace ឲ as a variant of ឱ
        text = text.replace('\u17B4', '')              # delete non-visible inherent vowel (឴)
        text = text.replace('\u17B5', '')              # delete non-visible inherent vowel (឵)
        text = text.replace('\u17DD', '\u17D1')        # replace obsolete ATTHACAN ៝ with VIRIAM ៑
        text = text.replace('\u17D3', '\u17C6')        # replace deprecated BATHAMASAT ៓ with NIKAHIT ំ as error
        text = text.replace('\u17D8', '\u17D4\u179B\u17D4')  # replace deprecated trigraph ៘ with ។ល។
        return text
