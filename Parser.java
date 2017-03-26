/* *** This file is given as part of the programming assignment. *** */
import java.util.*;

public class Parser {

    // tok is global to all these parsing methods;
    // scan just calls the scanner's scan method and saves the result in tok.
	private Stack<ArrayList<String>> symbolTable;
    private Token tok; // the current token
    private void scan() {
        tok = scanner.scan();
    }

    private Scan scanner;
    Parser(Scan scanner) {
        this.scanner = scanner;
        this.symbolTable = new Stack<ArrayList<String>>();
        scan();
        program();
        if( tok.kind != TK.EOF )
            parse_error("junk after logical end of program");
            
    }

    /******************
    begins parsing and translating the program
    ******************/
    private void program() {
        System.out.println("#include <stdio.h>");
        System.out.println("main()");
        block();
    }

    /******************
    parses and translate a block of the program
    ******************/
    private void block(){
        // new block, create new list to push on stack
    	symbolTable.push(new ArrayList<String>());
    	System.out.println("{");

        declaration_list();
        statement_list();

        // end block, pop the list for ended block
        symbolTable.pop();
        System.out.println("}");

    }

    /******************
    parses and translate variable declarations
    ******************/
    private void declaration_list() {
        // below checks whether tok is in first set of declaration.
        // here, that's easy since there's only one token kind in the set.
        // in other places, though, there might be more.
        // so, you might want to write a general function to handle that.
        while( is(TK.DECLARE) ) {
            declaration();
        }
    }

    /******************
    parses and translates multiple declarations.
    ******************/
    private void declaration() {
        mustbe(TK.DECLARE);
        declareVar();
        mustbe(TK.ID);
        
        // if declaring more than one variable
        while( is(TK.COMMA) ) {
            scan();
            declareVar();
            mustbe(TK.ID);
        }
    }

    /******************
    reads in the name of the variable,
    pushes onto symbol table for scope checking,
    and translates appropriately.
    ******************/
    private void declareVar() {
        if(is(TK.ID)) {
            String level = addToTable();        //holds the "level" the variable is in
            if(level == "skip") {
                return;
            }
            System.out.println("int x_" + tok.string + level + ";"); //prints out variable and level if it is not the first
        }
    }

    /******************
    checks if list of statements are valid
    ******************/
    private void statement_list() {
        while(isStatement()) {
            statement();
        }
    }

    /******************
    checks if terminal is valid character to begin statement(s)
    ******************/
    private boolean isStatement() {
        if(isRef_ID() || is(TK.PRINT) || is(TK.DO) || is(TK.IF) || is(TK.FOR)) return true;
        return false;
    }

    /******************
    redirects to the appropriate options of different statement types
    ******************/
    private void statement() {
        if(isRef_ID())        assignment();
        else if(is(TK.PRINT)) print();
        else if(is(TK.DO))    doParse();
        else if(is (TK.IF))   ifParse();
        else if(is(TK.FOR))   forParse();
    }

    //for ::= '{' number id block '}'
    /******************
    parses and translates for loop
    ******************/
    private void forParse() {
        int num = 0;            //used to temp hold the number of iterations 
        String level = "";      //holds the "level" of the variable for scope checking
        if(is(TK.FOR)) {
            scan();
            
            if(is(TK.NUM)) num = Integer.parseInt(tok.string);
            
            scan();
            
            if(is(TK.ID)) {
                declareFor(num);
            }
        }
        mustbe(TK.ID);
        
        block();
        mustbe(TK.ENDFOR);
    }
    
    /******************
    reads in the name of the variable,
    pushes onto symbol table for scope checking,
    and translates a for loop appropriately.
    ******************/
    private void declareFor(int num) {
        if(is(TK.ID)) {
            String level = addToTable();        //holds the "level" of the variable, important for nesting
            if(level == "skip") {
                return;
            }
            System.out.println("int x_" + tok.string + level + ";");
            
            System.out.println("for(x_" + tok.string + level + " = 0; x_" + 
                                 tok.string + level + " < " + num + "; x_" + tok.string + level + "++)");
        }
    }

    /******************
    parses and translates while loop
    ******************/
    private void doParse() {
        mustbe(TK.DO);
        System.out.print("while(");
        guarded_command();
        mustbe(TK.ENDDO);
        System.out.println();
    }

    /******************
    parses and translates a guarded command
    ******************/
    private void guarded_command() {
        expr();
        System.out.print(" <= 0)");
        mustbe(TK.THEN);
        block();
    }

    /******************
    parses and translates if statements
    ******************/
    private void ifParse() {
        mustbe(TK.IF);
        System.out.print("if(");
        guarded_command();
        while (is(TK.ELSEIF)) {
            System.out.print("else if(");
            scan();
            guarded_command();
        }
        if(is(TK.ELSE)) {
            System.out.print("else");
            scan();
            block();
        }
        mustbe(TK.ENDIF);
        System.out.println();
    }

    /******************
    parses and translates the print statement
    ******************/
    private void print() {
        mustbe(TK.PRINT);
        System.out.print("printf(\"%d\\n\", ");
        expr();
	    System.out.println(");");
    }

    /******************
    checks if the syntax is appropriate for referencing a variable
    ******************/
    private boolean isRef_ID() {
        if(is(TK.TILDE) || is(TK.ID)) return true;
        return false;
    }

