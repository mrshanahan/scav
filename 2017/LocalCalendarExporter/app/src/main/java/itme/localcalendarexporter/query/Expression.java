package itme.localcalendarexporter.query;

import java.util.ArrayList;
import java.util.Locale;

public class Expression extends Query {
    public Expression(String field, String value) {
        Field = field;
        Value = value;
    }

    public final String Field;
    public final String Value;

    @Override
    public String getSelectionText() {
        return String.format(
                Locale.getDefault(),
                "%s = %s",
                Field, Value);
    }

    @Override
    public ArrayList<String> getSelectionArguments() {
        ArrayList<String> list = new ArrayList<>();
        list.add(Value);
        return list;
    }


}
