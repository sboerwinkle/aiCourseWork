package Vole;

import java.io.Reader;
import java.io.Writer;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Scanner;
import java.lang.System;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.math.BigInteger;


abstract class Expression{
	Expression(){}

	boolean isAtom(){
		if(this instanceof Atom)
			return true;
		return false;
	}
	
	boolean isNumber(){
		if(this instanceof NumberVal)
			return true;
		return false;
	}

	boolean isString(){
		if(this instanceof StringVal)
			return true;
		return false;
	}

	boolean isSymbol(){
		if(this instanceof SymbolVal)
			return true;
		return false;
	}

	boolean isBoolean(){
		if(this instanceof BooleanVal)
			return true;
		return false;
	}

	boolean isPair(){
		if(this instanceof Pair && !this.isNil())
			return true;
		return false;
	}

	boolean isProcedure(){
		if(this instanceof ProcedureVal)
			return true;
		return false;
	}

	boolean isLambda(){
		if(this instanceof Lambda)
			return true;
		return false;
	}

	boolean isThunk(){
		if(this instanceof Thunk)
			return true;
		return false;
	}

	boolean isJavaFunction(){
		if(this instanceof JavaFunction)
			return true;
		return false;
	}

	boolean isNil(){
		if(this instanceof Pair){
			Expression car = ((Pair) this).getCar();
			Expression cdr = ((Pair) this).getCdr();
			if(car == null && cdr == null)
				return true;
		}
		return false;
	}

	boolean isList(){
		if(this.isPair()){
			Expression cdr = ((Pair) this).getCdr();
			if(cdr instanceof Pair)
				return true;
		}
		return false;
	}

	boolean isPort(){
		if(this instanceof Port)
			return true;
		return false;
	}

	boolean isEof(){
		if(this instanceof EofVal)
			return true;
		return false;
	}

	@Override
	abstract public boolean equals(Object e);

	@Override
	abstract public String toString();

}

abstract class Atom extends Expression{
	Atom(){}

}

class NumberVal extends Atom{
	BigInteger val;

	NumberVal(BigInteger val){
		this.val = val;
	}

	BigInteger getVal(){
		return val;
	}

	public String toString(){
		return val.toString();
	}

	public boolean equals(Object e){
		if(e instanceof NumberVal && ((NumberVal)e).getVal().equals(this.val))
			return true;
		else
			return false;
	}
}

class StringVal extends Atom{
	String val;

	StringVal(String str){
		this.val = str;
	}

	String getVal(){
		return val;
	}

	public String toString(){
		return "\"".concat(val).concat("\"");
	}

	public boolean equals(Object e){
		if(e instanceof StringVal && ((StringVal) e).getVal().equals(this.val))
			return true;
		else
			return false;
	}

}

class SymbolVal extends Atom{
	String identifier;

	SymbolVal(String ident){
		identifier = ident;
	}

	String getIdentifier(){
		return identifier;
	}

	@Override
	public boolean equals(Object key){
		if(key instanceof SymbolVal){
			SymbolVal keySym = (SymbolVal) key;
			if(keySym.getIdentifier().equals(this.identifier))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return identifier.hashCode();
	}

	public String toString(){
		return identifier;
	}
}

class BooleanVal extends Atom{
	boolean val;

	BooleanVal(Boolean val){
		this.val = val;
	}

	boolean getVal(){
		return val;
	}

	public String toString(){
		return val ? "#t" : "#f";
	}

	public boolean equals(Object e){
		if(e instanceof BooleanVal && ((BooleanVal) e).getVal() == val)
			return true;
		else
			return false;
	}
}

class Thunk extends Atom{
	Expression exp;
	Environment env;

	Thunk(Expression exp, Environment env){
		this.exp = exp;
		this.env = env;
	}

	Expression getExp(){
		return exp;
	}

	Environment getEnv(){
		return env;
	}

	public String toString(){
		return "<Thunk exp=".concat(exp.toString());
	}

	public boolean equals(Object e){
		if(e.equals(this))
			return true;
		else
			return false;
	}

}

class Pair extends Expression{
	Expression car;
	Expression cdr;

	Pair(Expression car, Expression cdr){
		this.car = car;
		this.cdr = cdr;
	}

	Expression getCar(){
		return car;
	}

	Expression getCdr(){
		return cdr;
	}

	void setCdr(Expression exp){
		cdr = exp;
	}

	void setCar(Expression exp){
		car = exp;
	}

	public String toString(){
		if(this.isNil())
			return "(list)";
		else
			return "(cons ".concat(car.toString()).concat(" ").concat(cdr.toString()).concat(")");
	}

