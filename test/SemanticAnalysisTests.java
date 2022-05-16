import norswap.autumn.AutumnTestFixture;
import norswap.autumn.positions.LineMapString;
import norswap.sigh.SemanticAnalysis;
import norswap.sigh.SighGrammar;
import norswap.sigh.ast.SighNode;
import norswap.uranium.Reactor;
import norswap.uranium.UraniumTestFixture;
import norswap.utils.visitors.Walker;
import org.testng.annotations.Test;

/**
 * NOTE(norswap): These tests were derived from the {@link InterpreterTests} and don't test anything
 * more, but show how to idiomatically test semantic analysis. using {@link UraniumTestFixture}.
 */
public final class SemanticAnalysisTests extends UraniumTestFixture
{
    // ---------------------------------------------------------------------------------------------

    private final SighGrammar grammar = new SighGrammar();
    private final AutumnTestFixture autumnFixture = new AutumnTestFixture();

    {
        autumnFixture.rule = grammar.root();
        autumnFixture.runTwice = false;
        autumnFixture.bottomClass = this.getClass();
    }

    private String input;

    @Override protected Object parse (String input) {
        this.input = input;
        return autumnFixture.success(input).topValue();
    }

    @Override protected String astNodeToString (Object ast) {
        LineMapString map = new LineMapString("<test>", input);
        return ast.toString() + " (" + ((SighNode) ast).span.startString(map) + ")";
    }

    // ---------------------------------------------------------------------------------------------

