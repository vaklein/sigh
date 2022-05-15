package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;

public final class CaseNode extends StatementNode
{
    public final ExpressionNode condition;
    public final StatementNode instructions;

    public CaseNode (Span span, Object condition, Object instructions) {
        super(span);
        this.condition = Util.cast(condition, ExpressionNode.class);
        this.instructions = Util.cast(instructions, StatementNode.class);
    }

    @Override public String contents ()
    {
        String condition = this.condition.contents();
        String candidate = String.format("if %s ...", condition);

        return candidate;
    }
}
