import norswap.autumn.AutumnTestFixture;
import norswap.autumn.Grammar;
import norswap.autumn.Grammar.rule;
import norswap.autumn.ParseResult;
import norswap.autumn.positions.LineMapString;
import norswap.sigh.SemanticAnalysis;
import norswap.sigh.SighGrammar;
import norswap.sigh.ast.SighNode;
import norswap.sigh.interpreter.Interpreter;
import norswap.sigh.interpreter.Null;
import norswap.uranium.Reactor;
import norswap.uranium.SemanticError;
import norswap.utils.IO;
import norswap.utils.TestFixture;
import norswap.utils.data.wrappers.Pair;
import norswap.utils.visitors.Walker;
import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Set;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertThrows;

public final class InterpreterTests extends TestFixture {

    // TODO peeling

    // ---------------------------------------------------------------------------------------------

    private final SighGrammar grammar = new SighGrammar();
    private final AutumnTestFixture autumnFixture = new AutumnTestFixture();

    {
        autumnFixture.runTwice = false;
        autumnFixture.bottomClass = this.getClass();
    }

    // ---------------------------------------------------------------------------------------------

    private Grammar.rule rule;

    // ---------------------------------------------------------------------------------------------

    private void check (String input, Object expectedReturn) {
        assertNotNull(rule, "You forgot to initialize the rule field.");
        check(rule, input, expectedReturn, null);
    }

    // ---------------------------------------------------------------------------------------------

    private void check (String input, Object expectedReturn, String expectedOutput) {
        assertNotNull(rule, "You forgot to initialize the rule field.");
        check(rule, input, expectedReturn, expectedOutput);
    }

    // ---------------------------------------------------------------------------------------------

    private void check (rule rule, String input, Object expectedReturn, String expectedOutput) {
        // TODO
        // (1) write proper parsing tests
        // (2) write some kind of automated runner, and use it here

        autumnFixture.rule = rule;
        ParseResult parseResult = autumnFixture.success(input);
        SighNode root = parseResult.topValue();

        Reactor reactor = new Reactor();
        Walker<SighNode> walker = SemanticAnalysis.createWalker(reactor);
        Interpreter interpreter = new Interpreter(reactor);
        walker.walk(root);
        reactor.run();
        Set<SemanticError> errors = reactor.errors();

        if (!errors.isEmpty()) {
            LineMapString map = new LineMapString("<test>", input);
            String report = reactor.reportErrors(it ->
                it.toString() + " (" + ((SighNode) it).span.startString(map) + ")");
            //            String tree = AttributeTreeFormatter.format(root, reactor,
            //                    new ReflectiveFieldWalker<>(SighNode.class, PRE_VISIT, POST_VISIT));
            //            System.err.println(tree);
            throw new AssertionError(report);
        }

        Pair<String, Object> result = IO.captureStdout(() -> interpreter.interpret(root));
        assertEquals(result.b, expectedReturn);
        if (expectedOutput != null) assertEquals(result.a, expectedOutput);
    }

    // ---------------------------------------------------------------------------------------------

    private void checkExpr (String input, Object expectedReturn, String expectedOutput) {
        rule = grammar.root;
        check("return " + input, expectedReturn, expectedOutput);
    }

    // ---------------------------------------------------------------------------------------------

    private void checkExpr (String input, Object expectedReturn) {
        rule = grammar.root;
        check("return " + input, expectedReturn);
    }

    // ---------------------------------------------------------------------------------------------

