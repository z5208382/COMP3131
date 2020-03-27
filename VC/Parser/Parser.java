
/*
 * Parser.java
 *
 * This parser for a subset of the VC language is intended to
 *  demonstrate how to create the AST nodes, including (among others):
 *  [1] a list (of statements)
 *  [2] a function
 *  [3] a statement (which is an expression statement),
 *  [4] a unary expression
 *  [5] a binary expression
 *  [6] terminals (identifiers, integer literals and operators)
 *
 * In addition, it also demonstrates how to use the two methods start
 * and finish to determine the position information for the start and
 * end of a construct (known as a phrase) corresponding an AST node.
 *
 * NOTE THAT THE POSITION INFORMATION WILL NOT BE MARKED. HOWEVER, IT CAN BE
 * USEFUL TO DEBUG YOUR IMPLEMENTATION.
 *
 * --- 5-March-2020 ---
program       -> func-decl
func-decl     -> type identifier "(" ")" compound-stmt
type          -> void
identifier    -> ID
// statements
compound-stmt -> "{" stmt* "}"
stmt          -> expr-stmt
expr-stmt     -> expr? ";"
// expressions
expr                -> additive-expr
additive-expr       -> multiplicative-expr
                    |  additive-expr "+" multiplicative-expr
                    |  additive-expr "-" multiplicative-expr
multiplicative-expr -> unary-expr
	            |  multiplicative-expr "*" unary-expr
	            |  multiplicative-expr "/" unary-expr
unary-expr          -> "-" unary-expr
		    |  primary-expr
primary-expr        -> identifier
 		    |  INTLITERAL
		    | "(" expr ")"
 */

package VC.Parser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;
import VC.ErrorReporter;
import VC.ASTs.*;

public class Parser {

  private Scanner scanner;
  private ErrorReporter errorReporter;
  private Token currentToken;
  private SourcePosition previousTokenPosition;
  private SourcePosition dummyPos = new SourcePosition();

  private String[] typeDeclaration = {"void", "boolean", "int", "float", "id"};
  private String[] stmt = {"if", "for", "while", "break", "continue", "return"};

  public Parser (Scanner lexer, ErrorReporter reporter) {
    scanner = lexer;
    errorReporter = reporter;

    previousTokenPosition = new SourcePosition();

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
      previousTokenPosition = currentToken.position;
      currentToken = scanner.getToken();
    } else {
      syntacticError("\"%\" expected here", Token.spell(tokenExpected));
    }
  }

  void accept() {
    previousTokenPosition = currentToken.position;
    currentToken = scanner.getToken();
  }

  void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
    SourcePosition pos = currentToken.position;
    errorReporter.reportError(messageTemplate, tokenQuoted, pos);
    throw(new SyntaxError());
  }

// start records the position of the start of a phrase.
// This is defined to be the position of the first
// character of the first token of the phrase.

  void start(SourcePosition position) {
    position.lineStart = currentToken.position.lineStart;
    position.charStart = currentToken.position.charStart;
  }

// finish records the position of the end of a phrase.
// This is defined to be the position of the last
// character of the last token of the phrase.

  void finish(SourcePosition position) {
    position.lineFinish = previousTokenPosition.lineFinish;
    position.charFinish = previousTokenPosition.charFinish;
  }

  void copyStart(SourcePosition from, SourcePosition to) {
    to.lineStart = from.lineStart;
    to.charStart = from.charStart;
  }

// ========================== PROGRAMS ========================

  public Program parseProgram() {

    Program programAST = null;
    
    SourcePosition programPos = new SourcePosition();
    start(programPos);

    try {
      List dlAST = parseFuncDeclList();
      finish(programPos);
      programAST = new Program(dlAST, programPos);
      if (currentToken.kind != Token.EOF) {
        syntacticError("\"%\" unknown type", currentToken.spelling);
      }
    }
    catch (SyntaxError s) { return null; }
    return programAST;
  }

