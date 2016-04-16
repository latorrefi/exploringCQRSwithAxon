/*
 * Copyright (c) 2015. Bearchoke
 */

package exploringaxon.mongo.converter.spring;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.time.Duration;

/**
 * Created by Bjorn Harvold
 * Date: 10/24/15
 * Time: 15:12
 * Responsibility:
 */
@WritingConverter
public class DurationWriteConverter implements Converter<Duration, String> {

    @Override
    public String convert(Duration duration) {
        String result = null;

        /*if (log.isTraceEnabled()) {
            log.trace("Converting Duration {} to string", duration.toString());
        }*/

        result = duration.toString();

        return result;
    }
}
