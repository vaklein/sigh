package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.sigh.types.VoidType;
import norswap.utils.Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ListComprehensionNode extends ExpressionNode
{
    public final String ref1;
    public final String ref2;
    public final Object lst;
    public final String ref3;
    public final StringLiteralNode condition;
    public final Object stmt;

    @SuppressWarnings("unchecked")
    public ListComprehensionNode (Span span, Object ref1, Object ref2, Object lst, Object ref3, Object condition, Object stmt) {
        super(span);
        this.ref1 = (String) ref1;
        this.ref2 = (String) ref2;
        this.lst = lst;
        this.ref3 = (String) ref3;
        this.condition = (StringLiteralNode) condition;
        this.stmt = stmt;
        System.out.println(this.ref1);
        System.out.println(ref2);
        System.out.println(lst);
        System.out.println(ref2);
        System.out.println(condition);
        System.out.println(((StringLiteralNode) condition).value);
        System.out.println(stmt);
    }

    @Override public String contents ()
    {
        System.out.println("enter");
        StringBuilder b = new StringBuilder("[");
        b.append(ref1);
        b.append(", ");
        b.append(ref2);
        b.append(", ");
        b.append(lst);
        b.append(", (");
        b.append(ref3);
        b.append(" ");
        b.append(condition);
        b.append(" ");
        b.append(stmt);
        b.append(")");
        b.append(']');
        System.out.println("b");
        System.out.println("icii");
        System.out.println(b);
        return b.toString();
    }
}