// ========================== DECLARATIONS ========================

  List parseFuncDeclList() throws SyntaxError {
    List dlAST = null;
    Decl dAST = null;
    DeclList currASTHead  = null;

    if(currentToken.kind == Token.EOF) {
        return new EmptyDeclList(dummyPos);
    }
    
    SourcePosition funcPos = new SourcePosition();
    start(funcPos);

    dAST = parseFuncDecl();

    if(dAST != null) {
        finish(funcPos);
        dlAST = new DeclList(dAST, new EmptyDeclList(dummyPos), funcPos);
    }
    
    currASTHead = (DeclList) dlAST;
    
    while(!(currASTHead.DL instanceof EmptyDeclList)) {
        currASTHead = (DeclList) currASTHead.DL;
    }
    
    if(currentToken.kind != Token.EOF) {
        currASTHead.DL = parseFuncDeclList();
    }
    return dlAST;
  }

  Decl parseFuncDecl() throws SyntaxError {

    Decl fAST = null;
 
    SourcePosition funcPos = new SourcePosition();
    start(funcPos);

    if(currentToken.kind == Token.LPAREN){
        Type tAST = parseType();
        Ident iAST = parseIdent();
        match(Token.LPAREN);
        List fplAST = parseParaList();
        Stmt cAST = parseCompoundStmt();
        finish(funcPos);
        fAST = new FuncDecl(tAST, iAST, fplAST, cAST, funcPos);
    } else {
        fAST = parseVarDeclaration("global");

    }
    return fAST;
  }

  Decl parseVarDeclaration(String declrType) throws SyntaxError {
    Decl var = null;
    SourcePosition pos = new SourcePosition();
    start(pos);
    Type tAST = parseType();
    Ident iAST = parseIdent();
    Expr assign = new EmptyExpr(dummyPos);
    if(currentToken.kind == Token.EQ) {
        match(Token.EQ);
        assign = parseExpr();
    }
    if(currentToken.kind == Token.SEMICOLON){
        match(Token.SEMICOLON);
    }
    if(declrType == "local"){
        var = new LocalVarDecl(tAST, iAST, assign, pos);
    } else if(declrType == "global") {
        var = new GlobalVarDecl(tAST, iAST, assign, pos);
    }

    return var;
  }
//  ======================== TYPES ==========================

  Type parseType() throws SyntaxError {
    Type typeAST = null;

    SourcePosition typePos = new SourcePosition();
    start(typePos);

    switch(currentToken.kind) {
        case Token.VOID:
            match(Token.VOID);
            typeAST = new VoidType(dummyPos);
            break;
        case Token.BOOLEAN:
            match(Token.BOOLEAN);
            typeAST = new BooleanType(dummyPos);
            break;
        case Token.INT:
            match(Token.INT);
            typeAST = new IntType(dummyPos);
            break;
        case Token.FLOAT:
            match(Token.FLOAT);
            typeAST = new FloatType(dummyPos);
            break;
        default:
            syntacticError("ERROR", currentToken.spelling);
    }

    finish(typePos);
    typeAST.position = typePos;

    return typeAST;
    }

