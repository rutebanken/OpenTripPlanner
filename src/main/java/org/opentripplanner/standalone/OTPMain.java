/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.standalone;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.OtpStatusFile;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.GraphScanner;
import org.opentripplanner.routing.impl.MemoryGraphSource;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.visualizer.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * This is the main entry point to OpenTripPlanner. It allows both building graphs and starting up an OTP server
 * depending on command line options. OTPMain is a concrete class making it possible to construct one with custom
 * CommandLineParameters and use its graph builder construction method from web services or scripts, not just from the
 * static main function below.
 *
 * TODO still it seems fairly natural for all of these methods to be static.
 */
public class OTPMain {

    private static final Logger LOG = LoggerFactory.getLogger(OTPMain.class);

    static {
        // Disable HSQLDB reconfiguration of Java Unified Logging (j.u.l)
        System.setProperty("hsqldb.reconfig_logging", "false");

        // Remove existing handlers attached to the j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        // Bridge j.u.l (used by Jersey) to the SLF4J root logger, so all logging goes through the same API
        SLF4JBridgeHandler.install();
    }

    private final CommandLineParameters params;
    public OTPServer otpServer = null;
    public GraphService graphService = null;

    /** ENTRY POINT: This is the main method that is called when running otp.jar from the command line. */
    public static void main(String[] args) {

        /* Parse and validate command line parameters. */
        CommandLineParameters params = new CommandLineParameters();
        try {
            JCommander jc = new JCommander(params, args);
            if (params.version) {
                System.out.println(MavenVersion.VERSION.getLongVersionString());
                System.exit(0);
            }
            if (params.help) {
                System.out.println(MavenVersion.VERSION.getShortVersionString());
                jc.setProgramName("java -Xmx[several]G -jar otp.jar");
                jc.usage();
                System.exit(0);
            }
            params.infer();
        } catch (ParameterException pex) {
            System.out.println(MavenVersion.VERSION.getShortVersionString());
            LOG.error("Parameter error: {}", pex.getMessage());
            System.exit(1);
        }

        if (params.build == null && !params.visualize && !params.server && params.scriptFile == null) {
            LOG.info("Nothing to do. Use --help to see available tasks.");
            System.exit(-1);
        }

        OTPMain main = new OTPMain(params);
        main.run();
        OtpStatusFile.exitStatusOk();
    }

    /* Constructor. */
    public OTPMain(CommandLineParameters params) {
        this.params = params;
    }

    /**
     * Making OTPMain a concrete class and placing this logic an instance method instead of embedding it in the static
     * main method makes it possible to build graphs from web services or scripts, not just from the command line.
     */
    public void run() {

        // TODO do params.infer() here to ensure coherency?

        try {

            /* Create the top-level objects that represent the OTP server. */
            makeGraphService();
            otpServer = new OTPServer(params, graphService);

            /* Start graph builder if requested */
            if (params.build != null) {
                GraphBuilder graphBuilder = GraphBuilder.forDirectory(params, params.build);

                if (graphBuilder != null) {
                    graphBuilder.run();
                    /* If requested, hand off the graph to the server as the default graph using an in-memory GraphSource. */
                    if (params.inMemory || params.preFlight) {
                        Graph graph = graphBuilder.getGraph();
                        graph.index(new DefaultStreetVertexIndexFactory());
                        // FIXME set true router IDs
                        graphService.registerGraph("", new MemoryGraphSource("", graph));

                    }
                } else {
                    LOG.error("An error occurred while building the graph. Exiting.");
                    OtpStatusFile.exitStatusFailed();
                    System.exit(-1);
                }
            }

            /* Scan for graphs to load from disk if requested */
            // FIXME eventually router IDs will be present even when just building a graph.
            if ((params.routerIds != null && params.routerIds.size() > 0) || params.autoScan) {
                /* Auto-register pre-existing graph on disk, with optional auto-scan. */
                GraphScanner graphScanner = new GraphScanner(graphService, params.graphDirectory);
                graphScanner.basePath = params.graphDirectory;
                if (params.routerIds != null && params.routerIds.size() > 0) {
                    graphScanner.defaultRouterId = params.routerIds.get(0);
                }
                graphScanner.autoRegister = params.routerIds;
                graphScanner.startup();
            }

            /* Start visualizer if requested */
            if (params.visualize) {
                Router defaultRouter = graphService.getRouter();
                defaultRouter.graphVisualizer = new GraphVisualizer(defaultRouter);
                defaultRouter.graphVisualizer.run();
                defaultRouter.timeouts = new double[] { 60 }; // avoid timeouts due to search animation
            }
        } catch (Throwable throwable) {
            LOG.error("An uncaught {} error occurred during startup. Shutting down.",
                    throwable.getClass().getSimpleName(), throwable);
            OtpStatusFile.exitStatusFailed();
            System.exit(1);
        }

        /* Start web server if requested */
        if (params.server) {
            GrizzlyServer grizzlyServer = new GrizzlyServer(params, otpServer);
            while (true) { // Loop to restart server on uncaught fatal exceptions.
                try {
                    grizzlyServer.run();
                    return;
                } catch (Throwable throwable) {
                    LOG.error("An uncaught {} occurred inside OTP. Restarting server.",
                            throwable.getClass().getSimpleName(), throwable);
                }
            }
        }
    }

    /**
     * Create a cached GraphService that will be used by all OTP components to resolve router IDs to
     * Graphs. If a graph is supplied (graph parameter is not null) then that graph is also
     * registered.
     */
    public void makeGraphService () {
        graphService = new GraphService();
    }
}
