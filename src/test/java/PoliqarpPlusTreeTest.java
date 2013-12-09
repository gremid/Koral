import static org.junit.Assert.*;

import org.junit.Test;

import de.ids_mannheim.korap.query.serialize.PoliqarpPlusTree;

public class PoliqarpPlusTreeTest {
	
	PoliqarpPlusTree ppt;
	String map;

	private boolean equalsContent(String str, Object map) {
		str = str.replaceAll(" ", "");
		String mapStr = map.toString().replaceAll(" ", "");
		return str.equals(mapStr);
	}
	
	private boolean equalsQueryContent(String res, String query) {
		res = res.replaceAll(" ", "");
		ppt = new PoliqarpPlusTree(query);
		String queryMap = ppt.getRequestMap().get("query").toString().replaceAll(" ", "");
		return res.equals(queryMap);
	}
	
	@Test
	public void testContext() {
		String contextString = "{korap=http://korap.ids-mannheim.de/ns/query, @language=de, operands={@id=korap:operands, @container=@list}, relation={@id=korap:relation, @type=korap:relation#types}, class={@id=korap:class, @type=xsd:integer}, query=korap:query, filter=korap:filter, meta=korap:meta}";
		ppt = new PoliqarpPlusTree("[base=test]");
		assertTrue(equalsContent(contextString, ppt.getRequestMap().get("@context")));
	}
	
	@Test
	public void testSingleTokens() {
		// [base=Mann]
		String token1 = "{@type=korap:token, @value={@type=korap:term, @value=base:Mann, relation==}}";
		assertTrue(equalsQueryContent(token1, "[base=Mann]"));
		
		// [orth!=Frau]
		String token2 = "{@type=korap:token, @value={@type=korap:term, @value=orth:Frau, relation=!=}}";
		assertTrue(equalsQueryContent(token2, "[orth!=Frau]"));
		
		// [!p=NN]
		String token3 = "{@type=korap:token, @value={@type=korap:term, @value=p:NN, relation=!=}}";
		assertTrue(equalsQueryContent(token3, "[!p=NN]"));
		
		// [!p!=NN]
		String token4 = "{@type=korap:token, @value={@type=korap:term, @value=p:NN, relation==}}";
		assertTrue(equalsQueryContent(token4, "[!p!=NN]"));
	}
	
	@Test
	public void testElements() {
		// <s>
		String elem1 = "{@type=korap:element, @value=s}";
		assertTrue(equalsQueryContent(elem1, "<s>"));
		
		// <vp>
		String elem2 = "{@type=korap:element, @value=vp}";
		assertTrue(equalsQueryContent(elem2, "<vp>"));
	}

	@Test
	public void testCoordinatedFields() {
		// [base=Mann&(cas=N|cas=A)]
		String cof1 = 
			"{@type=korap:token, @value=" +
				"{@type=korap:group, operands=[" +
					"{@type=korap:term, @value=base:Mann, relation==}," +
					"{@type=korap:group, operands=[" +
						"{@type=korap:term, @value=cas:N, relation==}," +
						"{@type=korap:term, @value=cas:A, relation==}" +
					"], relation=or}" +
				"], relation=and}" +
			"}";
		ppt = new PoliqarpPlusTree("[base=Mann&(cas=N|cas=A)]");
		map = ppt.getRequestMap().get("query").toString();
		assertEquals(cof1.replaceAll(" ", ""), map.replaceAll(" ", ""));
	}
	
	@Test
	public void testOccurrence() {
	}
	
	@Test
	public void testTokenSequence() {
		// [base=Mann][orth=Frau]
		String seq1 = "{@type=korap:sequence, operands=[" +
				"{@type=korap:token, @value={@type=korap:term, @value=base:Mann, relation==}}, " +
				"{@type=korap:token, @value={@type=korap:term, @value=orth:Frau, relation==}}" +
				"]}";
		assertTrue(equalsQueryContent(seq1, "[base=Mann][orth=Frau]"));
		
		// [base=Mann][orth=Frau][p=NN]
		String seq2 = "{@type=korap:sequence, operands=[" +
				"{@type=korap:token, @value={@type=korap:term, @value=base:Mann, relation==}}, " +
				"{@type=korap:token, @value={@type=korap:term, @value=orth:Frau, relation==}}, " +
				"{@type=korap:token, @value={@type=korap:term, @value=p:NN, relation==}}" +
				"]}";
		assertTrue(equalsQueryContent(seq2, "[base=Mann][orth=Frau][p=NN]"));
	}
	