// ======================= STATEMENTS ==============================

  Stmt parseCompoundStmt() throws SyntaxError {
    Stmt cAST = null;

    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    match(Token.LCURLY);

    // Insert code here to build a DeclList node for variable declarations
    List slAST = parseStmtList();
    match(Token.RCURLY);
    finish(stmtPos);

    /* In the subset of the VC grammar, no variable declarations are
     * allowed. Therefore, a block is empty iff it has no statements.
     */
    if (slAST instanceof EmptyStmtList)
      cAST = new EmptyCompStmt(stmtPos);
    else
      cAST = new CompoundStmt(new EmptyDeclList(dummyPos), slAST, stmtPos);
    return cAST;
  }


  List parseStmtList() throws SyntaxError {
    List slAST = null;
    Stmt sAST = null;
    Decl localDeclVar = null;

    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    if (currentToken.kind != Token.RCURLY) {
      if(isTypeDeclaration()){
         localDeclVar = parseVarDeclaration("local");
         if(currentToken.kind != Token.RCURLY) {
            slAST = parseStmtList();
            finish(stmtPos);
            slAST = new DeclList (localDeclVar, slAST, stmtPos);
         } else {
               finish(stmtPos);
                slAST = new DeclList(localDeclVar, new EmptyDeclList(dummyPos), stmtPos);
           }
      } else {
            sAST = parseStmt();
          {
            if (currentToken.kind != Token.RCURLY) {
              slAST = parseStmtList();
              finish(stmtPos);
              slAST = new StmtList(sAST, slAST, stmtPos);
            } else {
              finish(stmtPos);
              slAST = new StmtList(sAST, new EmptyStmtList(dummyPos), stmtPos);
            }
          }
        }
    }
    else
      slAST = new EmptyStmtList(dummyPos);
    
    return slAST;
  }

  Stmt parseStmt() throws SyntaxError {
    Stmt sAST = null;

    sAST = parseExprStmt();

    return sAST;
  }

  Stmt parseExprStmt() throws SyntaxError {
    Stmt sAST = null;

    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    if (currentToken.kind == Token.ID
        || currentToken.kind == Token.INTLITERAL
        || currentToken.kind == Token.LPAREN) {
        Expr eAST = parseExpr();
        match(Token.SEMICOLON);
        finish(stmtPos);
        sAST = new ExprStmt(eAST, stmtPos);
    } else {
      match(Token.SEMICOLON);
      finish(stmtPos);
      sAST = new ExprStmt(new EmptyExpr(dummyPos), stmtPos);
    }
    return sAST;
  }


// ======================= PARAMETERS =======================
 
  List parseParaList() throws SyntaxError {

    SourcePosition paraPos = new SourcePosition();
    List paraList = new EmptyParaList(dummyPos);
    
    start(paraPos);
    if(currentToken.kind != Token.RPAREN) {
        Type paraType = parseType();
        Ident paraIdent = parseIdent();
        if(currentToken.kind == Token.COMMA){
            match(Token.COMMA);
        }
        ParaDecl paraDecl = new ParaDecl(paraType, paraIdent, paraPos);
        paraList = new ParaList(paraDecl, parseParaList(), paraPos);
        finish(paraPos);
        paraList.position = paraPos;
    }
    
    if(currentToken.kind == Token.RPAREN){
        match(Token.RPAREN);
    }
    return paraList;
  }


