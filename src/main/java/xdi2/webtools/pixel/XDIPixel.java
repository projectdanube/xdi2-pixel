package xdi2.webtools.pixel;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.Graph;
import xdi2.core.features.contextfunctions.XdiAbstractEntity.MappingContextNodeXdiEntityIterator;
import xdi2.core.features.linkcontracts.LinkContract;
import xdi2.core.features.linkcontracts.condition.Condition;
import xdi2.core.features.linkcontracts.operator.ConditionOperator;
import xdi2.core.features.linkcontracts.operator.Operator;
import xdi2.core.features.linkcontracts.operator.Operator.MappingRelationOperatorIterator;
import xdi2.core.features.linkcontracts.policy.Policy.MappingXdiEntityPolicyIterator;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.io.XDIWriter;
import xdi2.core.io.XDIWriterRegistry;
import xdi2.core.io.writers.XDIDisplayWriter;
import xdi2.core.util.iterators.CastingIterator;
import xdi2.core.util.iterators.DescendingIterator;
import xdi2.core.util.iterators.IteratorCounter;
import xdi2.core.util.iterators.NotNullIterator;
import xdi2.pixel.PixelParser;
import xdi2.pixel.PixelPolicy;

/**
 * Servlet implementation class for Servlet: XDIPixel
 *
 */
public class XDIPixel extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

	private static final long serialVersionUID = 3749646618800425812L;

	private static Logger log = LoggerFactory.getLogger(XDIPixel.class);

	private static MemoryGraphFactory graphFactory;
	private static List<String> sampleInputs;

	static {

		graphFactory = MemoryGraphFactory.getInstance();
		graphFactory.setSortmode(MemoryGraphFactory.SORTMODE_ORDER);

		sampleInputs = new ArrayList<String> ();

		while (true) {

			InputStream inputStream = XDIPixel.class.getResourceAsStream("graph" + (sampleInputs.size() + 1) + ".xdi");
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			int i;

			try {

				while ((i = inputStream.read()) != -1) outputStream.write(i);
				sampleInputs.add(new String(outputStream.toByteArray()));
			} catch (Exception ex) {

				break;
			} finally {

				try {

					inputStream.close();
					outputStream.close();
				} catch (Exception ex) {

				}
			}
		}
	}

	public XDIPixel() {

		super();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String sample = request.getParameter("sample");
		if (sample == null) sample = "1";

		request.setAttribute("resultFormat", XDIDisplayWriter.FORMAT_NAME);
		request.setAttribute("writeImplied", null);
		request.setAttribute("writeOrdered", "on");
		request.setAttribute("writeInner", "on");
		request.setAttribute("writePretty", null);
		request.setAttribute("sampleInputs", Integer.valueOf(sampleInputs.size()));
		request.setAttribute("input", sampleInputs.get(Integer.parseInt(sample) - 1));
		request.getRequestDispatcher("/XDIPixel.jsp").forward(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String resultFormat = request.getParameter("resultFormat");
		String writeImplied = request.getParameter("writeImplied");
		String writeOrdered = request.getParameter("writeOrdered");
		String writeInner = request.getParameter("writeInner");
		String writePretty = request.getParameter("writePretty");
		String input = request.getParameter("input");
		String output1 = "";
		String output2 = "";
		String stats = "-1";
		String error = null;

		Properties xdiWriterParameters = new Properties();

		if ("on".equals(writeImplied)) xdiWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_IMPLIED, "1");
		if ("on".equals(writeOrdered)) xdiWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_ORDERED, "1");
		if ("on".equals(writeInner)) xdiWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_INNER, "1");
		if ("on".equals(writePretty)) xdiWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_PRETTY, "1");

		XDIWriter xdiResultWriter = XDIWriterRegistry.forFormat(resultFormat, xdiWriterParameters);
		PixelPolicy pixelPolicy = null;
		LinkContract linkContract = null;
		Graph graph = null;

		try {

			StringWriter writer = new StringWriter();

			pixelPolicy = PixelParser.pixel(input);
			linkContract = pixelPolicy.getLinkContract();

			graph = linkContract.getContextNode().getGraph();

			xdiResultWriter.write(graph, writer);

			output1 = StringEscapeUtils.escapeHtml(writer.getBuffer().toString());
			output2 = new JSONObject(pixelPolicy.getHashMap()).toString(2);
		} catch (Exception ex) {

			log.error(ex.getMessage(), ex);
			error = ex.getMessage();
			if (error == null) error = ex.getClass().getName();
		}

		stats = "";

		if (graph != null) {

			stats += Integer.toString(countPolicies(graph)) + " policies. ";
			stats += Integer.toString(countOperators(graph)) + " operators. ";
			stats += Integer.toString(countConditions(graph)) + " comparisons. ";

			graph.close();
		}

		// display results

		request.setAttribute("sampleInputs", Integer.valueOf(sampleInputs.size()));
		request.setAttribute("resultFormat", resultFormat);
		request.setAttribute("writeImplied", writeImplied);
		request.setAttribute("writeOrdered", writeOrdered);
		request.setAttribute("writeInner", writeInner);
		request.setAttribute("writePretty", writePretty);
		request.setAttribute("input", input);
		request.setAttribute("output1", output1);
		request.setAttribute("output2", output2);
		request.setAttribute("stats", stats);
		request.setAttribute("error", error);

		request.getRequestDispatcher("/XDIPixel.jsp").forward(request, response);
	}   	  	    

	private static int countPolicies(Graph graph) {

		Iterator<?> iterator = new MappingXdiEntityPolicyIterator(new MappingContextNodeXdiEntityIterator(graph.getRootContextNode().getAllContextNodes()));
		
		return new IteratorCounter(iterator).count();
	}

	private static int countOperators(Graph graph) {

		Iterator<?> iterator = new MappingRelationOperatorIterator(graph.getRootContextNode().getAllRelations());
		
		return new IteratorCounter(iterator).count();
	}

	private static int countConditions(Graph graph) {
		
		Iterator<?> iterator = new DescendingIterator<ConditionOperator, Condition> (new NotNullIterator<ConditionOperator> (new CastingIterator<Operator, ConditionOperator> (new MappingRelationOperatorIterator(graph.getRootContextNode().getAllRelations()), ConditionOperator.class))) {

			@Override
			public Iterator<Condition> descend(ConditionOperator operator) {
				
				return operator.getConditions();
			}			
		};
		
		return new IteratorCounter(iterator).count();
	}
}
