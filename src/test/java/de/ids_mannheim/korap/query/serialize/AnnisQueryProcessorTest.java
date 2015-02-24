package de.ids_mannheim.korap.query.serialize;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.query.serialize.util.StatusCodes;

/**
 * Tests for JSON-LD serialization of ANNIS QL queries. 
 * @author Joachim Bingel (bingel@ids-mannheim.de)
 * @version 1.0
 */
public class AnnisQueryProcessorTest {

    String query;
    ArrayList<JsonNode> operands;

    QuerySerializer qs = new QuerySerializer();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res;

    @Test
    public void testContext() throws JsonProcessingException, IOException {
        String contextUrl = "http://ids-mannheim.de/ns/KorAP/json-ld/v0.2/context.jsonld";
        query = "foo";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals(contextUrl, res.get("@context").asText());
    }

    @Test
    public void testSingleTokens() throws JsonProcessingException, IOException {
        query = "\"Mann\"";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:token",			 res.at("/query/@type").asText());
        assertEquals("koral:term",			 res.at("/query/wrap/@type").asText());
        assertEquals("orth",				 res.at("/query/wrap/layer").asText());
        assertEquals("Mann",				 res.at("/query/wrap/key").asText());
        assertEquals("match:eq",			 res.at("/query/wrap/match").asText());

        query = "tok!=\"Frau\"";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:token",			 res.at("/query/@type").asText());
        assertEquals("koral:term",			 res.at("/query/wrap/@type").asText());
        assertEquals("orth",				 res.at("/query/wrap/layer").asText());
        assertEquals("Frau",				 res.at("/query/wrap/key").asText());
        assertEquals("match:ne",			 res.at("/query/wrap/match").asText());

        query = "tok";  // special keyword for token
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:token",			 res.at("/query/@type").asText());

        query = "Mann"; // no special keyword -> defaults to layer name
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:span",			 res.at("/query/@type").asText());
        assertEquals("Mann",				 res.at("/query/layer").asText());
    }

    @Test 
    public void testSpans() throws JsonProcessingException, IOException {
        query = "node"; // special keyword for general span
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:span",			 res.at("/query/@type").asText());

        query = "cat=\"np\"";  // cat is special keyword for spans
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:span",			 res.at("/query/@type").asText());
        assertEquals("np",					 res.at("/query/key").asText());
        assertEquals("c",					 res.at("/query/layer").asText());

        query = "cat=\"NP\"";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:span",			 res.at("/query/@type").asText());
        assertEquals("NP",					 res.at("/query/key").asText());
        assertEquals("c",					 res.at("/query/layer").asText());
    }

    @Test
    public void testRegex() throws JsonProcessingException, IOException {
        query = "/Mann/";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:token",			 res.at("/query/@type").asText());
        assertEquals("koral:term",			 res.at("/query/wrap/@type").asText());
        assertEquals("type:regex",			 res.at("/query/wrap/type").asText());
        assertEquals("orth",				 res.at("/query/wrap/layer").asText());
        assertEquals("Mann",				 res.at("/query/wrap/key").asText());
        assertEquals("match:eq",			 res.at("/query/wrap/match").asText());

        query = "/.*?Mann.*?/";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("type:regex",			res.at("/query/wrap/type").asText());
        assertEquals(".*?Mann.*?",			res.at("/query/wrap/key").asText());
    }

    @Test
    public void testFoundriesLayers() throws JsonProcessingException, IOException {
        query = "c=\"np\"";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:span",			 res.at("/query/@type").asText());
        assertEquals("np",					 res.at("/query/key").asText());
        assertEquals("c",					 res.at("/query/layer").asText());

        query = "cnx/c=\"np\"";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:span",			 res.at("/query/@type").asText());
        assertEquals("np",					 res.at("/query/key").asText());
        assertEquals("c",					 res.at("/query/layer").asText());
        assertEquals("cnx",					 res.at("/query/foundry").asText());

        query = "tt/pos=\"np\"";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:token",			 res.at("/query/@type").asText());
        assertEquals("koral:term",			 res.at("/query/wrap/@type").asText());
        assertEquals("np",					 res.at("/query/wrap/key").asText());
        assertEquals("p",					 res.at("/query/wrap/layer").asText());
        assertEquals("tt",					 res.at("/query/wrap/foundry").asText());
    }

    @Test
    public void testDirectDeclarationRelations() throws JsonProcessingException, IOException {
        query = "node > node";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",			res.at("/query/@type").asText());
        assertEquals("operation:relation",	res.at("/query/operation").asText());
        assertEquals("koral:span",			res.at("/query/operands/0/@type").asText());
        assertEquals("koral:span",			res.at("/query/operands/1/@type").asText());
        assertEquals("koral:relation",		res.at("/query/relation/@type").asText());
        assertEquals("koral:term",			res.at("/query/relation/wrap/@type").asText());
        assertEquals("c",					res.at("/query/relation/wrap/layer").asText());

        query = "node > cnx/c=\"np\"";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",			res.at("/query/@type").asText());
        assertEquals("operation:relation",	res.at("/query/operation").asText());
        assertEquals("koral:span",			res.at("/query/operands/0/@type").asText());
        assertEquals("koral:span",			res.at("/query/operands/1/@type").asText());
        assertEquals("np",					res.at("/query/operands/1/key").asText());
        assertEquals("c",					res.at("/query/operands/1/layer").asText());
        assertEquals("cnx",					res.at("/query/operands/1/foundry").asText());
        assertEquals("koral:relation",		res.at("/query/relation/@type").asText());
        assertEquals("koral:term",			res.at("/query/relation/wrap/@type").asText());
        assertEquals("c",					res.at("/query/relation/wrap/layer").asText());

        query = "cnx/c=\"np\" > node";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals(true,					res.at("/query/operands/1/key").isMissingNode());
        assertEquals("np",					res.at("/query/operands/0/key").asText());

        query = "cat=/NP/ & cat=/PP/ > #1";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",			res.at("/query/@type").asText());
        assertEquals("operation:relation",	res.at("/query/operation").asText());
        assertEquals("koral:span",			res.at("/query/operands/0/@type").asText());
        assertEquals("PP",					res.at("/query/operands/0/key").asText());
        assertEquals("koral:span",			res.at("/query/operands/1/@type").asText());
        assertEquals("NP",					res.at("/query/operands/1/key").asText());
        assertEquals(true,					res.at("/query/operands/2").isMissingNode());
        assertEquals("koral:relation",		res.at("/query/relation/@type").asText());
        assertEquals("koral:term",			res.at("/query/relation/wrap/@type").asText());
        assertEquals("c",					res.at("/query/relation/wrap/layer").asText());
    }

