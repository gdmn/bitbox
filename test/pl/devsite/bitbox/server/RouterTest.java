package pl.devsite.bitbox.server;

import org.junit.*;
import static org.junit.Assert.*;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.sendables.SendableRoot;
import pl.devsite.bitbox.sendables.SendableString;

/**
 *
 * @author dmn
 */
public class RouterTest {
	
	public RouterTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}
	
	@Before
	public void setUp() {
	}
	
	@After
	public void tearDown() {
	}

	/**
	 * Test of execute method, of class Router.
	 */
	@Test
	public void testExecute_0args() throws Exception {
		///System.out.println("execute");
		Router instance = new Router();
		Object expResult = null;
		//Object result = instance.execute();
		//assertEquals(expResult, result);
		// TODO review the generated test code and remove the default call to fail.
		//fail("The test case is a prototype.");
	}

	/**
	 * Test of execute method, of class Router.
	 */
	@Test
	public void testExecute_Last() throws Exception {
		System.out.println("execute");
		Router instance = new Router();
		
		Object expResult = Processor.LAST;		
		assertEquals(expResult, instance.execute(Processor.LAST));
	}

	/**
	 * Test of execute method, of class Router.
	 */
	@Test
	public void testExecute_Sendable() throws Exception {
		System.out.println("execute");
		Router instance = new Router();
		
		SendableRoot root = new SendableRoot(null, "[]");
		Sendable abc = new SendableString(root, "abc", "abcvalue");
		root.addSendable(abc);
		
		RequestContext context = new RequestContext();
		context. setSendableRoot(root);
		instance.initialize(context);
		
		Object command = "/abc";
		Object result = instance.execute(command);
		
		assertEquals(abc, result);
	}

	/**
	 * Test of execute method, of class Router.
	 */
	@Test
	public void testExecute_Redirect() throws Exception {
		System.out.println("execute");
		Router instance = new Router();
		
		SendableRoot root = new SendableRoot(null, "[]");
		Sendable abc = new SendableRoot(root, "abc");
		Sendable def = new SendableString(abc, "def", "defvalue");
		Sendable def2 = new SendableString(abc, "def2", "def2value");
		((SendableRoot)abc).addSendable(def);
		((SendableRoot)abc).addSendable(def2);
		root.addSendable(abc);
		
		RequestContext context = new RequestContext();
		context. setSendableRoot(root);
		instance.initialize(context);
		
		Object command = "/abc/def2";
		Object result = instance.execute(command);
		
		assertEquals(def2, result);
	}

	/**
	 * Test of execute method, of class Router.
	 */
	@Test
	public void testExecute_Error() throws Exception {
		Router instance = new Router();
		
		RequestContext context = new RequestContext();
		instance.initialize(context);
		
		Object command = "error:this is error test";
		Object result = instance.execute(command);
		
		assertTrue(result instanceof Sendable);
		
		assertNotNull(context.getResponseHeader());
		
		assertTrue(context.getResponseHeader().getHttpResponseCode() >= 400);
	}

	/**
	 * Test of execute method, of class Router.
	 */
	@Test
	public void testExecute_NotFound() throws Exception {
		Router instance = new Router();
		
		RequestContext context = new RequestContext();
		instance.initialize(context);
		
		Object command = "/abc";
		Object result = instance.execute(command);
		
		assertTrue(result instanceof String);
		assertTrue(result.toString().contains("404"));
	}

	/**
	 * Test of execute method, of class Router.
	 */
	@Test
	public void testExecute_NotFoundRouted() throws Exception {
		Router instance = new Router();
		
		RequestContext context = new RequestContext();
		instance.initialize(context);
		
		Object command = "error:404";
		Object result = instance.execute(command);
		
		assertTrue(result instanceof Sendable);
		assertEquals(404, context.getResponseHeader().getHttpResponseCode());
		
		command = "error:404:custom message";
		result = instance.execute(command);
		
		assertTrue(result instanceof Sendable);
		assertEquals(404, context.getResponseHeader().getHttpResponseCode());
	}
}

