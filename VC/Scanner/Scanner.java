/*+*
 ***
 *+*	Scanner.java                        
 ***
 *+*/

package VC.Scanner;

import java.util.ArrayList;
import java.util.List;

import VC.ErrorReporter;

public final class Scanner { 

  private SourceFile sourceFile;
  private boolean debug;

  private ErrorReporter errorReporter;
  private StringBuffer currentSpelling;
  private char currentChar;
  private SourcePosition sourcePos;
  
  private int lineStart, lineFinish; 
  private int charStart, charFinish;
  private int charLine, charCount; 
  private boolean flag = true;
  
  private List<Character> escapeChar = new ArrayList<Character>();
// =========================================================

  public Scanner(SourceFile source, ErrorReporter reporter) {
    sourceFile = source;
    errorReporter = reporter;
    currentChar = sourceFile.getNextChar();
    debug = false;
    
    // you may initialise your counters for line and column numbers here
    charLine = charCount = 1; 
    lineStart = lineFinish = 1; 
    charStart = charFinish = 1;
    sourcePos = new SourcePosition(1, 1, 0);
    
    escapeChar.add('\b');
    escapeChar.add('\f');
    escapeChar.add('\n');
    escapeChar.add('\r');
    escapeChar.add('\t');
    escapeChar.add('\'');
    escapeChar.add('\"');
    escapeChar.add('\\');
    
  }

  public void enableDebugging() {
    debug = true;
  }

  public void incrementLine() {
	  
  }
  
  // accept gets the next character from the source program.

  private void accept() {
	  
	if(currentChar != '\n' && currentChar != ' ' && currentChar != '"') {
		currentSpelling.append(currentChar);
		if(flag) {
			charStart = charLine + charCount;
			flag = false;
		}
	}
	
	if (currentChar == ' ') {
		charLine++;
	}
	
    currentChar = sourceFile.getNextChar();
	
	charCount++;  
	
    if(currentChar == '\n') {
    	currentChar = sourceFile.getNextChar();
    	lineStart++;
    	lineFinish = lineStart;
    	charFinish = charLine; 
    	charStart = charLine = charCount = 0;
    	flag = true; 
    }
	
	
    
  // you may save the lexeme of the current token incrementally here
  // you may also increment your line and column counters here
  }

  // inspectChar returns the n-th character after currentChar
  // in the input stream. 
  //
  // If there are fewer than nthChar characters between currentChar 
  // and the end of file marker, SourceFile.eof is returned.
  // 
  // Both currentChar and the current position in the input stream
  // are *not* changed. Therefore, a subsequent call to accept()
  // will always return the next char after currentChar.

  private char inspectChar(int nthChar) {
    return sourceFile.inspectChar(nthChar);
  }
  
  private char getEndOfComment() {
	  
	  while(currentChar != SourceFile.eof) {
		  currentChar = sourceFile.getNextChar();
		  
		  if(currentChar == '*' && inspectChar(1) == '/') {
			  currentChar = sourceFile.getNextChar();
			  currentChar = sourceFile.getNextChar();
			  return currentChar; 
		  } 
	  }
	  
	  currentChar = sourceFile.getNextChar();
	  return currentChar;
  }
  
