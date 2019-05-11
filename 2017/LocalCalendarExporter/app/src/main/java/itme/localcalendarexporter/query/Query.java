package itme.localcalendarexporter.query;

import java.util.ArrayList;

public abstract class Query {
    public abstract String getSelectionText();
    public abstract ArrayList<String> getSelectionArguments();
}
