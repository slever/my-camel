import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplitAggregationTest extends CamelTestSupport {
    private final Logger LOGGER = LoggerFactory.getLogger(SplitAggregationTest.class);

    ExecutorService myExecutor = new ThreadPoolExecutor(4, 4, 10,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .split(body())
                        .aggregationStrategy((Exchange oldExchange, Exchange newExchange) -> {
                            LOGGER.info("> Aggregate {} to new: {}",
                                    oldExchange == null ? "NULL" : oldExchange.getMessage().getBody() + ":" + oldExchange.getExchangeId(),
                                    newExchange.getMessage().getBody() + ":" + newExchange.getExchangeId());

                            long start = System.currentTimeMillis();

                            while (System.currentTimeMillis() - start < 1000) {
                                Math.cos(Math.random());
                            }

                            return oldExchange == null ? newExchange : oldExchange;
                        })
                        .parallelAggregate()
                        .parallelProcessing()
                        .streaming()
                        .executorService(myExecutor)
                        .log(LoggingLevel.INFO, LOGGER, "Split part ${body}")
                        .end()
                        .log(LoggingLevel.INFO, LOGGER, "End aggregation  ${body}")
                        .end()
                ;
            }
        };
    }

    /**
     * Parallel aggregation is multithreaded with camel.version: 2.X
     * BUT one at a time from 3.X version
     */
    @Test
    public void testParallelAggregate() throws InterruptedException {

        while (true) {
            template.sendBody("direct:start", Arrays.asList("A", "B", "C", "D"));
            Thread.sleep(5000);
        }
    }
}