	public boolean equals(Object e){
		if(	e instanceof Pair &&
			((Pair) e).isNil() &&
			this.isNil())
			return true;

		else if(	e instanceof Pair &&
			((Pair) e).getCar().equals(this.car) &&
			((Pair) e).getCdr().equals(this.cdr)){

			return true;
		}
		return false;
	}
}


//Interface for adding functions written in java
abstract class ProcedureVal extends Atom{
	ProcedureVal(){}

	public boolean equals(Object e){
		if(e.equals(e))
			return true;
		else
			return false;
	}
}

//look at using anonymous classes for these
abstract class JavaFunction extends ProcedureVal{
	JavaFunction(){}
	abstract Expression call(Expression args) throws Exception;

	public String toString(){
		return "<java-function>";
	}
	
	public boolean equals(Object e){
		if(e.equals(this))
			return true;
		else
			return false;
	}
}


class Lambda extends ProcedureVal {
	Environment closure;
	SymbolVal arg;
	Expression exp;

	Lambda(Environment closure, SymbolVal arg, Expression exp){
		this.closure = closure;
		this.arg = arg;
		this.exp = exp;
	}
	
	Lambda(Expression exp, Environment env){
		try{
			closure = new Environment(env);
			if(exp.isList()){
				Expression car = ((Pair) exp).getCar();
				Expression cdr = ((Pair) exp).getCdr();
				if(car.isSymbol()){
					arg = (SymbolVal) car;
				}else if(car.isNil()){
					arg = null;
				}else{
					throw new Exception("Lambda expects the first argument to be a symbol or nil.");
				}
				this.exp = ((Pair)cdr).getCar();
			}else{
				throw new Exception("Lambda arguments in an unexpected form.");
			}

		}catch(Exception e){
			e.printStackTrace();
		}
	}

	Environment getClosure(){
		return closure;
	}

	SymbolVal getArg(){
		return arg;
	}

	Expression getExp(){
		return exp;
	}

	Environment getEvalEnvironment(Expression val, Environment env){
		Environment newEnv = new Environment(env);
		newEnv.concat(closure);
		if(arg != null)
			newEnv.add(arg,val);
		return newEnv;
	}

	public String toString(){
		StringWriter w = new StringWriter();
		Printer.printExpression(exp,w);
		return "<lambda arg=".concat(arg.getIdentifier()).concat(" exp=").concat(w.toString()).concat(">");
	}

	public boolean equals(Object e){
		if(e.equals(this))
			return true;
		else
			return false;
	}
}

class Port extends Atom{
	//Input and output are from the
	//users perspective. A program
	//will read in input and write out
	//output.
	Reader input;
	Writer output;

	Port(){
		input = null;
		output = null;
	}

	Port(Reader input, Writer output){
		//Input must be buffered so that peek() in parseSexp() will work.
		//If it is not (read) will not work. Output is left unbuffered
		//for now I don't see a need to buffer it. If it's buffered it
		//might lead to a bunch of annoying things like having to flush
		//the buffer to get stuff to write.
		this.input = input==null ? null : new BufferedReader(input);
		this.output = output;
	}
	
	Reader getInput(){
		return input;
	}

	Writer getOutput(){
		return output;
	}

	public String toString(){
		return "<port>";
	}

	public boolean equals(Object e){
		if(e.equals(this))
			return true;
		else
			return false;
	}
}

class EofVal extends Atom{
	EofVal(){}

	public String toString(){
		return "<eof>";
	}

	public boolean equals(Object e){
		if(e instanceof EofVal)
			return true;
		else
			return false;
	}
}


class Environment extends Atom{
	Map<SymbolVal,Expression> map;

	Environment(){
		this.map = new HashMap<SymbolVal,Expression>();
	}

	Environment(Environment e){
		this.map = new HashMap<SymbolVal,Expression>(e.getMap());
	}

	Map<SymbolVal,Expression> getMap(){
		return map;
	}

	void add(SymbolVal key, Expression value){
		this.map.put(key,value);
	}

	void concat(Environment env){
		this.map.putAll(env.getMap());
	}

	Expression lookUp(SymbolVal val) throws Exception{
		Expression result = map.get(val);
		return result;
	}

	public String toString(){
		return "<environment>";
	}

	public boolean equals(Object e){
		if(e.equals(this))
			return true;
		else
			return false;
	}

}

class Evaluator{
	static boolean debug = false;

	Evaluator(){}

	public static Expression trampoline(Expression thunk) throws Exception{

		Expression result = thunk;
		while(result != null && result.isThunk()){
			if(debug){
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));
				System.out.print("Boing!: ");
				Printer.printExpression(((Thunk)result).getExp(),writer);
				writer.flush();
				System.out.println();
			}

