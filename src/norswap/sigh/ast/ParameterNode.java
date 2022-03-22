package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;

public final class ParameterNode extends DeclarationNode
{
    public final String name;
    public TypeNode type;

    public ParameterNode (Span span, Object name, Object type) {
        super(span);
        this.name = Util.cast(name, String.class);
        this.type = Util.cast(type, TypeNode.class);
    }

    @Override public String name () {
        return name;
    }

    @Override public String contents () {
        return name;
    }

    @Override public String declaredThing () {
        return "parameter";
    }
}