	@Test
	public void testTokenElemSequence() {
		// [base=Mann]<vp>
		String seq1 = "{@type=korap:sequence, operands=[" +
				"{@type=korap:token, @value={@type=korap:term, @value=base:Mann, relation==}}, " +
				"{@type=korap:element, @value=vp}" +
				"]}";
		assertTrue(equalsQueryContent(seq1, "[base=Mann]<vp>"));
		
		// <vp>[base=Mann]
		String seq2 = "{@type=korap:sequence, operands=[" +
				"{@type=korap:element, @value=vp}, "+
				"{@type=korap:token, @value={@type=korap:term, @value=base:Mann, relation==}} " +
				"]}";
		assertTrue(equalsQueryContent(seq2, "<vp>[base=Mann]"));
		
		// <vp>[base=Mann]<pp>
		String seq3 = "{@type=korap:sequence, operands=[" +
				"{@type=korap:element, @value=vp}, "+
				"{@type=korap:token, @value={@type=korap:term, @value=base:Mann, relation==}}, " +
				"{@type=korap:element, @value=pp} "+
				"]}";
		assertTrue(equalsQueryContent(seq3, "<vp>[base=Mann]<pp>"));
	}
	
	@Test
	public void testElemSequence() {
		// <np><vp>
		String seq1 = "{@type=korap:sequence, operands=[" +
				"{@type=korap:element, @value=np}," +
				"{@type=korap:element, @value=vp}" +
				"]}";
		assertTrue(equalsQueryContent(seq1, "<np><vp>"));
		
		// <np><vp><pp>
		String seq2 = "{@type=korap:sequence, operands=[" +
				"{@type=korap:element, @value=np}," +
				"{@type=korap:element, @value=vp}," +
				"{@type=korap:element, @value=pp}" +
				"]}";
		assertTrue(equalsQueryContent(seq2, "<np><vp><pp>"));
	}
	
	@Test 
	public void testClasses() {
		// {[base=Mann]}
		String cls1 = "{@type=korap:group, class=0, operands=[" +
				"{@type=korap:token, @value={@type=korap:term, @value=base:Mann, relation==}}" +
				"]}";
		assertTrue(equalsQueryContent(cls1, "{[base=Mann]}"));
		
		// {[base=Mann][orth=Frau]}
		String cls2 = "{@type=korap:group, class=0, operands=[" +
				 "{@type=korap:sequence, operands=[" +
				  "{@type=korap:token, @value={@type=korap:term, @value=base:Mann, relation==}}," +
				  "{@type=korap:token, @value={@type=korap:term, @value=orth:Frau, relation==}}" +
				 "]}" +
				"]}";
		assertTrue(equalsQueryContent(cls2, "{[base=Mann][orth=Frau]}"));
		
		// [p=NN]{[base=Mann][orth=Frau]}
		String cls3 = "{@type=korap:sequence, operands=[" +
						"{@type=korap:token, @value={@type=korap:term, @value=p:NN, relation==}}," +
						"{@type=korap:group, class=0, operands=[" +
							"{@type=korap:sequence, operands=[" +
								"{@type=korap:token, @value={@type=korap:term, @value=base:Mann, relation==}}," +
								"{@type=korap:token, @value={@type=korap:term, @value=orth:Frau, relation==}}" +
							"]}" +
						"]}" +
					  "]}";
		assertTrue(equalsQueryContent(cls3, "[p=NN]{[base=Mann][orth=Frau]}"));
		
		// {[base=Mann][orth=Frau]}[p=NN]
		String cls4 = "{@type=korap:sequence, operands=[" +
						"{@type=korap:group, class=0, operands=[" +
						   "{@type=korap:sequence, operands=[" +
						     "{@type=korap:token, @value={@type=korap:term, @value=base:Mann, relation==}}," +
						     "{@type=korap:token, @value={@type=korap:term, @value=orth:Frau, relation==}}" +
						   "]}" +
						"]}," +
						"{@type=korap:token, @value={@type=korap:term, @value=p:NN, relation==}}" +
					  "]}";
		assertTrue(equalsQueryContent(cls4, "{[base=Mann][orth=Frau]}[p=NN]"));
	}
	
	@Test
	public void testPositions() {
		// contains(<s>,<np>)
		String pos1 = "{@type=korap:group, relation=position, position=contains, operands=[" +
				  "{@type=korap:element, @value=s}," +
				  "{@type=korap:element, @value=np}" +
				"]}";
		assertTrue(equalsQueryContent(pos1, "contains(<s>,<np>)"));
		
		// contains(<s>,[base=Mann])
		String pos2 = "{@type=korap:group, relation=position, position=contains, operands=[" +
				  "{@type=korap:element, @value=s}," +
				  "{@type=korap:token, @value= {@type=korap:term, @value=base:Mann, relation==}}" +
				"]}";
		assertTrue(equalsQueryContent(pos2, "contains(<s>,[base=Mann])"));
		
		// contains(<s>,[orth=der][orth=Mann])
		String pos3 = "{@type=korap:group, relation=position, position=contains, operands=[" +
				  	"{@type=korap:element, @value=s}," +
				  	"{@type=korap:sequence, operands=[" +
				  		"{@type=korap:token, @value={@type=korap:term, @value=orth:der, relation==}}," +
				  		"{@type=korap:token, @value={@type=korap:term, @value=orth:Mann, relation==}}" +
				  	"]}" +
				  "]}";
		ppt = new PoliqarpPlusTree("contains(<s>,[orth=der][orth=Mann])");
		map = ppt.getRequestMap().get("query").toString();
		assertEquals(pos3.replaceAll(" ", ""), map.replaceAll(" ", ""));
		
		// [base=Auto]contains(<s>,[base=Mann])
		String pos4 = 
				"{@type=korap:sequence, operands=[" +
					"{@type=korap:token, @value={@type=korap:term, @value=base:Auto, relation==}}," +
					"{@type=korap:group, relation=position, position=contains, operands=[" +
				  		"{@type=korap:element, @value=s}," +
				  		"{@type=korap:token, @value={@type=korap:term, @value=base:Mann, relation==}}" +
				  	"]}" +
				"]}";
		ppt = new PoliqarpPlusTree("[base=Auto]contains(<s>,[base=Mann])");
		map = ppt.getRequestMap().get("query").toString();
		assertEquals(pos4.replaceAll(" ", ""), map.replaceAll(" ", ""));
	}
	