    @Override protected void configureSemanticAnalysis (Reactor reactor, Object ast) {
        Walker<SighNode> walker = SemanticAnalysis.createWalker(reactor);
        walker.walk(((SighNode) ast));
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testLiteralsAndUnary() {
        successInput("return 42");
        successInput("return 42.0");
        successInput("return \"hello\"");
        successInput("return (42)");
        successInput("return [1, 2, 3]");
        successInput("return true");
        successInput("return false");
        successInput("return null");
        successInput("return !false");
        successInput("return !true");
        successInput("return !!true");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testNumericBinary() {
        successInput("return 1 + 2");
        successInput("return 2 - 1");
        successInput("return 2 * 3");
        successInput("return 2 / 3");
        successInput("return 3 / 2");
        successInput("return 2 % 3");
        successInput("return 3 % 2");

        successInput("return 1.0 + 2.0");
        successInput("return 2.0 - 1.0");
        successInput("return 2.0 * 3.0");
        successInput("return 2.0 / 3.0");
        successInput("return 3.0 / 2.0");
        successInput("return 2.0 % 3.0");
        successInput("return 3.0 % 2.0");

        successInput("return 1 + 2.0");
        successInput("return 2 - 1.0");
        successInput("return 2 * 3.0");
        successInput("return 2 / 3.0");
        successInput("return 3 / 2.0");
        successInput("return 2 % 3.0");
        successInput("return 3 % 2.0");

        successInput("return 1.0 + 2");
        successInput("return 2.0 - 1");
        successInput("return 2.0 * 3");
        successInput("return 2.0 / 3");
        successInput("return 3.0 / 2");
        successInput("return 2.0 % 3");
        successInput("return 3.0 % 2");

        failureInputWith("return 2 + true", "Trying to add Int with Bool");
        failureInputWith("return true + 2", "Trying to add Bool with Int");
        failureInputWith("return 2 + [1]", "Trying to add Int with Int[]");
        failureInputWith("return [1] + 2", "Trying to add Int[] with Int");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testOtherBinary() {
        successInput("return true && false");
        successInput("return false && true");
        successInput("return true && true");
        successInput("return true || false");
        successInput("return false || true");
        successInput("return false || false");

        failureInputWith("return false || 1",
            "Attempting to perform binary logic on non-boolean type: Int");
        failureInputWith("return 2 || true",
            "Attempting to perform binary logic on non-boolean type: Int");

        successInput("return 1 + \"a\"");
        successInput("return \"a\" + 1");
        successInput("return \"a\" + true");

        successInput("return 1 == 1");
        successInput("return 1 == 2");
        successInput("return 1.0 == 1.0");
        successInput("return 1.0 == 2.0");
        successInput("return true == true");
        successInput("return false == false");
        successInput("return true == false");
        successInput("return 1 == 1.0");

        failureInputWith("return true == 1", "Trying to compare incomparable types Bool and Int");
        failureInputWith("return 2 == false", "Trying to compare incomparable types Int and Bool");

        successInput("return \"hi\" == \"hi\"");
        successInput("return [1] == [1]");

        successInput("return 1 != 1");
        successInput("return 1 != 2");
        successInput("return 1.0 != 1.0");
        successInput("return 1.0 != 2.0");
        successInput("return true != true");
        successInput("return false != false");
        successInput("return true != false");
        successInput("return 1 != 1.0");

        failureInputWith("return true != 1", "Trying to compare incomparable types Bool and Int");
        failureInputWith("return 2 != false", "Trying to compare incomparable types Int and Bool");

        successInput("return \"hi\" != \"hi\"");
        successInput("return [1] != [1]");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testVarDecl() {
        successInput("var x: Int = 1; return x");
        successInput("var x: Float = 2.0; return x");

        successInput("var x: Int = 0; return x = 3");
        successInput("var x: String = \"0\"; return x = \"S\"");

        failureInputWith("var x: Int = true", "expected Int but got Bool");
        failureInputWith("return x + 1", "Could not resolve: x");
        failureInputWith("return x + 1; var x: Int = 2", "Variable used before declaration: x");

        // implicit conversions
        successInput("var x: Float = 1 ; x = 2");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testLstComp() {

        // Success
        successInput("[x for x in [1, 2, 3, 4] (x \"!=\" 5) ]");
        successInput("[x for x in [1, 2, 3, 4] (x \"!=\" 4) ]");
        successInput("[x for x in [1, 2, 3, 4] (x \">\" 3) ]");
        successInput("[x for x in [\"1\", \"2\", \"3\", \"4\"] (x \"!=\" \"5\") ]");
        successInput("[x for x in [\"1\", \"2\", \"3\", \"4\"] (x \"==\" \"4\") ]");
        successInput("[x for x in [1.2, 2.2, 3.2, 4.2] (x \"!=\" 4.2) ]");
        successInput("[x for x in [1.2, 2.2, 3.2, 4.2] (x \">\" 3.2) ]");
        successInput("[x for x in [1.2, 2.2, 3.2, 4.2] (x \">=\" 3.2) ]");

        // Failures
        failureInputWith("[x for y in [1, 2, 3, 4] (x \"!=\" 5) ]",
            "The variables don't have all the same name");
        failureInputWith("[y for y in [1, 2, 3, 4] (x \"!=\" 5) ]",
            "The variables don't have all the same name");
        failureInputWith("[x for x in [1, 2, 3, 4] (y \"!=\" 5) ]",
            "The variables don't have all the same name");
        failureInputWith("[x for y in [1, 2, 3, 4] (z \"!=\" 5) ]",
            "The variables don't have all the same name");

        failureInputWith("[x for x in [1, 2, 3, 4] (x \"!\" 5) ]",
            "Wrong conditions symbole");

        failureInputWith("[x for x in [] (x \"!=\" 5) ]",
            "The array is empty");

        failureInputWith("[x for x in [\"1\", \"2\", \"3\", \"4\"] (x \"<\" \"5\") ]",
            "incpompactible condition with the type of the values");

        failureInputWith("[x for x in [1, 2, 3, 4] (x \"!=\" \"4\") ]",
            "the elem in the conditon don't have the same type as the elem in the array");

        failureInputWith("[x for x in [1, \"4\", 3, 4] (x \"!=\" 4) ]",
            "The array don't have all the same type");

    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testRootAndBlock () {
        successInput("return");
        successInput("return 1");
        successInput("return 1; return 2");

        successInput("print(\"a\")");
        successInput("print(\"a\" + 1)");
        successInput("print(\"a\"); print(\"b\")");

        successInput("{ print(\"a\"); print(\"b\") }");

        successInput(
            "var x: Int = 1;" +
            "{ print(\"\" + x); var x: Int = 2; print(\"\" + x) }" +
            "print(\"\" + x)");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testCalls() {
        successInput(
            "fun add (a: Int, b: Int): Int { return a + b } " +
            "return add(4, 7)");

        successInput(
            "struct Point { var x: Int; var y: Int }" +
            "return $Point(1, 2)");

        successInput("var str: String = null; return print(str + 1)");

        failureInputWith("return print(1)", "argument 0: expected String but got Int");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testTemplate() {

        //////////////////////////////////////////////////
        //              Success Input                   //
        //////////////////////////////////////////////////

        // ** tests without "Void" **
        successInput(
            "template test (a: Int): Int { return a } " +
                "return test{4}");
        successInput(
            "template test (a: Int, b: Float): Float { return a + b } " +
                "return test{4, 7.1}");
        successInput(
            "template test (a: Int, b: Float, c: String): Float { return a + b } " +
                "return test{4, 7.1, \"3\"}");
        successInput(
            "template test (a: String, b: String, c: String): String { return a + b } " +
                "return test{\"1\", \"2\", \"3\"}");


        // tests mix "Void" and "no-Void"
        successInput(
            "template test (a: Int, b: Void): Float { return a + b } " +
                "return test{4, 7.1}");
        successInput(
            "template test (a: Void, b: Float): Float { return a + b } " +
                "return test{4, 7.1}");

        successInput(
            "template test (a: Int, b: Float, c: Void): Float { return a + b } " +
                "return test{4, 7.1, \"3\"}");
        successInput(
            "template test (a: Int, b: Void, c: String): Float { return a + b } " +
                "return test{4, 7.1, \"3\"}");
        successInput(
            "template test (a: Void, b: Float, c: String): Float { return a + b } " +
                "return test{4, 7.1, \"3\"}");
        successInput(
            "template test (a: Void, b: Void, c: String): Float { return a + b } " +
                "return test{4, 7.1, \"3\"}");
        successInput(
            "template test (a: Void, b: Float, c: Void): Float { return a + b } " +
                "return test{4, 7.1, \"3\"}");
        successInput(
            "template test (a: Int, b: Void, c: Void): Float { return a + b } " +
                "return test{4, 7.1, \"3\"}");

        successInput(
            "template test (a: Void, b: String, c: String): String { return a + b } " +
                "return test{\"1\", \"2\", \"3\"}");
        successInput(
            "template test (a: String, b: Void, c: String): String { return a + b } " +
                "return test{\"1\", \"2\", \"3\"}");
        successInput(
            "template test (a: String, b: String, c: Void): String { return a + b } " +
                "return test{\"1\", \"2\", \"3\"}");
        successInput(
            "template test (a: Void, b: Void, c: String): String { return a + b } " +
                "return test{\"1\", \"2\", \"3\"}");
        successInput(
            "template test (a: Void, b: String, c: Void): String { return a + b } " +
                "return test{\"1\", \"2\", \"3\"}");
        successInput(
            "template test (a: String, b: Void, c: Void): String { return a + b } " +
                "return test{\"1\", \"2\", \"3\"}");


        // ** tests with only "Void" **
        successInput(
            "template test (a: Void): Int { return a } " +
                "return test{4}");

        successInput(
            "template test (a: Void, b: Void): Float { return a + b } " +
                "return test{4, 7.1}");


        successInput(
            "template test (a: Void, b: Void, c: Void): Float { return a + b } " +
                "return test{4, 7.1, \"3\"}");
        successInput(
            "template test (a: Void, b: Void, c: Void): Int { return a } " +
                "return test{4, 7.1, \"3\"}");
        successInput(
            "template test (a: Void, b: Void, c: Void): String { return c } " +
                "return test{4, 7.1, \"3\"}");

        successInput(
            "template test (a: Void, b: Void, c: Void): String { return a + b } " +
                "return test{\"1\", \"2\", \"3\"}");
        successInput(
            "template test (a: Void, b: Void, c: Void): String { return a } " +
                "return test{\"1\", \"2\", \"3\"}");
        successInput(
            "template test (a: Void, b: Void, c: Void): String { return b } " +
                "return test{\"1\", \"2\", \"3\"}");
        successInput(
            "template test (a: Void, b: Void, c: Void): String { return c } " +
                "return test{\"1\", \"2\", \"3\"}");


        //////////////////////////////////////////////////
        //              Failure Input                   //
        //////////////////////////////////////////////////

        failureInputWith(
            "template test (a: Int): Int { return a } " +
                "return test{4.6}", "incompatible argument provided for argument 0: expected Int but got Float");

        failureInputWith(
            "template test (a: Void, b: String): Float { return a } " +
                "return test{4.6, 5}", "incompatible argument provided for argument 1: expected String but got Int");

        failureInputWith(
            "template test (a: Void, b: String, c: Int): Int { return a } " +
                "return test{4.6, \"3\", \"2\"}", "incompatible argument provided for argument 2: expected Int but got String");

        failureInputWith(
            "template test (a: Void, b : Void): Int { return a } " +
                "return test{4.6}", "wrong number of arguments, expected 2 but got 1");

        failureInputWith(
            "template test (a: Void): Int { return a } " +
                "return test{4.6, 10}", "wrong number of arguments, expected 1 but got 2");

    }
    // ---------------------------------------------------------------------------------------------

    @Test public void testArrayStructAccess() {
        successInput("return [1][0]");
        successInput("return [1.0][0]");
        successInput("return [1, 2][1]");

        failureInputWith("return [1][true]", "Indexing an array using a non-Int-valued expression");

        // TODO make this legal?
        // successInput("[].length", 0L);

        successInput("return [1].length");
        successInput("return [1, 2].length");

        successInput("var array: Int[] = null; return array[0]");
        successInput("var array: Int[] = null; return array.length");

        successInput("var x: Int[] = [0, 1]; x[0] = 3; return x[0]");
        successInput("var x: Int[] = []; x[0] = 3; return x[0]");
        successInput("var x: Int[] = null; x[0] = 3");

        successInput(
            "struct P { var x: Int; var y: Int }" +
            "return $P(1, 2).y");

        successInput(
            "struct P { var x: Int; var y: Int }" +
            "var p: P = null;" +
            "return p.y");

        successInput(
            "struct P { var x: Int; var y: Int }" +
            "var p: P = $P(1, 2);" +
            "p.y = 42;" +
            "return p.y");

        successInput(
            "struct P { var x: Int; var y: Int }" +
            "var p: P = null;" +
            "p.y = 42");

        failureInputWith(
            "struct P { var x: Int; var y: Int }" +
            "return $P(1, true)",
            "argument 1: expected Int but got Bool");

        failureInputWith(
            "struct P { var x: Int; var y: Int }" +
            "return $P(1, 2).z",
            "Trying to access missing field z on struct P");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testIfWhile () {
        successInput("if (true) ? return 1 : return 2");
        successInput("if (true) return 1 else return 2");
        successInput("if (false) return 1 else return 2");
        successInput("if (false) return 1 else if (true) return 2 else return 3 ");
        successInput("if (false) return 1 else if (false) return 2 else return 3 ");

        successInput("var i: Int = 0; while (i < 3) { print(\"\" + i); i = i + 1 } ");

        failureInputWith("if 1 return 1",
            "If statement with a non-boolean condition of type: Int");
        failureInputWith("while 1 return 1",
            "While statement with a non-boolean condition of type: Int");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testInference() {
        successInput("var array: Int[] = []");
        successInput("var array: String[] = []");
        successInput("fun use_array (array: Int[]) {} ; use_array([])");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testTypeAsValues() {
        successInput("struct S{} ; return \"\"+ S");
        successInput("struct S{} ; var type: Type = S ; return \"\"+ type");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testUnconditionalReturn()
    {
        successInput("fun f(): Int { if (true) return 1 else return 2 } ; return f()");
        // TODO: would be nice if this pinpointed the if-statement as missing the return,
        //   not the whole function declaration
        failureInputWith("fun f(): Int { if (true) return 1 } ; return f()",
            "Missing return in function");
    }
    // ---------------------------------------------------------------------------------------------
    @Test public void switchCase()
    {
        successInput("switch(1) { case(1): print(\"1\"), case(2): print(\"1\")}");
        successInput("var phrase : String = \"ca va ?\""+
            "switch(phrase){case(\"ca va ?\"):print(\"oui\")}");
        successInput("var x : Int = 2"+
            "switch(x){case(2):print(\"good\"), case(3):print(\"not good\")}");
        successInput("var x : Float = 2.5"+
            "switch(x){case(2.5):print(\"good\"), case(0.3):print(\"not good\")}");

        failureInput("switch(p) { case(1): print(\"1\"), case(2): print(\"1\")}");
        failureInput("switch(Null) { case(1): print(\"1\"), case(2): print(\"1\")}");
        failureInput("switch(1) { case(Null): print(\"1\"), case(2): print(\"1\")}");
        failureInput("switch(1) { case(1): print(\"1\"), case(Null): print(\"1\")}");
    }

    // ---------------------------------------------------------------------------------------------
    @Test public void forStmt()
    {
        successInput("for (var i: Int = 0 , i < 4 , i = i + 1) {}");
        successInput("for (var i: Int = 0 , i <= 4 , i = i * 2) {}");
        successInput("for (var i: Int = 10 , i > 4 , i = i / 2) {}");
        successInput("for (var i: Int = 11 , i >= 4 , i = i - 1) {}");

        successInput("for (var i: Float = 1.5 , i < 4.3 , i = i + 1.1) {}");
        successInput("for (var i: Float = 1.1 , i <= 4.1 , i = i * 2.5) {}");
        successInput("for (var i: Float = 9.9 , i > 4.6 , i = i / 1.22) {}");
        successInput("for (var i: Float = 10.6 , i >= 4.4 , i = i - 0.2) {}");

        failureInput("for (var i: Bool = true, i < true , i = i + 1) {}");
        failureInput("for (var i: Bool = true, i < 4 , i = i + false) {}");
    }
}
