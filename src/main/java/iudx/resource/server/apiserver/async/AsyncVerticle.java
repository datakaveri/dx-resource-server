package iudx.resource.server.apiserver.async;

import io.vertx.core.AbstractVerticle;

public class Async extends AbstractVerticle {
    @Override
    public void start() throws Exception {
    System.out.println("Aysnc verticle depolyed");
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}