			result = eval_tramp(((Thunk)result).getExp(),((Thunk)result).getEnv());
		}
		return result;
	}

	public static Expression apply(Expression fn, Expression args, Environment env) throws Exception{
		return trampoline(apply_tramp(fn,args,env));
	}

	public static Expression apply_tramp(Expression fn, Expression args, Environment env) throws Exception {
		if(debug){
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));
			System.out.print("apply() called on:\t");
			Printer.printExpression(new Pair(fn,args),writer);
			writer.flush();
			System.out.println();
		}

		if(fn.isLambda()){
			Lambda lambda = (Lambda) fn;
			//((<lambda> first-arg) rest-args)
			if(args.isList() && !((Pair) args).getCdr().isNil()) {
				return new Thunk( new Pair( new Pair( lambda, new Pair( ((Pair) args).getCar(), new Pair(null,null))), ((Pair) args).getCdr()),env);
			}else{
				Environment lambdaEnv = lambda.getEvalEnvironment(((Pair)args).getCar(),env);
				return new Thunk(lambda.getExp(),lambdaEnv);
			}
		}else if(fn.isJavaFunction()){
			JavaFunction jfunc = (JavaFunction) fn;
			return jfunc.call(args);
		}else{
			StringWriter error = new StringWriter();
			error.append("Apply could not apply function ");
			Printer.printExpression(fn,error);
			error.append(" to args ");
			Printer.printExpression(args,error);
			throw new Exception(error.toString());
		}
		
	}

	public static Expression eval(Expression exp, Environment env) throws Exception{
		return trampoline(eval_tramp(exp,env));
	}

	public static Expression eval_string(String expString, Environment env) throws Exception{
		Expression exp = Parser.parseSexp(new StringReader(expString));
		return eval(exp,env);
	}

	public static Expression eval_tramp(Expression exp, Environment env) throws Exception{

		if(debug){
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));
			System.out.print("eval() called on:\t");
			Printer.printExpression(exp,writer);
			writer.flush();
			System.out.println();
		}

		if(exp == null)
			return exp;

		if(exp.isAtom()){
			if(exp.isSymbol()){
				Expression val = env.lookUp((SymbolVal) exp);
				if(val != null)
					return val;
				else
					throw new Exception("Symbol ".concat(((SymbolVal) exp).getIdentifier()).concat(" is undefined."));
			}else{
				return exp;
			}
		}else if(exp.isList()){
			Pair expList = (Pair) exp;
			Expression car = expList.getCar();
			Expression cdr = expList.getCdr();
			if(car.isAtom()){
				if(car.isSymbol()){
					SymbolVal sym = (SymbolVal) car;

					Expression val = env.lookUp(sym);

					//(defined-symbol args)
					if(val != null)
						return apply_tramp(val,evlis(cdr,env),env);

					else if(sym.getIdentifier().equals("if")){
						Pair list = (Pair) cdr;
						//It's okay to introduce a new stack frame here because
						//there is no possible way this is a tail call
						Expression a = trampoline(eval_tramp(list.getCar(),env));
						Expression resultThunkExp;
						if(a.isBoolean() && ((BooleanVal)a).getVal() == true)
							resultThunkExp = ((Pair)list.getCdr()).getCar();
						else
							resultThunkExp = ((Pair)((Pair) list.getCdr()).getCdr()).getCar();
						return new Thunk(resultThunkExp,env);

					}else if(sym.getIdentifier().equals("lambda"))
						return new Lambda(cdr,env);
					else if(sym.getIdentifier().equals("define")){
						if(cdr.isList()){
							Expression cadr = ((Pair) cdr).getCar();
							Expression caddr =((Pair) ((Pair) cdr).getCdr()).getCar();
							if(cadr.isSymbol()){
								String name = ((SymbolVal) cadr).getIdentifier();
								Expression currentValue = env.lookUp((SymbolVal) cadr);
								if(currentValue != null){
									throw new Exception("Symbol ".concat(name).concat(" is already defined."));
								}		
								env.add((SymbolVal) cadr, eval(caddr,env));
								return null;
							}else{
								throw new Exception("define expects a symbol as the first argument.");
							}
						}else{
							throw new Exception("define expects at least two arguments.");
						}
					}else if(sym.getIdentifier().equals("current-environment")){
						return env;
					}else if(sym.getIdentifier().equals("load")){
						String filename;
						Expression cadr = ((Pair) cdr).getCar();
						if(cadr.isString())
							filename = ((StringVal) cadr).getVal();
						else
							throw new Exception("(load _) expects a filename as the first argument.");
						load(filename,env);
						return null;
					}else if(sym.getIdentifier().equals("toggle-debug")){
						debug = debug ? false : true;
						return new BooleanVal(debug);

					}else{
						throw new Exception("Undefined symbol: ".concat(car.toString()));
					}
				}
				//(<fn> args)
				return apply_tramp(car,evlis(cdr,env), env);
			}
			//((stuff) args ...)
			return apply_tramp(trampoline(eval_tramp(car,env)),evlis(cdr,env), env);
				
		}

		throw new Exception("Eval couldn't couldn't figure out how to evaluate that statement. Maybe you should rethink it.");

	}

	public static Expression evlis(Expression list, Environment env) throws Exception{
		if(list.isAtom())
			return list;
		else{
			Pair listPair = (Pair) list;
			if(listPair.isNil())
				return listPair;
			else
				return new Pair(eval(listPair.getCar(),env),evlis(listPair.getCdr(),env));
		}
	}

	public static void load(String filename, Environment env) throws Exception{

		Reader input = new BufferedReader(new FileReader(filename));
		
		Expression exp = Parser.parseSexp(input);

		while(exp != null){
			eval(exp,env);
			exp = Parser.parseSexp(input);
		}

	}

}

