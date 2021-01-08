# License

See the included LICENSE file for details of the project's licence. All
code submitted to this project must be compatible with this licence and all
source files must reproduce the copyright notice comment which is included
in other files of the project (see Main.java for an example).

This is a free and open source project and will remain so.

# Code Style

* Line Length: 120 characters + newline.
* Tab Wdith: 4 spaces (no tab characters)
* Brace Style: On the same line as the preceding close parens
* else & else if: On the same line as the preceding close brace. (i.e. `} else if (condition()) {`)
* Class structure: copyright header -> imports -> class body -> static fields -> member fields -> constructor -> public methods -> private methods
* In general, favour simplicity always, if in doubt see how it's done in other files (don't be afraid to make mistakes or ask questions).
* breaking long lines:

```java
public class foo
	<MANY, GENERIC, PARAMETERS>
	implements a, whole, lot, of, interfaces {

	public static void functionWithLongName (
		boolean param1,
		boolean param2,
		boolean param3,
		boolean param4
	) {

		if(
			param1 && param2 && param3 && param 4
		) {
			doSomething();
		}

		boolean bar = param1
			&& param2
			&& param3
			&& param4;

		/* Favor making a temporary variable or function instead of
                   line breaking a conditional. */
		if(bar) {
			doSomething();
		}
	}
}
```

## Javascript

* Json: quoted field names
* Strings: single quote by default, double only if string contain's a single quote
* Semicolons: after each statement
* Identifiers: snake case

## Java
* Dependencies: minimize as much as possible, avoid all utility libraries. Check the util package for helpful utility code and add to as appropriate.
* OOP: we do not do OOP (classes are fine but use static functions is encouraged where a more imperitive style would work better).
* Identifiers: camelCase, Captilize classes, ALL\_CAPS for constants

# Submitting Code

Submit as a pull request or email patches to cf1af460-ee27-4c2f-859f-852ca2b1ed98@protonmail.com.
If you would like to help out beyond a patch or two, email a request to be added
to the project trello.

# Bugs

Report on github.

