/*
 * Copyright (c) 2015. Bearchoke
 */

package exploringaxon.mongo.config;

import exploringaxon.mongo.converter.xstream.*;
import com.thoughtworks.xstream.XStream;
import org.axonframework.serializer.xml.CompactDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Bjorn Harvold
 * Date: 7/19/14
 * Time: 10:36 PM
 * Responsibility:
 */
@Configuration
public class XStreamConfig {

    @Bean(name = "xStream")
    public XStream xStream() {
        XStream xStream = new XStream(new CompactDriver());

        xStream.registerConverter(new XStreamZonedDateTimeConverter());
        xStream.registerConverter(new XStreamDurationConverter());
        xStream.registerConverter(new XStreamLocalDateTimeConverter());
        xStream.registerConverter(new XStreamLocalDateConverter());
        xStream.registerConverter(new XStreamLocalTimeConverter());
        xStream.registerConverter(new XStreamPeriodConverter());

        return xStream;
    }

}
