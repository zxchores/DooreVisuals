package dev.doorevisuals;

import dev.doorevisuals.net.PayloadRelay;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DooreServer implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Server");

    public void onInitialize() {
        PayloadRelay.initServer();
        LOG.info("DooreVisuals server relay ready");
    }
}
