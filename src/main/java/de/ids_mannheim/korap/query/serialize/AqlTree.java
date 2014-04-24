package de.ids_mannheim.korap.query.serialize;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import de.ids_mannheim.korap.query.annis.AqlLexer;
import de.ids_mannheim.korap.query.annis.AqlParser;
import de.ids_mannheim.korap.util.QueryException;

/**
 * Map representation of ANNIS QL syntax tree as returned by ANTLR
 * @author joachim
 *
 */
public class AqlTree extends Antlr4AbstractSyntaxTree {
	/**
	 * Top-level map representing the whole request.
	 */
	LinkedHashMap<String,Object> requestMap = new LinkedHashMap<String,Object>();
	/**
	 * Keeps track of open node categories
	 */
	LinkedList<String> openNodeCats = new LinkedList<String>();
	/**
	 * Flag that indicates whether token fields or meta fields are currently being processed
	 */
	boolean inMeta = false;
	/**
	 * Parser object deriving the ANTLR parse tree.
	 */
	Parser parser;
	/**
	 * Keeps track of all visited nodes in a tree
	 */
	List<ParseTree> visited = new ArrayList<ParseTree>();
	/**
	 * Keeps track of active object.
	 */
	LinkedList<LinkedHashMap<String,Object>> objectStack = new LinkedList<LinkedHashMap<String,Object>>();
	/**
	 * Keeps track of explicitly (by #-var definition) or implicitly (number as reference) introduced entities (for later reference by #-operator)
	 */
	Map<String, Object> variableReferences = new LinkedHashMap<String, Object>(); 
	/**
	 * Counter for variable definitions.
	 */
	Integer variableCounter = 1;
	/**
	 * Marks the currently active token in order to know where to add flags (might already have been taken away from token stack).
	 */
	LinkedHashMap<String,Object> curToken = new LinkedHashMap<String,Object>();

	private LinkedList<ArrayList<ArrayList<Object>>> distributedOperandsLists = new LinkedList<ArrayList<ArrayList<Object>>>();
	
	/**
	 * Keeps track of how many objects there are to pop after every recursion of {@link #processNode(ParseTree)}
	 */
	LinkedList<Integer> objectsToPop = new LinkedList<Integer>();
	Integer stackedObjects = 0;
	public static boolean verbose = false;
	
	/**
	 * 
	 * @param tree The syntax tree as returned by ANTLR
	 * @param parser The ANTLR parser instance that generated the parse tree
	 */
	public AqlTree(String query) {
//		prepareContext();
//		parseAnnisQuery(query);
//		super.parser = this.parser;
		requestMap.put("@context", "http://ids-mannheim.de/ns/KorAP/json-ld/v0.1/context.jsonld");
		try {
			process(query);
		} catch (QueryException e) {
			e.printStackTrace();
		}
		System.out.println(">>> "+requestMap.get("query")+" <<<");
	}

	@SuppressWarnings("unused")
	private void prepareContext() {
		LinkedHashMap<String,Object> context = new LinkedHashMap<String,Object>();
		LinkedHashMap<String,Object> operands = new LinkedHashMap<String,Object>();
		LinkedHashMap<String,Object> relation = new LinkedHashMap<String,Object>();
		LinkedHashMap<String,Object> classMap = new LinkedHashMap<String,Object>();
		
		operands.put("@id", "korap:operands");
		operands.put("@container", "@list");
		
		relation.put("@id", "korap:relation");
		relation.put("@type", "korap:relation#types");
		
		classMap.put("@id", "korap:class");
		classMap.put("@type", "xsd:integer");
		
		context.put("korap", "http://korap.ids-mannheim.de/ns/query");
		context.put("@language", "de");
		context.put("operands", operands);
		context.put("relation", relation);
		context.put("class", classMap);
		context.put("query", "korap:query");
		context.put("filter", "korap:filter");
		context.put("meta", "korap:meta");
		
		requestMap.put("@context", context);		
	}

	@Override
	public Map<String, Object> getRequestMap() {
		return requestMap;
	}
	
	@Override
	public void process(String query) throws QueryException {
		ParseTree tree = parseAnnisQuery(query);
		if (this.parser != null) {
			super.parser = this.parser;
		} else {
			throw new NullPointerException("Parser has not been instantiated!"); 
		}
		
		System.out.println("Processing Annis QL");
		if (verbose) System.out.println(tree.toStringTree(parser));
		processNode(tree);
	}
	
