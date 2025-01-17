package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;
import java.util.List;

public final class SwitchNode extends StatementNode
{
    public final ExpressionNode argument;
    public final List<CaseNode> cases;

    public SwitchNode (Span span, Object argument, Object cases) {
        super(span);
        this.argument = Util.cast(argument, ExpressionNode.class);
        this.cases = Util.cast(cases, List.class);

    }

    @Override public String contents ()
    {
        String condition = this.argument.contents();
        String candidate = String.format("switch(%s) ...", condition);

        return candidate.length() <= contentsBudget()
            ? candidate
            : "switch(?) ...";
    }
}