class Printer{
	
	Printer(){}

	public static void printList(Expression expr, Writer out) throws Exception{
		if(expr.isList()){
			Pair p = (Pair) expr;
			printExpression(p.getCar(),out);
			if(p.getCdr().isPair() && ! p.getCdr().isNil()){
				out.write(" ");
				printList(p.getCdr(),out);
			}
		}else{
			printExpression(expr,out);
		}
	}

	public static void printExpression(Expression expr, Writer out){
		try{
			if(expr == null){
				out.write("<unspecified>");
				return;
			}else if(expr.isList()){
				out.write("(");
				printList(expr,out);
				out.write(")");
				return;
			}else{
				out.write(expr.toString());
			}


		}catch(Exception e){
			e.printStackTrace();
		}
	}
}

class Parser{
	Parser(){}

	static int peek(Reader input){
		int c = 0;
		try{
			input.mark(1);
			c = (char) input.read();
			input.reset();
		}catch(Exception e){
			e.printStackTrace();
		}
		return c;
	}

	public static BooleanVal parseBoolean(Reader input) throws Exception{
		input.read();
		int c = input.read();
		if(c == 't')
			return new BooleanVal(true);
		else if(c == 'f')
			return new BooleanVal(false);
		else
			throw new Exception("Attempted to parse boolean but no boolean to be found.");
	}

	public static NumberVal parseNumber(Reader input) throws Exception{
		StringBuilder builder = new StringBuilder();
		int c = 0;

		c = peek(input);
		while( 	c != '#' &&
			c != '(' &&
			c != ')' &&
			c != ';' &&
		(short) c != -1  &&
			Character.isDigit(c) &&
			!Character.isWhitespace(c)){
			
			builder.append((char) c);
			input.read();
			c = peek(input);
		}

		Scanner s = new Scanner(builder.toString());

		return new NumberVal(s.nextBigInteger());
	}

	public static SymbolVal parseSymbol(Reader input) throws Exception{
		StringBuilder builder = new StringBuilder();
		int c = 0;

		c = peek(input);
		while( 	c != '#' &&
			c != '(' &&
			c != ')' &&
			c != ';' &&
		(short) c != -1  &&
			!Character.isWhitespace(c)){
			
			builder.append((char) c);
			input.read();
			c = peek(input);
		}

		return new SymbolVal(builder.toString());

	}

	public static StringVal parseString(Reader input) throws Exception{
		StringBuilder builder = new StringBuilder();
		int c = 0;

		//eat the initial '"'
		input.read();
		c = peek(input);
		while( 	c != '"' &&
			(short) c != -1 ){
			
			builder.append((char) c);
			input.read();
			c = peek(input);
		}
		//eat the end '"'
		input.read();

		return new StringVal(builder.toString());
		
	}

	public static void parseComment(Reader input) throws Exception{
		int c = peek(input);
		while(c != '\n' && (short) c != -1){
			input.read();
			c = peek(input);
		}
		return;
	}

	public static Pair parseList(Reader input) throws Exception{
		int c;
		Pair head = null;
		Pair tail = null;

		c = peek(input);

		if(c == '('){
			//eat the '('
			input.read();
			
			c = peek(input);

			if((short) c == -1)
				throw new Exception("Unmatched '(' in file.");

			while((short) c != ')'){

				Expression exp = null;

				if(c == '(')
					exp = parseList(input);

				else if(c == '#')
					exp = parseBoolean(input);

				else if(c == ';'){
					parseComment(input);
					c = peek(input);
					continue;
				}
				
				else if(Character.isDigit(c))
					exp = parseNumber(input);

				else if(c == '"')
					exp = parseString(input);

				else if(Character.isWhitespace(c)){
					input.read();
					c = peek(input);
					continue;
				}

				else if((short) c == -1)
					throw new Exception("Unexpected EOF.");

				//If it's none of those things it must be a symbol
				else 
					exp = parseSymbol(input);
				
				if(head == null){
					head = new Pair(exp,null);
					tail = head;
				}else{
					Pair newTail = new Pair(exp,null);
					tail.setCdr(newTail);
					tail = newTail;
				}

				c = peek(input);

			}

			//eat the ')'
			input.read();
			

		}else{
			throw new Exception("parseList() expected to start on a '('.");
		}

		if(head != null)
			tail.setCdr(new Pair(null,null));
		else
			head = new Pair(null,null);

		return head;

	}

