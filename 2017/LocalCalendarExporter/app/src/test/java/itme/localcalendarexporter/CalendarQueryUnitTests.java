package itme.localcalendarexporter;

import junit.framework.TestSuite;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import itme.localcalendarexporter.query.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class CalendarQueryUnitTests {
    @Test(expected = QueryParseException.class)
    public void emptyQuery() throws QueryParseException {
        CalendarQuery.parse("");
    }

    @Test(expected = QueryParseException.class)
    public void justOperator() throws QueryParseException {
        CalendarQuery.parse("AND");
    }

    @Test(expected = QueryParseException.class)
    public void multipleOperators() throws QueryParseException {
        CalendarQuery.parse("foo:bar AND OR");
    }

    @Test(expected = QueryParseException.class)
    public void multipleExpressions() throws QueryParseException {
        CalendarQuery.parse("foo:bar bat:baz");
    }

    @Test
    public void simpleField() throws QueryParseException {
        CalendarQuery query = CalendarQuery.parse("foo:bar");
        assertThat(query.getQuery(), instanceOf(Expression.class));
        Expression parsedQuery = (Expression) query.getQuery();
        assertEquals("foo", parsedQuery.Field);
        assertEquals("bar", parsedQuery.Value);
    }

    @Test
    public void allFields() throws QueryParseException {
        CalendarQuery query = CalendarQuery.parse("bar");
        assertThat(query.getQuery(), instanceOf(Expression.class));
        Expression parsedQuery = (Expression) query.getQuery();
        assertEquals("all", parsedQuery.Field);
        assertEquals("bar", parsedQuery.Value);
    }

    @Test
    public void multipleColons() throws QueryParseException {
        CalendarQuery query = CalendarQuery.parse("foo:bar:baz");
        assertThat(query.getQuery(), instanceOf(Expression.class));
        Expression parsedQuery = (Expression) query.getQuery();
        assertEquals("foo", parsedQuery.Field);
        assertEquals("bar:baz", parsedQuery.Value);
    }

    @Test
    public void andExpression() throws QueryParseException {
        CalendarQuery query = CalendarQuery.parse("foo AND bar");

        CompoundQuery parsedQuery = (CompoundQuery) query.getQuery();
        assertEquals(Operator.AND, parsedQuery.Operator);
    }

    @Test
    public void orExpression() throws QueryParseException {
        CalendarQuery query = CalendarQuery.parse("foo OR bar");

        CompoundQuery parsedQuery = (CompoundQuery) query.getQuery();
        assertEquals(Operator.OR, parsedQuery.Operator);
    }

    @Test
    public void compoundExpression() throws QueryParseException {
        CalendarQuery query = CalendarQuery.parse("foo:bar AND bat:baz");

        assertThat(query.getQuery(), instanceOf(CompoundQuery.class));
        CompoundQuery parsedQuery = (CompoundQuery) query.getQuery();

        Query left = parsedQuery.Left;
        Operator operator = parsedQuery.Operator;
        Query right = parsedQuery.Right;

        assertThat(left, instanceOf(Expression.class));
        Expression leftExp = (Expression) left;
        assertEquals("foo", leftExp.Field);
        assertEquals("bar", leftExp.Value);

        assertThat(right, instanceOf(Expression.class));
        Expression rightExp = (Expression) right;
        assertEquals("bat", rightExp.Field);
        assertEquals("baz", rightExp.Value);

        assertEquals(Operator.AND, operator);
    }
}