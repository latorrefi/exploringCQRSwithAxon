package exploringaxon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.XStream;
import exploringaxon.model.Account;
import exploringaxon.mongo.axon.AxonMongoTemplate;
import exploringaxon.mongo.axon.AxonSagaMongoTemplate;
import exploringaxon.mongo.config.MongoCoreConfig;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.commandhandling.annotation.AggregateAnnotationCommandHandler;
import org.axonframework.commandhandling.annotation.AnnotationCommandHandlerBeanPostProcessor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.CommandGatewayFactoryBean;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.axonframework.contextsupport.spring.AnnotationDriven;
import org.axonframework.eventhandling.*;
import org.axonframework.eventhandling.amqp.AMQPConsumerConfiguration;
import org.axonframework.eventhandling.amqp.DefaultAMQPMessageConverter;
import org.axonframework.eventhandling.amqp.spring.ListenerContainerLifecycleManager;
import org.axonframework.eventhandling.amqp.spring.SpringAMQPConsumerConfiguration;
import org.axonframework.eventhandling.amqp.spring.SpringAMQPTerminal;
import org.axonframework.eventhandling.annotation.AnnotationEventListenerBeanPostProcessor;
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
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.scheduling.support.TaskUtils;

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

    // Rabbit
    private @Value("${rabbitmq.host}") String rabbitHost;
    private @Value("${rabbitmq.port}") Integer rabbitPort;
    private @Value("${rabbitmq.username}") String rabbitUsername;

    private @Value("${rabbitmq.password}") String rabbitPassword;
    private @Value("${rabbitmq.exchange.name}") String rabbitExchangeName;
    private @Value("${rabbitmq.exchange.autodelete}") boolean rabbitExchangeAutodelete;
    private @Value("${rabbitmq.exchange.durable}") boolean rabbitExchangeDurable;
    private @Value("${rabbitmq.queue.name}") String rabbitQueueName;
    private @Value("${rabbitmq.queue.durable}") Boolean rabbitQueueDurable;
    private @Value("${rabbitmq.queue.exclusive}") Boolean rabbitQueueExclusive;
    private @Value("${rabbitmq.queue.autodelete}") Boolean rabbitQueueAutoDelete;
    private @Value("${rabbitmq.queue-listener.prefetch-count}") Integer rabbitQueueListenerPrefetchCount;
    private @Value("${rabbitmq.queue-listener.recovery-interval}") Long rabbitQueueListenerRecoveryInterval;
    private @Value("${rabbitmq.queue-listener.cluster-transaction-size}") Integer rabbitQueueClusterTransactionSize;

    // Connection Factory
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitHost, rabbitPort);
        connectionFactory.setUsername(rabbitUsername);
        connectionFactory.setPassword(rabbitPassword);
        return connectionFactory;
    }


    // Event bus exchange
    @Bean
    public FanoutExchange eventBusExchange() {
        return new FanoutExchange(rabbitExchangeName, rabbitExchangeDurable, rabbitExchangeAutodelete);
    }


    // Event bus queue
    @Bean
    public Queue eventBusQueue() {
        return new Queue(rabbitQueueName, rabbitQueueDurable, rabbitQueueExclusive, rabbitQueueAutoDelete);
    }


    // Binding
    @Bean
    public Binding binding() {
        return BindingBuilder.bind(eventBusQueue()).to(eventBusExchange());
    }


    // Rabit Admin
    @Bean
    public RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory());
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }


    @Bean
    public SimpleCommandBus commandBus() {
        SimpleCommandBus simpleCommandBus = new SimpleCommandBus();
        return simpleCommandBus;
    }


    // Event bus
    @Bean
    public EventBus eventBus() {
        ClusteringEventBus clusteringEventBus = new ClusteringEventBus(new DefaultClusterSelector(simpleCluster()), terminal());
        return clusteringEventBus;
    }

    /*@Bean
    public XStreamSerializer xstreamSerializer() {
        return new XStreamSerializer();
    }*/

    // Terminal
    @Bean
    public EventBusTerminal terminal() {
        SpringAMQPTerminal terminal = new SpringAMQPTerminal();
        terminal.setConnectionFactory(connectionFactory());
        XStreamSerializer xStreamSerializer = new XStreamSerializer(xStream);
        terminal.setSerializer(xStreamSerializer);
        terminal.setExchangeName(rabbitExchangeName);
        terminal.setListenerContainerLifecycleManager(listenerContainerLifecycleManager());
        terminal.setDurable(true);
        terminal.setTransactional(false);
        return terminal;
    }

    @Bean
    public ListenerContainerLifecycleManager listenerContainerLifecycleManager() {
        ListenerContainerLifecycleManager listenerContainerLifecycleManager = new ListenerContainerLifecycleManager();
        listenerContainerLifecycleManager.setConnectionFactory(connectionFactory());
        listenerContainerLifecycleManager.registerCluster(simpleCluster(), springAMQPConsumerConfiguration(), defaultAMQPMessageConverter() );
        return listenerContainerLifecycleManager;
    }

    // Configuration
    @Bean
    AMQPConsumerConfiguration springAMQPConsumerConfiguration() {
        SpringAMQPConsumerConfiguration springAMQPConsumerConfiguration = new SpringAMQPConsumerConfiguration();
        springAMQPConsumerConfiguration.setDefaults(null);
        springAMQPConsumerConfiguration.setQueueName(rabbitQueueName);
        springAMQPConsumerConfiguration.setErrorHandler(TaskUtils.getDefaultErrorHandler(false));
        //TODO Know how is to set acknoledge, maybe manual is the right choise
        springAMQPConsumerConfiguration.setAcknowledgeMode(AcknowledgeMode.AUTO);
        springAMQPConsumerConfiguration.setConcurrentConsumers(1);
        springAMQPConsumerConfiguration.setRecoveryInterval(rabbitQueueListenerRecoveryInterval);
        //TODO Know how is to set excluseive, maybe true is the right choise
        springAMQPConsumerConfiguration.setExclusive(false);
        springAMQPConsumerConfiguration.setPrefetchCount(rabbitQueueListenerPrefetchCount);
        springAMQPConsumerConfiguration.setTransactionManager(new RabbitTransactionManager(connectionFactory()));
        springAMQPConsumerConfiguration.setTxSize(rabbitQueueClusterTransactionSize);
        return springAMQPConsumerConfiguration;
    }


    // Cluster definition
    @Bean
    SimpleCluster simpleCluster() {
        SimpleCluster simpleCluster = new SimpleCluster(rabbitQueueName);
        return simpleCluster;
    }


    // Message converter
    @Bean
    DefaultAMQPMessageConverter defaultAMQPMessageConverter() {
        XStreamSerializer xStreamSerializer = new XStreamSerializer(xStream);
        return new DefaultAMQPMessageConverter(xStreamSerializer);
    }


    /*// Message listener configuration
    @Bean
    ListenerContainerLifecycleManager listenerContainerLifecycleManager() {
        ListenerContainerLifecycleManager listenerContainerLifecycleManager = new ListenerContainerLifecycleManager();
        listenerContainerLifecycleManager.setConnectionFactory(connectionFactory());
        return listenerContainerLifecycleManager;
    }*/


    // Event listener
    @Bean
    public AnnotationEventListenerBeanPostProcessor annotationEventListenerBeanPostProcessor() {
        AnnotationEventListenerBeanPostProcessor processor = new AnnotationEventListenerBeanPostProcessor();
        processor.setEventBus(eventBus());
        return processor;
    }


    /**
     * The simple command bus, an implementation of an EventBus
     * mostly appropriate in a single JVM, single threaded use case.
     * @return the {@link SimpleEventBus}

    @Bean
    public SimpleEventBus eventBus() {
        return new SimpleEventBus();
    }

    @Bean
    public DefaultCommandGateway commandGateway() {
        return new DefaultCommandGateway(commandBus());
    }*/

    @Bean
    public AnnotationCommandHandlerBeanPostProcessor annotationCommandHandlerBeanPostProcessor() {
        AnnotationCommandHandlerBeanPostProcessor processor = new AnnotationCommandHandlerBeanPostProcessor();
        processor.setCommandBus(commandBus());
        return processor;
    }

    // Command Gateway
    @Bean
    public CommandGatewayFactoryBean<CommandGateway> commandGatewayFactoryBean() {
        CommandGatewayFactoryBean<CommandGateway> factory = new CommandGatewayFactoryBean<CommandGateway>();
        factory.setCommandBus(commandBus());
        return factory;
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
