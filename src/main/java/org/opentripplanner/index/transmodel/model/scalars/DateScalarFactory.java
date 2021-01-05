package org.opentripplanner.index.transmodel.model.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;
import org.opentripplanner.model.calendar.ServiceDate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;

public class DateScalarFactory {


    public static final String EXAMPLE_DATE = "2017-04-23";


    public static final String DATE_PATTERN = "yyyy-MM-dd";

    public static final String DATE_SCALAR_DESCRIPTION = "Date  using the format: " + DATE_PATTERN + ". Example: " + EXAMPLE_DATE;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    private DateScalarFactory() {
    }


    public static GraphQLScalarType createSecondsSinceEpochAsDateStringScalar(TimeZone timeZone) {
        return new GraphQLScalarType("Date", DATE_SCALAR_DESCRIPTION, new Coercing() {
            @Override
            public String serialize(Object input) {
                if (input instanceof Long) {
                    // Add 1 hour before converting to date, to account for daylight savings time. This is done because
                    // in for example TripTimeShort, only epoch time 12 hours before noon is stored instead of
                    // the original service date.
                    return ((Instant.ofEpochSecond((Long) input))).atZone(timeZone.toZoneId()).toLocalDateTime().plusHours(1).toLocalDate().format(FORMATTER);
                }
                else if (input instanceof ServiceDate) {
                    return ((ServiceDate) input).toYyyyMmDd();
                }

                return null;
            }

            @Override
            public Long parseValue(Object input) {
                try {
                    return LocalDate.from(FORMATTER.parse((CharSequence) input)).atStartOfDay(timeZone.toZoneId()).toEpochSecond();
                } catch (DateTimeParseException dtpe) {
                    throw new CoercingParseValueException("Expected type 'Date' but was '" + input + "'.");
                }
            }

            @Override
            public Long parseLiteral(Object input) {
                if (input instanceof StringValue) {
                    return parseValue(((StringValue) input).getValue());
                }
                return null;
            }
        });
    }

}
