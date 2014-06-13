package de.ids_mannheim.korap.query.serialize;

import de.ids_mannheim.korap.query.serialize.util.CollectionQueryLexer;
import de.ids_mannheim.korap.query.serialize.util.CollectionQueryParser;
import de.ids_mannheim.korap.util.QueryException;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.reflect.Method;
import java.util.*;

/**
 * @author hanl
 * @date 06/12/2013
 */
public class CollectionQueryTree extends Antlr4AbstractSyntaxTree {

    private Parser parser;
    private boolean verbose = false;
    private List<ParseTree> visited = new ArrayList<ParseTree>();
    /**
     * Top-level map representing the whole request.
     */
    LinkedHashMap<String, Object> requestMap = new LinkedHashMap<String, Object>();
    /**
     * Keeps track of active object.
     */
    LinkedList<LinkedHashMap<String, Object>> objectStack = new LinkedList<LinkedHashMap<String, Object>>();
    /**
     * Keeps track of open node categories
     */
    LinkedList<String> openNodeCats = new LinkedList<String>();
    /**
     * Keeps track of how many objects there are to pop after every recursion of {@link #processNode(ParseTree)}
     */
    LinkedList<Integer> objectsToPop = new LinkedList<Integer>();
    Integer stackedObjects = 0;

    @Override
    public void process(String query) throws QueryException {
        ParseTree tree = parseCollectionQuery(query);
        if (this.parser != null) {
            super.parser = this.parser;
        } else {
            throw new NullPointerException("Parser has not been instantiated!");
        }

        System.out.println("Processing collection query");
        if (verbose) System.out.println(tree.toStringTree(parser));
        processNode(tree);
    }

