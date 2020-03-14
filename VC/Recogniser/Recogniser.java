/***
 * *
 * * Recogniser.java
 * *
 ***/

/* At this stage, this parser accepts a subset of VC defined	by
 * the following grammar.
 *
 * You need to modify the supplied parsing methods (if necessary) and
 * add the missing ones to obtain a parser for the VC language.
 *
 * (27---Feb---2019)

program       -> func-decl

// declaration

func-decl     -> void identifier "(" ")" compound-stmt

identifier    -> ID

// statements
compound-stmt -> "{" stmt* "}"
stmt          -> continue-stmt
    	      |  expr-stmt
continue-stmt -> continue ";"
expr-stmt     -> expr? ";"

// expressions
expr                -> assignment-expr
assignment-expr     -> additive-expr
additive-expr       -> multiplicative-expr
                    |  additive-expr "+" multiplicative-expr
multiplicative-expr -> unary-expr
	                |  multiplicative-expr "*" unary-expr
unary-expr          -> "-" unary-expr
		            |  primary-expr

primary-expr        -> identifier
 		            |  INTLITERAL
		            | "(" expr ")"
*/

package VC.Recogniser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;
import VC.ErrorReporter;

public class Recogniser {

  private Scanner scanner;
  private ErrorReporter errorReporter;
  private Token currentToken;
  private String[] typeDeclaration = {"void", "boolean", "int", "float", "id"};
  private String[] stmt = {"if", "for", "while", "break", "continue", "return"};

  public Recogniser (Scanner lexer, ErrorReporter reporter) {
    scanner = lexer;
    errorReporter = reporter;
    currentToken = scanner.getToken();
  }


  boolean isTypeDeclaration() {
      for(int i = 0; i < typeDeclaration.length; i++) {
          if(typeDeclaration[i] == currentToken.spell(currentToken.kind)){
              return true;
          }
      }
      return false;
  }
  
  boolean isStmtDeclaration() {
      for(int i = 0; i < stmt.length; i++) {
          if(stmt[i] == currentToken.spell(currentToken.kind)) {
              return true;
          }
      }
      return false;
  }
// match checks to see f the current token matches tokenExpected.
// If so, fetches the next token.
// If not, reports a syntactic error.

  void match(int tokenExpected) throws SyntaxError {
    if (currentToken.kind == tokenExpected) {
      currentToken = scanner.getToken();
    } else {
      syntacticError("\"%\" expected here", Token.spell(tokenExpected));
    }
  }

 // accepts the current token and fetches the next
  void accept() {
    currentToken = scanner.getToken();
  }

  void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
    SourcePosition pos = currentToken.position;
    errorReporter.reportError(messageTemplate, tokenQuoted, pos);
    throw(new SyntaxError());
  }


// ========================== PROGRAMS ========================

  public void parseProgram() {

    try {
      while(currentToken.kind != Token.EOF) {
          parseFuncDecl();
      }
      if (currentToken.kind != Token.EOF) {
        syntacticError("\"%\" wrong result type for a function", currentToken.spelling);
      }
    }
    catch (SyntaxError s) {  }
  }

