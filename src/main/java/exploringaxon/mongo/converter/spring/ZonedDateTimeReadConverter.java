/*
 * Copyright (c) 2015. Bearchoke
 */

package exploringaxon.mongo.converter.spring;


import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

/**
 * Created by Bjorn Harvold
 * Date: 10/24/15
 * Time: 15:12
 * Responsibility:
 */
@ReadingConverter
public class ZonedDateTimeReadConverter implements Converter<String, ZonedDateTime> {

    @Override
    public ZonedDateTime convert(String value) {
        ZonedDateTime result = null;

        try {
            /*if (log.isTraceEnabled()) {
                log.trace("Converting String {} to ZonedDateTime", value);
            }*/

            result = ZonedDateTime.parse(value);
        } catch (DateTimeParseException e) {
            /*log.error("{} could not be converted to java.time.ZonedDateTime", value);*/
        }

        return result;
    }
}