	public static Expression parseSexp(Reader input) throws Exception{

		int c;

		c = peek(input);

		while((short) c != -1){
			if(Character.isWhitespace(c)){
				input.read();
				c = peek(input);
				continue;
			}

			else if(c == '#'){
				return parseBoolean(input);
			}

			else if(Character.isDigit(c)){
				return parseNumber(input);
			}
			
			else if(c == '"'){
				return parseString(input);
			}
			
			else if(c == '('){
				return parseList(input);
			}

			else if(c == ';'){
				parseComment(input);
				c = peek(input);
				continue;
			}
			else if(c == ')'){
				throw new Exception("Unmatched ')' found.");
			}

			else
				return parseSymbol(input);
			

		}

		return null;
	}


}


class Core{
	Core(){}

	static Environment getEnv(){
		Environment env = new Environment();

		JavaFunction error = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair list = (Pair) exp;
				Expression a = list.getCar();
				throw new Exception(((StringVal) a).getVal());
			}
		};
		env.add(new SymbolVal("error"),error);

		//eval just returns a thunk to bounce off
		//of the trampoline.
		JavaFunction eval = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair list = (Pair) exp;
				Expression a = list.getCar();
				Expression b = ((Pair)list.getCdr()).getCar();
				return new Thunk(a,(Environment) b);
			}
		};
		env.add(new SymbolVal("eval"),eval);

		JavaFunction apply = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair list = (Pair) exp;
				Expression fn = list.getCar();
				Expression args = ((Pair)list.getCdr()).getCar();
				Expression env = ((Pair) ((Pair) list.getCdr()).getCdr()).getCar();
				return Evaluator.apply_tramp(fn,args,(Environment) env);
			}
		};
		env.add(new SymbolVal("apply"),apply);

		JavaFunction eq = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair list = (Pair) exp;
				Expression a = list.getCar();
				Expression b = ((Pair)list.getCdr()).getCar();
				//hopefully this does what I think it does.
				//We need to cast 'a to an object so that we don't use
				//the atoms equals() function which will actually check values.
				if(a == b)
					return new BooleanVal(true);
				else
					return new BooleanVal(false);
			}
		};
		env.add(new SymbolVal("eq?"),eq);

		JavaFunction eqv = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair list = (Pair) exp;
				Expression a = list.getCar();
				Expression b = ((Pair)list.getCdr()).getCar();

				if(	a.isNumber() &&
					b.isNumber()){
					if(	((NumberVal) a).getVal().equals(((NumberVal) b).getVal()))
						return new BooleanVal(true);
					else
						return new BooleanVal(false);
				}

				else if(	a.isSymbol() &&
						b.isSymbol()){
					if(((SymbolVal) a).getIdentifier().equals(((SymbolVal) b).getIdentifier()))
						return new BooleanVal(true);
					else
						return new BooleanVal(false);
				}

				else if(	a.isBoolean() &&
						b.isBoolean()){
					if(((BooleanVal) a).getVal() == ((BooleanVal) b).getVal())
						return new BooleanVal(true);
					else
						return new BooleanVal(false);
				}

				else if(	a.isString() &&
						b.isString()){
					if(((StringVal) a).getVal().equals(((StringVal) b).getVal()))
						return new BooleanVal(true);
					else
						return new BooleanVal(false);
				}
				return new BooleanVal(false);

			}
		};
		env.add(new SymbolVal("eqv?"),eqv);

		JavaFunction equals = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair list = (Pair) exp;
				Expression a = list.getCar();
				Expression b = ((Pair)list.getCdr()).getCar();
				return new BooleanVal(a.equals(b));
			}
		};
		env.add(new SymbolVal("equals?"),equals);

		JavaFunction cons = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair list = (Pair) exp;
				Expression a = list.getCar();
				Expression b = ((Pair)list.getCdr()).getCar();
				return new Pair(a,b);
			}
		};
		env.add(new SymbolVal("cons"),cons);

		JavaFunction list = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				return exp;
			}
		};
		env.add(new SymbolVal("list"),list);

		JavaFunction car = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				//get the first argument
				exp = ((Pair)exp).getCar();
				if(exp.isPair()){
					Expression a = ((Pair) exp).getCar();
					return a;
				}else{
					throw new Exception("Car expects a pair as argument.");
				}
			}
		};
		env.add(new SymbolVal("car"),car);

		JavaFunction cdr = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				//get the first argument
				exp = ((Pair) exp).getCar();
				if(exp.isPair()){
					Expression a = ((Pair) exp).getCdr();
					return a;
				}else{
					throw new Exception("Car expects a list as argument.");
				}
			}
		};
		env.add(new SymbolVal("cdr"),cdr);

		JavaFunction isPair = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				exp = ((Pair) exp).getCar();
				if(exp.isPair()){
					return new BooleanVal(true);
				}else{
					return new BooleanVal(false);
				}
			}
		};
		env.add(new SymbolVal("pair?"),isPair);

		JavaFunction isSymbol = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				exp = ((Pair) exp).getCar();
				if(exp.isSymbol()){
					return new BooleanVal(true);
				}else{
					return new BooleanVal(false);
				}
			}
		};
		env.add(new SymbolVal("symbol?"),isSymbol);

		JavaFunction isBoolean = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				exp = ((Pair) exp).getCar();
				if(exp.isBoolean()){
					return new BooleanVal(true);
				}else{
					return new BooleanVal(false);
				}
			}
		};
		env.add(new SymbolVal("boolean?"),isBoolean);

		JavaFunction not = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				exp = ((Pair) exp).getCar();
				if(exp.isBoolean()){
					return ((BooleanVal) exp).getVal() ? new BooleanVal(false) : new BooleanVal(true);
				}else{
					throw new Exception("(not _) expects a boolean value as an argument.");
				}
			}
		};
		env.add(new SymbolVal("not"),not);

		JavaFunction isAtom = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				exp = ((Pair) exp).getCar();
				if(exp.isAtom()){
					return new BooleanVal(true);
				}else{
					return new BooleanVal(false);
				}
			}
		};
		env.add(new SymbolVal("atom?"),isAtom);

		JavaFunction isNumber = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				exp = ((Pair) exp).getCar();
				if(exp.isNumber()){
					return new BooleanVal(true);
				}else{
					return new BooleanVal(false);
				}
			}
		};
		env.add(new SymbolVal("number?"),isNumber);

		JavaFunction isString = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				exp = ((Pair) exp).getCar();
				if(exp.isString()){
					return new BooleanVal(true);
				}else{
					return new BooleanVal(false);
				}
			}
		};
		env.add(new SymbolVal("string?"),isString);

		JavaFunction isList = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				exp = ((Pair) exp).getCar();
				if(exp.isList()){
					return new BooleanVal(true);
				}else{
					return new BooleanVal(false);
				}
			}
		};
		env.add(new SymbolVal("list?"),isList);

		JavaFunction stringToSymbol = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				exp = ((Pair) exp).getCar();
				if(exp.isString())
					return new SymbolVal(((StringVal)exp).getVal());
				else
					throw new Exception("(string->symbol _) expects a string as an argument.");
				
			}
		};
		env.add(new SymbolVal("string->symbol"),stringToSymbol);

		JavaFunction isNil = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				exp = ((Pair) exp).getCar();
				if(exp.isNil()){
					return new BooleanVal(true);
				}else{
					return new BooleanVal(false);
				}
			}
		};
		env.add(new SymbolVal("nil?"),isNil);

		return env;
	}

}

