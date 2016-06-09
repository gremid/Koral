package de.ids_mannheim.korap.query.serialize;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author margaretha
 * 
 */
public class FCSQLQueryProcessorTest {

    QuerySerializer qs = new QuerySerializer();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node;

    private void runAndValidate(String query, String jsonLD)
            throws JsonProcessingException {
        FCSQLQueryProcessor processor = new FCSQLQueryProcessor(query, "2.0");
        String serializedQuery = mapper.writeValueAsString(processor
                .getRequestMap().get("query"));
        assertEquals(jsonLD.replace(" ", ""), serializedQuery.replace("\"", ""));
    }

    private void validateNode(String query, String path, String jsonLd)
            throws JsonProcessingException, IOException {
        qs.setQuery(query, "fcsql", "2.0");
        node = mapper.readTree(qs.toJSON());
        String serializedQuery = mapper.writeValueAsString(node.at(path));
        assertEquals(jsonLd.replace(" ", ""), serializedQuery.replace("\"", ""));
    }

    private List<Object> getError(FCSQLQueryProcessor processor) {
        List<Object> errors = (List<Object>) processor.requestMap.get("errors");
        return (List<Object>) errors.get(0);
    }

    @Test
    public void testVersion() throws JsonProcessingException {
        List<Object> error = getError(new FCSQLQueryProcessor("\"Sonne\"",
                "1.0"));
        assertEquals(309, error.get(0));
        assertEquals("SRU diagnostic 5: Only supports SRU version 2.0.",
                error.get(1));

        error = getError(new FCSQLQueryProcessor("\"Sonne\"", null));
        assertEquals(309, error.get(0));
        assertEquals("SRU diagnostic 7: Version number is missing.",
                error.get(1));
    }

    @Test
    public void testTermQuery() throws JsonProcessingException {
        String query = "\"Sonne\"";
        String jsonLd = "{@type:koral:token, wrap:{@type:koral:term, key:Sonne, "
                + "foundry:opennlp, layer:orth, type:type:regex, match:match:eq}}";
        runAndValidate(query, jsonLd);
    }

    @Test
    public void testTermQueryWithRegexFlag() throws JsonProcessingException {
        String query = "\"Fliegen\" /c";
        String jsonLd = "{@type:koral:token, wrap:{@type:koral:term, caseInsensitive:true, "
                + "key:Fliegen, foundry:opennlp, layer:orth, type:type:regex, match:match:eq}}";
        runAndValidate(query, jsonLd);
    }

    @Test
    public void testTermQueryWithSpecificLayer() throws JsonProcessingException {
        String query = "[text = \"Sonne\"]";
        String jsonLd = "{@type:koral:token, wrap:{@type:koral:term, key:Sonne, "
                + "foundry:opennlp, layer:orth, type:type:regex, match:match:eq}}";
        runAndValidate(query, jsonLd);

        query = "[lemma = \"sein\"]";
        jsonLd = "{@type:koral:token, wrap:{@type:koral:term, key:sein, "
                + "foundry:tt, layer:l, type:type:regex, match:match:eq}}";
        runAndValidate(query, jsonLd);

        query = "[pos = \"NN\"]";
        jsonLd = "{@type:koral:token, wrap:{@type:koral:term, key:NN, "
                + "foundry:tt, layer:p, type:type:regex, match:match:eq}}";
        runAndValidate(query, jsonLd);
    }

    @Test
    public void testTermQueryWithQualifier() throws JsonProcessingException {
        String query = "[mate:lemma = \"sein\"]";
        String jsonLd = "{@type:koral:token, wrap:{@type:koral:term, key:sein, "
                + "foundry:mate, layer:l, type:type:regex, match:match:eq}}";
        runAndValidate(query, jsonLd);

        query = "[cnx:pos = \"N\"]";
        jsonLd = "{@type:koral:token, wrap:{@type:koral:term, key:N, "
                + "foundry:cnx, layer:p, type:type:regex, match:match:eq}}";
        runAndValidate(query, jsonLd);
    }

    @Test
    public void testTermQueryException() throws JsonProcessingException {
        String query = "[opennlp:lemma = \"sein\"]";
        List<Object> error = getError(new FCSQLQueryProcessor(query, "2.0"));
        assertEquals(306, error.get(0));
        assertEquals(
                "SRU diagnostic 48: Layer lemma with qualifier opennlp is unsupported.",
                error.get(1));

        query = "[malt:lemma = \"sein\"]";
        error = getError(new FCSQLQueryProcessor(query, "2.0"));
        assertEquals(306, error.get(0));
        assertEquals("SRU diagnostic 48: Qualifier malt is unsupported.",
                error.get(1));

        query = "[cnx:morph = \"heit\"]";
        error = getError(new FCSQLQueryProcessor(query, "2.0"));
        assertEquals(306, error.get(0));
        assertEquals("SRU diagnostic 48: Layer morph is unsupported.",
                error.get(1));

    }

