package itme.localcalendarexporter.query;

import java.util.ArrayList;
import java.util.Locale;

public class CompoundQuery extends Query {
    public CompoundQuery(Query left, Operator operator, Query right) {
        Left = left;
        Operator = operator;
        Right = right;
    }

    public final Query Left;
    public final Operator Operator;
    public final Query Right;

    @Override
    public String getSelectionText() {
        return String.format(
                Locale.getDefault(),
                "%s %s %s",
                Left, Operator, Right);
    }

    @Override
    public ArrayList<String> getSelectionArguments() {
        ArrayList<String> leftArgs = Left.getSelectionArguments();
        ArrayList<String> rightArgs = Right.getSelectionArguments();
        ArrayList<String> combinedArgs = new ArrayList<>(leftArgs);
        combinedArgs.addAll(rightArgs);
        return combinedArgs;
    }
}
