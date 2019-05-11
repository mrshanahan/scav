package itme.localcalendarexporter;

import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.Locale;

import itme.localcalendarexporter.query.CompoundQuery;
import itme.localcalendarexporter.query.Empty;
import itme.localcalendarexporter.query.Expression;
import itme.localcalendarexporter.query.Operator;
import itme.localcalendarexporter.query.Query;

public class CalendarQuery {
    static final String[] AVAILABLE_FIELDS = new String[] {
        CalendarContract.Events._ID,
        CalendarContract.Events.ALL_DAY,
        CalendarContract.Events.AVAILABILITY,
        CalendarContract.Events.DESCRIPTION,
        CalendarContract.Events.DTEND,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DURATION,
        CalendarContract.Events.EVENT_LOCATION,
        CalendarContract.Events.EVENT_TIMEZONE,
        CalendarContract.Events.EXDATE,
        CalendarContract.Events.EXRULE,
        CalendarContract.Events.LAST_DATE,
        CalendarContract.Events.ORIGINAL_ALL_DAY,
        CalendarContract.Events.ORIGINAL_ID,
        CalendarContract.Events.ORIGINAL_INSTANCE_TIME,
        CalendarContract.Events.RDATE,
        CalendarContract.Events.RRULE,
        CalendarContract.Events.STATUS,
        CalendarContract.Events.TITLE
    };

    static String[] getEventProjection() {
        return AVAILABLE_FIELDS;
    }

    private static ArrayList<String> loadOperators() {
        Operator[] operators = Operator.values();
        ArrayList<String> casedOperators = new ArrayList<String>();
        for (Operator op : operators) {
            casedOperators.add(op.toString().toUpperCase());
        }
        return casedOperators;
    }

    private static boolean isOperator(String token) {
        return _OPERATORS.contains(token.toUpperCase());
    }

    private static Operator asOperator(String token) {
        return Operator.valueOf(token.toUpperCase());
    }

    private static final ArrayList<String> _OPERATORS = loadOperators();

    static CalendarQuery parse(String queryString) throws QueryParseException {
        CalendarQuery query = new CalendarQuery();
        if (queryString == null || queryString.isEmpty()) {
            query.mQuery = new Empty();
            return query;
        }

        String[] tokens = queryString.split("\\s+");
        boolean expectingExpression = true;
        Query runningQuery = null;
        Operator runningOperator = null;
        for (String token : tokens) {
            if (expectingExpression) {
                if (isOperator(token)) {
                    throw new QueryParseException(String.format(Locale.getDefault(),
                            "Expected expression, got operator: '%s'",
                            token));
                }

                int firstColon = token.indexOf(':');

                if (firstColon == 0) {
                    throw new QueryParseException(String.format(Locale.getDefault(),
                            "Expressions should not begin with a colon. Follow the format of 'field:query' (to query one field) or 'query' (for all fields) (in expression: '%s').",
                            token));
                }
                if (firstColon == token.length() - 1) {
                    throw new QueryParseException(String.format(Locale.getDefault(),
                            "Expressions should not end with a colon. Follow the format of 'field:query' (to query one field) or 'query' (for all fields) (in expression: '%s').",
                            token));
                }

                String field, value;
                if (firstColon < 0) {
                    field = "all";
                    value = token;
                } else {
                    field = token.substring(0, firstColon);
                    value = token.substring(firstColon + 1);
                }

                Expression expression = new Expression(field, value);

                if (runningQuery == null) {
                    runningQuery = expression;
                } else {
                    runningQuery = new CompoundQuery(runningQuery, runningOperator, expression);
                }
                expectingExpression = false;
            } else {
                if (!isOperator(token)) {
                    throw new QueryParseException(String.format(
                            Locale.getDefault(),
                            "Expected operator, got expression: '%s'",
                            token));
                }
                runningOperator = asOperator(token);
                expectingExpression = true;
            }
        }

        query.mQuery = runningQuery;

        return query;
    }

    private Query mQuery;

    Query getQuery() {
        return mQuery;
    }

//    private String getSubSelectionText(Query query) throws SelectionGenerationException {
//        if (query instanceof CompoundQuery) {
//            CompoundQuery castQuery = (CompoundQuery) query;
//            String leftText = getSubSelectionText(castQuery.Left);
//            String rightText = getSubSelectionText(castQuery.Right);
//            String operator = castQuery.Operator.toString();
//            return String.format(
//                    Locale.getDefault(),
//                    "%s %s %s",
//                    leftText, operator, rightText);
//        }
//        else if (query instanceof Expression) {
//            Expression castQuery = (Expression) query;
//            return String.format(
//                    Locale.getDefault(),
//                    "%s = ?",
//                    castQuery.Field);
//        }
//        else {
//            throw new SelectionGenerationException(
//                    String.format(
//                            Locale.getDefault(),
//                            "Unexpected part of query: '%s'",
//                            query.toString()));
//        }
//    }

    String formatSelection() {
        String mainSelection = mQuery.getSelectionText();
        if (mainSelection == null || mainSelection.isEmpty()) {
            return "";
        }
        return String.format(
                Locale.getDefault(),
                " AND (%s)",
                mainSelection);
    }

    String getSelectionText() {
        return String.format(
                Locale.getDefault(),
                "((%s = ?)%s)",
                CalendarContract.Events._ID, formatSelection());
    }

    String[] getSelectionArguments(long localCalendarId) {
        String[] args = new String[] { Long.toString(localCalendarId) };
        return args;
    }
}