    @Test
    public void testRegex() throws JsonProcessingException {
        String query = "[text=\"M(a|ä)nn(er)?\"]";
        String jsonLd = "{@type:koral:token,wrap:{@type:koral:term,"
                + "key:M(a|ä)nn(er)?,foundry:opennlp,layer:orth,type:type:regex,match:match:eq}}";
        runAndValidate(query, jsonLd);

        query = "\".*?Mann.*?\"";
        jsonLd = "{@type:koral:token,wrap:{@type:koral:term,key:.*?Mann.*?,"
                + "foundry:opennlp,layer:orth,type:type:regex,match:match:eq}}";
        runAndValidate(query, jsonLd);

        query = "\"z.B.\"";
        jsonLd = "{@type:koral:token,wrap:{@type:koral:term,key:z.B.,"
                + "foundry:opennlp,layer:orth,type:type:regex,match:match:eq}}";
        runAndValidate(query, jsonLd);

        query = "\"Sonne&scheint\"";
        jsonLd = "{@type:koral:token,wrap:{@type:koral:term,key:Sonne&scheint,"
                + "foundry:opennlp,layer:orth,type:type:regex,match:match:eq}}";
        runAndValidate(query, jsonLd);

        // Not possible
        // query = "\"a\\.\"";
    }

    @Test
    public void testNot() throws IOException {
        String query = "[cnx:pos != \"N\"]";
        String jsonLd = "{@type:koral:token, wrap:{@type:koral:term, key:N, "
                + "foundry:cnx, layer:p, type:type:regex, match:match:ne}}";
        runAndValidate(query, jsonLd);

        jsonLd = "{@type:koral:token, wrap:{@type:koral:term, key:NN, "
                + "foundry:tt, layer:p, type:type:regex, match:match:eq}}";
        query = "[!pos != \"NN\"]";
        runAndValidate(query, jsonLd);
        query = "[!!pos = \"NN\"]";
        runAndValidate(query, jsonLd);
        query = "[!!!pos != \"NN\"]";
        runAndValidate(query, jsonLd);

        query = "[mate:lemma=\"sein\" & !mate:pos=\"PPOSS\"]";
        jsonLd = "{@type: koral:token,"
                + " wrap: { "
                + "@type: koral:termGroup,"
                + "relation: relation:and,"
                + " operands:["
                + "{@type: koral:term, key: sein, foundry: mate, layer: l, type:type:regex, match: match:eq},"
                + "{@type: koral:term, key: PPOSS, foundry: mate, layer: p, type:type:regex, match: match:ne}]}}";
        runAndValidate(query, jsonLd);
    }

    @Test
    public void testWrongQuery() throws IOException {
        String query = "!(mate:lemma=\"sein\" | mate:pos=\"PPOSS\")";
        List<Object> error = getError(new FCSQLQueryProcessor(query, "2.0"));
        assertEquals(399, error.get(0));
        assertEquals(true,
                error.get(1).toString().startsWith("FCS diagnostic 10"));

        query = "![mate:lemma=\"sein\" | mate:pos=\"PPOSS\"]";
        error = getError(new FCSQLQueryProcessor(query, "2.0"));
        assertEquals(true,
                error.get(1).toString().startsWith("FCS diagnostic 10"));

        query = "(\"blaue\"&\"grüne\")";
        error = getError(new FCSQLQueryProcessor(query, "2.0"));
        assertEquals(true,
                error.get(1).toString().startsWith("FCS diagnostic 10"));
    }

    @Test
    public void testSequenceQuery() throws IOException {
        String query = "\"blaue|grüne\" [pos = \"NN\"]";
        String jsonLd = "{@type:koral:group, "
                + "operation:operation:sequence, "
                + "operands:["
                + "{@type:koral:token, wrap:{@type:koral:term, key:blaue|grüne, foundry:opennlp, layer:orth, type:type:regex, match:match:eq}},"
                + "{@type:koral:token, wrap:{@type:koral:term, key:NN, foundry:tt, layer:p, type:type:regex, match:match:eq}}"
                + "]}";
        runAndValidate(query, jsonLd);

        query = "[text=\"blaue|grüne\"][pos = \"NN\"]";
        runAndValidate(query, jsonLd);

        query = "\"blaue\" \"grüne\" [pos = \"NN\"]";
        jsonLd = "{@type:koral:token, wrap:{@type:koral:term, key:grüne, foundry:opennlp, layer:orth, type:type:regex, match:match:eq}}";
        validateNode(query, "/query/operands/1", jsonLd);

    }