class MathLib{

	MathLib(){}

	static Environment getEnv(){

		Environment env = new Environment();

		JavaFunction add = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				NumberVal a = (NumberVal) (expPair.getCar());
				NumberVal b = (NumberVal) ((Pair) expPair.getCdr()).getCar();

				return new NumberVal(a.getVal().add(b.getVal()));
			}
		};

		env.add(new SymbolVal("+"),add);

		JavaFunction subtract = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				NumberVal a = (NumberVal) (expPair.getCar());
				NumberVal b = (NumberVal) ((Pair) expPair.getCdr()).getCar();

				return new NumberVal(a.getVal().subtract(b.getVal()));
			}
		};

		env.add(new SymbolVal("-"),subtract);

		JavaFunction multiply = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				NumberVal a = (NumberVal) (expPair.getCar());
				NumberVal b = (NumberVal) ((Pair) expPair.getCdr()).getCar();

				return new NumberVal(a.getVal().multiply(b.getVal()));
			}
		};

		env.add(new SymbolVal("*"),multiply);

		JavaFunction divide = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				NumberVal a = (NumberVal) (expPair.getCar());
				NumberVal b = (NumberVal) ((Pair) expPair.getCdr()).getCar();

				return new NumberVal(a.getVal().divide(b.getVal()));
			}
		};

		env.add(new SymbolVal("/"),divide);

		JavaFunction and = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				NumberVal a = (NumberVal) (expPair.getCar());
				NumberVal b = (NumberVal) ((Pair) expPair.getCdr()).getCar();

				return new NumberVal(a.getVal().and(b.getVal()));
			}
		};

		env.add(new SymbolVal("bitwise-and"),and);

		JavaFunction divideWithRemainder = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				NumberVal a = (NumberVal) (expPair.getCar());
				NumberVal b = (NumberVal) ((Pair) expPair.getCdr()).getCar();
				BigInteger[] result = a.getVal().divideAndRemainder(b.getVal());
				return new Pair(new NumberVal(result[0]), new NumberVal(result[1]));
			}
		};

		env.add(new SymbolVal("divide-with-remainder"),divideWithRemainder);

	//Comparisons

		JavaFunction gt = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				NumberVal a = (NumberVal) (expPair.getCar());
				NumberVal b = (NumberVal) ((Pair) expPair.getCdr()).getCar();

				return new BooleanVal(a.getVal().compareTo(b.getVal()) > 0);
			}
		};

		env.add(new SymbolVal(">"),gt);

		JavaFunction lt = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				NumberVal a = (NumberVal) (expPair.getCar());
				NumberVal b = (NumberVal) ((Pair) expPair.getCdr()).getCar();

				return new BooleanVal(a.getVal().compareTo(b.getVal()) < 0);
			}
		};

		env.add(new SymbolVal("<"),lt);

		JavaFunction eq = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				NumberVal a = (NumberVal) (expPair.getCar());
				NumberVal b = (NumberVal) ((Pair) expPair.getCdr()).getCar();

				return new BooleanVal(a.getVal().compareTo(b.getVal()) == 0);
			}
		};

		env.add(new SymbolVal("="),eq);

		JavaFunction gteq = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				NumberVal a = (NumberVal) (expPair.getCar());
				NumberVal b = (NumberVal) ((Pair) expPair.getCdr()).getCar();

				return new BooleanVal(a.getVal().compareTo(b.getVal()) >= 0);
			}
		};

		env.add(new SymbolVal(">="),gteq);

		JavaFunction lteq = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				NumberVal a = (NumberVal) (expPair.getCar());
				NumberVal b = (NumberVal) ((Pair) expPair.getCdr()).getCar();

				return new BooleanVal(a.getVal().compareTo(b.getVal()) <= 0);
			}
		};

		env.add(new SymbolVal("<="),lteq);

		JavaFunction abs = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				NumberVal a = (NumberVal) (expPair.getCar());
				return new NumberVal(a.getVal().abs());
			}
		};

		env.add(new SymbolVal("abs"),abs);


		return env;
	}

}