	@Test
	public void testNestedPositions() {
		// contains(<s>,startswith(<np>,[orth=Der]))
		String npos1 = 
			"{@type=korap:group, relation=position, position=contains, operands=[" +
				"{@type=korap:element, @value=s}," +
				"{@type=korap:group, relation=position, position=startswith, operands=[" +
					"{@type=korap:element, @value=np}," +
					"{@type=korap:token, @value={@type=korap:term, @value=orth:Der, relation==}}" +
				"]}" +
			"]}";
		ppt = new PoliqarpPlusTree("contains(<s>,startswith(<np>,[orth=Der]))");
		map = ppt.getRequestMap().get("query").toString();
		assertEquals(npos1.replaceAll(" ", ""), map.replaceAll(" ", ""));
	}
	
	@Test
	public void testShrinkSplit() {
		// shrink([orth=Der]{[orth=Mann]})
		String shr1 = 
			"{@type=korap:group, relation=shrink, shrink=0, operands=[" +
				"{@type=korap:sequence, operands=[" +
					"{@type=korap:token, @value={@type=korap:term, @value=orth:Der, relation==}}," +
					"{@type=korap:group, class=0, operands=[" +
						"{@type=korap:token, @value={@type=korap:term, @value=orth:Mann, relation==}}" +
					"]}" +
				"]}" +
			"]}";
		ppt = new PoliqarpPlusTree("shrink([orth=Der]{[orth=Mann]})");
		map = ppt.getRequestMap().get("query").toString();
		assertEquals(shr1.replaceAll(" ", ""), map.replaceAll(" ", ""));
		
		// shrink([orth=Der]{[orth=Mann][orth=geht]})
		String shr2 = 
			"{@type=korap:group, relation=shrink, shrink=0, operands=[" +
				"{@type=korap:sequence, operands=[" +
					"{@type=korap:token, @value={@type=korap:term, @value=orth:Der, relation==}}," +
					"{@type=korap:group, class=0, operands=[" +
						"{@type=korap:sequence, operands=[" +
							"{@type=korap:token, @value={@type=korap:term, @value=orth:Mann, relation==}}," +
							"{@type=korap:token, @value={@type=korap:term, @value=orth:geht, relation==}}" +
						"]}" +
					"]}" +
				"]}" +
			"]}";
		ppt = new PoliqarpPlusTree("shrink([orth=Der]{[orth=Mann][orth=geht]})");
		map = ppt.getRequestMap().get("query").toString();
		assertEquals(shr2.replaceAll(" ", ""), map.replaceAll(" ", ""));
		
		// shrink(1:[orth=Der]{1:[orth=Mann][orth=geht]})
		String shr3 = 
			"{@type=korap:group, relation=shrink, shrink=1, operands=[" +
				"{@type=korap:sequence, operands=[" +
					"{@type=korap:token, @value={@type=korap:term, @value=orth:Der, relation==}}," +
					"{@type=korap:group, class=1, operands=[" +
						"{@type=korap:sequence, operands=[" +
							"{@type=korap:token, @value={@type=korap:term, @value=orth:Mann, relation==}}," +
							"{@type=korap:token, @value={@type=korap:term, @value=orth:geht, relation==}}" +
						"]}" +
					"]}" +
				"]}" +
			"]}";
		ppt = new PoliqarpPlusTree("shrink(1:[orth=Der]{1:[orth=Mann][orth=geht]})");
		map = ppt.getRequestMap().get("query").toString();
		assertEquals(shr3.replaceAll(" ", ""), map.replaceAll(" ", ""));
		
		// shrink(1:startswith(<s>,{1:<np>}))
		String shr4 = 
			"{@type=korap:group, relation=shrink, shrink=1, operands=[" +
				"{@type=korap:group, relation=position, position=startswith, operands=[" +
					"{@type=korap:element, @value=s}," +
					"{@type=korap:group, class=1, operands=[" +
						"{@type=korap:element, @value=np}" +
					"]}" +
				"]}" +
			"]}";
		ppt = new PoliqarpPlusTree("shrink(1:startswith(<s>,{1:<np>}))");
		map = ppt.getRequestMap().get("query").toString();
		assertEquals(shr4.replaceAll(" ", ""), map.replaceAll(" ", ""));
	}
	
	@Test
	public void testLayers() {
		
	}
}