// ======================= EXPRESSIONS ======================


  Expr parseExpr() throws SyntaxError {
    Expr exprAST = null;
    exprAST = parseAssignExpr();
    return exprAST;
  }

  Expr parseAssignExpr() throws SyntaxError {
    SourcePosition addStartPos = new SourcePosition();
    start(addStartPos);
    Expr exprAST = parseCondOrExpr();

    while(currentToken.kind == Token.EQ) {
        Operator opAST = acceptOperator();
        Expr e2AST = parseCondOrExpr();
        
        SourcePosition addPos = new SourcePosition();
        copyStart(addStartPos, addPos);
        finish(addPos);
        exprAST = new AssignExpr(exprAST, e2AST, addPos);
    }
    
    return exprAST;
  }
  
  Expr parseCondOrExpr() throws SyntaxError {
      SourcePosition addStartPos = new SourcePosition();
      start(addStartPos);
      Expr exprAST = parseCondAndExpr();

      while(currentToken.kind == Token.OROR) {
          Operator opAST = acceptOperator();
          Expr e2AST = parseCondAndExpr();
          
          SourcePosition addPos = new SourcePosition();
          copyStart(addStartPos, addPos);
          finish(addPos);
          exprAST = new BinaryExpr(exprAST, opAST, e2AST, addPos);
      }
      
      return exprAST;
  }

  Expr parseCondAndExpr() throws SyntaxError {
      SourcePosition addStartPos = new SourcePosition();
      start(addStartPos);
      Expr exprAST = parseEqualityExpr();

      while(currentToken.kind == Token.ANDAND) {
          Operator opAST = acceptOperator();
          Expr e2AST = parseEqualityExpr();
          
          SourcePosition addPos = new SourcePosition();
          copyStart(addStartPos, addPos);
          finish(addPos);
          exprAST = new BinaryExpr(exprAST, opAST, e2AST, addPos);
      }

      return exprAST;
  }

  Expr parseEqualityExpr() throws SyntaxError {
      SourcePosition addStartPos = new SourcePosition();
      start(addStartPos);
      Expr exprAST = parseRelExpr();

      while(currentToken.kind == Token.EQEQ || currentToken.kind == Token.NOTEQ) {
          Operator opAST = acceptOperator();
          Expr e2AST = parseRelExpr();
          
          SourcePosition addPos = new SourcePosition();
          copyStart(addStartPos, addPos);
          finish(addPos);
          exprAST = new BinaryExpr(exprAST, opAST, e2AST, addPos);
      }

      return exprAST;
  }

  Expr parseRelExpr() throws SyntaxError {
      SourcePosition addStartPos = new SourcePosition();
      start(addStartPos);
      Expr exprAST = parseAdditiveExpr();

      while(currentToken.kind == Token.LT || currentToken.kind == Token.LTEQ
           || currentToken.kind == Token.GT || currentToken.kind == Token.GTEQ) {
          Operator opAST = acceptOperator();
          Expr e2AST = parseAdditiveExpr();
          
          SourcePosition addPos = new SourcePosition();
          copyStart(addStartPos, addPos);
          finish(addPos);
          exprAST = new BinaryExpr(exprAST, opAST, e2AST, addPos);
      }

      return exprAST;
  }

  Expr parseAdditiveExpr() throws SyntaxError {
    Expr exprAST = null;

    SourcePosition addStartPos = new SourcePosition();
    start(addStartPos);

    exprAST = parseMultiplicativeExpr();
    while (currentToken.kind == Token.PLUS
           || currentToken.kind == Token.MINUS) {
      Operator opAST = acceptOperator();
      Expr e2AST = parseMultiplicativeExpr();

      SourcePosition addPos = new SourcePosition();
      copyStart(addStartPos, addPos);
      finish(addPos);
      exprAST = new BinaryExpr(exprAST, opAST, e2AST, addPos);
    }
    return exprAST;
  }

  Expr parseMultiplicativeExpr() throws SyntaxError {

    Expr exprAST = null;

    SourcePosition multStartPos = new SourcePosition();
    start(multStartPos);

    exprAST = parseUnaryExpr();
    while (currentToken.kind == Token.MULT
           || currentToken.kind == Token.DIV) {
      Operator opAST = acceptOperator();
      Expr e2AST = parseUnaryExpr();
      SourcePosition multPos = new SourcePosition();
      copyStart(multStartPos, multPos);
      finish(multPos);
      exprAST = new BinaryExpr(exprAST, opAST, e2AST, multPos);
    }
    return exprAST;
  }

  Expr parseUnaryExpr() throws SyntaxError {

    Expr exprAST = null;

    SourcePosition unaryPos = new SourcePosition();
    start(unaryPos);

    switch (currentToken.kind) {
      case Token.MINUS:
        {
          Operator opAST = acceptOperator();
          Expr e2AST = parseUnaryExpr();
          finish(unaryPos);
          exprAST = new UnaryExpr(opAST, e2AST, unaryPos);
        }
        break;

      default:
        exprAST = parsePrimaryExpr();
        break;
       
    }
    return exprAST;
  }

  Expr parsePrimaryExpr() throws SyntaxError {

    Expr exprAST = null;

    SourcePosition primPos = new SourcePosition();
    start(primPos);

    switch (currentToken.kind) {

      case Token.ID:
        Ident iAST = parseIdent();
        finish(primPos);
        
        Var simVAST = new SimpleVar(iAST, primPos);
        exprAST = new VarExpr(simVAST, primPos);

        if(currentToken.kind == Token.LBRACKET) {
            match(Token.LBRACKET);
            Expr arrayExpr = parseExpr();
            match(Token.RBRACKET);
            finish(primPos);
            exprAST = new ArrayExpr(simVAST, arrayExpr, primPos);
        } else if(currentToken.kind == Token.LPAREN) {
            List args = parseParaList();
            finish(primPos);
            exprAST = new CallExpr(iAST, args, primPos);
        }

        break;
        
      case Token.LPAREN:
        {
          accept();
          exprAST = parseExpr();
	  match(Token.RPAREN);
        }
        break;

      case Token.INTLITERAL:
        IntLiteral ilAST = parseIntLiteral();
        finish(primPos);
        exprAST = new IntExpr(ilAST, primPos);
        break;
      
      case Token.BOOLEANLITERAL:
        BooleanLiteral booAST = parseBooleanLiteral();
        finish(primPos);
        exprAST = new BooleanExpr(booAST, primPos);
        break;

      case Token.FLOATLITERAL:
        FloatLiteral fltAST = parseFloatLiteral();
        finish(primPos);
        exprAST = new FloatExpr(fltAST, primPos);
        break;

      case Token.STRINGLITERAL:
        StringLiteral strAST = parseStringLiteral();
        finish(primPos);
        exprAST = new StringExpr(strAST, primPos);

      default:
        syntacticError("illegal primary expression", currentToken.spelling);
       
    }
    return exprAST;
  }

