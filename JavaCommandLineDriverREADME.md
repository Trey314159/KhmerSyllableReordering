# Khmer Syllable Reordering Command-Line Driver (Java)

April 2021

By [Trey Jones](https://github.com/Trey314159).

## Background

This is a very minimal command-line driver for the Khmer syllable reordering
class. It only takes one file name on the command line and writes the rewritten
text to `STDOUT`.

## Build & Run

If you have Maven installed, you can build the self-contained Khmer syllable
reordering driver by running `mvn clean package` from the `java/` directory of
the repository. The self-contained executable jar file will be built in
`target/khmerSyllableReordering-1.0-jar-with-dependencies.jar`.

You can run the executable jar from the command line with the command:

	java -jar target/khmerSyllableReordering-1.0-jar-with-dependencies.jar <input_file>

