grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : ([0]|[1-9][0-9]*);
ID : [a-zA-Z_$][a-zA-Z_0-9$]*;
STRING : '"' .*? '"' ;
CHAR : '\'' ( [^'\\\t\n\r\f] | '\\' . ) '\'';


COMMENT : '//' ~[\r\n]* -> channel(HIDDEN);
MULTICOMMENT : '/*' .*? '*/' -> channel(HIDDEN);

WS : [ \t\n\r\f]+ -> skip;

program
    : importDeclaration* classDeclaration  EOF
    ;

importDeclaration
    : 'import' values+=ID ('.' values+=ID)* ';'
    ;

classDeclaration
    : 'class' name=ID ('extends' superClass=ID)? '{' (declaration ';')* method* '}'
    ;

method
    : visibility=('public'|'private')? (istatic='static')? type name=ID '(' (declaration (',' declaration)*)? ')' '{' statement* '}'
    ;


statement
    : '{' statement* '}' #Scope
    | 'if' '('  expression ')' statement ('else' statement )? #IfElse
    | 'while' '(' expression ')' statement #While
    | 'return' expression? ';' #Return
    | expression ';' #ExpressionST
    | var=ID '=' expression ';' #Assignment
    | var=ID ('[' expression ']') '=' expression ';' #ArrayAssignment
    | declaration ';' #DeclarationST
    ;

expression
    : '(' expression ')' #Parenthesis
     // With return type
    | 'new' type'['expression']' #NewArray
    | 'new' type '(' ')' #NewObject
    | expression ( '.' value=ID ) ( '('(expression (',' expression)*)?')' ) #MethodCall
    | expression ( '.' value=ID )  #FieldAccess
    | expression '['expression']' #ArrayAccess
    | expression '.length' #ArrayLength
    // Operators with precedence
    | op='!' expression #UnaryOp
    | op='-' expression #UnaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('==' | '!=') expression #BinaryOp
    | expression op='<' expression #BinaryOp
    | expression op=('&&' | '||') expression #BinaryOp
    // Literals
    | value=('true'|'false') #Boolean
    | value=INTEGER #Integer
    | value=ID #Identifier
    | value=STRING #String
    | value=CHAR #Char
    | 'null' #Null
    | 'this' #This
    ;

declaration
    : type name=ID '=' expression #Initialization
    | type name=ID  #VariableDeclaration
    ;

type
    : value='int' #PrimitiveType
    | value='boolean' #PrimitiveType
    | value='void' #PrimitiveType
    | value='char' #PrimitiveType
    | value='short' #PrimitiveType
    | value='long' #PrimitiveType
    | value='float' #PrimitiveType
    | value='double' #PrimitiveType
    | value='byte' #PrimitiveType
    | value='String' #PrimitiveType
    | value=ID #ClassType
    | type'[]' #ArrayType
    ;
