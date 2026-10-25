# CustomLang Lexical Analyzer

The first assignment for Compiler Construction (CS4031, FAST-NUCES, Spring 2026), by Saad Lone and Mobeen Hassan.

We defined a small language called CustomLang and wrote a scanner for it twice: once by hand as a DFA in Java, and once generated with JFlex. Then we ran both on the same test files and compared the output.

## The language

The token rules are deliberately strict, so the scanner has edge cases to handle:

| Token | Rule | Examples |
|---|---|---|
| Identifier | Starts with an uppercase letter, then lowercase letters, digits or `_`. Max 31 characters. | `Counter_123`, `A` |
| Integer | Optional sign, then digits | `42`, `-567`, `+0` |
| Float | Optional sign, digits, `.`, 1–6 decimal digits, optional exponent | `3.14`, `-0.123456`, `2.0E-3` |
| Boolean | `true` / `false` (the only reserved words) | |
| Arithmetic | `+ - * / %` | |
| Relational | `< <= > >= == !=` | |
| Comment | `##` to end of line | `## like this` |

The full spec, with the grammar, precedence table and sample programs, is in [`docs/README.md`](docs/README.md) and [`docs/LanguageGrammar.txt`](docs/LanguageGrammar.txt).

## How it works

**Manual scanner** (`src/ManualScanner.java`). A hand-coded 32-state DFA, written as one transition function. It always takes the longest possible match: it remembers the last accepting state it passed through and backs up to it when it hits a dead end.

If no token matches, it reports the bad character with its line and column, skips it, and carries on. One typo doesn't stop the scan.

Identifiers go into a symbol table (`src/SymbolTable.java`), which records each one's type, the line it first appears on and how often it's used.

**JFlex scanner** (`src/Scanner.flex`). The same rules as regular expressions, about 110 lines. JFlex builds the DFA from them (`src/JFlexScanner.java` is the generated output). It uses the same `Token`, `SymbolTable` and `ErrorHandler` classes as the manual scanner.

## Results

We wrote five test files in `tests/`: valid tokens, complex expressions, edge cases (the 31-character limit, signed zero, the 6-digit float limit), lexical errors, and a full sample program.

| Test | Manual | JFlex | Same token stream? |
|---|---|---|---|
| 1. Valid tokens | 36 | 36 | yes |
| 2. Expressions | 73 | 73 | yes |
| 3. Edge cases | 61 | 61 | yes |
| 4. Errors | 125 | 35 | yes, once error tokens are left out |
| 5. Full program | 91 | 91 | yes |

On every valid file the two scanners produce exactly the same tokens. The diffs in `scanner_comparison/` look bigger than they are, because the two print slightly different headers and stats.

In test 4, both scanners report the same 90 errors at the same line and column. The token counts differ only because the manual scanner also puts each bad character into the token stream as an `ERROR` token.

The hand-written DFA took about 800 lines and several hours. The JFlex spec took about 110 lines and an hour. Writing the DFA by hand is what makes you understand how scanners work. JFlex is what you'd actually use.

## Run it

You need JDK 11+, and JFlex only if you want to regenerate the JFlex scanner.

```bash
javac -d build src/*.java
java -cp build ManualScanner tests/test1.lang
java -cp build JFlexScanner  tests/test1.lang

# regenerate src/JFlexScanner.java from the spec
jflex -d src src/Scanner.flex
```

Each run prints the tokens, the symbol table, a count of each token type, and any errors. Saved outputs for all five tests are in `test_results/` (manual) and `jflex_results/` (JFlex).
