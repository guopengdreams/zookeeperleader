package no.bekk.distsys.leader;

import com.google.common.eventbus.EventBus;
import io.dropwizard.Application;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import no.bekk.distsys.leader.dealer.DealerResource;
import no.bekk.distsys.leader.zookeeper.LeaderElector;
import no.bekk.distsys.leader.zookeeper.ZooKeeperService;
import no.bekk.distsys.leader.zookeeper.ZookeeperHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeadingApplication extends Application<LeadingConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(LeadingApplication.class);

    public static void main(String[] args) throws Exception {
        new LeadingApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<LeadingConfiguration> bootstrap) {
        super.initialize(bootstrap);
    }

    @Override
    public void run(LeadingConfiguration config, Environment env) throws Exception {
        LOG.info("Booting in ENVIRONMENT={}", System.getenv("ENVIRONMENT"));


        ZooKeeperService zooKeeperService = new ZooKeeperService(config.getZooKeeper());


        env.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                zooKeeperService.start();
            }

            @Override
            public void stop() throws Exception {
                zooKeeperService.stop();
            }
        });


        EventBus eventBus = new EventBus();


        LeaderElector leaderElector = new LeaderElector(eventBus, zooKeeperService, "/leaders", "dealer_");
        zooKeeperService.addListener(leaderElector);


        DealerResource dealerResource = new DealerResource();
        eventBus.register(dealerResource);


        env.jersey().register(dealerResource);


        env.healthChecks().register("zookeeper", new ZookeeperHealthCheck(zooKeeperService));
    }

    @Override
    public String getName() {
        return "zookeeperleader-app";
    }

}