	private void processNode(ParseTree node) {
		// Top-down processing
		if (visited.contains(node)) return;
		else visited.add(node);
		
		String nodeCat = getNodeCat(node);
		openNodeCats.push(nodeCat);
		
		stackedObjects = 0;
		
		if (verbose) {
			System.err.println(" "+objectStack);
			System.out.println(openNodeCats);
		}

		/*
		 ****************************************************************
		 **************************************************************** 
		 * 			Processing individual node categories  				*
		 ****************************************************************
		 ****************************************************************
		 */
		if (nodeCat.equals("start")) {
		}
		
		if (nodeCat.equals("exprTop")) {
			// has several andTopExpr as children delimited by OR (Disj normal form)
			if (node.getChildCount() > 1) {
				// TODO or-groups for every and
			}
		}
		
		if (nodeCat.equals("andTopExpr")) {
			if (node.getChildCount() > 1) {
				LinkedHashMap<String, Object> andGroup = makeGroup("and");
				objectStack.push(andGroup);
				stackedObjects++;
				putIntoSuperObject(andGroup,1);
			}
		}
		
		if (nodeCat.equals("expr")) {
			// establish new variables or relations between vars
			
		}
		
		if (nodeCat.equals("variableExpr")) {
			// simplex word or complex assignment (like qname = textSpec)?
			if (node.getChildCount()==1) {						// simplex
				String firstChildNodeCat = getNodeCat(node.getChild(0));
				if (firstChildNodeCat.equals("node")) {
					LinkedHashMap<String, Object> span = makeSpan();
					putIntoSuperObject(span);
					variableReferences.put(variableCounter.toString(), span);
					variableCounter++;
				} else if (firstChildNodeCat.equals("tok")) {
					// TODO
				} else if (firstChildNodeCat.equals("qName")) {	// only (foundry/)?layer specified
					// TODO may also be token!
					LinkedHashMap<String, Object> span = makeSpan();
					span.putAll(parseQNameNode(node.getChild(0)));
					putIntoSuperObject(span);
					variableReferences.put(variableCounter.toString(), span);
					variableCounter++;
				}
			} else if (node.getChildCount() == 3) {  			// (foundry/)?layer=key specification
				LinkedHashMap<String, Object> span = makeSpan();
				// get foundry and layer
				span.putAll(parseQNameNode(node.getChild(0)));
				// get key
				span.putAll(parseVarKey(node.getChild(2)));
				// get relation (match or no match)
				span.put("match", parseMatchOperator(node.getChild(1)));
				putIntoSuperObject(span);
				variableReferences.put(variableCounter.toString(), span);
				variableCounter++;
			}
		}
		
		if (nodeCat.equals("regex")) {
			// mother node can be start or other
			// if start: make token root of tree
			// else: integrate into super object
			if (openNodeCats.get(1).equals("start")) {
				LinkedHashMap<String, Object> token = makeToken();
				LinkedHashMap<String, Object> term = makeTerm();
				token.put("wrap", term);
				term.put("type", "type:regex");
				term.put("key", node.getChild(1).toStringTree(parser));
			}
		}
		
		

		objectsToPop.push(stackedObjects);
		
		/*
		 ****************************************************************
		 **************************************************************** 
		 *  recursion until 'request' node (root of tree) is processed  *
		 ****************************************************************
		 ****************************************************************
		 */
		for (int i=0; i<node.getChildCount(); i++) {
			ParseTree child = node.getChild(i);
			processNode(child);
		}
				
		
		/*
		 **************************************************************
		 * Stuff that happens after processing the children of a node *
		 **************************************************************
		 */
		
		if (!objectsToPop.isEmpty()) {
			for (int i=0; i<objectsToPop.pop(); i++) {
				objectStack.pop();
			}
		}
		
		

		openNodeCats.pop();
		
	}





	/**
	 * Parses the match operator (= or !=)
	 * @param node
	 * @return
	 */
	private String parseMatchOperator(ParseTree node) {
		return node.toStringTree(parser).equals("=") ? "match:eq" : "match:ne";
	}
	
	
	/**
	 * Parses a textSpec node (which holds the 'key' field)
	 * @param node
	 * @return
	 */
	private LinkedHashMap<String, Object> parseVarKey(ParseTree node) {
		LinkedHashMap<String, Object> fields = new LinkedHashMap<String, Object>();
		if (node.getChildCount() == 2) {	// no content, empty quotes
			
		} else if (node.getChildCount() == 3) {
			fields.put("key", node.getChild(1).toStringTree(parser));
			if (node.getChild(0).toStringTree(parser).equals("/") &&		// slashes -> regex
					node.getChild(2).toStringTree(parser).equals("/")) {
				fields.put("type", "type:regex");
			}
		}
		return fields;
	}


