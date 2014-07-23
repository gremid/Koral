import static org.junit.Assert.*;
import de.ids_mannheim.korap.query.serialize.CollectionQueryBuilder;
import de.ids_mannheim.korap.query.serialize.CollectionQueryBuilder2;
import de.ids_mannheim.korap.query.serialize.CollectionQueryTree;
import de.ids_mannheim.korap.resource.Relation;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import org.junit.Test;

public class CollectionQueryTreeTest {

	CollectionQueryTree cqt;
	String map;
	private String query;
	private String expected;

	private boolean equalsQueryContent(String res, String query) throws QueryException {
		res = res.replaceAll(" ", "");
		cqt = new CollectionQueryTree();
		cqt.process(query);
		String queryMap = cqt.getRequestMap().get("query").toString().replaceAll(" ", "");
		return res.equals(queryMap);
	}

	@Test
	public void testSimple() throws QueryException {
		query = "textClass=Sport";
		//      String regex1 = "{@type=korap:filter, filter={@type=korap:doc, attribute=textClass, key=Sport, match=match:eq}}";
		expected = "{@type=korap:filter, filter={@type=korap:doc, key=textClass, value=Sport, match=match:eq}}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));

		query = "textClass!=Sport";
		//	      String regex1 = "{@type=korap:filter, filter={@type=korap:doc, attribute=textClass, key=Sport, match=match:eq}}";
		expected = "{@type=korap:filter, filter={@type=korap:doc, key=textClass, value=Sport, match=match:ne}}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
	}
	
	@Test
	public void testTwoConjuncts() throws QueryException {
		query = "textClass=Sport & year=2014";
		expected = 
			"{@type=korap:filter, filter=" +
				"{@type=korap:docGroup, relation=relation:and, operands=[" +
					"{@type=korap:doc, key=textClass, value=Sport, match=match:eq}," +
					"{@type=korap:doc, key=year, value=2014, match=match:eq}" +
				"]}" +
			"}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
	}
	
	@Test
	public void testThreeConjuncts() throws QueryException {
		query = "textClass=Sport & year=2014 & corpusID=WPD";
		expected = 
			"{@type=korap:filter, filter=" +
				"{@type=korap:docGroup, relation=relation:and, operands=[" +
					"{@type=korap:doc, key=textClass, value=Sport, match=match:eq}," +
					"{@type=korap:docGroup, relation=relation:and, operands=[" +
						"{@type=korap:doc, key=year, value=2014, match=match:eq}," +
						"{@type=korap:doc, key=corpusID, value=WPD, match=match:eq}" +
					"]}" +
				"]}" +
			"}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
	}
	

	@Test
	public void testTwoDisjuncts() throws QueryException {
		query = "textClass=Sport | year=2014";
		expected = 
			"{@type=korap:filter, filter=" +
				"{@type=korap:docGroup, relation=relation:or, operands=[" +
					"{@type=korap:doc, key=textClass, value=Sport, match=match:eq}," +
					"{@type=korap:doc, key=year, value=2014, match=match:eq}" +
				"]}" +
			"}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
	}
	
	@Test
	public void testThreeDisjuncts() throws QueryException {
		query = "textClass=Sport | year=2014 | corpusID=WPD";
		expected = 
			"{@type=korap:filter, filter=" +
				"{@type=korap:docGroup, relation=relation:or, operands=[" +
					"{@type=korap:doc, key=textClass, value=Sport, match=match:eq}," +
					"{@type=korap:docGroup, relation=relation:or, operands=[" +
						"{@type=korap:doc, key=year, value=2014, match=match:eq}," +
						"{@type=korap:doc, key=corpusID, value=WPD, match=match:eq}" +
					"]}" +
				"]}" +
			"}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
	}