    private void checkThrows (String input, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> check(input, null));
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testLiteralsAndUnary () {
        checkExpr("42", 42L);
        checkExpr("42.0", 42.0d);
        checkExpr("\"hello\"", "hello");
        checkExpr("(42)", 42L);
        checkExpr("[1, 2, 3]", new Object[]{1L, 2L, 3L});
        checkExpr("true", true);
        checkExpr("false", false);
        checkExpr("null", Null.INSTANCE);
        checkExpr("!false", true);
        checkExpr("!true", false);
        checkExpr("!!true", true);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testNumericBinary () {
        checkExpr("1 + 2", 3L);
        checkExpr("2 - 1", 1L);
        checkExpr("2 * 3", 6L);
        checkExpr("2 / 3", 0L);
        checkExpr("3 / 2", 1L);
        checkExpr("2 % 3", 2L);
        checkExpr("3 % 2", 1L);

        checkExpr("1.0 + 2.0", 3.0d);
        checkExpr("2.0 - 1.0", 1.0d);
        checkExpr("2.0 * 3.0", 6.0d);
        checkExpr("2.0 / 3.0", 2d / 3d);
        checkExpr("3.0 / 2.0", 3d / 2d);
        checkExpr("2.0 % 3.0", 2.0d);
        checkExpr("3.0 % 2.0", 1.0d);

        checkExpr("1 + 2.0", 3.0d);
        checkExpr("2 - 1.0", 1.0d);
        checkExpr("2 * 3.0", 6.0d);
        checkExpr("2 / 3.0", 2d / 3d);
        checkExpr("3 / 2.0", 3d / 2d);
        checkExpr("2 % 3.0", 2.0d);
        checkExpr("3 % 2.0", 1.0d);

        checkExpr("1.0 + 2", 3.0d);
        checkExpr("2.0 - 1", 1.0d);
        checkExpr("2.0 * 3", 6.0d);
        checkExpr("2.0 / 3", 2d / 3d);
        checkExpr("3.0 / 2", 3d / 2d);
        checkExpr("2.0 % 3", 2.0d);
        checkExpr("3.0 % 2", 1.0d);

        checkExpr("2 * (4-1) * 4.0 / 6 % (2+1)", 1.0d);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testOtherBinary () {
        checkExpr("true  && true",  true);
        checkExpr("true  || true",  true);
        checkExpr("true  || false", true);
        checkExpr("false || true",  true);
        checkExpr("false && true",  false);
        checkExpr("true  && false", false);
        checkExpr("false && false", false);
        checkExpr("false || false", false);

        checkExpr("1 + \"a\"", "1a");
        checkExpr("\"a\" + 1", "a1");
        checkExpr("\"a\" + true", "atrue");

        checkExpr("1 == 1", true);
        checkExpr("1 == 2", false);
        checkExpr("1.0 == 1.0", true);
        checkExpr("1.0 == 2.0", false);
        checkExpr("true == true", true);
        checkExpr("false == false", true);
        checkExpr("true == false", false);
        checkExpr("1 == 1.0", true);
        checkExpr("[1] == [1]", false);

        checkExpr("1 != 1", false);
        checkExpr("1 != 2", true);
        checkExpr("1.0 != 1.0", false);
        checkExpr("1.0 != 2.0", true);
        checkExpr("true != true", false);
        checkExpr("false != false", false);
        checkExpr("true != false", true);
        checkExpr("1 != 1.0", false);

        checkExpr("\"hi\" != \"hi2\"", true);
        checkExpr("[1] != [1]", true);

         // test short circuit
        checkExpr("true || print(\"x\") == \"y\"", true, "");
        checkExpr("false && print(\"x\") == \"y\"", false, "");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testVarDecl () {
        check("var x: Int = 1; return x", 1L);
        check("var x: Float = 2.0; return x", 2d);

        check("var x: Int = 0; return x = 3", 3L);
        check("var x: String = \"0\"; return x = \"S\"", "S");

        // implicit conversions
        check("var x: Float = 1; x = 2; return x", 2.0d);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testRootAndBlock () {
        rule = grammar.root;
        check("return", null);
        check("return 1", 1L);
        check("return 1; return 2", 1L);

        check("print(\"a\")", null, "a\n");
        check("print(\"a\" + 1)", null, "a1\n");
        check("print(\"a\"); print(\"b\")", null, "a\nb\n");

        check("{ print(\"a\"); print(\"b\") }", null, "a\nb\n");

        check(
            "var x: Int = 1;" +
            "{ print(\"\" + x); var x: Int = 2; print(\"\" + x) }" +
            "print(\"\" + x)",
            null, "1\n2\n1\n");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testCalls () {
        rule = grammar.root;

        //Template tests

        check(
            "template test (x: Int):Int { return x }" +
                "return test{1}",
            1L);
        /*
        check(
            "template test (x: String, y: String):String { return x+y }" +
                "return test[\"1\", \"3\"]",
            "13");


        check(
            "template test (x: String, y: String):String { return x+y }" +
                "return test[\"1\", \"3\"]",
            "13");
         */
        check(
            "fun add (a: Int, b: Int): Int { return a + b } " +
                "return add(4, 7)",
            11L);

        HashMap<String, Object> point = new HashMap<>();
        point.put("x", 1L);
        point.put("y", 2L);

        check(
            "struct Point { var x: Int; var y: Int }" +
                "return $Point(1, 2)",
            point);

        check("var str: String = null; return print(str + 1)", "null1", "null1\n");
    }


    // ---------------------------------------------------------------------------------------------

    @Test
    public void testArrayStructAccess () {
        checkExpr("[1][0]", 1L);
        checkExpr("[1.0][0]", 1d);
        checkExpr("[1, 2][1]", 2L);

        // TODO check that this fails (& maybe improve so that it generates a better message?)
        // or change to make it legal (introduce a top type, and make it a top type array if thre
        // is no inference context available)
        // checkExpr("[].length", 0L);
        checkExpr("[1].length", 1L);
        checkExpr("[1, 2].length", 2L);

        checkThrows("var array: Int[] = null; return array[0]", NullPointerException.class);
        checkThrows("var array: Int[] = null; return array.length", NullPointerException.class);

        check("var x: Int[] = [0, 1]; x[0] = 3; return x[0]", 3L);
        checkThrows("var x: Int[] = []; x[0] = 3; return x[0]",
            ArrayIndexOutOfBoundsException.class);
        checkThrows("var x: Int[] = null; x[0] = 3",
            NullPointerException.class);

        check(
            "struct P { var x: Int; var y: Int }" +
                "return $P(1, 2).y",
            2L);

        checkThrows(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = null;" +
                "return p.y",
            NullPointerException.class);

        check(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = $P(1, 2);" +
                "p.y = 42;" +
                "return p.y",
            42L);

        checkThrows(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = null;" +
                "p.y = 42",
            NullPointerException.class);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testIfWhile () {
        rule = grammar.root;
        check("if (true) return 1 else return 2", 1L);
        check("if (false) ? return 1 : return 2", 2L);
        check("var a: Int = 5; var b: Int = 2; if (3!=2) ? return a+b : return 2", 7L);
        check("if (false) return 1 else return 2", 2L);
        check("if (false) return 1 else if (true) return 2 else return 3 ", 2L);
        check("if (false) return 1 else if (false) return 2 else return 3 ", 3L);

        check("var i: Int = 0; while (i < 3) { print(\"\" + i); i = i + 1 } ", null, "0\n1\n2\n");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testSwitchCase () {
        rule = grammar.root;
        check("switch(1) { case(1): return 1, case(2): return 2}", 1L);
        check("switch(2) { case(1): return 1, case(2): return 2}", 2L);
        check("switch(5) { case(1): return 1, case(2): return 2, case(5): return 5}", 5L);
        check("switch(1) { case(1): return 1, case(2): return 2, case(3): return 5}", 1L);
        check("switch(3) { case(1): return 1, case(2): return 2}", null);

        check("switch(\"1\") { case(\"1\"): return 1, case(\"2\"): return 2}", 1L);
        check("switch(\"3\") { case(\"1\"): return 1, case(\"2\"): return 2}", null);

        check("switch(1.2) { case(1.1): return \"1\", case(1.2): return \"2\"}", "2");

        check("switch([1,2]) { case([1]): return \"1\", case([1,2]): return \"2\"}", "2");
        check("switch([1,2]) { case([1]): return \"1\", case([1,2,3]): return \"2\"}", null);
    }

    // ---------------------------------------------------------------------------------------------
    @Test
    public void testLstComp () {
        rule = grammar.root;

        // test int
        checkExpr("[x for x in [1, 2, 3, 4] (x \"!=\" 5) ]", new Object[]{1L, 2L, 3L, 4L});
        checkExpr("[x for x in [1, 2, 3, 4] (x \"!=\" 4) ]", new Object[]{1L, 2L, 3L});
        checkExpr("[x for x in [1, 2, 3, 4] (x \"!=\" 3) ]", new Object[]{1L, 2L, 4L});
        checkExpr("[x for x in [1, 2, 3, 4] (x \">\" 3) ]", new Object[]{4L});
        checkExpr("[x for x in [1, 2, 3, 4] (x \">=\" 3) ]", new Object[]{3L, 4L});

        //String
        checkExpr("[x for x in [\"1\", \"2\", \"3\", \"4\"] (x \"!=\" \"5\") ]", new Object[]{"1", "2", "3", "4"});
        checkExpr("[x for x in [\"1\", \"2\", \"3\", \"4\"] (x \"!=\" \"3\") ]", new Object[]{"1", "2", "4"});
        checkExpr("[x for x in [\"1\", \"2\", \"3\", \"4\"] (x \"==\" \"4\") ]", new Object[]{"4"});

        //Float
        checkExpr("[x for x in [1.2, 2.2, 3.2, 4.2] (x \"!=\" 5.2) ]", new Object[]{1.2, 2.2, 3.2, 4.2});
        checkExpr("[x for x in [1.2, 2.2, 3.2, 4.2] (x \"!=\" 4.2) ]", new Object[]{1.2, 2.2, 3.2});
        checkExpr("[x for x in [1.2, 2.2, 3.2, 4.2] (x \"!=\" 3.2) ]", new Object[]{1.2, 2.2, 4.2});
        checkExpr("[x for x in [1.2, 2.2, 3.2, 4.2] (x \">\" 3.2) ]", new Object[]{4.2});
        checkExpr("[x for x in [1.2, 2.2, 3.2, 4.2] (x \">=\" 3.2) ]", new Object[]{3.2, 4.2});

        // Test get empty list
        checkExpr("[x for x in [1.2, 2.2, 3.2, 4.2] (x \">=\" 6.2) ]", new Object[]{});
        checkExpr("[x for x in [1, 2, 3, 4] (x \"<\" -5) ]", new Object[]{});
        checkExpr("[x for x in [\"1\", \"2\", \"3\", \"4\"] (x \"==\" \"10\") ]", new Object[]{});
    }
    @Test
    public void testFor () {
        rule = grammar.root;
        //Int
        check("" +
            "for (var i: Int = 1 , i < 3 , i = i + 1) {" +
            "print(\"\" + i)" +
            "}",
            null,
            "1\n2\n");
        check("" +
                "for (var i: Int = 1 , i <= 3 , i = i + 1) {" +
                "print(\"\" + i)" +
                "}",
            null,
            "1\n2\n3\n");
        check("" +
                "for (var i: Int = 1 , i < 5 , i = i * 2) {" +
                "print(\"\" + i)" +
                "}",
            null,
            "1\n2\n4\n");
        check("" +
                "for (var i: Int = 10 , i > 5 , i = i - 1) {" +
                "print(\"\" + i)" +
                "}",
            null,
            "10\n9\n8\n7\n6\n");
        check("" +
                "for (var i: Int = 10 , i >= 5 , i = i - 1) {" +
                "print(\"\" + i)" +
                "}",
            null,
            "10\n9\n8\n7\n6\n5\n");
        check("" +
                "for (var i: Int = 10 , i >= 5 , i = i / 2) {" +
                "print(\"\" + i)" +
                "}",
            null,
            "10\n5\n");

        //Float
        check("" +
                "for (var i: Float = 2.5 , i < 2.8 , i = i + 0.1) {" +
                "print(\"\" + i)" +
                "}",
            null,
            "2.5\n2.6\n2.7\n");
        check("" +
                "for (var i: Float = 2.5 , i <= 2.7 , i = i + 0.1) {" +
                "print(\"\" + i)" +
                "}",
            null,
            "2.5\n2.6\n2.7\n");
        check("" +
                "for (var i: Float = 1.25 , i < 5 , i = i * 2) {" +
                "print(\"\" + i)" +
                "}",
            null,
            "1.25\n2.5\n");
        check("" +
                "for (var i: Float = 6.2 , i > 5.4 , i = i - 0.2) {" +
                "print(\"\" + i)" +
                "}",
            null,
            "6.2\n6.0\n5.8\n5.6\n");
        check("" +
                "for (var i: Float = 6.2 , i >= 5.6 , i = i - 0.2) {" +
                "print(\"\" + i)" +
                "}",
            null,
            "6.2\n6.0\n5.8\n5.6\n");
        check("" +
                "for (var i: Float = 2.2 , i >= 1 , i = i / 2) {" +
                "print(\"\" + i)" +
                "}",
            null,
            "2.2\n1.1\n");
    }
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testInference () {
        check("var array: Int[] = []", null);
        check("var array: String[] = []", null);
        check("fun use_array (array: Int[]) {} ; use_array([])", null);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testTypeAsValues () {
        check("struct S{} ; return \"\"+ S", "S");
        check("struct S{} ; var type: Type = S ; return \"\"+ type", "S");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testUnconditionalReturn()
    {
        rule = grammar.root;
        check("fun f(): Int { if (true) return 1 else return 2 } ; return f()", 1L);
    }

    // ---------------------------------------------------------------------------------------------

    // NOTE(norswap): Not incredibly complete, but should cover the basics.
}