    /******************
     parses and translates the assignment of a variable
    ******************/
    private void assignment() {
        refID();
        mustbe(TK.ASSIGN);
        System.out.print(" = ");
        expr();
        System.out.println(";");
    }

    /******************
     parses and translates a referenced variable,
     also checks symbol table if it is in appropriate scope
    ******************/
    private void refID() {
    	String id;  //temp hold for the current tok.string
    	// default has no scope, can find variable anywhere
    	int scope = -2;     //holds the scope levle, intitialized to sentinel value
        if(is(TK.TILDE)) {
            scan();
            if(is(TK.NUM)) {
            	scope = Integer.parseInt(tok.string);
                scan();
            } else scope = -1; //global variable, search bottom of stack
        }
        if(is(TK.ID)) {
        	id = tok.string;
        	Integer level = findSymbol(scope, id);
            if (level == 1) System.out.print("x_" + tok.string);
        	else System.out.print("x_" + tok.string + level);
        }
        mustbe(TK.ID);
    }

    /******************
     searches symbol table for wanted variable,
     to validate scoping
    ******************/
    private Integer findSymbol(int scope, String id) {
    Iterator<ArrayList<String>> stackIterator = symbolTable.iterator(); //used to iterate the stack for scope checking
    ArrayList<String> currentBlock;                                     //holds the block currently being searched
	if (scope == -2) {
	    Integer i = 0;
	    Boolean found = false;
		while(stackIterator.hasNext()) {
			currentBlock = stackIterator.next();
			if(currentBlock.contains(id)) {
			    i++;
    			    found = true;
    			}
    		}
    		if(found) return i;
			System.err.println(id + " is an undeclared variable on line " + tok.lineNumber);
			System.exit(1);
			return -1;
    	} else if (scope == -1) {
            currentBlock = stackIterator.next();
    		if(currentBlock.contains(id)) return 1;
    		System.err.println("no such variable ~" + id + " on line " + tok.lineNumber);
    	    System.exit(1);
    	    return -1;
    	} else if (scope < symbolTable.size()) {
    		int level = symbolTable.size() - scope;
    		Integer j = 0;
    		currentBlock = stackIterator.next();
    		if(currentBlock.contains(id)) j++;
    		for(int i = 1; i < level; i++) {
    			currentBlock = stackIterator.next();
    			if(currentBlock.contains(id)) j++;
    		}
   			if(currentBlock.contains(id)) return j;
    	}
		System.err.println("no such variable ~" +  scope + id + " on line " + tok.lineNumber);
		System.exit(1);
		return -1;
    }

    /******************
    parses and translate an expression
    ******************/
    private void expr() {
        term();
        while(isAddop()) {
            System.out.print(" " + tok.string + " ");
            scan();
            term();
        }
    }

    /******************
    checks for addtion/subtraction terminal
    ******************/
    private boolean isAddop() {
        if(is(TK.PLUS) || is(TK.MINUS)) return true;
        return false;
    }

    /******************
    parses and translate each term
    ******************/
    private void term() {
        factor();
        while(isMultop()) {
            System.out.print(" " + tok.string + " ");
            scan();
            factor();
        }
    }

    /******************
    checks for multiplication/division terminal
    ******************/
    private boolean isMultop() {
        if(is(TK.TIMES) || is(TK.DIVIDE)) return true;
        return false;
    }

    /******************
    parses and translate individual factors
    ******************/
    private void factor() {
        if(is(TK.LPAREN)) {
            System.out.print("(");
            scan();
            expr();
            mustbe(TK.RPAREN);
            System.out.print(")");
        }
        else if(isRef_ID()) {
            refID();
        }
        else {
            if(is(TK.NUM)) System.out.print(tok.string);
            mustbe(TK.NUM);
        }
    }



    // is current token what we want?
    private boolean is(TK tk) {
        return tk == tok.kind;
    }

    // ensure current token is tk and skip over it.
    private void mustbe(TK tk) {
        if( tok.kind != tk ) {
            System.err.println( "mustbe: want " + tk + ", got " + tok);
            parse_error( "missing token (mustbe)" );
        }
        scan();
    }

    /******************
    adds ID to symbol table
    ******************/
    private String addToTable() {
    	ArrayList<String> currentBlock = symbolTable.peek();        //used to hold the top of the stack, and for traversal
    	if(currentBlock.contains(tok.string))  {
    		System.err.println("redeclaration of variable " + tok.string);
    		return "skip";
    	}
    	Integer i = 1;
    	Iterator<ArrayList<String>> stackIterator = symbolTable.iterator();     //function used to iterate through the stack
    	ArrayList<String> thisBlock;                        //holds the stack currently being searched
    	while(stackIterator.hasNext()) {
    	    thisBlock = stackIterator.next();
    	    if(thisBlock.contains(tok.string)) i++;
    	}
    	currentBlock.add(tok.string);

   	if(i == 1) return "";
    	return i.toString();
    }

    /******************
    displays error message is parsing detects error
    ******************/
    private void parse_error(String msg) {
        System.err.println( "can't parse: line " + tok.lineNumber + " " + msg );
        System.exit(1);
    }
}