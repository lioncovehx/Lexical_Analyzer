import java.io.*;
import java.util.*;

/**
 * Lexical analyzer using a 32-state DFA
 * Recognizes identifiers, numbers, operators, comments, and booleans
 */
public class ManualScanner {
    
    // DFA States
    private static final int q0 = 0;   // START
    private static final int q1 = 1;   // IDENTIFIER
    private static final int q2 = 2;   // PLUS
    private static final int q3 = 3;   // MINUS
    private static final int q4 = 4;   // INTEGER
    private static final int q5 = 5;   // DOT
    private static final int q6 = 6;   // FLOAT
    private static final int q7 = 7;   // EXP_E
    private static final int q8 = 8;   // EXP_SIGN
    private static final int q9 = 9;   // FLOAT_EXP
    private static final int q10 = 10; // HASH
    private static final int q11 = 11; // COMMENT
    private static final int q12 = 12; // t
    private static final int q13 = 13; // tr
    private static final int q14 = 14; // tru
    private static final int q15 = 15; // true
    private static final int q16 = 16; // f
    private static final int q17 = 17; // fa
    private static final int q18 = 18; // fal
    private static final int q19 = 19; // fals
    private static final int q20 = 20; // false
    private static final int q21 = 21; // LT
    private static final int q22 = 22; // LE
    private static final int q23 = 23; // GT
    private static final int q24 = 24; // GE
    private static final int q25 = 25; // EQ
    private static final int q26 = 26; // EQEQ
    private static final int q27 = 27; // NOT
    private static final int q28 = 28; // NE
    private static final int q29 = 29; // STAR
    private static final int q30 = 30; // SLASH
    private static final int q31 = 31; // MOD
    private static final int ERROR = -1;
    
    private String input;
    private int currentPos;
    private int line;
    private int column;
    private int tokenStartLine;
    private int tokenStartColumn;
    
    private List<Token> tokens;
    private SymbolTable symbolTable;
    private ErrorHandler errorHandler;
    private Map<TokenType, Integer> tokenCounts;
    private int commentCount;
    
    public ManualScanner(String filename) throws IOException {
        this.input = readFile(filename);
        this.currentPos = 0;
        this.line = 1;
        this.column = 1;
        this.tokens = new ArrayList<>();
        this.symbolTable = new SymbolTable();
        this.errorHandler = new ErrorHandler();
        this.tokenCounts = new HashMap<>();
        this.commentCount = 0;
    }
    
