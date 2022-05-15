package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;
import java.util.List;

public final class TemplateCallNode extends ExpressionNode
{
    public ExpressionNode template;
    public final List<ExpressionNode> arguments;

    @SuppressWarnings("unchecked")
    public TemplateCallNode (Span span, Object template, Object arguments) {
        super(span);
        this.template = Util.cast(template, ExpressionNode.class);
        this.arguments = Util.cast(arguments, List.class);
    }

    @Override public String contents ()
    {
        String args = arguments.size() == 0 ? "()" : "(...)";
        return template.contents() + args;
    }
}