// ========================== ID, OPERATOR and LITERALS ========================

  Ident parseIdent() throws SyntaxError {

    Ident I = null;

    if (currentToken.kind == Token.ID) {
      previousTokenPosition = currentToken.position;
      String spelling = currentToken.spelling;
      I = new Ident(spelling, previousTokenPosition);
      currentToken = scanner.getToken();
    } else
      syntacticError("identifier expected here", "");
    return I;
  }

// acceptOperator parses an operator, and constructs a leaf AST for it

  Operator acceptOperator() throws SyntaxError {
    Operator O = null;

    previousTokenPosition = currentToken.position;
    String spelling = currentToken.spelling;
    O = new Operator(spelling, previousTokenPosition);
    currentToken = scanner.getToken();
    return O;
  }


  IntLiteral parseIntLiteral() throws SyntaxError {
    IntLiteral IL = null;

    if (currentToken.kind == Token.INTLITERAL) {
      String spelling = currentToken.spelling;
      accept();
      IL = new IntLiteral(spelling, previousTokenPosition);
    } else
      syntacticError("integer literal expected here", "");
    return IL;
  }

  FloatLiteral parseFloatLiteral() throws SyntaxError {
    FloatLiteral FL = null;

    if (currentToken.kind == Token.FLOATLITERAL) {
      String spelling = currentToken.spelling;
      accept();
      FL = new FloatLiteral(spelling, previousTokenPosition);
    } else
      syntacticError("float literal expected here", "");
    return FL;
  }

  BooleanLiteral parseBooleanLiteral() throws SyntaxError {
    BooleanLiteral BL = null;

    if (currentToken.kind == Token.BOOLEANLITERAL) {
      String spelling = currentToken.spelling;
      accept();
      BL = new BooleanLiteral(spelling, previousTokenPosition);
    } else
      syntacticError("boolean literal expected here", "");
    return BL;
  }

  StringLiteral parseStringLiteral() throws SyntaxError {
    StringLiteral SL = null;
    
    if (currentToken.kind == Token.STRINGLITERAL) {
        String spelling = currentToken.spelling;
        accept();
        SL = new StringLiteral(spelling, previousTokenPosition);
    } else {
        syntacticError("string literal expected here", "");
    }
    return SL;
  }

}

