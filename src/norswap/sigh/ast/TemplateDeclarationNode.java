package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;
import java.util.List;

public class TemplateDeclarationNode extends DeclarationNode
{
    public final String name;
    public List<ParameterNode> parameters;
    public final TypeNode returnType;
    public final BlockNode block;

    @SuppressWarnings("unchecked")
    public TemplateDeclarationNode
            (Span span, Object name, Object parameters, Object returnType, Object block) {
        super(span);
        this.name = Util.cast(name, String.class);
        this.parameters = Util.cast(parameters, List.class);
        this.returnType = returnType == null
            ? new SimpleTypeNode(new Span(span.start, span.start), "Void")
            : Util.cast(returnType, TypeNode.class);
        this.block = Util.cast(block, BlockNode.class);
    }

    @Override public String name () {
        return name;
    }

    @Override public String contents () {
        return "template " + name;
    }

    @Override public String declaredThing () {
        return "template";
    }
}
