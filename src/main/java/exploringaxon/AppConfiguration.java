package exploringaxon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.XStream;
import exploringaxon.model.Account;
import exploringaxon.mongo.axon.AxonMongoTemplate;
import exploringaxon.mongo.axon.AxonSagaMongoTemplate;
import exploringaxon.mongo.config.MongoCoreConfig;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.axonframework.contextsupport.spring.AnnotationDriven;
import org.axonframework.eventhandling.SimpleEventBus;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.axonframework.eventsourcing.Snapshotter;
import org.axonframework.eventsourcing.SpringAggregateSnapshotter;
import org.axonframework.eventstore.EventStore;
import org.axonframework.eventstore.fs.FileSystemEventStore;
import org.axonframework.eventstore.fs.SimpleEventFileResolver;
import org.axonframework.eventstore.mongo.MongoEventStore;
import org.axonframework.repository.LockManager;
import org.axonframework.repository.PessimisticLockManager;
import org.axonframework.saga.SagaRepository;
import org.axonframework.saga.repository.mongo.MongoSagaRepository;
import org.axonframework.saga.spring.SpringResourceInjector;
import org.axonframework.serializer.xml.XStreamSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.mongodb.MongoDbFactory;

import javax.inject.Inject;
import java.io.File;

/**
 * Created by Dadepo Aderemi.
 */
@Configuration
@AnnotationDriven
@Import(MongoCoreConfig.class)
public class AppConfiguration {

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private XStream xStream;

    @Inject
    @Qualifier("taskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    @Qualifier("axonMongoDbFactory")
    private MongoDbFactory mongoDbFactory;

    @Bean
    public SimpleCommandBus commandBus() {
        SimpleCommandBus simpleCommandBus = new SimpleCommandBus();
        return simpleCommandBus;
    }

    /**
     * The simple command bus, an implementation of an EventBus
     * mostly appropriate in a single JVM, single threaded use case.
     * @return the {@link SimpleEventBus}
     */
    @Bean
    public SimpleEventBus eventBus() {
        return new SimpleEventBus();
    }

    @Bean
    public DefaultCommandGateway commandGateway() {
        return new DefaultCommandGateway(commandBus());
    }

    /**
     * An event sourcing implementation needs a place to store events. i.e. The event Store.
     * In our use case we will be storing our events in a file system, so we configure
     * the FileSystemEventStore as our EventStore implementation
     *
     * It should be noted that Axon allows storing the events
     * in other persistent mechanism...jdbc, jpa etc
     *
     * @return the {@link EventStore}

    @Bean
    public EventStore eventStore() {
        EventStore eventStore = new FileSystemEventStore(new SimpleEventFileResolver(new File("./events")));
        return eventStore;
    }*/


    @Bean(name = "lockManager")
    public LockManager lockManager() {
//        OptimisticLockManager lockManager = new OptimisticLockManager();
        PessimisticLockManager lockManager = new PessimisticLockManager(); // default Axon lock manager

        return lockManager;
    }

    @Bean(name = "axonMongoTemplate")
    public AxonMongoTemplate axonMongoTemplate() {
        return new AxonMongoTemplate(mongoDbFactory);
    }

    @Bean(name = "axonSagaMongoTemplate")
    public AxonSagaMongoTemplate mongoSagaTemplate() {
        return new AxonSagaMongoTemplate(mongoDbFactory);
    }

    @Bean(name = "eventStore")
    public MongoEventStore eventStore() {
//        JacksonSerializer jsonSerializer = new JacksonSerializer(objectMapper);
        XStreamSerializer xStreamSerializer = new XStreamSerializer(xStream);

        return new MongoEventStore(xStreamSerializer, axonMongoTemplate());
    }

    @Bean(name = "sagaRepository")
    public SagaRepository sagaRepository() {
        MongoSagaRepository sagaRepository = new MongoSagaRepository(mongoSagaTemplate());
        sagaRepository.setResourceInjector(new SpringResourceInjector());

        return sagaRepository;
    }

    @Bean(name = "snapshotter")
    public Snapshotter snapshotter() {
        SpringAggregateSnapshotter sas = new SpringAggregateSnapshotter();
        sas.setEventStore(eventStore());
        sas.setExecutor(taskExecutor);

        return sas;
    }

    /**
     * Our aggregate root is now created from stream of events and not from a representation in a persistent mechanism,
     * thus we need a repository that can handle the retrieving of our aggregate root from the stream of events.
     *
     * We configure the EventSourcingRepository which does exactly this. We supply it with the event store
     * @return {@link EventSourcingRepository}
     */
    @Bean
    public EventSourcingRepository eventSourcingRepository() {
        EventSourcingRepository eventSourcingRepository = new EventSourcingRepository(Account.class, eventStore());
        eventSourcingRepository.setEventBus(eventBus());
        return eventSourcingRepository;
    }
}