  private int nextToken() {
  // Tokens: separators, operators, literals, identifiers and keyworods
	
    switch (currentChar) {
    
    // separators
    
    case '(':
    	accept();
    	return Token.LPAREN;
    case ')':
    	accept();
    	return Token.RPAREN;
    case '{':
    	accept();
    	return Token.LCURLY;
    case '}':
    	accept();
    	return Token.RCURLY;
    case ';':
    	accept();
    	return Token.SEMICOLON;
    case ',':
    	accept();
    	return Token.COMMA;
    	
    // operators
    case '+':
    	accept();
    	return Token.PLUS;
    case '-':
    	accept();
    	return Token.MINUS;
    case '*':
    	accept();
    	return Token.MULT;
    case '/':
    	if(inspectChar(1) == '*') {
    		currentChar = getEndOfComment();
    		if(currentChar == SourceFile.eof) {
    			errorReporter.reportError("unterminated ting", "ting", sourcePos);
    		}
    		break;
    	} else if (inspectChar(1) >= '0' && inspectChar(1) <= '9') {
        	accept();
        	return Token.DIV;
    	} else if(inspectChar(1) == '/') {
    		currentChar = sourceFile.getNextChar();
    		while(currentChar != '\n' && currentChar != SourceFile.eof) {
    			currentChar = sourceFile.getNextChar();
    		}
    		break;
    	}
    case '!':
    	accept();
    	if(currentChar == '=') {
    		accept();
    		return Token.NOTEQ;
    	}
    	return Token.NOT;	
    case '=':
    	accept();
    	if(currentChar == '=') {
    		accept();
    		return Token.EQEQ;
    	}
    	return Token.EQ;
    case '<':
    	accept();
    	if(currentChar == '=') {
    		accept();
    		return Token.LTEQ;
    	}
    	return Token.LT;
    case '>':
    	accept();
    	if(currentChar == '=') {
    		accept();
    		return Token.GTEQ;
    	}
    	return Token.GT;
    case '&':
    	accept();
    	if(currentChar == '&') {
    		accept();
    		return Token.ANDAND;
    	} else {
    		return Token.ERROR;
    	}
    case '|':	
       	accept();
      	if (currentChar == '|') {
           accept();
           return Token.OROR;
       } else {
    	   return Token.ERROR;
       }
    case '"':
    	accept();
    	while(currentChar != '"') {
    		if(currentChar == '\n') {
    			errorReporter.reportError("unterminated ting", "ting", sourcePos);
    			break;
    		}
    		accept();
    	}
    	//accept();
    	return Token.STRINGLITERAL;
    case '.':
    	char nextChar = inspectChar(1);
        if(nextChar >= '0' && nextChar <= '9') {
        	accept();
        	while(currentChar >= '0' && currentChar <= '9' || currentChar == 'e' || currentChar == 'E') {
        		accept();
        	}
        	return Token.FLOATLITERAL;
        }
        accept();
        return Token.ERROR;
    // ....
    	
    case SourceFile.eof:	
    	currentSpelling.append(Token.spell(Token.EOF));
    	return Token.EOF;
    
    // this isnt how to find the string literal have to take into account the " "
    default:
    	if (currentChar >= 'A' && currentChar <= 'Z' || 
    	 	currentChar >= 'a' && currentChar <= 'z' || 
    	 	currentChar == '_') {
    		
    		//have to check if it is a reserved word
    		
    		while(Character.isAlphabetic(currentChar)) {
    			accept();
    		}
    		if(checkReservedWords(currentSpelling.toString()) >= 0 ) {
    			return checkReservedWords(currentSpelling.toString());
    		}
    		
    		return Token.ERROR;
    	}
    	
    	//will have to check if its a float literal as well
    	if (currentChar >= '0' && currentChar <= '9') {
    		while(Character.isDigit(currentChar)) {
    			accept();
    		}
    		return Token.INTLITERAL;
    	}
    		    		
	break;
    }

    accept(); 
    return nextToken();
  }
  
  //check if a reserved word was used.
  public int checkReservedWords(String spelling) {
	  if(spelling.equals("boolean")) {
		  return 0; 
	  } else if (spelling.equals("break")) {
		  return 1; 
	  } else if (spelling.equals("continue")) {
		  return 2;
	  } else if (spelling.equals("else")) {
		  return 3; 
	  } else if (spelling.equals("float")) {
		  return 4; 
	  } else if (spelling.equals("for")) {
		  return 5; 
	  } else if (spelling.equals("if")) {
		  return 6; 
	  } else if (spelling.equals("int")) {
		  return 7; 
	  } else if (spelling.equals("return")) {
		  return 8; 
	  } else if (spelling.equals("void")) {
		  return 9; 
	  } else if (spelling.equals("while")) {
		  return 10; 
	  } else if (spelling.equals("true")) {
		  return 36; 
	  } else if (spelling.equals("false")) {
		  return 36;
	  }
	  
	  return -1; 
  }
  
  void skipSpaceAndComments() {
	  if(currentChar == ' ') {
			accept();
			currentSpelling = new StringBuffer("");
			
		}
  }

  public Token getToken() {
    Token tok;
    int kind;

    // skip white space and comments

   skipSpaceAndComments();

   currentSpelling = new StringBuffer("");

   // You must record the position of the current token somehow
   
   kind = nextToken();
   
   sourcePos = new SourcePosition(lineStart, charStart, (charFinish + currentSpelling.length()));
   
   tok = new Token(kind, currentSpelling.toString(), sourcePos);

   // * do not remove these three lines
   if (debug)
     System.out.println(tok);
   return tok;
   }

}