// ========================== DECLARATIONS ========================

    void parseFuncDecl() throws SyntaxError {
     
 //System.out.println("1");
    parseType();
 //System.out.println("2");
    parseIdent();
 //System.out.println("3");
    // have to parse through para-list
    if(currentToken.kind == Token.LPAREN){
        parseParraList();
        parseCompoundStmt();
    }   else {
        parseVarDeclaration();
    }

    // have to parse through parseCompoundStmt if applicable.
    //System.out.println("4");
    
    //System.out.println("5");
    /*
    match(Token.VOID);
    parseIdent();
    match(Token.LPAREN);
    match(Token.RPAREN);
    parseCompoundStmt();
    */
  }
    
    public void parseVarDeclaration() throws SyntaxError {
        switch(currentToken.kind) {
            case Token.EQ:
                match(Token.EQ);
                parseNewAssignedValue();
                break;
            case Token.COMMA:
                match(Token.COMMA);
                parseDeclarationList();
                break;
            case Token.LBRACKET:
                match(Token.LBRACKET);
                if(currentToken.kind == Token.INTLITERAL) match(Token.INTLITERAL);
                match(Token.RBRACKET);
                break;
            default:
                break;
        }
        match(Token.SEMICOLON);
    }
    
    public void parseDeclarationList() throws SyntaxError {
            
    }
    
    void parseNewAssignedValue() throws SyntaxError {
        switch(currentToken.kind) {
            case Token.LCURLY:
                match(Token.LCURLY);
                parseExpr();
                while(currentToken.kind == Token.COMMA) {
                    match(Token.COMMA);
                    parseExpr();
                }
                match(Token.RCURLY);
                break;
            default:
                parseExpr();
        }
    }

    public void parseParraList() throws SyntaxError {
        match(Token.LPAREN);
        while(currentToken.kind != Token.RPAREN) {
            parseType();
            parseIdent();
            if(currentToken.kind == Token.COMMA){
                match(Token.COMMA);
            }
        }
        match(Token.RPAREN);
    }

    public void parseFunctionCall() throws SyntaxError {
        match(Token.LPAREN);
        while(currentToken.kind != Token.RPAREN) {
            parseType();
            if(currentToken.kind == Token.COMMA){
                match(Token.COMMA);
            }
        }
        match(Token.RPAREN);
    }

    public void parseType() throws SyntaxError {
        switch(currentToken.kind) {
            case Token.VOID:
                match(Token.VOID);
                break;
            case Token.BOOLEAN:
                match(Token.BOOLEAN);
                break;
            case Token.INT:
                match(Token.INT);
                break;
            case Token.FLOAT:
                match(Token.FLOAT);
                break;
            case Token.ID:
                match(Token.ID);
                break;
            case Token.INTLITERAL:
                parseIntLiteral();
                break;
            case Token.FLOATLITERAL:
                parseFloatLiteral();
                break;
            case Token.BOOLEANLITERAL:
                parseBooleanLiteral();
                break;
            case Token.STRINGLITERAL:
                parseStringLiteral();
                break;
            default:
                syntacticError("ERROR", currentToken.spelling);
      }
  }
// ======================= STATEMENTS ==============================


  void parseCompoundStmt() throws SyntaxError {
    
     //System.out.println("Blahhh");
    //System.out.println(currentToken.spell(currentToken.kind));
    match(Token.LCURLY);
    //System.out.println("Blob");
    while(currentToken.kind != Token.RCURLY) {
        parseExpr();
        if(currentToken.kind == Token.SEMICOLON){
            match(Token.SEMICOLON);
        }
        if(isStmtDeclaration()) {
            parseStmt();
        }
    }
    
    match(Token.RCURLY);
  }

 // Here, a new nontermial has been introduced to define { stmt } *
  void parseStmtList() throws SyntaxError {

    while (currentToken.kind != Token.RCURLY)
      parseStmt();
  }

  void parseStmt() throws SyntaxError {

    switch (currentToken.kind) {

    case Token.LCURLY:
        parseCompoundStmt();
        break;
    case Token.IF:
        match(Token.IF);
        parseIfStmt();
        break;
    case Token.FOR:
        parseForStmt();
        break;
    case Token.WHILE:
        parseWhileStmt();
        break;
    case Token.BREAK:
        parseBreakStmt();
        break;
    case Token.RETURN:
        parseReturnStmt();
        break;
    case Token.CONTINUE:
        parseContinueStmt();
        break;
    
    default:
        //System.out.println("Im here");
        parseExprStmt();
        break;

    }
  }

  void parseIfStmt() throws SyntaxError {
    match(Token.LPAREN);
    parseExpr();
    match(Token.RPAREN);
    parseStmt();
    if(currentToken.kind == Token.ELSE) {
        match(Token.ELSE);
        parseStmt();
    }
  }

  void parseForStmt() throws SyntaxError {

  }
  
  void parseWhileStmt() throws SyntaxError {

  }

  void parseBreakStmt() throws SyntaxError {

  }

  void parseReturnStmt() throws SyntaxError {

  }

  void parseContinueStmt() throws SyntaxError {
    
    match(Token.CONTINUE);
    match(Token.SEMICOLON);

  }

  void parseExprStmt() throws SyntaxError {

    if (currentToken.kind == Token.ID
        || currentToken.kind == Token.INTLITERAL
        || currentToken.kind == Token.MINUS
        || currentToken.kind == Token.LPAREN) {
        parseExpr();
        match(Token.SEMICOLON);
    } else {
      match(Token.SEMICOLON);
    }
  }


