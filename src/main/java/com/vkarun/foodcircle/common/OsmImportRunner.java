package com.vkarun.foodcircle.common;

import com.vkarun.foodcircle.dto.ImportResult;
import com.vkarun.foodcircle.service.OsmImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

// Runs once at startup, after the context is ready, and only when started with --import-osm=<file>.
// Normal runs and tests do not pass the option, so nothing is imported.
@Component
public class OsmImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OsmImportRunner.class);

    private final OsmImporter importer;

    public OsmImportRunner(OsmImporter importer) {
        this.importer = importer;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("import-osm")) {
            return;
        }
        Path file = Path.of(args.getOptionValues("import-osm").get(0));
        ImportResult result = importer.importFile(file);
        log.info("OSM import finished: {} added, {} updated, {} skipped", result.added(), result.updated(), result.skipped());
    }
}