	@Test
	public void testMixed() throws QueryException {
		query = "(textClass=Sport | textClass=ausland) & corpusID=WPD";
		expected = 
			"{@type=korap:filter, filter=" +
				"{@type=korap:docGroup, relation=relation:and, operands=[" +
					"{@type=korap:docGroup, relation=relation:or, operands=[" +
						"{@type=korap:doc, key=textClass, value=Sport, match=match:eq}," +
						"{@type=korap:doc, key=textClass, value=ausland, match=match:eq}" +
					"]}," +
					"{@type=korap:doc, key=corpusID, value=WPD, match=match:eq}" +
				"]}" +
			"}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
		
		query = "(textClass=Sport & textClass=ausland) & corpusID=WPD";
		expected = 
			"{@type=korap:filter, filter=" +
				"{@type=korap:docGroup, relation=relation:and, operands=[" +
					"{@type=korap:docGroup, relation=relation:and, operands=[" +
						"{@type=korap:doc, key=textClass, value=Sport, match=match:eq}," +
						"{@type=korap:doc, key=textClass, value=ausland, match=match:eq}" +
					"]}," +
					"{@type=korap:doc, key=corpusID, value=WPD, match=match:eq}" +
				"]}" +
			"}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
		
		query = "(textClass=Sport & textClass=ausland) | (corpusID=WPD & author=White)";
		expected = 
			"{@type=korap:filter, filter=" +
				"{@type=korap:docGroup, relation=relation:or, operands=[" +
					"{@type=korap:docGroup, relation=relation:and, operands=[" +
						"{@type=korap:doc, key=textClass, value=Sport, match=match:eq}," +
						"{@type=korap:doc, key=textClass, value=ausland, match=match:eq}" +
					"]}," +
					"{@type=korap:docGroup, relation=relation:and, operands=[" +
						"{@type=korap:doc, key=corpusID, value=WPD, match=match:eq}," +
						"{@type=korap:doc, key=author, value=White, match=match:eq}" +
					"]}" +
				"]}" +
			"}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
		
		query = "(textClass=Sport & textClass=ausland) | (corpusID=WPD & author=White & year=2010)";
		expected = 
			"{@type=korap:filter, filter=" +
				"{@type=korap:docGroup, relation=relation:or, operands=[" +
					"{@type=korap:docGroup, relation=relation:and, operands=[" +
						"{@type=korap:doc, key=textClass, value=Sport, match=match:eq}," +
						"{@type=korap:doc, key=textClass, value=ausland, match=match:eq}" +
					"]}," +
					"{@type=korap:docGroup, relation=relation:and, operands=[" +
						"{@type=korap:doc, key=corpusID, value=WPD, match=match:eq}," +
						"{@type=korap:docGroup, relation=relation:and, operands=[" +
							"{@type=korap:doc, key=author, value=White, match=match:eq}," +
							"{@type=korap:doc, key=year, value=2010, match=match:eq}" +
						"]}" +
					"]}" +
				"]}" +
			"}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
	}

	@Test
	public void testDate() throws QueryException {
		// search for pubDate between 1990 and 2010!
		query = "1990<pubDate<2010";
		expected = 
			"{@type=korap:filter, filter=" +
				"{@type=korap:docGroup, relation=relation:and, operands=[" +
					"{@type=korap:doc, key=pubDate, value=1990, match=match:gt}," +
					"{@type=korap:doc, key=pubDate, value=2010, match=match:lt}" +
				"]}" +
			"}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
		
		query = "pubDate>=1990";
		expected = 
			"{@type=korap:filter, filter=" +
				"{@type=korap:doc, key=pubDate, value=1990, match=match:geq}" +
			"}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
		
		query = "pubDate>=1990-05";
		expected = 
			"{@type=korap:filter, filter=" +
				"{@type=korap:doc, key=pubDate, value=1990-05, match=match:geq}" +
			"}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
		
		query = "pubDate>=1990-05-01";
		expected = 
			"{@type=korap:filter, filter=" +
				"{@type=korap:doc, key=pubDate, value=1990-05-01, match=match:geq}" +
			"}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
	}

	@Test
	public void testRegex() throws QueryException {
		query = "author=/Go.*he/";
		expected = 
			"{@type=korap:filter, filter=" +
				"{@type=korap:doc, key=author, value=Go.*he, type=type:regex, match=match:eq}" +
			"}";
		cqt = new CollectionQueryTree();
		cqt.process(query);
		map = cqt.getRequestMap().toString();
		assertEquals(expected.replaceAll(" ", ""), map.replaceAll(" ", ""));
	}

}

