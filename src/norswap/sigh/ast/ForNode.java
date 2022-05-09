package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;

public final class ForNode extends StatementNode
{
    public final ExpressionNode condition;
    public final StatementNode body;
    public final VarDeclarationNode variable;
    public final AssignmentNode operation;

    public ForNode (Span span, Object variable, Object condition, Object operation, Object body) {
        super(span);
        this.condition = Util.cast(condition, ExpressionNode.class);
        this.body = Util.cast(body, StatementNode.class);
        this.variable = Util.cast(variable, VarDeclarationNode.class);
        this.operation = Util.cast(operation, AssignmentNode.class);
    }

    @Override public String contents ()
    {
        String candidate = String.format("for (%s;%s;%s) ...", variable.contents(), condition.contents(), operation.contents());

        return candidate.length() <= contentsBudget()
            ? candidate
            : "for (?) ...";
    }
}