    @Test
    public void testDefPredicationInversion() throws JsonProcessingException, IOException {
        query = "#1 > #2 & cnx/cat=\"vp\" & cnx/cat=\"np\"";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",			res.at("/query/@type").asText());
        assertEquals("operation:relation",	res.at("/query/operation").asText());
        assertEquals("koral:span",			res.at("/query/operands/0/@type").asText());
        assertEquals("koral:span",			res.at("/query/operands/0/@type").asText());
        assertEquals("vp",					res.at("/query/operands/0/key").asText());
        assertEquals("c",					res.at("/query/operands/0/layer").asText());
        assertEquals("cnx",					res.at("/query/operands/0/foundry").asText());
        assertEquals("koral:span",			res.at("/query/operands/1/@type").asText());
        assertEquals("np",					res.at("/query/operands/1/key").asText());
        assertEquals("c",					res.at("/query/operands/1/layer").asText());
        assertEquals("cnx",					res.at("/query/operands/1/foundry").asText());
        assertEquals("koral:relation",		res.at("/query/relation/@type").asText());
        assertEquals("koral:term",			res.at("/query/relation/wrap/@type").asText());
        assertEquals("c",					res.at("/query/relation/wrap/layer").asText());
    }

    @Test
    public void testSimpleDominance() throws JsonProcessingException, IOException {
        query = "node & node & #2 > #1";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",			res.at("/query/@type").asText());
        assertEquals("operation:relation",	res.at("/query/operation").asText());
        assertEquals("koral:span",			res.at("/query/operands/0/@type").asText());
        assertEquals("koral:span",			res.at("/query/operands/1/@type").asText());
        assertEquals("koral:relation",		res.at("/query/relation/@type").asText());
        assertEquals("koral:term",			res.at("/query/relation/wrap/@type").asText());
        assertEquals("c",					res.at("/query/relation/wrap/layer").asText());

        query = "\"Mann\" & node & #2 > #1";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",			res.at("/query/@type").asText());
        assertEquals("operation:relation",	res.at("/query/operation").asText());
        assertEquals("koral:span",			res.at("/query/operands/0/@type").asText());
        assertEquals("koral:token",			res.at("/query/operands/1/@type").asText());
        assertEquals("Mann",				res.at("/query/operands/1/wrap/key").asText());
        assertEquals("koral:relation",		res.at("/query/relation/@type").asText());
        assertEquals("koral:term",			res.at("/query/relation/wrap/@type").asText());
        assertEquals("c",					res.at("/query/relation/wrap/layer").asText());

        query = "\"Mann\" & node & #2 >[func=\"SB\"] #1";  //coordinates the func=SB term and requires a "c"-layer term (consituency relation/dominance)
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:relation",		res.at("/query/relation/@type").asText());
        assertEquals("koral:termGroup",		res.at("/query/relation/wrap/@type").asText());
        assertEquals("relation:and",		res.at("/query/relation/wrap/relation").asText());
        assertEquals("c",					res.at("/query/relation/wrap/operands/1/layer").asText());
        assertEquals("func",				res.at("/query/relation/wrap/operands/0/layer").asText());
        assertEquals("SB",					res.at("/query/relation/wrap/operands/0/key").asText());

        query = "cat=\"S\" & node & #1 >[func=\"SB\" func=\"MO\"] #2";  // quite meaningless (function is subject and modifier), but this is allowed by Annis, however its backend only regards the 1st option
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:relation",		res.at("/query/relation/@type").asText());
        assertEquals("koral:termGroup",		res.at("/query/relation/wrap/@type").asText());
        assertEquals("relation:and",		res.at("/query/relation/wrap/relation").asText());
        assertEquals("func",				res.at("/query/relation/wrap/operands/0/layer").asText());
        assertEquals("SB",					res.at("/query/relation/wrap/operands/0/key").asText());
        assertEquals("func",				res.at("/query/relation/wrap/operands/1/layer").asText());
        assertEquals("MO"	,				res.at("/query/relation/wrap/operands/1/key").asText());
        assertEquals("c",					res.at("/query/relation/wrap/operands/2/layer").asText());

        query = "cat=\"S\" & cat=\"NP\" & #1 >@l #2";  // all sentences starting with NP  -> wrap relation in startswith and retrieve 2nd operand with focus
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("operation:position",	res.at("/query/operation").asText());
        assertEquals("operation:relation",	res.at("/query/operands/0/operation").asText());
        assertEquals("frames:startsWith",	res.at("/query/frames/0").asText());
        assertEquals("koral:span",			res.at("/query/operands/0/operands/0/@type").asText());
        assertEquals("S",					res.at("/query/operands/0/operands/0/key").asText());
        assertEquals("koral:group",			res.at("/query/operands/0/operands/1/@type").asText());
        assertEquals("operation:class",		res.at("/query/operands/0/operands/1/operation").asText());
        assertEquals(130,				    res.at("/query/operands/0/operands/1/classOut").asInt());
        assertEquals("koral:span",			res.at("/query/operands/0/operands/1/operands/0/@type").asText());
        assertEquals("NP",					res.at("/query/operands/0/operands/1/operands/0/key").asText());
        assertEquals("koral:reference",		res.at("/query/operands/1/@type").asText());
        assertEquals("operation:focus",		res.at("/query/operands/1/operation").asText());
        assertEquals(130,				    res.at("/query/operands/1/classRef/0").asInt());

        query = "cat=\"S\" & cat=\"NP\" & #1 >@r #2";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("operation:position",	res.at("/query/operation").asText());
        assertEquals("operation:relation",	res.at("/query/operands/0/operation").asText());
        assertEquals("frames:endsWith",		res.at("/query/frames/0").asText());
        assertEquals("koral:span",			res.at("/query/operands/0/operands/0/@type").asText());
        assertEquals("S",					res.at("/query/operands/0/operands/0/key").asText());
        assertEquals("koral:group",			res.at("/query/operands/0/operands/1/@type").asText());
        assertEquals("operation:class",		res.at("/query/operands/0/operands/1/operation").asText());
        assertEquals(130,					res.at("/query/operands/0/operands/1/classOut").asInt());
        assertEquals("koral:span",			res.at("/query/operands/0/operands/1/operands/0/@type").asText());
        assertEquals("NP",					res.at("/query/operands/0/operands/1/operands/0/key").asText());
        assertEquals("koral:reference",		res.at("/query/operands/1/@type").asText());
        assertEquals("operation:focus",		res.at("/query/operands/1/operation").asText());
        assertEquals(130,					res.at("/query/operands/1/classRef/0").asInt());
    }

    @Test
    public void testIndirectDominance() throws JsonProcessingException, IOException {
        query = "node & node & #1 >2,4 #2";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",			res.at("/query/@type").asText());
        assertEquals("operation:relation",	res.at("/query/operation").asText());
        assertEquals("koral:span",			res.at("/query/operands/0/@type").asText());
        assertEquals("koral:span",			res.at("/query/operands/1/@type").asText());
        assertEquals("koral:relation",		res.at("/query/relation/@type").asText());
        assertEquals(2,						res.at("/query/relation/boundary/min").asInt());
        assertEquals(4,						res.at("/query/relation/boundary/max").asInt());
        assertEquals("koral:term",			res.at("/query/relation/wrap/@type").asText());
        assertEquals("c",					res.at("/query/relation/wrap/layer").asText());

        query = "node & node & #1 >* #2";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals(0,						res.at("/query/relation/boundary/min").asInt());
        assertEquals(true,					res.at("/query/relation/boundary/max").isMissingNode());
    }