// ======================= IDENTIFIERS ======================

 // Call parseIdent rather than match(Token.ID).
 // In Assignment 3, an Identifier node will be constructed in here.


  void parseIdent() throws SyntaxError {

    if (currentToken.kind == Token.ID) {
      currentToken = scanner.getToken();
    } else
      syntacticError("identifier expected here", "");
  }

// ======================= OPERATORS ======================

 // Call acceptOperator rather than accept().
 // In Assignment 3, an Operator Node will be constructed in here.

  void acceptOperator() throws SyntaxError {

    currentToken = scanner.getToken();
  }


// ======================= EXPRESSIONS ======================

  void parseExpr() throws SyntaxError {
    parseAssignExpr();
  }


  void parseAssignExpr() throws SyntaxError {
    parseCondOrExpr();
    while(currentToken.kind == Token.EQ){
        match(Token.EQ);
        parseCondOrExpr();
    }
    // this is the original function call parseAdditiveExpr();

  }

  //TO DO
  void parseCondOrExpr() throws SyntaxError {
    parseCondAndExpr();
    while(currentToken.kind == Token.OROR) {
        acceptOperator();
        parseCondAndExpr();
    }
  }
  
    //TO DO
  void parseCondAndExpr() throws SyntaxError {
    parseEqualityExpr();
    while(currentToken.kind == Token.ANDAND){
        acceptOperator();
        parseEqualityExpr();
    }
  }
    //TO DO
  void parseEqualityExpr() throws SyntaxError {
    parseRelExpr();
    while(currentToken.kind == Token.EQEQ || currentToken.kind == Token.NOTEQ) {
        acceptOperator();
        parseRelExpr();
    }
  }
    //TO DO
  void parseRelExpr() throws SyntaxError {
    parseAdditiveExpr();
    while(currentToken.kind == Token.LT || currentToken.kind == Token.LTEQ
           || currentToken.kind == Token.GT || currentToken.kind == Token.GTEQ) {
        acceptOperator();
        parseAdditiveExpr();
    }
  }

  void parseAdditiveExpr() throws SyntaxError {

    parseMultiplicativeExpr();
    while (currentToken.kind == Token.PLUS) {
      acceptOperator();
      parseMultiplicativeExpr();
    }
  }

  void parseMultiplicativeExpr() throws SyntaxError {

    parseUnaryExpr();
    while (currentToken.kind == Token.MULT) {
      acceptOperator();
      parseUnaryExpr();
    }
  }

  void parseUnaryExpr() throws SyntaxError {

    switch (currentToken.kind) {
      case Token.MINUS:
        {
          acceptOperator();
          parseUnaryExpr();
        }
        break;

      default:
        parsePrimaryExpr();
        break;
       
    }
  }

  void parsePrimaryExpr() throws SyntaxError {

    switch (currentToken.kind) {

      case Token.ID:
        parseIdent();
        if (currentToken.kind == Token.LBRACKET) {
					match(Token.LBRACKET);
					parseExpr();
					match(Token.RBRACKET);
				} else if (currentToken.kind == Token.LPAREN){
					parseFunctionCall();
				}
        break;

      case Token.LPAREN:
        {
          accept();
          parseExpr();
	  match(Token.RPAREN);
        }
        break;

      case Token.INTLITERAL:
        parseIntLiteral();
        break;

      default:
        parseType();
        //syntacticError("illegal parimary expression", currentToken.spelling);
        break;
    }
  }

// ========================== LITERALS ========================

  // Call these methods rather than accept().  In Assignment 3,
  // literal AST nodes will be constructed inside these methods.

  //might have to change the string one cause i just copied the previous ones
  void parseStringLiteral() throws SyntaxError {
    if(currentToken.kind == Token.STRINGLITERAL) {
        currentToken = scanner.getToken();
    } else
        syntacticError("String literal expected here", "");
  }
  
  void parseIntLiteral() throws SyntaxError {

    if (currentToken.kind == Token.INTLITERAL) {
      currentToken = scanner.getToken();
    } else
      syntacticError("integer literal expected here", "");
  }

  void parseFloatLiteral() throws SyntaxError {

    if (currentToken.kind == Token.FLOATLITERAL) {
      currentToken = scanner.getToken();
    } else
      syntacticError("float literal expected here", "");
  }

  void parseBooleanLiteral() throws SyntaxError {

    if (currentToken.kind == Token.BOOLEANLITERAL) {
      currentToken = scanner.getToken();
    } else
      syntacticError("boolean literal expected here", "");
  }

}
