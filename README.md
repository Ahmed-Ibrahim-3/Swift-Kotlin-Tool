# Swift & Kotlin IDE

A Java program to run Swift or Kotlin Code

***This has only been tested on MacOS so far***

--- 

## Instructions 
Instructions for use: 
1. Download the [Swift compiler](https://www.swift.org/install/) for your platform
2. Download the [Kotlin compiler](https://kotlinlang.org/docs/command-line.html) for your platform
3. Clone or otherwise copy the contents of this repo
4. Run Main.java by either
   * Opening the project in your preferred IDE and clicking `Run`
   * In a terminal, navigate to `\src` and run `javac Main.java` followed by `java Main`
5. You should now see the GUI to enter and run code.

### Side notes

This has been built initially for Swift (with Kotlin being added soon after). It should be relatively easy to continue 
to extend to more languages by creating more `__Runner` classes inheriting from the `ScriptRunner` interface. Similarly, syntax highlighting
for languages can be added by creating more `__Highlighter` classes inheriting from the `ScriptHighlighter` abstract class. 

If more keyword types are to be added to a highlighter, such changes need to be reflected in `ScriptHighlighter` to account 
for this change. 

Languages with a different way of comments, e.g. python, may require a change in how `ScriptHighlighter` finds comments to be properly 
implemented. 