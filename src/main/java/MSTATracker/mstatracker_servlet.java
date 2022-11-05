package MSTATracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;

/**
 * Servlet implementation class mstatracker_servlet
 */
@WebServlet("/mstatracker_servlet")
public class mstatracker_servlet extends HttpServlet {
	////////////////////////////////////////////////
	// temp cassandra stuff
	public static Cluster CASSANDRA_CLUSTER;
	private Session CASSANDRA_SESSION;
    private static String CASSANDRA_URL = "127.0.0.1";
	private static Integer CASSANDRA_PORT = 9042;
	private static String CASSANDRA_AUTH = "";
	private static String CASSANDRA_USER = ""; 
	private static String CASSANDRA_PASSWORD = ""; 
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public mstatracker_servlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		CassandraCreate();
	}

	/**
	 * @see Servlet#destroy()
	 */
	public void destroy() {
		// TODO Auto-generated method stub
		CASSANDRA_CLUSTER.close();	// not sure this does anything		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		StringBuffer jb = new StringBuffer();
		String line = null;
		String root = "";
		JSONObject jsonresponse = null;
		try 
		{
			// read the input json into jb
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null)
				jb.append(line);
		} 
		catch (Exception e) 
		{ 
			/*report an error*/ 
			// crash and burn
			throw new IOException(e);
		}
		System.out.println(jb);
		JSONObject jsoninput =  new JSONObject(jb.toString());
	    String type = jsoninput.getString("message_type");	
	    if (type.equals("GetTree")) {
		    root = jsoninput.getString("root_message");	
			System.out.println(root);
			jsonresponse = RetrieveJsonTree(root) ;		  
	    }
		
		response.getWriter().append(jsonresponse.toString());
	}



	private void CassandraCreate() throws ServletException {
		/////////////////////////////////////////
		// temp cassandra stuff
		int tries = 3;
		while (tries > 0)
		{
			try {
				CASSANDRA_CLUSTER = Cluster.builder()
						.addContactPoint(CASSANDRA_URL)
						.withPort(CASSANDRA_PORT)
	//					.withAuthProvider(new SigV4AuthProvider(CASSANDRA_AUTH))
	//	                .withSSL()
	//					.withCredentials(CASSANDRA_USER, CASSANDRA_PASSWORD)
						.build();
	
				CASSANDRA_SESSION = CASSANDRA_CLUSTER.connect();
				CASSANDRA_SESSION.execute("USE mstauth");
				return;
			}
			catch(Exception e) {
				tries --;
				  System.out.println("MST-Auth" + e.toString());
				  if (tries > 0) {
					  try 
					  {
						  TimeUnit.MILLISECONDS.sleep(5000);	// add a little wait, to see if root will end
					  }
					  catch (JSONException | InterruptedException ie) 
					  {
						  throw new ServletException(": MST-Auth mstatracker_servlet Cassandra InterruptedException " + ie.toString());
					  }						  
				  }
			}
		}
	}

	public JSONObject RetrieveJsonTree(String root) throws ServletException
	{
		String stquery = "SELECT JSON * FROM mstauth.service_tree WHERE ";
		stquery += "root_msgid = ";
		stquery += root;
	    //System.out.println(stquery);
		Statement  st = new SimpleStatement(stquery);
	    st.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
		if (CASSANDRA_CLUSTER== null || CASSANDRA_CLUSTER.isClosed()) CassandraCreate();		
	    ResultSet resultSet = CASSANDRA_SESSION.execute(st);
	    List<Row> all = resultSet.all();
	    ArrayDeque<String> records = new ArrayDeque<String>(); 
	    for (int i = 0; i < all.size(); i++)
	    {
	    	String jsonstr = all.get(i).getString("[json]");
	    	records.addFirst(jsonstr);		// cassandra sorts in reverse what we want (last entered is the top) so we just reverse it here
	    }
	    System.out.println("OY1");
		
	    System.out.println(records);
    	JSONObject jsontree = new JSONObject();		
		jsontree.put("records", records);
		
	    //System.out.println(jsontree.toString());
	    return jsontree;
	}
}