    @Test
    public void testBooleanQuery() throws IOException {
        String query = "[mate:lemma=\"sein\" & mate:pos=\"PPOSS\"]";
        String jsonLd = "{@type: koral:token,"
                + " wrap: { @type: koral:termGroup,"
                + "relation: relation:and,"
                + " operands:["
                + "{@type: koral:term, key: sein, foundry: mate, layer: l, type:type:regex, match: match:eq},"
                + "{@type: koral:term, key: PPOSS, foundry: mate, layer: p, type:type:regex, match: match:eq}]}}";
        runAndValidate(query, jsonLd);

        query = "[mate:lemma=\"sein\" | mate:pos=\"PPOSS\"]";
        validateNode(query, "/query/wrap/relation", "relation:or");

        query = "[cnx:lemma=\"sein\" | mate:lemma=\"sein\" | mate:pos=\"PPOSS\"]";
        jsonLd = "{@type: koral:term, key: sein, foundry: cnx, layer: l, type:type:regex, match: match:eq}";
        validateNode(query, "/query/wrap/operands/0", jsonLd);

        // group with two tokens
        query = "[pos=\"NN\"]|[text=\"Mann\"]";
        jsonLd = "{@type:koral:group,"
                + "operation:operation:disjunction,"
                + "operands:["
                + "{@type:koral:token, wrap:{@type:koral:term,key:NN,foundry:tt,layer:p,type:type:regex,match:match:eq}},"
                + "{@type:koral:token, wrap:{@type:koral:term,key:Mann,foundry:opennlp,layer:orth,type:type:regex,match:match:eq}}]}";
        runAndValidate(query, jsonLd);

        query = "[pos=\"NN\"]&[text=\"Mann\"]";
        List<Object> error = getError(new FCSQLQueryProcessor(query, "2.0"));
        assertEquals(399, error.get(0));
        String msg = (String) error.get(1);
        assertEquals(true, msg.startsWith("FCS diagnostic 10"));
    }

    @Test
    public void testGroupQuery() throws IOException {
        String query = "(\"blaue\"|\"grüne\")";
        String jsonLd = "{@type:koral:group,"
                + "operation:operation:disjunction,"
                + "operands:["
                + "{@type:koral:token, wrap:{@type:koral:term,key:blaue,foundry:opennlp,layer:orth,type:type:regex,match:match:eq}},"
                + "{@type:koral:token, wrap:{@type:koral:term,key:grüne,foundry:opennlp,layer:orth,type:type:regex,match:match:eq}}]}";;
        runAndValidate(query, jsonLd);

        // group and disjunction
        query = "([pos=\"NN\"]|[cnx:pos=\"N\"]|[text=\"Mann\"])";
        jsonLd = "{@type:koral:token,wrap:{@type:koral:term,key:N,foundry:cnx,layer:p,type:type:regex,match:match:eq}}";
        validateNode(query, "/query/operands/1", jsonLd);

        // sequence and disjunction
        query = "([pos=\"NN\"]|[cnx:pos=\"N\"])[text=\"Mann\"]";
        jsonLd = "{@type:koral:group,"
                + "operation:operation:sequence,"
                + "operands:["
                + "{@type:koral:group,"
                + "operation:operation:disjunction,"
                + "operands:[{@type:koral:token,wrap:{@type:koral:term,key:NN,foundry:tt,layer:p,type:type:regex,match:match:eq}},"
                + "{@type:koral:token,wrap:{@type:koral:term,key:N,foundry:cnx,layer:p,type:type:regex,match:match:eq}}"
                + "]},"
                + "{@type:koral:token,wrap:{@type:koral:term,key:Mann,foundry:opennlp,layer:orth,type:type:regex,match:match:eq}}"
                + "]}";
        runAndValidate(query, jsonLd);

        // group and sequence
        query = "([text=\"blaue\"][pos=\"NN\"])";
        jsonLd = "{@type:koral:group,"
                + "operation:operation:sequence,"
                + "operands:["
                + "{@type:koral:token,wrap:{@type:koral:term,key:blaue,foundry:opennlp,layer:orth,type:type:regex,match:match:eq}},"
                + "{@type:koral:token,wrap:{@type:koral:term,key:NN,foundry:tt,layer:p,type:type:regex,match:match:eq}}"
                + "]}";
        runAndValidate(query, jsonLd);
    }
    
    @Test
    public void testWithinQuery() throws JsonProcessingException {
    	String query = "[cnx:pos=\"VVFIN\"] within s";
    	String jsonLd = "{@type:koral:group,"
    			+ "operation:operation:position,"
    			+ "operands:["
    			+ "{@type:koral:span,wrap:{@type:koral:term,key:s,foundry:base,layer:s}},"
    			+ "{@type:koral:token,wrap:{@type:koral:term,key:VVFIN,foundry:cnx,layer:p,type:type:regex,match:match:eq}}"
    			+ "]}";
    	runAndValidate(query, jsonLd);
	}
}
