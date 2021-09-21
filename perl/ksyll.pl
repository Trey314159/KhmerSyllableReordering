#! /usr/bin/perl

# A simple command-line driver for line-by-line KhmerSyllableReordering of a
# text file.

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

# tell perl to look for KhmerSyllableReorder.pm in this directory
use FindBin 1.51 qw( $RealBin );
use lib $RealBin;

use KhmerSyllableReorder;

use open qw(:std :utf8);
binmode( STDOUT, ':encoding(UTF-8)' );

my $FILE;
open $FILE, '<', $ARGV[0];
while ( my $line = <$FILE> ) {
    print KhmerSyllableReorder::reorderText($line);
}
close $FILE;