class IOLib{

	static Port currentInputPort;
	static Port currentOutputPort;
	static Port currentErrorPort;

	IOLib(){}

	public static Environment getEnv(){
		Environment env = new Environment();

		JavaFunction isEof = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				Expression a = expPair.getCar();
				if(a.isEof())
					return new BooleanVal(true);
				else
					return new BooleanVal(false);
			}
		};

		env.add(new SymbolVal("eof?"),isEof);

		JavaFunction isPort = new JavaFunction(){
			Expression call(Expression exp){
				Pair expPair = (Pair) exp;
				Expression a = expPair.getCar();
				if(a.isPort())
					return new BooleanVal(true);
				else
					return new BooleanVal(false);
			}
		};

		env.add(new SymbolVal("port?"),isPort);

		JavaFunction openInputFilePort = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair expPair = (Pair) exp;
				Expression a = expPair.getCar();
				if(a.isString())
					return new Port(new FileReader(((StringVal) a).getVal()),null);
				else
					throw new Exception("(open-input-file _) expects a filename as an argument.");
			}
		};

		env.add(new SymbolVal("open-input-file"),openInputFilePort);

		JavaFunction openOutputFilePort = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair expPair = (Pair) exp;
				Expression a = expPair.getCar();
				if(a.isString())
					return new Port(null, new FileWriter(((StringVal) a).getVal()));
				else
					throw new Exception("(open-output-file _) expects a filename as an argument.");
			}
		};

		env.add(new SymbolVal("open-output-file"),openOutputFilePort);

		JavaFunction closePort = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair expPair = (Pair) exp;
				Expression a = expPair.getCar();
				if(a.isPort()){
					Reader input = ((Port) a).getInput();
					Writer output = ((Port) a).getOutput();

					if(input != null)
						input.close();
					if(output != null)
						output.close();

					return null;
				}else
					throw new Exception("(close-port _) expects a port as an argument.");
			}
		};

		env.add(new SymbolVal("close-port"),closePort);

		JavaFunction setCurrentInputPort = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair expPair = (Pair) exp;
				if(exp.isNil())
					return currentInputPort;

				Expression a = expPair.getCar();
				if(a.isPort()){
					currentInputPort = (Port) a;
					return null;
				}else{
					throw new Exception("(current-input-port _) expects a port as an argument or no arguments.");
				}
			}
		};

		env.add(new SymbolVal("current-input-port"),setCurrentInputPort);

		JavaFunction setCurrentOutputPort = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair expPair = (Pair) exp;
				if(exp.isNil())
					return currentOutputPort;

				Expression a = expPair.getCar();
				if(a.isPort()){
					currentOutputPort = (Port) a;
					return null;
				}else if(a.isNil()){
					return currentOutputPort;
				}else{
					throw new Exception("(current-output-port _) expects a port as an argument or no arguments.");
				}
			}
		};

		env.add(new SymbolVal("current-output-port"), setCurrentOutputPort);

		JavaFunction setCurrentErrorPort = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair expPair = (Pair) exp;
				if(exp.isNil())
					return currentErrorPort;

				Expression a = expPair.getCar();
				if(a.isPort()){
					currentErrorPort = (Port) a;
					return null;
				}else if(a.isNil()){
					return currentErrorPort;
				}else{
					throw new Exception("(current-error-port _) expects a port as an argument or no arguments.");
				}
			}
		};

		env.add(new SymbolVal("current-error-port"), setCurrentErrorPort);



		JavaFunction read = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair expPair = (Pair) exp;
				Expression a = expPair.getCar();

				if(a.isPort()){
					Reader input = ((Port) a).getInput();
					
					if(input != null){

						Expression parsedExp = Parser.parseSexp(input);
						if(parsedExp != null)
							return parsedExp;
						else
							return new EofVal();
					}
				}

				throw new Exception("(read _) expects an open input port as an argument.");
			}
		};

		env.add(new SymbolVal("read"),read);

		JavaFunction write = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair expPair = (Pair) exp;
				Expression a = expPair.getCar();
				Expression b = ((Pair) expPair.getCdr()).getCar();
				while(true){
					if(b.isPort()){
						Writer output = ((Port) b).getOutput();
						
						//Java really should have gotos.
						//This is ridiculous.
						if(output == null)
							break;
						
						Printer.printExpression(a,output);
						output.flush();
						return null;
					}
					break;
				}
				throw new Exception("(write _ _) expects an expression and an open output port as arguments.");
			}
		};

		env.add(new SymbolVal("write"),write);

		JavaFunction newline = new JavaFunction(){
			Expression call(Expression exp) throws Exception{
				Pair expPair = (Pair) exp;
				Expression a = expPair.getCar();
				if(a.isPort()){
					Writer output = ((Port) a).getOutput();
					if(output == null)
						throw new Exception("(newline) expects an output port as an argument.");
					output.write("\n");
				}

				return null;

			}
		};

		env.add(new SymbolVal("newline"),newline);
		
		return env;


	}
}

