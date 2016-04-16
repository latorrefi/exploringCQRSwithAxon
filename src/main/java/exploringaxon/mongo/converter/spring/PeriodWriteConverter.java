/*
 * Copyright (c) 2015. Bearchoke
 */

package exploringaxon.mongo.converter.spring;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.time.Period;

/**
 * Created by Bjorn Harvold
 * Date: 10/24/15
 * Time: 15:12
 * Responsibility:
 */
@WritingConverter
public class PeriodWriteConverter implements Converter<Period, String> {

    @Override
    public String convert(Period period) {
        String result = null;

        /*if (log.isTraceEnabled()) {
            log.trace("Converting Period {} to string", period.toString());
        }*/

        result = period.toString();

        return result;
    }
}