	private LinkedHashMap<String, Object> parseQNameNode(ParseTree node) {
		LinkedHashMap<String, Object> fields = new LinkedHashMap<String, Object>();
		if (node.getChildCount() == 1) { 									// only layer specification
			fields.put("layer", node.getChild(0).toStringTree(parser));
		} else if (node.getChildCount() == 3) {								// foundry / layer specification
			fields.put("foundry", node.getChild(0).toStringTree(parser));
			fields.put("layer", node.getChild(2).toStringTree(parser));
		}
		return fields;
	}

	private void putIntoSuperObject(LinkedHashMap<String, Object> object) {
		putIntoSuperObject(object, 0);
	}
	
	@SuppressWarnings({ "unchecked" })
	private void putIntoSuperObject(LinkedHashMap<String, Object> object, int objStackPosition) {
		if (distributedOperandsLists.size()>0) {
			ArrayList<ArrayList<Object>> distributedOperands = distributedOperandsLists.pop();
			for (ArrayList<Object> operands : distributedOperands) {
				operands.add(object);
			}
		} else if (objectStack.size()>objStackPosition) {
			ArrayList<Object> topObjectOperands = (ArrayList<Object>) objectStack.get(objStackPosition).get("operands");
			topObjectOperands.add(0, object);
			
		} else {
			requestMap.put("query", object);
		}
	}
	
	private ParserRuleContext parseAnnisQuery (String p) throws QueryException {
		Lexer poliqarpLexer = new AqlLexer((CharStream)null);
	    ParserRuleContext tree = null;
	    // Like p. 111
	    try {

	      // Tokenize input data
	      ANTLRInputStream input = new ANTLRInputStream(p);
	      poliqarpLexer.setInputStream(input);
	      CommonTokenStream tokens = new CommonTokenStream(poliqarpLexer);
	      parser = new AqlParser(tokens);

	      // Don't throw out erroneous stuff
	      parser.setErrorHandler(new BailErrorStrategy());
	      parser.removeErrorListeners();

	      // Get starting rule from parser
	      Method startRule = AqlParser.class.getMethod("start"); 
	      tree = (ParserRuleContext) startRule.invoke(parser, (Object[])null);
	    }

	    // Some things went wrong ...
	    catch (Exception e) {
	      System.err.println( e.getMessage() );
	    }
	    
	    if (tree == null) {
	    	throw new QueryException("Could not parse query. Make sure it is correct ANNIS QL syntax.");
	    }

	    // Return the generated tree
	    return tree;
	  }
	
	public static void main(String[] args) {
		/*
		 * For testing
		 */
		String[] queries = new String[] {
			
			"#1 . #2 ",
			"#1 . #2 & meta::Genre=\"Sport\"",
			"A _i_ B",
			"A .* B",
			"A >* B",
			"#1 > [label=\"foo\"] #2",
			"pos=\"VVFIN\" & cas=\"Nom\" & #1 . #2",
			"A .* B ",
			"A .* B .* C",
			
			"#1 ->LABEL[lbl=\"foo\"] #2",
			"#1 ->LABEL[lbl=/foo/] #2",
			"#1 ->LABEL[foundry/layer=\"foo\"] #2",
			"#1 ->LABEL[foundry/layer=\"foo\"] #2",
			"node & pos=\"VVFIN\" & #2 > #1",
			"node & pos=\"VVFIN\" & #2 > #1",
			"pos=\"VVFIN\" > cas=\"Nom\" ",
			"pos=\"VVFIN\" >* cas=\"Nom\" ",
			"tiger/pos=\"NN\" >  node",
			"ref#node & pos=\"NN\" > #ref",
			"node & tree/pos=\"NN\"",
			"/node/"
			};
		AqlTree.verbose=true;
		for (String q : queries) {
			try {
				System.out.println(q);
//				System.out.println(AqlTree.parseAnnisQuery(q).toStringTree(AqlTree.parser));
				AqlTree at = new AqlTree(q);
				System.out.println(at.parseAnnisQuery(q).toStringTree(at.parser));
				System.out.println();
				
			} catch (NullPointerException | QueryException npe) {
				npe.printStackTrace();
			}
		}
	}

}