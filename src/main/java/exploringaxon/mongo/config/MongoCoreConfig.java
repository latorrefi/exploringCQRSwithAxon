/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package exploringaxon.mongo.config;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import exploringaxon.mongo.listener.DateCreatorMongoEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import javax.inject.Inject;
import java.net.UnknownHostException;

/**
 * Created by Bjorn Harvold
 * Date: 1/9/14
 * Time: 11:55 PM
 * Responsibility:
 */
@Configuration
@EnableMongoRepositories("exploringaxon")
public class MongoCoreConfig {

    @Inject
    private MongoDbFactory mongoDbFactory;

    @Bean(name = "mongoTemplate")
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(mongoDbFactory);
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor persistenceExceptionTranslationPostProcessor() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

    @Bean
    public DateCreatorMongoEventListener dateCreatorMongoEventListener() {
        return new DateCreatorMongoEventListener();
    }

    @Bean
    public MongoClient mongo() throws UnknownHostException {
        // location of db
        ServerAddress sa = new ServerAddress(
               "127.0.0.1",
                new Integer("27017")
        );

        // set optional default parameters here
        MongoClientOptions.Builder builder = MongoClientOptions.builder();

        // none yet

        MongoClientOptions options = builder.build();

        return new MongoClient(sa, options);
    }

    /*@Bean(name = "mongoDbFactory")
    public SimpleMongoDbFactory mongoDbFactory() throws Exception {
        return new SimpleMongoDbFactory(mongo(), "eventstore");
    }*/

    @Bean(name = "axonMongoDbFactory")
    public SimpleMongoDbFactory axonMongoDbFactory() throws Exception {
        return new SimpleMongoDbFactory(mongo(), "eventstore");
    }

}
