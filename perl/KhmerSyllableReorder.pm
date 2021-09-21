package KhmerSyllableReorder;

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
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

use strict;
use warnings;
use utf8;

my $CONS       = "[\x{1780}-\x{17A2}]";                 # consonants
my $RO         = "\x{179A}";                            #   ro
my $INDEPVOWEL = "[\x{17A3}-\x{17B3}]";                 # independent vowels
my $DEPVOWEL   = "[\x{17B6}-\x{17C5}]";                 # dependent vowels
my $COENG      = "\x{17D2}";                            # coeng
my $DIACRITIC  = "[\x{17C6}-\x{17D1}\x{17DD}]";         # diacritics
my $REGSHIFTER = "[\x{17C9}\x{17CA}]";                  #   register shifters
my $ROBAT      = "\x{17CC}";                            #   robat
my $ZEROWIDTH  = "[\x{200B}-\x{200D}\x{00AD}\x{2063}]"; #   zero-width elements
my $SPACING    = "[\x{17C7}\x{17C8}]";                  #   spacing diacritics
my $NONSPACING =
    "[\x{17C6}\x{17CB}\x{17CD}-\x{17D1}\x{17DD}]";      #   non-spacing diacritics

# A syllable is a consonant or independent vowel followed by any sequence of
# coeng(s)+consonant clusters, coeng(s)+independent vowel clusters, dependent
# vowels, diacritics, or zero-width elements.
my $SYLLPAT =
    "(?:$CONS|$INDEPVOWEL)(?:$COENG+(?:$CONS|$INDEPVOWEL)|(?:$DEPVOWEL|$DIACRITIC|$ZEROWIDTH)+)*";

# A "coeng chunk" is a coeng (or several) plus a cons or indep vowel, and an
# optional register shifter. Match a coeng chunk or individual character.
my $CHUNKPAT = "(?:$COENG+(?:$CONS|$INDEPVOWEL)$REGSHIFTER?)|.";

sub reorderText {
    my ($text) = @_;
    $text = regularizeText($text);
    my @syll = split( /($SYLLPAT)/o, $text );
    $text = join( '', map { reorderSyll($_) } grep {$_} @syll );
    return $text;
}

sub reorderSyll {
    my ($syll) = @_;
    if ( $syll =~ /^(?:$CONS|$INDEPVOWEL)/o ) {
        my $base = substr( $syll, 0, 1 );
        my @chunks = grep {$_} split( /($CHUNKPAT)/o, substr( $syll, 1 ) );

        my @coeng = ();
        my @dv    = ();
        my @rs    = ();
        my @robat = ();
        my @nonsp = ();
        my @sp    = ();

        foreach my $c (@chunks) {
            if ( $c =~ /^$DEPVOWEL/o ) {
                push @dv, $c;
            }
            elsif ( $c =~ /^$COENG/o ) {
                $c =~ s/$COENG+/$COENG/og;
                push @coeng, $c;
            }
            elsif ( $c =~ /^$NONSPACING/o ) {
                push @nonsp, $c;
            }
            elsif ( $c =~ /^$SPACING/o ) {
                push @sp, $c;
            }
            elsif ( $c =~ /^$REGSHIFTER/o ) {
                push @rs, $c;
            }
            elsif ( $c eq $ROBAT ) {
                push @robat, $c;
            }
        }

        # re-order supplementary consonants (ro is always last)
        my $numCoeng = scalar(@coeng) - 2;
        foreach my $i ( 0 .. $numCoeng ) {
            if ( $coeng[$i] =~ /^$COENG$RO/o ) {
                push @coeng, $coeng[$i];
                $coeng[$i] = '';
            }
        }

        # join the elements: base character + register shifters + robat +
        # coeng chunks + dependent vowels + non-spacing diacritics +
        # spacing diacritics
        @chunks = ( $base, @rs, @robat, @coeng, @dv, @nonsp, @sp );

        foreach my $i ( 1 .. $#chunks ) {
            if ( $chunks[$i] eq $chunks[ $i - 1 ] ) {
                $chunks[ $i - 1 ] = '';
            }
        }

        $syll = join( '', grep {$_} @chunks );

        $syll =~ s/\x{17C1}\x{17B8}/\x{17BE}/g;  # replace េ + ី with ើ
        $syll =~ s/\x{17B8}\x{17C1}/\x{17BE}/g;  # replace ី + េ with ើ
        $syll =~ s/\x{17C1}\x{17B6}/\x{17C4}/g;  # replace េ + ា  with ោ
    }

    return $syll;
}

sub regularizeText {
    my ($text) = @_;
    $text =~ s/\x{17A8}/\x{17A7}\x{1780}/g;    # replace obsolete ligature ឨ with ឧក
    $text =~ s/\x{17A3}/\x{17A2}/g;            # replace deprecated independent vowel ឣ with អ
    $text =~ s/\x{17A4}/\x{17A2}\x{17B6}/g;    # replace deprecated independent vowel digraph ឤ with អា
    $text =~ s/\x{17B2}/\x{17B1}/g;            # replace ឲ as a variant of ឱ
    $text =~ s/\x{17D8}/\x{17D4}\x{179B}\x{17D4}/g; # replace deprecated trigraph ៘ with ។ល។
    $text =~ s/[\x{17B4}\x{17B5}]//g;          # delete non-visible inherent vowels (឴) and (឵)
    $text =~ s/\x{17DD}/\x{17D1}/g;            # replace obsolete ATTHACAN ៝ with VIRIAM ៑
    $text =~ s/\x{17D3}/\x{17C6}/g;            # replace deprecated BATHAMASAT ៓ with NIKAHIT ំ as error
    return $text;
}

1;