    private void processNode(ParseTree node) {
        // Top-down processing
        String nodeCat = getNodeCat(node);
        openNodeCats.push(nodeCat);

        stackedObjects = 0;

        if (verbose) {
            System.err.println(" " + objectStack);
            System.out.println(openNodeCats);
        }

		/*
         ****************************************************************
		 **************************************************************** 
		 * 			Processing individual node categories  				*
		 ****************************************************************
		 ****************************************************************
		 */

        if (nodeCat.equals("andGroup")) {
            LinkedHashMap<String, Object> exprGroup = makeTermGroup("and");
            objectStack.push(exprGroup);
            stackedObjects++;
            putIntoSuperObject(exprGroup, 1);
        }

        if (nodeCat.equals("orGroup")) {
            LinkedHashMap<String, Object> exprGroup = makeTermGroup("or");
            objectStack.push(exprGroup);
            stackedObjects++;
            putIntoSuperObject(exprGroup, 1);
        }

        if (nodeCat.equals("expr")) {
            ParseTree fieldNode = getFirstChildWithCat(node, "field");
            String field = fieldNode.getChild(0).toStringTree(parser);
            List<ParseTree> operatorNodes = getChildrenWithCat(node, "operator");
            List<ParseTree> valueNodes = getChildrenWithCat(node, "value");

            if (valueNodes.size() == 1) {
                LinkedHashMap<String, Object> term = makeTerm();
                term.put("attribute", "korap:field#" + field);
                term.put("key", valueNodes.get(0).getChild(0).toStringTree(parser));
                String match = operatorNodes.get(0).getChild(0).toStringTree(parser);
                term.put("match", "match:" + interpretMatch(match));
                putIntoSuperObject(term);
            } else { // (valueNodes.size()==2)
                LinkedHashMap<String, Object> termGroup = makeTermGroup("and");
                ArrayList<Object> termGroupOperands = (ArrayList<Object>) termGroup.get("operands");

                LinkedHashMap<String, Object> term1 = makeTerm();
                term1.put("attribute", "korap:field#" + field);
                term1.put("key", valueNodes.get(0).getChild(0).toStringTree(parser));
                String match1 = operatorNodes.get(0).getChild(0).toStringTree(parser);
                term1.put("match", "match:" + invertInequation(interpretMatch(match1)));
                termGroupOperands.add(term1);

                LinkedHashMap<String, Object> term2 = makeTerm();
                term2.put("attribute", "korap:field#" + field);
                term2.put("key", valueNodes.get(1).getChild(0).toStringTree(parser));
                String match2 = operatorNodes.get(1).getChild(0).toStringTree(parser);
                term2.put("match", "match:" + interpretMatch(match2));
                termGroupOperands.add(term2);

                putIntoSuperObject(termGroup);
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
        for (int i = 0; i < node.getChildCount(); i++) {
            ParseTree child = node.getChild(i);
            processNode(child);
        }

		/*
         **************************************************************
		 * Stuff that happens after processing the children of a node *
		 **************************************************************
		 */
        if (!objectsToPop.isEmpty()) {
            for (int i = 0; i < objectsToPop.pop(); i++) {
                objectStack.pop();
            }
        }
        openNodeCats.pop();


    }


    private String interpretMatch(String match) {
        String out = null;
        switch (match) {
            case "<":
                out = "lt";
                break;
            case ">":
                out = "gt";
                break;
            case "<=":
                out = "leq";
                break;
            case ">=":
                out = "geq";
                break;
            case "=":
                out = "eq";
                break;
            case "!=":
                out = "ne";
                break;
        }
        return out;
    }

    private String invertInequation(String op) {
        String inv = null;
        switch (op) {
            case "lt":
                inv = "gt";
                break;
            case "leq":
                inv = "geq";
                break;
            case "gt":
                inv = "lt";
                break;
            case "geq":
                inv = "leq";
                break;
        }
        return inv;
    }

    private void putIntoSuperObject(LinkedHashMap<String, Object> object) {
        putIntoSuperObject(object, 0);
    }

    @SuppressWarnings({"unchecked"})
    private void putIntoSuperObject(LinkedHashMap<String, Object> object, int objStackPosition) {
        if (objectStack.size() > objStackPosition) {
            ArrayList<Object> topObjectOperands = (ArrayList<Object>) objectStack.get(objStackPosition).get("operands");
            topObjectOperands.add(0, object);
        } else {
            // I want the raw object, not a wrapped
            requestMap.put("query", object);
        }
    }

    private ParserRuleContext parseCollectionQuery(String p) throws QueryException {
        Lexer collectionQueryLexer = new CollectionQueryLexer((CharStream) null);
        ParserRuleContext tree = null;
        // Like p. 111
        try {

            // Tokenize input data
            ANTLRInputStream input = new ANTLRInputStream(p);
            collectionQueryLexer.setInputStream(input);
            CommonTokenStream tokens = new CommonTokenStream(collectionQueryLexer);
            parser = new CollectionQueryParser(tokens);

            // Don't throw out erroneous stuff
            parser.setErrorHandler(new BailErrorStrategy());
            parser.removeErrorListeners();
            // Get starting rule from parser
            Method startRule = CollectionQueryParser.class.getMethod("start");
            tree = (ParserRuleContext) startRule.invoke(parser, (Object[]) null);

        }
        // Some things went wrong ...
        catch (Exception e) {
            System.err.println("Parsing exception message: " + e.getMessage());
        }
        if (tree == null) {
            throw new QueryException("Could not parse query. Make sure it is correct syntax.");
        }
        // Return the generated tree
        return tree;
    }

    public static void main(String[] args) {
        String query = "foo=bar&c=d";
        query = "(1990<year<2010&genre=Sport)|textClass=politk";
        query = "(textClass=wissenschaft & textClass=politik) | textClass=ausland";
        query = "1990<year<2010 & genre=Sport";
        CollectionQueryTree filter = new CollectionQueryTree();
//    	filter.verbose = true;
        try {
            filter.process(query);
        } catch (QueryException e) {
            e.printStackTrace();
        }
        System.out.println(filter.getRequestMap());

    }

    @Override
    public Map<String, Object> getRequestMap() {
        return requestMap;
    }


}