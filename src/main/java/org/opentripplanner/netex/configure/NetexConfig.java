package org.opentripplanner.netex.configure;

import org.opentripplanner.datastore.CompositeDataSource;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;
import org.opentripplanner.datastore.file.ZipFileDataSource;
import org.opentripplanner.model.modes.TransitModeConfiguration;
import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.netex.loader.NetexBundle;
import org.opentripplanner.netex.loader.NetexDataSourceHierarchy;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.SubmodesConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for dependency injection and creating main NeTEx module
 * objects. This decouple the main classes in the netex module, and serve
 * as a single entry-point to create a {@link NetexModule} witch simplify
 * the code({@link org.opentripplanner.graph_builder.GraphBuilder}) using it.
 * <p>
 * This class inject the build configuration. This way none of the other
 * classes in the `org.opentripplanner.netex` have dependencies to the
 * {@link BuildConfig}.
 * <p>
 * The naming convention used is close to the defacto standard used by Spring.
 */
public class NetexConfig {

    private final BuildConfig buildParams;

    private final SubmodesConfig submodesConfig;

    private NetexConfig(
        BuildConfig builderParams,
        SubmodesConfig submodesConfig
    ) {
        this.buildParams = builderParams;
        this.submodesConfig = submodesConfig;
    }


    public static NetexModule netexModule(
            BuildConfig buildParams,
            SubmodesConfig submodesConfig,
            Iterable<DataSource> netexSources
    ) {
        return new NetexConfig(buildParams, submodesConfig)
            .netexModule(netexSources);
    }

    public static NetexBundle netexBundleForTest(
        BuildConfig builderParams,
        SubmodesConfig submodesConfig,
        TransitModeConfiguration transitModeConfiguration,
        File netexZipFile
    ) {
        return new NetexConfig(builderParams, submodesConfig)
            .netexBundle(new ZipFileDataSource(netexZipFile, FileType.NETEX));
    }

    private NetexModule netexModule(Iterable<DataSource> netexSources) {
        List<NetexBundle> netexBundles = new ArrayList<>();

        for(DataSource it : netexSources){
            NetexBundle netexBundle = netexBundle((CompositeDataSource)it);
            netexBundles.add(netexBundle);
        }

        return new NetexModule(
                buildParams.netex.netexFeedId,
                buildParams.parentStopLinking,
                buildParams.stationTransfers,
                buildParams.getSubwayAccessTimeSeconds(),
                buildParams.maxInterlineDistance,
                buildParams.getTransitServicePeriod(),
                netexBundles
        );
    }

    /** public to enable testing */
    private NetexBundle netexBundle(CompositeDataSource source) {
        return new NetexBundle(
            buildParams.netex.netexFeedId,
            source,
            hierarchy(source),
            submodesConfig
        );
    }

    private NetexDataSourceHierarchy hierarchy(CompositeDataSource source){
        org.opentripplanner.standalone.config.NetexConfig c = buildParams.netex;
        return new NetexDataSourceHierarchy(source).prepare(
                c.ignoreFilePattern,
                c.sharedFilePattern,
                c.sharedGroupFilePattern,
                c.groupFilePattern
        );
    }
}