class CoreLispLib{
	CoreLispLib(){}

	public static Environment getEnv(){
		Environment env = new Environment();

		env.concat(Core.getEnv());
		env.concat(MathLib.getEnv());
		env.concat(IOLib.getEnv());
		
		return env;
	}
}

public class Vole{ 

	BufferedReader reader;
	BufferedWriter writer;
	BufferedWriter error;

	Environment env;

	public Vole(Reader input, Writer output, Writer error){

		this.reader = new BufferedReader(input);
		this.writer = new BufferedWriter(output);
		this.error = new BufferedWriter(error);
		this.env = new Environment();
		this.env.concat(CoreLispLib.getEnv());
		
	}

	public void repl(){

		try{
			Evaluator.eval(new Pair(new SymbolVal("current-input-port"),new Pair(new Port( reader, null),new Pair(null,null))), env);
			Evaluator.eval(new Pair(new SymbolVal("current-output-port"),new Pair(new Port( null, writer),new Pair(null,null))), env);
			Evaluator.eval(new Pair(new SymbolVal("current-error-port"),new Pair(new Port( null, error),new Pair(null,null))), env);
		
			Evaluator.load("repl.scm",env);
			Evaluator.eval(new Pair(new SymbolVal("eval-loop"),new Pair(null,null)),env);
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	public void eval(){
		try{
			Evaluator.eval(new Pair(new SymbolVal("current-input-port"),new Pair(new Port( reader, null),new Pair(null,null))), env);
			Evaluator.eval(new Pair(new SymbolVal("current-output-port"),new Pair(new Port( null, writer),new Pair(null,null))), env);
			Evaluator.eval(new Pair(new SymbolVal("current-error-port"),new Pair(new Port( null, error),new Pair(null,null))), env);
			Expression exp = Parser.parseSexp(reader);
			Evaluator.eval(exp,env);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
