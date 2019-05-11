package itme.localcalendarexporter.query;

import java.util.ArrayList;

public class Empty extends Query {
    @Override
    public String getSelectionText() {
        return null;
    }

    @Override
    public ArrayList<String> getSelectionArguments() {
        return null;
    }
}