    @Test
    public void testMultipleDominance() throws JsonProcessingException, IOException {
        query = "cat=\"CP\" & cat=\"VP\" & cat=\"NP\" & #1 > #2 > #3";  
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",			res.at("/query/@type").asText());
        assertEquals("operation:relation",	res.at("/query/operation").asText());
        assertEquals("koral:reference",		res.at("/query/operands/0/@type").asText());
        assertEquals("operation:focus",		res.at("/query/operands/0/operation").asText());
        assertEquals(129,   			    res.at("/query/operands/0/classRef/0").asInt());
        assertEquals("koral:group",			res.at("/query/operands/0/operands/0/@type").asText());
        assertEquals("operation:relation",	res.at("/query/operands/0/operands/0/operation").asText());
        assertEquals("koral:relation",		res.at("/query/operands/0/operands/0/relation/@type").asText());
        assertEquals("c",					res.at("/query/operands/0/operands/0/relation/wrap/layer").asText());
        assertEquals("koral:span",			res.at("/query/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("c",					res.at("/query/operands/0/operands/0/operands/0/layer").asText());
        assertEquals("CP",					res.at("/query/operands/0/operands/0/operands/0/key").asText());
        assertEquals("koral:group",			res.at("/query/operands/0/operands/0/operands/1/@type").asText());
        assertEquals("operation:class",		res.at("/query/operands/0/operands/0/operands/1/operation").asText());
        assertEquals(129,   			    res.at("/query/operands/0/operands/0/operands/1/classOut").asInt());
        assertEquals("VP",					res.at("/query/operands/0/operands/0/operands/1/operands/0/key").asText());
    }
    //		query = "cat=\"CP\" & cat=\"VP\" & cat=\"NP\" & #1 > #2 > #3";
    //		String dom1 = 
    //				"{@type=koral:group, operation=operation:relation, operands=[" +
    //						"{@type=koral:reference, operation=operation:focus, classRef=[0], operands=[" +
    //							"{@type=koral:group, operation=operation:relation, operands=[" +
    //								"{@type=koral:span, layer=cat, key=CP, match=match:eq}," +
    //								"{@type=koral:group, operation=operation:class, class=1, classOut=1, operands=[" +
    //									"{@type=koral:span, layer=cat, key=VP, match=match:eq}" +
    //								"]}" +
    //							"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c}}}" +
    //						"]}," +
    //						"{@type=koral:span, layer=cat, key=NP, match=match:eq}" +
    //				"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c}}" +
    //				"}";
    //		aqlt = new AqlTree(query);
    //		map = aqlt.getRequestMap().get("query").toString();
    //		assertEquals(dom1.replaceAll(" ", ""), map.replaceAll(" ", ""));
    //		
    //		query = "cat=\"CP\" & cat=\"VP\" & cat=\"NP\" & cat=\"DP\" & #1 > #2 > #3 > #4";
    //		String dom2 = 
    //				"{@type=koral:group, operation=operation:relation, operands=[" +
    //						"{@type=koral:reference, operation=operation:focus, classRef=[1], operands=[" +
    //							"{@type=koral:group, operation=operation:relation, operands=[" +
    //								"{@type=koral:reference, operation=operation:focus, classRef=[0], operands=[" +
    //									"{@type=koral:group, operation=operation:relation, operands=[" +
    //										"{@type=koral:span, layer=cat, key=CP, match=match:eq}," +
    //										"{@type=koral:group, operation=operation:class, class=1, classOut=1, operands=[" +
    //											"{@type=koral:span, layer=cat, key=VP, match=match:eq}" +
    //										"]}" +
    //									"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c}}}" +
    //								"]}," +
    //								"{@type=koral:group, operation=operation:class, class=2, classOut=2, operands=[" +
    //									"{@type=koral:span, layer=cat, key=NP, match=match:eq}" +
    //								"]}" +
    //							"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c}}}" +
    //						"]}," +
    //						"{@type=koral:span, layer=cat, key=DP, match=match:eq}" +
    //					"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c}}" +
    //				"}";
    //		aqlt = new AqlTree(query);
    //		map = aqlt.getRequestMap().get("query").toString();
    //		assertEquals(dom2.replaceAll(" ", ""), map.replaceAll(" ", ""));
    //	}
    //	
    //	@Test
    //	public void testPointingRelations() throws Exception {
    //		query = "node & node & #2 ->coref[val=\"true\"] #1";
    //		String dom1 = 
    //				"{@type=koral:group, operation=operation:relation, operands=[" +
    //						"{@type=koral:span}," +
    //						"{@type=koral:span}" +
    //				"], relation={@type=koral:relation, wrap={@type=koral:term, layer=coref, key=true, match=match:eq}}" +
    //				"}";
    //		aqlt = new AqlTree(query);
    //		map = aqlt.getRequestMap().get("query").toString();
    //		assertEquals(dom1.replaceAll(" ", ""), map.replaceAll(" ", ""));
    //		
    //		query = "node & node & #2 ->mate/coref[val=\"true\"] #1";
    //		String dom2 = 
    //				"{@type=koral:group, operation=operation:relation, operands=[" +
    //						"{@type=koral:span}," +
    //						"{@type=koral:span}" +
    //				"], relation={@type=koral:relation, wrap={@type=koral:term, foundry=mate, layer=coref, key=true, match=match:eq}}" +
    //				"}";
    //		aqlt = new AqlTree(query);
    //		map = aqlt.getRequestMap().get("query").toString();
    //		assertEquals(dom2.replaceAll(" ", ""), map.replaceAll(" ", ""));
    //	}

    @Test
    public void testSequence() throws Exception {
        query = "tok=\"der\" & tok=\"die\" & #1 . #2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:sequence",  res.at("/query/operation").asText());
        assertEquals("der",                 res.at("/query/operands/0/wrap/key").asText());
        assertEquals("die",                 res.at("/query/operands/1/wrap/key").asText());
        assertEquals(true,                  res.at("/query/inOrder").asBoolean());
        assertEquals(true,                  res.at("/query/operands/2").isMissingNode());

        query = "tok=\"der\" & tok=\"die\" & #1 .2,3 #2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:sequence",  res.at("/query/operation").asText());
        assertEquals("der",                 res.at("/query/operands/0/wrap/key").asText());
        assertEquals("die",                 res.at("/query/operands/1/wrap/key").asText());
        assertEquals(true,                  res.at("/query/inOrder").asBoolean());
        assertEquals(true,                  res.at("/query/operands/2").isMissingNode());
        assertEquals("koral:distance",      res.at("/query/distances/0/@type").asText());
        assertEquals("koral:boundary",      res.at("/query/distances/0/boundary/@type").asText());
        assertEquals(2,                     res.at("/query/distances/0/boundary/min").asInt());
        assertEquals(3,                     res.at("/query/distances/0/boundary/max").asInt());

        query = "tok=\"der\" & tok=\"die\" & #1 .* #2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:boundary",      res.at("/query/distances/0/boundary/@type").asText());
        assertEquals(0,                     res.at("/query/distances/0/boundary/min").asInt());
        assertEquals(true,                  res.at("/query/distances/0/boundary/max").isMissingNode());
    }

    @Test
    public void testNear() throws Exception {
        query = "tok=\"der\" & tok=\"die\" & #1 ^ #2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:sequence",  res.at("/query/operation").asText());
        assertEquals("der",                 res.at("/query/operands/0/wrap/key").asText());
        assertEquals("die",                 res.at("/query/operands/1/wrap/key").asText());
        assertEquals(false,                 res.at("/query/inOrder").asBoolean());
        assertEquals(true,                  res.at("/query/operands/2").isMissingNode());

        query = "tok=\"der\" & tok=\"die\" & #1 ^2,3 #2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:sequence",  res.at("/query/operation").asText());
        assertEquals("der",                 res.at("/query/operands/0/wrap/key").asText());
        assertEquals("die",                 res.at("/query/operands/1/wrap/key").asText());
        assertEquals(false,                 res.at("/query/inOrder").asBoolean());
        assertEquals(true,                  res.at("/query/operands/2").isMissingNode());
        assertEquals("koral:distance",      res.at("/query/distances/0/@type").asText());
        assertEquals("koral:boundary",      res.at("/query/distances/0/boundary/@type").asText());
        assertEquals(2,                     res.at("/query/distances/0/boundary/min").asInt());
        assertEquals(3,                     res.at("/query/distances/0/boundary/max").asInt());

        query = "tok=\"der\" & tok=\"die\" & #1 ^* #2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:boundary",      res.at("/query/distances/0/boundary/@type").asText());
        assertEquals(0,                     res.at("/query/distances/0/boundary/min").asInt());
        assertEquals(true,                  res.at("/query/distances/0/boundary/max").isMissingNode());
        assertEquals(false,                 res.at("/query/inOrder").asBoolean());
    }


    @Test
    public void testMultipleSequence() throws Exception {
        query = "tok=\"a\" & tok=\"b\" & tok=\"c\" & #1 . #2 & #2 . #3";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:sequence",  res.at("/query/operation").asText());
        assertEquals("koral:reference",     res.at("/query/operands/0/@type").asText());
        assertEquals(129,                   res.at("/query/operands/0/classRef/0").asInt());
        assertEquals(res.at("/query/operands/0/classRef/0").asInt(), 
                res.at("/query/operands/0/operands/0/operands/1/classOut").asInt());
    }


    //		
    //		query = "node & node & node & #1 . #2 .1,3 #3";
    //		String seq5 = 
    //				"{@type=koral:group, operation=operation:sequence, operands=[" +
    //						"{@type=koral:reference, operation=operation:focus, classRef=[0], operands=[" +
    //							"{@type=koral:group, operation=operation:sequence, operands=[" +
    //								"{@type=koral:span}," +
    //								"{@type=koral:group, operation=operation:class, class=1, classOut=1, operands=[" +
    //									"{@type=koral:span}" +
    //								"]} "+
    //							"], inOrder=true}" +
    //						"]}," +
    //						"{@type=koral:span}" +
    //					"], distances=[" +
    //							"{@type=koral:distance, key=w, boundary={@type=koral:boundary, min=1, max=3}, min=1, max=3}" +
    //						"], inOrder=true" +
    //				"}";
    //		aqlt = new AqlTree(query);
    //		map = aqlt.getRequestMap().get("query").toString();
    //		assertEquals(seq5.replaceAll(" ", ""), map.replaceAll(" ", ""));
    //		
    //		query = "tok=\"Sonne\" & tok=\"Mond\" & tok=\"Sterne\" & tok=\"Himmel\" & #1 .0,2 #2 .0,4 #3 . #4";
    //		String seq6 = 
    //				"{@type=koral:group, operation=operation:sequence, operands=[" +
    //					"{@type=koral:reference, operation=operation:focus, classRef=[1], operands=[" +
    //						"{@type=koral:group, operation=operation:sequence, operands=[" +
    //							"{@type=koral:reference, operation=operation:focus, classRef=[0], operands=[" +
    //								"{@type=koral:group, operation=operation:sequence, operands=[" +
    //									"{@type=koral:token, wrap={@type=koral:term, layer=orth, key=Sonne, match=match:eq}}," +
    //									"{@type=koral:group, operation=operation:class, class=1, classOut=1, operands=[" +
    //										"{@type=koral:token, wrap={@type=koral:term, layer=orth, key=Mond, match=match:eq}}" +
    //									"]}" +
    //								"], distances=[" +
    //									"{@type=koral:distance, key=w, boundary={@type=koral:boundary, min=0, max=2}, min=0, max=2}" +
    //								"], inOrder=true}" +
    //							"]}," +	
    //							"{@type=koral:group, operation=operation:class, class=2, classOut=2, operands=[" +
    //								"{@type=koral:token, wrap={@type=koral:term, layer=orth, key=Sterne, match=match:eq}}" +
    //							"]}" +
    //						"],distances=[" +
    //							"{@type=koral:distance, key=w, boundary={@type=koral:boundary, min=0, max=4}, min=0, max=4}" +
    //						"], inOrder=true}" +
    //					"]}," +
    //					"{@type=koral:token, wrap={@type=koral:term, layer=orth, key=Himmel, match=match:eq}}" +
    //				"], inOrder=true}" ;
    //		aqlt = new AqlTree(query);
    //		map = aqlt.getRequestMap().get("query").toString();
    //		assertEquals(seq6.replaceAll(" ", ""), map.replaceAll(" ", ""));
    //	}
    //	
    /**
     * Tests the (rather difficult) serialization of queries where two subsequent relations
     * do not share any common operand. Makes it impossible to wrap 2nd relation around 1st. 
     * Must therefore re-order relations (or postpone processing of 2nd).
     * @throws JsonProcessingException
     * @throws IOException
     */
    @Test
    public void testNoSharedOperand() throws JsonProcessingException, IOException {
        query = "cat=\"A\" & cat=\"B\" & cat=\"C\" & cat=\"D\" & #1 . #2 & #3 . #4 & #1 > #3";  
        // the resulting query should be equivalent to PQ+:  focus(2:dominates(focus(1:{1:<A>}<B>),{2:<C>}))<D> 
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:sequence",  res.at("/query/operation").asText());
        assertEquals("koral:reference",     res.at("/query/operands/0/@type").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operation").asText());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/@type").asText());
        assertEquals("operation:relation",  res.at("/query/operands/0/operands/0/operation").asText());
        assertEquals("koral:reference",     res.at("/query/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operands/0/operands/0/operation").asText());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("operation:sequence",  res.at("/query/operands/0/operands/0/operands/0/operands/0/operation").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/0/operation").asText());
        assertEquals("A",                   res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/key").asText());
        assertEquals("B",                   res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/1/key").asText());
        assertEquals("C",                   res.at("/query/operands/0/operands/0/operands/1/operands/0/key").asText());
        assertEquals("D",                   res.at("/query/operands/1/key").asText());

        query = "cat=\"A\" & cat=\"B\" & cat=\"C\" & cat=\"D\" & cat=\"E\" & cat=\"F\" & #1 . #2 & #3 . #4 & #5 . #6 & #1 > #3 & #3 > #5";  
        // the resulting query should be equivalent to PQ+:   focus(3:dominates(focus(2:dominates(focus(1:{1:<A>}<B>),{2:<C>}))<D>,{3:<E>}))<F> 
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:sequence",  res.at("/query/operation").asText());
        assertEquals("koral:reference",     res.at("/query/operands/0/@type").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operation").asText());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/@type").asText());
        assertEquals("operation:relation",  res.at("/query/operands/0/operands/0/operation").asText());
        assertEquals("koral:reference",     res.at("/query/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operands/0/operands/0/operation").asText());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("operation:sequence",  res.at("/query/operands/0/operands/0/operands/0/operands/0/operation").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/operation").asText());
        assertEquals("A",                   res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/key").asText());
        assertEquals("B",                   res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/operands/1/key").asText());
        assertEquals("C",                   res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/operands/1/operands/0/key").asText());
        assertEquals("D",                   res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/1/key").asText());
        assertEquals("E",                   res.at("/query/operands/0/operands/0/operands/1/operands/0/key").asText());
        assertEquals("F",                   res.at("/query/operands/1/key").asText());

        query = "cat=\"A\" & cat=\"B\" & cat=\"C\" & cat=\"D\" & #1 . #2 & #3 . #4";  
        // the resulting query should be equivalent to PQ+:  focus(2:dominates(focus(1:{1:<A>}<B>),{2:<C>}))<D> 
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals(true,         res.at("/query/@type").isMissingNode());
        assertEquals(StatusCodes.UNBOUND_ANNIS_RELATION,   res.at("/errors/0/0").asInt());
    }

    @Test
    public void testMultipleMixedOperators() throws Exception {
        query = "tok=\"Sonne\" & tok=\"Mond\" & tok=\"Sterne\" & #1 > #2 .0,4 #3";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:sequence",  res.at("/query/operation").asText());
        assertEquals("koral:reference",     res.at("/query/operands/0/@type").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operation").asText());
        assertEquals(129,                   res.at("/query/operands/0/classRef/0").asInt());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/@type").asText());
        assertEquals("operation:relation",  res.at("/query/operands/0/operands/0/operation").asText());
        assertEquals("koral:token",         res.at("/query/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("Sonne",               res.at("/query/operands/0/operands/0/operands/0/wrap/key").asText());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/operands/1/@type").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/1/operation").asText());
        assertEquals(129,                   res.at("/query/operands/0/operands/0/operands/1/classOut").asInt());
        assertEquals("Mond",                res.at("/query/operands/0/operands/0/operands/1/operands/0/wrap/key").asText());
        assertEquals("Sterne",              res.at("/query/operands/1/wrap/key").asText());
        assertEquals("w",                   res.at("/query/distances/0/key").asText());
        assertEquals(0,                     res.at("/query/distances/0/boundary/min").asInt());
        assertEquals(4,                     res.at("/query/distances/0/boundary/max").asInt());

        query = "tok=\"Sonne\" & tok=\"Mond\" & #1 > #2 .0,4  tok=\"Sterne\"";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:sequence",  res.at("/query/operation").asText());
        assertEquals("koral:reference",     res.at("/query/operands/0/@type").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operation").asText());
        assertEquals(129,                   res.at("/query/operands/0/classRef/0").asInt());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/@type").asText());
        assertEquals("operation:relation",  res.at("/query/operands/0/operands/0/operation").asText());
        assertEquals("koral:token",         res.at("/query/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("Sonne",               res.at("/query/operands/0/operands/0/operands/0/wrap/key").asText());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/operands/1/@type").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/1/operation").asText());
        assertEquals(129,                   res.at("/query/operands/0/operands/0/operands/1/classOut").asInt());
        assertEquals("Mond",                res.at("/query/operands/0/operands/0/operands/1/operands/0/wrap/key").asText());
        assertEquals("Sterne",              res.at("/query/operands/1/wrap/key").asText());
        assertEquals("w",                   res.at("/query/distances/0/key").asText());
        assertEquals(0,                     res.at("/query/distances/0/boundary/min").asInt());
        assertEquals(4,                     res.at("/query/distances/0/boundary/max").asInt());

        query = "cat=\"NP\" & cat=\"VP\" & cat=\"PP\" & #1 $ #2 > #3";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:relation",  res.at("/query/operation").asText());
        assertEquals("koral:reference",     res.at("/query/operands/0/@type").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operation").asText());
        assertEquals(130,                   res.at("/query/operands/0/classRef/0").asInt());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/@type").asText());
        assertEquals("operation:relation",  res.at("/query/operands/0/operands/0/operation").asText());
        assertEquals("koral:reference",     res.at("/query/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operands/0/operands/0/operation").asText());
        assertEquals(129,                   res.at("/query/operands/0/operands/0/operands/0/classRef/0").asInt());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("operation:relation",  res.at("/query/operands/0/operands/0/operands/0/operands/0/operation").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/0/operation").asText());
        assertEquals(129,                   res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/0/classOut").asInt());
        assertEquals("koral:span",          res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("NP",                  res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/1/key").asText());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/operands/1/@type").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/1/operation").asText());
        assertEquals(130,                   res.at("/query/operands/0/operands/0/operands/1/classOut").asInt());
        assertEquals("VP",                  res.at("/query/operands/0/operands/0/operands/1/operands/0/key").asText());
        assertEquals("PP",                  res.at("/query/operands/1/key").asText());

    }
    @Test
    public void testMultipleOperatorsWithSameOperands() throws Exception {
        query = "cat=\"NP\" > cat=\"VP\" & #1 _l_ #2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:position",  res.at("/query/operation").asText());
        assertEquals("frames:startsWith",   res.at("/query/frames/0").asText());
        assertEquals("koral:reference",     res.at("/query/operands/0/@type").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operation").asText());
        assertEquals(129,                   res.at("/query/operands/0/classRef/0").asInt());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/@type").asText());
        assertEquals("operation:relation",  res.at("/query/operands/0/operands/0/operation").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/0/operation").asText());
        assertEquals(129,                   res.at("/query/operands/0/operands/0/operands/0/classOut").asInt());
        assertEquals("koral:span",          res.at("/query/operands/0/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("NP",                  res.at("/query/operands/0/operands/0/operands/0/operands/0/key").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/1/operation").asText());
        assertEquals(130,                   res.at("/query/operands/0/operands/0/operands/1/classOut").asInt());
        assertEquals("VP",                  res.at("/query/operands/0/operands/0/operands/1/operands/0/key").asText());
        assertEquals("koral:reference",     res.at("/query/operands/1/@type").asText());
        assertEquals("operation:focus",     res.at("/query/operands/1/operation").asText());
        assertEquals(130,                   res.at("/query/operands/1/classRef/0").asInt());

    }
    @Test
    public void testPositions() throws Exception {
        query = "node & node & #1 _=_ #2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:position",  res.at("/query/operation").asText());
        assertEquals("frames:matches",      res.at("/query/frames/0").asText());
        assertEquals("koral:span",          res.at("/query/operands/0/@type").asText());
        assertEquals("koral:span",          res.at("/query/operands/1/@type").asText());

        query = "node & node & #1 _i_ #2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("frames:isAround",     res.at("/query/frames/0").asText());
        assertEquals("koral:span",          res.at("/query/operands/0/@type").asText());
        assertEquals("koral:span",          res.at("/query/operands/1/@type").asText());

        query = "node & node & #1 _l_ #2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("frames:startsWith",   res.at("/query/frames/0").asText());
        assertEquals("frames:matches",      res.at("/query/frames/1").asText());
        assertEquals("koral:span",          res.at("/query/operands/0/@type").asText());
        assertEquals("koral:span",          res.at("/query/operands/1/@type").asText());

        query = "node & node & #1 _r_ #2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("frames:endsWith",     res.at("/query/frames/0").asText());
        assertEquals("frames:matches",      res.at("/query/frames/1").asText());
        assertEquals("koral:span",          res.at("/query/operands/0/@type").asText());
        assertEquals("koral:span",          res.at("/query/operands/1/@type").asText());

        query = "node & \"Mann\" & #1 _r_ #2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("frames:endsWith",     res.at("/query/frames/0").asText());
        assertEquals("koral:span",          res.at("/query/operands/0/@type").asText());
        assertEquals("koral:token",         res.at("/query/operands/1/@type").asText());
        assertEquals("Mann",                res.at("/query/operands/1/wrap/key").asText());

        query = "node & \"Mann\" & #2 _r_ #1";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("frames:endsWith",     res.at("/query/frames/0").asText());
        assertEquals("koral:span",          res.at("/query/operands/1/@type").asText());
        assertEquals("koral:token",         res.at("/query/operands/0/@type").asText());
        assertEquals("Mann",                res.at("/query/operands/0/wrap/key").asText());

        query = "node & cat=\"VP\" & cat=\"NP\" & #1 _r_ #2 & #2 _l_ #3";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("frames:startsWith",   res.at("/query/frames/0").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operation").asText());
        assertEquals(129,                   res.at("/query/operands/0/classRef/0").asInt());
        assertEquals("frames:endsWith",     res.at("/query/operands/0/operands/0/frames/0").asText());
        assertEquals("koral:span",          res.at("/query/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/operands/1/@type").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/1/operation").asText());
        assertEquals(129,                   res.at("/query/operands/0/operands/0/operands/1/classOut").asInt());
        assertEquals("VP",                  res.at("/query/operands/0/operands/0/operands/1/operands/0/key").asText());
        assertEquals("NP",                  res.at("/query/operands/1/key").asText());

        query = "node & \"Mann\" & #2 _o_ #1";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("frames:overlapsLeft",     res.at("/query/frames/0").asText());
        assertEquals("frames:overlapsRight",    res.at("/query/frames/1").asText());
        assertEquals("koral:span",              res.at("/query/operands/1/@type").asText());
        assertEquals("koral:token",             res.at("/query/operands/0/@type").asText());
        assertEquals("Mann",                    res.at("/query/operands/0/wrap/key").asText());

        query = "node & \"Mann\" & #2 _ol_ #1";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("frames:overlapsLeft",     res.at("/query/frames/0").asText());
        assertEquals("koral:span",              res.at("/query/operands/1/@type").asText());
        assertEquals("koral:token",             res.at("/query/operands/0/@type").asText());
        assertEquals("Mann",                    res.at("/query/operands/0/wrap/key").asText());

        query = "node & \"Mann\" & #2 _or_ #1";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("frames:overlapsRight",    res.at("/query/frames/0").asText());
        assertEquals("koral:span",              res.at("/query/operands/1/@type").asText());
        assertEquals("koral:token",             res.at("/query/operands/0/@type").asText());
        assertEquals("Mann",                    res.at("/query/operands/0/wrap/key").asText());
    }

    @Test
    public void testMultiplePredications() throws Exception {
        // a noun before a verb before a preposition
        query = "pos=\"N\" & pos=\"V\" & pos=\"P\" & #1 . #2 & #2 . #3"; 
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("operation:sequence",  res.at("/query/operation").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operation").asText());
        assertEquals(129,                   res.at("/query/operands/0/classRef/0").asInt());
        assertEquals("operation:sequence",  res.at("/query/operands/0/operands/0/operation").asText());
        assertEquals("koral:token",         res.at("/query/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("p",                   res.at("/query/operands/0/operands/0/operands/0/wrap/layer").asText());
        assertEquals("N",                   res.at("/query/operands/0/operands/0/operands/0/wrap/key").asText());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/operands/1/@type").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/1/operation").asText());
        assertEquals(129,                   res.at("/query/operands/0/operands/0/operands/1/classOut").asInt());
        assertEquals("V",                   res.at("/query/operands/0/operands/0/operands/1/operands/0/wrap/key").asText());
        assertEquals("P",                   res.at("/query/operands/1/wrap/key").asText());

        query = "pos=\"N\" & pos=\"V\" & #1 . #2 & #2 . pos=\"P\""; 
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("operation:sequence",  res.at("/query/operation").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operation").asText());
        assertEquals(129,                     res.at("/query/operands/0/classRef/0").asInt());
        assertEquals("operation:sequence",  res.at("/query/operands/0/operands/0/operation").asText());
        assertEquals("koral:token",         res.at("/query/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("p",                   res.at("/query/operands/0/operands/0/operands/0/wrap/layer").asText());
        assertEquals("N",                   res.at("/query/operands/0/operands/0/operands/0/wrap/key").asText());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/operands/1/@type").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/1/operation").asText());
        assertEquals(129,                     res.at("/query/operands/0/operands/0/operands/1/classOut").asInt());
        assertEquals("V",                   res.at("/query/operands/0/operands/0/operands/1/operands/0/wrap/key").asText());
        assertEquals("P",                   res.at("/query/operands/1/wrap/key").asText());

        query = "pos=\"N\" & pos=\"V\" & pos=\"P\" & #1 > #2 & #1 > #3";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("operation:relation",  res.at("/query/operation").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operation").asText());
        assertEquals(129,                     res.at("/query/operands/0/classRef/0").asInt());
        assertEquals("operation:relation",  res.at("/query/operands/0/operands/0/operation").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/0/operation").asText());
        assertEquals(129,                     res.at("/query/operands/0/operands/0/operands/0/classOut").asInt());
        assertEquals("N",                   res.at("/query/operands/0/operands/0/operands/0/operands/0/wrap/key").asText());
        assertEquals("V",                   res.at("/query/operands/0/operands/0/operands/1/wrap/key").asText());
        assertEquals("P",                   res.at("/query/operands/1/wrap/key").asText());

        query = "cat=\"NP\" & pos=\"V\" & pos=\"P\" & #1 > #2 & #1 > #3 & #2 . #3";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("operation:sequence",  res.at("/query/operation").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operation").asText());
        assertEquals(130,                     res.at("/query/operands/0/classRef/0").asInt());
        assertEquals("operation:relation",  res.at("/query/operands/0/operands/0/operation").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operands/0/operands/0/operation").asText());
        assertEquals(129,                     res.at("/query/operands/0/operands/0/operands/0/classRef/0").asInt());
        assertEquals("operation:relation",  res.at("/query/operands/0/operands/0/operands/0/operands/0/operation").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/0/operation").asText());
        assertEquals(129,                     res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/0/classOut").asInt());
        assertEquals("NP",                  res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/0/operands/0/key").asText());
        assertEquals(130,                     res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/1/classOut").asInt());
        assertEquals("V",                   res.at("/query/operands/0/operands/0/operands/0/operands/0/operands/1/operands/0/wrap/key").asText());
        assertEquals(131,                     res.at("/query/operands/0/operands/0/operands/1/classOut").asInt());
        assertEquals("P",                   res.at("/query/operands/0/operands/0/operands/1/operands/0/wrap/key").asText());
        assertEquals("operation:focus",     res.at("/query/operands/1/operation").asText());
        assertEquals(131,                     res.at("/query/operands/1/classRef/0").asInt());
        assertEquals(true,                  res.at("/query/operands/1/operands").isMissingNode());
    }

    @Test
    public void testUnaryRelations() throws JsonProcessingException, IOException {
        query = "node & #1:tokenarity=2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:span",      res.at("/query/@type").asText());
        assertEquals("koral:term",      res.at("/query/attr/@type").asText());
        assertEquals("koral:boundary",  res.at("/query/attr/tokenarity/@type").asText());
        assertEquals(2,                 res.at("/query/attr/tokenarity/min").asInt());
        assertEquals(2,                 res.at("/query/attr/tokenarity/max").asInt());

        query = "cnx/cat=\"NP\" & #1:tokenarity=2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:span",      res.at("/query/@type").asText());
        assertEquals("cnx",             res.at("/query/foundry").asText());
        assertEquals("c",               res.at("/query/layer").asText());
        assertEquals("NP",              res.at("/query/key").asText());
        assertEquals("koral:term",      res.at("/query/attr/@type").asText());
        assertEquals("koral:boundary",  res.at("/query/attr/tokenarity/@type").asText());
        assertEquals(2,                 res.at("/query/attr/tokenarity/min").asInt());
        assertEquals(2,                 res.at("/query/attr/tokenarity/max").asInt());

        query = "cnx/cat=\"NP\" & #1:tokenarity=2,5";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals(2,                 res.at("/query/attr/tokenarity/min").asInt());
        assertEquals(5,                 res.at("/query/attr/tokenarity/max").asInt());

        query = "cnx/cat=\"NP\" & #1:root";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:span",      res.at("/query/@type").asText());
        assertEquals("cnx",             res.at("/query/foundry").asText());
        assertEquals("c",               res.at("/query/layer").asText());
        assertEquals("NP",              res.at("/query/key").asText());
        assertEquals("match:eq",        res.at("/query/match").asText());
        assertEquals("koral:term",      res.at("/query/attr/@type").asText());
        assertEquals(true,              res.at("/query/attr/root").asBoolean());
        
        query = "cnx/cat=\"NP\" & #1:root & #1:arity=2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:span",      res.at("/query/@type").asText());
        assertEquals("cnx",             res.at("/query/foundry").asText());
        assertEquals("c",               res.at("/query/layer").asText());
        assertEquals("NP",              res.at("/query/key").asText());
        assertEquals("koral:termGroup", res.at("/query/attr/@type").asText());
        assertEquals("koral:term",      res.at("/query/attr/operands/0/@type").asText());
        assertEquals(true,              res.at("/query/attr/operands/0/root").asBoolean());
        assertEquals("koral:term",      res.at("/query/attr/operands/1/@type").asText());
        assertEquals("koral:boundary",  res.at("/query/attr/operands/1/arity/@type").asText());
        assertEquals(2,                 res.at("/query/attr/operands/1/arity/min").asInt());
        assertEquals(2,                 res.at("/query/attr/operands/1/arity/max").asInt());
        
        query = "cnx/cat=\"NP\" & node & #1>#2 & #1:tokenarity=2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",     res.at("/query/@type").asText());
        assertEquals("operation:relation",     res.at("/query/operation").asText());
        assertEquals("koral:span",      res.at("/query/operands/0/@type").asText());
        assertEquals("cnx",             res.at("/query/operands/0/foundry").asText());
        assertEquals("c",               res.at("/query/operands/0/layer").asText());
        assertEquals("NP",              res.at("/query/operands/0/key").asText());
        assertEquals("koral:term",      res.at("/query/operands/0/attr/@type").asText());
        assertEquals("koral:boundary",  res.at("/query/operands/0/attr/tokenarity/@type").asText());
        assertEquals(2,                 res.at("/query/operands/0/attr/tokenarity/min").asInt());
        assertEquals(2,                 res.at("/query/operands/0/attr/tokenarity/max").asInt());
        assertEquals("koral:span",      res.at("/query/operands/1/@type").asText());

    }

    @Test
    public void testCommonParent() throws Exception {
        query = "cat=\"NP\" & cat=\"VP\" & #1 $ #2";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:relation",  res.at("/query/operation").asText());
        assertEquals("koral:reference",     res.at("/query/operands/0/@type").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operation").asText());
        assertEquals(129,                     res.at("/query/operands/0/classRef/0").asInt());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/@type").asText());
        assertEquals("operation:relation",  res.at("/query/operands/0/operands/0/operation").asText());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/operands/0/@type").asText());
        assertEquals("operation:class",     res.at("/query/operands/0/operands/0/operands/0/operation").asText());
        assertEquals(129,                     res.at("/query/operands/0/operands/0/operands/0/classOut").asInt());
        assertEquals("koral:span",          res.at("/query/operands/0/operands/0/operands/0/operands/0/@type").asText());
        assertEquals(true,                  res.at("/query/operands/0/operands/0/operands/0/operands/0/key").isMissingNode());
        assertEquals("koral:span",          res.at("/query/operands/0/operands/0/operands/1/@type").asText());
        assertEquals("NP",                  res.at("/query/operands/0/operands/0/operands/1/key").asText());
        assertEquals("c",                   res.at("/query/operands/0/operands/0/operands/1/layer").asText());
        assertEquals("koral:span",          res.at("/query/operands/1/@type").asText());
        assertEquals("VP",                  res.at("/query/operands/1/key").asText());
        assertEquals("c",                   res.at("/query/operands/1/layer").asText());
        
        query = "cat=\"NP\" & cat=\"VP\" & cat=\"PP\" & #1 $ #2 $ #3";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:relation",  res.at("/query/operation").asText());
        assertEquals("koral:reference",     res.at("/query/operands/0/@type").asText());
        assertEquals("operation:focus",     res.at("/query/operands/0/operation").asText());
        assertEquals(129,                     res.at("/query/operands/0/classRef/0").asInt());
        assertEquals("koral:group",         res.at("/query/operands/0/operands/0/@type").asText());
        assertEquals("operation:relation",  res.at("/query/operands/0/operands/0/operation").asText());
    }
    
    @Test
    public void testDisjunction() throws Exception {
        query = "cat=\"NP\" | cat=\"VP\"";
        qs.setQuery(query, "annis");
        res = mapper.readTree(qs.toJSON());
        assertEquals("koral:group",         res.at("/query/@type").asText());
        assertEquals("operation:disjunction",  res.at("/query/operation").asText());
        assertEquals("koral:span",          res.at("/query/operands/0/@type").asText());
        assertEquals("NP",                  res.at("/query/operands/0/key").asText());
        assertEquals("c",                   res.at("/query/operands/0/layer").asText());
        assertEquals("koral:span",          res.at("/query/operands/1/@type").asText());
        assertEquals("VP",                  res.at("/query/operands/1/key").asText());
        assertEquals("c",                   res.at("/query/operands/1/layer").asText());
    }
    
    //		
    //		query = "cat=\"NP\" & cat=\"VP\" & cat=\"PP\" & #1 $ #2 $ #3";
    //		String cp2 =
    //				"{@type=koral:group, operation=operation:relation, operands=[" +
    //					"{@type=koral:reference, operation=operation:focus, classRef=[0], operands=[" +
    //						"{@type=koral:group, operation=operation:relation, operands=[" +
    //							"{@type=koral:reference, operation=operation:focus, classRef=[0], operands=[" +
    //								"{@type=koral:group, operation=operation:relation, operands=[" +
    //									"{@type=koral:group, operation=operation:class, class=1, classOut=1, operands=[" +
    //										"{@type=koral:span}" +
    //									"]}," +
    //									"{@type=koral:span, layer=cat, key=NP, match=match:eq}" +
    //								"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c}}}" +
    //							"]}," +
    //							"{@type=koral:span, layer=cat, key=VP, match=match:eq}" +
    //						"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c}}" +
    //						"}" +
    //					"]}," +
    //					"{@type=koral:span, layer=cat, key=PP, match=match:eq}" +
    //				"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c}}" +
    //				"}";
    //		aqlt = new AqlTree(query);
    //		map = aqlt.getRequestMap().get("query").toString();
    //		assertEquals(cp2.replaceAll(" ", ""), map.replaceAll(" ", ""));		
    //		
    //		query = "cat=\"NP\" & cat=\"VP\" & cat=\"PP\" & cat=\"CP\" & #1 $ #2 $ #3 $ #4";
    //		String cp3 =
    //				"{@type=koral:group, operation=operation:relation, operands=[" +
    //					"{@type=koral:reference, operation=operation:focus, classRef=[0], operands=[" +
    //						"{@type=koral:group, operation=operation:relation, operands=[" +
    //							"{@type=koral:reference, operation=operation:focus, classRef=[0], operands=[" +
    //								"{@type=koral:group, operation=operation:relation, operands=[" +
    //									"{@type=koral:reference, operation=operation:focus, classRef=[0], operands=[" +
    //										"{@type=koral:group, operation=operation:relation, operands=[" +
    //											"{@type=koral:group, operation=operation:class, class=1, classOut=1, operands=[" +
    //												"{@type=koral:span}" +
    //											"]}," +
    //											"{@type=koral:span, layer=cat, key=NP, match=match:eq}" +
    //										"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c}}}" +
    //									"]}," +
    //									"{@type=koral:span, layer=cat, key=VP, match=match:eq}" +
    //								"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c}}}" +
    //							"]}," +
    //							"{@type=koral:span, layer=cat, key=PP, match=match:eq}" +
    //						"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c}}}" +
    //					"]}," +
    //					"{@type=koral:span, layer=cat, key=CP, match=match:eq}" +
    //				"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c}}" +
    //				"}" +
    //				"";
    //		aqlt = new AqlTree(query);
    //		map = aqlt.getRequestMap().get("query").toString();
    //		assertEquals(cp3.replaceAll(" ", ""), map.replaceAll(" ", ""));		
    //		
    //		query = "cat=\"NP\" & cat=\"VP\" & #1 $* #2";
    //		String cp4 =
    //				"{@type=koral:group, operation=operation:relation, operands=[" +
    //						"{@type=koral:reference, operation=operation:focus, classRef=[0], operands=[" +
    //							"{@type=koral:group, operation=operation:relation, operands=[" +
    //								"{@type=koral:group, operation=operation:class, class=1, classOut=1, operands=[" +
    //									"{@type=koral:span}" +
    //								"]}," +
    //								"{@type=koral:span, layer=cat, key=NP, match=match:eq}" +
    //							"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c},boundary={@type=koral:boundary,min=1}}}" +
    //						"]}," +
    //						"{@type=koral:span, layer=cat, key=VP, match=match:eq}" +
    //					"], relation={@type=koral:relation, wrap={@type=koral:term, layer=c},boundary={@type=koral:boundary,min=1}}}" +
    //					"";
    //		aqlt = new AqlTree(query);
    //		map = aqlt.getRequestMap().get("query").toString();
    //		assertEquals(cp4.replaceAll(" ", ""), map.replaceAll(" ", ""));		
    //	}

    /*		
	@Test
	public void testEqualNotequalValue() throws Exception {
		query = "cat=\"NP\" & cat=\"VP\" & #1 == #2";
		String eq1 =
				"{}"; // ???
		aqlt = new AqlTree(query);
		map = aqlt.getRequestMap().get("query").toString();
		assertEquals(eq1.replaceAll(" ", ""), map.replaceAll(" ", ""));		
	}
     */

}