    private String readFile(String filename) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }
        return content.toString();
    }
    
    public void scan() {
        while (currentPos < input.length()) {
            if (skipWhitespace()) {
                continue;
            }
            
            if (currentPos >= input.length()) {
                break;
            }
            
            Token token = getNextToken();
            
            if (token != null) {
                if (token.getType() == TokenType.COMMENT) {
                    commentCount++;
                }
                
                tokens.add(token);
                
                TokenType type = token.getType();
                tokenCounts.put(type, tokenCounts.getOrDefault(type, 0) + 1);
                
                if (type == TokenType.IDENTIFIER) {
                    symbolTable.addSymbol(token.getLexeme(), "IDENTIFIER", token.getLine());
                }
            }
        }
        
        tokens.add(new Token(TokenType.EOF, "", line, column));
    }
    
    private boolean skipWhitespace() {
        if (currentPos >= input.length()) {
            return false;
        }
        
        char c = input.charAt(currentPos);
        if (Character.isWhitespace(c)) {
            if (c == '\n') {
                line++;
                column = 1;
            } else if (c == '\t') {
                column += 4;
            } else {
                column++;
            }
            currentPos++;
            return true;
        }
        return false;
    }
    
    private Token getNextToken() {
        tokenStartLine = line;
        tokenStartColumn = column;
        int tokenStartPos = currentPos;
        
        int state = q0;
        int lastFinalState = ERROR;
        int lastFinalPos = currentPos;
        int lastFinalLine = line;
        int lastFinalColumn = column;
        int lastFinalLength = 0;
        int fractionDigits = 0;
        StringBuilder lexeme = new StringBuilder();
        
        while (currentPos < input.length()) {
            char c = input.charAt(currentPos);
            
            int nextState = transition(state, c);
            // Floats allow at most 6 digits after the point
            if (state == q6 && Character.isDigit(c) && fractionDigits == 6) {
                nextState = ERROR;
            }
            
            if (nextState == ERROR) {
                if (lastFinalState != ERROR) {
                    // Back up to the end of the longest match
                    currentPos = lastFinalPos;
                    line = lastFinalLine;
                    column = lastFinalColumn;
                    return makeToken(lastFinalState, lexeme.substring(0, lastFinalLength));
                } else {
                    if (lexeme.length() > 0) {
                        char errorChar = lexeme.charAt(0);
                        errorHandler.reportInvalidCharacter(tokenStartLine, tokenStartColumn, errorChar);
                        
                        currentPos = tokenStartPos + 1;
                        if (errorChar == '\n') {
                            line++;
                            column = 1;
                        } else {
                            column = tokenStartColumn + 1;
                        }
                        
                        return new Token(TokenType.ERROR, String.valueOf(errorChar), 
                                       tokenStartLine, tokenStartColumn);
                    } else {
                        errorHandler.reportInvalidCharacter(tokenStartLine, tokenStartColumn, c);
                        currentPos++;
                        if (c == '\n') {
                            line++;
                            column = 1;
                        } else {
                            column++;
                        }
                        return new Token(TokenType.ERROR, String.valueOf(c), 
                                       tokenStartLine, tokenStartColumn);
                    }
                }
            }
            
            lexeme.append(c);
            currentPos++;
            if (c == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            
            if (nextState == q6) {
                fractionDigits = (state == q5) ? 1 : fractionDigits + 1;
            }
            state = nextState;
            
            if (isFinalState(state)) {
                lastFinalState = state;
                lastFinalPos = currentPos;
                lastFinalLine = line;
                lastFinalColumn = column;
                lastFinalLength = lexeme.length();
            }
            
            if (state == q1 && lexeme.length() >= 31) {
                return makeToken(q1, lexeme.toString());
            }
        }
        
        if (isFinalState(state)) {
            return makeToken(state, lexeme.toString());
        } else if (lastFinalState != ERROR) {
            currentPos = lastFinalPos;
            line = lastFinalLine;
            column = lastFinalColumn;
            return makeToken(lastFinalState, lexeme.substring(0, lastFinalLength));
        } else {
            if (lexeme.length() > 0) {
                errorHandler.reportIncompleteToken(tokenStartLine, tokenStartColumn, 
                                                  lexeme.toString(), "complete token");
                return new Token(TokenType.ERROR, lexeme.toString(), 
                               tokenStartLine, tokenStartColumn);
            }
            return null;
        }
    }
    
    private int transition(int state, char c) {
        switch (state) {
            case q0: return transition_q0(c);
            case q1: return transition_q1(c);
            case q2: return transition_q2(c);
            case q3: return transition_q3(c);
            case q4: return transition_q4(c);
            case q5: return transition_q5(c);
            case q6: return transition_q6(c);
            case q7: return transition_q7(c);
            case q8: return transition_q8(c);
            case q9: return transition_q9(c);
            case q10: return transition_q10(c);
            case q11: return transition_q11(c);
            case q12: return transition_q12(c);
            case q13: return transition_q13(c);
            case q14: return transition_q14(c);
            case q15: return ERROR;
            case q16: return transition_q16(c);
            case q17: return transition_q17(c);
            case q18: return transition_q18(c);
            case q19: return transition_q19(c);
            case q20: return ERROR;
            case q21: return transition_q21(c);
            case q22: return ERROR;
            case q23: return transition_q23(c);
            case q24: return ERROR;
            case q25: return transition_q25(c);
            case q26: return ERROR;
            case q27: return transition_q27(c);
            case q28: return ERROR;
            case q29: return ERROR;
            case q30: return ERROR;
            case q31: return ERROR;
            default: return ERROR;
        }
    }
    
    private int transition_q0(char c) {
        if (Character.isUpperCase(c)) return q1;
        if (c == '+') return q2;
        if (c == '-') return q3;
        if (Character.isDigit(c)) return q4;
        if (c == '#') return q10;
        if (c == 't') return q12;
        if (c == 'f') return q16;
        if (c == '<') return q21;
        if (c == '>') return q23;
        if (c == '=') return q25;
        if (c == '!') return q27;
        if (c == '*') return q29;
        if (c == '/') return q30;
        if (c == '%') return q31;
        return ERROR;
    }
    
    private int transition_q1(char c) {
        if (Character.isLowerCase(c) || Character.isDigit(c) || c == '_') {
            return q1;
        }
        return ERROR;
    }
    
    private int transition_q2(char c) {
        if (Character.isDigit(c)) return q4;
        return ERROR;
    }
    
    private int transition_q3(char c) {
        if (Character.isDigit(c)) return q4;
        return ERROR;
    }
    
    private int transition_q4(char c) {
        if (Character.isDigit(c)) return q4;
        if (c == '.') return q5;
        return ERROR;
    }
    
    private int transition_q5(char c) {
        if (Character.isDigit(c)) return q6;
        return ERROR;
    }
    
    private int transition_q6(char c) {
        if (Character.isDigit(c)) return q6;
        if (c == 'e' || c == 'E') return q7;
        return ERROR;
    }
    
    private int transition_q7(char c) {
        if (c == '+' || c == '-') return q8;
        if (Character.isDigit(c)) return q9;
        return ERROR;
    }
    
    private int transition_q8(char c) {
        if (Character.isDigit(c)) return q9;
        return ERROR;
    }
    
    private int transition_q9(char c) {
        if (Character.isDigit(c)) return q9;
        return ERROR;
    }
    
    private int transition_q10(char c) {
        if (c == '#') return q11;
        return ERROR;
    }
    
    private int transition_q11(char c) {
        if (c != '\n') return q11;
        return ERROR;
    }
    
    private int transition_q12(char c) {
        if (c == 'r') return q13;
        return ERROR;
    }
    
    private int transition_q13(char c) {
        if (c == 'u') return q14;
        return ERROR;
    }
    
    private int transition_q14(char c) {
        if (c == 'e') return q15;
        return ERROR;
    }
    
    private int transition_q16(char c) {
        if (c == 'a') return q17;
        return ERROR;
    }
    
    private int transition_q17(char c) {
        if (c == 'l') return q18;
        return ERROR;
    }
    
    private int transition_q18(char c) {
        if (c == 's') return q19;
        return ERROR;
    }
    
    private int transition_q19(char c) {
        if (c == 'e') return q20;
        return ERROR;
    }
    
    private int transition_q21(char c) {
        if (c == '=') return q22;
        return ERROR;
    }
    
    private int transition_q23(char c) {
        if (c == '=') return q24;
        return ERROR;
    }
    
    private int transition_q25(char c) {
        if (c == '=') return q26;
        return ERROR;
    }
    
    private int transition_q27(char c) {
        if (c == '=') return q28;
        return ERROR;
    }
    
    private boolean isFinalState(int state) {
        return state == q1 || state == q2 || state == q3 || 
               state == q4 || state == q6 || state == q9 || 
               state == q11 || state == q15 || state == q20 ||
               state == q21 || state == q22 || state == q23 || 
               state == q24 || state == q26 || state == q28 ||
               state == q29 || state == q30 || state == q31;
    }
    
    private Token makeToken(int finalState, String lexeme) {
        TokenType type = getTokenType(finalState);
        return new Token(type, lexeme, tokenStartLine, tokenStartColumn);
    }
    
    private TokenType getTokenType(int finalState) {
        switch (finalState) {
            case q1: return TokenType.IDENTIFIER;
            case q2:
            case q3:
            case q29:
            case q30:
            case q31: return TokenType.OPERATOR;
            case q4: return TokenType.INTEGER;
            case q6:
            case q9: return TokenType.FLOAT;
            case q11: return TokenType.COMMENT;
            case q15:
            case q20: return TokenType.BOOLEAN;
            case q21:
            case q22:
            case q23:
            case q24:
            case q26:
            case q28: return TokenType.RELATIONAL;
            default: return TokenType.ERROR;
        }
    }
    
    public void displayTokens() {
        System.out.println("\n========== TOKENS ==========");
        for (Token token : tokens) {
            if (token.getType() != TokenType.EOF) {
                System.out.println(token);
            }
        }
        System.out.println("============================\n");
    }
    
    public void displayStatistics() {
        System.out.println("\n========== STATISTICS ==========");
        System.out.println("Total tokens: " + (tokens.size() - 1));
        System.out.println("Lines processed: " + line);
        System.out.println("Comments removed: " + commentCount);
        System.out.println("\nToken counts by type:");
        
        for (Map.Entry<TokenType, Integer> entry : tokenCounts.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        
        System.out.println("================================\n");
    }
    
    public List<Token> getTokens() {
        return tokens;
    }
    
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
    
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ManualScanner <input-file>");
            System.exit(1);
        }
        
        try {
            ManualScanner scanner = new ManualScanner(args[0]);
            
            System.out.println("Scanning file: " + args[0]);
            scanner.scan();
            
            scanner.displayTokens();
            scanner.displayStatistics();
            scanner.getSymbolTable().display();
            scanner.getErrorHandler().displaySummary();
            
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
    }
}
