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

package org.opentripplanner.updater.siri;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.updater.SiriHelper;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.UUID;

public class SiriVMHttpTripUpdateSource implements VehicleMonitoringSource, JsonConfigurable {
    private static final Logger LOG =
            LoggerFactory.getLogger(SiriVMHttpTripUpdateSource.class);

    /**
     * True iff the last list with updates represent all updates that are active right now, i.e. all
     * previous updates should be disregarded
     */
    private boolean fullDataset = true;

    /**
     * Feed id that is used to match trip ids in the TripUpdates
     */
    private String feedId;

    private String url;

    private ZonedDateTime lastTimestamp = ZonedDateTime.now().minusMonths(1);

    private String requestorRef;
    private int timeout;

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
        String url = config.path("url").asText();
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        }
        this.url = url;

        this.requestorRef = config.path("requestorRef").asText();
        if (requestorRef == null || requestorRef.isEmpty()) {
            requestorRef = UUID.randomUUID().toString();
        }
        this.feedId = config.path("feedId").asText();

        int timeoutSec = config.path("timeoutSec").asInt();
        if (timeoutSec > 0) {
            this.timeout = 1000*timeoutSec;
        }

    }

    @Override
    public Siri getUpdates() {
        long t1 = System.currentTimeMillis();
        long creating = 0;
        long fetching = 0;
        long unmarshalling = 0;

        fullDataset = false;
        try {
            String vmServiceRequest = SiriHelper.createVMServiceRequestAsXml(requestorRef);
            creating = System.currentTimeMillis()-t1;
            t1 = System.currentTimeMillis();

            InputStream is = HttpUtils.postData(url, vmServiceRequest, timeout);
            if (is != null) {
                // Decode message
                fetching = System.currentTimeMillis()-t1;
                t1 = System.currentTimeMillis();
                Siri siri = SiriHelper.unmarshal(is);
                unmarshalling = System.currentTimeMillis()-t1;

                if (siri.getServiceDelivery().getResponseTimestamp().isBefore(lastTimestamp)) {
                    LOG.info("Newer data has already been processed");
                    return null;
                }
                lastTimestamp = siri.getServiceDelivery().getResponseTimestamp();

                RuterSiriHelper.rewriteVmIds(siri.getServiceDelivery().getVehicleMonitoringDeliveries());
                return siri;

            }
        } catch (Exception e) {
            LOG.info("Failed after {} ms", (System.currentTimeMillis()-t1));
            LOG.warn("Failed to parse SIRI-VM feed from " + url + ":", e);
        } finally {
            LOG.info("Updating VM: Create req: {}, Fetching data: {}, Unmarshalling: {}", creating, fetching, unmarshalling);
        }
        return null;
    }

    @Override
    public boolean getFullDatasetValueOfLastUpdates() {
        return fullDataset;
    }
    
    public String toString() {
        return "SiriVMHttpTripUpdateSource(" + url + ")";
    }

    @Override
    public String getFeedId() {
        return this.feedId;
    }
}
