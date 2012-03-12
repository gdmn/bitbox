/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.devsite.bitbox.server;

import org.junit.*;
import static org.junit.Assert.*;
import static pl.devsite.bitbox.server.Operation.Type.*;

/**
 *
 * @author dmn
 */
public class OperationTest {

	public OperationTest() {
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
	 * Test of parse method, of class OperationParser.
	 */
	@Test
	public void testParse() {
		System.out.println("parse");
		Operation instance = new Operation();

		instance.parse("");
		assertEquals("", instance.getArgument());
		assertEquals(UNKNOWN, instance.getType());

		instance.parse("redirect:/abc");
		assertEquals("/abc", instance.getArgument());
		assertEquals(REDIRECT, instance.getType());

		instance.parse("http://abc");
		assertEquals("http://abc", instance.getArgument());
		assertEquals(UNKNOWN, instance.getType());

		instance.parse("abc");
		assertEquals("abc", instance.getArgument());
		assertEquals(UNKNOWN, instance.getType());
	}
}
